package org.broadinstitute.consent.http.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sendgrid.Response;
import com.sendgrid.helpers.mail.Mail;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.broadinstitute.consent.http.AbstractTestHelper;
import org.broadinstitute.consent.http.configurations.ConsentConfiguration;
import org.broadinstitute.consent.http.configurations.MailConfiguration;
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.MailMessageDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.enumeration.EmailType;
import org.broadinstitute.consent.http.mail.SendGridAPI;
import org.broadinstitute.consent.http.mail.freemarker.FreeMarkerTemplateHelper;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.Reminder;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserVoteReminder;
import org.broadinstitute.consent.http.models.dto.DatasetMailDTO;
import org.broadinstitute.consent.http.models.mail.MailMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * This class can be used to functionally test email notifications as well as unit test. To enable
 * functional tests, configure MailService with correct values (i.e. is active, sendgrid key, etc.)
 * Functional test emails will be directed to the private google group: <a
 * href="https://groups.google.com/a/broadinstitute.org/g/duos-dev">duos-dev</a>
 */
@ExtendWith(MockitoExtension.class)
class EmailServiceTest extends AbstractTestHelper {

  private static final String SERVER_URL = "http://localhost:8000/#/";
  private static final String FROM = "from@duos";
  private EmailService service;
  @Mock private ElectionDAO electionDAO;
  @Mock private UserDAO userDAO;
  @Mock private MailMessageDAO emailDAO;
  @Mock private SendGridAPI sendGridAPI;
  @Mock private FreeMarkerTemplateHelper templateHelper;

  @BeforeEach
  void initService() {
    ConsentConfiguration config = new ConsentConfiguration();
    MailConfiguration mConfig = config.getMailConfiguration();
    mConfig.setGoogleAccount(FROM);

    ServicesConfiguration servicesConfiguration = config.getServicesConfiguration();
    servicesConfiguration.setLocalURL(SERVER_URL);
    service = new EmailService(userDAO, emailDAO, electionDAO, sendGridAPI, templateHelper, config);
  }

  @Test
  void sendMessage() throws Exception {
    String userEmail = "user@duos";
    User user = new User();
    user.setEmail(userEmail);
    String subject = "subject";
    var model = Map.of("key", "value");
    String entityReferenceId = "entityReferenceId";
    Integer userId = 1234;
    Integer voteId = 4567;
    when(templateHelper.getTemplate(EmailType.COLLECT.templateName)).thenReturn(mock());
    var message =
        new org.broadinstitute.consent.http.mail.message.MailMessage(user, EmailType.COLLECT) {
          @Override
          public String createSubject() {
            return subject;
          }

          @Override
          public Object createModel(String serverUrl) {
            return model;
          }

          @Override
          public String getEntityReferenceId() {
            return entityReferenceId;
          }

          @Override
          public Integer getVoteId() {
            return voteId;
          }
        };
    Template template = mock();
    when(templateHelper.getTemplate(EmailType.COLLECT.templateName)).thenReturn(template);
    Response response = new Response();
    response.setStatusCode(200);
    response.setBody("body");
    when(sendGridAPI.sendMessage(any(), any())).thenReturn(response);
    String emailText = "emailText";
    doNothing()
        .when(template)
        .process(
            eq(model),
            argThat(
                writer -> {
                  try {
                    writer.append(emailText);
                  } catch (IOException e) {
                    throw new RuntimeException(e);
                  }
                  return true;
                }));

    Instant fixedInstant = Instant.now();
    try (var mockedStatic = mockStatic(Instant.class)) {
      mockedStatic.when(Instant::now).thenReturn(fixedInstant);
      service.sendMessage(message, userId);
    }

    var captor = ArgumentCaptor.forClass(Mail.class);
    verify(sendGridAPI).sendMessage(captor.capture(), eq(user.getEmail()));
    var mail = captor.getValue();
    assertEquals(FROM, mail.getFrom().getEmail());
    assertEquals(user.getEmail(), mail.getPersonalization().get(0).getTos().get(0).getEmail());
    assertEquals(subject, mail.getSubject());

    verify(emailDAO)
        .insert(
            entityReferenceId,
            voteId,
            1234,
            EmailType.COLLECT.getTypeInt(),
            fixedInstant,
            emailText,
            response.getBody(),
            response.getStatusCode(),
            fixedInstant);
  }

  @Test
  void testSendNewResearcherEmail() throws Exception {
    User user = new User();
    user.setUserId(1234);
    user.setDisplayName("John Doe");

    User so = new User();
    user.setEmail("fake_email@asdf.com");

    when(templateHelper.getTemplate(EmailType.NEW_RESEARCHER.templateName)).thenReturn(mock());

    service.sendNewResearcherMessage(user, so);

    verify(sendGridAPI).sendMessage(any(), any());
    verify(emailDAO)
        .insert(
            eq("1234"),
            eq(null),
            eq(1234),
            eq(EmailType.NEW_RESEARCHER.getTypeInt()),
            any(),
            any(),
            any(),
            any(),
            any());
  }

  @Test
  void sendDarExpiredMessage() throws Exception {
    User user = new User();
    user.setUserId(123);
    user.setDisplayName("John Doe");
    user.setEmail("jd@somewhere");
    String darCode = "DAR-12345";
    Integer otherUserId = 456;
    String referenceId = UUID.randomUUID().toString();
    when(templateHelper.getTemplate(EmailType.DAR_EXPIRED.templateName)).thenReturn(mock());

    service.sendDarExpiredMessage(user, darCode, otherUserId, referenceId);
    verify(sendGridAPI).sendMessage(any(), eq(user.getEmail()));
    verify(emailDAO)
        .insert(
            eq(referenceId),
            isNull(),
            eq(otherUserId),
            eq(EmailType.DAR_EXPIRED.getTypeInt()),
            any(),
            any(),
            any(),
            any(),
            any());
  }

  @Test
  void sendDarExpirationReminderMessage() throws Exception {
    User user = new User();
    user.setUserId(123);
    user.setDisplayName("John Doe");
    user.setEmail("jd@somewhere");
    String darCode = "DAR-12345";
    String referenceId = UUID.randomUUID().toString();
    Integer otherUserId = 456;
    when(templateHelper.getTemplate(EmailType.DAR_EXPIRATION_REMINDER.templateName))
        .thenReturn(mock());

    service.sendDarExpirationReminderMessage(user, darCode, otherUserId, referenceId);
    verify(sendGridAPI).sendMessage(any(), eq(user.getEmail()));
    verify(emailDAO)
        .insert(
            eq(referenceId),
            isNull(),
            eq(otherUserId),
            eq(EmailType.DAR_EXPIRATION_REMINDER.getTypeInt()),
            any(),
            any(),
            any(),
            any(),
            any());
  }

  @Test
  void testSendDatasetSubmittedMessage() throws Exception {
    User dacChair = new User();
    dacChair.setUserId(456);
    dacChair.setDisplayName("Jane Evans");
    dacChair.setEmail("dacchair@example.com");

    User dataSubmitter = new User();
    dataSubmitter.setUserId(123);
    dataSubmitter.setDisplayName("John Doe");
    dataSubmitter.setEmail("submitter@example.com");

    String dacName = "DAC-123";
    String datasetName = "testDataset";
    when(templateHelper.getTemplate(EmailType.NEW_DATASET.templateName)).thenReturn(mock());

    service.sendDatasetSubmittedMessage(dacChair, dataSubmitter, dacName, datasetName);

    verify(sendGridAPI).sendMessage(any(), any());
    verify(emailDAO)
        .insert(
            eq(datasetName),
            eq(null),
            eq(456),
            eq(EmailType.NEW_DATASET.getTypeInt()),
            any(),
            any(),
            any(),
            any(),
            any());
  }

  @Test
  void testSendStudySubmissionConfirmation() throws Exception {
    User dataSubmitter = new User();
    dataSubmitter.setUserId(1);
    dataSubmitter.setEmail("submitter@example.com");
    dataSubmitter.setDisplayName("Submitter Name");

    String studyName = "Test Study";
    Integer studyId = 42;
    Map<String, Object> studyAssets = Map.of("assetKey", "assetValue");

    when(templateHelper.getTemplate(EmailType.NEW_STUDY_REGISTRATION_CONFIRMATION.templateName))
        .thenReturn(mock());

    service.sendStudySubmissionConfirmation(dataSubmitter, studyName, studyId, studyAssets);

    verify(sendGridAPI).sendMessage(any(), any());
    verify(emailDAO)
        .insert(
            eq(studyName),
            eq(null),
            eq(1), // userId
            eq(EmailType.NEW_STUDY_REGISTRATION_CONFIRMATION.getTypeInt()),
            any(),
            any(),
            any(),
            any(),
            any());
  }

  @Test
  void testSendDaaRequestMessage() throws Exception {
    User signingOfficial = new User();
    signingOfficial.setDisplayName("Jane Evans");
    signingOfficial.setEmail("signingofficial@example.com");

    User user = new User();
    user.setDisplayName("John Doe");
    user.setUserId(123);

    String daaName = "DAA-123";
    int daaId = 456;
    when(templateHelper.getTemplate(EmailType.NEW_DAA_REQUEST.templateName)).thenReturn(mock());

    service.sendDaaRequestMessage(signingOfficial, user, daaName, daaId);

    verify(sendGridAPI).sendMessage(any(), any());
    verify(emailDAO)
        .insert(
            eq("456"),
            eq(null),
            eq(user.getUserId()),
            eq(EmailType.NEW_DAA_REQUEST.getTypeInt()),
            any(),
            any(),
            any(),
            any(),
            any());
  }

  @Test
  void testSendNewDAAUploadResearcherMessage() throws Exception {
    User researcher = new User();
    researcher.setDisplayName("Jane Evans");
    researcher.setEmail("signingofficial@example.com");

    Dac dac = new Dac();
    dac.setDacId(1);
    dac.setName("DAC-01");

    User user = new User();
    user.setUserId(123);

    String previousDaaName = "DAA-123";

    String newDaaName = "DAA-456";
    when(templateHelper.getTemplate(EmailType.NEW_DAA_UPLOAD_RESEARCHER.templateName))
        .thenReturn(mock());

    service.sendNewDAAUploadResearcherMessage(
        researcher, dac.getName(), previousDaaName, newDaaName, user.getUserId());

    verify(sendGridAPI).sendMessage(any(), any());
    verify(emailDAO)
        .insert(
            eq("DAC-01"),
            eq(null),
            eq(user.getUserId()),
            eq(EmailType.NEW_DAA_UPLOAD_RESEARCHER.getTypeInt()),
            any(),
            any(),
            any(),
            any(),
            any());
  }

  @Test
  void testSendNewDAAUploadSOMessage() throws Exception {
    User signingOfficial = new User();
    signingOfficial.setDisplayName("Jane Evans");
    signingOfficial.setEmail("signingofficial@example.com");

    Dac dac = new Dac();
    dac.setDacId(1);
    dac.setName("DAC-01");

    User user = new User();
    user.setUserId(123);

    String previousDaaName = "DAA-123";

    String newDaaName = "DAA-456";
    when(templateHelper.getTemplate(EmailType.NEW_DAA_UPLOAD_SO.templateName)).thenReturn(mock());

    service.sendNewDAAUploadSOMessage(
        signingOfficial, dac.getName(), previousDaaName, newDaaName, user.getUserId());

    verify(sendGridAPI).sendMessage(any(), any());
    verify(emailDAO)
        .insert(
            eq("DAC-01"),
            eq(null),
            eq(user.getUserId()),
            eq(EmailType.NEW_DAA_UPLOAD_SO.getTypeInt()),
            any(),
            any(),
            any(),
            any(),
            any());
  }

  @Test
  void testSendResearcherCloseoutCompletedMessage() throws Exception {
    User user = new User();
    user.setUserId(123);
    user.setDisplayName("John Doe");
    user.setEmail("jd@somewhere");
    String darCode = "DAR-12345";
    String referenceId = UUID.randomUUID().toString();
    when(templateHelper.getTemplate(EmailType.RESEARCHER_CLOSEOUT_COMPLETED.templateName))
        .thenReturn(mock());

    service.sendResearcherCloseoutCompletedMessage(user, darCode, referenceId);
    verify(sendGridAPI).sendMessage(any(), eq(user.getEmail()));
    verify(emailDAO)
        .insert(
            eq(referenceId),
            isNull(),
            eq(user.getUserId()),
            eq(EmailType.RESEARCHER_CLOSEOUT_COMPLETED.getTypeInt()),
            any(),
            any(),
            any(),
            any(),
            any());
  }

  @Test
  void testFetchEmails() {
    List<MailMessage> mailMessages = generateMailMessageList();
    when(emailDAO.fetchMessagesByType(any(), anyInt(), anyInt())).thenReturn(mailMessages);
    assertEquals(2, service.fetchEmailMessagesByType(EmailType.COLLECT, 20, 0).size());
  }

  @Test
  void testFetchEmailsByCreateDate() {
    List<MailMessage> mailMessages = generateMailMessageList();
    Date startDate = new Date();
    Date endDate = new Date();
    when(emailDAO.fetchMessagesByCreateDate(any(), any(), anyInt(), anyInt()))
        .thenReturn(mailMessages);
    assertEquals(2, service.fetchEmailMessagesByCreateDate(startDate, endDate, 20, 0).size());
  }

  @Test
  void testSendSubmittedCloseoutMessage() throws Exception {
    String darId = "DAR-123";
    String referenceId = "ref-456";
    String closeoutUrl = SERVER_URL + "dar/" + darId + "/closeout";
    when(templateHelper.getTemplate(EmailType.SUBMITTED_CLOSEOUT.templateName)).thenReturn(mock());
    User toUser = new User();
    toUser.setDisplayName("Test User");
    toUser.setEmail("test.user@test.com");
    when(templateHelper.getTemplate(EmailType.SUBMITTED_CLOSEOUT.templateName)).thenReturn(mock());

    service.sendSubmittedCloseoutMessage(toUser, darId, referenceId, closeoutUrl);
    verify(sendGridAPI).sendMessage(any(Mail.class), eq(toUser.getEmail()));
    verify(emailDAO)
        .insert(
            eq(referenceId),
            eq(null),
            eq(toUser.getUserId()),
            eq(EmailType.SUBMITTED_CLOSEOUT.getTypeInt()),
            any(),
            any(),
            any(),
            any(),
            any());
  }

  @Test
  void testSendNewLibraryCardIssuedMessage() throws Exception {
    User toUser = new User();
    toUser.setDisplayName("Test User");
    toUser.setEmail("test.user@test.com");
    when(templateHelper.getTemplate(EmailType.NEW_LIBRARY_CARD_ISSUED.templateName))
        .thenReturn(mock());

    service.sendNewLibraryCardIssuedMessage(toUser);
    verify(sendGridAPI).sendMessage(any(Mail.class), eq(toUser.getEmail()));
    verify(emailDAO)
        .insert(
            eq(toUser.getEmail()),
            eq(null),
            eq(toUser.getUserId()),
            eq(EmailType.NEW_LIBRARY_CARD_ISSUED.getTypeInt()),
            any(),
            any(),
            any(),
            any(),
            any());
  }

  @Test
  void testSendNewDARRADARApprovalToDAC() throws Exception {
    User toUser = new User();
    toUser.setUserId(1);
    toUser.setDisplayName("Test User");
    toUser.setEmail("test.user@test.com");
    User researcherUser = new User();
    researcherUser.setDisplayName("Research User");

    String referenceId = "abc-123";

    when(templateHelper.getTemplate(EmailType.DAC_RADAR_APPROVED.templateName)).thenReturn(mock());

    service.sendNewDARRADARApprovalToDAC(
        toUser,
        "DAR-00001",
        referenceId,
        List.of(new DatasetMailDTO("dataset-name", "DUOS-00123")),
        researcherUser);
    verify(sendGridAPI).sendMessage(any(Mail.class), eq(toUser.getEmail()));
    verify(emailDAO)
        .insert(
            eq(referenceId),
            eq(null),
            eq(toUser.getUserId()),
            eq(EmailType.DAC_RADAR_APPROVED.getTypeInt()),
            any(),
            any(),
            any(),
            any(),
            any());
  }

  @Test
  void testSendEmailToSOWhenApprovalRqdForNewDAR() throws TemplateException, IOException {
    User signingOfficial = new User();
    signingOfficial.setUserId(1);
    signingOfficial.setDisplayName("Test User");
    signingOfficial.setEmail("test.user@test.com");
    User researcherUser = new User();
    researcherUser.setDisplayName("Research User");

    String referenceId = "abc-123";

    when(templateHelper.getTemplate(EmailType.NEW_DAR_SO_NEEDS_TO_APPROVE.templateName))
        .thenReturn(mock());

    service.sendNewDARSigningOfficialRequestEmail(
        signingOfficial, researcherUser.getDisplayName(), referenceId);
    verify(sendGridAPI).sendMessage(any(Mail.class), eq(signingOfficial.getEmail()));
    verify(emailDAO)
        .insert(
            eq(referenceId),
            eq(null),
            eq(signingOfficial.getUserId()),
            eq(EmailType.NEW_DAR_SO_NEEDS_TO_APPROVE.getTypeInt()),
            any(),
            any(),
            any(),
            any(),
            any());
  }

  @Test
  void testSendVoteDigestMessages_no_notices() {
    when(electionDAO.findElectionReminders(anyInt(), anyInt(), anyString())).thenReturn(List.of());
    assertDoesNotThrow(() -> service.sendVoteDigestMessages());
  }

  @Test
  void testSendVoteDigestMessages_continues_to_process_with_error() throws IOException {
    User user = new User();
    user.setDisplayName("Test User");
    user.setEmail("test@example.com");
    user.setUserId(1);

    User user2 = new User();
    user2.setDisplayName("Test User");
    user2.setEmail("test@example.com");
    user2.setUserId(2);

    Reminder user1Reminder = new Reminder(user.getUserId(), "1234", 1234, Instant.now());
    UserVoteReminder user1VoteReminder = new UserVoteReminder(user1Reminder.userId());
    user1VoteReminder.addReminder(user1Reminder);

    Reminder user2Reminder = new Reminder(user2.getUserId(), "1234", 1234, Instant.now());
    UserVoteReminder user2VoteReminder = new UserVoteReminder(user2Reminder.userId());
    user2VoteReminder.addReminder(user2Reminder);

    when(templateHelper.getTemplate(EmailType.DAC_VOTE_REMINDER_DIGEST.templateName))
        .thenReturn(mock());
    when(electionDAO.findElectionReminders(anyInt(), anyInt(), anyString()))
        .thenReturn(List.of(user1VoteReminder, user2VoteReminder));
    when(userDAO.findUserById(user2.getUserId())).thenReturn(user);
    doThrow(new RuntimeException("Some IO Exception")).when(userDAO).findUserById(user.getUserId());

    assertDoesNotThrow(() -> service.sendVoteDigestMessages());

    verify(emailDAO, times(1))
        .insert(
            any(),
            any(),
            eq(user2.getUserId()),
            eq(EmailType.DAC_VOTE_REMINDER_DIGEST.getTypeInt()),
            any(),
            any(),
            any(),
            any(),
            any());
  }

  private List<MailMessage> generateMailMessageList() {
    return Collections.nCopies(2, generateMailMessage());
  }

  private MailMessage generateMailMessage() {
    return new MailMessage(
        randomInt(1, 10),
        randomInt(11, 20),
        randomInt(21, 30),
        randomAlphanumeric(10),
        new Date(),
        randomAlphanumeric(10),
        randomAlphanumeric(10),
        randomInt(31, 40),
        new Date());
  }
}
