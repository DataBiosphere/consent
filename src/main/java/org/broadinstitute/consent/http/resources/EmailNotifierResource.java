package org.broadinstitute.consent.http.resources;

import com.google.inject.Inject;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import org.broadinstitute.consent.http.service.EmailService;

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
