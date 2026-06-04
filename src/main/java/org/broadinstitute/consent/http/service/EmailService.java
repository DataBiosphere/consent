package org.broadinstitute.consent.http.service;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.sendgrid.Response;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.broadinstitute.consent.http.configurations.ConsentConfiguration;
import org.broadinstitute.consent.http.db.DAOContainer;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.MailMessageDAO;
import org.broadinstitute.consent.http.db.StudyDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.enumeration.EmailType;
import org.broadinstitute.consent.http.mail.SendGridAPI;
import org.broadinstitute.consent.http.mail.freemarker.FreeMarkerTemplateHelper;
import org.broadinstitute.consent.http.mail.message.DacVoteDigestMessage;
import org.broadinstitute.consent.http.mail.message.MailMessage;
import org.broadinstitute.consent.http.mail.message.NewStudyDigestMessage;
import org.broadinstitute.consent.http.models.StudyDatasetCountRecord;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserVoteReminder;
import org.broadinstitute.consent.http.models.mail.MailMessageInsert;
import org.broadinstitute.consent.http.util.ConsentLogger;
import org.jdbi.v3.core.result.ResultIterable;

public class EmailService implements ConsentLogger {

  private static final int LOOKBACK_DELAY_HOURS = 24;
  @VisibleForTesting protected static int sendgridThrottleMessageCount = 500;
  @VisibleForTesting protected static int sendgridThrottleResetTime = 60;
  private final UserDAO userDAO;
  private final MailMessageDAO emailDAO;
  private final ElectionDAO electionDAO;
  private final FreeMarkerTemplateHelper templateHelper;
  private final SendGridAPI sendGridAPI;
  private final String fromAccount;
  private final String serverUrl;
  private final DatasetDAO datasetDAO;
  private final StudyDAO studyDAO;

  @Inject
  public EmailService(
      DAOContainer daoContainer,
      SendGridAPI sendGridAPI,
      FreeMarkerTemplateHelper helper,
      ConsentConfiguration config) {
    this.userDAO = daoContainer.getUserDAO();
    this.templateHelper = helper;
    this.emailDAO = daoContainer.getMailMessageDAO();
    this.electionDAO = daoContainer.getElectionDAO();
    this.datasetDAO = daoContainer.getDatasetDAO();
    this.studyDAO = daoContainer.getStudyDAO();
    this.sendGridAPI = sendGridAPI;
    this.serverUrl = config.getServicesConfiguration().getLocalURL();
    this.fromAccount = config.getMailConfiguration().getGoogleAccount();
  }

  public void sendMessage(MailMessage mailMessage, Integer userId)
      throws IOException, TemplateException {
    Writer out = new StringWriter();
    Template template = templateHelper.getTemplate(mailMessage.getTemplateName());
    template.process(mailMessage.createModel(serverUrl), out);
    String content = out.toString();
    Mail message =
        new Mail(
            new Email(fromAccount),
            mailMessage.createSubject(),
            new Email(mailMessage.toUser.getEmail()),
            new Content("text/html", content));
    // Add SendGrid categories for email analytics if categories are defined for the email type
    if (mailMessage.emailType.getSendGridCategories() != null
        && !mailMessage.emailType.getSendGridCategories().isEmpty()) {
      mailMessage.emailType.getSendGridCategories().forEach(message::addCategory);
    }
    // Checks that the user has not disabled email before sending
    Response response = sendGridAPI.sendMessage(message, mailMessage.toUser.getEmail());
    Instant now = Instant.now();
    Date dateSent = (response != null && response.getStatusCode() < 400) ? Date.from(now) : null;
    String sendgridResponse = response != null ? response.getBody() : null;
    Integer sendgridStatus = response != null ? response.getStatusCode() : null;
    MailMessageInsert mailMessageInsert =
        new MailMessageInsert(
            mailMessage.getEntityReferenceId(),
            mailMessage.getVoteId(),
            userId,
            mailMessage.emailType.getTypeInt(),
            dateSent,
            content,
            sendgridResponse,
            sendgridStatus);
    emailDAO.insert(mailMessageInsert);
  }

  public List<org.broadinstitute.consent.http.models.mail.MailMessage> fetchEmailMessagesByType(
      EmailType emailType, Integer limit, Integer offset) {
    return emailDAO.fetchMessagesByType(emailType.getTypeInt(), limit, offset);
  }

  public List<org.broadinstitute.consent.http.models.mail.MailMessage> fetchEmailMessagesByUserId(
      Integer userId, Integer limit, Integer offset) {
    return emailDAO.fetchMessagesByUserId(userId, limit, offset);
  }

  public List<org.broadinstitute.consent.http.models.mail.MailMessage>
      fetchEmailMessagesByCreateDate(Date start, Date end, Integer limit, Integer offset) {
    return emailDAO.fetchMessagesByCreateDate(start, end, limit, offset);
  }

  public void sendVoteDigestMessages() {
    Instant timeBasis = Instant.now();
    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneOffset.UTC);
    String referenceId = dateTimeFormatter.format(timeBasis);
    Integer emailType = EmailType.DAC_VOTE_REMINDER_DIGEST.getTypeInt();
    List<UserVoteReminder> userOpenElections =
        electionDAO.findElectionReminders(LOOKBACK_DELAY_HOURS, emailType, referenceId);
    for (UserVoteReminder entry : userOpenElections) {
      try {
        sendMessage(
            new DacVoteDigestMessage(
                userDAO.findUserById(entry.getuserId()),
                entry.getUserReminderList(),
                referenceId,
                timeBasis),
            entry.getuserId());
      } catch (Exception e) {
        logWarn(e.getMessage());
      }
    }
  }

  public void sendNewDatasetInDUOSNotifications() {
    Instant timeBasis = Instant.now();
    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneOffset.UTC);
    String referenceId = dateTimeFormatter.format(timeBasis);
    Integer emailType = EmailType.NEW_STUDY_DIGEST.getTypeInt();
    List<StudyDatasetCountRecord> studyInfo = getRecentStudyInfoForDigestMessage();
    if (studyInfo != null && !studyInfo.isEmpty()) {
      processNewStudyMessages(emailType, referenceId, studyInfo);
    } else {
      logInfo("Skipping New Study Digest emails, no data found to send.");
    }
  }

  private void processNewStudyMessages(
      Integer emailType, String referenceId, List<StudyDatasetCountRecord> studyInfo) {
    AtomicInteger success = new AtomicInteger(0);
    AtomicInteger errors = new AtomicInteger(0);
    userDAO
        .getHandle()
        .getJdbi()
        .useHandle(
            _ -> {
              ResultIterable<User> users =
                  userDAO.allEmailReceivingThinlyPopulatedUsers(emailType, referenceId);
              users.forEach(
                  user -> {
                    try {
                      sendMessage(
                          new NewStudyDigestMessage(user, studyInfo, referenceId),
                          user.getUserId());
                      success.incrementAndGet();
                    } catch (IOException | TemplateException e) {
                      logWarn(
                          "Failed to send NewStudyDigestMessage email for user %d"
                              .formatted(user.getUserId()),
                          e);
                      errors.incrementAndGet();
                    }
                    // sleep for a minute after each batch of 500 because Twiliow/SendGrid may throw
                    // 429s if we send too many email messages too quickly.
                    if (((success.get() + errors.get()) % sendgridThrottleMessageCount) == 0) {
                      logInfo(
                          "Processing user emails for NewStudyDigestMessage, %d processed.  Pausing for %d seconds."
                              .formatted(success.get() + errors.get(), sendgridThrottleResetTime));
                      try {
                        Thread.sleep(Duration.ofSeconds(sendgridThrottleResetTime).toMillis());
                      } catch (InterruptedException e) {
                        logWarn(
                            "NewStudyDigestMessage process interrupted. %d successfully sent, %d failed"
                                .formatted(success.get(), errors.get()));
                        logWarn(e.getMessage());
                        Thread.currentThread().interrupt();
                      }
                    }
                  });
            });
    logInfo(
        "NewStudyDigestMessage stats: %d successfully sent, %d failed."
            .formatted(success.get(), errors.get()));
  }

  private List<StudyDatasetCountRecord> getRecentStudyInfoForDigestMessage() {
    List<Integer> newDacApprovedDatasetStudyIds = datasetDAO.getRecentDacApprovedDatasetStudyIds();
    List<Integer> newOpenOrExternalDatasetStudyIds =
        datasetDAO.getRecentlyCreatedOpenOrExternalDatasetStudyIds();
    Set<Integer> studyIds = new HashSet<>();
    studyIds.addAll(newDacApprovedDatasetStudyIds);
    studyIds.addAll(newOpenOrExternalDatasetStudyIds);
    return studyDAO.findStudyDatasetCountsWithAccessTypes(studyIds);
  }
}
