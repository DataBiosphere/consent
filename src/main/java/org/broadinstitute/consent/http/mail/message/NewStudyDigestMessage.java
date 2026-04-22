package org.broadinstitute.consent.http.mail.message;

import java.util.List;
import java.util.Map;
import org.broadinstitute.consent.http.enumeration.EmailType;
import org.broadinstitute.consent.http.models.StudyDatasetCountRecord;
import org.broadinstitute.consent.http.models.User;

public class NewStudyDigestMessage extends MailMessage {

  private final List<StudyDatasetCountRecord> newStudiesList;
  private final String referenceId;

  public NewStudyDigestMessage(
      User toUser, List<StudyDatasetCountRecord> newStudies, String referenceId) {
    super(toUser, EmailType.NEW_STUDY_DIGEST);
    this.newStudiesList = newStudies;
    this.referenceId = referenceId;
  }

  @Override
  public String createSubject() {
    return "New data in DUOS today!";
  }

  @Override
  public Object createModel(String serverUrl) {
    return Map.of(
        "userName", toUser.getDisplayName(), "newStudies", newStudiesList, "serverUrl", serverUrl);
  }

  @Override
  public String getEntityReferenceId() {
    return this.referenceId;
  }
}
