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
class NewCaseMessageTest {

  @Mock
  Writer template;

  @Test
  void testMessageSubject() throws MessagingException {
    Mail message = new NewCaseMessage().newCaseMessage("to@address.com", "from@address.com",
        template, "DUL-123", "Data Use Limitations");
    assertEquals("Log vote on Data Use Limitations case id: DUL-123.",
        message.getSubject());
    Mail message2 = new NewCaseMessage().newCaseMessage("to@address.com", "from@address.com",
        template, "DAR-123", "Data Access");
    assertEquals("Log votes on Data Access Request case id: DAR-123.",
        message2.getSubject());
  }

}
