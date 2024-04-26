package org.broadinstitute.consent.http.mail.message;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.sendgrid.helpers.mail.Mail;
import java.io.Writer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DisabledDatasetMessageTest {

  @Mock
  Writer template;

  @Test
  void testMessageSubject() {
    Mail message = new DisabledDatasetMessage().disabledDatasetMessage("to@address.com",
        "from@address.com", template, "DAR-123", "SomeType");
    assertEquals(
        "Datasets not available for Data Access Request Application id: DAR-123.",
        message.getSubject());
  }

}
