package org.broadinstitute.consent.http.service.showcase;

/**
 * A Data Showcase lookup request that breaks the public contract. The message goes back to the
 * caller as the body of a 400, so it names only the caller's own input and the contract's limits.
 */
public class ShowcaseLookupException extends RuntimeException {

  /** Echoed input is cut to this many code points, so a 400 never repeats a large request. */
  static final int MAX_ECHOED_LENGTH = 64;

  public ShowcaseLookupException(String message) {
    super(message);
  }

  /** The caller's value for a message, cut to {@link #MAX_ECHOED_LENGTH} code points. */
  static String echoed(String value) {
    if (value.codePointCount(0, value.length()) <= MAX_ECHOED_LENGTH) {
      return value;
    }
    return value.substring(0, value.offsetByCodePoints(0, MAX_ECHOED_LENGTH)) + "…";
  }
}
