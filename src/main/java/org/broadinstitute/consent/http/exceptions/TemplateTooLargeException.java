package org.broadinstitute.consent.http.exceptions;

/**
 * An upload too large to validate: a failed request, not a validation result. Not a JAX-RS
 * exception — the validator has no HTTP to answer with, and the resource decides the status.
 */
public class TemplateTooLargeException extends RuntimeException {
  public TemplateTooLargeException(String message) {
    super(message);
  }
}
