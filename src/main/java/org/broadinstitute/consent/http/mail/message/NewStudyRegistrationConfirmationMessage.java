package org.broadinstitute.consent.http.mail.message;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.broadinstitute.consent.http.enumeration.EmailType;
import org.broadinstitute.consent.http.models.User;

public class NewStudyRegistrationConfirmationMessage extends MailMessage {

  private static final String NEW_STUDY_REGISTRATION_CONFIRMATION =
      "Confirmation of Study Registration Request Submission";

  private final String studyName;
  private final Integer studyId;
  private final UUID studyUuid;
  private final Map<String, Object> studyAssets;

  public NewStudyRegistrationConfirmationMessage(
      User dataSubmitter,
      String studyName,
      Integer studyId,
      UUID studyUuid,
      Map<String, Object> studyAssets) {
    super(dataSubmitter, EmailType.NEW_STUDY_REGISTRATION_CONFIRMATION);
    this.studyName = studyName;
    this.studyId = studyId;
    this.studyUuid = studyUuid;
    this.studyAssets = studyAssets;
  }

  @Override
  public String createSubject() {
    return NEW_STUDY_REGISTRATION_CONFIRMATION;
  }

  @Override
  public Map<String, Object> createModel() {
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
    return Objects.toString(studyUuid, null);
  }
}
