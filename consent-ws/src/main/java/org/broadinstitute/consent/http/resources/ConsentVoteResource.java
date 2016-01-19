package org.broadinstitute.consent.http.resources;

import org.broadinstitute.consent.http.models.dto.DefaultErrorMessage;
import org.broadinstitute.consent.http.service.VoteAPI;
import org.broadinstitute.consent.http.service.AbstractVoteAPI;
import org.broadinstitute.consent.http.service.AbstractEmailNotifierAPI;
import org.broadinstitute.consent.http.service.AbstractElectionAPI;
import org.broadinstitute.consent.http.service.ElectionAPI;
import org.broadinstitute.consent.http.service.EmailNotifierAPI;
import freemarker.template.TemplateException;
import org.broadinstitute.consent.http.models.Vote;
import org.broadinstitute.consent.http.models.dto.Error;

import javax.mail.MessagingException;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
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
    public Response firstVoteUpdate(@Context UriInfo info, Vote rec,
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
        } catch (IllegalArgumentException e) {
            return Response.status(Status.BAD_REQUEST).entity(new Error(e.getMessage(), Status.BAD_REQUEST.getStatusCode())).build();
        } catch (NullPointerException e) {
            return Response.status(Status.NOT_FOUND).entity(new Error(String.format("Could not find vote with id %s", voteId), Status.NOT_FOUND.getStatusCode())).build();
        }catch(Exception e){
            return Response.serverError().entity(new Error( DefaultErrorMessage.INTERNAL_SERVER_ERROR.getMessage() , Status.INTERNAL_SERVER_ERROR.getStatusCode())).build();
        }
    }

    @PUT
    @Consumes("application/json")
    @Produces("application/json")
    @Path("/{id}")
    public Response updateConsentVote(@Context UriInfo info, Vote rec,
                                      @PathParam("consentId") String consentId, @PathParam("id") Integer id) {
        try {
            Vote vote = api.updateVote(rec, id, consentId);
            return Response.ok(vote).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Status.BAD_REQUEST).entity(new Error(e.getMessage(), Status.BAD_REQUEST.getStatusCode())).build();
        }catch(Exception e){
            return Response.serverError().entity(new Error( DefaultErrorMessage.INTERNAL_SERVER_ERROR.getMessage() , Status.INTERNAL_SERVER_ERROR.getStatusCode())).build();
        }
    }

    @GET
    @Produces("application/json")
    @Path("/{id}")
    public Vote describe(@PathParam("consentId") String consentId,
                         @PathParam("id") Integer id) {
        return api.describeVoteById(id, consentId);
    }

    @GET
    @Produces("application/json")
    public List<Vote> describeAllVotes(@PathParam("consentId") String consentId) {
        return api.describeVotes(consentId);
    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id}")
    public Response deleteVote(@PathParam("consentId") String consentId, @PathParam("id") Integer id) {
        try {
            api.deleteVote(id, consentId);
            return Response.status(Response.Status.OK).entity("Vote was deleted").build();
        } catch (NotFoundException e) {
            return Response.status(Status.NOT_FOUND).entity(new Error( e.getMessage(), Status.NOT_FOUND.getStatusCode())).build();
        } catch (Exception e) {
            return Response.serverError().entity(new Error(DefaultErrorMessage.INTERNAL_SERVER_ERROR.getMessage() , Status.INTERNAL_SERVER_ERROR.getStatusCode())).build();
        }
    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteVotes(@PathParam("consentId") String consentId) {
        try {
            if (consentId == null) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            api.deleteVotes(consentId);
            return Response.ok().entity("Votes for specified consent have been deleted").build();
        } catch (NotFoundException e) {
            return Response.status(Status.NOT_FOUND).entity(new Error( String.format(
                    "Could not find votes for specified consent id %s", consentId), Status.NOT_FOUND.getStatusCode())).build();
        }catch (Exception e) {
            return Response.serverError().entity(new Error( DefaultErrorMessage.INTERNAL_SERVER_ERROR.getMessage() , Status.INTERNAL_SERVER_ERROR.getStatusCode())).build();
        }
    }

    @OPTIONS
    @Produces(MediaType.APPLICATION_JSON)
    public Response options(@PathParam("consentId") String consentId) {
        return Response.ok()
                .build();
    }

}
