package org.genomebridge.consent.http.resources;

import org.genomebridge.consent.http.models.Consent;
import org.genomebridge.consent.http.service.AbstractConsentAPI;
import org.genomebridge.consent.http.service.ConsentAPI;

import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

@Path("consent")
public class AllConsentsResource extends Resource {

    private ConsentAPI api;

    public AllConsentsResource() {
        this.api = AbstractConsentAPI.getInstance();
    }

    @PUT
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
}
