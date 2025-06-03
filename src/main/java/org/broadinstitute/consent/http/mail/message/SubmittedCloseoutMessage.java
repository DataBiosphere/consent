package org.broadinstitute.consent.http.mail.message;

import java.util.Map;
import org.broadinstitute.consent.http.enumeration.EmailType;
import org.broadinstitute.consent.http.models.User;

public class SubmittedCloseoutMessage extends MailMessage {

  private static final String SUBJECT = "DAR %s Closeout Available for Review";

  private final String darId;

  private final String referenceId;

  public SubmittedCloseoutMessage(User toUser, String darId, String referenceId) {
    super(toUser, EmailType.SUBMITTED_CLOSEOUT);
    this.darId = darId;
    this.referenceId = referenceId;
  }

  @Override
  public String createSubject() {
    return String.format(SUBJECT, darId);
  }

  @Override
  public Object createModel(String linkUrl) {
    return Map.of("displayName", toUser.getDisplayName(),
        "darId", darId,
        "linkUrl", linkUrl);
  }

  @Override
  public String getEntityReferenceId() {
    return referenceId;
  }

}
