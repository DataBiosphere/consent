package org.broadinstitute.consent.http.filters;

import com.google.inject.Inject;
import jakarta.annotation.Priority;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Caches the {@code OAUTH2_CLAIM_*} headers that the Apache sidecar sets after it validates a
 * token. The authenticators build the user from this cache, so only claims that the sidecar set may
 * enter it, and only a request that came through the sidecar may use them.
 *
 * <p>The sidecar runs in the same pod and reaches Consent on loopback. A peer that is not loopback,
 * for example another pod that calls the pod IP on the app port, skipped the sidecar (DT-4419). For
 * such a request the filter removes the {@code Authorization} header on every route, and it does
 * not fill the cache. The authenticators then get no bearer, so they cannot read claims that an
 * earlier sidecar request cached for the same token. The request is anonymous: protected routes
 * return 401, and open routes still work. Cache entries are not evicted, so the real user's
 * requests through the sidecar are not affected.
 *
 * <p>The auth filter reads a bearer only from the {@code Authorization} header, never from the
 * {@code access_token} query parameter (DT-4428). So removing the header is enough: a direct caller
 * has no other way to send a bearer to the authenticators.
 *
 * <p>For a loopback peer, the cache is filled only on a route the sidecar authenticates (/api). See
 * the route pattern below.
 *
 * <p>The filter does not log a request from a peer that is not loopback, so a direct caller cannot
 * add a log line to each request.
 */
@Provider
@Priority(Integer.MIN_VALUE)
public class RequestHeaderCacheFilter implements ContainerRequestFilter {

  /**
   * Routes the sidecar authenticates. Only /api is authenticated; open routes (for example
   * /status), /swagger, and the root are not. The cache must be filled only on these routes, so a
   * request on an open route cannot plant claims that a later authenticated request with the same
   * token would reuse. The check uses the raw path, so a matrix parameter (for example
   * /api;x=1/...) does not match and the sidecar did not authenticate it. See DT-4316 and DT-4317.
   */
  private static final Pattern AUTHENTICATED_ROUTE = Pattern.compile("^/api(/|$)");

  private final ClaimsCache claimsCache;

  @Context private HttpServletRequest servletRequest;

  @Inject
  public RequestHeaderCacheFilter(ClaimsCache claimsCache) {
    this.claimsCache = claimsCache;
  }

  RequestHeaderCacheFilter(ClaimsCache claimsCache, HttpServletRequest servletRequest) {
    this(claimsCache);
    this.servletRequest = servletRequest;
  }

  @Override
  public void filter(ContainerRequestContext containerRequestContext) throws IOException {
    var headers = containerRequestContext.getHeaders();
    if (!ClientIpResolver.isLoopback(servletRequest.getRemoteAddr())) {
      // Runs before the auth filter (Priorities.AUTHENTICATION), so the request has no bearer.
      headers.remove(HttpHeaders.AUTHORIZATION);
      return;
    }
    if (!isAuthenticatedRoute(containerRequestContext)) {
      return;
    }
    var token = headers.getFirst(HttpHeaders.AUTHORIZATION);
    if (token != null) {
      var bearer = token.replaceFirst("(?i)^Bearer ", "");
      claimsCache.loadCache(bearer, headers);
    }
  }

  private boolean isAuthenticatedRoute(ContainerRequestContext containerRequestContext) {
    var rawPath = containerRequestContext.getUriInfo().getRequestUri().getRawPath();
    return rawPath != null && AUTHENTICATED_ROUTE.matcher(rawPath).find();
  }
}
