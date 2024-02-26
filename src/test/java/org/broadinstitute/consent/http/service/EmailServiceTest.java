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
import org.broadinstitute.consent.http.models.DataAccessRequest;
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
    User researcher = new User();
    researcher.setUserId(1234);
    researcher.setDisplayName("Researcher");
    researcher.setEmail("researcher@abc.com");
    UserRole researcherRole = new UserRole(
        UserRoles.RESEARCHER.getRoleId(),
        UserRoles.RESEARCHER.getRoleName()
    );
    researcher.setRoles(List.of(researcherRole));

    User chairperson = new User();
    chairperson.setUserId(1234);
    chairperson.setDisplayName("Chairperson");
    chairperson.setEmail("chairperson@abc.com");
    UserRole chairpersonRole = new UserRole(
        UserRoles.CHAIRPERSON.getRoleId(),
        UserRoles.CHAIRPERSON.getRoleName()
    );
    chairpersonRole.setDacId(1);
    chairperson.setRoles(List.of(chairpersonRole));
    chairperson.setEmailPreference(Boolean.TRUE);

    Dac dac = new Dac();
    dac.setDacId(1);
    dac.setChairpersons(List.of(chairperson));
    dac.setMembers(List.of(researcher));
    dac.setName("DAC-01");

    Dataset d1 = new Dataset();
    d1.setDataSetId(1);
    d1.setAlias(1);
    d1.setDatasetIdentifier();
    d1.setDacId(1);
    d1.setName("d1");

    Dataset d2 = new Dataset();
    d2.setDataSetId(2);
    d2.setAlias(2);
    d2.setDatasetIdentifier();
    d2.setDacId(1);
    d2.setName("d2");

    DataAccessRequest dar = new DataAccessRequest();
    dar.setId(1);
    dar.setDatasetIds(List.of(1, 2));
    dar.setCollectionId(1);
    dar.setUserId(researcher.getUserId());

    DarCollection collection = new DarCollection();
    collection.setDarCode("01");
    collection.setDarCollectionId(1);
    collection.setDatasets(Set.of(d1, d2));
    System.out.println(collection);

    Map<String, List<String>> dacDatasetGroups = new HashMap<>();
    dacDatasetGroups.put(dac.getName(), List.of(d1.getDatasetIdentifier(),
        d2.getDatasetIdentifier()));

    when(collectionDAO.findDARCollectionByCollectionId(any())).thenReturn(collection);
    when(userDAO.findUserById(any())).thenReturn(researcher);
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
        eq(1234),
        eq(EmailType.NEW_DAR.getTypeInt()),
        any(),
        any(),
        any(),
        any(),
        any()
    );
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
