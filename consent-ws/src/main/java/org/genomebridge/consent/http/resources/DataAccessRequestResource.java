package org.genomebridge.consent.http.resources;

import java.net.URI;
import java.util.ArrayList;
import org.genomebridge.consent.http.service.*;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.List;
import javax.ws.rs.core.MediaType;
import org.bson.Document;

@Path("dar")
public class DataAccessRequestResource extends Resource {

    private DataAccessRequestAPI dataAccessRequestAPI;

    public DataAccessRequestResource(){
        this.dataAccessRequestAPI = AbstractDataAccessRequestAPI.getInstance();
    }

    @POST
    @Consumes("application/json")
    @Produces("application/json")
    public Response createdDataAccessRequest(@Context UriInfo info, Document dar) {
        URI uri;
        Document result;
        try {
            result = dataAccessRequestAPI.createDataAccessRequest(dar);
            uri = info.getRequestUriBuilder().path("{id}").build(result.get("_id"));
            return Response.created(uri).entity(result).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    @GET
    @Produces("application/json")
    public List<Document> describeAllUsers() {
        return dataAccessRequestAPI.describeDataAccessRequests();
    }

    @GET
    @Path("/{id}")
    @Produces("application/json")
    public Document describe(@PathParam("id") String id) {
        return dataAccessRequestAPI.describeDataAccessRequestById(id);
    }

    @GET
    @Path("/search/{partial}")
    @Produces("application/json")
    public Response autocompleteDatasets(@PathParam("partial") String partial) {
        List<String> resp = dataAccessRequestAPI.findDataSets(partial);
        return Response.ok(resp, MediaType.APPLICATION_JSON).build();
    }
    
    @GET
    @Path("/ontology/{partial}")
    @Produces("application/json")
    public Response autocompleteOntology(@PathParam("partial") String partial) {
        List<Document> docs = new ArrayList<>();
        docs.add(new Document("id","OID-1234").append("label", "cancer"));
        docs.add(new Document("id","OID-1235").append("label", "linfoma"));
        docs.add(new Document("id","OID-1236").append("label", "tuberculosis"));
        docs.add(new Document("id","OID-1237").append("label", "alzheimer"));
        docs.add(new Document("id","OID-1238").append("label", "leucemia"));
        return Response.ok(docs, MediaType.APPLICATION_JSON).build();
    }
}
