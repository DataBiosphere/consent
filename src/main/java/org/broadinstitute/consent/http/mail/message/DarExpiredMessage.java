package org.broadinstitute.consent.http.mail.message;

import java.util.Map;
import org.broadinstitute.consent.http.enumeration.EmailType;
import org.broadinstitute.consent.http.models.User;

public class DarExpiredMessage extends MailMessage {

  private final String darCode;

  public DarExpiredMessage(User toUser, String darCode) {
    super(toUser, EmailType.DAR_EXPIRED);
    this.darCode = darCode;
  }

  @Override
  public String createSubject() {
    return "Data Access Request Expired in DUOS";
  }

  @Override
  public Object createModel(String serverUrl) {
    return Map.of("researcherName", toUser.getDisplayName(),
        "darCode", darCode);
  }

  @Override
  public String getEntityReferenceId() {
    return darCode;
  }
}
