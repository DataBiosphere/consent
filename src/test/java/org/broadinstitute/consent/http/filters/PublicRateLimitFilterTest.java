package org.broadinstitute.consent.http.filters;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.lenient;
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
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Stream;
import org.broadinstitute.consent.http.configurations.PublicRateLimitConfiguration;
import org.broadinstitute.consent.http.configurations.RateLimitConfiguration;
import org.glassfish.jersey.uri.UriComponent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PublicRateLimitFilterTest {

  private static final String TRUSTED_PROXY = "127.0.0.1";
  private static final String PROXY_SECRET = "test-consent-proxy-secret-0123456789abcdef";
  private static final String OTHER_SECRET = "unrelated-consent-proxy-secret-0123456789";
  private static final long NOW = 1793376000L;
  private static final Clock CLOCK = Clock.fixed(Instant.ofEpochSecond(NOW), ZoneOffset.UTC);
  // The DUOS pod's egress address, which the load balancer writes into X-Real-IP for every
  // BFF-proxied request.
  private static final String BFF_EGRESS = "192.0.2.1";

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

  private PublicRateLimitFilter buildSigningFilter(String current, String previous) {
    PublicRateLimitConfiguration showcase = new PublicRateLimitConfiguration();
    showcase.setRequestsPerMinute(1);
    showcase.setTrustedProxies(List.of("127.0.0.1/32"));
    RateLimitConfiguration config = new RateLimitConfiguration();
    config.setEnabled(true);
    config.setRequestsPerMinute(100);
    config.setPodCount(1);
    config.setShowcase(showcase);
    config.setClientIpSecret(current);
    config.setClientIpPreviousSecret(previous);
    return spy(new PublicRateLimitFilter(config, servletRequest, CLOCK));
  }

  private void mockSignedRequest(String ip, String timestamp, String signature) {
    mockPath("showcase/datasets");
    // A verified signature never reads the peer, so this stub is lenient.
    lenient().when(servletRequest.getRemoteAddr()).thenReturn(TRUSTED_PROXY);
    putHeader(PublicRateLimitFilter.REAL_IP_HEADER, BFF_EGRESS);
    when(requestContext.getHeaders()).thenReturn(headers);
    putHeader(SignedClientIpVerifier.CLIENT_IP_HEADER, ip);
    putHeader(SignedClientIpVerifier.TIMESTAMP_HEADER, timestamp);
    putHeader(SignedClientIpVerifier.DIGEST_HEADER, signature);
  }

  private void putHeader(String name, String value) {
    headers.remove(name);
    if (value != null) {
      headers.add(name, value);
    }
  }

  private static String sign(String hmacSecret, String ip, long timestamp) {
    return SignedClientIpVerifier.hmacHex(hmacSecret, ip, timestamp);
  }

  private void mockRequest(String path, String peer, String realIp) {
    mockPath(path);
    when(servletRequest.getRemoteAddr()).thenReturn(peer);
    headers.remove(PublicRateLimitFilter.REAL_IP_HEADER);
    if (realIp != null) {
      headers.add(PublicRateLimitFilter.REAL_IP_HEADER, realIp);
    }
    when(requestContext.getHeaders()).thenReturn(headers);
  }

  private void mockPath(String path) {
    when(requestContext.getUriInfo()).thenReturn(uriInfo);
    // The path as sent, still encoded; the segments are Jersey's decoded form of it.
    when(uriInfo.getPath(false)).thenReturn(path);
    // Jersey's own parser, so the segments match what the filter gets at run time.
    when(uriInfo.getPathSegments()).thenReturn(UriComponent.decodePath(path, true));
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
  void testScopedRealIpFallsBackToPeerWithoutError() {
    // Guava's forString throws for an unknown interface name; the filter must not return a 500.
    PublicRateLimitFilter filter = spy(buildFilter(1, 1, true));
    mockRequest("showcase/datasets", TRUSTED_PROXY, "fe80::1%eth0");

    assertDoesNotThrow(() -> send(filter, 2));

    verify(filter)
        .logWarn(
            "Showcase rate limit exceeded: key=127.0.0.1, source=TRUSTED_PEER_WITHOUT_HEADER, "
                + "path=showcase/datasets");
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
  void testLogKeepsEncodedLineBreaksInThePath() {
    // A decoded path would carry a real line break into the log message.
    PublicRateLimitFilter filter = spy(buildFilter(1, 1, true));
    mockRequest("showcase/datasets%0A%0Dforged", TRUSTED_PROXY, "192.0.2.1");

    send(filter, 2);

    verify(requestContext).abortWith(any());
    verify(filter)
        .logWarn(
            "Showcase rate limit exceeded: key=192.0.2.1, source=REAL_IP_HEADER, "
                + "path=showcase/datasets%0A%0Dforged");
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

  @Test
  void testValidSignatureKeysOnTheForwardedIp() {
    PublicRateLimitFilter filter = buildSigningFilter(PROXY_SECRET, null);

    // Same X-Real-IP (the BFF pod), different browsers: each gets its own bucket.
    mockSignedRequest("203.0.113.7", Long.toString(NOW), sign(PROXY_SECRET, "203.0.113.7", NOW));
    filter.filter(requestContext);
    mockSignedRequest("203.0.113.8", Long.toString(NOW), sign(PROXY_SECRET, "203.0.113.8", NOW));
    filter.filter(requestContext);

    verify(requestContext, never()).abortWith(any());

    mockSignedRequest("203.0.113.7", Long.toString(NOW), sign(PROXY_SECRET, "203.0.113.7", NOW));
    filter.filter(requestContext);

    verify(requestContext).abortWith(any());
    verify(filter)
        .logWarn(
            "Showcase rate limit exceeded: key=203.0.113.7, source=SIGNED_CLIENT_IP_HEADER, "
                + "path=showcase/datasets");
    verify(filter, never()).logWarn(startsWith("Showcase signed"));
  }

  @Test
  void testSignedIpv6IsKeyedOnItsSlash64() {
    PublicRateLimitFilter filter = buildSigningFilter(PROXY_SECRET, null);
    String ip = "2001:db8:1:2::";

    mockSignedRequest(ip, Long.toString(NOW), sign(PROXY_SECRET, ip, NOW));
    send(filter, 2);

    verify(filter)
        .logWarn(
            "Showcase rate limit exceeded: key=2001:db8:1:2::/64, source=SIGNED_CLIENT_IP_HEADER, "
                + "path=showcase/datasets");
  }

  @Test
  void testPreviousSecretIsAcceptedDuringRotation() {
    PublicRateLimitFilter filter = buildSigningFilter(PROXY_SECRET, OTHER_SECRET);

    mockSignedRequest("203.0.113.7", Long.toString(NOW), sign(OTHER_SECRET, "203.0.113.7", NOW));
    send(filter, 2);

    verify(filter)
        .logWarn(
            "Showcase rate limit exceeded: key=203.0.113.7, source=SIGNED_CLIENT_IP_HEADER, "
                + "path=showcase/datasets");
  }

  static Stream<Arguments> rejectedSignedHeaders() {
    String ip = "203.0.113.7";
    String ts = Long.toString(NOW);
    String valid = sign(PROXY_SECRET, ip, NOW);
    return Stream.of(
        Arguments.of("spoofed IP", "198.51.100.1", ts, valid, "BAD_SIGNATURE"),
        Arguments.of("wrong signature", ip, ts, sign(OTHER_SECRET, ip, NOW), "BAD_SIGNATURE"),
        Arguments.of("missing signature", ip, ts, null, "INCOMPLETE"),
        Arguments.of("missing timestamp", ip, null, valid, "INCOMPLETE"),
        Arguments.of(
            "stale timestamp",
            ip,
            Long.toString(NOW - 61),
            sign(PROXY_SECRET, ip, NOW - 61),
            "OUTSIDE_WINDOW"),
        Arguments.of(
            "previous secret after its removal",
            ip,
            ts,
            sign("previous-consent-proxy-secret-0123456789ab", ip, NOW),
            "BAD_SIGNATURE"));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("rejectedSignedHeaders")
  void testRejectedSignatureFallsBackToRealIp(
      String label, String ip, String timestamp, String signature, String reason) {
    PublicRateLimitFilter filter = buildSigningFilter(PROXY_SECRET, null);
    mockSignedRequest(ip, timestamp, signature);

    send(filter, 2);

    verify(filter, times(2)).logWarn("Showcase signed client IP rejected: reason=" + reason);
    verify(filter)
        .logWarn(
            "Showcase rate limit exceeded: key=192.0.2.1, source=REAL_IP_HEADER, "
                + "path=showcase/datasets");
  }

  @Test
  void testRequestWithoutSignedHeadersLogsNoRejection() {
    PublicRateLimitFilter filter = buildSigningFilter(PROXY_SECRET, null);
    mockRequest("showcase/datasets", TRUSTED_PROXY, "192.0.2.9");

    send(filter, 1);

    verify(filter, never()).logWarn(anyString());
  }
}
