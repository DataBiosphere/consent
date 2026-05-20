package org.broadinstitute.consent.http.mail.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.HashMap;
import java.util.Map;
import org.broadinstitute.consent.http.enumeration.EmailType;
import org.broadinstitute.consent.http.models.User;
import org.junit.jupiter.api.Test;

class MailMessageTest {

  @Test
  void testGetTemplateName_UsesEmailTypeTemplateName() {
    var message =
        new TestMailMessage(
            new User(),
            EmailType.NEW_DAR,
            Map.of("messageKey", "messageValue"),
            "subject",
            "entity-reference-id");

    assertEquals(EmailType.NEW_DAR.templateName, message.getTemplateName());
  }

  @Test
  void testCreateModel_AddsServerUrlWhenMissing() {
    var message =
        new TestMailMessage(
            new User(),
            EmailType.NEW_DAR,
            Map.of("messageKey", "messageValue"),
            "subject",
            "entity-reference-id");

    Map<String, Object> model = message.createModel("http://testServerUrl");

    assertEquals("messageValue", model.get("messageKey"));
    assertEquals("http://testServerUrl", model.get("serverUrl"));
  }

  @Test
  void testCreateModel_PreservesSubclassServerUrlOverride() {
    var message =
        new TestMailMessage(
            new User(),
            EmailType.NEW_DAR,
            Map.of("messageKey", "messageValue", "serverUrl", "http://overrideUrl"),
            "subject",
            "entity-reference-id");

    Map<String, Object> model = message.createModel("http://testServerUrl");

    assertEquals("messageValue", model.get("messageKey"));
    assertEquals("http://overrideUrl", model.get("serverUrl"));
  }

  @Test
  void testCreateModel_ReplacesNullSubclassServerUrl() {
    Map<String, Object> baseModel = new HashMap<>();
    baseModel.put("messageKey", "messageValue");
    baseModel.put("serverUrl", null);
    var message =
        new TestMailMessage(
            new User(), baseModel, EmailType.NEW_DAR, "subject", "entity-reference-id");

    Map<String, Object> model = message.createModel("http://testServerUrl");

    assertEquals("messageValue", model.get("messageKey"));
    assertEquals("http://testServerUrl", model.get("serverUrl"));
  }

  @Test
  void testGetVoteId_DefaultsToNull() {
    var message =
        new TestMailMessage(
            new User(),
            EmailType.NEW_DAR,
            Map.of("messageKey", "messageValue"),
            "subject",
            "entity-reference-id");

    assertNull(message.getVoteId());
  }

  private static final class TestMailMessage extends MailMessage {

    private final Map<String, Object> model;
    private final String subject;
    private final String entityReferenceId;

    private TestMailMessage(
        User toUser,
        EmailType emailType,
        Map<String, Object> model,
        String subject,
        String entityReferenceId) {
      this(toUser, model, emailType, subject, entityReferenceId);
    }

    private TestMailMessage(
        User toUser,
        Map<String, Object> model,
        EmailType emailType,
        String subject,
        String entityReferenceId) {
      super(toUser, emailType);
      this.model = model;
      this.subject = subject;
      this.entityReferenceId = entityReferenceId;
    }

    @Override
    public String createSubject() {
      return subject;
    }

    @Override
    public Map<String, Object> createModel() {
      return model;
    }

    @Override
    public String getEntityReferenceId() {
      return entityReferenceId;
    }
  }
}
