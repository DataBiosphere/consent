package org.broadinstitute.consent.http.mail.message;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.broadinstitute.consent.http.enumeration.EmailType;
import org.broadinstitute.consent.http.models.User;

public class NewProgressReportCaseMessage extends MailMessage {

  private static final String NEWCASE_DAR = "Log votes on Progress Report case id: %s.";
  private final String referenceId;
  private final List<Integer> datasetIds;

  public NewProgressReportCaseMessage(User toUser, String referenceId, List<Integer> datasetIds) {
    super(toUser, EmailType.NEW_PROGRESS_REPORT_CASE);
    this.referenceId = referenceId;
    this.datasetIds = datasetIds;
  }

  @Override
  public String createSubject() {
    return String.format(NEWCASE_DAR, referenceId);
  }

  @Override
  public Map<String, Object> createModel() {
    return Map.of("userName", toUser.getDisplayName(), "entityName", referenceId);
  }

  @Override
  public Optional<List<Integer>> getDatasetIds() {
    return Optional.ofNullable(datasetIds);
  }

  @Override
  public String getEntityReferenceId() {
    return referenceId;
  }
}
