package org.broadinstitute.consent.http.resources;

import org.broadinstitute.consent.http.service.NihAuthApi;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.Map;

@Path("{api : (api/)?}nih-login/")

public class NihAccountResource extends Resource {

    private NihAuthApi nihAuthApi;

    public NihAccountResource(NihAuthApi nihAuthApi) {
        this.nihAuthApi = nihAuthApi;
    }

    @POST
    @Path("{userId}/{token}")
    @Produces("application/json")
    @RolesAllowed("RESEARCHER")

    public Response registerResearcher(@PathParam("userId") Integer userId, @PathParam("token") String jwt) {
        try{
            return Response.status(Response.Status.OK).entity(nihAuthApi.authenticateNih(jwt, userId)).build();
        }catch (Exception e){
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
