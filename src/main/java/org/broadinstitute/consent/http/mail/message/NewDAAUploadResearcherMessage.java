package org.broadinstitute.consent.http.mail.message;

import java.util.Map;
import org.broadinstitute.consent.http.enumeration.EmailType;
import org.broadinstitute.consent.http.models.User;

public class NewDAAUploadResearcherMessage extends MailMessage {
  private static final String NEW_DAA_UPLOAD_RESEARCHER =
      "New DAA uploaded and sent to researcher for DAC in DUOS";

  private final String dacName;
  private final String previousDaaName;
  private final String newDaaName;

  public NewDAAUploadResearcherMessage(
      User toUser, String dacName, String previousDaaName, String newDaaName) {
    super(toUser, EmailType.NEW_DAA_UPLOAD_RESEARCHER);
    this.dacName = dacName;
    this.previousDaaName = previousDaaName;
    this.newDaaName = newDaaName;
  }

  @Override
  public String createSubject() {
    return NEW_DAA_UPLOAD_RESEARCHER;
  }

  @Override
  public Object createModel(String serverUrl) {
    return Map.of("serverUrl", serverUrl,
        "dacName", dacName,
        "researcherUserName", toUser.getDisplayName(),
        "previousDaaName", previousDaaName,
        "newDaaName", newDaaName);
  }

  @Override
  public String getEntityReferenceId() {
    return dacName;
  }
}
