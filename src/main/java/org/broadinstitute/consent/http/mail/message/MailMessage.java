package org.broadinstitute.consent.http.mail.message;

import java.util.HashMap;
import java.util.Map;
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

  public final Map<String, Object> createModel(String serverUrl) {
    Map<String, Object> model = new HashMap<>(createModel());
    model.putIfAbsent("serverUrl", serverUrl);
    return model;
  }

  public abstract Map<String, Object> createModel();

  public abstract String getEntityReferenceId();

  public Integer getVoteId() {
    return null;
  }
}
