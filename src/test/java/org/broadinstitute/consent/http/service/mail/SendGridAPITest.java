package org.broadinstitute.consent.http.service.mail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
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

@ExtendWith(MockitoExtension.class)
class SendGridAPITest {

  private static final String FROM = "from@broadinstitute.org";
  private static final String TO = "to@broadinstitute.org";
  private static final Response RESPONSE = new Response();

  private SendGrid sendGrid;
  private SendGridAPI sendGridAPI;

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
    }
    when(userDAO.findUserByEmail(TO)).thenReturn(new User());
    when(sendGrid.makeCall(any())).thenReturn(RESPONSE);
  }

  @Test
  void sendMessage() throws Exception {
    String messageBody = "This is a test message";
    Mail mail = new Mail() {
      @Override
      public String build()  {
        return messageBody;
      }
    };
    assertEquals(RESPONSE, sendGridAPI.sendMessage(mail, TO));
    ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
    verify(sendGrid).makeCall(requestCaptor.capture());
    assertEquals(messageBody, requestCaptor.getValue().getBody());
  }
}
