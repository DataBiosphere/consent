package org.broadinstitute.consent.http.mail.message;

import com.google.common.annotations.VisibleForTesting;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.broadinstitute.consent.http.enumeration.EmailType;
import org.broadinstitute.consent.http.models.Reminder;
import org.broadinstitute.consent.http.models.User;

public class DacVoteDigestMessage extends MailMessage {

  private final List<Reminder> userReminderList;
  private final String referenceId;
  private final Instant timeBasis;

  public DacVoteDigestMessage(
      User toUser, List<Reminder> userReminderList, String referenceId, Instant timeBasis) {
    super(toUser, EmailType.DAC_VOTE_REMINDER_DIGEST);
    this.userReminderList = userReminderList;
    this.referenceId = referenceId;
    this.timeBasis = timeBasis;
  }

  @Override
  public String createSubject() {
    return "DUOS: Votes Needed on DARs";
  }

  @Override
  public Object createModel(String serverUrl) {
    List<Reminder> sortedReminderList =
        userReminderList.stream()
            .filter(r -> r.createDate() != null)
            .sorted(Comparator.comparing(Reminder::createDate).reversed())
            .toList();
    List<Reminder> currentWeekReminders = getCurrentWeekReminders(sortedReminderList);
    List<Reminder> lastWeekReminders = getLastWeekReminders(sortedReminderList);
    List<Reminder> olderReminders = getOlderReminders(sortedReminderList);
    return Map.of(
        "userName", toUser.getDisplayName(),
        "openedThisWeek", currentWeekReminders,
        "openedLastWeek", lastWeekReminders,
        "olderRequests", olderReminders,
        "serverUrl", serverUrl);
  }

  @Override
  public String getEntityReferenceId() {
    return referenceId;
  }

  @VisibleForTesting
  protected List<Reminder> getCurrentWeekReminders(List<Reminder> reminders) {
    Instant oneWeekAgo = timeBasis.minus(7, ChronoUnit.DAYS);
    return reminders.stream()
        .filter(
            reminder ->
                reminder.darCode() != null
                    && reminder.createDate() != null
                    && reminder.createDate().isAfter(oneWeekAgo))
        .toList();
  }

  @VisibleForTesting
  protected List<Reminder> getLastWeekReminders(List<Reminder> reminders) {
    Instant oneWeekAgo = timeBasis.minus(7, ChronoUnit.DAYS);
    Instant twoWeeksAgo = oneWeekAgo.minus(7, ChronoUnit.DAYS);
    return reminders.stream()
        .filter(
            reminder ->
                reminder.darCode() != null
                    && reminder.createDate() != null
                    && (reminder.createDate().equals(oneWeekAgo)
                        || reminder.createDate().isBefore(oneWeekAgo))
                    && reminder.createDate().isAfter(twoWeeksAgo))
        .toList();
  }

  @VisibleForTesting
  protected List<Reminder> getOlderReminders(List<Reminder> reminders) {
    Instant twoWeeksAgo = timeBasis.minus(14, ChronoUnit.DAYS);
    return reminders.stream()
        .filter(
            reminder ->
                reminder.darCode() != null
                        && reminder.createDate() != null
                        && (reminder.createDate().isBefore(twoWeeksAgo)
                    || reminder.createDate().equals(twoWeeksAgo)))
        .toList();
  }
}
