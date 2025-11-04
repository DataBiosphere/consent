package org.broadinstitute.consent.http.mail.message;

import java.util.Map;
import org.broadinstitute.consent.http.enumeration.EmailType;
import org.broadinstitute.consent.http.models.User;

public class NewCaseMessage extends MailMessage {

  private static final String NEWCASE_DUL = "Log vote on Data Use Limitations case id: %s.";
  private static final String NEWCASE_DAR = "Log votes on Data Access Request case id: %s.";
  private final String type;
  private final String referenceId;

  public NewCaseMessage(User toUser, String referenceId, String type) {
    super(toUser, EmailType.NEW_CASE);
    this.referenceId = referenceId;
    this.type = type;
  }

  @Override
  public String createSubject() {
    if (type.equals("Data Use Limitations")) {
      return String.format(NEWCASE_DUL, referenceId);
    } else {
      return String.format(NEWCASE_DAR, referenceId);
    }
  }

  @Override
  public Object createModel(String serverUrl) {
    return Map.of(
        "userName",
        toUser.getDisplayName(),
        "electionType",
        type,
        "entityName",
        referenceId,
        "serverUrl",
        serverUrl);
  }

  @Override
  public String getEntityReferenceId() {
    return referenceId;
  }
}
