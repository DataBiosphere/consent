package org.broadinstitute.consent.http.resources;

import org.broadinstitute.consent.http.service.users.handler.ResearcherAPI;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

// change endpoint path to a more representative one
@Path("{api : (api/)?}nihlogin")
public class NIHAccountLinkResource extends Resource {
    private ResearcherAPI researcherAPI;
    public NIHAccountLinkResource(ResearcherAPI researcherAPI){
        this.researcherAPI = researcherAPI;
    }

    @GET
    @Path("/{userId}")
    @Produces("application/json")
    public Response getEraStatusByResearcherId(@PathParam("userId") Integer userId) {
        // usar endpoint para traer info del researcher, esto es lo mismo, o solo mapear los valores de era
        try {
            return Response.ok(researcherAPI.describeResearcherPropertiesMap(userId)).build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }

    @POST
    @Path("/{userId}/{eraToken}")
    @Consumes("application/json")
    @RolesAllowed({"RESEARCHER"})
    public Response addEraSubscription(@PathParam("userId") Integer userId, @PathParam("eraToken") String eraToken) {
        // atrapar excepcion cuando esta repetido el token
        try {
            return Response.ok(researcherAPI.insertEraByResearcherId(userId, eraToken)).build();
        } catch(Exception e) {
            return createExceptionResponse(e);
        }

    }

    @PUT
    @Path("/{userId}/{eraToken}")
    @Consumes("application/json")
    @RolesAllowed("RESEARCHER")
    public Response updateEraSubscription(@PathParam("userId") Integer userId, String eraToken) {
        try{
            return Response.ok(researcherAPI.updateEraByResearcherId(userId, eraToken, true)).build();
        }catch (Exception e){
            return createExceptionResponse(e);
        }
    }
}
