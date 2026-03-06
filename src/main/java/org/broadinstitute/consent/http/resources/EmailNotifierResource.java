package org.broadinstitute.consent.http.resources;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import java.util.concurrent.ExecutorService;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.service.DataAccessRequestService;
import org.broadinstitute.consent.http.service.EmailService;
import org.broadinstitute.consent.http.util.ThreadUtils;

@Path("api/emailNotifier")
public class EmailNotifierResource extends Resource {

  private final DataAccessRequestService dataAccessRequestService;
  private final EmailService emailService;
  @VisibleForTesting
  protected final ExecutorService executor;

  @Inject
  public EmailNotifierResource(
      DataAccessRequestService dataAccessRequestService, EmailService emailService) {
    this.dataAccessRequestService = dataAccessRequestService;
    this.emailService = emailService;
    this.executor = new ThreadUtils().getExecutorService(EmailNotifierResource.class);
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
  @Path("/dailyMessages")
  @RolesAllowed({SERVICE_ACCOUNT})
  public Response sendDailyMessages(@Auth AuthUser authUser) {
    executor.submit(this::processExpirationNotices);
    executor.submit(this::processVoteDigestMessages);
    executor.submit(this::processNewDatasetInDUOSNotifications);
    return Response.ok().build();
  }

  private void processNewDatasetInDUOSNotifications() {
    try {
      emailService.sendNewDatasetInDUOSNotifications();
    } catch (Exception e) {
      logWarn("Failed to send new dataset in duos messages", e);
    }
  }

  private void processExpirationNotices() {
    try {
      dataAccessRequestService.sendExpirationNotices();
    } catch (Exception e) {
      logWarn("Error encountered when processing Expiration notices", e);
    }
  }

  private void processVoteDigestMessages() {
    try {
      emailService.sendVoteDigestMessages();
    } catch (Exception e) {
      logWarn("Error encountered when processing vote reminder notices", e);
    }
  }
}
