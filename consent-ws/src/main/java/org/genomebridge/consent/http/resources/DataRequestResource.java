package org.genomebridge.consent.http.resources;

import java.net.URI;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.genomebridge.consent.http.models.DataRequest;
import org.genomebridge.consent.http.service.AbstractDataRequestAPI;
import org.genomebridge.consent.http.service.DataRequestAPI;

@Path("{api : (api/)?}dataRequest")
public class DataRequestResource extends Resource {

    private final DataRequestAPI api;

    public DataRequestResource() {
        this.api = AbstractDataRequestAPI.getInstance();
    }

    @POST
    @Consumes("application/json")
    public Response createDataRequest(@Context UriInfo info, DataRequest rec) {
        URI uri;
        DataRequest dataRequest;
        try {
            dataRequest = api.createDataRequest(rec);
            uri = info.getRequestUriBuilder().path("{id}").build(dataRequest.getRequestId());
            return Response.created(uri).entity(dataRequest).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    @PUT
    @Consumes("application/json")
    @Produces("application/json")
    @Path("/{id}")
    public Response updateDataRequest(@Context UriInfo info, DataRequest rec, @PathParam("id") Integer id) {
        try {
            DataRequest dataRequest = api.updateDataRequestById(rec, id);
            URI assocURI = buildDataRequestURI(id);
            return Response.ok(dataRequest).location(assocURI).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        }

    }

    @GET
    @Produces("application/json")
    @Path("/{id}")
    public DataRequest describe(@PathParam("id") Integer requestId) {
        return api.describeDataRequest(requestId);
    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id}")
    public Response deleteDataRequest(@PathParam("id") Integer requestId, @Context UriInfo info) {
        api.deleteDataRequest(requestId);
        return Response.ok().entity("DataRequest was deleted").build();
    }

    private URI buildDataRequestURI(Integer id) {
        return UriBuilder.fromResource(DataRequestResource.class).build(id);
    }

}
