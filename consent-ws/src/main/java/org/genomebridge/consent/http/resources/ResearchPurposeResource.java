package org.genomebridge.consent.http.resources;

import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.genomebridge.consent.http.models.ResearchPurpose;
import org.genomebridge.consent.http.service.AbstractResearchPurposeAPI;
import org.genomebridge.consent.http.service.ResearchPurposeAPI;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.Status;
import java.io.IOException;
import java.net.URI;
import java.util.List;

@Path("purpose")
public class ResearchPurposeResource extends Resource {

    private ResearchPurposeAPI api;

    public ResearchPurposeResource() {
        this.api = AbstractResearchPurposeAPI.getInstance();
    }

    @POST
    @Consumes("application/json")
    public Response createPurpose(@Context UriInfo info, ResearchPurpose rec) {
        URI uri;
        try {
            Document purpose = api.createResearchPurpose(rec);
            uri = info.getRequestUriBuilder().path("{id}").build(purpose.get("_id").toString());
            return Response.created(uri).entity(purpose).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    @PUT
    @Consumes("application/json")
    @Produces("application/json")
    @Path("/{id}")
    public Response updatePurpose(@Context UriInfo info, ResearchPurpose rec, @PathParam("id") String id) {
        try {
            Document purpose = api.updateResearchPurpose(rec, id);
            URI assocURI = UriBuilder.fromResource(ResearchPurposeResource.class).build(id);
            return Response.ok(purpose).location(assocURI).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        }

    }

    @GET
    @Produces("application/json")
    @Path("/{id}")
    public Response describePurpose(@PathParam("id") String purposeId) throws IOException{
        try {
            return Response.status(Response.Status.OK).entity(api.describeResearchPurpose(purposeId)).build();
        } catch (NotFoundException e) {
            return Response.status(Status.NOT_FOUND).entity(e.getMessage()).build();
        }
    }

    @GET
    @Produces("application/json")
    public Response describePurposes(@QueryParam("ids") String ids) {
        try {
            if(StringUtils.isEmpty(ids)){
                return Response.status(Status.BAD_REQUEST).entity("Parameter ids is required").build();
            }
            return Response.status(Response.Status.OK).entity(api.describeResearchPurposes(ids.split(","))).build();
        } catch (NotFoundException e) {
            return Response.status(Status.NOT_FOUND).entity(e.getMessage()).build();
        }
    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id}")
    public Response deletePurpose(@PathParam("id") String requestId, @Context UriInfo info) {
        try {
            api.deleteResearchPurpose(requestId);
            return Response.status(Response.Status.OK).entity("Research Purpose was deleted").build();
        } catch (NotFoundException e) {
            return Response.status(Status.NOT_FOUND).entity(e.getMessage()).build();
        }
    }



}
