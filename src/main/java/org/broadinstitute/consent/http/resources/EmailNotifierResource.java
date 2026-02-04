package org.broadinstitute.consent.http.resources;

import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.service.DataAccessRequestService;
import org.broadinstitute.consent.http.service.EmailService;

@Path("api/emailNotifier")
public class EmailNotifierResource extends Resource {

  private final DataAccessRequestService dataAccessRequestService;
  private final EmailService emailService;

  @Inject
  public EmailNotifierResource(
      DataAccessRequestService dataAccessRequestService, EmailService emailService) {
    this.dataAccessRequestService = dataAccessRequestService;
    this.emailService = emailService;
  }

  @POST
  @Path("/reminderMessage/{voteId}")
  @RolesAllowed({ADMIN, CHAIRPERSON})
  public Response sendReminderMessage(@Auth AuthUser authUser, @PathParam("voteId") String voteId) {
    try {
      dataAccessRequestService.sendReminderMessage(Integer.valueOf(voteId));
      return Response.ok().build();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }
  }

  @GET
  @Path("/darExpirationNotices")
  @RolesAllowed({SERVICE_ACCOUNT})
  public Response sendDarExpirationNotices(@Auth AuthUser authUser) {
    try {
      dataAccessRequestService.sendExpirationNotices();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }

    return Response.ok().build();
  }

  @GET
  @Path("/dacVoteDigestNotices")
  @RolesAllowed({SERVICE_ACCOUNT})
  public Response sendDacVoteDigestNotices(@Auth AuthUser authUser) {
    try {
      emailService.sendVoteDigestMessages();
    } catch (Exception e) {
      return createExceptionResponse(e);
    }

    return Response.ok().build();
  }
}
