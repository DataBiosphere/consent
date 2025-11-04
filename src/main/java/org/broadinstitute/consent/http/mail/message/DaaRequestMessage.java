package org.broadinstitute.consent.http.mail.message;

import java.util.Map;
import org.broadinstitute.consent.http.enumeration.EmailType;
import org.broadinstitute.consent.http.models.User;

public class DaaRequestMessage extends MailMessage {
  private static final String NEW_DAA_LIBRARY_CARD_REQUEST =
      "New DAA-Library Card Relationship Request in DUOS";
  private final User requestUser;
  private final String daaName;
  private final Integer daaId;

  public DaaRequestMessage(User signingOfficial, User requestUser, String daaName, Integer daaId) {
    super(signingOfficial, EmailType.NEW_DAA_REQUEST);
    this.requestUser = requestUser;
    this.daaName = daaName;
    this.daaId = daaId;
  }

  @Override
  public String createSubject() {
    return NEW_DAA_LIBRARY_CARD_REQUEST;
  }

  @Override
  public Object createModel(String serverUrl) {
    return Map.of(
        "serverUrl",
        serverUrl,
        "daaName",
        daaName,
        "userName",
        requestUser.getDisplayName(),
        "signingOfficialUserName",
        toUser.getDisplayName());
  }

  @Override
  public String getEntityReferenceId() {
    return daaId.toString();
  }
}
