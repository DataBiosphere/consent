package org.broadinstitute.consent.http.resources;

import com.google.inject.Inject;
import freemarker.template.TemplateException;
import org.broadinstitute.consent.http.models.Vote;
import org.broadinstitute.consent.http.service.AbstractVoteAPI;
import org.broadinstitute.consent.http.service.ElectionService;
import org.broadinstitute.consent.http.service.EmailNotifierService;
import org.broadinstitute.consent.http.service.VoteAPI;

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
import java.io.IOException;
import java.util.logging.Logger;


@Path("{api : (api/)?}consent/{consentId}/vote")
public class ConsentVoteResource extends Resource {

    private final EmailNotifierService emailNotifierService;
    private final VoteAPI api;
    private final ElectionService electionService;
    private static final Logger logger = Logger.getLogger(ConsentVoteResource.class.getName());

    @Inject
    public ConsentVoteResource(EmailNotifierService emailNotifierService, ElectionService electionService) {
        this.emailNotifierService = emailNotifierService;
        this.api = AbstractVoteAPI.getInstance();
        this.electionService = electionService;
    }

    @POST
    @Consumes("application/json")
    @Path("/{id}")
    @RolesAllowed({MEMBER, CHAIRPERSON, DATAOWNER})
    public Response firstVoteUpdate(Vote rec,
                                    @PathParam("consentId") String consentId, @PathParam("id") Integer voteId){
        try {
            Vote vote = api.updateVoteById(rec, voteId);
            if(electionService.validateCollectEmailCondition(vote)){
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

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id}")
    @RolesAllowed(ADMIN)
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
    @RolesAllowed(ADMIN)
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
