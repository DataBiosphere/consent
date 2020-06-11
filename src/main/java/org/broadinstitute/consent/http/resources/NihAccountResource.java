package org.broadinstitute.consent.http.resources;

import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.NIHUserAccount;
import org.broadinstitute.consent.http.service.NihAuthApi;
import org.broadinstitute.consent.http.service.UserService;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

@Path("api/nih")
public class NihAccountResource extends Resource {

    private NihAuthApi nihAuthApi;
    private UserService userService;

    @Inject
    public NihAccountResource(NihAuthApi nihAuthApi, UserService userService) {
        this.nihAuthApi = nihAuthApi;
        this.userService = userService;
    }

    @POST
    @Produces("application/json")
    @RolesAllowed(RESEARCHER)
    public Response registerResearcher(NIHUserAccount nihAccount, @Auth AuthUser user) {
        try {
            userService.findUserByEmail(user.getName());
            return Response.ok(nihAuthApi.authenticateNih(nihAccount, user)).build();
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
            nihAuthApi.deleteNihAccountById(dacUser.getDacUserId());
            return Response.ok().build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }
}
