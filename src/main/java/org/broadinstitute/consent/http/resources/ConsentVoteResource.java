package org.broadinstitute.consent.http.resources;

import com.google.inject.Inject;
import freemarker.template.TemplateException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.mail.MessagingException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.broadinstitute.consent.http.models.Vote;
import org.broadinstitute.consent.http.service.AbstractElectionAPI;
import org.broadinstitute.consent.http.service.ElectionAPI;
import org.broadinstitute.consent.http.service.EmailNotifierService;
import org.broadinstitute.consent.http.service.VoteService;


@Path("{api : (api/)?}consent/{consentId}/vote")
public class ConsentVoteResource extends Resource {

    private final EmailNotifierService emailNotifierService;
    private final ElectionAPI electionAPI;
    private final VoteService voteService;
    private static final Logger logger = Logger.getLogger(ConsentVoteResource.class.getName());

    @Inject
    public ConsentVoteResource(EmailNotifierService emailNotifierService, VoteService voteService) {
        this.emailNotifierService = emailNotifierService;
        this.electionAPI = AbstractElectionAPI.getInstance();
        this.voteService = voteService;
    }

    @POST
    @Consumes("application/json")
    @Path("/{id}")
    @RolesAllowed({MEMBER, CHAIRPERSON, DATAOWNER})
    public Response firstVoteUpdate(Vote rec,
                                    @PathParam("consentId") String consentId, @PathParam("id") Integer voteId){
        try {
            Vote vote = voteService.firstVoteUpdate(rec, voteId);
            if(electionAPI.validateCollectEmailCondition(vote)){
                try {
                    emailNotifierService.sendCollectMessage(vote.getElectionId());
                } catch (MessagingException | IOException | TemplateException e) {
                    logger.severe("Error when sending email notification to Chairpersons to collect votes. Cause: " + e);
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
    @RolesAllowed({MEMBER, CHAIRPERSON, DATAOWNER})
    public Response updateConsentVote(Vote rec,
                                      @PathParam("consentId") String consentId, @PathParam("id") Integer id) {
        try {
            Vote updatedVote = voteService.updateVote(rec);
            return Response.ok(updatedVote).build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }

    @GET
    @Produces("application/json")
    @Path("/{id}")
    @PermitAll
    public Response describe(@PathParam("consentId") String consentId,
                         @PathParam("id") Integer id) {
        try {
            Vote vote = voteService.findVoteById(id);
            return Response.ok(vote).build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }

    @GET
    @Produces("application/json")
    @PermitAll
    public Response describeAllVotes(@PathParam("consentId") String consentId) {
        Collection<Vote> votes = voteService.findVotesByReferenceId(consentId);
        return Response.ok(votes).build();
    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id}")
    @RolesAllowed(ADMIN)
    public Response deleteVote(@PathParam("consentId") String consentId, @PathParam("id") Integer id) {
        try {
            voteService.deleteVote(id);
            return Response.status(Response.Status.OK).entity("Vote was deleted").build();
        }  catch (Exception e) {
            return createExceptionResponse(e);
        }
    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(ADMIN)
    public Response deleteVotes(@PathParam("consentId") String consentId) {
        try {
            if (consentId == null) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            voteService.deleteVotes(consentId);
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
