package org.broadinstitute.consent.http.mail.message;

import java.util.Map;
import org.broadinstitute.consent.http.enumeration.EmailType;
import org.broadinstitute.consent.http.models.User;

public class ResearcherCloseoutCompletedMessage extends MailMessage {

  private final String darCode;
  private final String referenceId;

  public ResearcherCloseoutCompletedMessage(User toUser, String darCode, String referenceId) {
    super(toUser, EmailType.RESEARCHER_CLOSEOUT_COMPLETED);
    this.darCode = darCode;
    this.referenceId = referenceId;
  }

  @Override
  public String createSubject() {
    return "Researcher Closeout completed for %s".formatted(darCode);
  }

  @Override
  public Object createModel(String serverUrl) {
    return Map.of("userName", toUser.getDisplayName(), "darCode", darCode, "serverUrl", serverUrl);
  }

  @Override
  public String getEntityReferenceId() {
    return referenceId;
  }
}
