package org.broadinstitute.consent.http.mail.message;

import java.util.Map;
import org.broadinstitute.consent.http.enumeration.EmailType;
import org.broadinstitute.consent.http.models.User;

public class DarExpirationReminderMessage extends MailMessage {

  private final String darCode;
  private final String referenceId;

  public DarExpirationReminderMessage(User toUser, String darCode, String referenceId) {
    super(toUser, EmailType.DAR_EXPIRATION_REMINDER);
    this.darCode = darCode;
    this.referenceId = referenceId;
  }

  @Override
  public String createSubject() {
    return "Data Access Request Expiration Reminder for %s".formatted(darCode);
  }

  @Override
  public Object createModel(Map<String, Object> model) {
    return mergeModel(model, Map.of("userName", toUser.getDisplayName(), "darCode", darCode));
  }

  @Override
  public String getEntityReferenceId() {
    return referenceId;
  }
}
