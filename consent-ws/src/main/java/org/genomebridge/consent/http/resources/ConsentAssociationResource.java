package org.genomebridge.consent.http.resources;

import com.sun.jersey.api.NotFoundException;
import org.apache.log4j.Logger;
import org.genomebridge.consent.http.service.UnknownIdentifierException;
import org.genomebridge.consent.http.models.ConsentAssociation;
import org.genomebridge.consent.http.service.ConsentAPI;
import com.google.inject.Inject;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.MediaType;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by egolin on 9/15/14.
 */
@Path("consent/{id}/association")
public class ConsentAssociationResource extends Resource {

    private ConsentAPI api;

    public ConsentAssociationResource() {}

    @Inject
    public ConsentAssociationResource(ConsentAPI api) { this.api = api; }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createAssociation(@PathParam("id") String consentId, ArrayList<ConsentAssociation> body) {
        try {
            String msg = String.format("POSTing association to id '%s' with body '%s'", consentId, body.toString());
            logger().info(msg);
            List<ConsentAssociation> result = api.createAssociation(consentId, body);
            return Response.ok(result).build();
        } catch (Exception e) { //catch (UnknownIdentifierException e) {
            throw new NotFoundException(String.format("Could not find consent with id %s to update", consentId));
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateAssociation(@PathParam("id") String consentId, ArrayList<ConsentAssociation> body) {
        try {
            String msg = String.format("PUTing association to id '%s' with body '%s'", consentId, body.toString());
            logger().info(msg);
            List<ConsentAssociation> result = api.updateAssociation(consentId, body);
            return Response.ok(result).build();
        } catch (Exception e) { //catch (UnknownIdentifierException e) {
            throw new NotFoundException(String.format("Could not find consent with id %s to update", consentId));
        }
    }

    @GET
//    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAssociation(@PathParam("id") String consentId, @QueryParam("associationType") String atype, @QueryParam("id") String objectId) {
        try {
            String msg = String.format("GETing association for id '%s' with associationType='%s' and id='%s'", consentId, (atype==null?"<null>":atype), (objectId==null?"<null>":objectId));
            logger().info(msg);
            if (atype==null && objectId!=null)
                return Response.status(Response.Status.BAD_REQUEST).build();
            List<ConsentAssociation> result = api.getAssociation(consentId, atype, objectId);
            return Response.ok(result).build();
        } catch (Exception e) { //catch (UnknownIdentifierException e) {
            throw new NotFoundException(String.format("Could not find consent with id %s to update", consentId));
        }
    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteAssociation(@PathParam("id") String consentId, @QueryParam("associationType") String atype, @QueryParam("id") String objectId) {
        try {
            String msg = String.format("DELETEing association for id '%s' with associationType='%s' and id='%s'", consentId, (atype == null ? "<null>" : atype), (objectId == null ? "<null>" : objectId));
            logger().info(msg);
            if (atype == null && objectId != null)
                return Response.status(Response.Status.BAD_REQUEST).build();
            List<ConsentAssociation> result = api.deleteAssociation(consentId, atype, objectId);
            return Response.ok(result).build();
        } catch (Exception e) { //catch (UnknownIdentifierException e) {
            throw new NotFoundException(String.format("Could not find consent with id %s to update", consentId));
        }
    }

    @Override
    protected Logger logger() {
        return Logger.getLogger("ConsentAssociationResource");
    }
}
