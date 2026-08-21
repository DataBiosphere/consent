package org.broadinstitute.consent.http.exceptions;

/**
 * An upload too large to validate: a failed request rather than a validation result. Deliberately
 * not a JAX-RS exception — the validator that raises it has no HTTP to answer with, and the
 * resource that catches it decides the status.
 */
public class TemplateTooLargeException extends RuntimeException {
  public TemplateTooLargeException(String message) {
    super(message);
  }
}
