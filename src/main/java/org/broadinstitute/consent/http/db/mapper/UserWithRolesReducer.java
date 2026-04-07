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
    Integer userId = resolveUserId(rowView);
    User user = map.computeIfAbsent(userId, id -> rowView.getRow(User.class));
    addUserRole(rowView, user, userId);
    addInstitution(rowView, user);
    addLibraryCard(rowView, user);
    addUserProperty(rowView, user);
  }

  private Integer resolveUserId(RowView rowView) {
    // Some queries look for `user_id` while those that use a prefix look for `u_user_id`
    return hasOptionalColumn(rowView, "user_id", Integer.class)
        .or(() -> hasOptionalColumn(rowView, "u_user_id", Integer.class))
        .orElse(0);
  }

  private void addUserRole(RowView rowView, User user, Integer userId) {
    try {
      UserRole userRole = mapUserRoleFromRowView(rowView, userId);
      if (userRole != null) {
        user.addRole(userRole);
      }
    } catch (MappingException e) {
      logWarn("Error adding User Role to User", e);
    }
  }

  private void addInstitution(RowView rowView, User user) {
    try {
      hasOptionalColumn(rowView, "i_id", Integer.class)
          .ifPresent(
              id -> {
                Institution institution = rowView.getRow(Institution.class);
                // There are unusual cases where we somehow create an institution with null values
                if (Objects.nonNull(institution.getId())) {
                  user.setInstitution(institution);
                }
              });
    } catch (MappingException e) {
      logDebug("Error adding Institution to User: %s".formatted(e.getMessage()));
    }
  }

  // user role join can cause duplication of data if done in tandem with joins on other tables
  // ex) The same LC can end up being repeated multiple times
  // Below only adds LC if not currently saved on the user
  private void addLibraryCard(RowView rowView, User user) {
    try {
      hasOptionalColumn(rowView, "lc_id", Integer.class)
          .ifPresent(
              lcId -> {
                if (Objects.isNull(user.getLibraryCard())) {
                  user.setLibraryCard(rowView.getRow(LibraryCard.class));
                }
                hasOptionalColumn(rowView, "lc_daa_id", Integer.class)
                    .ifPresent(daaId -> user.getLibraryCard().addDaa(daaId));
              });
    } catch (MappingException e) {
      logDebug("Error adding Library Card to User: %s".formatted(e.getMessage()));
    }
  }

  private void addUserProperty(RowView rowView, User user) {
    try {
      hasOptionalColumn(rowView, "up_property_id", Integer.class)
          .ifPresent(id -> user.addProperty(rowView.getRow(UserProperty.class)));
    } catch (MappingException e) {
      logDebug("Error adding User Property to User: %s".formatted(e.getMessage()));
    }
  }

  // Some queries look for `user_role_id` while those that use a prefix look for `ur_user_role_id`
  private UserRole mapUserRoleFromRowView(RowView rowView, Integer userId) {
    if (hasNonZeroColumn(rowView, "user_role_id")) {
      return buildUserRole(rowView, userId, "user_role_id", "role_id", "name", "dac_id");
    }
    if (hasNonZeroColumn(rowView, "ur_user_role_id")) {
      return buildUserRole(
          rowView, userId, "ur_user_role_id", "ur_role_id", "ur_name", "ur_dac_id");
    }
    return null;
  }

  private UserRole buildUserRole(
      RowView rowView,
      Integer userId,
      String userRoleIdCol,
      String roleIdCol,
      String nameCol,
      String dacIdCol) {
    return new UserRole(
        rowView.getColumn(userRoleIdCol, Integer.class),
        userId,
        rowView.getColumn(roleIdCol, Integer.class),
        rowView.getColumn(nameCol, String.class),
        hasOptionalColumn(rowView, dacIdCol, Integer.class).orElse(null));
  }
}
