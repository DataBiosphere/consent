package org.broadinstitute.consent.http.db.mapper;

import java.util.Map;
import org.broadinstitute.consent.http.models.Reminder;
import org.broadinstitute.consent.http.models.UserVoteReminder;
import org.jdbi.v3.core.result.LinkedHashMapRowReducer;
import org.jdbi.v3.core.result.RowView;

public class VoteReminderReducer
    implements LinkedHashMapRowReducer<Integer, UserVoteReminder>, RowMapperHelper {

  @Override
  public void accumulate(Map<Integer, UserVoteReminder> map, RowView rowView) {
    Reminder reminder = rowView.getRow(Reminder.class);
    UserVoteReminder userVoteReminder =
        map.computeIfAbsent(reminder.userId(), UserVoteReminder::new);
    userVoteReminder.addReminder(reminder);
  }
}
