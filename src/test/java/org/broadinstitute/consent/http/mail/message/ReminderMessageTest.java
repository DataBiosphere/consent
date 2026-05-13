package org.broadinstitute.consent.http.mail.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Objects;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.Vote;
import org.junit.jupiter.api.Test;

class ReminderMessageTest extends AbstractMailMessageTest {

  @Test
  void testMessageSubject() {
    var message =
        new ReminderMessage(new User(), new Vote(), "DUL-123", "Data Use Limitations", "");
    assertEquals(
        "Urgent: Log vote on Data Use Limitations case id: DUL-123.", message.createSubject());
    var message2 =
        new ReminderMessage(new User(), new Vote(), "DAR-123", "Data Access Request", "");
    assertEquals(
        "Urgent: Log votes on Data Access Request case id: DAR-123.", message2.createSubject());
    var message3 = new ReminderMessage(new User(), new Vote(), "RP-123", "Research Purpose", "");
    assertEquals(
        "Urgent: Log votes on Research Purpose Review case id: RP-123.", message3.createSubject());
  }

  @Test
  void testGetReminderTemplate() throws Exception {
    User toUser = new User();
    toUser.setDisplayName("Reminder User");
    Vote vote = new Vote();
    vote.setVoteId(1);
    vote.setElectionId(123);
    String darCode = "DUL-123";
    String voteUrl = "http://testVoteUrl";
    var message = new ReminderMessage(toUser, vote, darCode, "Data Use Limitations", voteUrl);
    assertEquals(vote.getVoteId(), message.getVoteId());
    assertEquals(vote.getElectionId().toString(), message.getEntityReferenceId());

    var rendered = renderTemplate(message, "http://testServerUrl");

    assertEquals(
        "Broad Data Use Oversight System - Your vote was requested for a Data Access Request",
        rendered.document().title());
    assertEquals(
        "Hello Reminder User,",
        Objects.requireNonNull(rendered.document().getElementById("userName")).text());
    assertTrue(rendered.content().contains(darCode));
    assertTrue(rendered.content().contains(voteUrl));
  }
}
