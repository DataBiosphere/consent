package org.genomebridge.consent.http.resources;

import org.genomebridge.consent.http.models.Election;
import org.genomebridge.consent.http.service.AbstractElectionAPI;
import org.genomebridge.consent.http.service.AbstractVoteAPI;
import org.genomebridge.consent.http.service.ElectionAPI;
import org.genomebridge.consent.http.service.VoteAPI;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

@Path("consent/{consentId}/election")
public class ConsentElectionResource extends Resource {

    private ElectionAPI api;
    private VoteAPI voteAPI;

    public ConsentElectionResource() {
        this.api = AbstractElectionAPI.getInstance();
        this.voteAPI = AbstractVoteAPI.getInstance();
    }

    @POST
    @Consumes("application/json")
    public Response createConsentElection(@Context UriInfo info, Election rec,
                                          @PathParam("consentId") String consentId) {
        URI uri;
        try {
            Election election = api.createElection(rec, consentId, true);
            voteAPI.createVotes(election.getElectionId(), true);
            uri = info.getRequestUriBuilder().build();
        } catch (IllegalArgumentException e) {
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
        return Response.created(uri).build();
    }

    @PUT
    @Consumes("application/json")
    @Produces("application/json")
    @Path("/{id}")
    public Response updateConsentElection(@Context UriInfo info, Election rec,
                                          @PathParam("consentId") String consentId, @PathParam("id") Integer id) {
        try {
            Election election = api.updateElectionById(rec, id);
            URI uri = info.getRequestUriBuilder().build(ConsentElectionResource.class);
            return Response.ok(election).location(uri).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    @GET
    @Produces("application/json")
    public Election describe(@PathParam("consentId") String consentId) {
        try {
            return api.describeConsentElection(consentId);
        } catch (Exception e) {
            throw new NotFoundException("Invalid id:" + consentId);
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
            throw new NotFoundException(e.getMessage());
        }
    }


}
