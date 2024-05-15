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

/**
 * This class works well for individual Users as well as collections.
 */
public class UserWithRolesReducer implements LinkedHashMapRowReducer<Integer, User>,
    RowMapperHelper {

  @Override
  public void accumulate(Map<Integer, User> map, RowView rowView) {
    // Some queries look for `user_id` while those that use a prefix look for `u_user_id`
    Integer userId = 0;
    if (hasColumn(rowView, "user_id", Integer.class)) {
      userId = rowView.getColumn("user_id", Integer.class);
    } else if (hasColumn(rowView, "u_user_id", Integer.class)) {
      userId = rowView.getColumn("u_user_id", Integer.class);
    }
    User user =
        map.computeIfAbsent(
            userId,
            id -> rowView.getRow(User.class));

    try {
      // Some queries look for `user_role_id` while those that use a prefix look for `u_user_role_id`
      Integer userRoleId = null;
      if (hasColumn(rowView, "user_role_id", Integer.class)) {
        userRoleId = rowView.getColumn("user_role_id", Integer.class);
      } else if (hasColumn(rowView, "ur_user_role_id", Integer.class)) {
        userRoleId = rowView.getColumn("ur_user_role_id", Integer.class);
      }
      if (Objects.nonNull(userRoleId)) {
        UserRole ur = rowView.getRow(UserRole.class);
        user.addRole(ur);
      }
    } catch (MappingException e) {
      // Ignore any attempt to map a column that doesn't exist
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
      //Ignore institution mapping errors, possible for new users to not have an institution
    }
    //user role join can cause duplication of data if done in tandem with joins on other tables
    //ex) The same LC can end up being repeated multiple times
    //Below only adds LC if not currently saved on the array
    try {
      if (Objects.nonNull(rowView.getColumn("lc_id", Integer.class))) {
        LibraryCard lc = rowView.getRow(LibraryCard.class);
        try {
          if (Objects.nonNull(rowView.getColumn("lci_id", Integer.class))) {
            Institution institution = rowView.getRow(Institution.class);
            // There are unusual cases where we somehow create an institution with null values
            if (Objects.nonNull(institution.getId())) {
              lc.setInstitution(institution);
            }
          }
        } catch (MappingException e) {
          // Ignore institution mapping errors
        }
        if (rowView.getColumn("lc_daa_id", Integer.class) != null) {
          lc.addDaa(rowView.getColumn("lc_daa_id", Integer.class));
        }
        if (Objects.isNull(user.getLibraryCards()) || user.getLibraryCards().stream()
            .noneMatch(card -> card.getId().equals(lc.getId()))) {
          user.addLibraryCard(lc);
        }
      }
    } catch (MappingException e) {
      //Ignore exceptions here, user may not have a library card issued under this instiution
    }
    try {
      if (Objects.nonNull(rowView.getColumn("up_property_id", Integer.class))) {
        UserProperty p = rowView.getRow(UserProperty.class);
        user.addProperty(p);
      }
    } catch (MappingException e) {
      // Ignore any attempt to map a column that doesn't exist
    }
  }
}
