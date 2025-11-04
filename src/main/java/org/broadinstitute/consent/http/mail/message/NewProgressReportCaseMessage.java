package org.broadinstitute.consent.http.mail.message;

import java.util.Map;
import org.broadinstitute.consent.http.enumeration.EmailType;
import org.broadinstitute.consent.http.models.User;

public class NewProgressReportCaseMessage extends MailMessage {

  private static final String NEWCASE_DAR = "Log votes on Progress Report case id: %s.";
  private final String referenceId;

  public NewProgressReportCaseMessage(User toUser, String referenceId) {
    super(toUser, EmailType.NEW_PROGRESS_REPORT_CASE);
    this.referenceId = referenceId;
  }

  @Override
  public String createSubject() {
    return String.format(NEWCASE_DAR, referenceId);
  }

  @Override
  public Object createModel(String serverUrl) {
    return Map.of(
        "userName", toUser.getDisplayName(), "entityName", referenceId, "serverUrl", serverUrl);
  }

  @Override
  public String getEntityReferenceId() {
    return referenceId;
  }
}
