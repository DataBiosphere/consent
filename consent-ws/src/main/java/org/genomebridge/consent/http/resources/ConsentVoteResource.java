package org.genomebridge.consent.http.resources;

import freemarker.template.TemplateException;
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
import java.util.List;

@Path("{api : (api/)?}consent/{consentId}/vote")
public class ConsentVoteResource extends Resource {

    private final VoteAPI api;
    private final ElectionAPI electionAPI;
    private final EmailNotifierAPI emailAPI;

    public ConsentVoteResource() {
        this.api = AbstractVoteAPI.getInstance();
        this.electionAPI = AbstractElectionAPI.getInstance();
        this.emailAPI = AbstractEmailNotifierAPI.getInstance();
    }

    @POST
    @Consumes("application/json")
    @Path("/{id}")
    public Response firstVoteUpdate(@Context UriInfo info, Vote rec,
                                    @PathParam("consentId") String consentId, @PathParam("id") Integer voteId){
        try {
            Vote vote = api.firstVoteUpdate(rec, voteId);
            if(electionAPI.validateCollectEmailCondition(vote)){
                try {
                    emailAPI.sendCollectMessage(vote.getElectionId());
                } catch (MessagingException | IOException | TemplateException e) {
                    e.printStackTrace();
                }
            }
            return Response.ok(vote).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (Exception e) {
            throw new NotFoundException(String.format(
                    "Could not find vote with id %s", voteId));
        }
    }

    @PUT
    @Consumes("application/json")
    @Produces("application/json")
    @Path("/{id}")
    public Response updateConsentVote(@Context UriInfo info, Vote rec,
                                      @PathParam("consentId") String consentId, @PathParam("id") Integer id) {
        try {
            Vote vote = api.updateVote(rec, id, consentId);
            return Response.ok(vote).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    @GET
    @Produces("application/json")
    @Path("/{id}")
    public Vote describe(@PathParam("consentId") String consentId,
                         @PathParam("id") Integer id) {
        return api.describeVoteById(id, consentId);
    }

    @GET
    @Produces("application/json")
    public List<Vote> describeAllVotes(@PathParam("consentId") String consentId) {
        return api.describeVotes(consentId);
    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id}")
    public Response deleteVote(@PathParam("consentId") String consentId, @PathParam("id") Integer id) {
        try {
            api.deleteVote(id, consentId);
            return Response.status(Response.Status.OK).entity("Vote was deleted").build();
        } catch (Exception e) {
            throw new NotFoundException(String.format(
                    "Could not find vote with id %s", id));
        }
    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteVotes(@PathParam("consentId") String consentId) {
        try {
            if (consentId == null) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            api.deleteVotes(consentId);
            return Response.ok().entity("Votes for specified consent have been deleted").build();
        } catch (Exception e) {
            throw new NotFoundException(String.format(
                    "Could not find votes for specified consent id %s", consentId));
        }
    }

    @OPTIONS
    @Produces(MediaType.APPLICATION_JSON)
    public Response options(@PathParam("consentId") String consentId) {
        return Response.ok()
                .build();
    }

}
