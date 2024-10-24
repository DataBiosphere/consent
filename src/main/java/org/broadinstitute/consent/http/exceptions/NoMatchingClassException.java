package org.broadinstitute.consent.http.exceptions;

import jakarta.ws.rs.ServerErrorException;
import jakarta.ws.rs.core.Response.Status;

public class NoMatchingClassException extends ServerErrorException {
  public NoMatchingClassException(String message) {
    super(Status.INTERNAL_SERVER_ERROR);
  }
}
