package org.broadinstitute.consent.http.mail.message;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.broadinstitute.consent.http.models.User;
import org.junit.jupiter.api.Test;

class NewCaseMessageTest {

  @Test
  void testMessageSubject() {
    var message = new NewCaseMessage(new User(), "DUL-123", "Data Use Limitations");
    assertEquals("Log vote on Data Use Limitations case id: DUL-123.", message.createSubject());
    var message2 = new NewCaseMessage(new User(), "DAR-123", "Data Access");
    assertEquals("Log votes on Data Access Request case id: DAR-123.", message2.createSubject());
  }
}
