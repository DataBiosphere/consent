package org.broadinstitute.consent.http.filters;

import com.google.inject.Inject;
import jakarta.annotation.Priority;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.regex.Pattern;

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

  @Inject
  public RequestHeaderCacheFilter(ClaimsCache claimsCache) {
    this.claimsCache = claimsCache;
  }

  @Override
  public void filter(ContainerRequestContext containerRequestContext) throws IOException {
    if (!isAuthenticatedRoute(containerRequestContext)) {
      return;
    }
    var headers = containerRequestContext.getHeaders();
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
