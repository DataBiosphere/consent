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
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;
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
import org.broadinstitute.consent.http.mail.message.DACMembersDARRADARApprovedMessage;
import org.broadinstitute.consent.http.mail.message.DacVoteDigestMessage;
import org.broadinstitute.consent.http.mail.message.DarExpirationReminderMessage;
import org.broadinstitute.consent.http.mail.message.DarExpiredMessage;
import org.broadinstitute.consent.http.mail.message.DataCustodianApprovalMessage;
import org.broadinstitute.consent.http.mail.message.DatasetApprovedMessage;
import org.broadinstitute.consent.http.mail.message.DatasetDeniedMessage;
import org.broadinstitute.consent.http.mail.message.DatasetSubmittedMessage;
import org.broadinstitute.consent.http.mail.message.MailMessage;
import org.broadinstitute.consent.http.mail.message.NewCaseMessage;
import org.broadinstitute.consent.http.mail.message.NewDAAUploadResearcherMessage;
import org.broadinstitute.consent.http.mail.message.NewDAAUploadSOMessage;
import org.broadinstitute.consent.http.mail.message.NewDARRequestMessage;
import org.broadinstitute.consent.http.mail.message.NewDARSigningOfficialRequestMessage;
import org.broadinstitute.consent.http.mail.message.NewLibraryCardIssuedMessage;
import org.broadinstitute.consent.http.mail.message.NewProgressReportCaseMessage;
import org.broadinstitute.consent.http.mail.message.NewProgressReportRequestMessage;
import org.broadinstitute.consent.http.mail.message.NewResearcherLibraryRequestMessage;
import org.broadinstitute.consent.http.mail.message.NewStudyDigestMessage;
import org.broadinstitute.consent.http.mail.message.NewStudyRegistrationConfirmationMessage;
import org.broadinstitute.consent.http.mail.message.ReminderMessage;
import org.broadinstitute.consent.http.mail.message.ResearcherApprovedProgressReportMessage;
import org.broadinstitute.consent.http.mail.message.ResearcherCloseoutCompletedMessage;
import org.broadinstitute.consent.http.mail.message.SoDARSubmitted;
import org.broadinstitute.consent.http.mail.message.SoPRApproved;
import org.broadinstitute.consent.http.mail.message.SoPRSubmitted;
import org.broadinstitute.consent.http.mail.message.SubmittedCloseoutMessage;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.StudyDatasetCountRecord;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserVoteReminder;
import org.broadinstitute.consent.http.models.Vote;
import org.broadinstitute.consent.http.models.dto.DatasetMailDTO;
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

  /**
   * This method saves an email (either sent or unsent) with all available metadata from the
   * SendGrid response.
   */
  private void saveEmailAndResponse(
      @Nullable Response response,
      @Nullable String entityReferenceId,
      @Nullable Integer voteId,
      Integer userId,
      EmailType emailType,
      String content) {
    Instant now = Instant.now();
    Date dateSent =
        (Objects.nonNull(response) && response.getStatusCode() < 400) ? Date.from(now) : null;
    String sendgridResponse = Objects.nonNull(response) ? response.getBody() : null;
    Integer sendgridStatus = Objects.nonNull(response) ? response.getStatusCode() : null;
    org.broadinstitute.consent.http.models.mail.MailMessage mailMessage =
        new org.broadinstitute.consent.http.models.mail.MailMessage(
            entityReferenceId,
            null,
            voteId,
            userId,
            emailType.getTypeInt(),
            dateSent,
            content,
            sendgridResponse,
            sendgridStatus,
            Date.from(now));
    emailDAO.insert(mailMessage);
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
    saveEmailAndResponse(
        response,
        mailMessage.getEntityReferenceId(),
        mailMessage.getVoteId(),
        userId,
        mailMessage.emailType,
        content);
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


  public void sendResearcherProgressReportApproved(
      String darCode,
      Integer researcherId,
      List<DatasetMailDTO> datasets,
      String dataUseRestriction,
      boolean radarApproved)
      throws TemplateException, IOException {
    User user = userDAO.findUserById(researcherId);
    sendMessage(
        new ResearcherApprovedProgressReportMessage(
            user, darCode, datasets, dataUseRestriction, radarApproved),
        researcherId);
  }

  public void sendDataCustodianApprovalMessage(
      User custodian,
      String darCode,
      List<DatasetMailDTO> datasets,
      String dataDepositorName,
      String researcherEmail,
      boolean radarApproved)
      throws TemplateException, IOException {
    sendMessage(
        new DataCustodianApprovalMessage(
            custodian, darCode, datasets, dataDepositorName, researcherEmail, radarApproved),
        custodian.getUserId());
  }

  public void sendDatasetSubmittedMessage(
      User dacChair, User dataSubmitter, String dacName, String datasetName)
      throws TemplateException, IOException {
    sendMessage(
        new DatasetSubmittedMessage(dacChair, dataSubmitter.getDisplayName(), datasetName, dacName),
        dacChair.getUserId());
  }

  public void sendStudySubmissionConfirmation(
      User dataSubmitter, String studyName, Integer studyId, Map<String, Object> studyAssets)
      throws TemplateException, IOException {
    sendMessage(
        new NewStudyRegistrationConfirmationMessage(dataSubmitter, studyName, studyId, studyAssets),
        dataSubmitter.getUserId());
  }

  public void sendDatasetApprovedMessage(
      User user, String dacName, String datasetIdentifier, String datasetName)
      throws TemplateException, IOException {
    sendMessage(
        new DatasetApprovedMessage(user, dacName, datasetIdentifier, datasetName),
        user.getUserId());
  }

  public void sendDatasetDeniedMessage(
      User user, String dacName, String datasetName, String dacEmail)
      throws TemplateException, IOException {
    sendMessage(new DatasetDeniedMessage(user, dacName, datasetName, dacEmail), user.getUserId());
  }

  public void sendNewResearcherMessage(User researcher, User signingOfficial)
      throws TemplateException, IOException {
    sendMessage(
        new NewResearcherLibraryRequestMessage(signingOfficial, researcher),
        researcher.getUserId());
  }

  public void sendNewDAAUploadSOMessage(
      User signingOfficial,
      String dacName,
      String previousDaaName,
      String newDaaName,
      Integer userId)
      throws TemplateException, IOException {
    sendMessage(
        new NewDAAUploadSOMessage(signingOfficial, dacName, previousDaaName, newDaaName), userId);
  }

  public void sendNewDAAUploadResearcherMessage(
      User researcher, String dacName, String previousDaaName, String newDaaName, Integer userId)
      throws TemplateException, IOException {
    sendMessage(
        new NewDAAUploadResearcherMessage(researcher, dacName, previousDaaName, newDaaName),
        userId);
  }

  public void sendDarNewCollectionElectionMessage(List<User> users, String darCode)
      throws IOException, TemplateException {
    String electionType = "Data Access Request";
    for (User user : users) {
      sendMessage(new NewCaseMessage(user, darCode, electionType), user.getUserId());
    }
  }

  public void sendProgressReportNewCollectionElectionMessage(List<User> users, String darCode)
      throws IOException, TemplateException {
    for (User user : users) {
      sendMessage(new NewProgressReportCaseMessage(user, darCode), user.getUserId());
    }
  }

  public void sendNewDARRequestEmail(
      User user, Map<String, List<String>> dacDatasetMap, String researcherName, String darCode)
      throws TemplateException, IOException {
    sendMessage(
        new NewDARRequestMessage(user, darCode, dacDatasetMap, researcherName), user.getUserId());
  }

  public void sendNewDARSigningOfficialRequestEmail(
      User signingOfficial, String researcherName, String darCode)
      throws TemplateException, IOException {
    sendMessage(
        new NewDARSigningOfficialRequestMessage(signingOfficial, darCode, researcherName),
        signingOfficial.getUserId());
  }

  public void sendNewProgressReportRequestEmail(
      User user,
      Map<String, List<String>> dacDatasetMap,
      String researcherName,
      String darCode,
      String referenceId)
      throws TemplateException, IOException {
    sendMessage(
        new NewProgressReportRequestMessage(
            user, darCode, referenceId, dacDatasetMap, researcherName),
        user.getUserId());
  }

  /**
   * Send a message to a Signing Official that a new Data Access Request has been submitted.
   *
   * @param user The user to send the message to
   * @param darCode The Data Access Request code which is submitted
   * @param researcher The researcher whose DAR has been submitted
   * @param referenceId The reference ID of the DAR
   * @param datasets The datasets associated with the DAR
   * @throws TemplateException Template processing exception
   * @throws IOException IOException when processing the template or sending the email
   */
  public void sendNewSoDARSubmittedEmail(
      User user, String darCode, User researcher, String referenceId, List<Dataset> datasets)
      throws TemplateException, IOException {
    sendMessage(
        new SoDARSubmitted(user, darCode, researcher, referenceId, datasets), user.getUserId());
  }

  /**
   * Send a message to a Signing Official that a new progress report has been submitted.
   *
   * @param user The user to send the message to
   * @param darCode The Data Access Request code for which the progress report is submitted
   * @param researcher The researcher whose progress report has been submitted
   * @param referenceId The reference ID of the progress report
   * @param datasets The datasets associated with the progress report
   * @throws TemplateException Template processing exception
   * @throws IOException IOException when processing the template or sending the email
   */
  public void sendNewSoProgressReportSubmittedEmail(
      User user, String darCode, User researcher, String referenceId, List<Dataset> datasets)
      throws TemplateException, IOException {
    sendMessage(
        new SoPRSubmitted(user, darCode, researcher, referenceId, datasets), user.getUserId());
  }


  /**
   * Send a message to a Signing Official that a new progress report has been approved.
   *
   * @param user The user to send the message to
   * @param darCode The Data Access Request code for which the progress report is approved
   * @param researcher The researcher whose progress report has been approved
   * @param referenceId The reference ID of the progress report
   * @param datasets The datasets associated with the progress report
   * @param dataUseRestriction The data use restriction associated with the datasets in the progress
   *     report
   * @throws TemplateException Template processing exception
   * @throws IOException IOException when processing the template or sending the email
   */
  public void sendNewSoProgressReportApprovedEmail(
      User user,
      String darCode,
      User researcher,
      String referenceId,
      List<Dataset> datasets,
      String dataUseRestriction,
      boolean radarApproved)
      throws TemplateException, IOException {
    sendMessage(
        new SoPRApproved(
            user, darCode, researcher, referenceId, datasets, dataUseRestriction, radarApproved),
        user.getUserId());
  }

  /**
   * Send a message to a researcher that their data access request has expired.
   *
   * @param researcher the researcher to send the message to
   * @param darCode the data access request code that's expired
   * @param userId the user id of the person sending the message
   * @param referenceId the data access request reference id that's expired
   */
  public void sendDarExpiredMessage(
      User researcher, String darCode, Integer userId, String referenceId)
      throws TemplateException, IOException {
    sendMessage(new DarExpiredMessage(researcher, darCode, referenceId), userId);
  }

  /**
   * Remind the user that their data access request is about to expire.
   *
   * @param user the user to send the message to
   * @param darCode the data access request code that's about to expire
   * @param userId the user id of the person sending the message
   * @param referenceId the data access request reference id that is expiring
   */
  public void sendDarExpirationReminderMessage(
      User user, String darCode, Integer userId, String referenceId)
      throws TemplateException, IOException {
    sendMessage(new DarExpirationReminderMessage(user, darCode, referenceId), userId);
  }

  public void sendReminderMessage(
      User user, Vote vote, String darCode, String electionType, String url)
      throws TemplateException, IOException {
    sendMessage(new ReminderMessage(user, vote, darCode, electionType, url), user.getUserId());
  }

  /**
   * Send a message to a user that their closeout has been completed.
   *
   * @param user the user to send the message to
   * @param darCode the data access request code for which closeout is completed
   * @param referenceId the data access request reference id for which closeout is completed
   */
  public void sendResearcherCloseoutCompletedMessage(User user, String darCode, String referenceId)
      throws TemplateException, IOException {
    sendMessage(
        new ResearcherCloseoutCompletedMessage(user, darCode, referenceId), user.getUserId());
  }

  /**
   * Send a message to a Signing Official or a DAC member that a closeout has been submitted for
   * review.
   *
   * @param toUser The user to send the message to
   * @param darId The Data Access Request ID associated with the closeout
   * @param referenceId The Reference ID of the closeout request
   * @param closeoutUrl The URL to the closeout request for review
   * @throws TemplateException Template processing exception
   * @throws IOException IOException when processing the template or sending the email
   */
  public void sendSubmittedCloseoutMessage(
      User toUser, String darId, String referenceId, String closeoutUrl)
      throws TemplateException, IOException {
    sendMessage(
        new SubmittedCloseoutMessage(toUser, darId, referenceId, closeoutUrl), toUser.getUserId());
  }

  /**
   * Send a message to the user when they are issued a library card
   *
   * @param toUser The user to send the message to
   * @throws TemplateException Template processing exception
   * @throws IOException IOException when processing the template or sending the email
   */
  public void sendNewLibraryCardIssuedMessage(User toUser) throws TemplateException, IOException {
    sendMessage(new NewLibraryCardIssuedMessage(toUser), toUser.getUserId());
  }

  public void sendNewDARRADARApprovalToDAC(
      User dacMember,
      String darCode,
      String referenceId,
      List<DatasetMailDTO> datasetList,
      User researcher)
      throws TemplateException, IOException {
    sendMessage(
        new DACMembersDARRADARApprovedMessage(
            dacMember, darCode, researcher, referenceId, datasetList),
        dacMember.getUserId());
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
    return studyDAO.findNameAndDatasetCount(studyIds);
  }
}
