package org.broadinstitute.consent.http.service.mail;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import java.io.Writer;
import org.broadinstitute.consent.http.configurations.MailConfiguration;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.mail.SendGridAPI;
import org.broadinstitute.consent.http.models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.skyscreamer.jsonassert.JSONAssert;

@ExtendWith(MockitoExtension.class)
class SendGridAPITest {

  private static final String FROM = "from@broadinstitute.org";
  private static final String TO = "to@broadinstitute.org";
  private static final String ID = "DUL-123";
  private static final String TYPE = "Data Use Limitations";
  private static final String TEMPLATE = "template";

  private SendGrid sendGrid;
  private SendGridAPI sendGridAPI;

  @Mock
  private Writer template;

  @Mock
  private UserDAO userDAO;

  @BeforeEach
  void setUp() throws Exception {
    MailConfiguration config = new MailConfiguration();
    config.setGoogleAccount(FROM);
    config.setActivateEmailNotifications(true);
    try (var mockedSendGrid = mockConstruction(SendGrid.class)) {
      sendGridAPI = new SendGridAPI(config, userDAO);
      sendGrid = mockedSendGrid.constructed().get(0);
      when(userDAO.findUserByEmail(TO)).thenReturn(new User());
      when(sendGrid.makeCall(any())).thenReturn(new Response());
      when(template.toString()).thenReturn(TEMPLATE);
    }
  }

  @Test
  void testNewCaseMessage() throws Exception {
    var response = sendGridAPI.sendNewCaseMessage(TO, ID, TYPE, template);
    assertTrue(response.isPresent());
    ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
    verify(sendGrid).makeCall(requestCaptor.capture());
    JSONAssert.assertEquals("""
                       {"from": {"email": "%s"},"personalizations":[{"to":[{"email":"%s"}]}],
                       "content":[{"type":"text/html","value":"%s"}]}""".formatted(FROM, TO, TEMPLATE),
        requestCaptor.getValue().getBody(), false);
  }

  @Test
  void testReminderMessage() {
    var response = sendGridAPI.sendReminderMessage(TO, ID, TYPE, template);
    assertTrue(response.isPresent());
  }

  @Test
  void testDisabledDatasetMessage() {
    var response = sendGridAPI.sendDisabledDatasetMessage(TO, ID, TYPE, template);
    assertTrue(response.isPresent());
  }

  @Test
  void testNewDARRequests() {
    var response = sendGridAPI.sendNewDARRequests(TO, ID, TYPE, template);
    assertTrue(response.isPresent());
  }

  @Test
  void testNewResearcherApprovedMessage() {
    var response = sendGridAPI.sendNewResearcherApprovedMessage(TO, template, "Test");
    assertTrue(response.isPresent());
  }

  @Test
  void testSendDataCustodianApprovalMessage() {
    var response = sendGridAPI.sendDataCustodianApprovalMessage(TO, "Test", template);
    assertTrue(response.isPresent());
  }

  @Test
  void testSendDatasetSubmittedMessage() throws Exception {
    var response = sendGridAPI.sendDatasetSubmittedMessage(TO, template);
    assertTrue(response.isPresent());
  }

  @Test
  void testSendDaaRequestMessage() throws Exception {
    var response = sendGridAPI.sendDaaRequestMessage(TO, template, "1");
    assertTrue(response.isPresent());
  }

  @Test
  void testSendNewDAAUploadSOMessage() throws Exception {
    var response = sendGridAPI.sendNewDAAUploadSOMessage(TO, template, "Test DAC");
    assertTrue(response.isPresent());
  }

  @Test
  void testSendNewDAAUploadResearcherMessage() throws Exception {
    var response = sendGridAPI.sendNewDAAUploadResearcherMessage(TO, template, "Test DAC");
    assertTrue(response.isPresent());
  }
}
