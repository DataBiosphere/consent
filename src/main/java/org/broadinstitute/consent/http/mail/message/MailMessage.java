package org.broadinstitute.consent.http.mail.message;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
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

  public final Object createModel(String serverUrl) {
    return createModel(Map.of("serverUrl", serverUrl));
  }

  public abstract Object createModel(Map<String, Object> model);

  protected final Map<String, Object> mergeModel(
      Map<String, Object> model, Map<String, Object> additionalModel) {
    Map<String, Object> mergedModel = new HashMap<>();
    if (model != null) {
      mergedModel.putAll(model);
    }
    mergedModel.putAll(additionalModel);
    return mergedModel;
  }

  protected final String requireServerUrl(Map<String, Object> model) {
    return Objects.requireNonNull(model.get("serverUrl"), () -> "serverUrl is required").toString();
  }

  public abstract String getEntityReferenceId();

  public Integer getVoteId() {
    return null;
  }
}
