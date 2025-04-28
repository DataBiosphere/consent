package org.broadinstitute.consent.http.mail;

import com.google.api.client.http.HttpStatusCodes;
import com.google.gson.Gson;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import jakarta.ws.rs.WebApplicationException;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import org.broadinstitute.consent.http.configurations.MailConfiguration;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.util.ConsentLogger;

public class SendGridAPI implements ConsentLogger {

  private final SendGrid sendGrid;
  private final boolean activateEmailNotifications;

  private final UserDAO userDAO;

  public SendGridAPI(MailConfiguration config, UserDAO userDAO) {
    this.sendGrid = new SendGrid(config.getSendGridApiKey());
    this.activateEmailNotifications = config.isActivateEmailNotifications();
    this.userDAO = userDAO;
  }

  /**
   * Determine if the user we are sending an email to has set their preference to false or not.
   * Users who have been disabled like this should never receive an email.
   *
   * @param userEmail The email of the user we are sending an email to.
   * @return False if the user has explicitly disabled email, True otherwise.
   */
  private boolean findUserEmailPreference(String userEmail) {
    User user = userDAO.findUserByEmail(userEmail);
    if (user == null) {
      logWarn("Unknown user ID: %s".formatted(userEmail));
      return false;
    }
    return Objects.requireNonNullElse(user.getEmailPreference(), true);
  }

  public Response sendMessage(Mail message, String toUserEmail) {
    if (!activateEmailNotifications) {
      return null;
    }
    boolean userEmailPreference = findUserEmailPreference(toUserEmail);
    if (!userEmailPreference) {
      logInfo(
          "User Email Preference has evaluated to 'false', not sending to: %s"
              .formatted(toUserEmail));
      return null;
    }
    try {
      // See https://github.com/sendgrid/sendgrid-java/issues/163
      // for what actually works as compared to the documentation - which doesn't.
      Request request = new Request();
      request.setMethod(Method.POST);
      request.setBody(message.build());
      // make request
      request.setBaseUri(sendGrid.getHost());
      request.setEndpoint("/" + sendGrid.getVersion() + "/mail/send");
      for (String key : sendGrid.getRequestHeaders().keySet()) {
        request.addHeader(key, sendGrid.getRequestHeaders().get(key));
      }
      // send
      Response response = sendGrid.makeCall(request);
      if (response.getStatusCode() > 202) {
        // Indicates some form of error:
        // https://docs.sendgrid.com/api-reference/mail-send/mail-send#responses
        logException(
            "Error sending email via SendGrid: '%s': %s"
                .formatted(response.getStatusCode(), response.getBody()),
            new WebApplicationException(response.getStatusCode()));
      }
      return response;
    } catch (IOException ex) {
      logException("Exception sending email via SendGrid: %s".formatted(ex.getMessage()), ex);
      // Create a response that we can use to capture this failure.
      return new Response(HttpStatusCodes.STATUS_CODE_SERVER_ERROR, ex.getMessage(), Map.of());
    }
  }
}
