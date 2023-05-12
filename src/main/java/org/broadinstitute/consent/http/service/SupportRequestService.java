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
import java.util.Objects;
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
import org.broadinstitute.consent.http.db.InstitutionDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserUpdateFields;
import org.broadinstitute.consent.http.models.support.SupportTicket;
import org.broadinstitute.consent.http.models.support.SupportTicketCreator;
import org.broadinstitute.consent.http.util.ConsentLogger;
import org.broadinstitute.consent.http.util.HttpClientUtil;

public class SupportRequestService implements ConsentLogger {

  private final SupportTicketCreator supportTicketCreator;
  private final HttpClientUtil clientUtil;
  private final ServicesConfiguration configuration;

  @Inject
  public SupportRequestService(ServicesConfiguration configuration, InstitutionDAO institutionDAO,
      UserDAO userDAO) {
    this.supportTicketCreator = new SupportTicketCreator(institutionDAO, userDAO, configuration);
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

  /**
   * Creates and sends a support ticket for a user selecting an existing or requesting an unfamiliar
   * institution and/or signing official, if provided
   *
   * @param userUpdateFields A UserUpdateFields object containing update information for the user
   * @param user             The user requesting the institution and/or signing official
   */
  public void handleInstitutionSOSupportRequest(UserUpdateFields userUpdateFields, User user) {
    if (Objects.nonNull(userUpdateFields) && Objects.nonNull(user)) {
      boolean updateFieldProvided = Objects.nonNull(userUpdateFields.getSuggestedInstitution())
          || Objects.nonNull(userUpdateFields.getInstitutionId())
          || Objects.nonNull(userUpdateFields.getSuggestedSigningOfficial())
          || Objects.nonNull(userUpdateFields.getSelectedSigningOfficialId());

      //only send ticket if an institution or signing official is provided; ignore otherwise
      if (updateFieldProvided) {
        try {
          SupportTicket ticket = supportTicketCreator.createInstitutionSOSupportTicket(
              userUpdateFields, user);
          postTicketToSupport(ticket);
        } catch (Exception e) {
          String errorMessage =
              "Exception sending suggested user fields support request: " + e.getMessage();
          var errorException = new ServerErrorException(
              "Unable to send support ticket for user with email:" +
                  " " + user.getEmail(),
              HttpStatusCodes.STATUS_CODE_SERVER_ERROR);
          logException(errorMessage, errorException);
          throw errorException;
        }
      }
    }
  }
}
