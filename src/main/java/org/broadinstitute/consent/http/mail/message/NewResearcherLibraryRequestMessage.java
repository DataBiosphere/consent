package org.broadinstitute.consent.http.mail.message;

import java.util.Map;
import org.broadinstitute.consent.http.enumeration.EmailType;
import org.broadinstitute.consent.http.models.User;

public class NewResearcherLibraryRequestMessage extends MailMessage {

  private static final String NEW_RESEARCHER = "New Library Card Request in DUOS";

  private final User researcher;

  public NewResearcherLibraryRequestMessage(User signingOfficial, User researcher) {
    super(signingOfficial, EmailType.NEW_RESEARCHER);
    this.researcher = researcher;
  }

  @Override
  public String createSubject() {
    return NEW_RESEARCHER;
  }

  @Override
  public Object createModel(String serverUrl) {
    return Map.of("researcherName", researcher.getDisplayName(), "serverUrl", serverUrl);
  }

  @Override
  public String getEntityReferenceId() {
    return researcher.getUserId().toString();
  }
}
