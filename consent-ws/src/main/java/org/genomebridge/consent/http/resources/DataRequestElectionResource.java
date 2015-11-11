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

@Path("{api : (api/)?}dataRequest/{requestId}/election")
public class DataRequestElectionResource extends Resource {

    private final ElectionAPI api;
    private final VoteAPI voteAPI;
    private final EmailNotifierAPI emailApi;

    public DataRequestElectionResource() {
        this.api = AbstractElectionAPI.getInstance();
        this.voteAPI = AbstractVoteAPI.getInstance();
        this.emailApi = AbstractEmailNotifierAPI.getInstance();
    }

    @POST
    @Consumes("application/json")
    public Response createDataRequestElection(@Context UriInfo info, Election rec,
                                              @PathParam("requestId") String requestId) {
        URI uri;
        Election accessElection;
        try {
            accessElection = api.createElection(rec, requestId.toString(), ElectionType.DATA_ACCESS);
            List<Vote> votes = voteAPI.createVotes(accessElection.getElectionId(), ElectionType.DATA_ACCESS);
            emailApi.sendNewCaseMessageToList(votes.stream().filter(vote -> vote.getType().equals("DAC")).collect(Collectors.toList()), accessElection);
            //create RP election
            Election rpElection = api.createElection(rec, requestId.toString(), ElectionType.RP);
            voteAPI.createVotes(rpElection.getElectionId(), ElectionType.RP);
            uri = info.getRequestUriBuilder().build();
        } catch (NotFoundException e){
            return Response.status(Status.NOT_FOUND).entity(e.getMessage()).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (Exception e){
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
        return Response.created(uri).entity(accessElection).build();
    }


    @GET
    @Produces("application/json")
    public Response describe(@PathParam("requestId") String requestId) {
        try {
            return  Response.status(Status.OK).entity(api.describeDataRequestElection(requestId)).build();
        } catch (Exception e) {
            return Response.status(Status.NOT_FOUND).entity(e.getMessage()).build();
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

}
