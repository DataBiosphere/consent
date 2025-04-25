package org.broadinstitute.consent.http.mail.message;

import java.util.List;
import java.util.Map;
import org.broadinstitute.consent.http.enumeration.EmailType;
import org.broadinstitute.consent.http.models.User;

public class NewDARRequestMessage extends MailMessage {

  private static final String NEW_DAR_REQUEST = "Create an election for Data Access Request id: %s.";

  private final String darCode;
  private final Map<String, List<String>> sendList;
  private final String researcherName;

  public NewDARRequestMessage(User toUser, String darCode, Map<String, List<String>> sendList,
      String researcherName) {
    super(toUser, EmailType.NEW_DAR);
    this.darCode = darCode;
    this.sendList = sendList;
    this.researcherName = researcherName;
  }

  @Override
  public String createSubject() {
    // nosemgrep
    return String.format(NEW_DAR_REQUEST, darCode);
  }

  @Override
  public Object createModel(String serverUrl) {
    return Map.of(
        "serverUrl", serverUrl,
        "userName", toUser.getDisplayName(),
        "dacDatasetGroups", sendList,
        "researcherUserName", researcherName,
        "darID", darCode);
  }

  @Override
  public String getEntityReferenceId() {
    return darCode;
  }
}
