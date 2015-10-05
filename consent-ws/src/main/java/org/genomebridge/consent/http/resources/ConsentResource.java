package org.genomebridge.consent.http.resources;

import org.genomebridge.consent.http.models.Consent;
import org.genomebridge.consent.http.service.*;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

@Path("{api : (api/)?}consent")
public class ConsentResource extends Resource {

    private final ConsentAPI api;
    private final MatchAPI matchAPI;

    @Path("{id}")
    @GET
    @Produces("application/json")
    public Response describe(@PathParam("id") String id) {
        try {
            return Response.ok(populateFromApi(id))
                    .build();
        } catch (UnknownIdentifierException e) {
            throw new NotFoundException(String.format("Could not find consent with id %s", id));
        }
    }

    @POST
    @Consumes("application/json")
    public Response createConsent(@Context UriInfo info, Consent rec) {
        try {
            Consent consent = api.create(rec);
            URI uri = info.getRequestUriBuilder().path("{id}").build(consent.consentId);
            return Response.created(uri).build();
        } catch (Exception e) {
            return Response.serverError().entity(e).build();
        }
    }

    @Path("{id}")
    @PUT
    @Consumes("application/json")
    @Produces("application/json")
    public Response update(@PathParam("id") String id, Consent updated) {
        try {
            api.update(id, updated);
            return Response.ok(updated).build();
        } catch (UnknownIdentifierException e) {
            throw new NotFoundException(String.format("Could not find consent with id %s to update", id));
        } catch (Exception e) {
            return Response.serverError().entity(e).build();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id}/matches")
    public Response getMatches(@PathParam("id") String purposeId, @Context UriInfo info) {
        try {
            return Response.status(Response.Status.OK).entity(matchAPI.findMatchByConsentId(purposeId)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        }
    }

    private Consent populateFromApi(String id) throws UnknownIdentifierException {
        return api.retrieve(id);
    }

    public ConsentResource() {
        this.api = AbstractConsentAPI.getInstance();
        this.matchAPI = AbstractMatchAPI.getInstance();
    }

}
