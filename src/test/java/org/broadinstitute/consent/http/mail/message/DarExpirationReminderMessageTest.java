package org.broadinstitute.consent.http.mail.message;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.sendgrid.helpers.mail.Mail;
import java.io.Writer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DarExpirationReminderMessageTest {

  @Mock
  Writer template;

  @Test
  void testMessageSubject() {
    Mail message = new DarExpirationReminderMessage().darExpirationReminderMessage("to@address.com",
        "from@address.com", template, "DAR-123", "DAR Expiration Reminder");
    assertEquals("Remind user of expiring DAR id: DAR-123",
        message.getSubject());
  }

}
