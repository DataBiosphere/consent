package org.broadinstitute.consent.http.exceptions;

import com.google.api.client.http.HttpStatusCodes;
import jakarta.ws.rs.ClientErrorException;

public class UnprocessableEntityException extends ClientErrorException {

  public UnprocessableEntityException(String message) {
    super(message, HttpStatusCodes.STATUS_CODE_UNPROCESSABLE_ENTITY);
  }

}
