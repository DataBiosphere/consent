package org.broadinstitute.consent.http.filters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RequestHeaderCacheFilterTest {

  private static final String SIDECAR = "127.0.0.1";

  @Mock private ClaimsCache claimsCache;
  @Mock private ContainerRequestContext requestContext;
  @Mock private HttpServletRequest servletRequest;
  @Mock private UriInfo uriInfo;
  @Mock private MultivaluedMap<String, String> headers;

  private RequestHeaderCacheFilter filter;

  @BeforeEach
  void setUp() {
    filter = new RequestHeaderCacheFilter(claimsCache, servletRequest);
  }

  /** Point the request at the given raw path, as the sidecar would see it. */
  private void stubPath(String rawPath) {
    when(requestContext.getUriInfo()).thenReturn(uriInfo);
    when(uriInfo.getRequestUri()).thenReturn(URI.create("https://consent.local" + rawPath));
  }

  /** An /api request with a bearer token, from the given loopback peer. */
  private void stubApiRequestFromLoopback(String peer) {
    when(servletRequest.getRemoteAddr()).thenReturn(peer);
    stubPath("/api/user/me");
    when(requestContext.getHeaders()).thenReturn(headers);
    when(headers.getFirst(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer test-token");
  }

  /** Headers that a direct caller sends: a bearer string and forged claims. */
  private static MultivaluedMap<String, String> forgedHeaders(String bearer) {
    MultivaluedMap<String, String> forged = new MultivaluedHashMap<>();
    forged.add(HttpHeaders.AUTHORIZATION, "Bearer " + bearer);
    forged.add(ClaimsCache.OAUTH2_CLAIM_email, "admin@example.com");
    forged.add(ClaimsCache.OAUTH2_CLAIM_name, "Forged Admin");
    return forged;
  }

  @ParameterizedTest
  @ValueSource(strings = {"Bearer test-token", "bearer test-token", "BEARER test-token"})
  void testFilterStripsBearerPrefixCaseInsensitivelyOnApiRoute(String authHeader) throws Exception {
    stubPath("/api/user/me");
    when(requestContext.getHeaders()).thenReturn(headers);
    when(headers.getFirst(HttpHeaders.AUTHORIZATION)).thenReturn(authHeader);
    when(servletRequest.getRemoteAddr()).thenReturn(SIDECAR);

    filter.filter(requestContext);

    verify(claimsCache).loadCache("test-token", headers);
  }

  @Test
  void testFilterCachesOnBareApiRoute() throws Exception {
    stubPath("/api");
    when(requestContext.getHeaders()).thenReturn(headers);
    when(headers.getFirst(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer test-token");
    when(servletRequest.getRemoteAddr()).thenReturn(SIDECAR);

    filter.filter(requestContext);

    verify(claimsCache).loadCache("test-token", headers);
  }

  @Test
  void testFilterDoesNotCallLoadCacheWhenNoAuthorizationHeaderOnApiRoute() throws Exception {
    stubPath("/api/user/me");
    when(requestContext.getHeaders()).thenReturn(headers);
    when(servletRequest.getRemoteAddr()).thenReturn(SIDECAR);

    filter.filter(requestContext);

    verify(claimsCache, never()).loadCache(any(), any());
  }

  /**
   * The Apache sidecar reaches Consent on loopback. Jetty reports an IPv6 peer in brackets, so the
   * bracketed form must also count.
   */
  @ParameterizedTest
  @ValueSource(strings = {"127.0.0.1", "::1", "[0:0:0:0:0:0:0:1]"})
  void testFilterCachesForLoopbackPeer(String peer) throws Exception {
    stubApiRequestFromLoopback(peer);

    filter.filter(requestContext);

    verify(claimsCache).loadCache("test-token", headers);
  }

  /**
   * A peer that is not loopback did not come through the sidecar, so its claim headers are not
   * trusted. On every route, the filter removes the Authorization header and does not fill the
   * cache, whatever headers the caller sends (DT-4419).
   */
  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"10.0.0.5", "192.0.2.50", "2001:db8::1", "[2001:db8::1]", "unknown"})
  void testFilterStripsAuthorizationForNonLoopbackPeer(String peer) throws Exception {
    when(servletRequest.getRemoteAddr()).thenReturn(peer);
    when(requestContext.getHeaders()).thenReturn(headers);

    filter.filter(requestContext);

    verify(headers).remove(HttpHeaders.AUTHORIZATION);
    verify(claimsCache, never()).loadCache(any(), any());
  }

  /**
   * The auth filter also runs on routes where the cache is not filled (for example /swagger, or
   * /api with a matrix parameter), so the header must go on those routes too.
   */
  @ParameterizedTest
  @ValueSource(strings = {"/api/user/me", "/api;x=1/user/me", "/swagger/index.html", "/status"})
  void testFilterStripsAuthorizationForNonLoopbackPeerOnEveryRoute(String rawPath)
      throws Exception {
    MultivaluedMap<String, String> forged = forgedHeaders("forged-token");
    when(servletRequest.getRemoteAddr()).thenReturn("10.0.0.5");
    when(requestContext.getHeaders()).thenReturn(forged);

    filter.filter(requestContext);

    assertNull(forged.get(HttpHeaders.AUTHORIZATION), rawPath);
  }

  /**
   * A direct caller sends a forged admin email with any bearer string. With a real cache, nothing
   * is stored under that bearer, so the authenticators find no user and the request is anonymous.
   */
  @Test
  void testForgedClaimsFromNonLoopbackPeerDoNotEnterRealCache() throws Exception {
    ClaimsCache realCache = new ClaimsCache();
    when(requestContext.getHeaders()).thenReturn(forgedHeaders("forged-token"));
    when(servletRequest.getRemoteAddr()).thenReturn("10.0.0.5");

    new RequestHeaderCacheFilter(realCache, servletRequest).filter(requestContext);

    assertNull(realCache.cache.getIfPresent("forged-token"));
  }

  /**
   * A sidecar request on loopback caches the real user's claims. A later direct request with the
   * same bearer must not reach those claims: the filter removes its Authorization header, so the
   * authenticators get no bearer. The cache entry stays, so the real user is not affected.
   */
  @Test
  void testNonLoopbackPeerCannotReuseClaimsCachedThroughSidecar() throws Exception {
    ClaimsCache realCache = new ClaimsCache();
    RequestHeaderCacheFilter realFilter = new RequestHeaderCacheFilter(realCache, servletRequest);
    MultivaluedMap<String, String> sidecar = new MultivaluedHashMap<>();
    sidecar.add(HttpHeaders.AUTHORIZATION, "Bearer shared-token");
    sidecar.add(ClaimsCache.OAUTH2_CLAIM_email, "user@example.com");
    stubPath("/api/user/me");
    when(requestContext.getHeaders()).thenReturn(sidecar);
    when(servletRequest.getRemoteAddr()).thenReturn(SIDECAR);
    realFilter.filter(requestContext);
    assertNotNull(realCache.cache.getIfPresent("shared-token"));

    MultivaluedMap<String, String> direct = forgedHeaders("shared-token");
    when(requestContext.getHeaders()).thenReturn(direct);
    when(servletRequest.getRemoteAddr()).thenReturn("10.0.0.5");
    realFilter.filter(requestContext);

    assertNull(direct.get(HttpHeaders.AUTHORIZATION));
    assertEquals(
        Map.of(ClaimsCache.OAUTH2_CLAIM_email, "user@example.com"),
        realCache.cache.getIfPresent("shared-token"));
  }

  /**
   * None of these routes is the authenticated /api route, so the filter must not fill the cache: an
   * open route (/status, /), a route the sidecar does not authenticate (/swagger), a matrix
   * parameter on the first segment (/api;x=1/...), and an /api lookalike prefix (/apidocs).
   */
  @ParameterizedTest
  @ValueSource(
      strings = {"/status", "/", "/swagger/index.html", "/api;x=1/user/me", "/apidocs/index.html"})
  void testFilterDoesNotCacheOnUnauthenticatedRoute(String rawPath) throws Exception {
    when(servletRequest.getRemoteAddr()).thenReturn(SIDECAR);
    when(requestContext.getHeaders()).thenReturn(headers);
    stubPath(rawPath);

    filter.filter(requestContext);

    verify(claimsCache, never()).loadCache(any(), any());
  }
}
