package org.broadinstitute.consent.http.service.mail;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.google.api.client.http.HttpStatusCodes;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import java.io.IOException;
import java.util.Map;
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
  private static final int UNSUBSCRIBE_GROUP_ID = 12345;
  private static final Response RESPONSE = new Response();

  private SendGrid sendGrid;
  private SendGridAPI sendGridAPI;

  @Mock private UserDAO userDAO;

  @BeforeEach
  void setUp() throws Exception {
    MailConfiguration config = new MailConfiguration();
    config.setGoogleAccount(FROM);
    config.setActivateEmailNotifications(true);
    try (var mockedSendGrid = mockConstruction(SendGrid.class)) {
      sendGridAPI = new SendGridAPI(config, userDAO);
      sendGrid = mockedSendGrid.constructed().getFirst();
    }
    when(userDAO.findUserByEmail(TO)).thenReturn(new User());
    when(sendGrid.makeCall(any())).thenReturn(RESPONSE);
  }

  @Test
  void sendMessage() throws Exception {
    String messageBody = "This is a test message";
    Mail mail =
        new Mail() {
          @Override
          public String build() {
            return messageBody;
          }
        };
    assertEquals(RESPONSE, sendGridAPI.sendMessage(mail, TO));
    ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
    verify(sendGrid).makeCall(requestCaptor.capture());
    assertEquals(messageBody, requestCaptor.getValue().getBody());
  }

  @Test
  void sendMessageAttachesAsmWhenGroupConfigured() throws Exception {
    try (var mockedSendGrid = mockConstruction(SendGrid.class)) {
      MailConfiguration config = new MailConfiguration();
      config.setGoogleAccount(FROM);
      config.setActivateEmailNotifications(true);
      config.setSendGridUnsubscribeGroupId(UNSUBSCRIBE_GROUP_ID);
      SendGridAPI configuredSendGridApi = new SendGridAPI(config, userDAO);
      SendGrid configuredSendGrid = mockedSendGrid.constructed().getFirst();
      when(configuredSendGrid.makeCall(any())).thenReturn(RESPONSE);

      Mail mail =
          new Mail(new Email(FROM), "Subject", new Email(TO), new Content("text/html", "Body"));

      assertEquals(RESPONSE, configuredSendGridApi.sendMessage(mail, TO));

      verify(configuredSendGrid).makeCall(any());
      assertEquals(UNSUBSCRIBE_GROUP_ID, mail.getASM().getGroupId());
      assertArrayEquals(new int[] {UNSUBSCRIBE_GROUP_ID}, mail.getASM().getGroupsToDisplay());
    }
  }

  @Test
  void sendMessageDoesNotAttachAsmWhenGroupMissing() throws Exception {
    Mail mail =
        new Mail(new Email(FROM), "Subject", new Email(TO), new Content("text/html", "Body"));

    assertEquals(RESPONSE, sendGridAPI.sendMessage(mail, TO));

    verify(sendGrid).makeCall(any());
    assertNull(mail.getASM());
  }

  @Test
  void sendMessageDoesNotAttachAsmWhenGroupInvalid() throws Exception {
    try (var mockedSendGrid = mockConstruction(SendGrid.class)) {
      MailConfiguration config = new MailConfiguration();
      config.setGoogleAccount(FROM);
      config.setActivateEmailNotifications(true);
      // invalid group ID should be treated as missing/absent
      config.setSendGridUnsubscribeGroupId(0);
      SendGridAPI configuredSendGridApi = new SendGridAPI(config, userDAO);
      SendGrid configuredSendGrid = mockedSendGrid.constructed().getFirst();
      when(configuredSendGrid.makeCall(any())).thenReturn(RESPONSE);

      Mail mail =
          new Mail(new Email(FROM), "Subject", new Email(TO), new Content("text/html", "Body"));

      assertEquals(RESPONSE, configuredSendGridApi.sendMessage(mail, TO));

      verify(configuredSendGrid).makeCall(any());
      assertNull(mail.getASM());
    }
  }

  @Test
  void sendMessageUserMissing() {
    reset(userDAO);
    reset(sendGrid);
    assertNull(sendGridAPI.sendMessage(null, TO));
    verifyNoInteractions(sendGrid);
  }

  @Test
  void sendMessageUserDisabledEmails() {
    User user = new User();
    user.setEmailPreference(false);
    when(userDAO.findUserByEmail(TO)).thenReturn(user);
    reset(sendGrid);
    assertNull(sendGridAPI.sendMessage(new Mail(), TO));
    verifyNoInteractions(sendGrid);
  }

  @Test
  void sendMessageApiError() throws Exception {
    Response response = new Response(400, "", Map.of());
    when(sendGrid.makeCall(any())).thenReturn(response);
    assertEquals(response, sendGridAPI.sendMessage(new Mail(), TO));
  }

  @Test
  void sendMessageExceptionThrown() throws Exception {
    when(sendGrid.makeCall(any())).thenThrow(new IOException());
    var response = sendGridAPI.sendMessage(new Mail(), TO);
    assertEquals(HttpStatusCodes.STATUS_CODE_SERVER_ERROR, response.getStatusCode());
  }
}
