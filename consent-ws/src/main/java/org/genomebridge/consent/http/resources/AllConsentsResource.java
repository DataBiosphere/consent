package org.genomebridge.consent.http.resources;

import org.genomebridge.consent.http.models.Consent;
import org.genomebridge.consent.http.service.AbstractConsentAPI;
import org.genomebridge.consent.http.service.ConsentAPI;
import org.genomebridge.consent.http.service.DuplicateIdentifierException;

import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.UUID;

@Path("consent")
public class AllConsentsResource extends Resource {

    private ConsentAPI api;

    public AllConsentsResource() { this.api = AbstractConsentAPI.getInstance(); }

    @PUT
    @Consumes("application/json")
    public Response createConsent(@Context UriInfo info, Consent rec) {
        URI uri = null;
        do {
            String newId = UUID.randomUUID().toString();
            try {
                api.create(newId, rec);
                uri = info.getRequestUriBuilder().path("{id}").build(newId);
            } catch (DuplicateIdentifierException ignored) {
                // since user is not passing in id, generate a new ID and try again
            }
        } while (uri == null);
        return Response.created(uri).build();
    }
}
