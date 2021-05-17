package org.broadinstitute.consent.http.resources;

import com.google.gson.Gson;
import com.google.inject.Inject;
import freemarker.template.TemplateException;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.mail.MessagingException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.apache.commons.collections.CollectionUtils;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.Vote;
import org.broadinstitute.consent.http.service.DataAccessRequestService;
import org.broadinstitute.consent.http.service.DatasetAssociationService;
import org.broadinstitute.consent.http.service.DatasetService;
import org.broadinstitute.consent.http.service.ElectionService;
import org.broadinstitute.consent.http.service.EmailNotifierService;
import org.broadinstitute.consent.http.service.UserService;
import org.broadinstitute.consent.http.service.VoteService;

@Path("api/dataRequest/{requestId}/vote")
public class DataRequestVoteResource extends Resource {

    private final UserService userService;
    private final DataAccessRequestService dataAccessRequestService;
    private final DatasetService datasetService;
    private final DatasetAssociationService datasetAssociationService;
    private final ElectionService electionService;
    private final EmailNotifierService emailNotifierService;
    private final VoteService voteService;

    private static final Logger logger = Logger.getLogger(DataRequestVoteResource.class.getName());

    @Inject
    public DataRequestVoteResource(
            DataAccessRequestService dataAccessRequestService,
            DatasetAssociationService datasetAssociationService,
            EmailNotifierService emailNotifierService,
            VoteService voteService,
            DatasetService datasetService,
            ElectionService electionService,
            UserService userService) {
        this.emailNotifierService = emailNotifierService;
        this.userService = userService;
        this.datasetService = datasetService;
        this.dataAccessRequestService = dataAccessRequestService;
        this.datasetAssociationService = datasetAssociationService;
        this.electionService = electionService;
        this.voteService = voteService;
    }

    @POST
    @Consumes("application/json")
    @Path("/{id}")
    @RolesAllowed({MEMBER, CHAIRPERSON, DATAOWNER})
    public Response createDataRequestVote(@Context UriInfo info, Vote rec,
                                          @PathParam("requestId") String requestId,
                                          @PathParam("id") Integer voteId) {
        try {
            Vote vote = voteService.updateVoteById(rec, voteId);
            validateCollectDAREmail(vote);
            if(electionService.checkDataOwnerToCloseElection(vote.getElectionId())){
                electionService.closeDataOwnerApprovalElection(vote.getElectionId());
            }
            URI uri = info.getRequestUriBuilder().path("{id}").build(vote.getVoteId());
            return Response.ok(uri).build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }


    @POST
    @Consumes("application/json")
    @Produces("application/json")
    @Path("/{id}/final")
    @RolesAllowed(CHAIRPERSON)
    public Response submitFinalAccessVote(
        @PathParam("requestId") String referenceId,
        @PathParam("id") Integer id,
        String json) {
        try {
            Vote voteRecord = new Gson().fromJson(json, Vote.class);
            electionService.submitFinalAccessVoteDataRequestElection(voteRecord.getElectionId());
            Vote updatedVote = voteService.updateVoteById(voteRecord, id);
            DataAccessRequest dar = dataAccessRequestService.findByReferenceId(referenceId);
            createDataOwnerElection(updatedVote, dar);
            return Response.ok(updatedVote).build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }


    @PUT
    @Consumes("application/json")
    @Produces("application/json")
    @Path("/{id}")
    @RolesAllowed({MEMBER, CHAIRPERSON, DATAOWNER})
    public Response updateDataRequestVote(Vote rec,
                                          @PathParam("requestId") String requestId, @PathParam("id") Integer id) {
        try {
            Vote vote = voteService.updateVote(rec, id, requestId);
            if(electionService.checkDataOwnerToCloseElection(vote.getElectionId())){
                electionService.closeDataOwnerApprovalElection(vote.getElectionId());
            }
            return Response.ok(vote).build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }


    @GET
    @Produces("application/json")
    @Path("/{id}")
    @PermitAll
    public Vote describe(@PathParam("requestId") String requestId,
                         @PathParam("id") Integer id) {
        return voteService.describeVoteById(id, requestId);
    }

    @GET
    @Produces("application/json")
    @Path("/final")
    @PermitAll
    public Vote describeFinalAccessVote(@PathParam("requestId") Integer requestId) {
        return voteService.describeFinalAccessVoteByElectionId(requestId);
    }

    @GET
    @Produces("application/json")
    @Path("/dataOwner/{dataOwnerId}")
    @PermitAll
    public Response describeDataOwnerVote(@PathParam("requestId") String requestId, @PathParam("dataOwnerId") Integer dataOwnerId){
        try{
            return Response.ok(voteService.describeDataOwnerVote(requestId,dataOwnerId)).build();
        }catch (Exception e){
            return createExceptionResponse(e);
        }
    }

    @GET
    @Produces("application/json")
    @PermitAll
    public List<Vote> describeAllVotes(@PathParam("requestId") String requestId) {
        return voteService.describeVotes(requestId);
    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id}")
    @RolesAllowed(ADMIN)
    public Response deleteVote(@PathParam("requestId") String requestId, @PathParam("id") Integer id) {
        try {
            voteService.deleteVote(id, requestId);
            return Response.status(Response.Status.OK).entity("Vote was deleted").build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(ADMIN)
    public Response deleteVotes(@PathParam("requestId") String requestId) {
        try {
            if (requestId == null)
                return Response.status(Response.Status.BAD_REQUEST).build();
            voteService.deleteVotes(requestId);
            return Response.ok().entity("Votes for specified id have been deleted").build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }

    private void createDataOwnerElection(Vote vote, DataAccessRequest dar) throws MessagingException, IOException, TemplateException {
        Vote agreementVote = null;
        Vote finalVote = null;
        if(vote.getType().equals(VoteType.FINAL.getValue())){
            List<Vote> agreement = voteService.describeVoteByTypeAndElectionId(VoteType.AGREEMENT.getValue(), vote.getElectionId());
            agreementVote = CollectionUtils.isNotEmpty(agreement) ? agreement.get(0) : null;
            finalVote = vote;
        }else if(vote.getType().equals(VoteType.AGREEMENT.getValue())){
            List<Vote> finalVotes = voteService.describeVoteByTypeAndElectionId(VoteType.FINAL.getValue(), vote.getElectionId());
            finalVote = CollectionUtils.isNotEmpty(finalVotes) ? finalVotes.get(0) : null;
            agreementVote = vote;
        }
        if((finalVote != null && finalVote.getVote() != null && finalVote.getVote()) && (agreementVote == null || (agreementVote != null && agreementVote.getVote() != null))){
            List<DataSet> needsApprovedDataSets = datasetService.findNeedsApprovalDataSetByObjectId(dar.getData().getDatasetIds());
            List<Integer> dataSetIds = needsApprovedDataSets.stream().map(DataSet::getDataSetId).collect(Collectors.toList());
            if(CollectionUtils.isNotEmpty(needsApprovedDataSets)){
                Map<User, List<DataSet>> dataOwnerDataSet = datasetAssociationService.findDataOwnersWithAssociatedDataSets(dataSetIds);
                List<Election> elections = electionService.createDataSetElections(dar.getReferenceId(), dataOwnerDataSet);
                if(CollectionUtils.isNotEmpty(elections)){
                    elections.forEach(voteService::createDataOwnersReviewVotes);
                }
                List<User> admins = userService.describeAdminUsersThatWantToReceiveMails();
                if(CollectionUtils.isNotEmpty(admins)) {
                    emailNotifierService.sendAdminFlaggedDarApproved(dar.getData().getDarCode(), admins, dataOwnerDataSet);
                }
            }
        }
    }

    private void validateCollectDAREmail(Vote vote) {
        if(!vote.getType().equals(VoteType.DATA_OWNER.getValue()) && electionService.validateCollectDAREmailCondition(vote)){
            try {
                emailNotifierService.sendCollectMessage(vote.getElectionId());
            } catch (MessagingException | IOException | TemplateException e) {
                logger.severe("Error when sending email notification to Chairpersons to collect votes. Cause: " + e);
            }
        }
    }

}
