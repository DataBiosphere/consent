package org.broadinstitute.consent.http.mappers;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import org.broadinstitute.consent.http.models.Error;

/**
 * Jersey throws a {@link NotFoundException} with no custom message when no resource method matches
 * the requested path; {@code getMessage()} instead returns a generic, JAX-RS-generated "HTTP 404
 * Not Found" string. Without this mapper, that exception reaches Jetty with no response body, and
 * Jetty's own default error handler fills in a differently-shaped JSON error.
 */
public class NotFoundExceptionMapper implements ExceptionMapper<NotFoundException> {

  // The default message jakarta.ws.rs.core.WebApplicationException synthesizes when no message
  // is supplied to the constructor - not useful to a client, so it's replaced with the path.
  private static final String GENERIC_MESSAGE =
      "HTTP "
          + Response.Status.NOT_FOUND.getStatusCode()
          + " "
          + Response.Status.NOT_FOUND.getReasonPhrase();

  @Context UriInfo uriInfo;

  @Override
  public Response toResponse(NotFoundException exception) {
    String message = exception.getMessage();
    if (message == null || message.isBlank() || message.equals(GENERIC_MESSAGE)) {
      message = "Unable to find requested path: " + uriInfo.getRequestUri().getPath();
    }
    Error error = new Error(message, Response.Status.NOT_FOUND.getStatusCode());
    return Response.status(Response.Status.NOT_FOUND)
        .type(MediaType.APPLICATION_JSON)
        .entity(error)
        .build();
  }
}
