package org.broadinstitute.consent.http.service.showcase;

/**
 * A Data Showcase lookup request that breaks the public contract. The message goes back to the
 * caller as the body of a 400, so it names only the caller's own input and the contract's limits.
 */
public class ShowcaseLookupException extends RuntimeException {

  public ShowcaseLookupException(String message) {
    super(message);
  }
}
