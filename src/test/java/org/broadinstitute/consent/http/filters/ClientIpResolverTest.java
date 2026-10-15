package org.broadinstitute.consent.http.filters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.broadinstitute.consent.http.filters.ClientIpResolver.ClientIp;
import org.broadinstitute.consent.http.filters.ClientIpResolver.KeySource;
import org.junit.jupiter.api.Test;

class ClientIpResolverTest {

  // Loopback stands for the Apache sidecar in the same pod.
  private final ClientIpResolver resolver =
      new ClientIpResolver(List.of("127.0.0.1/32", "::1/128"));

  @Test
  void testTrustedPeerUsesRealIpHeader() {
    assertEquals(
        new ClientIp("192.0.2.1", KeySource.REAL_IP_HEADER),
        resolver.resolve("127.0.0.1", List.of("192.0.2.1")));
  }

  @Test
  void testUntrustedPeerIgnoresRealIpHeader() {
    assertEquals(
        new ClientIp("192.0.2.50", KeySource.UNTRUSTED_PEER),
        resolver.resolve("192.0.2.50", List.of("192.0.2.99")));
  }

  @Test
  void testUntrustedPeerWithNoHeaderUsesPeer() {
    assertEquals(
        new ClientIp("192.0.2.50", KeySource.UNTRUSTED_PEER),
        resolver.resolve("192.0.2.50", List.of()));
  }

  @Test
  void testTrustedPeerWithNoHeaderUsesPeer() {
    assertEquals(
        new ClientIp("127.0.0.1", KeySource.TRUSTED_PEER_WITHOUT_HEADER),
        resolver.resolve("127.0.0.1", List.of()));
  }

  @Test
  void testRepeatedHeaderUsesPeer() {
    // The load balancer writes one value, so two values did not come from it alone.
    assertEquals(
        new ClientIp("127.0.0.1", KeySource.TRUSTED_PEER_WITHOUT_HEADER),
        resolver.resolve("127.0.0.1", List.of("192.0.2.99", "192.0.2.1")));
  }

  @Test
  void testCommaListInHeaderUsesPeer() {
    assertEquals(
        new ClientIp("127.0.0.1", KeySource.TRUSTED_PEER_WITHOUT_HEADER),
        resolver.resolve("127.0.0.1", List.of("192.0.2.99, 192.0.2.1")));
  }

  @Test
  void testUnparseableHeaderUsesPeer() {
    assertEquals(
        new ClientIp("127.0.0.1", KeySource.TRUSTED_PEER_WITHOUT_HEADER),
        resolver.resolve("127.0.0.1", List.of("not-an-ip")));
  }

  @Test
  void testHostnameInHeaderIsNotResolved() {
    assertEquals(
        new ClientIp("127.0.0.1", KeySource.TRUSTED_PEER_WITHOUT_HEADER),
        resolver.resolve("127.0.0.1", List.of("localhost")));
  }

  @Test
  void testHeaderWhitespaceIsStripped() {
    assertEquals(
        new ClientIp("192.0.2.1", KeySource.REAL_IP_HEADER),
        resolver.resolve("127.0.0.1", List.of(" 192.0.2.1 ")));
  }

  @Test
  void testIpv6ClientIsNormalized() {
    assertEquals(
        new ClientIp("2001:db8::1", KeySource.REAL_IP_HEADER),
        resolver.resolve("::1", List.of("2001:DB8:0:0:0:0:0:1")));
  }

  @Test
  void testIpv4MappedPeerMatchesIpv4Range() {
    assertEquals(
        new ClientIp("192.0.2.1", KeySource.REAL_IP_HEADER),
        resolver.resolve("::ffff:127.0.0.1", List.of("192.0.2.1")));
  }

  @Test
  void testCidrRangeMatchesPartialByte() {
    ClientIpResolver rangeResolver = new ClientIpResolver(List.of("10.64.0.0/10"));

    assertEquals(
        KeySource.REAL_IP_HEADER,
        rangeResolver.resolve("10.127.255.255", List.of("192.0.2.1")).source());
    assertEquals(
        KeySource.UNTRUSTED_PEER,
        rangeResolver.resolve("10.128.0.1", List.of("192.0.2.1")).source());
  }

  @Test
  void testNullPeerUsesUnknownKey() {
    assertEquals(
        new ClientIp("unknown", KeySource.UNTRUSTED_PEER),
        resolver.resolve(null, List.of("192.0.2.1")));
  }

  @Test
  void testHostnameIsRejectedAsTrustedProxy() {
    List<String> trustedProxies = List.of("localhost");

    assertThrows(IllegalArgumentException.class, () -> new ClientIpResolver(trustedProxies));
  }

  @Test
  void testPrefixLongerThanAddressIsRejected() {
    List<String> trustedProxies = List.of("127.0.0.1/33");

    assertThrows(IllegalArgumentException.class, () -> new ClientIpResolver(trustedProxies));
  }

  @Test
  void testNonNumericPrefixIsRejected() {
    List<String> trustedProxies = List.of("127.0.0.1/abc");

    assertThrows(IllegalArgumentException.class, () -> new ClientIpResolver(trustedProxies));
  }

  @Test
  void testPrefixWithTrailingTextIsRejected() {
    // Jetty alone parses the prefix leniently.
    List<String> trustedProxies = List.of("127.0.0.1/3x");

    assertThrows(IllegalArgumentException.class, () -> new ClientIpResolver(trustedProxies));
  }

  @Test
  void testMinMaxRangeIsRejected() {
    // Jetty alone accepts this form.
    List<String> trustedProxies = List.of("10.0.0.1-10.0.0.9");

    assertThrows(IllegalArgumentException.class, () -> new ClientIpResolver(trustedProxies));
  }

  @Test
  void testCidrWithHostBitsSetIsRejected() {
    List<String> trustedProxies = List.of("10.64.1.0/10");

    assertThrows(IllegalArgumentException.class, () -> new ClientIpResolver(trustedProxies));
  }

  @Test
  void testBareLiteralTrustsOnlyThatAddress() {
    ClientIpResolver literalResolver = new ClientIpResolver(List.of("10.1.2.3"));

    assertEquals(
        KeySource.REAL_IP_HEADER,
        literalResolver.resolve("10.1.2.3", List.of("192.0.2.1")).source());
    assertEquals(
        KeySource.UNTRUSTED_PEER,
        literalResolver.resolve("10.1.2.4", List.of("192.0.2.1")).source());
  }
}
