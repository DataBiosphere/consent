package org.genomebridge.consent.http.resources;

import freemarker.template.TemplateException;
import org.genomebridge.consent.http.enumeration.ElectionType;
import org.genomebridge.consent.http.enumeration.VoteType;
import org.genomebridge.consent.http.models.Election;
import org.genomebridge.consent.http.models.Vote;
import org.genomebridge.consent.http.service.*;

import javax.mail.MessagingException;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.net.URI;
import java.util.List;

@Path("{api : (api/)?}dataRequest/{requestId}/vote")
public class DataRequestVoteResource extends Resource {

    private final VoteAPI api;
    private final ElectionAPI electionAPI;
    private final EmailNotifierAPI emailAPI;

    public DataRequestVoteResource() {
        this.api = AbstractVoteAPI.getInstance();
        this.electionAPI = AbstractElectionAPI.getInstance();
        this.emailAPI = AbstractEmailNotifierAPI.getInstance();
    }

    @POST
    @Consumes("application/json")
    @Path("/{id}")
    public Response createDataRequestVote(@Context UriInfo info, Vote rec,
                                          @PathParam("requestId") String requestId,
                                          @PathParam("id") Integer voteId) {
        try {
            Vote vote = api.firstVoteUpdate(rec, voteId);
            if(electionAPI.validateCollectDAREmailCondition(vote)){
                try {
                    emailAPI.sendCollectMessage(vote.getElectionId());
                } catch (MessagingException | IOException | TemplateException e) {
                    e.printStackTrace();
                }
            }
            URI uri = info.getRequestUriBuilder().path("{id}").build(vote.getVoteId());
            return Response.ok(uri).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Status.BAD_REQUEST)
                    .entity(e.getMessage()).build();
        } catch (Exception e) {
            throw new NotFoundException(String.format(
                    "Could not find vote with id %s", voteId));
        }
    }

    @POST
    @Consumes("application/json")
    @Produces("application/json")
    @Path("/{id}/final")
    public Response updateFinalAccessConsentVote(@Context UriInfo info, Vote rec,
                                                 @PathParam("requestId") String requestId, @PathParam("id") Integer id) {
        try {
            Vote vote = api.firstVoteUpdate(rec, id);
            List<Vote> votes = vote.getType().equals(VoteType.FINAL.getValue()) ? api.describeVoteByTypeAndElectionId(VoteType.AGREEMENT.getValue(), vote.getElectionId()) :  api.describeVoteByTypeAndElectionId(VoteType.FINAL.getValue(), vote.getElectionId());
            if(vote.getVote() != null && votes.get(0).getVote() != null){
                electionAPI.updateFinalAccessVoteDataRequestElection(rec.getElectionId());
            }
            return Response.ok(vote).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    @PUT
    @Consumes("application/json")
    @Produces("application/json")
    @Path("/{id}")
    public Response updateDataRequestVote(@Context UriInfo info, Vote rec,
                                          @PathParam("requestId") String requestId, @PathParam("id") Integer id) {
        try {
            Vote vote = api.updateVote(rec, id, requestId);
            return Response.ok(vote).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }


    @GET
    @Produces("application/json")
    @Path("/{id}")
    public Vote describe(@PathParam("requestId") String requestId,
                         @PathParam("id") Integer id) {
        return api.describeVoteById(id, requestId);
    }

    @GET
    @Produces("application/json")
    @Path("/final")
    public Vote describeFinalAccessVote(@PathParam("requestId") Integer requestId){
        return api.describeVoteFinalAccessVoteById(requestId);

    }

    @GET
    @Produces("application/json")
    public List<Vote> describeAllVotes(@PathParam("requestId") String requestId) {
        return api.describeVotes(requestId);

    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id}")
    public Response deleteVote(@PathParam("requestId") String requestId, @PathParam("id") Integer id) {
        try {
            api.deleteVote(id, requestId);
            return Response.status(Response.Status.OK).entity("Vote was deleted").build();
        } catch (Exception e) {
            throw new NotFoundException(String.format(
                    "Could not find vote with id %s", id));
        }
    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteVotes(@PathParam("requestId") String requestId) {
        try {
            if (requestId == null)
                return Response.status(Response.Status.BAD_REQUEST).build();
            api.deleteVotes(requestId);
            return Response.ok().entity("Votes for specified id have been deleted").build();
        } catch (Exception e) {
            throw new NotFoundException();
        }
    }

}
