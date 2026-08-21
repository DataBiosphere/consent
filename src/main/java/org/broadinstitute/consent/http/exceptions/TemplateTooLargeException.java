package org.broadinstitute.consent.http.exceptions;

import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.core.Response.Status;

/** An upload too large to validate: a failed request rather than a validation result. */
public class TemplateTooLargeException extends ClientErrorException {
  public TemplateTooLargeException(String message) {
    super(message, Status.REQUEST_ENTITY_TOO_LARGE);
  }
}
