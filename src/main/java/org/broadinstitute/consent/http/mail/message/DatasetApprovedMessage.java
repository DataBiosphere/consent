package org.broadinstitute.consent.http.mail.message;

import org.broadinstitute.consent.http.enumeration.EmailType;
import org.broadinstitute.consent.http.models.User;

public class DatasetApprovedMessage extends MailMessage {

  private static final String DATASET_APPROVED = "Dataset approved for DUOS";
  private final String dacName;
  private final String datasetName;

  public DatasetApprovedMessage(User toUser, String dacName, String datasetName) {
    super(toUser, EmailType.DATASET_APPROVED);
    this.dacName = dacName;
    this.datasetName = datasetName;
  }

  @Override
  public String createSubject() {
    return DATASET_APPROVED;
  }

  record Model(String dataSubmitterName, String datasetName, String dacName) {}

  @Override
  public Object createModel(String serverUrl) {
    return new Model(toUser.getDisplayName(), datasetName, dacName);
  }

  @Override
  public String getEntityReferenceId() {
    return datasetName;
  }
}
