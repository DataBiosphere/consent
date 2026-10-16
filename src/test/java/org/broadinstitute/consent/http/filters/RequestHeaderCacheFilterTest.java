package org.broadinstitute.consent.http.filters;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriInfo;
import java.net.URI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RequestHeaderCacheFilterTest {

  @Mock private ClaimsCache claimsCache;
  @Mock private ContainerRequestContext requestContext;
  @Mock private UriInfo uriInfo;
  @Mock private MultivaluedMap<String, String> headers;

  private RequestHeaderCacheFilter filter;

  @BeforeEach
  void setUp() {
    filter = new RequestHeaderCacheFilter(claimsCache);
  }

  /** Point the request at the given raw path, as the sidecar would see it. */
  private void stubPath(String rawPath) {
    when(requestContext.getUriInfo()).thenReturn(uriInfo);
    when(uriInfo.getRequestUri()).thenReturn(URI.create("https://consent.local" + rawPath));
  }

  @ParameterizedTest
  @ValueSource(strings = {"Bearer test-token", "bearer test-token", "BEARER test-token"})
  void testFilterStripsBearerPrefixCaseInsensitivelyOnApiRoute(String authHeader) throws Exception {
    stubPath("/api/user/me");
    when(requestContext.getHeaders()).thenReturn(headers);
    when(headers.getFirst(HttpHeaders.AUTHORIZATION)).thenReturn(authHeader);

    filter.filter(requestContext);

    verify(claimsCache).loadCache("test-token", headers);
  }

  @Test
  void testFilterCachesOnBareApiRoute() throws Exception {
    stubPath("/api");
    when(requestContext.getHeaders()).thenReturn(headers);
    when(headers.getFirst(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer test-token");

    filter.filter(requestContext);

    verify(claimsCache).loadCache("test-token", headers);
  }

  @Test
  void testFilterDoesNotCallLoadCacheWhenNoAuthorizationHeaderOnApiRoute() throws Exception {
    stubPath("/api/user/me");
    when(requestContext.getHeaders()).thenReturn(headers);

    filter.filter(requestContext);

    verify(claimsCache, never()).loadCache(any(), any());
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
    stubPath(rawPath);

    filter.filter(requestContext);

    verify(claimsCache, never()).loadCache(any(), any());
  }
}
