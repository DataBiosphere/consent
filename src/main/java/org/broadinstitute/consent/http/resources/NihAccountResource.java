package org.broadinstitute.consent.http.resources;

import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.broadinstitute.consent.http.models.DuosUser;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.service.NihService;

@Path("api/nih")
public class NihAccountResource extends Resource {

  private final NihService nihService;

  @Inject
  public NihAccountResource(NihService nihService) {
    this.nihService = nihService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("sync")
  @PermitAll
  public Response syncAccount(@Auth DuosUser duosUser) {
    try {
      User user = nihService.syncAccount(duosUser);
      return Response.ok(user).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @DELETE
  @Produces(MediaType.APPLICATION_JSON)
  @PermitAll
  public Response deleteNihAccount(@Auth DuosUser duosUser) {
    try {
      nihService.deleteNihAccountById(duosUser);
      return Response.ok().build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }
}
