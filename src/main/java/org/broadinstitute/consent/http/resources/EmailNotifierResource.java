package org.broadinstitute.consent.http.resources;

import org.broadinstitute.consent.http.service.AbstractEmailNotifierAPI;
import org.broadinstitute.consent.http.service.EmailNotifierAPI;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

@Path("{api : (api/)?}emailNotifier")
public class EmailNotifierResource extends Resource {

    EmailNotifierAPI emailApi;

    public EmailNotifierResource(){
        this.emailApi = AbstractEmailNotifierAPI.getInstance();
    }

    @POST
    @Path("/reminderMessage/{voteId}")
    @RolesAllowed({"ADMIN", "CHAIRPERSON"})
    public Response sendReminderMessage(@PathParam("voteId") String voteId) {
        try {
            emailApi.sendReminderMessage(Integer.valueOf(voteId));
            return Response.ok().build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }
}
