package org.broadinstitute.consent.http.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.sendgrid.Response;
import com.sendgrid.helpers.mail.Mail;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.configurations.ConsentConfiguration;
import org.broadinstitute.consent.http.configurations.MailConfiguration;
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
import org.broadinstitute.consent.http.db.DacDAO;
import org.broadinstitute.consent.http.db.DarCollectionDAO;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.MailMessageDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.EmailType;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.mail.SendGridAPI;
import org.broadinstitute.consent.http.mail.freemarker.FreeMarkerTemplateHelper;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DarCollection;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.models.Vote;
import org.broadinstitute.consent.http.models.mail.MailMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

/**
 * This class can be used to functionally test email notifications as well as unit test. To enable
 * functional tests, configure MailService with correct values (i.e. is active, sendgrid key, etc.)
 * Functional test emails will be directed to the private google group:
 * <a href="https://groups.google.com/a/broadinstitute.org/g/duos-dev">duos-dev</a>
 */
@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

  private EmailService service;
  @Mock
  private DarCollectionDAO collectionDAO;
  @Mock
  private VoteDAO voteDAO;
  @Mock
  private ElectionDAO electionDAO;
  @Mock
  private UserDAO userDAO;
  @Mock
  private MailMessageDAO emailDAO;
  @Mock
  private DatasetDAO datasetDAO;
  @Mock
  private DacDAO dacDAO;
  @Mock
  private DataAccessRequestDAO dataAccessRequestDAO;
  @Mock
  private SendGridAPI sendGridAPI;

  @Mock
  private FreeMarkerTemplateHelper templateHelper;

  private static final String SERVER_URL = "http://localhost:8000/#/";
  private static final String FROM = "from@duos";

  @BeforeEach
  void initService() {
    ConsentConfiguration config = new ConsentConfiguration();
    MailConfiguration mConfig = config.getMailConfiguration();
    mConfig.setGoogleAccount(FROM);

    ServicesConfiguration servicesConfiguration = config.getServicesConfiguration();
    servicesConfiguration.setLocalURL(SERVER_URL);
    service = new EmailService(
        collectionDAO,
        voteDAO,
        electionDAO,
        userDAO,
        emailDAO,
        datasetDAO,
        dacDAO,
        dataAccessRequestDAO,
        sendGridAPI,
        templateHelper,
        config);
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
    var message = new org.broadinstitute.consent.http.mail.message.MailMessage(user, EmailType.COLLECT) {
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
    verify(emailDAO).insert(
        eq("1234"),
        eq(null),
        eq(1234),
        eq(EmailType.NEW_RESEARCHER.getTypeInt()),
        any(),
        any(),
        any(),
        any(),
        any()
    );
  }

  @Test
  void testSendNewDARCollectionMessage() throws TemplateException, IOException {
    User researcher = createUserWithRole(UserRoles.RESEARCHER, null);
    Dac dac = new Dac();
    dac.setDacId(1);
    User chairperson = createUserWithRole(UserRoles.CHAIRPERSON, dac.getDacId());
    dac.setChairpersons(List.of(chairperson));
    dac.setName("DAC-01");

    Dataset d1 = createDataset(dac.getDacId());
    Dataset d2 = createDataset(dac.getDacId());

    DarCollection collection = new DarCollection();
    collection.setDarCode("01");
    collection.setDarCollectionId(1);
    collection.setDatasets(Set.of(d1, d2));
    DataAccessRequest dar = new DataAccessRequest();
    dar.setReferenceId(UUID.randomUUID().toString());
    dar.setSubmissionDate(Timestamp.from(Instant.now()));
    dar.setDatasetIds(List.of(d1.getDatasetId(), d2.getDatasetId()));
    collection.setDars(Map.of(dar.getReferenceId(), dar));


    when(collectionDAO.findDARCollectionByCollectionId(any())).thenReturn(collection);
    when(userDAO.findUserById(any())).thenReturn(researcher);
    when(dacDAO.findDacsForDatasetIds(any())).thenReturn(Set.of(dac));
    when(datasetDAO.findDatasetsByIdList(any())).thenReturn(List.of(d1, d2));
    when(userDAO.describeUsersByRoleAndEmailPreference(any(), any())).thenReturn(List.of());
    when(userDAO.findUsersForDatasetsByRole(any(), any())).thenReturn(Set.of(chairperson));
    when(templateHelper.getTemplate(EmailType.NEW_DAR.templateName)).thenReturn(mock());

    service.sendNewDARCollectionMessage(collection.getDarCollectionId());

    verify(sendGridAPI).sendMessage(any(), any());
    verify(emailDAO).insert(
        eq("01"),
        eq(null),
        eq(chairperson.getUserId()),
        eq(EmailType.NEW_DAR.getTypeInt()),
        any(),
        any(),
        any(),
        any(),
        any()
    );
  }

  private Dataset createDataset(Integer dacId) {
    Dataset dataset = new Dataset();
    dataset.setDatasetId(RandomUtils.nextInt(1, 100000));
    dataset.setAlias(dataset.getDatasetId());
    dataset.setDatasetIdentifier();
    dataset.setDacId(dacId);
    dataset.setName(String.format("Dataset %s-%s", RandomStringUtils.randomAlphabetic(10),
        dataset.getDatasetId()));
    return dataset;
  }

  private User createUserWithRole(UserRoles userRoles, Integer dacId) {
    User user = new User();
    user.setUserId(RandomUtils.nextInt(1, 100000));
    user.setDisplayName(String.format("%s - %s", userRoles.getRoleName(), user.getUserId()));
    user.setEmail(String.format("%s@test.com", userRoles.getRoleName()));
    UserRole role = new UserRole(
        userRoles.getRoleId(),
        userRoles.getRoleName()
    );
    if (dacId != null) {
      role.setDacId(dacId);
    }
    user.setRoles(List.of(role));
    user.setEmailPreference(Boolean.TRUE);
    return user;
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
    verify(emailDAO).insert(
        eq(datasetName),
        eq(null),
        eq(456),
        eq(EmailType.NEW_DATASET.getTypeInt()),
        any(),
        any(),
        any(),
        any(),
        any()
    );
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
    verify(emailDAO).insert(
        eq("456"),
        eq(null),
        eq(user.getUserId()),
        eq(EmailType.NEW_DAA_REQUEST.getTypeInt()),
        any(),
        any(),
        any(),
        any(),
        any()
    );
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
    when(templateHelper.getTemplate(EmailType.NEW_DAA_UPLOAD_RESEARCHER.templateName)).thenReturn(
        mock());

    service.sendNewDAAUploadResearcherMessage(
        researcher, dac.getName(), previousDaaName, newDaaName, user.getUserId());

    verify(sendGridAPI).sendMessage(any(), any());
    verify(emailDAO).insert(
        eq("DAC-01"),
        eq(null),
        eq(user.getUserId()),
        eq(EmailType.NEW_DAA_UPLOAD_RESEARCHER.getTypeInt()),
        any(),
        any(),
        any(),
        any(),
        any()
    );
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

    service.sendNewDAAUploadSOMessage(signingOfficial,
        dac.getName(), previousDaaName, newDaaName, user.getUserId());

    verify(sendGridAPI).sendMessage(any(), any());
    verify(emailDAO).insert(
        eq("DAC-01"),
        eq(null),
        eq(user.getUserId()),
        eq(EmailType.NEW_DAA_UPLOAD_SO.getTypeInt()),
        any(),
        any(),
        any(),
        any(),
        any()
    );
  }

  @Test
  void testFetchEmails() {
    List<MailMessage> mailMessages = generateMailMessageList();
    when(emailDAO.fetchMessagesByType(any(), anyInt(), anyInt())).thenReturn(mailMessages);
    assertEquals(2,
        service.fetchEmailMessagesByType(EmailType.COLLECT, 20, 0).size());
  }

  @Test
  void testFetchEmailsByCreateDate() {
    List<MailMessage> mailMessages = generateMailMessageList();
    Date startDate = new Date();
    Date endDate = new Date();
    when(emailDAO.fetchMessagesByCreateDate(any(), any(), anyInt(), anyInt())).thenReturn(
        mailMessages);
    assertEquals(2,
        service.fetchEmailMessagesByCreateDate(startDate, endDate, 20, 0).size());
  }

  private List<MailMessage> generateMailMessageList() {
    return Collections.nCopies(2, generateMailMessage());
  }

  private MailMessage generateMailMessage() {
    return new MailMessage(
        RandomUtils.nextInt(),
        RandomUtils.nextInt(),
        RandomUtils.nextInt(),
        RandomStringUtils.randomAlphanumeric(10),
        new Date(),
        RandomStringUtils.randomAlphanumeric(10),
        RandomStringUtils.randomAlphanumeric(10),
        RandomUtils.nextInt(),
        new Date()
    );
  }

  @Test
  void testSendReminderMessage() throws Exception {
    Election election = new Election();
    election.setElectionId(RandomUtils.nextInt());
    election.setReferenceId(UUID.randomUUID().toString());
    election.setElectionType(ElectionType.DATA_ACCESS.getValue());
    when(electionDAO.findElectionWithFinalVoteById(any())).thenReturn(election);

    Vote vote = new Vote();
    vote.setVoteId(RandomUtils.nextInt());
    vote.setElectionId(election.getElectionId());
    when(voteDAO.findVoteById(any())).thenReturn(vote);

    DarCollection collection = new DarCollection();
    collection.setDarCollectionId(RandomUtils.nextInt());
    collection.setDarCode("DAR-12345");
    when(collectionDAO.findDARCollectionByReferenceId(any())).thenReturn(collection);

    User user = new User();
    user.setDisplayName(RandomStringUtils.randomAlphanumeric(10));
    user.setEmail(RandomStringUtils.randomAlphanumeric(10));
    when(userDAO.findUserById(any())).thenReturn(user);

    when(templateHelper.getTemplate(EmailType.REMINDER.templateName)).thenReturn(mock());

    service.sendReminderMessage(vote.getVoteId());
    verify(sendGridAPI).sendMessage(any(), any());
    verify(emailDAO)
        .insert(
            eq(String.valueOf(vote.getElectionId())),
            eq(vote.getVoteId()),
            eq(user.getUserId()),
            eq(EmailType.REMINDER.getTypeInt()),
            any(),
            any(),
            any(),
            any(),
            any());
    verify(voteDAO).updateVoteReminderFlag(vote.getVoteId(), true);
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
    verify(emailDAO).insert(
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
    when(templateHelper.getTemplate(EmailType.DAR_EXPIRATION_REMINDER.templateName)).thenReturn(
        mock());

    service.sendDarExpirationReminderMessage(user, darCode, otherUserId, referenceId);
    verify(sendGridAPI).sendMessage(any(), eq(user.getEmail()));
    verify(emailDAO).insert(
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
  void sendExpirationNoticesTest() throws IOException {
    User user1 = new User();
    user1.setUserId(123);
    user1.setDisplayName("John Doe");
    user1.setEmail("jd@somewhere");

    User user2 = new User();
    user2.setUserId(124);
    user2.setDisplayName("Jane Doe");
    user2.setEmail("jd@somewhereelse");

    DataAccessRequest dar1 = getMockedDar("DAR-12345", UUID.randomUUID().toString(), user1);
    DataAccessRequest dar2 = getMockedDar("DAR-12346", UUID.randomUUID().toString(), user2);

    when(userDAO.findUserById(user1.getUserId())).thenReturn(user1);
    when(userDAO.findUserById(user2.getUserId())).thenReturn(user2);
    List<DataAccessRequest> dars = List.of(dar1, dar2);
    when(dataAccessRequestDAO.findAgedDARsByEmailTypeOlderThanInterval(any(), any(), any())).thenReturn(dars);
    when(templateHelper.getTemplate(EmailType.DAR_EXPIRATION_REMINDER.templateName)).thenReturn(mock());
    when(templateHelper.getTemplate(EmailType.DAR_EXPIRED.templateName)).thenReturn(mock());

    assertDoesNotThrow(() -> service.sendExpirationNotices());
    verify(emailDAO)
        .insert(
            eq(dar1.getReferenceId()),
            isNull(),
            eq(user1.getUserId()),
            eq(EmailType.DAR_EXPIRED.getTypeInt()),
            any(),
            any(),
            any(),
            any(),
            any());
    verify(emailDAO)
        .insert(
            eq(dar1.getReferenceId()),
            isNull(),
            eq(user1.getUserId()),
            eq(EmailType.DAR_EXPIRATION_REMINDER.getTypeInt()),
            any(),
            any(),
            any(),
            any(),
            any());
    verify(emailDAO)
        .insert(
            eq(dar2.getReferenceId()),
            isNull(),
            eq(user2.getUserId()),
            eq(EmailType.DAR_EXPIRED.getTypeInt()),
            any(),
            any(),
            any(),
            any(),
            any());
    verify(emailDAO)
        .insert(
            eq(dar2.getReferenceId()),
            isNull(),
            eq(user2.getUserId()),
            eq(EmailType.DAR_EXPIRATION_REMINDER.getTypeInt()),
            any(),
            any(),
            any(),
            any(),
            any());
    verify(emailDAO, times(4))
        .insert(any(), any(), any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  void sendExpirationNoticesTestMissingEmailForOneUser() throws IOException {
    ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
    ch.qos.logback.classic.Logger log = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(EmailService.class);
    listAppender.start();
    log.addAppender(listAppender);
    User user1 = new User();
    user1.setUserId(123);
    user1.setDisplayName("John Doe");
    user1.setEmail("jd@somewhere");

    User user2 = new User();
    user2.setUserId(124);
    user2.setDisplayName("Jane Doe");

    DataAccessRequest dar1 =getMockedDar("DAR-12345", UUID.randomUUID().toString(), user1);
    DataAccessRequest dar2 = getMockedDar("DAR-12346", UUID.randomUUID().toString(), user2);

    when(userDAO.findUserById(user1.getUserId())).thenReturn(user1);
    when(userDAO.findUserById(user2.getUserId())).thenReturn(user2);

    List<DataAccessRequest> dars = List.of(dar2, dar1);

    when(dataAccessRequestDAO.findAgedDARsByEmailTypeOlderThanInterval(any(), any(), any())).thenReturn(dars);
    when(templateHelper.getTemplate(EmailType.DAR_EXPIRATION_REMINDER.templateName)).thenReturn(mock());
    when(templateHelper.getTemplate(EmailType.DAR_EXPIRED.templateName)).thenReturn(mock());

    assertDoesNotThrow(()->service.sendExpirationNotices());

    verify(emailDAO).insert(eq(dar1.getReferenceId()),isNull(), eq(user1.getUserId()), eq(EmailType.DAR_EXPIRED.getTypeInt()), any(), any(), any(), any(), any());
    verify(emailDAO).insert(eq(dar1.getReferenceId()),isNull(), eq(user1.getUserId()), eq(EmailType.DAR_EXPIRATION_REMINDER.getTypeInt()), any(), any(), any(), any(), any());
    verify(emailDAO, times(2)).insert(any(), any(), any(), any(), any(), any(), any(), any(), any());
    assertEquals(2, listAppender.list.size());
  }

  @Test
  void sendExpirationNoticesTestUnderlyingExceptionThrownSendingOneTypeOfMessage() throws IOException {
    ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
    ch.qos.logback.classic.Logger log = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(EmailService.class);
    listAppender.start();
    log.addAppender(listAppender);
    User user1 = new User();
    user1.setUserId(123);
    user1.setDisplayName("John Doe");
    user1.setEmail("jd@somewhere");

    User user2 = new User();
    user2.setUserId(124);
    user2.setDisplayName("Jane Doe");
    user2.setEmail("jane@somewhere");

    DataAccessRequest dar1 =getMockedDar("DAR-12345", UUID.randomUUID().toString(), user1);
    DataAccessRequest dar2 = getMockedDar("DAR-12346", UUID.randomUUID().toString(), user2);

    when(userDAO.findUserById(user1.getUserId())).thenReturn(user1);
    when(userDAO.findUserById(user2.getUserId())).thenReturn(user2);

    List<DataAccessRequest> dars = List.of(dar2, dar1);

    when(dataAccessRequestDAO.findAgedDARsByEmailTypeOlderThanInterval(any(), any(), any())).thenReturn(dars);
    when(templateHelper.getTemplate(EmailType.DAR_EXPIRED.templateName)).thenReturn(mock());

    assertDoesNotThrow(()->service.sendExpirationNotices());

    verify(emailDAO).insert(eq(dar1.getReferenceId()),isNull(), eq(user1.getUserId()), eq(EmailType.DAR_EXPIRED.getTypeInt()), any(), any(), any(), any(), any());
    verify(emailDAO).insert(eq(dar2.getReferenceId()),isNull(), eq(user2.getUserId()), eq(EmailType.DAR_EXPIRED.getTypeInt()), any(), any(), any(), any(), any());
    verify(emailDAO, times(2)).insert(any(), any(), any(), any(), any(), any(), any(), any(), any());
    assertEquals(2, listAppender.list.size());
  }

  private DataAccessRequest getMockedDar(String darCode, String referenceId, User user) {
    DataAccessRequest dar = mock(DataAccessRequest.class);
    when(dar.getReferenceId()).thenReturn(referenceId);
    when(dar.getDarCode()).thenReturn(darCode);
    when(dar.getUserId()).thenReturn(user.getUserId());
    return dar;
  }
}
