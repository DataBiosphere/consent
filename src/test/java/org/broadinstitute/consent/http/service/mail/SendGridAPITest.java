package org.broadinstitute.consent.http.service.mail;

import static org.junit.jupiter.api.Assertions.fail;

import com.sendgrid.SendGrid;
import java.io.Writer;
import org.broadinstitute.consent.http.configurations.MailConfiguration;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.mail.SendGridAPI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SendGridAPITest {

  private static final String TO = "to@broadinstitute.org";
  private static final String ID = "DUL-123";
  private static final String TYPE = "Data Use Limitations";
  private SendGridAPI sendGridAPI;

  @Mock
  private SendGrid sendGrid;

  @Mock
  private Writer template;

  @Mock
  private UserDAO userDAO;

  @BeforeEach
  void setUp() throws Exception {
    // For most tests, we don't want to actually make an external call to SendGrid.
    configureApi(false);
  }

  private void configureApi(boolean active) {
    MailConfiguration config = new MailConfiguration();
    config.setSendGridApiKey("test");
    config.setGoogleAccount("from@broadinstitute.org");
    config.setActivateEmailNotifications(active);
    sendGridAPI = new SendGridAPI(config, userDAO);
    sendGridAPI.setSendGrid(sendGrid);
  }

  @Test
  void testNewCaseMessage() {
    try {
      sendGridAPI.sendNewCaseMessage(TO, ID, TYPE, template);
    } catch (Exception e) {
      fail("Should not throw exception");
    }
  }

  @Test
  void testReminderMessage() {
    try {
      sendGridAPI.sendReminderMessage(TO, ID, TYPE, template);
    } catch (Exception e) {
      fail("Should not throw exception");
    }
  }

  @Test
  void testDisabledDatasetMessage() {
    try {
      sendGridAPI.sendDisabledDatasetMessage(TO, ID, TYPE, template);
    } catch (Exception e) {
      fail("Should not throw exception");
    }
  }

  @Test
  void testNewDARRequests() {
    try {
      sendGridAPI.sendNewDARRequests(TO, ID, TYPE, template);
    } catch (Exception e) {
      fail("Should not throw exception");
    }
  }

  @Test
  void testNewResearcherApprovedMessage() {
    try {
      sendGridAPI.sendNewResearcherApprovedMessage(TO, template, "Test");
    } catch (Exception e) {
      fail("Should not throw exception");
    }
  }

  @Test
  void testSendDataCustodianApprovalMessage() {
    try {
      sendGridAPI.sendDataCustodianApprovalMessage(TO, "Test", template);
    } catch (Exception e) {
      fail("Should not throw exception");
    }
  }

  @Test
  void testSendDatasetSubmittedMessage() {
    try {
      sendGridAPI.sendDatasetSubmittedMessage(TO, template);
    } catch (Exception e) {
      fail("Should not throw exception");
    }
  }

  @Test
  void testSendDaaRequestMessage() {
    try {
      sendGridAPI.sendDaaRequestMessage(TO, template, "1");
    } catch (Exception  e) {
      fail("Should not throw exception");
    }
  }

  @Test
  void testSendNewDAAUploadSOMessage() {
    try {
      sendGridAPI.sendNewDAAUploadSOMessage(TO, template, "Test DAC");
    } catch (Exception  e) {
      fail("Should not throw exception");
    }
  }

  @Test
  void testSendNewDAAUploadResearcherMessage() {
    try {
      sendGridAPI.sendNewDAAUploadResearcherMessage(TO, template, "Test DAC");
    } catch (Exception  e) {
      fail("Should not throw exception");
    }
  }

}
