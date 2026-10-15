package org.broadinstitute.consent.http.filters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class ClientIpResolverTest {

  // Loopback stands for the Apache sidecar; 35.191.0.0/16 for the load balancer front ends;
  // 203.0.113.10 for the load balancer address; 198.51.100.7 for the BFF egress address.
  private final ClientIpResolver resolver =
      new ClientIpResolver(
          List.of("127.0.0.1/32", "::1/128", "35.191.0.0/16", "203.0.113.10", "198.51.100.7"));

  @Test
  void testUntrustedPeerIgnoresForwardedHeader() {
    assertEquals("192.0.2.50", resolver.resolve("192.0.2.50", List.of("192.0.2.99")));
  }

  @Test
  void testUntrustedPeerWithNoHeaderUsesPeer() {
    assertEquals("192.0.2.50", resolver.resolve("192.0.2.50", List.of()));
  }

  @Test
  void testTrustedPeerWithNoHeaderUsesPeer() {
    assertEquals("127.0.0.1", resolver.resolve("127.0.0.1", List.of()));
  }

  @Test
  void testTrustedPeerUsesForwardedClient() {
    assertEquals("192.0.2.1", resolver.resolve("127.0.0.1", List.of("192.0.2.1")));
  }

  @Test
  void testSkipsTrustedHopsFromTheRight() {
    // Header as the Apache sidecar sees it behind the Google load balancer.
    assertEquals(
        "192.0.2.1", resolver.resolve("127.0.0.1", List.of("192.0.2.1, 203.0.113.10, 35.191.4.5")));
  }

  @Test
  void testCallerSuppliedEntriesLeftOfTheClientAreIgnored() {
    assertEquals(
        "192.0.2.1",
        resolver.resolve("127.0.0.1", List.of("10.9.9.9, 192.0.2.1, 203.0.113.10, 35.191.4.5")));
  }

  @Test
  void testCallerCannotSpoofATrustedAddressToShiftTheKey() {
    // The caller prepends a trusted address. The rightmost untrusted entry is still the caller.
    assertEquals(
        "192.0.2.1",
        resolver.resolve(
            "127.0.0.1", List.of("198.51.100.7, 192.0.2.1, 203.0.113.10, 35.191.4.5")));
  }

  @Test
  void testRequestThroughTheBffUsesTheForwardedBrowserAddress() {
    assertEquals(
        "192.0.2.1",
        resolver.resolve(
            "127.0.0.1", List.of("192.0.2.1, 198.51.100.7, 203.0.113.10, 35.191.4.5")));
  }

  @Test
  void testMultipleHeaderValuesAreReadInOrder() {
    assertEquals(
        "192.0.2.1", resolver.resolve("127.0.0.1", List.of("10.9.9.9", "192.0.2.1, 203.0.113.10")));
  }

  @Test
  void testAllTrustedEntriesUseTheLeftmostEntry() {
    assertEquals(
        "203.0.113.10", resolver.resolve("127.0.0.1", List.of("203.0.113.10, 35.191.4.5")));
  }

  @Test
  void testUnparseableClientEntryUsesPeer() {
    assertEquals("127.0.0.1", resolver.resolve("127.0.0.1", List.of("not-an-ip, 203.0.113.10")));
  }

  @Test
  void testEmptyEntriesAreSkipped() {
    assertEquals("192.0.2.1", resolver.resolve("127.0.0.1", List.of("192.0.2.1, , 203.0.113.10")));
  }

  @Test
  void testIpv6ClientIsNormalized() {
    assertEquals("2001:db8::1", resolver.resolve("::1", List.of("2001:DB8:0:0:0:0:0:1")));
  }

  @Test
  void testIpv4MappedPeerMatchesIpv4Range() {
    assertEquals("192.0.2.1", resolver.resolve("::ffff:127.0.0.1", List.of("192.0.2.1")));
  }

  @Test
  void testNullPeerUsesUnknownKey() {
    assertEquals("unknown", resolver.resolve(null, List.of("192.0.2.1")));
  }

  @Test
  void testHostnameIsRejectedAsTrustedProxy() {
    assertThrows(IllegalArgumentException.class, () -> new ClientIpResolver(List.of("localhost")));
  }

  @Test
  void testPrefixLongerThanAddressIsRejected() {
    assertThrows(
        IllegalArgumentException.class, () -> new ClientIpResolver(List.of("127.0.0.1/33")));
  }
}
