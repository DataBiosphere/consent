package org.broadinstitute.consent.http.mail.message;

import org.broadinstitute.consent.http.enumeration.EmailType;
import org.broadinstitute.consent.http.models.User;

public class DatasetDeniedMessage extends MailMessage {

  private static final String DATASET_DENIED = "Dataset denied for DUOS";

  private final String dacName;
  private final String datasetName;
  private final String dacEmail;

  public DatasetDeniedMessage(User toUser, String dacName, String datasetName, String dacEmail) {
    super(toUser, EmailType.DATASET_DENIED);
    this.dacName = dacName;
    this.datasetName = datasetName;
    this.dacEmail = dacEmail;
  }

  @Override
  public String createSubject() {
    return DATASET_DENIED;
  }

  record Model(String dataSubmitterName, String datasetName, String dacName, String dacEmail) {}

  @Override
  public Object createModel(String serverUrl) {
    return new Model(toUser.getDisplayName(), datasetName, dacName, dacEmail);
  }

  @Override
  public String getEntityReferenceId() {
    return datasetName;
  }
}
