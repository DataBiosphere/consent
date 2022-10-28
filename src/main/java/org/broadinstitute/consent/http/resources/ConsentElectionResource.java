package org.broadinstitute.consent.http.resources;

import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.Vote;
import org.broadinstitute.consent.http.models.dto.Error;
import org.broadinstitute.consent.http.service.DacService;
import org.broadinstitute.consent.http.service.ElectionService;
import org.broadinstitute.consent.http.service.EmailService;
import org.broadinstitute.consent.http.service.VoteService;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
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
import java.util.Optional;
import java.util.stream.Collectors;


@Path("api/consent/{consentId}/election")
public class ConsentElectionResource extends Resource {

    private final DacService dacService;
    private final EmailService emailService;
    private final VoteService voteService;
    private final ElectionService electionService;

    @Inject
    public ConsentElectionResource(DacService dacService,
                                   EmailService emailService, VoteService voteService, ElectionService electionService) {
        this.dacService = dacService;
        this.emailService = emailService;
        this.voteService = voteService;
        this.electionService = electionService;
    }

    @Deprecated
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

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id}")
    @RolesAllowed({ADMIN, CHAIRPERSON})
    public Response deleteElection(@PathParam("consentId") String consentId, @Context UriInfo info, @PathParam("id") Integer id) {
        try {
            electionService.deleteElection(id);
            return Response.status(Response.Status.OK).entity("Election was deleted").build();
        } catch (Exception e) {
            return Response.status(Status.NOT_FOUND).entity(new Error(e.getMessage(), Status.NOT_FOUND.getStatusCode())).build();
        }
    }

    private URI createElectionURI(UriInfo info, Election election, String consentId) throws Exception {
        // For a consent election, any dataset associated to the consent is
        // appropriate for assignment to this election.
        Optional<Dataset> dataset = dacService.findDatasetsByConsentId(consentId).
                stream().
                findFirst();
        dataset.ifPresent(dataSet -> election.setDataSetId(dataSet.getDataSetId()));
        Election newElection = electionService.createElection(election, consentId, ElectionType.TRANSLATE_DUL);
        List<Vote> votes = voteService.createVotes(newElection, ElectionType.TRANSLATE_DUL, false);
        List<Vote> dulVotes = votes.stream().
                filter(vote -> vote.getType().equals(VoteType.DAC.getValue())).
                collect(Collectors.toList());
        emailService.sendNewCaseMessageToList(dulVotes, newElection);
        return info.getRequestUriBuilder().build();
    }

}
