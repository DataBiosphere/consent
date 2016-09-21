package org.broadinstitute.consent.http.resources;

import org.broadinstitute.consent.http.service.users.handler.ResearcherAPI;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.Map;


@Path("{api : (api/)?}researcher/{userId}")
public class ResearcherResource extends Resource{

    private ResearcherAPI researcherAPI;

    public ResearcherResource(ResearcherAPI researcherAPI){
        this.researcherAPI = researcherAPI;
    }

    @POST
    @Consumes("application/json")
    @RolesAllowed("RESEARCHER")
    public Response registerResearcher(@QueryParam("validate") Boolean validate, @Context UriInfo info, @PathParam("userId") Integer userId, Map<String,String> researcherPropertiesMap) {
        try{
            researcherAPI.registerResearcher(researcherPropertiesMap, userId, validate);
            return Response.created(info.getRequestUriBuilder().build()).build();
        }catch (Exception e){
            return createExceptionResponse(e);
        }
    }

    @PUT
    @Consumes("application/json")
    @RolesAllowed("RESEARCHER")
    public Response updateResearcher(@QueryParam("validate") Boolean validate, @PathParam("userId") Integer userId, Map<String, String> researcherProperties) {
        try{
            return Response.ok(researcherAPI.updateResearcher(researcherProperties, userId, validate)).build();
        }catch (Exception e){
            return createExceptionResponse(e);
        }
    }

    @GET
    @Produces("application/json")
    @RolesAllowed({"ADMIN","RESEARCHER"})
    public Response describeAllResearcherProperties(@PathParam("userId") Integer userId) {
        try{
            return Response.ok(researcherAPI.describeResearcherPropertiesMap(userId)).build();
        }catch (Exception e){
            return createExceptionResponse(e);
        }
    }

    @DELETE
    @Produces("application/json")
    @RolesAllowed("ADMIN")
    public Response deleteAllProperties(@PathParam("userId") Integer userId) {
        try{
            researcherAPI.deleteResearcherProperties(userId);
            return Response.ok().build();
        }catch (Exception e){
            return createExceptionResponse(e);
        }
    }

    @GET
    @Path("/dar")
    @Produces("application/json")
    @RolesAllowed({"ADMIN","RESEARCHER"})
    public Response getResearcherPropertiesForDAR(@PathParam("userId") Integer userId) {
        try{
            return Response.ok(researcherAPI.describeResearcherPropertiesForDAR(userId)).build();
        }catch (Exception e){
            return createExceptionResponse(e);
        }
    }

}
