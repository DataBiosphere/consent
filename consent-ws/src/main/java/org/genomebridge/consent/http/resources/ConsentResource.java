package org.genomebridge.consent.http.resources;

import org.genomebridge.consent.http.models.Consent;
import org.genomebridge.consent.http.service.AbstractConsentAPI;
import org.genomebridge.consent.http.service.ConsentAPI;
import org.genomebridge.consent.http.service.UnknownIdentifierException;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

@Path("{api : (api/)?}consent")
public class ConsentResource extends Resource {

    private final ConsentAPI api;

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

    private Consent populateFromApi(String id) throws UnknownIdentifierException {
        return api.retrieve(id);
    }

    public ConsentResource() {
        this.api = AbstractConsentAPI.getInstance();
    }

}
