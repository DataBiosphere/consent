package org.broadinstitute.consent.http.mappers;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import org.broadinstitute.consent.http.exceptions.TemplateTooLargeException;
import org.broadinstitute.consent.http.models.Error;

/**
 * Answers a refused upload with 413 wherever the refusal is raised: from the validator, inside the
 * resource method, or from the bounded entity stream a request filter installs, which throws while
 * Jersey is still reading the body and so never reaches the resource's own catch.
 */
public class TemplateTooLargeExceptionMapper implements ExceptionMapper<TemplateTooLargeException> {

  @Override
  public Response toResponse(TemplateTooLargeException exception) {
    return tooLarge(exception.getMessage());
  }

  /** The one 413 the template uploads answer with, whichever layer decided to refuse. */
  public static Response tooLarge(String message) {
    return Response.status(Response.Status.REQUEST_ENTITY_TOO_LARGE)
        .type(MediaType.APPLICATION_JSON)
        .entity(new Error(message, Response.Status.REQUEST_ENTITY_TOO_LARGE.getStatusCode()))
        .build();
  }
}
