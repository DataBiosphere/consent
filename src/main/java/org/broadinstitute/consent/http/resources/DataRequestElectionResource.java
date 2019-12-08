package org.broadinstitute.consent.http.resources;

import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.Vote;
import org.broadinstitute.consent.http.models.dto.Error;
import org.broadinstitute.consent.http.service.AbstractDataAccessRequestAPI;
import org.broadinstitute.consent.http.service.AbstractElectionAPI;
import org.broadinstitute.consent.http.service.AbstractEmailNotifierAPI;
import org.broadinstitute.consent.http.service.AbstractSummaryAPI;
import org.broadinstitute.consent.http.service.AbstractVoteAPI;
import org.broadinstitute.consent.http.service.DataAccessRequestAPI;
import org.broadinstitute.consent.http.service.ElectionAPI;
import org.broadinstitute.consent.http.service.EmailNotifierAPI;
import org.broadinstitute.consent.http.service.SummaryAPI;
import org.broadinstitute.consent.http.service.VoteAPI;
import org.broadinstitute.consent.http.util.DarConstants;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
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
    @RolesAllowed({ADMIN, CHAIRPERSON})
    public Response createDataRequestElection(@Context UriInfo info, Election rec,
                                              @PathParam("requestId") String requestId) {
        URI uri;
        Election accessElection;
        try {
            accessElection = api.createElection(rec, requestId, ElectionType.DATA_ACCESS);
            List<Vote> votes;
            //create RP election
            if (!Objects.isNull(darApi.getField(requestId, DarConstants.RESTRICTION))) {
                votes = voteAPI.createVotes(accessElection.getElectionId(), ElectionType.DATA_ACCESS, false);
                Election rpElection = api.createElection(rec, requestId, ElectionType.RP);
                voteAPI.createVotes(rpElection.getElectionId(), ElectionType.RP, false);
            } else {
                votes = voteAPI.createVotes(accessElection.getElectionId(), ElectionType.DATA_ACCESS, true);
            }
            List<Vote> darVotes = votes.stream().
                    filter(vote -> vote.getType().equals(VoteType.DAC.getValue())).
                    collect(Collectors.toList());
            emailApi.sendNewCaseMessageToList(darVotes, accessElection);
            uri = info.getRequestUriBuilder().build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
        return Response.created(uri).entity(accessElection).build();
    }


    @GET
    @Produces("application/json")
    @PermitAll
    public Response describe(@PathParam("requestId") String requestId) {
        try {
            return Response.status(Status.OK).entity(api.describeDataRequestElection(requestId)).build();
        } catch (Exception e) {
            return Response.status(Status.NOT_FOUND).entity(new Error(e.getMessage(), Status.NOT_FOUND.getStatusCode())).build();
        }
    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id}")
    @RolesAllowed({ADMIN, CHAIRPERSON})
    public Response deleteElection(@PathParam("requestId") String requestId, @PathParam("id") Integer id, @Context UriInfo info) {
        try {
            api.deleteElection(requestId, id);
            return Response.status(Response.Status.OK).entity("Election was deleted").build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }

    @GET
    @Produces("text/plain")
    @Path("/dataSetVotes")
    @PermitAll
    public Response describeDataSetVotes(@PathParam("requestId") String id) {
        Response.ResponseBuilder response;
        try {
            File fileToSend = summaryAPI.describeDataSetElectionsVotesForDar(id);
            if ((fileToSend != null)) {
                response = Response.ok(fileToSend);
            } else response = Response.ok();
            return response.build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }

}
