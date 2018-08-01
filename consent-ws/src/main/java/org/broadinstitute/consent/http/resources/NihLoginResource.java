package org.broadinstitute.consent.http.resources;

import com.rabbitmq.client.ExceptionHandler;
import org.broadinstitute.consent.http.service.NihAuthApi;
import org.broadinstitute.consent.http.service.NihAuthServiceAPI;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.Map;

@Path("{api : (api/)?}nih-login/")

public class NihLoginResource extends Resource {

    NihAuthApi nihAuthApi;

    public NihLoginResource (NihAuthApi nihAuthApi) {
        this.nihAuthApi = nihAuthApi;
    }

    @POST
    @Consumes("application/json")
    @Path("{userId}/{token}")
    @RolesAllowed("RESEARCHER")
    @Produces("application/json")

    public Response registerResearcher(@PathParam("userId") Integer userId, @PathParam("token") String jwt, Map<String, String> properties) {
        try{

            return Response.status(Response.Status.OK).entity(nihAuthApi.authenticateNih(nihAuthApi.generateToken(), userId, properties)).build();
//            return Response.status(Response.Status.OK).entity(nihAuthApi.authenticateNih(jwt, userId, properties)).build();
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
            return Response.ok(nihAuthApi.deleteNihAccountById(userId)).build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }
}
