package org.broadinstitute.consent.http.resources;

import com.google.inject.Inject;
import java.io.File;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.Vote;
import org.broadinstitute.consent.http.models.dto.Error;
import org.broadinstitute.consent.http.service.DataAccessRequestService;
import org.broadinstitute.consent.http.service.ElectionService;
import org.broadinstitute.consent.http.service.EmailNotifierService;
import org.broadinstitute.consent.http.service.SummaryService;
import org.broadinstitute.consent.http.service.VoteService;

@Path("{api : (api/)?}dataRequest/{requestId}/election")
public class DataRequestElectionResource extends Resource {

    private final DataAccessRequestService darService;
    private final ElectionService electionService;
    private final EmailNotifierService emailNotifierService;
    private final SummaryService summaryService;
    private final VoteService voteService;

    @Inject
    public DataRequestElectionResource(DataAccessRequestService darService,
                                       EmailNotifierService emailNotifierService, SummaryService summaryService,
                                       VoteService voteService, ElectionService electionService) {
        this.darService = darService;
        this.emailNotifierService = emailNotifierService;
        this.summaryService = summaryService;
        this.voteService = voteService;
        this.electionService = electionService;
    }

    @POST
    @Consumes("application/json")
    @RolesAllowed({ADMIN, CHAIRPERSON})
    public Response createDataRequestElection(@Context UriInfo info, Election rec,
                                              @PathParam("requestId") String requestId) {
        URI uri;
        Election accessElection = null;
        try {
            DataAccessRequest dar = darService.findByReferenceId(requestId);
            boolean manualReview = dar.requiresManualReview();
            accessElection = electionService.createElection(rec, requestId, ElectionType.DATA_ACCESS);
            List<Vote> votes = voteService.createVotes(accessElection, ElectionType.DATA_ACCESS, manualReview);
            //create RP election
            Election rpElection = electionService.createElection(rec, requestId, ElectionType.RP);
            voteService.createVotes(rpElection, ElectionType.RP, false);
            List<Vote> darVotes = votes.stream().
                    filter(vote -> vote.getType().equals(VoteType.DAC.getValue())).
                    collect(Collectors.toList());
            emailNotifierService.sendNewCaseMessageToList(darVotes, accessElection);
            uri = info.getRequestUriBuilder().build();
        } catch (Exception e) {
            try {
                if (Objects.nonNull(accessElection)) {
                    electionService.deleteElection(accessElection.getElectionId());
                }
            } catch (Exception e2) {
                logger().warn("Error deleting created access election: ", e2);
            }
            return createExceptionResponse(e);
        }
        return Response.created(uri).entity(accessElection).build();
    }


    @GET
    @Produces("application/json")
    @PermitAll
    public Response describe(@PathParam("requestId") String requestId) {
        try {
            return Response.status(Status.OK).entity(electionService.describeDataRequestElection(requestId)).build();
        } catch (Exception e) {
            return Response.status(Status.NOT_FOUND).entity(new Error(e.getMessage(), Status.NOT_FOUND.getStatusCode())).build();
        }
    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id}")
    @RolesAllowed({ADMIN, CHAIRPERSON})
    public Response deleteElection(@PathParam("requestId") String requestId, @PathParam("id") Integer id, @Context UriInfo info) {
        try {
            electionService.deleteElection(id);
            return Response.status(Response.Status.OK).entity("Election was deleted").build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }

    @GET
    @Produces("text/plain")
    @Path("/dataSetVotes")
    @PermitAll
    public Response describeDataSetVotes(@PathParam("requestId") String id) {
        Response.ResponseBuilder response;
        try {
            File fileToSend = summaryService.describeDataSetElectionsVotesForDar(id);
            if ((fileToSend != null)) {
                response = Response.ok(fileToSend);
            } else response = Response.ok();
            return response.build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }

}
