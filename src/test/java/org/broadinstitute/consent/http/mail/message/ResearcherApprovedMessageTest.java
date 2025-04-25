package org.broadinstitute.consent.http.mail.message;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.sendgrid.helpers.mail.Mail;
import java.io.Writer;
import java.util.List;
import org.broadinstitute.consent.http.models.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

class ResearcherApprovedMessageTest {

  @Test
  void testMessageSubject() {
    var message = new ResearcherApprovedMessage(new User(), "DAR-123", List.of(), "");
    assertEquals("Your DUOS Data Access Request Results", message.createSubject());
  }
}
