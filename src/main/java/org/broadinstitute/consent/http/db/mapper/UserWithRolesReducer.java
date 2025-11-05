package org.broadinstitute.consent.http.db.mapper;

import java.util.Map;
import java.util.Objects;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.models.LibraryCard;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserProperty;
import org.broadinstitute.consent.http.models.UserRole;
import org.jdbi.v3.core.mapper.MappingException;
import org.jdbi.v3.core.result.LinkedHashMapRowReducer;
import org.jdbi.v3.core.result.RowView;

/** This class works well for individual Users as well as collections. */
public class UserWithRolesReducer
    implements LinkedHashMapRowReducer<Integer, User>, RowMapperHelper {

  @Override
  public void accumulate(Map<Integer, User> map, RowView rowView) {
    // Some queries look for `user_id` while those that use a prefix look for `u_user_id`
    Integer userId = 0;
    if (hasNonZeroColumn(rowView, "user_id")) {
      userId = rowView.getColumn("user_id", Integer.class);
    } else if (hasNonZeroColumn(rowView, "u_user_id")) {
      userId = rowView.getColumn("u_user_id", Integer.class);
    }
    User user = map.computeIfAbsent(userId, id -> rowView.getRow(User.class));

    try {
      UserRole userRole = mapUserRoleFromRowView(rowView, userId);
      if (userRole != null) {
        user.addRole(userRole);
      }
    } catch (MappingException e) {
      logWarn("Error adding User Role to User", e);
    }
    try {
      if (Objects.nonNull(rowView.getColumn("i_id", Integer.class))) {
        Institution institution = rowView.getRow(Institution.class);
        // There are unusual cases where we somehow create an institution with null values
        if (Objects.nonNull(institution.getId())) {
          user.setInstitution(institution);
        }
      }
    } catch (MappingException e) {
      logDebug("Error adding Institution to User: %s".formatted(e.getMessage()));
    }
    // user role join can cause duplication of data if done in tandem with joins on other tables
    // ex) The same LC can end up being repeated multiple times
    // Below only adds LC if not currently saved on the array
    try {
      if (rowView.getColumn("lc_id", Integer.class) != null) {
        LibraryCard lc = rowView.getRow(LibraryCard.class);
        if (rowView.getColumn("lc_daa_id", Integer.class) != null) {
          lc.addDaa(rowView.getColumn("lc_daa_id", Integer.class));
        }
        user.setLibraryCard(lc);
      }
    } catch (MappingException e) {
      logDebug("Error adding Library Card to User: %s".formatted(e.getMessage()));
    }
    try {
      if (Objects.nonNull(rowView.getColumn("up_property_id", Integer.class))) {
        UserProperty p = rowView.getRow(UserProperty.class);
        user.addProperty(p);
      }
    } catch (MappingException e) {
      logDebug("Error adding User Property to User: %s".formatted(e.getMessage()));
    }
  }

  // Some queries look for `user_role_id` while those that use a prefix look for `u_user_role_id`
  private UserRole mapUserRoleFromRowView(RowView rowView, Integer userId) {
    Integer userRoleId;
    Integer roleId;
    String name;
    Integer dacId = null;
    // Some queries look for `user_role_id` while those that use a prefix look for `ur_user_role_id`
    if (hasNonZeroColumn(rowView, "user_role_id")) {
      userRoleId = rowView.getColumn("user_role_id", Integer.class);
      roleId = rowView.getColumn("role_id", Integer.class);
      name = rowView.getColumn("name", String.class);
      if (hasNonZeroColumn(rowView, "dac_id")) {
        dacId = rowView.getColumn("dac_id", Integer.class);
      }
      return new UserRole(userRoleId, userId, roleId, name, dacId);
    } else if (hasNonZeroColumn(rowView, "ur_user_role_id")) {
      userRoleId = rowView.getColumn("ur_user_role_id", Integer.class);
      roleId = rowView.getColumn("ur_role_id", Integer.class);
      name = rowView.getColumn("ur_name", String.class);
      if (hasNonZeroColumn(rowView, "ur_dac_id")) {
        dacId = rowView.getColumn("ur_dac_id", Integer.class);
      }
      return new UserRole(userRoleId, userId, roleId, name, dacId);
    }
    return null;
  }
}
