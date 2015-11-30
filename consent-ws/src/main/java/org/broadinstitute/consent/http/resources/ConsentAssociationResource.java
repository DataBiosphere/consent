package org.broadinstitute.consent.http.resources;

import org.apache.log4j.Logger;
import org.broadinstitute.consent.http.models.ConsentAssociation;
import org.broadinstitute.consent.http.service.AbstractConsentAPI;
import org.broadinstitute.consent.http.service.AbstractMatchProcessAPI;
import org.broadinstitute.consent.http.service.ConsentAPI;
import org.broadinstitute.consent.http.service.MatchProcessAPI;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Path("{api : (api/)?}consent/{id}/association")
public class ConsentAssociationResource extends Resource {

    private final ConsentAPI api;
    private final MatchProcessAPI matchProcessAPI;

    public ConsentAssociationResource() {
        this.api = AbstractConsentAPI.getInstance();
        this.matchProcessAPI = AbstractMatchProcessAPI.getInstance();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createAssociation(@PathParam("id") String consentId, ArrayList<ConsentAssociation> body) {
        try {
            String msg = String.format("POSTing association to id '%s' with body '%s'", consentId, body.toString());
            logger().debug(msg);
            List<ConsentAssociation> result = api.createAssociation(consentId, body);
            URI assocURI = buildConsentAssociationURI(consentId);
            matchProcessAPI.processMatchesForConsent(consentId);
            return Response.ok(result).location(assocURI).build();
        } catch (Exception e) { //catch (UnknownIdentifierException e) {
            throw new NotFoundException(String.format("Could not find consent with id %s", consentId));
        }
    }

    private URI buildConsentAssociationURI(String id) {
        return UriBuilder.fromResource(ConsentAssociationResource.class).build("api/", id);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateAssociation(@PathParam("id") String consentId, ArrayList<ConsentAssociation> body) {
        try {
            String msg = String.format("PUTing association to id '%s' with body '%s'", consentId, body.toString());
            logger().debug(msg);
            List<ConsentAssociation> result = api.updateAssociation(consentId, body);
            URI assocURI = buildConsentAssociationURI(consentId);
            matchProcessAPI.processMatchesForConsent(consentId);
            return Response.ok(result).location(assocURI).build();
        } catch (Exception e) { //catch (UnknownIdentifierException e) {
            throw new NotFoundException(String.format("Could not find consent with id %s", consentId));
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAssociation(@PathParam("id") String consentId, @QueryParam("associationType") String atype, @QueryParam("id") String objectId) {
        try {
            String msg = String.format("GETing association for id '%s' with associationType='%s' and id='%s'", consentId, (atype == null ? "<null>" : atype), (objectId == null ? "<null>" : objectId));
            logger().debug(msg);
            if (atype == null && objectId != null)
                return Response.status(Response.Status.BAD_REQUEST).build();
            List<ConsentAssociation> result = api.getAssociation(consentId, atype, objectId);
            URI assocURI = buildConsentAssociationURI(consentId);
            return Response.ok(result).location(assocURI).build();
        } catch (Exception e) { //catch (UnknownIdentifierException e) {
            throw new NotFoundException(String.format("Could not find consent with id %s", consentId));
        }
    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteAssociation(@PathParam("id") String consentId, @QueryParam("associationType") String atype, @QueryParam("id") String objectId) {
        try {
            String msg = String.format("DELETEing association for id '%s' with associationType='%s' and id='%s'", consentId, (atype == null ? "<null>" : atype), (objectId == null ? "<null>" : objectId));
            logger().debug(msg);
            if (atype == null && objectId != null)
                return Response.status(Response.Status.BAD_REQUEST).build();
            List<ConsentAssociation> result = api.deleteAssociation(consentId, atype, objectId);
            URI assocURI = buildConsentAssociationURI(consentId);
            matchProcessAPI.processMatchesForConsent(consentId);
            return Response.ok(result).location(assocURI).build();
        } catch (Exception e) { //catch (UnknownIdentifierException e) {
            throw new NotFoundException(String.format("Could not find consent with id %s", consentId));
        }
    }

    @Override
    protected Logger logger() {
        return Logger.getLogger("ConsentAssociationResource");
    }
}
