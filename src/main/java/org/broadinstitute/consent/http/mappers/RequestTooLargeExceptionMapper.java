package org.broadinstitute.consent.http.mappers;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import org.broadinstitute.consent.http.exceptions.RequestTooLargeException;

/**
 * Answers a request body stopped at its size limit with 413. The bounded entity stream throws while
 * Jersey is still reading the body, so the refusal never reaches the resource's own catch; Jersey
 * passes the {@link java.io.IOException} to the exception mappers, and without this one the client
 * would get a 500.
 */
public class RequestTooLargeExceptionMapper implements ExceptionMapper<RequestTooLargeException> {

  @Override
  public Response toResponse(RequestTooLargeException exception) {
    return TemplateTooLargeExceptionMapper.tooLarge(exception.getMessage());
  }
}
