package org.broadinstitute.consent.http.service;

import static org.broadinstitute.consent.http.configurations.ServicesConfiguration.BROAD_ZENDESK_URL;

import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpStatusCodes;
import com.google.common.net.MediaType;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.inject.Inject;
import jakarta.ws.rs.ServerErrorException;
import java.nio.charset.StandardCharsets;
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
import org.broadinstitute.consent.http.models.support.SupportTicket;
import org.broadinstitute.consent.http.util.ConsentLogger;
import org.broadinstitute.consent.http.util.HttpClientUtil;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.zendesk.client.v2.Zendesk;
import org.zendesk.client.v2.model.Attachment.Upload;
import org.zendesk.client.v2.model.Ticket;

public class SupportRequestService implements ConsentLogger {

  private final HttpClientUtil clientUtil;
  private final ServicesConfiguration configuration;
  private final Zendesk zendeskClient;

  @Inject
  public SupportRequestService(ServicesConfiguration configuration) {
    this.clientUtil = new HttpClientUtil(configuration);
    this.configuration = configuration;
    this.zendeskClient = new Zendesk.Builder(BROAD_ZENDESK_URL).build();
  }

  public Upload postZendeskAttachment(String fileName, byte[] content) {
    if (configuration.isActivateSupportNotifications()) {
      return zendeskClient.createUpload(fileName, MediaType.APPLICATION_BINARY.type(), content);
    } else {
      logDebug("Not configured to send support attachments");
    }
    return null;
  }

  public HttpResponse postZendeskTicket(Ticket ticket) throws Exception {
    if (configuration.isActivateSupportNotifications()) {
      GenericUrl genericUrl = new GenericUrl(configuration.postSupportRequestUrl());

      // A zendesk ticket needs to be wrapped in {"request": <Ticket json>} to be correctly sent
      DuosTicket duosTicket = new DuosTicket(ticket);
      ByteArrayContent content = new ByteArrayContent("application/json",
          duosTicket.toString().getBytes(StandardCharsets.UTF_8));
      HttpRequest request = clientUtil.buildUnAuthedPostRequest(genericUrl, content);
      HttpResponse response = clientUtil.handleHttpRequest(request);

      if (!response.isSuccessStatusCode()) {
        String errorMessage = "Error posting ticket to support: " + response.getStatusMessage();
        var errorException = new ServerErrorException(response.getStatusMessage(),
            HttpStatusCodes.STATUS_CODE_SERVER_ERROR);
        logException(errorMessage, errorException);
        throw errorException;
      }
      return response;
    } else {
      logDebug("Not configured to send support requests");
    }
    return null;
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

  /**
   * Wrapper around org.zendesk.client.v2.model.Ticket to do two things:
   * 1. Provide a top level field "request"
   * 2. Ignore the Ticket.hasIncidents field on serialization
   */
  private static class DuosTicket {
    private final Ticket request;
    private final ExclusionStrategy strategy = new ExclusionStrategy() {
      @Override
      public boolean shouldSkipField(FieldAttributes f) {
        return f.getName().equals("hasIncidents");
      }
      @Override
      public boolean shouldSkipClass(Class<?> aClass) {
        return false;
      }
    };

    public DuosTicket(Ticket request) {
      this.request = request;
    }

    public String toString() {
      return GsonUtil.gsonBuilderWithAdapters()
          .addSerializationExclusionStrategy(strategy)
          .create()
          .toJson(this);
    }
  }

}
