package org.broadinstitute.consent.http.mail.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.UUID;
import org.broadinstitute.consent.http.models.User;
import org.junit.jupiter.api.Test;

class ResearcherCloseoutCompletedMessageTest extends AbstractMailMessageTest {

  @Test
  void testCreateModel_AddsRequiredFields() {
    User requestUser = new User();
    requestUser.setDisplayName("Test User");
    var message =
        new ResearcherCloseoutCompletedMessage(
            requestUser, "DAR-123", UUID.randomUUID().toString());

    assertRequiredModelFields(message, Map.of("userName", "Test User", "darCode", "DAR-123"));
  }

  @Test
  void testGetResearcherCloseoutCompletedTemplate() throws Exception {
    String userName = randomAlphabetic(10);
    String darCode = randomAlphabetic(10);
    String referenceId = UUID.randomUUID().toString();
    User requestUser = new User();
    requestUser.setDisplayName(userName);

    var message = new ResearcherCloseoutCompletedMessage(requestUser, darCode, referenceId);
    assertEquals(referenceId, message.getEntityReferenceId());

    var rendered = renderTemplate(message, "localhost:8080");

    assertEquals(
        "Broad Data Use Oversight System - Researcher - Closeout Complete",
        rendered.document().title());
    assertEquals(
        "Dear %s,".formatted(userName), getElementTextById(rendered.document(), "userName"));
    assertEquals(
        "The closeout on Data Access Request (DAR) %s has been approved and your access to all datasets in this DAR will be revoked unless you have permission to use that data under another DAR."
            .formatted(darCode),
        getElementTextById(rendered.document(), "content"));
    assertTrue(
        getElementTextById(rendered.document(), "warning")
            .contains("you have agreed to destroy all copies"));
    assertFalse(rendered.content().contains("${"));
  }
}
