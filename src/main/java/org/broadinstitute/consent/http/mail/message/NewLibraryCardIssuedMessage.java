package org.broadinstitute.consent.http.mail.message;

import java.util.Map;
import org.broadinstitute.consent.http.enumeration.EmailType;
import org.broadinstitute.consent.http.models.User;

public class NewLibraryCardIssuedMessage extends MailMessage{
  private static final String NEW_LIBRARY_CARD_ISSUED =
      "Library Card Issued in DUOS.";

  public NewLibraryCardIssuedMessage(User toUser) {
    super(toUser, EmailType.NEW_LIBRARY_CARD_ISSUED);
  }

  @Override
  public String createSubject() {
    return NEW_LIBRARY_CARD_ISSUED;
  }

  @Override
  public Object createModel(String serverUrl) {
    return Map.of(
        "linkUrl", serverUrl + "datalibrary",
        "displayName", toUser.getDisplayName()
    );
  }

  @Override
  public String getEntityReferenceId() {
    return toUser.getEmail();
  }
}
