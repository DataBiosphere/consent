package org.broadinstitute.consent.http.db.mapper;

import java.util.Map;
import java.util.Objects;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.jdbi.v3.core.result.LinkedHashMapRowReducer;
import org.jdbi.v3.core.result.RowView;

public class UserReducer implements LinkedHashMapRowReducer<Integer, User> {
  @Override
  public void accumulate(Map<Integer, User> map, RowView rowView) {
    User user =
        map.computeIfAbsent(
            rowView.getColumn("dacuserid", Integer.class),
            id -> rowView.getRow(User.class));

    if (Objects.nonNull(rowView.getColumn("user_role_id", Integer.class))) {
      UserRole ur = rowView.getRow(UserRole.class);
      user.addRole(ur);
    }
  }
}
