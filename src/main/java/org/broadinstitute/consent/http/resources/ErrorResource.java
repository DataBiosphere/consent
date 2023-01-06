package org.broadinstitute.consent.http.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.broadinstitute.consent.http.models.dto.Error;

@Path("error")
public class ErrorResource {

  @GET
  @Path("404")
  @Produces("application/json")
  public Response notFound(@Context UriInfo info) {
    String msg = String.format("Unable to find requested path: '%s'", info.getPath());
    Error error = new Error(msg, 404);
    return Response.status(error.getCode()).entity(error).build();
  }
}
