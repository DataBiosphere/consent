package org.broadinstitute.consent.http.mail.message;

import java.util.Map;
import org.broadinstitute.consent.http.enumeration.EmailType;
import org.broadinstitute.consent.http.models.User;

public class NewDARSigningOfficialRequestMessage extends MailMessage {

  private static final String SUBJECT = "A data access request requires your approval: %s.";

  private final String darCode;
  private final String researcherName;

  public NewDARSigningOfficialRequestMessage(User toUser, String darCode, String researcherName) {
    super(toUser, EmailType.NEW_DAR_SO_NEEDS_TO_APPROVE);
    this.darCode = darCode;
    this.researcherName = researcherName;
  }

  @Override
  public String createSubject() {
    // nosemgrep
    return String.format(SUBJECT, darCode);
  }

  @Override
  public Object createModel(String serverUrl) {
    return Map.of(
        "serverUrl", serverUrl,
        "userName", toUser.getDisplayName(),
        "researcherUserName", researcherName,
        "darID", darCode);
  }

  @Override
  public String getEntityReferenceId() {
    return darCode;
  }
}
