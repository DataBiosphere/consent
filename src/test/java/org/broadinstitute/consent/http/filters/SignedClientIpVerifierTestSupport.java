package org.broadinstitute.consent.http.filters;

/** Gives tests outside this package the BFF's signature, without making it public in main. */
public final class SignedClientIpVerifierTestSupport {

  private SignedClientIpVerifierTestSupport() {}

  public static String hmacHex(String hmacSecret, String ip, long timestamp) {
    return SignedClientIpVerifier.hmacHex(hmacSecret, ip, timestamp);
  }
}
