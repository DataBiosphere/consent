package org.broadinstitute.consent.http.resources;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import org.broadinstitute.consent.http.models.Error;
import org.eclipse.jetty.server.Request;

@Path("error")
public class ErrorResource {

  @GET
  @Path("404")
  @Produces("application/json")
  public Response notFound(@Context HttpServletRequest request) {
    String originalUri = ((Request) request).getOriginalURI();
    String decodedUri = URLDecoder.decode(originalUri, Charset.defaultCharset());
    String msg = String.format("Unable to find requested path: '%s'", decodedUri);
    Error error = new Error(msg, 404);
    return Response.status(error.code()).entity(error).build();
  }
}
