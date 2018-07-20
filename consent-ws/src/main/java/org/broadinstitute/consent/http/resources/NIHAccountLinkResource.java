package org.broadinstitute.consent.http.resources;

import org.broadinstitute.consent.http.service.users.handler.ResearcherAPI;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

// change endpoint path to a more representative one
@Path("{api : (api/)?}nihlogin")
public class NIHAccountLinkResource extends Resource {
    private ResearcherAPI researcherAPI;
    public NIHAccountLinkResource(ResearcherAPI researcherAPI){
        this.researcherAPI = researcherAPI;
    }

//    @GET
//    @Path("/{userId}")
//    @Produces("application/json")
//    public Response getEraStatusByResearcherId(@PathParam("userId") Integer userId) {
////        Map<String,String> researcherProperties = new HashMap<>();
//
//        System.out.println("USERID");
//        System.out.println(userId);
//
//    }

    @POST
    @Path("/{userId}/{eraToken}")
    @Consumes("application/json")
    @RolesAllowed({"RESEARCHER"})
    public Response addEraSubscription(@PathParam("userId") Integer userId, @PathParam("eraToken") String eraToken) {
        try {
            return Response.ok(researcherAPI.updateEraByResearcherId(userId, eraToken)).build();
        } catch(Exception e) {
            return createExceptionResponse(e);
        }

    }
}
