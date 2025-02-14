package org.broadinstitute.consent.http.service;

import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpStatusCodes;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ServerErrorException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
import org.broadinstitute.consent.http.exceptions.UnprocessableEntityException;
import org.broadinstitute.consent.http.models.support.DuosTicket;
import org.broadinstitute.consent.http.models.support.TicketFactory;
import org.broadinstitute.consent.http.util.ConsentLogger;
import org.broadinstitute.consent.http.util.HttpClientUtil;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.zendesk.client.v2.model.Request;

public class SupportRequestService implements ConsentLogger {

  private final HttpClientUtil clientUtil;
  private final ServicesConfiguration configuration;

  @Inject
  public SupportRequestService(ServicesConfiguration configuration) {
    this.clientUtil = new HttpClientUtil(configuration);
    this.configuration = configuration;
  }

  /**
   * Submit binary content to Zendesk as an attachment. The token in the response can be used in a
   * subsequent ticket submission.
   *
   * @param content Binary attachment content
   * @return JsonObject with a "token" key containing the file upload token
   * @throws Exception The exception
   */
  public JsonObject postAttachmentToSupport(byte[] content) throws Exception {
    if (configuration.isActivateSupportNotifications()) {
      GenericUrl genericUrl = new GenericUrl(configuration.postSupportUploadUrl());
      ByteArrayContent byteContent = new ByteArrayContent("application/binary", content);
      HttpRequest request = clientUtil.buildUnAuthedPostRequest(genericUrl, byteContent);
      HttpResponse response = clientUtil.handleHttpRequest(request);

      if (!response.isSuccessStatusCode()) {
        String errorMessage = "Error sending attachment to support: " + response.getStatusMessage();
        var errorException =
            response.getStatusCode() == HttpStatusCodes.STATUS_CODE_UNPROCESSABLE_ENTITY ?
                new UnprocessableEntityException(errorMessage) :
                new ServerErrorException(response.getStatusMessage(), response.getStatusCode());
        logException(errorMessage, errorException);
        throw errorException;
      }
      String responseContent = IOUtils.toString(response.getContent(), Charset.defaultCharset());
      JsonObject obj = GsonUtil.getInstance().fromJson(responseContent, JsonObject.class);
      if (obj != null && obj.get("upload") != null) {
        return obj.get("upload").getAsJsonObject();
      } else {
        var errorException = new ServerErrorException(response.getStatusMessage(),
            HttpStatusCodes.STATUS_CODE_SERVER_ERROR);
        String errorMessage = "Error reading attachment response content: " + responseContent;
        logException(errorMessage, errorException);
        throw errorException;
      }
    }
    throw new BadRequestException("Not configured to send support attachments");
  }

  /**
   * Submit a new ticket to Broad's Zendesk instance
   *
   * @param ticket An instance of DuosTicket
   * @return The response
   * @throws Exception The exception
   */
  public Request postTicketToSupport(DuosTicket ticket) throws Exception {
    if (configuration.isActivateSupportNotifications()) {
      GenericUrl genericUrl = new GenericUrl(configuration.postSupportRequestUrl());
      ByteArrayContent content = new ByteArrayContent("application/json",
          ticket.toString().getBytes(StandardCharsets.UTF_8));
      HttpRequest request = clientUtil.buildUnAuthedPostRequest(genericUrl, content);
      HttpResponse response = clientUtil.handleHttpRequest(request);

      if (!response.isSuccessStatusCode()) {
        String errorMessage = "Error posting ticket to support: " + response.getStatusMessage();
        var errorException =
            response.getStatusCode() == HttpStatusCodes.STATUS_CODE_UNPROCESSABLE_ENTITY ?
                new UnprocessableEntityException(errorMessage) :
                new ServerErrorException(response.getStatusMessage(), response.getStatusCode());
        logException(errorMessage, errorException);
        throw errorException;
      }
      return TicketFactory.parseRequestResponse(
          IOUtils.toString(response.getContent(), Charset.defaultCharset()));
    }
    throw new BadRequestException("Not configured to send support requests");
  }

}
