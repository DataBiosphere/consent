package org.broadinstitute.consent.http.resources;

import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserProperty;
import org.broadinstitute.consent.http.models.NIHUserAccount;
import org.broadinstitute.consent.http.service.NihService;
import org.broadinstitute.consent.http.service.UserService;

import java.util.List;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

@Path("api/nih")
public class NihAccountResource extends Resource {

    private NihService nihService;
    private UserService userService;

    @Inject
    public NihAccountResource(NihService nihService, UserService userService) {
        this.nihService = nihService;
        this.userService = userService;
    }

    @POST
    @Produces("application/json")
    @RolesAllowed(RESEARCHER)
    public Response registerResearcher(NIHUserAccount nihAccount, @Auth AuthUser user) {
        try {
            User u = userService.findUserByEmail(user.getName());
            List<UserProperty> authUserProps = nihService.authenticateNih(nihAccount, user, u.getDacUserId());
            return Response.ok(authUserProps).build();
        } catch (Exception e){
            return createExceptionResponse(e);
        }
    }

    @DELETE
    @Produces("application/json")
    @RolesAllowed(RESEARCHER)
    public Response deleteNihAccount(@Auth AuthUser user) {
        try {
            User dacUser = userService.findUserByEmail(user.getName());
            nihService.deleteNihAccountById(dacUser.getDacUserId());
            return Response.ok().build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }
}
