package org.broadinstitute.consent.http.mail.message;

import java.util.Map;
import org.broadinstitute.consent.http.enumeration.EmailType;
import org.broadinstitute.consent.http.models.User;

public class NewCaseMessage extends MailMessage {

  private static final String NEW_CASE_DAR = "Log votes on Data Access Request case id: %s.";
  private final String referenceId;

  public NewCaseMessage(User toUser, String referenceId) {
    super(toUser, EmailType.NEW_CASE);
    this.referenceId = referenceId;
  }

  @Override
  public String createSubject() {
    return String.format(NEW_CASE_DAR, referenceId);
  }

  @Override
  public Map<String, Object> createModel() {
    return Map.of(
        "userName",
        toUser.getDisplayName(),
        "electionType",
        "Data Access Request",
        "entityName",
        referenceId);
  }

  @Override
  public String getEntityReferenceId() {
    return referenceId;
  }
}
