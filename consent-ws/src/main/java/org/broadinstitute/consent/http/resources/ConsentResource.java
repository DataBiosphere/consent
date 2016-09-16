package org.broadinstitute.consent.http.resources;

import com.google.gson.Gson;
import java.net.URI;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.broadinstitute.consent.http.enumeration.TranslateType;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.dto.Error;
import org.broadinstitute.consent.http.service.AbstractConsentAPI;
import org.broadinstitute.consent.http.service.AbstractMatchAPI;
import org.broadinstitute.consent.http.service.AbstractMatchProcessAPI;
import org.broadinstitute.consent.http.service.AbstractTranslateServiceAPI;
import org.broadinstitute.consent.http.service.ConsentAPI;
import org.broadinstitute.consent.http.service.MatchAPI;
import org.broadinstitute.consent.http.service.MatchProcessAPI;
import org.broadinstitute.consent.http.service.TranslateServiceAPI;
import org.broadinstitute.consent.http.service.UnknownIdentifierException;
import org.broadinstitute.consent.http.service.validate.AbstractUseRestrictionValidatorAPI;
import org.broadinstitute.consent.http.service.validate.UseRestrictionValidatorAPI;

@Path("{auth: (basic/|api/)?}consent")
public class ConsentResource extends Resource {

    private final ConsentAPI api;
    private final MatchProcessAPI matchProcessAPI;
    private final MatchAPI matchAPI;
    private final TranslateServiceAPI translateServiceAPI = AbstractTranslateServiceAPI.getInstance();
    private final UseRestrictionValidatorAPI useRestrictionValidatorAPI;


    @Path("{id}")
    @GET
    @Produces("application/json")
    @PermitAll
    public Response describe(@PathParam("id") String id) {
        try {
            return Response.ok(populateFromApi(id))
                    .build();
        } catch (UnknownIdentifierException e) {
            return Response.status(Response.Status.NOT_FOUND).entity(new Error(String.format("Could not find consent with id %s", id), Response.Status.NOT_FOUND.getStatusCode())).build();
        }
    }

    @Path("invalid")
    @GET
    @Produces("application/json")
    @RolesAllowed({"ADMIN"})
    public Response describeInvalidConsents() {
        try {
            return Response.ok(api.getInvalidConsents()).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new Error(e.getMessage().toString(), Response.Status.NOT_FOUND.getStatusCode())).build();
        }
    }

    @POST
    @Consumes("application/json")
    @RolesAllowed("ADMIN")
    public Response createConsent(@Context UriInfo info, Consent rec) {
        try {
            if (rec.getTranslatedUseRestriction() == null) {
                rec.setTranslatedUseRestriction(translateServiceAPI.translate(TranslateType.SAMPLESET.getValue(),rec.getUseRestriction()));
            }
            if(rec.getUseRestriction() != null){
                useRestrictionValidatorAPI.validateUseRestriction(new Gson().toJson(rec.getUseRestriction()));
            }
            Consent consent = api.create(rec);
            URI uri = info.getRequestUriBuilder().path("{id}").build(consent.consentId);
            matchProcessAPI.processMatchesForConsent(consent.consentId);
            return Response.created(uri).build();
        }  catch (Exception e) {
            return createExceptionResponse(e);
        }
    }

    @Path("{id}")
    @PUT
    @Consumes("application/json")
    @Produces("application/json")
    @RolesAllowed("ADMIN")
    public Response update(@PathParam("id") String id, Consent updated) {
        try {
            if (updated.getTranslatedUseRestriction() == null) {
                updated.setTranslatedUseRestriction(translateServiceAPI.translate(TranslateType.SAMPLESET.getValue(),updated.getUseRestriction()));
            }
            if(updated.getUseRestriction() != null) {
                useRestrictionValidatorAPI.validateUseRestriction(new Gson().toJson(updated.getUseRestriction()));
            }
            updated = api.update(id, updated);
            matchProcessAPI.processMatchesForConsent(id);
            return Response.ok(updated).build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }


    @DELETE
    @Produces("application/json")
    @Path("{id}")
    @RolesAllowed("ADMIN")
    public Response delete(@PathParam("id") String consentId) {
        try {
            api.delete(consentId);
            return Response.ok().build();
        }
        catch (Exception e) {
            return createExceptionResponse(e);
        }
    }


    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id}/matches")
    @PermitAll
    public Response getMatches(@PathParam("id") String purposeId, @Context UriInfo info) {
        try {
            return Response.status(Response.Status.OK).entity(matchAPI.findMatchByConsentId(purposeId)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.NOT_FOUND).entity(new Error(e.getMessage(), Response.Status.NOT_FOUND.getStatusCode())).build();
        }
    }


    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @PermitAll
    public Response getByName(@QueryParam("name") String name, @Context UriInfo info) {
        String id;
        try {
            id = api.getByName(name);
            if (id == null) {
                return Response.status(Response.Status.NOT_FOUND).entity(new Error(String.format("Requested name = %s not found on consents", name), Response.Status.NOT_FOUND.getStatusCode())).build();
            }
            return Response.status(Response.Status.OK).entity(id).build();
        } catch(Exception ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new Error(ex.getMessage(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())).build();
        }
    }


    private Consent populateFromApi(String id) throws UnknownIdentifierException {
        return api.retrieve(id);
    }


    public ConsentResource() {
        this.api = AbstractConsentAPI.getInstance();
        this.matchProcessAPI = AbstractMatchProcessAPI.getInstance();
        this.matchAPI = AbstractMatchAPI.getInstance();
        this.useRestrictionValidatorAPI = AbstractUseRestrictionValidatorAPI.getInstance();
    }

}