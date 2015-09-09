package org.genomebridge.consent.http.resources;

import org.genomebridge.consent.http.models.ResearchPurpose;
import org.genomebridge.consent.http.service.AbstractResearchPurposeAPI;
import org.genomebridge.consent.http.service.ResearchPurposeAPI;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.Status;
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
            ResearchPurpose purpose = api.createResearchPurpose(rec);
            uri = info.getRequestUriBuilder().path("{id}").build(purpose.getPurposeId());
            return Response.created(uri).entity(purpose).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    @PUT
    @Consumes("application/json")
    @Produces("application/json")
    @Path("/{id}")
    public Response updatePurpose(@Context UriInfo info, ResearchPurpose rec, @PathParam("id") Integer id) {
        try {
            ResearchPurpose purpose = api.updateResearchPurpose(rec, id);
            URI assocURI = UriBuilder.fromResource(ResearchPurposeResource.class).build(id);
            return Response.ok(purpose).location(assocURI).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        }

    }

    @GET
    @Produces("application/json")
    @Path("/{id}")
    public ResearchPurpose describePurpose(@PathParam("id") Integer purposeId) {
        return api.describeResearchPurpose(purposeId);
    }

    @GET
    @Produces("application/json")
    public List<ResearchPurpose> describePurposes(@QueryParam("ids") List<Integer> ids) {
        return api.describeResearchPurposes(ids);
    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id}")
    public Response deletePurpose(@PathParam("id") Integer requestId, @Context UriInfo info) {
        api.deleteResearchPurpose(requestId);
        return Response.ok().entity("Research purpose was deleted").build();
    }



}
