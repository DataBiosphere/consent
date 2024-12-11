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
import org.broadinstitute.consent.http.models.support.SupportTicket;
import org.broadinstitute.consent.http.util.ConsentLogger;
import org.broadinstitute.consent.http.util.HttpClientUtil;

public class SupportRequestService implements ConsentLogger {

  private final HttpClientUtil clientUtil;
  private final ServicesConfiguration configuration;

  @Inject
  public SupportRequestService(ServicesConfiguration configuration) {
    this.clientUtil = new HttpClientUtil(configuration);
    this.configuration = configuration;
  }

  /**
   * Posts the given SupportTicket as JSON to the Support Request API if notifications are enabled
   *
   * @param ticket SupportTicket to be sent to support application
   * @throws Exception if an error occurs while posting the HttpRequest
   */
  public void postTicketToSupport(SupportTicket ticket) throws Exception {
    if (configuration.isActivateSupportNotifications()) {
      GenericUrl genericUrl = new GenericUrl(configuration.postSupportRequestUrl());
      String ticketJson = ticket.toString();
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
