package org.broadinstitute.consent.http.mail.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import freemarker.template.TemplateException;
import java.io.IOException;
import java.io.StringWriter;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.broadinstitute.consent.http.configurations.FreeMarkerConfiguration;
import org.broadinstitute.consent.http.mail.freemarker.FreeMarkerTemplateHelper;
import org.broadinstitute.consent.http.models.Reminder;
import org.broadinstitute.consent.http.models.User;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DacVoteDigestMessageTest {
  private FreeMarkerTemplateHelper helper;

  @BeforeEach
  void setUp() {
    FreeMarkerConfiguration freeMarkerConfig = new FreeMarkerConfiguration();
    freeMarkerConfig.setTemplateDirectory("/freemarker");
    freeMarkerConfig.setDefaultEncoding("UTF-8");
    helper = new FreeMarkerTemplateHelper(freeMarkerConfig);
  }

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
  void testMessageTemplate_Reminders_template_all_groupings()
      throws IOException, TemplateException {
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

    var template = helper.getTemplate(message.getTemplateName());
    var out = new StringWriter();
    template.process(message.createModel("localhost:8080"), out);
    var templateString = out.toString();
    Document parsedTemplate = Jsoup.parse(templateString);
    assertEquals(
        "Dear %s,".formatted(toUser.getDisplayName()),
        getElementTextById(parsedTemplate, "userName"));
    assertTrue(hasElementWithId(parsedTemplate, "submittedThisWeek"));
    assertTrue(hasElementWithId(parsedTemplate, "submittedLastWeek"));
    assertTrue(hasElementWithId(parsedTemplate, "olderRequests"));
  }

  @Test
  void testMessageTemplate_Reminders_template_only_older_reminders()
      throws IOException, TemplateException {
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

    var template = helper.getTemplate(message.getTemplateName());
    var out = new StringWriter();
    template.process(message.createModel("localhost:8080"), out);
    var templateString = out.toString();
    Document parsedTemplate = Jsoup.parse(templateString);
    assertEquals(
        "Dear %s,".formatted(toUser.getDisplayName()),
        getElementTextById(parsedTemplate, "userName"));
    assertFalse(hasElementWithId(parsedTemplate, "submittedThisWeek"));
    assertFalse(hasElementWithId(parsedTemplate, "submittedLastWeek"));
    assertTrue(hasElementWithId(parsedTemplate, "olderRequests"));
  }

  String getElementTextById(Document document, String id) {
    return Objects.requireNonNull(document.getElementById(id)).text();
  }

  boolean hasElementWithId(Document document, String id) {
    return document.getElementById(id) != null;
  }
}
