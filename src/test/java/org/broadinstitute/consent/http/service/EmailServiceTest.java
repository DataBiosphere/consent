package org.broadinstitute.consent.http.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
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
import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.broadinstitute.consent.http.AbstractTestHelper;
import org.broadinstitute.consent.http.configurations.ConsentConfiguration;
import org.broadinstitute.consent.http.configurations.MailConfiguration;
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
import org.broadinstitute.consent.http.db.DAOContainer;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.MailMessageDAO;
import org.broadinstitute.consent.http.db.StudyDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.enumeration.EmailType;
import org.broadinstitute.consent.http.mail.SendGridAPI;
import org.broadinstitute.consent.http.mail.freemarker.FreeMarkerTemplateHelper;
import org.broadinstitute.consent.http.models.Reminder;
import org.broadinstitute.consent.http.models.StudyDatasetCountRecord;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserVoteReminder;
import org.broadinstitute.consent.http.models.mail.MailMessage;
import org.broadinstitute.consent.http.models.mail.MailMessageInsert;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.HandleConsumer;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.result.ResultIterable;
import org.jdbi.v3.core.result.ResultIterator;
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
  @Mock private DatasetDAO datasetDAO;
  @Mock private StudyDAO studyDAO;
  @Mock private SendGridAPI sendGridAPI;
  @Mock private FreeMarkerTemplateHelper templateHelper;
  @Mock private DAOContainer daoContainer;

  @BeforeEach
  void initService() {
    ConsentConfiguration config = new ConsentConfiguration();
    MailConfiguration mConfig = config.getMailConfiguration();
    mConfig.setGoogleAccount(FROM);

    ServicesConfiguration servicesConfiguration = config.getServicesConfiguration();
    servicesConfiguration.setLocalURL(SERVER_URL);
    when(daoContainer.getElectionDAO()).thenReturn(electionDAO);
    when(daoContainer.getUserDAO()).thenReturn(userDAO);
    when(daoContainer.getMailMessageDAO()).thenReturn(emailDAO);
    when(daoContainer.getDatasetDAO()).thenReturn(datasetDAO);
    when(daoContainer.getStudyDAO()).thenReturn(studyDAO);
    service = new EmailService(daoContainer, sendGridAPI, templateHelper, config);
  }

  @Test
  void testSendMessage() throws Exception {
    String userEmail = "user@duos";
    User user = new User();
    user.setEmail(userEmail);
    String subject = "subject";
    Map<String, Object> model = Map.of("key", "value");
    String entityReferenceId = "entityReferenceId";
    Integer userId = 1234;
    Integer voteId = 4567;
    var message =
        createMailMessage(
            user, EmailType.NEW_CASE, subject, model, entityReferenceId, voteId, null);
    Template template = mock();
    when(templateHelper.getTemplate(EmailType.NEW_CASE.templateName)).thenReturn(template);
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
    assertEquals(
        user.getEmail(), mail.getPersonalization().getFirst().getTos().getFirst().getEmail());
    assertEquals(subject, mail.getSubject());

    // Verify sendgrid categories are configured for NEW_CASE and applied to the outgoing mail
    assertTrue(
        EmailType.NEW_CASE.getSendGridCategories() != null
            && !EmailType.NEW_CASE.getSendGridCategories().isEmpty(),
        "Expected NEW_CASE to define SendGrid categories");
    assertEquals(EmailType.NEW_CASE.getSendGridCategories(), mail.getCategories());

    verify(emailDAO)
        .insert(
            argThat(
                (MailMessageInsert m) ->
                    Objects.equals(m.entityReferenceId(), entityReferenceId)
                        && Objects.equals(m.voteId(), voteId)
                        && Objects.equals(m.userId(), 1234)
                        && Objects.equals(m.emailType(), EmailType.NEW_CASE.getTypeInt())
                        && Objects.equals(m.dateSent(), Date.from(fixedInstant))
                        && Objects.equals(m.emailText(), emailText)
                        && Objects.equals(m.sendgridResponse(), response.getBody())
                        && Objects.equals(m.sendgridStatus(), response.getStatusCode())));
  }

  @Test
  @SuppressWarnings("deprecation")
  void testSendMessage_SkipsCategoriesAndPersistsNullSendGridValues_WhenResponseIsNull()
      throws Exception {
    String userEmail = "user@duos";
    User user = new User();
    user.setEmail(userEmail);
    String subject = "subject";
    Map<String, Object> model = Map.of("key", "value");
    String entityReferenceId = "entityReferenceId";
    Integer userId = 1234;
    String templateName = "test-template.ftl";
    var message =
        createMailMessage(
            user, EmailType.NEW_DAA_REQUEST, subject, model, entityReferenceId, null, templateName);
    Template template = mock();
    when(templateHelper.getTemplate(templateName)).thenReturn(template);
    when(sendGridAPI.sendMessage(any(), any())).thenReturn(null);
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

    service.sendMessage(message, userId);

    var captor = ArgumentCaptor.forClass(Mail.class);
    verify(sendGridAPI).sendMessage(captor.capture(), eq(user.getEmail()));
    var mail = captor.getValue();
    assertNull(mail.getCategories());

    verify(emailDAO)
        .insert(
            argThat(
                (MailMessageInsert m) ->
                    Objects.equals(m.entityReferenceId(), entityReferenceId)
                        && m.voteId() == null
                        && Objects.equals(m.userId(), userId)
                        && Objects.equals(m.emailType(), EmailType.NEW_DAA_REQUEST.getTypeInt())
                        && m.dateSent() == null
                        && Objects.equals(m.emailText(), emailText)
                        && m.sendgridResponse() == null
                        && m.sendgridStatus() == null));
  }

  @Test
  void testSendMessage_DoesNotSetDateSent_WhenSendGridReturnsErrorResponse() throws Exception {
    String userEmail = "user@duos";
    User user = new User();
    user.setEmail(userEmail);
    String subject = "subject";
    Map<String, Object> model = Map.of("key", "value");
    String entityReferenceId = "entityReferenceId";
    Integer userId = 1234;
    Integer voteId = 4567;
    var message =
        createMailMessage(
            user, EmailType.NEW_CASE, subject, model, entityReferenceId, voteId, null);
    Template template = mock();
    when(templateHelper.getTemplate(EmailType.NEW_CASE.templateName)).thenReturn(template);
    Response response = new Response();
    response.setStatusCode(500);
    response.setBody("error");
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

    service.sendMessage(message, userId);

    verify(emailDAO)
        .insert(
            argThat(
                (MailMessageInsert m) ->
                    Objects.equals(m.entityReferenceId(), entityReferenceId)
                        && Objects.equals(m.voteId(), voteId)
                        && Objects.equals(m.userId(), userId)
                        && Objects.equals(m.emailType(), EmailType.NEW_CASE.getTypeInt())
                        && m.dateSent() == null
                        && Objects.equals(m.emailText(), emailText)
                        && Objects.equals(m.sendgridResponse(), response.getBody())
                        && Objects.equals(m.sendgridStatus(), response.getStatusCode())));
  }

  @Test
  void testFetchEmails() {
    List<MailMessage> mailMessages = generateMailMessageList();
    when(emailDAO.fetchMessagesByType(any(), anyInt(), anyInt())).thenReturn(mailMessages);
    assertEquals(2, service.fetchEmailMessagesByType(EmailType.COLLECT, 20, 0).size());
  }

  @Test
  void testFetchEmailsByUserId() {
    Integer userId = 123;
    List<MailMessage> mailMessages = generateMailMessageList();
    when(emailDAO.fetchMessagesByUserId(eq(userId), anyInt(), anyInt())).thenReturn(mailMessages);

    assertEquals(2, service.fetchEmailMessagesByUserId(userId, 20, 0).size());
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
            argThat(
                m ->
                    Objects.equals(m.userId(), user2.getUserId())
                        && Objects.equals(
                            m.emailType(), EmailType.DAC_VOTE_REMINDER_DIGEST.getTypeInt())));
  }

  @Test
  void testSendNewDatasetInDUOSNotifications_No_New_Datasets() {
    when(datasetDAO.getRecentDacApprovedDatasetStudyIds()).thenReturn(List.of());
    when(datasetDAO.getRecentlyCreatedOpenOrExternalDatasetStudyIds()).thenReturn(List.of());
    when(studyDAO.findNameAndDatasetCount(any())).thenReturn(List.of());

    service.sendNewDatasetInDUOSNotifications();

    verify(userDAO, times(0)).getHandle();
  }

  @Test
  void testSendNewDatasetInDUOSNotifications_Null_Datasets() {
    when(datasetDAO.getRecentDacApprovedDatasetStudyIds()).thenReturn(List.of());
    when(datasetDAO.getRecentlyCreatedOpenOrExternalDatasetStudyIds()).thenReturn(List.of());
    when(studyDAO.findNameAndDatasetCount(any())).thenReturn(null);

    service.sendNewDatasetInDUOSNotifications();

    verify(userDAO, times(0)).getHandle();
  }

  @Test
  void testSendNewDatasetInDUOSNotifications() throws IOException {
    User toUser = new User();
    toUser.setDisplayName("Test User");
    toUser.setUserId(1);
    toUser.setEmail("test@example.com");
    toUser.setEmailPreference(true);
    when(datasetDAO.getRecentDacApprovedDatasetStudyIds()).thenReturn(List.of());
    when(datasetDAO.getRecentlyCreatedOpenOrExternalDatasetStudyIds()).thenReturn(List.of());
    when(studyDAO.findNameAndDatasetCount(any()))
        .thenReturn(List.of(new StudyDatasetCountRecord("New Study", 1, 7)));
    Handle handle = mock(Handle.class);
    Jdbi jdbi = mock(Jdbi.class);
    when(userDAO.getHandle()).thenReturn(handle);
    when(handle.getJdbi()).thenReturn(jdbi);
    doAnswer(
            invocation -> {
              HandleConsumer<Exception> consumer = invocation.getArgument(0);
              consumer.useHandle(handle);
              return null;
            })
        .when(jdbi)
        .useHandle(any());
    @SuppressWarnings("unchecked")
    ResultIterator<User> mockIterator = mock(ResultIterator.class);
    when(mockIterator.hasNext()).thenReturn(true, false);
    when(mockIterator.next()).thenReturn(toUser);
    when(userDAO.allEmailReceivingThinlyPopulatedUsers(any(), any()))
        .thenReturn(ResultIterable.of(mockIterator));
    when(templateHelper.getTemplate(EmailType.NEW_STUDY_DIGEST.templateName)).thenReturn(mock());
    EmailService.sendgridThrottleMessageCount = 500;
    EmailService.sendgridThrottleResetTime = 60;
    service.sendNewDatasetInDUOSNotifications();
    verify(userDAO, times(1)).getHandle();
    verify(emailDAO, times(1))
        .insert(
            argThat(
                m ->
                    Objects.equals(m.userId(), toUser.getUserId())
                        && Objects.equals(m.emailType(), EmailType.NEW_STUDY_DIGEST.getTypeInt())));
  }

  @Test
  void testSendNewDatasetInDUOSNotifications_IOException() throws IOException {
    User toUser = new User();
    toUser.setDisplayName("Test User");
    toUser.setUserId(1);
    toUser.setEmail("test@example.com");
    toUser.setEmailPreference(true);
    when(datasetDAO.getRecentDacApprovedDatasetStudyIds()).thenReturn(List.of());
    when(datasetDAO.getRecentlyCreatedOpenOrExternalDatasetStudyIds()).thenReturn(List.of());
    when(studyDAO.findNameAndDatasetCount(any()))
        .thenReturn(List.of(new StudyDatasetCountRecord("New Study", 1, 7)));
    Handle handle = mock(Handle.class);
    Jdbi jdbi = mock(Jdbi.class);
    when(userDAO.getHandle()).thenReturn(handle);
    when(handle.getJdbi()).thenReturn(jdbi);
    doAnswer(
            invocation -> {
              HandleConsumer<Exception> consumer = invocation.getArgument(0);
              consumer.useHandle(handle);
              return null;
            })
        .when(jdbi)
        .useHandle(any());
    @SuppressWarnings("unchecked")
    ResultIterator<User> mockIterator = mock(ResultIterator.class);
    when(mockIterator.hasNext()).thenReturn(true, false);
    when(mockIterator.next()).thenReturn(toUser);
    when(userDAO.allEmailReceivingThinlyPopulatedUsers(any(), any()))
        .thenReturn(ResultIterable.of(mockIterator));
    doThrow(new IOException("Some exception", null))
        .when(templateHelper)
        .getTemplate(EmailType.NEW_STUDY_DIGEST.templateName);
    EmailService.sendgridThrottleMessageCount = 1;
    EmailService.sendgridThrottleResetTime = 1;
    try {
      Thread.currentThread().interrupt();
      service.sendNewDatasetInDUOSNotifications();
    } finally {
      boolean interruptStatusCleared = Thread.interrupted();
      assertTrue(interruptStatusCleared || !Thread.currentThread().isInterrupted());
      EmailService.sendgridThrottleMessageCount = 500;
      EmailService.sendgridThrottleResetTime = 60;
    }
    verify(userDAO, times(1)).getHandle();
  }

  @Test
  void testSendNewDatasetInDUOSNotifications_ReinterruptsThreadWhenThrottleSleepIsInterrupted()
      throws IOException {
    User toUser = new User();
    toUser.setDisplayName("Test User");
    toUser.setUserId(1);
    toUser.setEmail("test@example.com");
    toUser.setEmailPreference(true);
    when(datasetDAO.getRecentDacApprovedDatasetStudyIds()).thenReturn(List.of());
    when(datasetDAO.getRecentlyCreatedOpenOrExternalDatasetStudyIds()).thenReturn(List.of());
    when(studyDAO.findNameAndDatasetCount(any()))
        .thenReturn(List.of(new StudyDatasetCountRecord("New Study", 1, 7)));
    Handle handle = mock(Handle.class);
    Jdbi jdbi = mock(Jdbi.class);
    when(userDAO.getHandle()).thenReturn(handle);
    when(handle.getJdbi()).thenReturn(jdbi);
    doAnswer(
            invocation -> {
              HandleConsumer<Exception> consumer = invocation.getArgument(0);
              consumer.useHandle(handle);
              return null;
            })
        .when(jdbi)
        .useHandle(any());
    @SuppressWarnings("unchecked")
    ResultIterator<User> mockIterator = mock(ResultIterator.class);
    when(mockIterator.hasNext()).thenReturn(true, false);
    when(mockIterator.next()).thenReturn(toUser);
    when(userDAO.allEmailReceivingThinlyPopulatedUsers(any(), any()))
        .thenReturn(ResultIterable.of(mockIterator));
    when(templateHelper.getTemplate(EmailType.NEW_STUDY_DIGEST.templateName)).thenReturn(mock());
    Response response = new Response();
    response.setStatusCode(202);
    response.setBody("accepted");
    when(sendGridAPI.sendMessage(any(), any())).thenReturn(response);
    EmailService.sendgridThrottleMessageCount = 1;
    EmailService.sendgridThrottleResetTime = 1;

    try {
      Thread.currentThread().interrupt();
      service.sendNewDatasetInDUOSNotifications();
      assertTrue(Thread.currentThread().isInterrupted());
    } finally {
      boolean interruptStatusCleared = Thread.interrupted();
      assertTrue(interruptStatusCleared || !Thread.currentThread().isInterrupted());
      EmailService.sendgridThrottleMessageCount = 500;
      EmailService.sendgridThrottleResetTime = 60;
    }

    verify(userDAO, times(1)).getHandle();
    verify(emailDAO, times(1))
        .insert(
            argThat(
                m ->
                    Objects.equals(m.userId(), toUser.getUserId())
                        && Objects.equals(m.emailType(), EmailType.NEW_STUDY_DIGEST.getTypeInt())));
  }

  private List<MailMessage> generateMailMessageList() {
    return Collections.nCopies(2, generateMailMessage());
  }

  private org.broadinstitute.consent.http.mail.message.MailMessage createMailMessage(
      User user,
      EmailType emailType,
      String subject,
      Object model,
      String entityReferenceId,
      Integer voteId,
      String templateNameOverride) {
    return new org.broadinstitute.consent.http.mail.message.MailMessage(user, emailType) {
      @Override
      public String getTemplateName() {
        return templateNameOverride != null ? templateNameOverride : super.getTemplateName();
      }

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
  }

  private MailMessage generateMailMessage() {
    return new MailMessage(
        randomAlphanumeric(10),
        randomInt(1, 10),
        randomInt(11, 20),
        randomInt(21, 30),
        randomInt(1, 10),
        new Date(),
        randomAlphanumeric(10),
        randomAlphanumeric(10),
        randomInt(31, 40),
        new Date());
  }
}
