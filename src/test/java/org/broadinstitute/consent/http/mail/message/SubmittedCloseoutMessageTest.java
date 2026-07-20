package org.broadinstitute.consent.http.mail.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Objects;
import org.broadinstitute.consent.http.models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SubmittedCloseoutMessageTest extends AbstractMailMessageTest {

  private User toUser;

  @BeforeEach
  void setUp() {
    toUser = new User();
    toUser.setDisplayName("Test User");
  }

  @Test
  void testMessageSubject() {
    var message =
        new SubmittedCloseoutMessage(toUser, "DAR-123", "ref-456", "http://testServerUrl");
    assertEquals("DAR DAR-123 Closeout Available for Review", message.createSubject());
  }

  @Test
  void testCreateModel_AddsRequiredFields() {
    var message =
        new SubmittedCloseoutMessage(toUser, "DAR-123", "ref-456", "http://testServerUrl");

    assertRequiredModelFields(
        message,
        Map.of("displayName", "Test User", "darId", "DAR-123", "linkUrl", "http://testServerUrl"));
  }

  @Test
  void testGetSubmittedCloseoutTemplate() throws Exception {
    String darId = "DAR-123";
    String referenceId = "ref-456";
    String linkUrl = "http://testServerUrl";

    var message = new SubmittedCloseoutMessage(toUser, darId, referenceId, "http://testServerUrl");
    assertEquals(referenceId, message.getEntityReferenceId());

    var rendered = renderTemplate(message, linkUrl);

    assertTrue(rendered.content().contains(toUser.getDisplayName()));
    assertTrue(rendered.content().contains(darId));
    assertTrue(rendered.content().contains("closeout for your review and approval"));
    assertEquals(
        "Hello Test User,",
        Objects.requireNonNull(rendered.document().getElementById("userName")).text());
  }
}
