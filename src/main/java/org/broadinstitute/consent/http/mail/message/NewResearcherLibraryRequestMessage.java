package org.broadinstitute.consent.http.mail.message;

import org.broadinstitute.consent.http.enumeration.EmailType;
import org.broadinstitute.consent.http.models.User;

public class NewResearcherLibraryRequestMessage extends MailMessage {

  private static final String NEW_RESEARCHER = "New Library Card Request in DUOS";

  private final User researcher;

  public NewResearcherLibraryRequestMessage(User researcher, User signingOfficial) {
    super(signingOfficial, EmailType.NEW_RESEARCHER);
    this.researcher = researcher;
  }

  @Override
  public String createSubject() {
    return NEW_RESEARCHER;
  }

  record Model(String researcherName, String serverUrl) { }

  @Override
  public Object createModel(String serverUrl) {
    return new Model(researcher.getDisplayName(), serverUrl);
  }

  @Override
  public String getEntityReferenceId() {
    return researcher.getUserId().toString();
  }
}
