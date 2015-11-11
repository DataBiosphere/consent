package org.genomebridge.consent.http.resources;

import org.genomebridge.consent.http.service.AbstractEmailNotifierAPI;
import org.genomebridge.consent.http.service.EmailNotifierAPI;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

@Path("{api : (api/)?}emailNotifier")
public class EmailNotifierResource {

    EmailNotifierAPI emailApi;

    public EmailNotifierResource(){
        this.emailApi = AbstractEmailNotifierAPI.getInstance();
    }

    @POST
    @Path("/collectMessage/{electionId}")
    public Response sendCollectMessage(@PathParam("electionId") String electionId) {
        try {
            emailApi.sendCollectMessage(Integer.valueOf(electionId));
            return Response.ok().build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @POST
    @Path("/newCaseMessage/{voteId}")
    public Response sendNewCaseMessage(@PathParam("voteId") String voteId) {
        try {
            emailApi.sendNewCaseMessage(Integer.valueOf(voteId));
            return Response.ok().build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @POST
    @Path("/reminderMessage/{voteId}")
    public Response sendReminderMessage(@PathParam("voteId") String voteId) {
        try {
            emailApi.sendReminderMessage(Integer.valueOf(voteId));
            return Response.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }
}
