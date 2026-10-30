package org.broadinstitute.consent.http.filters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.broadinstitute.consent.http.filters.SignedClientIpVerifier.Rejection;
import org.broadinstitute.consent.http.filters.SignedClientIpVerifier.Result;
import org.junit.jupiter.api.Test;

class SignedClientIpVerifierTest {

  // The same vector is in duos-ui's server/test/clientIpSignature.test.ts. A change to the signed
  // bytes on one side only fails one of the two tests.
  private static final String CURRENT = "test-consent-proxy-secret-0123456789abcdef";
  private static final String IP = "203.0.113.7";
  private static final long TIMESTAMP = 1793376000L;
  private static final String VECTOR =
      "c22a76358b1464270b18f819cc3192670ea01d728241c9754b8973e0333ac6ad";

  private static final String PREVIOUS = "previous-consent-proxy-secret-0123456789ab";
  private static final String OTHER = "unrelated-consent-proxy-secret-0123456789";

  private static Clock clockAt(long epochSeconds) {
    return Clock.fixed(Instant.ofEpochSecond(epochSeconds), ZoneOffset.UTC);
  }

  private static SignedClientIpVerifier verifier(String current, String previous) {
    return new SignedClientIpVerifier(current, previous, clockAt(TIMESTAMP));
  }

  private static MultivaluedMap<String, String> headers(
      String ip, String timestamp, String signature) {
    MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
    if (ip != null) {
      headers.add(SignedClientIpVerifier.CLIENT_IP_HEADER, ip);
    }
    if (timestamp != null) {
      headers.add(SignedClientIpVerifier.TIMESTAMP_HEADER, timestamp);
    }
    if (signature != null) {
      headers.add(SignedClientIpVerifier.DIGEST_HEADER, signature);
    }
    return headers;
  }

  private static MultivaluedMap<String, String> signedWith(String hmacSecret, String ip, long ts) {
    return headers(ip, Long.toString(ts), SignedClientIpVerifier.hmacHex(hmacSecret, ip, ts));
  }

  private static void assertRejected(Rejection expected, Result result) {
    assertNull(result.clientIp());
    assertEquals(expected, result.rejection());
  }

  @Test
  void testSharedVectorMatchesTheBff() {
    assertEquals(VECTOR, SignedClientIpVerifier.hmacHex(CURRENT, IP, TIMESTAMP));
  }

  @Test
  void testValidSignatureGivesTheForwardedIp() {
    Result result = verifier(CURRENT, null).verify(headers(IP, "1793376000", VECTOR));

    assertEquals(IP, result.clientIp());
    assertNull(result.rejection());
  }

  @Test
  void testNoSignedHeadersIsNotARejection() {
    Result result = verifier(CURRENT, null).verify(headers(null, null, null));

    assertNull(result.clientIp());
    assertNull(result.rejection());
  }

  @Test
  void testSpoofedIpWithTheSignatureOfAnotherIpIsRejected() {
    Result result = verifier(CURRENT, null).verify(headers("198.51.100.1", "1793376000", VECTOR));

    assertRejected(Rejection.BAD_SIGNATURE, result);
  }

  @Test
  void testWrongSignatureIsRejected() {
    Result result = verifier(CURRENT, null).verify(signedWith(OTHER, IP, TIMESTAMP));

    assertRejected(Rejection.BAD_SIGNATURE, result);
  }

  @Test
  void testMissingSignatureIsRejected() {
    Result result = verifier(CURRENT, null).verify(headers(IP, "1793376000", null));

    assertRejected(Rejection.INCOMPLETE, result);
  }

  @Test
  void testMissingTimestampIsRejected() {
    Result result = verifier(CURRENT, null).verify(headers(IP, null, VECTOR));

    assertRejected(Rejection.INCOMPLETE, result);
  }

  @Test
  void testRepeatedHeaderIsRejected() {
    MultivaluedMap<String, String> headers = headers(IP, "1793376000", VECTOR);
    headers.add(SignedClientIpVerifier.CLIENT_IP_HEADER, "198.51.100.1");

    assertRejected(Rejection.INCOMPLETE, verifier(CURRENT, null).verify(headers));
  }

  @Test
  void testTimestampOlderThanTheWindowIsRejected() {
    long stale = TIMESTAMP - SignedClientIpVerifier.MAX_AGE_SECONDS - 1;

    Result result = verifier(CURRENT, null).verify(signedWith(CURRENT, IP, stale));

    assertRejected(Rejection.OUTSIDE_WINDOW, result);
  }

  @Test
  void testTimestampAtTheEdgeOfTheWindowIsAccepted() {
    long oldest = TIMESTAMP - SignedClientIpVerifier.MAX_AGE_SECONDS;
    long newest = TIMESTAMP + SignedClientIpVerifier.MAX_CLOCK_SKEW_SECONDS;

    assertEquals(IP, verifier(CURRENT, null).verify(signedWith(CURRENT, IP, oldest)).clientIp());
    assertEquals(IP, verifier(CURRENT, null).verify(signedWith(CURRENT, IP, newest)).clientIp());
  }

  @Test
  void testTimestampFurtherAheadThanTheSkewIsRejected() {
    long future = TIMESTAMP + SignedClientIpVerifier.MAX_CLOCK_SKEW_SECONDS + 1;

    Result result = verifier(CURRENT, null).verify(signedWith(CURRENT, IP, future));

    assertRejected(Rejection.OUTSIDE_WINDOW, result);
  }

  @Test
  void testMalformedValuesAreRejected() {
    SignedClientIpVerifier verifier = verifier(CURRENT, null);

    assertRejected(Rejection.MALFORMED, verifier.verify(headers(IP, "1793376000.5", VECTOR)));
    assertRejected(
        Rejection.MALFORMED, verifier.verify(headers(IP, "1793376000", VECTOR.toUpperCase())));
    assertRejected(
        Rejection.MALFORMED, verifier.verify(headers(IP, "1793376000", VECTOR.substring(2))));
    assertRejected(
        Rejection.MALFORMED, verifier.verify(signedWith(CURRENT, "consent.example", TIMESTAMP)));
  }

  @Test
  void testPreviousSecretIsAcceptedDuringRotation() {
    Result result = verifier(OTHER, PREVIOUS).verify(signedWith(PREVIOUS, IP, TIMESTAMP));

    assertEquals(IP, result.clientIp());
  }

  @Test
  void testPreviousSecretIsRejectedAfterItsRemoval() {
    Result result = verifier(OTHER, null).verify(signedWith(PREVIOUS, IP, TIMESTAMP));

    assertRejected(Rejection.BAD_SIGNATURE, result);
  }

  @Test
  void testSignedHeadersWithoutAConfiguredSecretAreRejected() {
    Result result = verifier(null, null).verify(headers(IP, "1793376000", VECTOR));

    assertRejected(Rejection.NO_SECRET_CONFIGURED, result);
  }
}
