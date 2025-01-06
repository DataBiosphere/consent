package org.broadinstitute.consent.http.service;

import static org.broadinstitute.consent.http.configurations.ServicesConfiguration.BROAD_ZENDESK_URL;

import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpStatusCodes;
import com.google.common.net.MediaType;
import com.google.inject.Inject;
import jakarta.ws.rs.ServerErrorException;
import java.nio.charset.StandardCharsets;
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
import org.broadinstitute.consent.http.models.support.DuosTicket;
import org.broadinstitute.consent.http.util.ConsentLogger;
import org.broadinstitute.consent.http.util.HttpClientUtil;
import org.zendesk.client.v2.Zendesk;
import org.zendesk.client.v2.model.Attachment.Upload;

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

  public HttpResponse postZendeskTicket(DuosTicket ticket) throws Exception {
    if (configuration.isActivateSupportNotifications()) {
      GenericUrl genericUrl = new GenericUrl(configuration.postSupportRequestUrl());

      ByteArrayContent content = new ByteArrayContent("application/json",
          ticket.toString().getBytes(StandardCharsets.UTF_8));
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

}
