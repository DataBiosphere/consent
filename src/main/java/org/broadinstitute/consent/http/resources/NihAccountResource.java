package org.broadinstitute.consent.http.resources;

import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
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

  @POST
  @Produces("application/json")
  @PermitAll
  public Response registerResearcher(NIHUserAccount nihAccount, @Auth AuthUser authUser) {
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
  @Produces("application/json")
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
