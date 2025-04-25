package org.broadinstitute.consent.http.mail.message;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.broadinstitute.consent.http.models.User;
import org.junit.jupiter.api.Test;

class NewDARRequestMessageTest {
  @Test
  void testMessageSubject() {
    var message = new NewDARRequestMessage(new User(), "DAR-123", Map.of(), "Researcher Name");
    assertEquals(
        "Create an election for Data Access Request id: DAR-123.", message.createSubject());
  }
}
