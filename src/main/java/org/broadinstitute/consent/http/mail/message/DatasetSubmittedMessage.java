package org.broadinstitute.consent.http.mail.message;

import java.util.Map;
import org.broadinstitute.consent.http.enumeration.EmailType;
import org.broadinstitute.consent.http.models.User;

public class DatasetSubmittedMessage extends MailMessage {

  private static final String DATASET_SUBMITTED =
      "Decision Needed: Accept or Reject Dataset Management Request";

  private final String dataSubmitterName;
  private final String datasetName;
  private final String dacName;

  public DatasetSubmittedMessage(
      User dacChair, String dataSubmitterName, String datasetName, String dacName) {
    super(dacChair, EmailType.NEW_DATASET);
    this.dataSubmitterName = dataSubmitterName;
    this.datasetName = datasetName;
    this.dacName = dacName;
  }

  @Override
  public String createSubject() {
    return DATASET_SUBMITTED;
  }

  @Override
  public Map<String, Object> createModel() {
    return Map.of(
        "dacChairName",
        toUser.getDisplayName(),
        "dataSubmitterName",
        dataSubmitterName,
        "datasetName",
        datasetName,
        "dacName",
        dacName);
  }

  @Override
  public String getEntityReferenceId() {
    return datasetName;
  }
}
