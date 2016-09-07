package org.broadinstitute.consent.http.resources;

import org.broadinstitute.consent.http.models.dto.Error;
import org.broadinstitute.consent.http.service.users.handler.ResearcherAPI;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
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
public class ResearcherResource {

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
        }catch (IllegalArgumentException e){
            return Response.status(Response.Status.BAD_REQUEST).entity(new Error(e.getMessage(), Response.Status.BAD_REQUEST.getStatusCode())).build();
        }catch (NotFoundException e){
            return Response.status(Response.Status.NOT_FOUND).entity(new Error(e.getMessage(), Response.Status.NOT_FOUND.getStatusCode())).build();
        }catch (UnsupportedOperationException e){
            return Response.status(Response.Status.CONFLICT).entity(new Error(e.getMessage(), Response.Status.CONFLICT.getStatusCode())).build();
        }catch (Exception e){
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new Error(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())).build();
        }
    }

    @PUT
    @Consumes("application/json")
    @RolesAllowed("RESEARCHER")
    public Response updateResearcher(@QueryParam("validate") Boolean validate, @PathParam("userId") Integer userId, Map<String, String> researcherProperties) {
        try{
            return Response.ok(researcherAPI.updateResearcher(researcherProperties, userId, validate)).build();
        }catch (IllegalArgumentException e){
            return Response.status(Response.Status.BAD_REQUEST).entity(new Error(e.getMessage(), Response.Status.BAD_REQUEST.getStatusCode())).build();
        }catch (NotFoundException e){
            return Response.status(Response.Status.NOT_FOUND).entity(new Error(e.getMessage(), Response.Status.NOT_FOUND.getStatusCode())).build();
        }catch (Exception e){
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new Error(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())).build();
        }
    }

    @GET
    @Produces("application/json")
    @RolesAllowed({"ADMIN","RESEARCHER"})
    public Response describeAllResearcherProperties(@PathParam("userId") Integer userId) {
        try{
            return Response.ok(researcherAPI.describeResearcherPropertiesMap(userId)).build();
        }catch (NotFoundException e){
            return Response.status(Response.Status.NOT_FOUND).entity(new Error(e.getMessage(), Response.Status.NOT_FOUND.getStatusCode())).build();
        }
    }

    @DELETE
    @Produces("application/json")
    @RolesAllowed("ADMIN")
    public Response deleteAllProperties(@PathParam("userId") Integer userId) {
        try{
            researcherAPI.deleteResearcherProperties(userId);
            return Response.ok().build();
        }catch (NotFoundException e){
            return Response.status(Response.Status.NOT_FOUND).entity(new Error(e.getMessage(), Response.Status.NOT_FOUND.getStatusCode())).build();
        }
    }

    @GET
    @Path("/dar")
    @Produces("application/json")
    @RolesAllowed({"ADMIN","RESEARCHER"})
    public Response getResearcherPropertiesForDAR(@PathParam("userId") Integer userId) {
        try{
            return Response.ok(researcherAPI.describeResearcherPropertiesForDAR(userId)).build();
        }catch (NotFoundException e){
            return Response.status(Response.Status.NOT_FOUND).entity(new Error(e.getMessage(), Response.Status.NOT_FOUND.getStatusCode())).build();
        }
    }

}
