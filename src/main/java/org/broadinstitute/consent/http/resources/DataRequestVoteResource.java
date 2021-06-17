package org.broadinstitute.consent.http.resources;

import com.google.gson.Gson;
import com.google.inject.Inject;
import freemarker.template.TemplateException;
import io.dropwizard.auth.Auth;
import org.apache.commons.collections.CollectionUtils;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.AuthUser;
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

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.mail.MessagingException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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
    public Response createDataRequestVote(
            @Auth AuthUser authUser,
            @Context UriInfo info,
            @PathParam("requestId") String requestId,
            @PathParam("id") Integer voteId,
            String json) {
        try {
            Vote parsedVote = new Gson().fromJson(json, Vote.class);
            validateUserAndVoteId(authUser, voteId);
            Vote vote = voteService.updateVoteById(parsedVote, voteId);
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
    @RolesAllowed({ADMIN, CHAIRPERSON})
    public Response submitFinalAccessVote(
        @Auth AuthUser authUser,
        @PathParam("requestId") String referenceId,
        @PathParam("id") Integer id,
        String json) {
        try {
            validateUserAndVoteId(authUser, id);
            Vote parsedVote = new Gson().fromJson(json, Vote.class);
            electionService.submitFinalAccessVoteDataRequestElection(parsedVote.getElectionId(), parsedVote.getVote());
            Vote updatedVote = voteService.updateVoteById(parsedVote, id);
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
    @RolesAllowed({ADMIN, MEMBER, CHAIRPERSON, DATAOWNER})
    public Response updateDataRequestVote(
            @Auth AuthUser authUser,
            @PathParam("requestId") String requestId,
            @PathParam("id") Integer id,
            String json) {
        try {
            validateUserAndVoteId(authUser, id);
            Vote parsedVote = new Gson().fromJson(json, Vote.class);
            Vote updatedVote = voteService.updateVote(parsedVote, id, requestId);
            if (electionService.checkDataOwnerToCloseElection(updatedVote.getElectionId())) {
                electionService.closeDataOwnerApprovalElection(updatedVote.getElectionId());
            }
            return Response.ok(updatedVote).build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }


    @GET
    @Produces("application/json")
    @Path("/{id}")
    @PermitAll
    public Response describe(
            @Auth AuthUser authUser,
            @PathParam("requestId") String requestId,
            @PathParam("id") Integer id) {
        try {
            Vote vote = voteService.findVoteById(id);
            return Response.ok().entity(vote).build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }

    @GET
    @Produces("application/json")
    @Path("/final")
    @PermitAll
    public Response describeFinalAccessVote(
            @Auth AuthUser authUser,
            @PathParam("requestId") Integer requestId) {
        try {
            Vote vote = voteService.describeFinalAccessVoteByElectionId(requestId);
            return Response.ok().entity(vote).build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }

    @GET
    @Produces("application/json")
    @Path("/dataOwner/{dataOwnerId}")
    @PermitAll
    public Response describeDataOwnerVote(
            @Auth AuthUser authUser,
            @PathParam("requestId") String requestId,
            @PathParam("dataOwnerId") Integer dataOwnerId){
        try {
            Vote vote = voteService.describeDataOwnerVote(requestId,dataOwnerId);
            return Response.ok().entity(vote).build();
        } catch (Exception e){
            return createExceptionResponse(e);
        }
    }

    @GET
    @Produces("application/json")
    @PermitAll
    public Response describeAllVotes(
            @Auth AuthUser authUser,
            @PathParam("requestId") String requestId) {
        try {
            List<Vote> votes = voteService.describeVotes(requestId);
            return Response.ok().entity(votes).build();
        } catch (Exception e){
            return createExceptionResponse(e);
        }
    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id}")
    @RolesAllowed({ADMIN})
    public Response deleteVote(
            @Auth AuthUser authUser,
            @PathParam("requestId") String requestId,
            @PathParam("id") Integer id) {
        try {
            validateUserAndVoteId(authUser, id);
            voteService.deleteVote(id, requestId);
            return Response.status(Response.Status.OK).entity("Vote was deleted").build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ADMIN})
    public Response deleteVotes(
            @Auth AuthUser authUser,
            @PathParam("requestId") String requestId) {
        try {
            if (requestId == null)
                return Response.status(Response.Status.BAD_REQUEST).build();
            voteService.deleteVotes(requestId);
            return Response.ok().entity("Votes for specified id have been deleted").build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }

    // Validate that the user is an admin OR has the same user id as the target vote's user id
    // Chairs, Members, and Data Owners should only be able to update their own votes
    // while Admins can make updates to anyone's vote.
    private void validateUserAndVoteId(AuthUser authUser, Integer voteId) throws NotFoundException {
        User user = userService.findUserByEmail(authUser.getName());
        Vote existingVote = voteService.findVoteById(voteId);
        validateAuthedRoleUser(
                Collections.singletonList(UserRoles.ADMIN),
                user,
                existingVote.getDacUserId());
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
