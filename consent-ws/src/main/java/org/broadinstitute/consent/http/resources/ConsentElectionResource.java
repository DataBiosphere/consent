package org.broadinstitute.consent.http.resources;

import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.Vote;
import org.broadinstitute.consent.http.models.dto.Error;
import org.broadinstitute.consent.http.service.*;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
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
    @RolesAllowed("ADMIN")
    public Response createConsentElection(@Context UriInfo info, Election rec,
                                          @PathParam("consentId") String consentId) {
        URI uri;
        try {
            Election election = api.createElection(rec, consentId, ElectionType.TRANSLATE_DUL);
            logger().info("Election ID: " + election.getElectionId());
            List<Vote> votes  = voteAPI.createVotes(election.getElectionId(), ElectionType.TRANSLATE_DUL, false);
            votes.forEach(vote -> {
                logger().info("Translate Vote ID: " + vote.getVoteId());
            });
            List<Vote> dulVotes = votes.stream().filter(vote -> vote.getType().equals("DAC")).collect(Collectors.toList());
            dulVotes.forEach(vote -> {
                logger().info("DAC Vote ID: " + vote.getVoteId());
            });
            try {
                emailApi.sendNewCaseMessageToList(dulVotes, election);
            } catch (NullPointerException e) {
                logger().error("Email API is null");
                throw e;
            }
            uri = info.getRequestUriBuilder().build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
        return Response.created(uri).build();
    }

    @GET
    @Produces("application/json")
    @PermitAll
    public Response describe(@PathParam("consentId") String consentId) {
        try {
            return Response.status(Status.OK).entity(api.describeConsentElection(consentId)).build();
        } catch (Exception e) {
            return Response.status(Status.NOT_FOUND).entity(new Error(e.getMessage(), Status.NOT_FOUND.getStatusCode())).build();
        }
    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id}")
    @RolesAllowed("ADMIN")
    public Response deleteElection(@PathParam("consentId") String consentId, @Context UriInfo info, @PathParam("id") Integer id) {
        try {
            api.deleteElection(consentId, id);
            return Response.status(Response.Status.OK).entity("Election was deleted").build();
        } catch (Exception e) {
            return Response.status(Status.NOT_FOUND).entity(new Error(e.getMessage(), Status.NOT_FOUND.getStatusCode())).build();
        }
    }


}
