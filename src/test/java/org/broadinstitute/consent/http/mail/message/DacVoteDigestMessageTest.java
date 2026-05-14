package org.broadinstitute.consent.http.mail.message;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Stream;
import org.broadinstitute.consent.http.models.Reminder;
import org.broadinstitute.consent.http.models.User;
import org.junit.jupiter.api.Test;

class DacVoteDigestMessageTest extends AbstractMailMessageTest {

  @Test
  void testMessageSubject() {
    var message = new DacVoteDigestMessage(new User(), List.of(), "", Instant.now());
    assertEquals("DUOS: Votes Needed on DARs", message.createSubject());
  }

  @Test
  void testMessageRef() {
    String refId = "ABC";
    var message = new DacVoteDigestMessage(new User(), List.of(), refId, Instant.now());
    assertEquals(refId, message.getEntityReferenceId());
  }

  @Test
  void testMessageTemplate_Reminders_reminder_groupings() {
    User toUser = new User();
    toUser.setUserId(1);
    toUser.setDisplayName("Reminder User");
    Instant timeBasis = Instant.now();
    String refId = timeBasis.toString();
    List<Reminder> olderReminders =
        List.of(
            new Reminder(toUser.getUserId(), "DAR-1", 2, timeBasis.minus(21, ChronoUnit.DAYS)),
            new Reminder(toUser.getUserId(), "DAR-2", 3, timeBasis.minus(14, ChronoUnit.DAYS)));
    Reminder lastWeekReminder =
        new Reminder(toUser.getUserId(), "DAR-3", 4, timeBasis.minus(7, ChronoUnit.DAYS));
    Reminder currentWeekReminder =
        new Reminder(toUser.getUserId(), "DAR-4", 5, timeBasis.minus(1, ChronoUnit.DAYS));
    List<Reminder> reminderList =
        Stream.concat(Stream.of(lastWeekReminder, currentWeekReminder), olderReminders.stream())
            .toList();
    var message = new DacVoteDigestMessage(toUser, reminderList, refId, timeBasis);

    List<Reminder> extractedCurrentWeekReminders = message.getCurrentWeekReminders(reminderList);
    assertEquals(1, extractedCurrentWeekReminders.size());
    assertTrue(extractedCurrentWeekReminders.contains(currentWeekReminder));

    List<Reminder> extractedLastWeekReminders = message.getLastWeekReminders(reminderList);
    assertEquals(1, extractedLastWeekReminders.size());
    assertTrue(extractedLastWeekReminders.contains(lastWeekReminder));

    List<Reminder> extractedOlderReminders = message.getOlderReminders(reminderList);
    assertEquals(2, message.getOlderReminders(reminderList).size());
    assertTrue(extractedOlderReminders.containsAll(olderReminders));
  }

  @Test
  void testMessageTemplate_Reminders_template_all_groupings() throws Exception {
    User toUser = new User();
    toUser.setUserId(1);
    toUser.setDisplayName("Reminder User");
    Instant timeBasis = Instant.now();
    String refId = timeBasis.toString();
    List<Reminder> olderReminders =
        List.of(
            new Reminder(toUser.getUserId(), "DAR-1", 2, timeBasis.minus(21, ChronoUnit.DAYS)),
            new Reminder(toUser.getUserId(), "DAR-2", 3, timeBasis.minus(14, ChronoUnit.DAYS)));
    Reminder lastWeekReminder =
        new Reminder(toUser.getUserId(), "DAR-3", 4, timeBasis.minus(7, ChronoUnit.DAYS));
    Reminder currentWeekReminder =
        new Reminder(toUser.getUserId(), "DAR-4", 5, timeBasis.minus(1, ChronoUnit.DAYS));
    List<Reminder> reminderList =
        Stream.concat(Stream.of(lastWeekReminder, currentWeekReminder), olderReminders.stream())
            .toList();
    var message = new DacVoteDigestMessage(toUser, reminderList, refId, timeBasis);

    var rendered = renderTemplate(message, "localhost:8080");
    assertEquals(
        "Dear %s,".formatted(toUser.getDisplayName()),
        getElementTextById(rendered.document(), "userName"));
    assertTrue(hasElementWithId(rendered.document(), "submittedThisWeek"));
    assertTrue(hasElementWithId(rendered.document(), "submittedLastWeek"));
    assertTrue(hasElementWithId(rendered.document(), "olderRequests"));
  }

  @Test
  void testMessageTemplate_Reminders_template_only_older_reminders() throws Exception {
    User toUser = new User();
    toUser.setUserId(1);
    toUser.setDisplayName("Reminder User");
    Instant timeBasis = Instant.now();
    String refId = timeBasis.toString();
    List<Reminder> olderReminders =
        List.of(
            new Reminder(toUser.getUserId(), "DAR-1", 2, timeBasis.minus(21, ChronoUnit.DAYS)),
            new Reminder(toUser.getUserId(), "DAR-2", 3, timeBasis.minus(14, ChronoUnit.DAYS)));

    var message = new DacVoteDigestMessage(toUser, olderReminders, refId, timeBasis);

    var rendered = renderTemplate(message, "localhost:8080");
    assertEquals(
        "Dear %s,".formatted(toUser.getDisplayName()),
        getElementTextById(rendered.document(), "userName"));
    assertFalse(hasElementWithId(rendered.document(), "submittedThisWeek"));
    assertFalse(hasElementWithId(rendered.document(), "submittedLastWeek"));
    assertTrue(hasElementWithId(rendered.document(), "olderRequests"));
  }

  @Test
  void testMessageTemplate_Reminders_template_missing_dar_code() throws Exception {
    User toUser = new User();
    toUser.setUserId(1);
    toUser.setDisplayName("Reminder User");
    Instant timeBasis = Instant.now();
    String refId = timeBasis.toString();
    List<Reminder> olderReminders =
        List.of(
            new Reminder(toUser.getUserId(), "DAR-1", 2, timeBasis.minus(21, ChronoUnit.DAYS)),
            new Reminder(toUser.getUserId(), null, 3, timeBasis.minus(14, ChronoUnit.DAYS)));

    var message = new DacVoteDigestMessage(toUser, olderReminders, refId, timeBasis);

    var rendered = renderTemplate(message, "localhost:8080");
    assertEquals(
        "Dear %s,".formatted(toUser.getDisplayName()),
        getElementTextById(rendered.document(), "userName"));
    assertFalse(hasElementWithId(rendered.document(), "submittedThisWeek"));
    assertFalse(hasElementWithId(rendered.document(), "submittedLastWeek"));
    assertTrue(hasElementWithId(rendered.document(), "olderRequests"));
  }

  @Test
  void testMessageTemplate_Reminders_template_missing_create_date() throws Exception {
    User toUser = new User();
    toUser.setUserId(1);
    toUser.setDisplayName("Reminder User");
    Instant timeBasis = Instant.now();
    String refId = timeBasis.toString();
    List<Reminder> olderReminders =
        List.of(
            new Reminder(toUser.getUserId(), "DAR-1", 2, timeBasis.minus(21, ChronoUnit.DAYS)),
            new Reminder(toUser.getUserId(), "DAR-2", 3, null));

    var message = new DacVoteDigestMessage(toUser, olderReminders, refId, timeBasis);

    var rendered = renderTemplate(message, "localhost:8080");
    assertEquals(
        "Dear %s,".formatted(toUser.getDisplayName()),
        getElementTextById(rendered.document(), "userName"));
    assertFalse(hasElementWithId(rendered.document(), "submittedThisWeek"));
    assertFalse(hasElementWithId(rendered.document(), "submittedLastWeek"));
    assertTrue(hasElementWithId(rendered.document(), "olderRequests"));
  }

  @Test
  void testReminderInTemplate() throws Exception {
    User toUser = new User();
    toUser.setUserId(1);
    toUser.setDisplayName("Reminder User");
    Instant timeBasis = Instant.now().minus(48, ChronoUnit.HOURS);
    Reminder reminder = new Reminder(1, "DAR-1", 2, timeBasis);
    String refId = timeBasis.toString();

    var message = new DacVoteDigestMessage(toUser, List.of(reminder), refId, timeBasis);

    var rendered = renderTemplate(message, "localhost:8080");

    assertEquals(1, reminder.userId());
    assertEquals("DAR-1", reminder.darCode());
    assertEquals(2, reminder.collectionId());
    assertEquals(timeBasis, reminder.createDate());
    assertThat(rendered.content(), containsString("DAR-1"));
  }
}
