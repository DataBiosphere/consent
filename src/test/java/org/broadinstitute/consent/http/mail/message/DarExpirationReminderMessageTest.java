package org.broadinstitute.consent.http.mail.message;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import java.util.UUID;
import org.broadinstitute.consent.http.models.User;
import org.junit.jupiter.api.Test;

class DarExpirationReminderMessageTest extends AbstractMailMessageTest {

  @Test
  void testCreateModel_AddsRequiredFields() {
    User requestUser = new User();
    requestUser.setDisplayName("Test User");

    var message =
        new DarExpirationReminderMessage(requestUser, "DAR-123", UUID.randomUUID().toString());

    assertRequiredModelFields(message, Map.of("userName", "Test User", "darCode", "DAR-123"));
  }

  @Test
  void testGetDarExpiredTemplate() throws Exception {
    String userName = randomAlphabetic(10);
    String darCode = randomAlphabetic(10);
    String darReferenceId = UUID.randomUUID().toString();
    User requestUser = new User();
    requestUser.setDisplayName(userName);

    var message = new DarExpirationReminderMessage(requestUser, darCode, darReferenceId);
    assertEquals(darReferenceId, message.getEntityReferenceId());

    var rendered = renderTemplate(message, "localhost:8080");

    assertEquals(
        "Broad Data Use Oversight System - Your DAR is about to expire",
        rendered.document().title());
    assertEquals(
        "Hello %s,".formatted(userName), getElementTextById(rendered.document(), "userName"));
    assertEquals(
        "Your Data Access Request %s is expiring in 30 days. Please complete a progress report to preserve your access to this data."
            .formatted(darCode),
        getElementTextById(rendered.document(), "expirationWarning"));
    assertEquals(
        "Login to DUOS to submit a progress report.",
        getElementTextById(rendered.document(), "loginLink"));
  }
}
