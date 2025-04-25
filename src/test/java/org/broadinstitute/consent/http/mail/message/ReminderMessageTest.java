package org.broadinstitute.consent.http.mail.message;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.Writer;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.Vote;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReminderMessageTest {

  @Mock
  Writer template;

  @Test
  void testMessageSubject() {
    var message = new ReminderMessage(new User(), new Vote(), "DUL-123", "Data Use Limitations", "");
    assertEquals("Urgent: Log vote on Data Use Limitations case id: DUL-123.", message.createSubject());
    var message2 = new ReminderMessage(new User(), new Vote(), "DAR-123", "Data Access Request", "");
    assertEquals("Urgent: Log votes on Data Access Request case id: DAR-123.", message2.createSubject());
    var message3 = new ReminderMessage(new User(), new Vote(), "RP-123", "Research Purpose", "");
    assertEquals("Urgent: Log votes on Research Purpose Review case id: RP-123.", message3.createSubject());
  }
}
