package org.broadinstitute.consent.http.service;

import static org.broadinstitute.consent.http.service.DataAccessRequestService.EXPIRE_NOTICE_INTERVAL;
import static org.broadinstitute.consent.http.service.DataAccessRequestService.EXPIRE_WARN_INTERVAL;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.sendgrid.Response;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import jakarta.ws.rs.NotFoundException;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import org.broadinstitute.consent.http.configurations.ConsentConfiguration;
import org.broadinstitute.consent.http.db.DacDAO;
import org.broadinstitute.consent.http.db.DarCollectionDAO;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.MailMessageDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.enumeration.EmailType;
import org.broadinstitute.consent.http.mail.SendGridAPI;
import org.broadinstitute.consent.http.mail.freemarker.FreeMarkerTemplateHelper;
import org.broadinstitute.consent.http.mail.message.DaaRequestMessage;
import org.broadinstitute.consent.http.mail.message.DarExpirationReminderMessage;
import org.broadinstitute.consent.http.mail.message.DarExpiredMessage;
import org.broadinstitute.consent.http.mail.message.DataCustodianApprovalMessage;
import org.broadinstitute.consent.http.mail.message.DatasetApprovedMessage;
import org.broadinstitute.consent.http.mail.message.DatasetDeniedMessage;
import org.broadinstitute.consent.http.mail.message.DatasetSubmittedMessage;
import org.broadinstitute.consent.http.mail.message.MailMessage;
import org.broadinstitute.consent.http.mail.message.NewDAAUploadResearcherMessage;
import org.broadinstitute.consent.http.mail.message.NewDAAUploadSOMessage;
import org.broadinstitute.consent.http.mail.message.NewResearcherLibraryRequestMessage;
import org.broadinstitute.consent.http.mail.message.ReminderMessage;
import org.broadinstitute.consent.http.mail.message.ResearcherApprovedMessage;
import org.broadinstitute.consent.http.models.DarCollection;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.Vote;
import org.broadinstitute.consent.http.models.dto.DatasetMailDTO;
import org.broadinstitute.consent.http.util.ConsentLogger;

public class EmailService implements ConsentLogger {

  public static final Timestamp MINIMUM_SUBMITTED_DATE_FOR_DAR_EXPIRATIONS = Timestamp.from(
      Instant.ofEpochSecond(
          LocalDate.of(2024, 9, 30).toEpochSecond(LocalTime.of(0, 0, 0, 0), ZoneOffset.UTC)));
  private final DarCollectionDAO collectionDAO;
  private final DataAccessRequestDAO dataAccessRequestDAO;
  private final UserDAO userDAO;
  private final ElectionDAO electionDAO;
  private final MailMessageDAO emailDAO;
  private final VoteDAO voteDAO;
  private final DatasetDAO datasetDAO;
  private final DacDAO dacDAO;
  private final FreeMarkerTemplateHelper templateHelper;
  private final SendGridAPI sendGridAPI;
  private final String fromAccount;
  private final String serverUrl;

  @Inject
  public EmailService(
      DarCollectionDAO collectionDAO,
      VoteDAO voteDAO,
      ElectionDAO electionDAO,
      UserDAO userDAO,
      MailMessageDAO emailDAO,
      DatasetDAO datasetDAO,
      DacDAO dacDAO,
      DataAccessRequestDAO dataAccessRequestDAO,
      SendGridAPI sendGridAPI,
      FreeMarkerTemplateHelper helper,
      ConsentConfiguration config) {
    this.collectionDAO = collectionDAO;
    this.userDAO = userDAO;
    this.electionDAO = electionDAO;
    this.voteDAO = voteDAO;
    this.templateHelper = helper;
    this.emailDAO = emailDAO;
    this.datasetDAO = datasetDAO;
    this.dacDAO = dacDAO;
    this.dataAccessRequestDAO = dataAccessRequestDAO;
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
    Instant dateSent = (Objects.nonNull(response) && response.getStatusCode() < 400) ? now : null;
    emailDAO.insert(
        entityReferenceId,
        voteId,
        userId,
        emailType.getTypeInt(),
        dateSent,
        content,
        Objects.nonNull(response) ? response.getBody() : null,
        Objects.nonNull(response) ? response.getStatusCode() : null,
        now);
  }

  @VisibleForTesting
  protected void sendMessage(MailMessage mailMessage, Integer userId)
      throws IOException, TemplateException {
    Writer out = new StringWriter();
    Template template = templateHelper.getTemplate(mailMessage.getTemplateName());
    template.process(mailMessage.createModel(serverUrl), out);
    String content = out.toString();
    Mail message = new Mail(new Email(fromAccount), mailMessage.createSubject(),
        new Email(mailMessage.toUser.getEmail()), new Content("text/html", content));
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
      EmailType emailType, Integer limit,
      Integer offset) {
    return emailDAO.fetchMessagesByType(emailType.getTypeInt(), limit, offset);
  }

  public List<org.broadinstitute.consent.http.models.mail.MailMessage> fetchEmailMessagesByCreateDate(
      Date start, Date end, Integer limit,
      Integer offset) {
    return emailDAO.fetchMessagesByCreateDate(start, end, limit, offset);
  }

  public void sendExpirationNotices() {
    sendDARExpirationReminderNotices();
    sendDARExpirationNotices();
  }

  private void sendDARExpirationNotices() {
    EmailType emailType = EmailType.DAR_EXPIRED;
    sendDARMessageToList(emailType, EXPIRE_NOTICE_INTERVAL);
  }

  private void sendDARExpirationReminderNotices() {
    EmailType emailType = EmailType.DAR_EXPIRATION_REMINDER;
    sendDARMessageToList(emailType, EXPIRE_WARN_INTERVAL);
  }

  private void sendDARMessageToList(EmailType type, String interval) {
    List<DataAccessRequest> expiredDars = dataAccessRequestDAO.findAgedDARsByEmailTypeOlderThanInterval(
        type.getTypeInt(), interval, MINIMUM_SUBMITTED_DATE_FOR_DAR_EXPIRATIONS);
    expiredDars.forEach(expiredDar -> {
      try {
        String referenceId = expiredDar.getReferenceId();
        User user = userDAO.findUserById(expiredDar.getUserId());
        String darCode = expiredDar.getDarCode();
        String userName = user.getDisplayName();
        if (user.getEmail() == null) {
          // Do not throw here.  Log information about the DAR since this will continue
          // to appear broken until manual intervention is taken to resolve the missing user
          // email address
          logException(new Exception(String.format(
              "Email address for user %d (%s) not found for expiring warning.  DAR reference id: %s",
              expiredDar.getUserId(), userName, referenceId)));
        } else {
          switch (type) {
            case DAR_EXPIRATION_REMINDER:
              sendDarExpirationReminderMessage(user, darCode, user.getUserId(), referenceId);
              break;
            case DAR_EXPIRED:
              sendDarExpiredMessage(user, darCode, user.getUserId(), referenceId);
          }
        }
      } catch (Exception e) {
        logException(e);
      }
    });
  }

  public void sendReminderMessage(Integer voteId) throws IOException, TemplateException {
    Vote vote = voteDAO.findVoteById(voteId);
    Election election = electionDAO.findElectionWithFinalVoteById(vote.getElectionId());
    DarCollection collection = collectionDAO.findDARCollectionByReferenceId(
        election.getReferenceId());
    User user = findUserById(vote.getUserId());
    String voteUrl = serverUrl + "dar_collection/%d".formatted(collection.getDarCollectionId());
    sendMessage(new ReminderMessage(user, vote, collection.getDarCode(), election.getElectionType(),
        voteUrl), user.getUserId());
    voteDAO.updateVoteReminderFlag(voteId, true);
  }

  public void sendResearcherDarApproved(
      String darCode,
      Integer researcherId,
      List<DatasetMailDTO> datasets,
      String dataUseRestriction)
      throws TemplateException, IOException {
    User user = userDAO.findUserById(researcherId);
    sendMessage(
        new ResearcherApprovedMessage(user, darCode, datasets, dataUseRestriction), researcherId);
  }

  public void sendDataCustodianApprovalMessage(
      User custodian,
      String darCode,
      List<DatasetMailDTO> datasets,
      String dataDepositorName,
      String researcherEmail)
      throws TemplateException, IOException {
    sendMessage(
        new DataCustodianApprovalMessage(
            custodian, darCode, datasets, dataDepositorName, researcherEmail),
        custodian.getUserId());
  }

  public void sendDatasetSubmittedMessage(
      User dacChair, User dataSubmitter, String dacName, String datasetName)
      throws TemplateException, IOException {
    sendMessage(
        new DatasetSubmittedMessage(dacChair, dataSubmitter.getDisplayName(), datasetName, dacName),
        dacChair.getUserId());
  }

  public void sendDatasetApprovedMessage(User user, String dacName, String datasetName)
      throws TemplateException, IOException {
    sendMessage(new DatasetApprovedMessage(user, dacName, datasetName), user.getUserId());
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

  public void sendDaaRequestMessage(
      User signingOfficial, User requestUser, String daaName, Integer daaId)
      throws TemplateException, IOException {
    sendMessage(
        new DaaRequestMessage(signingOfficial, requestUser, daaName, daaId),
        requestUser.getUserId());
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
        new NewDAAUploadResearcherMessage(
            researcher, dacName, previousDaaName, newDaaName),
        userId);
  }

  private User findUserById(Integer id) throws IllegalArgumentException {
    User user = userDAO.findUserById(id);
    if (user == null) {
      throw new NotFoundException("Could not find dacUser for specified id : " + id);
    }
    return user;
  }

  /**
   * Send a message to a researcher that their data access request has expired.
   *
   * @param researcher  the researcher to send the message to
   * @param darCode     the data access request code that's expired
   * @param userId      the user id of the person sending the message
   * @param referenceId the data access request reference id that's expired
   */
  public void sendDarExpiredMessage(User researcher, String darCode, Integer userId,
      String referenceId)
      throws TemplateException, IOException {
    sendMessage(new DarExpiredMessage(researcher, darCode, referenceId), userId);
  }

  /**
   * Remind the user that their data access request is about to expire.
   *
   * @param user        the user to send the message to
   * @param darCode     the data access request code that's about to expire
   * @param userId      the user id of the person sending the message
   * @param referenceId the data access request reference id that is expiring
   */
  public void sendDarExpirationReminderMessage(User user, String darCode, Integer userId,
      String referenceId)
      throws TemplateException, IOException {
    sendMessage(new DarExpirationReminderMessage(user, darCode, referenceId), userId);
  }
}
