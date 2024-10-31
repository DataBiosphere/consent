package org.broadinstitute.consent.http.exceptions;

import jakarta.ws.rs.ServerErrorException;
import jakarta.ws.rs.core.Response.Status;

public class UnknownDraftTypeException extends ServerErrorException {
  public UnknownDraftTypeException(String message) {
    super(message, Status.INTERNAL_SERVER_ERROR);
  }
}
