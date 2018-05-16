package org.broadinstitute.consent.http.resources;

import com.google.api.client.http.GenericUrl;
import com.google.gson.Gson;
import io.dropwizard.auth.Auth;
import org.broadinstitute.consent.http.enumeration.Actions;
import org.broadinstitute.consent.http.enumeration.AuditTable;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.TranslateType;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.DACUser;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.dto.Error;
import org.broadinstitute.consent.http.service.*;
import org.broadinstitute.consent.http.service.users.AbstractDACUserAPI;
import org.broadinstitute.consent.http.service.users.DACUserAPI;
import org.broadinstitute.consent.http.service.validate.AbstractUseRestrictionValidatorAPI;
import org.broadinstitute.consent.http.service.validate.UseRestrictionValidatorAPI;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;

@Path("{auth: (basic/|api/)?}consent")
public class ConsentResource extends Resource {

    private final ConsentAPI api;
    private final DACUserAPI dacUserAPI;
    private final AuditServiceAPI auditServiceAPI;
    private final MatchProcessAPI matchProcessAPI;
    private final MatchAPI matchAPI;
    private final TranslateServiceAPI translateServiceAPI = AbstractTranslateServiceAPI.getInstance();
    private final UseRestrictionValidatorAPI useRestrictionValidatorAPI;
    private final ElectionAPI electionAPI;


    @Path("{id}")
    @GET
    @Produces("application/json")
    @PermitAll
    public Response describe(@PathParam("id") String id) {
        // TODO devolver last election status
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
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new Error(e.getMessage(), Response.Status.NOT_FOUND.getStatusCode())).build();
        }
    }

    @POST
    @Consumes("application/json")
    @RolesAllowed({"ADMIN", "RESEARCHER", "DATAOWNER"})
    public Response createConsent(@Context UriInfo info, Consent rec, @Auth User user) {
        try {
            DACUser dacUser = dacUserAPI.describeDACUserByEmail(user.getName());
            if (rec.getTranslatedUseRestriction() == null) {
                rec.setTranslatedUseRestriction(translateServiceAPI.translate(TranslateType.SAMPLESET.getValue(),rec.getUseRestriction()));
            }
            if(rec.getUseRestriction() != null){
                useRestrictionValidatorAPI.validateUseRestriction(new Gson().toJson(rec.getUseRestriction()));
            }
            if (rec.getDataUse() == null) {
                throw new IllegalArgumentException("Data Use Object is required.");
            }
            if (rec.getDataUseLetter() != null) {
                checkValidDUL(rec);
            }
            Consent consent = api.create(rec);
            auditServiceAPI.saveConsentAudit(consent.getConsentId(), AuditTable.CONSENT.getValue(), Actions.CREATE.getValue(), dacUser.getEmail());
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
    @RolesAllowed({"ADMIN", "RESEARCHER", "DATAOWNER"})
    public Response update(@PathParam("id") String id, Consent updated, @Auth User user) {
        try {
            checkConsentElection(populateFromApi(id));
            if (updated.getTranslatedUseRestriction() == null) {
                updated.setTranslatedUseRestriction(translateServiceAPI.translate(TranslateType.SAMPLESET.getValue(),updated.getUseRestriction()));
            }
            if(updated.getUseRestriction() != null) {
                useRestrictionValidatorAPI.validateUseRestriction(new Gson().toJson(updated.getUseRestriction()));
            }
            if (updated.getDataUse() == null) {
                throw new IllegalArgumentException("Data Use Object is required.");
            }
            if (updated.getDataUseLetter() != null) {
                checkValidDUL(updated);
            }
            DACUser dacUser = dacUserAPI.describeDACUserByEmail(user.getName());
            updated = api.update(id, updated);
            auditServiceAPI.saveConsentAudit(updated.getConsentId(), AuditTable.CONSENT.getValue(), Actions.REPLACE.getValue(), dacUser.getEmail());
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
        try {
            Consent consent = api.getByName(name);
            Election election = electionAPI.describeConsentElection(consent.consentId);
            if (election.getFinalVote() != null && election.getFinalVote()) {
                return Response.ok(consent).build();
            } else {
                // electionAPI.describeConsentElection will throw NotFoundException if no election exists, and we catch
                // that below. Here, we have an existing-but-failed election. Let's send it to the same catch clause.
                throw new NotFoundException();
            }
        } catch (UnknownIdentifierException ex) {
            return Response.status(Response.Status.NOT_FOUND).entity(new Error(String.format("Consent with a name of '%s' was not found.", name), Response.Status.NOT_FOUND.getStatusCode())).build();
        } catch (NotFoundException nfe) {
            return Response.status(Response.Status.BAD_REQUEST).entity(new Error(String.format("Consent with a name of '%s' is not approved.", name), Response.Status.BAD_REQUEST.getStatusCode())).build();
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
        this.dacUserAPI = AbstractDACUserAPI.getInstance();
        this.auditServiceAPI = AbstractAuditServiceAPI.getInstance();
        this.electionAPI = AbstractElectionAPI.getInstance();
    }

    private void checkValidDUL(Consent rec) {
        // ensure that the URL is a valid one
        try {
            new GenericUrl(rec.getDataUseLetter());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid Data Use Letter URL: " + rec.getDataUseLetter());
        }
    }

    private void checkConsentElection(Consent consentElection) throws Exception {
        ArrayList<String> notAllowedConditions = new ArrayList<>(Arrays.asList(ElectionStatus.OPEN.getValue(), ElectionStatus.CLOSED.getValue()));
        String consentElectionStatus = consentElection.getLastElectionStatus();
        Boolean consentElectionArchived = consentElection.getLastElectionArchived();
        if(consentElectionStatus == ElectionStatus.OPEN.getValue() ||
           consentElectionStatus == ElectionStatus.CLOSED.getValue() ||
           !consentElectionArchived) {
            throw new Exception("Election condition fails");
        }
    }
}
