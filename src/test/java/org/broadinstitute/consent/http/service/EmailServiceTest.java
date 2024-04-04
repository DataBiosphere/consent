package org.broadinstitute.consent.http.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import freemarker.template.TemplateException;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.configurations.FreeMarkerConfiguration;
import org.broadinstitute.consent.http.configurations.MailConfiguration;
import org.broadinstitute.consent.http.db.DacDAO;
import org.broadinstitute.consent.http.db.DarCollectionDAO;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.MailMessageDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.enumeration.EmailType;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.mail.SendGridAPI;
import org.broadinstitute.consent.http.mail.freemarker.FreeMarkerTemplateHelper;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DarCollection;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.models.mail.MailMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

/**
 * This class can be used to functionally test email notifications as well as unit test. To enable
 * functional tests, configure MailService with correct values (i.e. is active, sendgrid key, etc.)
 * Functional test emails will be directed to the private google group:
 * https://groups.google.com/a/broadinstitute.org/g/duos-dev
 */
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
  private SendGridAPI sendGridAPI;

  FreeMarkerTemplateHelper templateHelper;


  private final static String serverUrl = "http://localhost:8000/#/";

  @BeforeEach
  void setUp() {
  }

  private void initService() {
    boolean serviceActive = false;

    openMocks(this);
    MailConfiguration mConfig = new MailConfiguration();
    mConfig.setActivateEmailNotifications(serviceActive);
    mConfig.setGoogleAccount("");
    mConfig.setSendGridApiKey("");
    sendGridAPI = spy(new SendGridAPI(mConfig, userDAO));

    FreeMarkerConfiguration fmConfig = new FreeMarkerConfiguration();
    fmConfig.setDefaultEncoding("UTF-8");
    fmConfig.setTemplateDirectory("/freemarker");
    templateHelper = spy(new FreeMarkerTemplateHelper(fmConfig));
    service = new EmailService(
        collectionDAO,
        voteDAO,
        electionDAO,
        userDAO,
        emailDAO,
        datasetDAO,
        dacDAO,
        sendGridAPI,
        templateHelper,
        serverUrl);
  }

  @Test
  void testSendNewResearcherEmail() throws Exception {
    initService();
    User user = new User();
    user.setUserId(1234);
    user.setDisplayName("John Doe");

    User so = new User();
    user.setEmail("fake_email@asdf.com");
    try {
      service.sendNewResearcherMessage(user, so);
    } catch (Exception e) {
      fail("Should not fail sending message: " + e);
    }

    verify(sendGridAPI, times(1)).sendNewResearcherLibraryRequestMessage(any(), any());
    verify(templateHelper, times(1)).getNewResearcherLibraryRequestTemplate("John Doe", serverUrl);
    verify(emailDAO, times(1)).insert(
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
    initService();
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

    Map<String, List<String>> dacDatasetGroups = new HashMap<>();
    dacDatasetGroups.put(dac.getName(), List.of(d1.getDatasetIdentifier(),
        d2.getDatasetIdentifier()));

    when(collectionDAO.findDARCollectionByCollectionId(any())).thenReturn(collection);
    when(userDAO.findUserById(any())).thenReturn(researcher);
    when(userDAO.findUserByEmail(chairperson.getEmail())).thenReturn(chairperson);
    when(dacDAO.findDacsForCollectionId(any())).thenReturn(Set.of(dac));
    when(datasetDAO.findDatasetsByIdList(any())).thenReturn(List.of(d1, d2));
    when(userDAO.describeUsersByRoleAndEmailPreference(any(), any())).thenReturn(List.of());
    when(userDAO.findUsersForDatasetsByRole(any(), any())).thenReturn(Set.of(chairperson));


    try {
      service.sendNewDARCollectionMessage(collection.getDarCollectionId());
    } catch (Exception e) {
      fail("Should not fail sending message: " + e);
    }

    verify(sendGridAPI, times(1)).sendNewDARRequests(
        any(),
        any(),
        any(),
        any()
    );
    verify(templateHelper, times(1)).getNewDARRequestTemplate(
        serverUrl,
        chairperson.getDisplayName(),
        dacDatasetGroups,
        researcher.getDisplayName(),
        collection.getDarCode()
    );
    verify(emailDAO, times(1)).insert(
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
    dataset.setDataSetId(RandomUtils.nextInt(1, 100000));
    dataset.setAlias(dataset.getDataSetId());
    dataset.setDatasetIdentifier();
    dataset.setDacId(dacId);
    dataset.setName(String.format("Dataset %s-%s", RandomStringUtils.randomAlphabetic(10), dataset.getDataSetId()));
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

    initService();

    try {
      service.sendDatasetSubmittedMessage(dacChair, dataSubmitter, dacName, datasetName);
    } catch (Exception e) {
      fail("Should not fail sending message: " + e);
    }

    verify(sendGridAPI, times(1)).sendDatasetSubmittedMessage(any(), any());
    verify(templateHelper, times(1)).getDatasetSubmittedTemplate(dacChair.getDisplayName(),
        dataSubmitter.getDisplayName(),
        datasetName, dacName);
    verify(emailDAO, times(1)).insert(
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

    initService();

    try {
      service.sendDaaRequestMessage(signingOfficial.getDisplayName(), signingOfficial.getEmail(),
          user.getDisplayName(), daaName, daaId, user.getUserId());
    } catch (Exception e) {
      fail("Should not fail sending message: " + e);
    }

    verify(sendGridAPI, times(1)).sendDaaRequestMessage(any(), any());
    verify(templateHelper, times(1)).getDaaRequestTemplate(signingOfficial.getDisplayName(),
        user.getDisplayName(),
        daaName, serverUrl);
    verify(emailDAO, times(1)).insert(
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
  void testFetchEmails() {
    List<MailMessage> mailMessages = generateMailMessageList();
    initService();
    when(emailDAO.fetchMessagesByType(any(), anyInt(), anyInt())).thenReturn(mailMessages);
    assertEquals(2,
        service.fetchEmailMessagesByType(EmailType.COLLECT, 20, 0).size());
  }

  @Test
  void testFetchEmailsByCreateDate() {
    List<MailMessage> mailMessages = generateMailMessageList();
    initService();
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
}
