package org.broadinstitute.consent.http.resources;

import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.Vote;
import org.broadinstitute.consent.http.models.dto.Error;
import org.broadinstitute.consent.http.service.*;
import org.broadinstitute.consent.http.util.DarConstants;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import java.io.File;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Path("{api : (api/)?}dataRequest/{requestId}/election")
public class DataRequestElectionResource extends Resource {

    private final ElectionAPI api;
    private final VoteAPI voteAPI;
    private final EmailNotifierAPI emailApi;
    private final DataAccessRequestAPI darApi;
    private final SummaryAPI summaryAPI;

    public DataRequestElectionResource() {
        this.api = AbstractElectionAPI.getInstance();
        this.voteAPI = AbstractVoteAPI.getInstance();
        this.emailApi = AbstractEmailNotifierAPI.getInstance();
        this.darApi = AbstractDataAccessRequestAPI.getInstance();
        this.summaryAPI = AbstractSummaryAPI.getInstance();
    }

    @POST
    @Consumes("application/json")
    public Response createDataRequestElection(@Context UriInfo info, Election rec,
                                              @PathParam("requestId") String requestId) {
        URI uri;
        Election accessElection;
        try {
            accessElection = api.createElection(rec, requestId.toString(), ElectionType.DATA_ACCESS);
            List<Vote> votes;
            //create RP election
            if(!Objects.isNull(darApi.getField(requestId, DarConstants.RESTRICTION))){
                votes = voteAPI.createVotes(accessElection.getElectionId(), ElectionType.DATA_ACCESS, false);
                Election rpElection = api.createElection(rec, requestId.toString(), ElectionType.RP);
                voteAPI.createVotes(rpElection.getElectionId(), ElectionType.RP, false);
            }else{
                votes = voteAPI.createVotes(accessElection.getElectionId(), ElectionType.DATA_ACCESS, true);
            }
            List<Vote> darVotes = votes.stream().filter(vote -> vote.getType().equals("DAC")).collect(Collectors.toList());
            emailApi.sendNewCaseMessageToList(darVotes, accessElection);
            uri = info.getRequestUriBuilder().build();
        } catch (NotFoundException e){
            return Response.status(Status.NOT_FOUND).entity(new Error(e.getMessage(), Status.NOT_FOUND.getStatusCode())).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Status.BAD_REQUEST).entity(new Error(e.getMessage(), Status.BAD_REQUEST.getStatusCode())).build();
        } catch (Exception e){
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(new Error(e.getMessage(),Status.INTERNAL_SERVER_ERROR.getStatusCode())).build();
        }
        return Response.created(uri).entity(accessElection).build();
    }


    @GET
    @Produces("application/json")
    public Response describe(@PathParam("requestId") String requestId) {
        try {
            return  Response.status(Status.OK).entity(api.describeDataRequestElection(requestId)).build();
        } catch (Exception e) {
            return Response.status(Status.NOT_FOUND).entity(new Error(e.getMessage(), Status.NOT_FOUND.getStatusCode())).build();
        }
    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id}")
    public Response deleteElection(@PathParam("requestId") String requestId, @PathParam("id") Integer id, @Context UriInfo info) {
        try {
            api.deleteElection(requestId, id);
            return Response.status(Response.Status.OK).entity("Election was deleted").build();
        } catch (Exception e) {
            throw new NotFoundException(e.getMessage());
        }
    }

    @GET
    @Produces("text/plain")
    @Path("/dataSetVotes")
    public Response describeDataSetVotes(@PathParam("requestId") String id) {
        Response.ResponseBuilder response;
        try {
            File fileToSend = summaryAPI.describeDataSetElectionsVotesForDar(id);
            if ((fileToSend != null)) {
                response = Response.ok(fileToSend);
            } else response = Response.ok();
            return response.build();
        } catch (Exception e) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(new Error(e.getMessage(), Status.INTERNAL_SERVER_ERROR.getStatusCode())).build();
        }
    }


}
