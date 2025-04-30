package org.broadinstitute.consent.http.mail.message;

import java.util.Map;
import org.broadinstitute.consent.http.enumeration.EmailType;
import org.broadinstitute.consent.http.models.User;

public class DarExpirationReminderMessage extends MailMessage {

  private final String darCode;

  public DarExpirationReminderMessage(User toUser, String darCode) {
    super(toUser, EmailType.DAR_EXPIRATION_REMINDER);
    this.darCode = darCode;
  }

  @Override
  public String createSubject() {
    return "Data Access Request Expiration Reminder for %s".formatted(darCode);
  }

  @Override
  public Object createModel(String serverUrl) {
    return Map.of("userName", toUser.getDisplayName(),
        "darCode", darCode,
        "serverUrl", serverUrl);
  }

  @Override
  public String getEntityReferenceId() {
    return darCode;
  }
}
