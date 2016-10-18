package org.broadinstitute.consent.http.resources;

import freemarker.template.TemplateException;
import org.broadinstitute.consent.http.models.Vote;
import org.broadinstitute.consent.http.service.*;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.mail.MessagingException;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;


@Path("{api : (api/)?}consent/{consentId}/vote")
public class ConsentVoteResource extends Resource {

    private final VoteAPI api;
    private final ElectionAPI electionAPI;
    private final EmailNotifierAPI emailAPI;
    private static final Logger logger = Logger.getLogger(ConsentVoteResource.class.getName());

    public ConsentVoteResource() {
        this.api = AbstractVoteAPI.getInstance();
        this.electionAPI = AbstractElectionAPI.getInstance();
        this.emailAPI = AbstractEmailNotifierAPI.getInstance();
    }

    @POST
    @Consumes("application/json")
    @Path("/{id}")
    @RolesAllowed({"MEMBER", "CHAIRPERSON", "DATAOWNER"})
    public Response firstVoteUpdate(Vote rec,
                                    @PathParam("consentId") String consentId, @PathParam("id") Integer voteId){
        try {
            Vote vote = api.firstVoteUpdate(rec, voteId);
            if(electionAPI.validateCollectEmailCondition(vote)){
                try {
                    emailAPI.sendCollectMessage(vote.getElectionId());
                } catch (MessagingException | IOException | TemplateException e) {
                    logger.severe("Error when sending email notification to Chaiperson to collect votes. Cause: "+e);
                }
            }
            return Response.ok(vote).build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }

    @PUT
    @Consumes("application/json")
    @Produces("application/json")
    @Path("/{id}")
    @RolesAllowed({"MEMBER", "CHAIRPERSON", "DATAOWNER"})
    public Response updateConsentVote(Vote rec,
                                      @PathParam("consentId") String consentId, @PathParam("id") Integer id) {
        try {
            Vote vote = api.updateVote(rec, id, consentId);
            return Response.ok(vote).build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }

    @GET
    @Produces("application/json")
    @Path("/{id}")
    @PermitAll
    public Vote describe(@PathParam("consentId") String consentId,
                         @PathParam("id") Integer id) {
        return api.describeVoteById(id, consentId);
    }

    @GET
    @Produces("application/json")
    @PermitAll
    public List<Vote> describeAllVotes(@PathParam("consentId") String consentId) {
        return api.describeVotes(consentId);
    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id}")
    @RolesAllowed("ADMIN")
    public Response deleteVote(@PathParam("consentId") String consentId, @PathParam("id") Integer id) {
        try {
            api.deleteVote(id, consentId);
            return Response.status(Response.Status.OK).entity("Vote was deleted").build();
        }  catch (Exception e) {
            return createExceptionResponse(e);
        }
    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("ADMIN")
    public Response deleteVotes(@PathParam("consentId") String consentId) {
        try {
            if (consentId == null) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            api.deleteVotes(consentId);
            return Response.ok().entity("Votes for specified consent have been deleted").build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }

    @OPTIONS
    @Produces(MediaType.APPLICATION_JSON)
    public Response options(@PathParam("consentId") String consentId) {
        return Response.ok()
                .build();
    }

}
