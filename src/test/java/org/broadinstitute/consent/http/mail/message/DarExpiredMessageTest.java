package org.broadinstitute.consent.http.mail.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.UUID;
import org.broadinstitute.consent.http.models.User;
import org.junit.jupiter.api.Test;

class DarExpiredMessageTest extends AbstractMailMessageTest {

  @Test
  void testCreateModel_AddsRequiredFields() {
    User requestUser = new User();
    requestUser.setDisplayName("Test User");

    var message = new DarExpiredMessage(requestUser, "DAR-123", UUID.randomUUID().toString());

    assertRequiredModelFields(message, Map.of("researcherName", "Test User", "darCode", "DAR-123"));
  }

  @Test
  void testGetDarExpiredTemplate() throws Exception {
    String userName = randomAlphabetic(10);
    String darCode = randomAlphabetic(10);
    String referenceId = UUID.randomUUID().toString();
    User requestUser = new User();
    requestUser.setDisplayName(userName);

    var message = new DarExpiredMessage(requestUser, darCode, referenceId);
    assertEquals(referenceId, message.getEntityReferenceId());

    var rendered = renderTemplate(message, "");

    assertEquals(
        "Broad Data Use Oversight System - Researcher - Data Access Request Expired",
        rendered.document().title());
    assertEquals(
        "Dear %s,".formatted(userName), getElementTextById(rendered.document(), "userName"));
    assertTrue(
        getElementTextById(rendered.document(), "content")
            .contains("Your Data Access Request %s has expired".formatted(darCode)));
  }
}
