package org.broadinstitute.consent.http.mail;

import com.google.api.client.http.HttpStatusCodes;
import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.helpers.mail.objects.Personalization;
import jakarta.ws.rs.WebApplicationException;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.mail.MessagingException;
import org.broadinstitute.consent.http.configurations.MailConfiguration;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.mail.message.DaaRequestMessage;
import org.broadinstitute.consent.http.mail.message.DataCustodianApprovalMessage;
import org.broadinstitute.consent.http.mail.message.DatasetApprovedMessage;
import org.broadinstitute.consent.http.mail.message.DatasetDeniedMessage;
import org.broadinstitute.consent.http.mail.message.DatasetSubmittedMessage;
import org.broadinstitute.consent.http.mail.message.DisabledDatasetMessage;
import org.broadinstitute.consent.http.mail.message.NewCaseMessage;
import org.broadinstitute.consent.http.mail.message.NewDAAUploadResearcherMessage;
import org.broadinstitute.consent.http.mail.message.NewDAAUploadSOMessage;
import org.broadinstitute.consent.http.mail.message.NewDARRequestMessage;
import org.broadinstitute.consent.http.mail.message.NewResearcherLibraryRequestMessage;
import org.broadinstitute.consent.http.mail.message.ReminderMessage;
import org.broadinstitute.consent.http.mail.message.ResearcherApprovedMessage;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.util.ConsentLogger;

public class SendGridAPI implements ConsentLogger {

  private String fromAccount;
  private SendGrid sendGrid;
  private Boolean activateEmailNotifications;
  private final NewCaseMessage newCaseMessageCreator = new NewCaseMessage();
  private final NewDARRequestMessage newDARMessageCreator = new NewDARRequestMessage();
  private final ReminderMessage reminderMessageCreator = new ReminderMessage();
  private final DisabledDatasetMessage disabledDatasetCreator = new DisabledDatasetMessage();
  private final ResearcherApprovedMessage researcherApprovedMessage = new ResearcherApprovedMessage();
  private final DataCustodianApprovalMessage dataCustodianApprovalMessage = new DataCustodianApprovalMessage();
  private final DatasetApprovedMessage datasetApprovedMessage = new DatasetApprovedMessage();
  private final DatasetDeniedMessage datasetDeniedMessage = new DatasetDeniedMessage();
  private final DatasetSubmittedMessage datasetSubmittedMessage = new DatasetSubmittedMessage();
  private final NewResearcherLibraryRequestMessage newResearcherLibraryRequestMessage = new NewResearcherLibraryRequestMessage();
  private final DaaRequestMessage daaRequestMessage = new DaaRequestMessage();
  private final NewDAAUploadSOMessage newDAAUploadSOMessage = new NewDAAUploadSOMessage();
  private final NewDAAUploadResearcherMessage newDAAUploadResearcherMessage = new NewDAAUploadResearcherMessage();
  private final UserDAO userDAO;

  public SendGridAPI(MailConfiguration config, UserDAO userDAO) {
    setFromAccount(config.getGoogleAccount());
    setSendGrid(new SendGrid(config.getSendGridApiKey()));
    setActivateEmailNotifications(config.isActivateEmailNotifications());
    this.userDAO = userDAO;
  }

  private void setFromAccount(String fromAccount) {
    this.fromAccount = fromAccount;
  }

  @VisibleForTesting
  public void setSendGrid(SendGrid sendGrid) {
    this.sendGrid = sendGrid;
  }

  private void setActivateEmailNotifications(Boolean activateEmailNotifications) {
    this.activateEmailNotifications = activateEmailNotifications;
  }

  /**
   * Determine if the user we are sending an email to has set their preference to false or not.
   * Users who have been disabled like this should never receive an email.
   *
   * @param message The mail message
   * @return False if the user has explicitly disabled email, True otherwise.
   */
  private boolean findUserEmailPreference(Mail message) {
    List<Personalization> personalizations = message.getPersonalization();
    Optional<Personalization> toEmail = personalizations.stream().findFirst();
    if (toEmail.isPresent()) {
      List<Email> tos = toEmail.get().getTos();
      // When we construct a mail message, we always send it to just one individual
      // so that we can track emails at that level of granularity.
      if (!tos.isEmpty()) {
        User user = userDAO.findUserByEmail(tos.get(0).getEmail());
        if (Objects.isNull(user)) {
          logWarn("Unknown user: %s".formatted(tos.get(0).getEmail()));
          return false;
        }
        if (Objects.isNull(user.getEmailPreference())) {
          return true;
        }
        return user.getEmailPreference();
      }
    }
    return false;
  }

  public Optional<Response> sendMessage(Mail message) {
    boolean userEmailPreference = findUserEmailPreference(message);
    if (!userEmailPreference) {
      Gson gson = new Gson();
      logInfo("User Email Preference has evaluated to 'false', not sending to: %s".formatted(gson.toJson(
          message.getPersonalization())));
    }
    if (activateEmailNotifications && userEmailPreference) {
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
              "Error sending email via SendGrid: '%s': %s".formatted(response.getStatusCode(),
                  response.getBody()), new WebApplicationException(response.getStatusCode()));
        }
        return Optional.of(response);
      } catch (IOException ex) {
        logException("Exception sending email via SendGrid: %s".formatted(ex.getMessage()), ex);
        // Create a response that we can use to capture this failure.
        Response response = new Response(
            HttpStatusCodes.STATUS_CODE_SERVER_ERROR,
            ex.getMessage(),
            Map.of()
        );
        return Optional.of(response);
      }
    }
    return Optional.empty();
  }

  public Optional<Response> sendNewCaseMessage(String toAddress, String referenceId, String type,
      Writer template) {
    Mail message = newCaseMessageCreator.newCaseMessage(toAddress, fromAccount, template,
        referenceId, type);
    return sendMessage(message);
  }

  public Optional<Response> sendReminderMessage(String toAddress, String referenceId, String type,
      Writer template) {
    Mail message = reminderMessageCreator.reminderMessage(toAddress, fromAccount, template,
        referenceId, type);
    return sendMessage(message);
  }

  public Optional<Response> sendDisabledDatasetMessage(String toAddress, String referenceId,
      String type, Writer template) {
    Mail message = disabledDatasetCreator.disabledDatasetMessage(toAddress, fromAccount, template,
        referenceId, type);
    return sendMessage(message);
  }

  public Optional<Response> sendNewDARRequests(String toAddress, String referenceId, String type,
      Writer template) {
    Mail message = newDARMessageCreator.newDARRequestMessage(toAddress, fromAccount, template,
        referenceId, type);
    return sendMessage(message);
  }

  public Optional<Response> sendNewResearcherApprovedMessage(String toAddress, Writer template,
      String darCode) {
    Mail message = researcherApprovedMessage.researcherApprovedMessage(toAddress, fromAccount,
        template, darCode);
    return sendMessage(message);
  }

  public Optional<Response> sendDataCustodianApprovalMessage(String toAddress, String darCode,
      Writer template) {
    Mail message = dataCustodianApprovalMessage.dataCustodianApprovalMessage(toAddress, fromAccount,
        darCode, template);
    return sendMessage(message);
  }

  public Optional<Response> sendDatasetApprovedMessage(String toAddress, Writer template)
      throws MessagingException {
    Mail message = datasetApprovedMessage.datasetApprovedMessage(toAddress, fromAccount, template);
    return sendMessage(message);
  }

  public Optional<Response> sendDatasetDeniedMessage(String toAddress, Writer template)
      throws MessagingException {
    Mail message = datasetDeniedMessage.datasetDeniedMessage(toAddress, fromAccount, template);
    return sendMessage(message);
  }

  public Optional<Response> sendNewResearcherLibraryRequestMessage(String toAddress,
      Writer template) throws MessagingException {
    Mail message = newResearcherLibraryRequestMessage.newResearcherLibraryRequestMessage(toAddress,
        fromAccount, template);
    return sendMessage(message);
  }

  public Optional<Response> sendDatasetSubmittedMessage(String toAddress, Writer template)
      throws MessagingException {
    Mail message = datasetSubmittedMessage.datasetSubmittedMessage(toAddress, fromAccount,
        template);
    return sendMessage(message);
  }

  public Optional<Response> sendDaaRequestMessage(String toAddress, Writer template, String daaId)
      throws MessagingException {
    Mail message = daaRequestMessage.newDaaRequestMessage(toAddress, fromAccount,
        template, daaId);
    return sendMessage(message);
  }

  public Optional<Response> sendNewDAAUploadSOMessage(String toAddress, Writer template, String dacName)
      throws MessagingException {
    Mail message = newDAAUploadSOMessage.newDAAUploadSOMessage(toAddress, fromAccount,
        template, dacName);
    return sendMessage(message);
  }

  public Optional<Response> sendNewDAAUploadResearcherMessage(String toAddress, Writer template, String dacName)
      throws MessagingException {
    Mail message = newDAAUploadResearcherMessage.newDAAUploadResearcherMessage(toAddress, fromAccount,
        template, dacName);
    return sendMessage(message);
  }

}
