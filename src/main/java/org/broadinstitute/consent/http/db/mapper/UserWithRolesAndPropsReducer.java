package org.broadinstitute.consent.http.db.mapper;

import java.util.Map;
import java.util.Objects;
import org.broadinstitute.consent.http.enumeration.RoleStatus;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserProperty;
import org.broadinstitute.consent.http.models.UserRole;
import org.jdbi.v3.core.mapper.MappingException;
import org.jdbi.v3.core.result.LinkedHashMapRowReducer;
import org.jdbi.v3.core.result.RowView;

/**
 * This class works well for individual Users as well as collections.
 */
public class UserWithRolesAndPropsReducer implements LinkedHashMapRowReducer<Integer, User> {

  @Override
  public void accumulate(Map<Integer, User> map, RowView rowView) {
    User user =
        map.computeIfAbsent(
            rowView.getColumn("dacuserid", Integer.class),
            id -> rowView.getRow(User.class));
    // Status is an enum type and we need to get the string value
    try {
      if (Objects.nonNull(rowView.getColumn("status", Integer.class))) {
        user.setStatus(getStatus(rowView));
      }
    } catch (MappingException e) {
      // Ignore any attempt to map a column that doesn't exist
    }
    try {
      if (Objects.nonNull(rowView.getColumn("user_role_id", Integer.class))) {
        UserRole ur = rowView.getRow(UserRole.class);
        user.addRole(ur);
      }
    } catch (MappingException e) {
      // Ignore any attempt to map a column that doesn't exist
    }
    try {
      if (Objects.nonNull(rowView.getColumn("propertyid", Integer.class))) {
        UserProperty p = rowView.getRow(UserProperty.class);
        user.addProperty(p);
      }
    } catch (MappingException e) {
      // Ignore any attempt to map a column that doesn't exist
    }
  }

  private String getStatus(RowView r) {
    try {
      return RoleStatus.getStatusByValue(r.getColumn("status", Integer.class));
    } catch (Exception e) {
      return null;
    }
  }
}
