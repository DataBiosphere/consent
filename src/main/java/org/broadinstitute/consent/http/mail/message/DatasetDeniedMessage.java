package org.broadinstitute.consent.http.mail.message;

import java.util.Map;
import org.broadinstitute.consent.http.enumeration.EmailType;
import org.broadinstitute.consent.http.models.User;

public class DatasetDeniedMessage extends MailMessage {

  private static final String DATASET_DENIED = "Dataset denied for DUOS";

  private final String dacName;
  private final String datasetIdentifier;
  private final String datasetName;
  private final String dacEmail;

  public DatasetDeniedMessage(
      User toUser, String dacName, String datasetIdentifier, String datasetName, String dacEmail) {
    super(toUser, EmailType.DATASET_DENIED);
    this.dacName = dacName;
    this.datasetIdentifier = datasetIdentifier;
    this.datasetName = datasetName;
    this.dacEmail = dacEmail;
  }

  @Override
  public String createSubject() {
    return DATASET_DENIED;
  }

  @Override
  public Map<String, Object> createModel() {
    return Map.of(
        "dataSubmitterName",
        toUser.getDisplayName(),
        "datasetName",
        datasetName,
        "dacName",
        dacName,
        "dacEmail",
        dacEmail);
  }

  /**
   * The dataset is referenced by its identifier rather than its name. The name is free text sourced
   * from the submitter, so it makes a poor key and can fill the entire
   * email_entity.entity_reference_id column.
   */
  @Override
  public String getEntityReferenceId() {
    return datasetIdentifier;
  }
}
