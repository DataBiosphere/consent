package org.broadinstitute.consent.http.mail.message;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.broadinstitute.consent.http.enumeration.EmailType;
import org.broadinstitute.consent.http.models.User;

public class NewCaseMessage extends MailMessage {

  private static final String NEW_CASE_DAR = "Log votes on Data Access Request case id: %s.";
  private final String referenceId;
  private final List<Integer> datasetIds;

  public NewCaseMessage(User toUser, String referenceId, List<Integer> datasetIds) {
    super(toUser, EmailType.NEW_CASE);
    this.referenceId = referenceId;
    this.datasetIds = datasetIds;
  }

  @Override
  public String createSubject() {
    return String.format(NEW_CASE_DAR, referenceId);
  }

  @Override
  public Map<String, Object> createModel() {
    return Map.of(
        "userName",
        toUser.getDisplayName(),
        "electionType",
        "Data Access Request",
        "entityName",
        referenceId);
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
