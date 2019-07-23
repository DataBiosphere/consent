package org.broadinstitute.consent.http.resources;

import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.Vote;
import org.broadinstitute.consent.http.models.dto.Error;
import org.broadinstitute.consent.http.service.AbstractElectionAPI;
import org.broadinstitute.consent.http.service.AbstractVoteAPI;
import org.broadinstitute.consent.http.service.ElectionAPI;
import org.broadinstitute.consent.http.service.VoteAPI;

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
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Path("{api : (api/)?}election/")
public class ElectionResource extends Resource {

    private final ElectionAPI api;
    private final VoteAPI voteApi;

    public ElectionResource() {
        this.api = AbstractElectionAPI.getInstance();
        this.voteApi = AbstractVoteAPI.getInstance();
    }

    @POST
    @Consumes("application/json")
    @Produces("application/json")
    @Path("/{referenceId}/advance/{vote}")
    @RolesAllowed({ADMIN})
    public Response advanceElection(@PathParam("referenceId") String referenceId, @PathParam("vote") String vote) {
        try {
            List<Vote> voteList = voteApi.describeVotes(referenceId);
            List<Integer> electionIds = voteList.stream().map(Vote::getElectionId).collect(Collectors.toList());
            Date now = new Date();
            Boolean voteValue = vote.equalsIgnoreCase("yes");
            voteList.
                    stream().
                    filter(v -> v.getVote() == null).
                    filter(v -> v.getType().equalsIgnoreCase(VoteType.DAC.getValue())).
                    forEach(v -> {
                        v.setCreateDate(now);
                        v.setVote(voteValue);
                        v.setRationale("Advanced by administrator");
                        voteApi.updateVote(v, v.getVoteId(), referenceId);
                    });
            electionIds.forEach(id -> {
                if (api.checkDataOwnerToCloseElection(id)) {
                    api.closeDataOwnerApprovalElection(id);
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
            return Response.ok().entity(api.updateElectionById(rec, id)).build();
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
            return Response.ok().entity(api.describeElectionById(id)).build();
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
            return Response.ok().entity(api.describeElectionByVoteId(id)).build();
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
            return Response.ok().entity("{ \"open\" : " + api.isDataSetElectionOpen() + " }").build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }

    @GET
    @Produces("application/json")
    @Path("/consent/{requestElectionId}")
    @PermitAll
    public Response describeConsentElectionByDARElectionId(@PathParam("requestElectionId") Integer requestElectionId) {
        try {
            return  Response.status(Response.Status.OK).entity(api.getConsentElectionByDARElectionId(requestElectionId)).build();
        } catch (Exception e) {
            return Response.status(Response.Status.NOT_FOUND).entity(new Error(e.getMessage(), Response.Status.NOT_FOUND.getStatusCode())).build();
        }
    }


}