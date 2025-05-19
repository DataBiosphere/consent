package org.broadinstitute.consent.http.mail.message;

import java.util.List;
import java.util.Map;
import org.broadinstitute.consent.http.enumeration.EmailType;
import org.broadinstitute.consent.http.models.User;

public class NewProgressReportRequestMessage extends MailMessage {

  private static final String NEW_PROGRESS_REPORT_REQUEST = "Create an election for Progress Report id: %s.";

  private final String darCode;
  private final Map<String, List<String>> sendList;
  private final String researcherName;
  private final String referenceId;


  public NewProgressReportRequestMessage(User toUser, String darCode, String referenceId, Map<String, List<String>> sendList,
      String researcherName) {
    super(toUser, EmailType.NEW_PROGRESS_REPORT_REQUEST);
    this.darCode = darCode;
    this.referenceId = referenceId;
    this.sendList = sendList;
    this.researcherName = researcherName;
  }

  @Override
  public String createSubject() {
    // nosemgrep
    return String.format(NEW_PROGRESS_REPORT_REQUEST, darCode);
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
    //
    return referenceId;
  }
}
