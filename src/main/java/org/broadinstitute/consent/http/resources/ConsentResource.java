package org.broadinstitute.consent.http.resources;

import com.google.api.client.http.HttpStatusCodes;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.broadinstitute.consent.http.models.AuthUser;

@Path("api/consent")
public class ConsentResource extends Resource {

  @Inject
  public ConsentResource() {
  }

  @Deprecated
  @Path("{id}")
  @GET
  @Produces("application/json")
  @PermitAll
  public Response describe(@PathParam("id") String id) {
    return Response.status(HttpStatusCodes.STATUS_CODE_NOT_FOUND).build();
  }

  @Deprecated
  @POST
  @Consumes("application/json")
  @RolesAllowed({ADMIN, RESEARCHER, DATAOWNER})
  public Response createConsent(@Context UriInfo info, String rec, @Auth AuthUser user) {
    return Response.status(HttpStatusCodes.STATUS_CODE_NOT_FOUND).build();
  }

  @Deprecated
  @Path("{id}")
  @PUT
  @Consumes("application/json")
  @Produces("application/json")
  @RolesAllowed({ADMIN, RESEARCHER, DATAOWNER})
  public Response update(@PathParam("id") String id, String updated, @Auth AuthUser user) {
    return Response.status(HttpStatusCodes.STATUS_CODE_NOT_FOUND).build();
  }


  @Deprecated
  @DELETE
  @Produces("application/json")
  @Path("{id}")
  @RolesAllowed(ADMIN)
  public Response delete(@PathParam("id") String consentId) {
    return Response.status(HttpStatusCodes.STATUS_CODE_NOT_FOUND).build();
  }


  @Deprecated
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/{id}/matches")
  @PermitAll
  public Response getMatches(@PathParam("id") String purposeId, @Context UriInfo info) {
    return Response.status(HttpStatusCodes.STATUS_CODE_NOT_FOUND).build();
  }


  @Deprecated
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @PermitAll
  public Response getByName(@QueryParam("name") String name, @Context UriInfo info) {
    return Response.status(HttpStatusCodes.STATUS_CODE_NOT_FOUND).build();
  }

}
