package org.broadinstitute.consent.http.resources;

import com.google.inject.Inject;
import freemarker.template.TemplateException;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.Vote;
import org.broadinstitute.consent.http.service.AbstractDataAccessRequestAPI;
import org.broadinstitute.consent.http.service.AbstractElectionAPI;
import org.broadinstitute.consent.http.service.AbstractVoteAPI;
import org.broadinstitute.consent.http.service.DataAccessRequestAPI;
import org.broadinstitute.consent.http.service.DatasetAssociationService;
import org.broadinstitute.consent.http.service.DatasetService;
import org.broadinstitute.consent.http.service.ElectionAPI;
import org.broadinstitute.consent.http.service.EmailNotifierService;
import org.broadinstitute.consent.http.service.VoteAPI;
import org.broadinstitute.consent.http.service.VoteService;
import org.broadinstitute.consent.http.service.users.AbstractDACUserAPI;
import org.broadinstitute.consent.http.service.users.DACUserAPI;
import org.broadinstitute.consent.http.util.DarConstants;
import org.broadinstitute.consent.http.util.DarUtil;
import org.bson.Document;

@Path("{api : (api/)?}dataRequest/{requestId}/vote")
public class DataRequestVoteResource extends Resource {

    private final DACUserAPI dacUserAPI;
    private final DataAccessRequestAPI accessRequestAPI;
    private final DatasetService datasetService;
    private final DatasetAssociationService datasetAssociationService;
    private final ElectionAPI electionAPI;
    private final EmailNotifierService emailNotifierService;
    private final VoteAPI api;
    private final VoteService voteService;

    private static final Logger logger = Logger.getLogger(DataRequestVoteResource.class.getName());

    @Inject
    public DataRequestVoteResource(DatasetAssociationService datasetAssociationService,
                                   EmailNotifierService emailNotifierService, VoteService voteService, DatasetService datasetService) {
        this.emailNotifierService = emailNotifierService;
        this.dacUserAPI = AbstractDACUserAPI.getInstance();
        this.accessRequestAPI = AbstractDataAccessRequestAPI.getInstance();
        this.datasetService = datasetService;
        this.datasetAssociationService = datasetAssociationService;
        this.electionAPI = AbstractElectionAPI.getInstance();
        this.api = AbstractVoteAPI.getInstance();
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
    @RolesAllowed(CHAIRPERSON)
    public Response updateFinalAccessVote(Vote rec,
                                                 @PathParam("requestId") String requestId, @PathParam("id") Integer id) {
        try {
            Vote vote = api.firstVoteUpdate(rec, id);
            Document access = accessRequestAPI.describeDataAccessRequestById(requestId);
            List<Integer> dataSets = DarUtil.getIntegerList(access, DarConstants.DATASET_ID);
            // Find any final or agreement votes for this election that have a populated vote:
            List<Vote> votes = voteService
                  .findVotesByElectionIds(Collections.singletonList(rec.getElectionId()))
                  .stream()
                  .filter(v ->
                      v.getType().equalsIgnoreCase(VoteType.FINAL.getValue()) ||
                      v.getType().equalsIgnoreCase(VoteType.AGREEMENT.getValue()))
                  .filter(v -> Objects.nonNull(v.getVote()))
                  .collect(Collectors.toList());
            // If we have any votes with a populated vote, we can now update the election.
            if (!votes.isEmpty()) {
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
    @RolesAllowed({MEMBER, CHAIRPERSON, DATAOWNER})
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
    public Vote describeFinalAccessVote(@PathParam("requestId") Integer requestId) {
        return voteService.describeFinalAccessVoteByElectionId(requestId);
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
    @RolesAllowed(ADMIN)
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
    @RolesAllowed(ADMIN)
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

    private void createDataOwnerElection(String requestId, Vote vote, Document access, List<Integer> dataSets) throws MessagingException, IOException, TemplateException {
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
            List<DataSet> needsApprovedDataSets = datasetService.findNeedsApprovalDataSetByObjectId(dataSets);
            List<Integer> dataSetIds = needsApprovedDataSets.stream().map(DataSet::getDataSetId).collect(Collectors.toList());
            if(CollectionUtils.isNotEmpty(needsApprovedDataSets)){
                Map<User, List<DataSet>> dataOwnerDataSet = datasetAssociationService.findDataOwnersWithAssociatedDataSets(dataSetIds);
                List<Election> elections = electionAPI.createDataSetElections(requestId, dataOwnerDataSet);
                if(CollectionUtils.isNotEmpty(elections)){
                    elections.forEach(voteService::createDataOwnersReviewVotes);
                }
                List<User> admins = dacUserAPI.describeAdminUsersThatWantToReceiveMails();
                if(CollectionUtils.isNotEmpty(admins)) {
                    emailNotifierService.sendAdminFlaggedDarApproved(access.getString(DarConstants.DAR_CODE), admins, dataOwnerDataSet);
                }
            }
        }
    }

    private void validateCollectDAREmail(Vote vote) {
        if(!vote.getType().equals(VoteType.DATA_OWNER.getValue()) && electionAPI.validateCollectDAREmailCondition(vote)){
            try {
                emailNotifierService.sendCollectMessage(vote.getElectionId());
            } catch (MessagingException | IOException | TemplateException e) {
                logger.severe("Error when sending email notification to Chairpersons to collect votes. Cause: " + e);
            }
        }
    }

}
