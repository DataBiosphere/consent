package org.broadinstitute.consent.http.mail.message;

import java.util.Map;
import org.broadinstitute.consent.http.enumeration.EmailType;
import org.broadinstitute.consent.http.models.User;

public class DatasetApprovedMessage extends MailMessage {

  private static final String DATASET_APPROVED = "Dataset approved for DUOS";
  private final String dacName;
  private final String datasetIdentifier;
  private final String datasetName;

  public DatasetApprovedMessage(
      User toUser, String dacName, String datasetIdentifier, String datasetName) {
    super(toUser, EmailType.DATASET_APPROVED);
    this.dacName = dacName;
    this.datasetIdentifier = datasetIdentifier;
    this.datasetName = datasetName;
  }

  @Override
  public String createSubject() {
    return DATASET_APPROVED;
  }

  @Override
  public Object createModel(String serverUrl) {
    return Map.of(
        "serverUrl",
        serverUrl,
        "dataSubmitterName",
        toUser.getDisplayName(),
        "datasetIdentifier",
        datasetIdentifier,
        "datasetName",
        datasetName,
        "dacName",
        dacName);
  }

  @Override
  public String getEntityReferenceId() {
    return datasetIdentifier;
  }
}
