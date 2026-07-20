package org.broadinstitute.consent.http.mail.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.broadinstitute.consent.http.models.User;
import org.junit.jupiter.api.Test;

class NewCaseMessageTest extends AbstractMailMessageTest {

  @Test
  void testMessageSubject() {
    var message = new NewCaseMessage(new User(), "DAR-123");
    assertEquals("Log votes on Data Access Request case id: DAR-123.", message.createSubject());
  }

  @Test
  void testCreateModel_AddsRequiredFields() {
    User toUser = new User();
    toUser.setDisplayName("Test User");
    var message = new NewCaseMessage(toUser, "DAR-123");

    assertRequiredModelFields(
        message,
        Map.of(
            "userName",
            "Test User",
            "electionType",
            "Data Access Request",
            "entityName",
            "DAR-123"));
  }

  @Test
  void testGetNewCaseTemplate() throws Exception {
    String userName = randomAlphabetic(10);
    String referenceId = randomAlphabetic(10);
    String serverUrl = randomAlphabetic(10);
    User toUser = new User();
    toUser.setDisplayName(userName);

    var message = new NewCaseMessage(toUser, referenceId);
    assertEquals(referenceId, message.getEntityReferenceId());

    var rendered = renderTemplate(message, serverUrl);

    assertEquals(
        "Broad Data Use Oversight System - New DAR ready for your vote",
        rendered.document().title());
    assertEquals("Hello " + userName + ",", getElementTextById(rendered.document(), "userName"));
    assertTrue(
        rendered
            .content()
            .contains("Data Access Request Review case id " + referenceId + ", has been created"));
    assertTrue(rendered.content().contains(serverUrl));
  }
}
