package org.broadinstitute.consent.http.filters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.broadinstitute.consent.http.filters.ClientIpResolver.ClientIp;
import org.broadinstitute.consent.http.filters.ClientIpResolver.KeySource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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
  void testIpv6ClientIsKeyedByItsSlash64() {
    assertEquals(
        new ClientIp("2001:db8:1:2::/64", KeySource.REAL_IP_HEADER),
        resolver.resolve("::1", List.of("2001:DB8:1:2:aaaa:bbbb:cccc:dddd")));
  }

  @Test
  void testIpv6AddressesInOneSlash64ShareAKey() {
    // One caller rotating through its /64 must not get a fresh bucket for each address.
    String first = resolver.resolve("::1", List.of("2001:db8:1:2::1")).key();
    String second = resolver.resolve("::1", List.of("2001:db8:1:2:ffff:ffff:ffff:ffff")).key();

    assertEquals(first, second);
  }

  @Test
  void testIpv6AddressesInDifferentSlash64sHaveDifferentKeys() {
    String first = resolver.resolve("::1", List.of("2001:db8:1:2::1")).key();
    String second = resolver.resolve("::1", List.of("2001:db8:1:3::1")).key();

    assertNotEquals(first, second);
  }

  @Test
  void testUntrustedIpv6PeerIsKeyedByItsSlash64() {
    assertEquals(
        new ClientIp("2001:db8:9:9::/64", KeySource.UNTRUSTED_PEER),
        resolver.resolve("2001:db8:9:9::42", List.of("192.0.2.1")));
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

  @ParameterizedTest
  @ValueSource(
      strings = {
        "localhost", // a hostname; Jetty alone would resolve it with DNS
        "127.0.0.1/33", // a prefix longer than the address
        "127.0.0.1/abc", // a non-numeric prefix
        "127.0.0.1/3x", // trailing text; Jetty alone would read it as /3
        "10.0.0.1-10.0.0.9", // a min-max range; Jetty alone would accept it
        "10.64.1.0/10" // a CIDR range with host bits set
      })
  void testInvalidTrustedProxyIsRejected(String entry) {
    List<String> trustedProxies = List.of(entry);

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
