package org.broadinstitute.consent.http.mail.message;

import java.util.Map;
import org.broadinstitute.consent.http.enumeration.EmailType;
import org.broadinstitute.consent.http.models.User;

public class NewDAAUploadSOMessage extends MailMessage {
  private static final String NEW_DAA_UPLOAD_SO = "New DAA uploaded and sent to SO for DAC in DUOS";

  private final String dacName;
  private final String previousDaaName;
  private final String newDaaName;

  public NewDAAUploadSOMessage(
      User toUser, String dacName, String previousDaaName, String newDaaName) {
    super(toUser, EmailType.NEW_DAA_UPLOAD_SO);
    this.dacName = dacName;
    this.previousDaaName = previousDaaName;
    this.newDaaName = newDaaName;
  }

  @Override
  public String createSubject() {
    return NEW_DAA_UPLOAD_SO;
  }

  @Override
  public Object createModel(Map<String, Object> model) {
    return mergeModel(
        model,
        Map.of(
            "dacName",
            dacName,
            "signingOfficialUserName",
            toUser.getDisplayName(),
            "previousDaaName",
            previousDaaName,
            "newDaaName",
            newDaaName));
  }

  @Override
  public String getEntityReferenceId() {
    return dacName;
  }
}
