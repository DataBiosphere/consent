package org.broadinstitute.consent.http.service.mail;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import java.io.Writer;
import org.broadinstitute.consent.http.configurations.MailConfiguration;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.mail.SendGridAPI;
import org.broadinstitute.consent.http.models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SendGridAPITest {

  private static final String FROM = "from@broadinstitute.org";
  private static final String TO = "to@broadinstitute.org";
  private static final String ID = "DUL-123";
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

//  @Test
//  void testNewCaseMessage() throws Exception {
//    var response = sendGridAPI.sendNewCaseMessage(TO, ID, TYPE, template);
//    assertTrue(response.isPresent());
//    ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
//    verify(sendGrid).makeCall(requestCaptor.capture());
//    JSONAssert.assertEquals("""
//                       {"from": {"email": "%s"},"personalizations":[{"to":[{"email":"%s"}]}],
//                       "content":[{"type":"text/html","value":"%s"}]}""".formatted(FROM, TO, TEMPLATE),
//        requestCaptor.getValue().getBody(), false);
//  }
}
