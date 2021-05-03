package org.broadinstitute.consent.http.resources;

import com.google.inject.Inject;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.Vote;
import org.broadinstitute.consent.http.service.ElectionService;
import org.broadinstitute.consent.http.service.VoteService;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Path("{api : (api/)?}election/")
public class ElectionResource extends Resource {

    private final ElectionService electionService;

    private final VoteService voteService;

    @Inject
    public ElectionResource(VoteService voteService, ElectionService electionService) {
        this.voteService = voteService;
        this.electionService = electionService;
    }

    @POST
    @Consumes("application/json")
    @Produces("application/json")
    @Path("/{referenceId}/advance/{vote}")
    @RolesAllowed({ADMIN})
    public Response advanceElection(@PathParam("referenceId") String referenceId, @PathParam("vote") String vote) {
        try {
            Collection<Vote> voteList = voteService.findVotesByReferenceId(referenceId).stream().
                    filter(v -> v.getVote() == null).
                    filter(v -> v.getType().equalsIgnoreCase(VoteType.DAC.getValue())).
                    collect(Collectors.toList());
            boolean voteValue = vote.equalsIgnoreCase("yes");
            voteService.advanceVotes(voteList, voteValue, "Advanced by administrator");
            Collection<Integer> electionIds = voteList.stream().
                    map(Vote::getElectionId).
                    collect(Collectors.toList());
            electionIds.forEach(id -> {
                if (electionService.checkDataOwnerToCloseElection(id)) {
                    electionService.closeDataOwnerApprovalElection(id);
                }
            });
            return Response.ok().build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }

    @PUT
    @Consumes("application/json")
    @Produces("application/json")
    @Path("/{id}")
    @RolesAllowed({ADMIN, DATAOWNER, CHAIRPERSON, MEMBER})
    public Response updateElection(Election rec, @PathParam("id") Integer id) {
        try {
            return Response.ok().entity(electionService.updateElectionById(rec, id)).build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }

    @GET
    @Consumes("application/json")
    @Produces("application/json")
    @Path("/{id}")
    @PermitAll
    public Response describeElectionById(@PathParam("id") Integer id) {
        try {
            return Response.ok().entity(electionService.describeElectionById(id)).build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }


    @GET
    @Consumes("application/json")
    @Produces("application/json")
    @Path("/vote/{voteId}")
    @PermitAll
    public Response describeElectionByVoteId(@PathParam("voteId") Integer id) {
        try {
            return Response.ok().entity(electionService.describeElectionByVoteId(id)).build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }

    @GET
    @Consumes("application/json")
    @Produces("application/json")
    @Path("/checkdataset")
    @PermitAll
    public Response isDataSetElectionOpen(@Context UriInfo info) {
        try {
            return Response.ok().entity("{ \"open\" : " + electionService.isDataSetElectionOpen() + " }").build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }

    @GET
    @Produces("application/json")
    @PermitAll
    @Path("/{electionId}")
    public Response describeVotesOnElection(@PathParam("electionId") Integer electionId) {
        try {
            return Response.ok().entity(voteService.findVotesByElectionId(electionId)).build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }

}