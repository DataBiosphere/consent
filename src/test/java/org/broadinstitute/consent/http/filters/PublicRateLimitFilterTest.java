package org.broadinstitute.consent.http.filters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.util.List;
import org.broadinstitute.consent.http.configurations.PublicRateLimitConfiguration;
import org.broadinstitute.consent.http.configurations.RateLimitConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PublicRateLimitFilterTest {

  private static final String TRUSTED_PROXY = "127.0.0.1";

  @Mock private ContainerRequestContext requestContext;
  @Mock private UriInfo uriInfo;
  @Mock private HttpServletRequest servletRequest;

  private final MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();

  private PublicRateLimitFilter buildFilter(
      int requestsPerMinute, int podCount, boolean authenticatedLimiterEnabled) {
    PublicRateLimitConfiguration showcase = new PublicRateLimitConfiguration();
    showcase.setRequestsPerMinute(requestsPerMinute);
    showcase.setTrustedProxies(List.of("127.0.0.1/32"));
    RateLimitConfiguration config = new RateLimitConfiguration();
    config.setEnabled(authenticatedLimiterEnabled);
    config.setRequestsPerMinute(100);
    config.setPodCount(podCount);
    config.setShowcase(showcase);
    return new PublicRateLimitFilter(config, servletRequest);
  }

  private void mockRequest(String path, String peer, String realIp) {
    when(requestContext.getUriInfo()).thenReturn(uriInfo);
    when(uriInfo.getPath()).thenReturn(path);
    when(servletRequest.getRemoteAddr()).thenReturn(peer);
    headers.remove(PublicRateLimitFilter.REAL_IP_HEADER);
    if (realIp != null) {
      headers.add(PublicRateLimitFilter.REAL_IP_HEADER, realIp);
    }
    when(requestContext.getHeaders()).thenReturn(headers);
  }

  private void mockPath(String path) {
    when(requestContext.getUriInfo()).thenReturn(uriInfo);
    when(uriInfo.getPath()).thenReturn(path);
  }

  private void send(PublicRateLimitFilter filter, int count) {
    for (int i = 0; i < count; i++) {
      filter.filter(requestContext);
    }
  }

  @Test
  void testAllowsRequestsWithinPerPodCapacity() {
    // requestsPerMinute / podCount == 3 per pod
    PublicRateLimitFilter filter = buildFilter(9, 3, true);
    mockRequest("showcase/datasets", "192.0.2.50", null);

    send(filter, 3);

    verify(requestContext, never()).abortWith(any());
  }

  @Test
  void testRejectsRequestsOverTheLimitWithRetryAfter() {
    PublicRateLimitFilter filter = buildFilter(9, 3, true);
    mockRequest("showcase/datasets", "192.0.2.50", null);

    send(filter, 4);

    ArgumentCaptor<Response> captor = ArgumentCaptor.forClass(Response.class);
    verify(requestContext).abortWith(captor.capture());
    assertEquals(429, captor.getValue().getStatus());
    long retryAfterSeconds = Long.parseLong(captor.getValue().getHeaderString("Retry-After"));
    assertTrue(retryAfterSeconds > 0 && retryAfterSeconds <= 60);
  }

  @Test
  void testRejectsWhenTheAuthenticatedLimiterIsDisabled() {
    PublicRateLimitFilter filter = buildFilter(1, 1, false);
    mockRequest("showcase/datasets", "192.0.2.50", null);

    send(filter, 2);

    verify(requestContext).abortWith(any());
  }

  @Test
  void testDifferentPeersHaveIndependentBuckets() {
    PublicRateLimitFilter filter = buildFilter(1, 1, true);

    mockRequest("showcase/datasets", "192.0.2.50", null);
    filter.filter(requestContext);
    mockRequest("showcase/datasets", "192.0.2.51", null);
    filter.filter(requestContext);

    verify(requestContext, never()).abortWith(any());
  }

  @Test
  void testSpoofedHeaderFromUntrustedPeerDoesNotChangeTheKey() {
    PublicRateLimitFilter filter = buildFilter(1, 1, true);

    mockRequest("showcase/datasets", "192.0.2.50", "198.51.100.1");
    filter.filter(requestContext);
    mockRequest("showcase/datasets", "192.0.2.50", "198.51.100.2");
    filter.filter(requestContext);

    verify(requestContext).abortWith(any());
  }

  @Test
  void testTrustedProxyRequestsAreKeyedByRealIp() {
    PublicRateLimitFilter filter = buildFilter(1, 1, true);

    mockRequest("showcase/datasets", TRUSTED_PROXY, "192.0.2.1");
    filter.filter(requestContext);
    mockRequest("showcase/datasets", TRUSTED_PROXY, "192.0.2.2");
    filter.filter(requestContext);

    verify(requestContext, never()).abortWith(any());

    mockRequest("showcase/datasets", TRUSTED_PROXY, "192.0.2.1");
    filter.filter(requestContext);

    verify(requestContext, times(1)).abortWith(any());
  }

  @Test
  void testShowcaseRootPathIsLimited() {
    PublicRateLimitFilter filter = buildFilter(1, 1, true);
    mockRequest("showcase", "192.0.2.50", null);

    send(filter, 2);

    verify(requestContext).abortWith(any());
  }

  @Test
  void testApiPathIsNotLimited() {
    PublicRateLimitFilter filter = buildFilter(1, 1, true);
    mockPath("api/user/me");

    send(filter, 20);

    verify(requestContext, never()).abortWith(any());
    verify(servletRequest, never()).getRemoteAddr();
  }

  @Test
  void testProbePathsAreNotLimited() {
    PublicRateLimitFilter filter = buildFilter(1, 1, true);

    for (String path : List.of("status", "liveness", "showcases", "swagger/index.html")) {
      mockPath(path);
      send(filter, 20);
    }

    verify(requestContext, never()).abortWith(any());
    verify(servletRequest, never()).getRemoteAddr();
  }

  @Test
  void testLogsKeyAndSourceOnEach429() {
    PublicRateLimitFilter filter = spy(buildFilter(1, 1, true));
    mockRequest("showcase/datasets", TRUSTED_PROXY, "192.0.2.1");

    send(filter, 3);

    verify(filter, times(2))
        .logWarn(
            "Showcase rate limit exceeded: key=192.0.2.1, source=REAL_IP_HEADER, "
                + "path=showcase/datasets");
  }

  @Test
  void testLogShowsFallbackWhenTrustedProxySendsNoHeader() {
    PublicRateLimitFilter filter = spy(buildFilter(1, 1, true));
    mockRequest("showcase/datasets", TRUSTED_PROXY, null);

    send(filter, 2);

    verify(filter)
        .logWarn(
            "Showcase rate limit exceeded: key=127.0.0.1, source=TRUSTED_PEER_WITHOUT_HEADER, "
                + "path=showcase/datasets");
  }

  @Test
  void testDoesNotLogWithinTheLimit() {
    PublicRateLimitFilter filter = spy(buildFilter(1, 1, true));
    mockRequest("showcase/datasets", "192.0.2.50", null);

    send(filter, 1);

    verify(filter, never()).logWarn(anyString());
  }
}
