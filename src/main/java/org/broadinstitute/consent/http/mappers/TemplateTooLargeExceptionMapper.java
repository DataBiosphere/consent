package org.broadinstitute.consent.http.mappers;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import org.broadinstitute.consent.http.exceptions.TemplateTooLargeException;
import org.broadinstitute.consent.http.models.Error;

/**
 * Answers a template the validator refused as too large with 413, when the refusal is not caught in
 * the resource method. A request body stopped while Jersey reads it is answered by {@link
 * RequestTooLargeExceptionMapper}.
 */
public class TemplateTooLargeExceptionMapper implements ExceptionMapper<TemplateTooLargeException> {

  @Override
  public Response toResponse(TemplateTooLargeException exception) {
    return tooLarge(exception.getMessage());
  }

  /** The one 413 a refused upload is answered with, whichever layer decided to refuse. */
  public static Response tooLarge(String message) {
    return Response.status(Response.Status.REQUEST_ENTITY_TOO_LARGE)
        .type(MediaType.APPLICATION_JSON)
        .entity(new Error(message, Response.Status.REQUEST_ENTITY_TOO_LARGE.getStatusCode()))
        .build();
  }
}
