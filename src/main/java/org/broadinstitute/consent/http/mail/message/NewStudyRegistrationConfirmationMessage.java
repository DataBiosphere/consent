package org.broadinstitute.consent.http.mail.message;

import java.util.Map;
import org.broadinstitute.consent.http.enumeration.EmailType;
import org.broadinstitute.consent.http.models.User;

public class NewStudyRegistrationConfirmationMessage extends MailMessage {

  private static final String NEW_STUDY_REGISTRATION_CONFIRMATION =
      "Confirmation of Study Registration Request Submission";

  private final String studyName;
  private final Integer studyId;
  private final Map<String, Object> studyAssets;

  public NewStudyRegistrationConfirmationMessage(
      User dataSubmitter, String studyName, Integer studyId, Map<String, Object> studyAssets) {
    super(dataSubmitter, EmailType.NEW_STUDY_REGISTRATION_CONFIRMATION);
    this.studyName = studyName;
    this.studyId = studyId;
    this.studyAssets = studyAssets;
  }

  @Override
  public String createSubject() {
    return NEW_STUDY_REGISTRATION_CONFIRMATION;
  }

  @Override
  public Object createModel(String serverUrl) {
    return Map.of(
        "studySubmitterName",
        toUser.getDisplayName(),
        "studyName",
        studyName,
        "studyId",
        studyId,
        "studyAssets",
        studyAssets);
  }

  @Override
  public String getEntityReferenceId() {
    return studyName;
  }
}
