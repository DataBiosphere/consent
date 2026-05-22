package org.broadinstitute.consent.http.mail.message;

import java.util.Map;
import org.broadinstitute.consent.http.enumeration.EmailType;
import org.broadinstitute.consent.http.models.User;

public class DarExpiredMessage extends MailMessage {

  private final String darCode;
  private final String referenceId;

  public DarExpiredMessage(User toUser, String darCode, String referenceId) {
    super(toUser, EmailType.DAR_EXPIRED);
    this.darCode = darCode;
    this.referenceId = referenceId;
  }

  @Override
  public String createSubject() {
    return "Data Access Request Expired in DUOS";
  }

  @Override
  public Map<String, Object> createModel() {
    return Map.of("researcherName", toUser.getDisplayName(), "darCode", darCode);
  }

  @Override
  public String getEntityReferenceId() {
    return referenceId;
  }
}
