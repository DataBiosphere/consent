package org.broadinstitute.consent.http.models;

import java.util.ArrayList;
import java.util.List;

public class UserVoteReminder {
  Integer userId;
  List<Reminder> userReminderList;

  public UserVoteReminder(int userId) {
    this.userReminderList = new ArrayList<>();
    this.userId = userId;
  }

  public void addReminder(Reminder reminder) {
    userReminderList.add(reminder);
  }

  public Integer getuserId() {
    return this.userId;
  }

  public List<Reminder> getUserReminderList() {
    return userReminderList;
  }
}
