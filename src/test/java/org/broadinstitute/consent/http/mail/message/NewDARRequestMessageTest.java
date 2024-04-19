package org.broadinstitute.consent.http.mail.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.MockitoAnnotations.openMocks;

import com.sendgrid.helpers.mail.Mail;
import java.io.Writer;
import javax.mail.MessagingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NewDARRequestMessageTest {

  @Mock
  Writer template;

  @Test
  void testMessageSubject() throws MessagingException {
    Mail message = new NewDARRequestMessage().newDARRequestMessage("to@address.com",
        "from@address.com", template, "DAR-123", "Data Use Limitations");
    assertEquals("Create an election for Data Access Request id: DAR-123.",
        message.getSubject());
  }

}