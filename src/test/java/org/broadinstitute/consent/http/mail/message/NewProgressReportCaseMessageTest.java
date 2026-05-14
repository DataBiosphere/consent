package org.broadinstitute.consent.http.mail.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.broadinstitute.consent.http.models.User;
import org.junit.jupiter.api.Test;

class NewProgressReportCaseMessageTest extends AbstractMailMessageTest {

  @Test
  void testMessageSubject() {
    var message2 = new NewProgressReportCaseMessage(new User(), "DAR-123");
    assertEquals("Log votes on Progress Report case id: DAR-123.", message2.createSubject());
  }

  @Test
  void testGetNewCaseTemplate() throws Exception {
    String userName = randomAlphabetic(10);
    String referenceId = randomAlphabetic(10);
    String serverUrl = randomAlphabetic(10);
    User toUser = new User();
    toUser.setDisplayName(userName);

    var message = new NewProgressReportCaseMessage(toUser, referenceId);
    assertEquals(referenceId, message.getEntityReferenceId());

    var rendered = renderTemplate(message, serverUrl);

    assertEquals(
        "Broad Data Use Oversight System - New Progress Report ready for your vote",
        rendered.document().title());
    assertEquals("Hello " + userName + ",", getElementTextById(rendered.document(), "userName"));
    assertTrue(
        rendered
            .content()
            .contains("Progress Report Review case id " + referenceId + ", has been created"));
    assertTrue(rendered.content().contains(serverUrl));
  }
}
