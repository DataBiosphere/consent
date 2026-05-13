package org.broadinstitute.consent.http.mail.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.broadinstitute.consent.http.models.User;
import org.junit.jupiter.api.Test;

class NewCaseMessageTest extends AbstractMailMessageTest {

  @Test
  void testMessageSubject() {
    var message = new NewCaseMessage(new User(), "DUL-123", "Data Use Limitations");
    assertEquals("Log vote on Data Use Limitations case id: DUL-123.", message.createSubject());
    var message2 = new NewCaseMessage(new User(), "DAR-123", "Data Access");
    assertEquals("Log votes on Data Access Request case id: DAR-123.", message2.createSubject());
  }

  @Test
  void testGetNewCaseTemplate() throws Exception {
    String userName = randomAlphabetic(10);
    String referenceId = randomAlphabetic(10);
    String serverUrl = randomAlphabetic(10);
    User toUser = new User();
    toUser.setDisplayName(userName);

    var message = new NewCaseMessage(toUser, referenceId, "Data Use Limitations");
    assertEquals(referenceId, message.getEntityReferenceId());

    var rendered = renderTemplate(message, serverUrl);

    assertEquals(
        "Broad Data Use Oversight System - New DAR ready for your vote",
        rendered.document().title());
    assertEquals("Hello " + userName + ",", getElementTextById(rendered.document(), "userName"));
    assertTrue(
        rendered
            .content()
            .contains("Data Use Limitations Review case id " + referenceId + ", has been created"));
    assertTrue(rendered.content().contains(serverUrl));
  }
}
