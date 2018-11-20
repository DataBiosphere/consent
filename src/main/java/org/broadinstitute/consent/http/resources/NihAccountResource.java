package org.broadinstitute.consent.http.resources;

import org.broadinstitute.consent.http.models.NIHUserAccount;
import org.broadinstitute.consent.http.service.NihAuthApi;

import javax.annotation.security.RolesAllowed;

import javax.ws.rs.Path;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.PathParam;
import javax.ws.rs.DELETE;
import javax.ws.rs.core.Response;

@Path("api/nih/")
public class NihAccountResource extends Resource {

    private NihAuthApi nihAuthApi;

    public NihAccountResource(NihAuthApi nihAuthApi) {
        this.nihAuthApi = nihAuthApi;
    }

    @POST
    @Path("{userId}")
    @Produces("application/json")
    @RolesAllowed("RESEARCHER")
    public Response registerResearcher(@PathParam("userId") Integer userId, NIHUserAccount nihAccount) {
        try {
            return Response.ok(nihAuthApi.authenticateNih(nihAccount, userId)).build();
        } catch (Exception e){
            return createExceptionResponse(e);
        }
    }

    @DELETE
    @Path("{userId}")
    @Produces("application/json")
    @RolesAllowed("RESEARCHER")
    public Response deleteNihAccount(@PathParam("userId") Integer userId) {
        try {
            nihAuthApi.deleteNihAccountById(userId);
            return Response.ok().build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }
}
