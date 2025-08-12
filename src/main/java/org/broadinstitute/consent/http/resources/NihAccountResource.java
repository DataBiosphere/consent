package org.broadinstitute.consent.http.resources;

import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.NIHUserAccount;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserProperty;
import org.broadinstitute.consent.http.service.NihService;
import org.broadinstitute.consent.http.service.UserService;

@Path("api/nih")
public class NihAccountResource extends Resource {

  private final NihService nihService;
  private final UserService userService;

  @Inject
  public NihAccountResource(NihService nihService, UserService userService) {
    this.nihService = nihService;
    this.userService = userService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("sync")
  @PermitAll
  public Response syncAccount(@Auth AuthUser authUser) {
    try {
      User user = nihService.syncAccount(authUser);
      return Response.ok(user).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @PermitAll
  public Response registerResearcher(@Auth AuthUser authUser, NIHUserAccount nihAccount) {
    try {
      nihService.validateNihUserAccount(nihAccount, authUser);
      User user = userService.findUserByEmail(authUser.getEmail());
      List<UserProperty> authUserProps = nihService.authenticateNih(nihAccount, authUser,
          user.getUserId());
      return Response.ok(authUserProps).build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @DELETE
  @Produces(MediaType.APPLICATION_JSON)
  @PermitAll
  public Response deleteNihAccount(@Auth AuthUser user) {
    try {
      User dacUser = userService.findUserByEmail(user.getEmail());
      nihService.deleteNihAccountById(dacUser.getUserId());
      return Response.ok().build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }
}
