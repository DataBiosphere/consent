package org.genomebridge.consent.http.resources;

import org.genomebridge.consent.http.enumeration.ElectionType;
import org.genomebridge.consent.http.models.Election;
import org.genomebridge.consent.http.models.Vote;
import org.genomebridge.consent.http.service.*;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@Path("{api : (api/)?}consent/{consentId}/election")
public class ConsentElectionResource extends Resource {

    private final ElectionAPI api;
    private final VoteAPI voteAPI;
    private final EmailNotifierAPI emailApi;

    public ConsentElectionResource() {
        this.api = AbstractElectionAPI.getInstance();
        this.voteAPI = AbstractVoteAPI.getInstance();
        this.emailApi = AbstractEmailNotifierAPI.getInstance();
    }

    @POST
    @Consumes("application/json")
    public Response createConsentElection(@Context UriInfo info, Election rec,
                                          @PathParam("consentId") String consentId) {
        URI uri;
        try {
            Election election = api.createElection(rec, consentId, ElectionType.TRANSLATE_DUL);
            List<Vote> votes  = voteAPI.createVotes(election.getElectionId(), ElectionType.TRANSLATE_DUL);
            emailApi.sendNewCaseMessageToList(votes.stream().map(Vote::getVoteId).collect(Collectors.toList()));
            uri = info.getRequestUriBuilder().build();
        } catch (IllegalArgumentException e) {
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (NotFoundException e){
            return Response.status(Status.NOT_FOUND).entity(e.getMessage()).build();
        } catch (Exception e){
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
        return Response.created(uri).build();
    }

    @GET
    @Produces("application/json")
    public Response describe(@PathParam("consentId") String consentId) {
        try {
            return Response.status(Status.OK).entity(api.describeConsentElection(consentId)).build();
        } catch (Exception e) {
            return Response.status(Status.NOT_FOUND).entity(e.getMessage()).build();
        }
    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id}")
    public Response deleteElection(@PathParam("consentId") String consentId, @Context UriInfo info, @PathParam("id") Integer id) {
        try {
            api.deleteElection(consentId, id);
            return Response.status(Response.Status.OK).entity("Election was deleted").build();
        } catch (Exception e) {
            return Response.status(Status.NOT_FOUND).entity(e.getMessage()).build();
        }
    }


}
