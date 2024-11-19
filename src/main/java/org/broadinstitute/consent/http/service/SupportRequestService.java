package org.broadinstitute.consent.http.service;

import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpStatusCodes;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import jakarta.ws.rs.ServerErrorException;
import java.nio.charset.StandardCharsets;
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.support.SupportTicket;
import org.broadinstitute.consent.http.util.ConsentLogger;
import org.broadinstitute.consent.http.util.HttpClientUtil;

public class SupportRequestService implements ConsentLogger {

  private final HttpClientUtil clientUtil;
  private final ServicesConfiguration configuration;
  private final UserDAO userDAO;

  @Inject
  public SupportRequestService(ServicesConfiguration configuration,
      UserDAO userDAO) {
    this.clientUtil = new HttpClientUtil(configuration);
    this.configuration = configuration;
    this.userDAO = userDAO;
  }

  /**
   * Posts the given SupportTicket as JSON to the Support Request API if notifications are enabled
   *
   * @param ticket SupportTicket to be sent to support application
   * @throws Exception if an error occurs while posting the HttpRequest
   */
  public void postTicketToSupport(SupportTicket ticket) throws Exception {
    if (configuration.isActivateSupportNotifications()) {
      if (ticket.getRequest() != null && ticket.getRequest().getRequester() != null && ticket.getRequest().getRequester().getEmail() != null) {
        User user = userDAO.findUserByEmail(ticket.getRequest().getRequester().getEmail());
        if (user == null) {
          logWarn("Unknown user submitting a support request: " + ticket.getRequest().getRequester().getEmail());
        }
      }
      GenericUrl genericUrl = new GenericUrl(configuration.postSupportRequestUrl());
      //Using GsonBuilder directly to convert ticket to json since GsonFactory does not allow custom FieldNamingPolicy
      String ticketJson = new GsonBuilder()
          .setPrettyPrinting()
          .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
          .create()
          .toJson(ticket);
      ByteArrayContent content = new ByteArrayContent("application/json",
          ticketJson.getBytes(StandardCharsets.UTF_8));
      HttpRequest request = clientUtil.buildUnAuthedPostRequest(genericUrl, content);
      HttpResponse response = clientUtil.handleHttpRequest(request);

      if (!response.isSuccessStatusCode()) {
        String errorMessage = "Error posting ticket to support: " + response.getStatusMessage();
        var errorException = new ServerErrorException(response.getStatusMessage(),
            HttpStatusCodes.STATUS_CODE_SERVER_ERROR);
        logException(errorMessage, errorException);
        throw errorException;
      }
    } else {
      logDebug("Not configured to send support requests");
    }
  }

}
