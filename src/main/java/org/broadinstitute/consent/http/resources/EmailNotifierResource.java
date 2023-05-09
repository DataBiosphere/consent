package org.broadinstitute.consent.http.resources;

import com.google.inject.Inject;
import org.broadinstitute.consent.http.service.EmailService;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

@Path("api/emailNotifier")
public class EmailNotifierResource extends Resource {

    private final EmailService emailService;

    @Inject
    public EmailNotifierResource(EmailService emailService) {
        this.emailService = emailService;
    }

    @POST
    @Path("/reminderMessage/{voteId}")
    @RolesAllowed({ADMIN, CHAIRPERSON})
    public Response sendReminderMessage(@PathParam("voteId") String voteId) {
        try {
            emailService.sendReminderMessage(Integer.valueOf(voteId));
            return Response.ok().build();
        } catch (Exception e) {
            return createExceptionResponse(e);
        }
    }
}
