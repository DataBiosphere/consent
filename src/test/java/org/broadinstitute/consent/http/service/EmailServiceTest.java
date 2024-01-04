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

import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.configurations.FreeMarkerConfiguration;
import org.broadinstitute.consent.http.configurations.MailConfiguration;
import org.broadinstitute.consent.http.db.DarCollectionDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.MailMessageDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.enumeration.EmailType;
import org.broadinstitute.consent.http.mail.SendGridAPI;
import org.broadinstitute.consent.http.mail.freemarker.FreeMarkerTemplateHelper;
import org.broadinstitute.consent.http.models.User;
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
    service = new EmailService(collectionDAO, voteDAO, electionDAO, userDAO,
        emailDAO, sendGridAPI, templateHelper, serverUrl);
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
