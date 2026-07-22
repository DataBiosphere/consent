package org.broadinstitute.consent.http.mappers;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import org.broadinstitute.consent.http.models.Error;

/**
 * Jersey's {@code RolesAllowedDynamicFeature} throws a {@link ForbiddenException} with no response
 * body when an {@code @RolesAllowed} check fails. Without this mapper, that exception reaches Jetty
 * with no response body, and Jetty's own default error handler fills in a differently-shaped JSON
 * error.
 */
public class ForbiddenExceptionMapper implements ExceptionMapper<ForbiddenException> {

  // The default message jakarta.ws.rs.core.WebApplicationException synthesizes when no message
  // is supplied to the constructor - normalized to the plain reason phrase for clients.
  private static final String GENERIC_MESSAGE =
      "HTTP "
          + Response.Status.FORBIDDEN.getStatusCode()
          + " "
          + Response.Status.FORBIDDEN.getReasonPhrase();

  @Override
  public Response toResponse(ForbiddenException exception) {
    String message = exception.getMessage();
    if (message == null || message.isBlank() || message.equals(GENERIC_MESSAGE)) {
      message = Response.Status.FORBIDDEN.getReasonPhrase();
    }
    Error error = new Error(message, Response.Status.FORBIDDEN.getStatusCode());
    return Response.status(Response.Status.FORBIDDEN).entity(error).build();
  }
}
