package org.broadinstitute.consent.http.mail.message;

import org.broadinstitute.consent.http.enumeration.EmailType;
import org.broadinstitute.consent.http.models.User;

public abstract class MailMessage {

  public final User toUser;
  public final EmailType emailType;

  protected MailMessage(User toUser, EmailType emailType) {
    this.toUser = toUser;
    this.emailType = emailType;
  }

  public String getTemplateName() {
    return emailType.templateName;
  }

  public abstract String createSubject();

  public abstract Object createModel(String serverUrl);

  public abstract String getEntityReferenceId();

  public Integer getVoteId() {
    return null;
  }
}
