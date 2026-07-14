package org.broadinstitute.consent.http.mail.message;

import java.util.Map;
import org.broadinstitute.consent.http.enumeration.EmailType;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.Vote;

public class ReminderMessage extends MailMessage {

  private static final String REMINDER_DAR =
      "Urgent: Log votes on Data Access Request case id: %s.";

  private final Vote vote;
  private final String darCode;
  private final String voteUrl;

  public ReminderMessage(User toUser, Vote vote, String darCode, String voteUrl) {
    super(toUser, EmailType.REMINDER);
    this.vote = vote;
    this.darCode = darCode;
    this.voteUrl = voteUrl;
  }

  @Override
  public String createSubject() {
    return String.format(REMINDER_DAR, darCode);
  }

  @Override
  public Map<String, Object> createModel() {
    return Map.of("userName", toUser.getDisplayName(), "entityName", darCode, "serverUrl", voteUrl);
  }

  @Override
  public String getEntityReferenceId() {
    return vote.getElectionId().toString();
  }

  @Override
  public Integer getVoteId() {
    return vote.getVoteId();
  }
}
