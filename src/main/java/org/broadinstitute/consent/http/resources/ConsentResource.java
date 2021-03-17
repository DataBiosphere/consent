package org.broadinstitute.consent.http.resources;

import com.google.api.client.http.GenericUrl;
import com.google.gson.Gson;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import java.net.URI;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
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
import org.broadinstitute.consent.http.enumeration.Actions;
import org.broadinstitute.consent.http.enumeration.AuditTable;
import org.broadinstitute.consent.http.exceptions.UpdateConsentException;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.dto.Error;
import org.broadinstitute.consent.http.service.AbstractElectionAPI;
import org.broadinstitute.consent.http.service.AbstractMatchAPI;
import org.broadinstitute.consent.http.service.AbstractMatchProcessAPI;
import org.broadinstitute.consent.http.service.AuditService;
import org.broadinstitute.consent.http.service.ConsentService;
import org.broadinstitute.consent.http.service.ElectionAPI;
import org.broadinstitute.consent.http.service.MatchAPI;
import org.broadinstitute.consent.http.service.MatchProcessAPI;
import org.broadinstitute.consent.http.service.UnknownIdentifierException;
import org.broadinstitute.consent.http.service.UserService;
import org.broadinstitute.consent.http.service.validate.AbstractUseRestrictionValidatorAPI;
import org.broadinstitute.consent.http.service.validate.UseRestrictionValidatorAPI;

@Path("{auth: (basic/|api/)?}consent")
public class ConsentResource extends Resource {

    private final ConsentService consentService;
    private final AuditService auditService;
    private final MatchProcessAPI matchProcessAPI;
    private final MatchAPI matchAPI;
    private final UseRestrictionValidatorAPI useRestrictionValidatorAPI;
    private final ElectionAPI electionAPI;
    private final UserService userService;

    @Inject
    public ConsentResource(AuditService auditService, UserService userService, ConsentService consentService) {
        this.auditService = auditService;
        this.consentService = consentService;
        this.matchProcessAPI = AbstractMatchProcessAPI.getInstance();
        this.matchAPI = AbstractMatchAPI.getInstance();
        this.useRestrictionValidatorAPI = AbstractUseRestrictionValidatorAPI.getInstance();
        this.electionAPI = AbstractElectionAPI.getInstance();
        this.userService = userService;
    }

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

    @POST
    @Consumes("application/json")
    @RolesAllowed({ADMIN, RESEARCHER, DATAOWNER})
    public Response createConsent(@Context UriInfo info, Consent rec, @Auth AuthUser user) {
        try {
            User dacUser = userService.findUserByEmail(user.getName());
            if(rec.getUseRestriction() != null){
                useRestrictionValidatorAPI.validateUseRestriction(new Gson().toJson(rec.getUseRestriction()));
            }
            if (rec.getDataUse() == null) {
                throw new IllegalArgumentException("Data Use Object is required.");
            }
            if (rec.getDataUseLetter() != null) {
                checkValidDUL(rec);
            }
            Consent consent = consentService.create(rec);
            auditService.saveConsentAudit(consent.getConsentId(), AuditTable.CONSENT.getValue(), Actions.CREATE.getValue(), dacUser.getEmail());
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
    @RolesAllowed({ADMIN, RESEARCHER, DATAOWNER})
    public Response update(@PathParam("id") String id, Consent updated, @Auth AuthUser user) {
        try {
            checkConsentElection(id);
            if(updated.getUseRestriction() != null) {
                useRestrictionValidatorAPI.validateUseRestriction(new Gson().toJson(updated.getUseRestriction()));
            }
            if (updated.getDataUse() == null) {
                throw new IllegalArgumentException("Data Use Object is required.");
            }
            if (updated.getDataUseLetter() != null) {
                checkValidDUL(updated);
            }
            User dacUser = userService.findUserByEmail(user.getName());
            updated = consentService.update(id, updated);
            auditService.saveConsentAudit(updated.getConsentId(), AuditTable.CONSENT.getValue(), Actions.REPLACE.getValue(), dacUser.getEmail());
            matchProcessAPI.processMatchesForConsent(id);
            return Response.ok(updated).build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }


    @DELETE
    @Produces("application/json")
    @Path("{id}")
    @RolesAllowed(ADMIN)
    public Response delete(@PathParam("id") String consentId) {
        try {
            consentService.delete(consentId);
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
            Consent consent = consentService.getByName(name);
            return Response.ok(consent).build();
        } catch (UnknownIdentifierException ex) {
            return Response.status(Response.Status.NOT_FOUND).entity(new Error(String.format("Consent with a name of '%s' was not found.", name), Response.Status.NOT_FOUND.getStatusCode())).build();
        }
    }


    private Consent populateFromApi(String id) throws UnknownIdentifierException {
        return consentService.retrieve(id);
    }

    private void checkValidDUL(Consent rec) {
        // ensure that the URL is a valid one
        try {
            new GenericUrl(rec.getDataUseLetter());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid Data Use Letter URL: " + rec.getDataUseLetter());
        }
    }

    private void checkConsentElection(String consentId) throws Exception {
        Consent consent = populateFromApi(consentId);
        Boolean consentElectionArchived = consent.getLastElectionArchived();

        if (consentElectionArchived != null && !consentElectionArchived) {
            // NOTE: we still need to define a proper error message that clarifies the cause of the error.
            throw new UpdateConsentException(String.format("Consent '%s' cannot be updated with an active election.", consentId));
        }
    }
}
