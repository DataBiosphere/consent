package org.broadinstitute.consent.http.resources;

import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.Vote;
import org.broadinstitute.consent.http.models.dto.Error;
import org.broadinstitute.consent.http.service.AbstractElectionAPI;
import org.broadinstitute.consent.http.service.AbstractEmailNotifierAPI;
import org.broadinstitute.consent.http.service.AbstractVoteAPI;
import org.broadinstitute.consent.http.service.ConsentService;
import org.broadinstitute.consent.http.service.DacService;
import org.broadinstitute.consent.http.service.ElectionAPI;
import org.broadinstitute.consent.http.service.EmailNotifierAPI;
import org.broadinstitute.consent.http.service.VoteAPI;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;


@Path("api/consent/{consentId}/election")
public class ConsentElectionResource extends Resource {

    private ConsentService consentService;
    private DacService dacService;
    private final ElectionAPI api;
    private final VoteAPI voteAPI;
    private final EmailNotifierAPI emailApi;

    @Inject
    public ConsentElectionResource(ConsentService consentService, DacService dacService) {
        this.consentService = consentService;
        this.dacService = dacService;
        this.api = AbstractElectionAPI.getInstance();
        this.voteAPI = AbstractVoteAPI.getInstance();
        this.emailApi = AbstractEmailNotifierAPI.getInstance();
    }

    @POST
    @Consumes("application/json")
    @Path("/dac/{dacId}")
    @RolesAllowed({ADMIN, CHAIRPERSON})
    public Response createConsentElectionForDac(
            @Auth AuthUser authUser,
            @Context UriInfo info,
            @PathParam("consentId") String consentId,
            @PathParam("dacId") Integer dacId,
            Election election) {
        URI uri;
        try {
            Consent consent = consentService.getById(consentId);
            Dac dac = dacService.findById(dacId);
            if (dac == null) {
                throw new NotFoundException("Cannot find DAC with the provided id: " + dacId);
            }
            if (consent.getDacId() != null && !consent.getDacId().equals(dacId)) {
                throw new BadRequestException("Consent is already associated to a DAC.");
            }
            consentService.updateConsentDac(consentId, dacId); // Wrap with a try to keep db errors from the user
            uri = createElectionURI(info, election, consentId);
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
        return Response.created(uri).build();
    }

    @POST
    @Consumes("application/json")
    @RolesAllowed({ADMIN, CHAIRPERSON})
    public Response createConsentElection(
            @Auth AuthUser authUser,
            @Context UriInfo info,
            @PathParam("consentId") String consentId,
            Election election) {
        URI uri;
        try {
            uri = createElectionURI(info, election, consentId);
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
        return Response.created(uri).build();
    }

    @GET
    @Produces("application/json")
    @PermitAll
    public Response describe(@PathParam("consentId") String consentId) {
        try {
            return Response.status(Status.OK).entity(api.describeConsentElection(consentId)).build();
        } catch (Exception e) {
            return Response.status(Status.NOT_FOUND).entity(new Error(e.getMessage(), Status.NOT_FOUND.getStatusCode())).build();
        }
    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id}")
    @RolesAllowed({ADMIN, CHAIRPERSON})
    public Response deleteElection(@PathParam("consentId") String consentId, @Context UriInfo info, @PathParam("id") Integer id) {
        try {
            api.deleteElection(consentId, id);
            return Response.status(Response.Status.OK).entity("Election was deleted").build();
        } catch (Exception e) {
            return Response.status(Status.NOT_FOUND).entity(new Error(e.getMessage(), Status.NOT_FOUND.getStatusCode())).build();
        }
    }

    private URI createElectionURI(UriInfo info, Election election, String consentId) throws Exception {
        Election newElection = api.createElection(election, consentId, ElectionType.TRANSLATE_DUL);
        List<Vote> votes = voteAPI.createVotes(newElection.getElectionId(), ElectionType.TRANSLATE_DUL, false);
        List<Vote> dulVotes = votes.stream().
                filter(vote -> vote.getType().equals(VoteType.DAC.getValue())).
                collect(Collectors.toList());
        emailApi.sendNewCaseMessageToList(dulVotes, newElection);
        return info.getRequestUriBuilder().build();
    }

}
