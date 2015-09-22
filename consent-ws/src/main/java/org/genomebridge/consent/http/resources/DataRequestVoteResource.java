package org.genomebridge.consent.http.resources;

import org.genomebridge.consent.http.models.Vote;
import org.genomebridge.consent.http.service.AbstractVoteAPI;
import org.genomebridge.consent.http.service.VoteAPI;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;

@Path("{api : (api/)?}dataRequest/{requestId}/vote")
public class DataRequestVoteResource extends Resource {

    private final VoteAPI api;

    public DataRequestVoteResource() {
        this.api = AbstractVoteAPI.getInstance();
    }

    @POST
    @Consumes("application/json")
    @Path("/{id}")
    public Response createDataRequestVote(@Context UriInfo info, Vote rec,
                                          @PathParam("requestId") String requestId,
                                          @PathParam("id") String voteId) {
        try {
            Vote vote = api.firstVoteUpdate(rec, requestId, voteId);
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
