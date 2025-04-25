package org.broadinstitute.consent.http.mail.message;

import org.broadinstitute.consent.http.enumeration.EmailType;
import org.broadinstitute.consent.http.models.User;

public class DatasetSubmittedMessage extends MailMessage {

  private static final String DATASET_SUBMITTED = "Dataset submitted to DUOS";

  private final String dataSubmitterName;
  private final String datasetName;
  private final String dacName;

  public DatasetSubmittedMessage(User dacChair, String dataSubmitterName, String datasetName,
      String dacName) {
    super(dacChair, EmailType.NEW_DATASET);
    this.dataSubmitterName = dataSubmitterName;
    this.datasetName = datasetName;
    this.dacName = dacName;
  }

  @Override
  public String createSubject() {
    return DATASET_SUBMITTED;
  }

  record Model(String dacChairName, String dataSubmitterName, String datasetName, String dacName) {}

  @Override
  public Object createModel(String serverUrl) {
    return new Model(
        toUser.getDisplayName(),
        dataSubmitterName,
        datasetName,
        dacName);
  }

  @Override
  public String getEntityReferenceId() {
    return datasetName;
  }
}
