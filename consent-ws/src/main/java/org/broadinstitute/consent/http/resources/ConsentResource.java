package org.broadinstitute.consent.http.resources;

import org.broadinstitute.consent.http.service.UnknownIdentifierException;
import org.broadinstitute.consent.http.service.AbstractMatchProcessAPI;
import org.broadinstitute.consent.http.service.ConsentAPI;
import org.broadinstitute.consent.http.service.MatchAPI;
import org.broadinstitute.consent.http.service.MatchProcessAPI;
import org.broadinstitute.consent.http.service.AbstractTranslateServiceAPI;
import org.broadinstitute.consent.http.service.AbstractMatchAPI;
import org.broadinstitute.consent.http.service.AbstractConsentAPI;
import org.broadinstitute.consent.http.service.TranslateServiceAPI;
import org.broadinstitute.consent.http.enumeration.TranslateType;
import org.broadinstitute.consent.http.models.Consent;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.net.URI;

@Path("{api : (api/)?}consent")
public class ConsentResource extends Resource {

    private final ConsentAPI api;
    private final MatchProcessAPI matchProcessAPI;
    private final MatchAPI matchAPI;
    private final TranslateServiceAPI translateServiceAPI = AbstractTranslateServiceAPI.getInstance();


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
            rec.setTranslatedUseRestriction(translateServiceAPI.translate(TranslateType.SAMPLESET.getValue(),rec.getUseRestriction()));
            Consent consent = api.create(rec);
            URI uri = info.getRequestUriBuilder().path("{id}").build(consent.consentId);
            matchProcessAPI.processMatchesForConsent(consent.consentId);
            return Response.created(uri).build();
        }  catch (IOException ie) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        catch (Exception e){
            return Response.serverError().entity(e).build();
        }
    }

    @Path("{id}")
    @PUT
    @Consumes("application/json")
    @Produces("application/json")
    public Response update(@PathParam("id") String id, Consent updated) {
        try {
            updated.setTranslatedUseRestriction(translateServiceAPI.translate(TranslateType.SAMPLESET.getValue(),updated.getUseRestriction()));
            api.update(id, updated);
            matchProcessAPI.processMatchesForConsent(id);
            return Response.ok(updated).build();
        } catch (UnknownIdentifierException e) {
            throw new NotFoundException(String.format("Could not find consent with id %s to update", id));
        } catch (IOException ie) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        } catch (Exception e) {
            return Response.serverError().entity(e).build();
        }
    }


    @DELETE
    @Produces("application/json")
    @Path("{id}")
    public Response delete(@PathParam("id") String consentId) {
        try {
            api.delete(consentId);
            return Response.ok().build();
        }catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        } catch (Exception e) {
            return Response.serverError().entity(e.getMessage()).build();
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
        this.matchProcessAPI = AbstractMatchProcessAPI.getInstance();
        this.matchAPI = AbstractMatchAPI.getInstance();
    }

}
