package org.broadinstitute.consent.http.resources;

import freemarker.template.TemplateException;
import org.apache.commons.collections.CollectionUtils;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.*;
import org.broadinstitute.consent.http.service.*;
import org.broadinstitute.consent.http.service.users.AbstractDACUserAPI;
import org.broadinstitute.consent.http.service.users.DACUserAPI;
import org.broadinstitute.consent.http.util.DarConstants;
import org.bson.Document;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.mail.MessagingException;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Path("{api : (api/)?}dataRequest/{requestId}/vote")
public class DataRequestVoteResource extends Resource {

    private final VoteAPI api;
    private final ElectionAPI electionAPI;
    private final EmailNotifierAPI emailAPI;
    private final DataAccessRequestAPI accessRequestAPI;
    private final DataSetAPI dataSetAPI;
    private final DACUserAPI dacUserAPI;
    private final EmailNotifierAPI emailNotifierAPI;
    private final DataSetAssociationAPI dataSetAssociationAPI;
    private final ApprovalExpirationTimeAPI approvalExpirationTimeAPI;
    private static final Logger logger = Logger.getLogger(DataRequestVoteResource.class.getName());

    public DataRequestVoteResource() {
        this.api = AbstractVoteAPI.getInstance();
        this.electionAPI = AbstractElectionAPI.getInstance();
        this.emailAPI = AbstractEmailNotifierAPI.getInstance();
        this.accessRequestAPI = AbstractDataAccessRequestAPI.getInstance();
        this.dataSetAPI = AbstractDataSetAPI.getInstance();
        this.dacUserAPI = AbstractDACUserAPI.getInstance();
        this.emailNotifierAPI = AbstractEmailNotifierAPI.getInstance();
        this.dataSetAssociationAPI = AbstractDataSetAssociationAPI.getInstance();
        this.approvalExpirationTimeAPI = AbstractApprovalExpirationTimeAPI.getInstance();
    }

    @POST
    @Consumes("application/json")
    @Path("/{id}")
    @RolesAllowed({"MEMBER", "CHAIRPERSON", "DATAOWNER"})
    public Response createDataRequestVote(@Context UriInfo info, Vote rec,
                                          @PathParam("requestId") String requestId,
                                          @PathParam("id") Integer voteId) {
        try {
            Vote vote = api.firstVoteUpdate(rec, voteId);
            validateCollectDAREmail(vote);
            if(electionAPI.checkDataOwnerToCloseElection(vote.getElectionId())){
                electionAPI.closeDataOwnerApprovalElection(vote.getElectionId());
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
    @RolesAllowed("CHAIRPERSON")
    public Response updateFinalAccessVote(Vote rec,
                                                 @PathParam("requestId") String requestId, @PathParam("id") Integer id) {
        try {
            Vote vote = api.firstVoteUpdate(rec, id);
            Document access = accessRequestAPI.describeDataAccessRequestById(requestId);
            List<String> dataSets = access.get(DarConstants.DATASET_ID, List.class);
            if(access.containsKey(DarConstants.RESTRICTION)){
                List<Vote> votes = vote.getType().equals(VoteType.FINAL.getValue()) ? api.describeVoteByTypeAndElectionId(VoteType.AGREEMENT.getValue(), vote.getElectionId()) :  api.describeVoteByTypeAndElectionId(VoteType.FINAL.getValue(), vote.getElectionId());
                if(vote.getVote() != null && votes.get(0).getVote() != null){
                    electionAPI.updateFinalAccessVoteDataRequestElection(rec.getElectionId());
                }
            }else {
                electionAPI.updateFinalAccessVoteDataRequestElection(rec.getElectionId());
            }
            createDataOwnerElection(requestId, vote, access, dataSets);
            return Response.ok(vote).build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }


    @PUT
    @Consumes("application/json")
    @Produces("application/json")
    @Path("/{id}")
    @RolesAllowed({"MEMBER", "CHAIRPERSON", "DATAOWNER"})
    public Response updateDataRequestVote(Vote rec,
                                          @PathParam("requestId") String requestId, @PathParam("id") Integer id) {
        try {
            Vote vote = api.updateVote(rec, id, requestId);
            if(electionAPI.checkDataOwnerToCloseElection(vote.getElectionId())){
                electionAPI.closeDataOwnerApprovalElection(vote.getElectionId());
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
        return api.describeVoteById(id, requestId);
    }

    @GET
    @Produces("application/json")
    @Path("/final")
    @PermitAll
    public Vote describeFinalAccessVote(@PathParam("requestId") Integer requestId){
        return api.describeVoteFinalAccessVoteById(requestId);

    }

    @GET
    @Produces("application/json")
    @Path("/dataOwner/{dataOwnerId}")
    @PermitAll
    public Response describeDataOwnerVote(@PathParam("requestId") String requestId, @PathParam("dataOwnerId") Integer dataOwnerId){
        try{
            return Response.ok(api.describeDataOwnerVote(requestId,dataOwnerId)).build();
        }catch (Exception e){
            return createExceptionResponse(e);
        }
    }

    @GET
    @Produces("application/json")
    @PermitAll
    public List<Vote> describeAllVotes(@PathParam("requestId") String requestId) {
        return api.describeVotes(requestId);
    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id}")
    @RolesAllowed("ADMIN")
    public Response deleteVote(@PathParam("requestId") String requestId, @PathParam("id") Integer id) {
        try {
            api.deleteVote(id, requestId);
            return Response.status(Response.Status.OK).entity("Vote was deleted").build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("ADMIN")
    public Response deleteVotes(@PathParam("requestId") String requestId) {
        try {
            if (requestId == null)
                return Response.status(Response.Status.BAD_REQUEST).build();
            api.deleteVotes(requestId);
            return Response.ok().entity("Votes for specified id have been deleted").build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }

    private void createDataOwnerElection(String requestId, Vote vote, Document access, List<String> dataSets) throws MessagingException, IOException, TemplateException {
        Vote agreementVote = null;
        Vote finalVote = null;
        if(vote.getType().equals(VoteType.FINAL.getValue())){
            List<Vote> agreement = api.describeVoteByTypeAndElectionId(VoteType.AGREEMENT.getValue(), vote.getElectionId());
            agreementVote = CollectionUtils.isNotEmpty(agreement) ? agreement.get(0) : null;
            finalVote = vote;
        }else if(vote.getType().equals(VoteType.AGREEMENT.getValue())){
            List<Vote> finalVotes = api.describeVoteByTypeAndElectionId(VoteType.FINAL.getValue(), vote.getElectionId());
            finalVote = CollectionUtils.isNotEmpty(finalVotes) ? finalVotes.get(0) : null;
            agreementVote = vote;
        }
        if((finalVote != null && finalVote.getVote() != null && finalVote.getVote()) && (agreementVote == null || (agreementVote != null && agreementVote.getVote() != null))){
            List<DataSet> needsApprovedDataSets = dataSetAPI.findNeedsApprovalDataSetByObjectId(dataSets);
            List<String> objectIds = needsApprovedDataSets.stream().map(DataSet::getObjectId).collect(Collectors.toList());
            if(CollectionUtils.isNotEmpty(needsApprovedDataSets)){
                Map<DACUser, List<DataSet>> dataOwnerDataSet = dataSetAssociationAPI.findDataOwnersWithAssociatedDataSets(objectIds);
                List<Election> elections = electionAPI.createDataSetElections(requestId, dataOwnerDataSet);
                if(CollectionUtils.isNotEmpty(elections)){
                    elections.stream().forEach(election -> {
                        api.createDataOwnersReviewVotes(election);
                    });
                }
                List<DACUser> admins = dacUserAPI.describeAdminUsersThatWantToReceiveMails();
                if(CollectionUtils.isNotEmpty(admins)) {
                    emailNotifierAPI.sendAdminFlaggedDarApproved(access.getString(DarConstants.DAR_CODE), admins, dataOwnerDataSet);
                }
                emailNotifierAPI.sendNeedsPIApprovalMessage(dataOwnerDataSet, access, approvalExpirationTimeAPI.findApprovalExpirationTime().getAmountOfDays());
            }
        }
    }

    private void validateCollectDAREmail(Vote vote) {
        if(!vote.getType().equals(VoteType.DATA_OWNER.getValue()) && electionAPI.validateCollectDAREmailCondition(vote)){
            try {
                emailAPI.sendCollectMessage(vote.getElectionId());
            } catch (MessagingException | IOException | TemplateException e) {
                logger.severe("Error when sending email notification to Chaiperson to collect votes. Cause: "+e);
            }
        }
    }

}

