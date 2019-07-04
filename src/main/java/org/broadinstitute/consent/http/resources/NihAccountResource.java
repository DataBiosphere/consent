package org.broadinstitute.consent.http.resources;

import io.dropwizard.auth.Auth;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.NIHUserAccount;
import org.broadinstitute.consent.http.service.NihAuthApi;
import org.broadinstitute.consent.http.service.users.DACUserAPI;

import javax.annotation.security.RolesAllowed;

import javax.ws.rs.Path;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.DELETE;
import javax.ws.rs.core.Response;

@Path("api/nih")
public class NihAccountResource extends Resource {

    private NihAuthApi nihAuthApi;
    private DACUserAPI dacUserAPI;

    public NihAccountResource(NihAuthApi nihAuthApi, DACUserAPI dacUserAPI) {
        this.nihAuthApi = nihAuthApi;
        this.dacUserAPI = dacUserAPI;
    }

    @POST
    @Produces("application/json")
    @RolesAllowed(RESEARCHER)
    public Response registerResearcher(NIHUserAccount nihAccount, @Auth AuthUser authUser) {
        try {
            User user = dacUserAPI.describeDACUserByEmail(authUser.getName());
            return Response.ok(nihAuthApi.authenticateNih(nihAccount, user.getUserId())).build();
        } catch (Exception e){
            return createExceptionResponse(e);
        }
    }

    @DELETE
    @Produces("application/json")
    @RolesAllowed(RESEARCHER)
    public Response deleteNihAccount(@Auth AuthUser authUser) {
        try {
            User user = dacUserAPI.describeDACUserByEmail(authUser.getName());
            nihAuthApi.deleteNihAccountById(user.getUserId());
            return Response.ok().build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }
}
