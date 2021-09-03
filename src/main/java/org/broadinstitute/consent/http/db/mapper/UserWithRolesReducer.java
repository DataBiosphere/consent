package org.broadinstitute.consent.http.db.mapper;

import java.util.Map;
import java.util.Objects;

import org.broadinstitute.consent.http.enumeration.RoleStatus;
import org.broadinstitute.consent.http.models.LibraryCard;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.jdbi.v3.core.mapper.MappingException;
import org.jdbi.v3.core.result.LinkedHashMapRowReducer;
import org.jdbi.v3.core.result.RowView;

/**
 * This class works well for individual Users as well as collections.
 */
public class UserWithRolesReducer implements LinkedHashMapRowReducer<Integer, User> {
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
      if(Objects.nonNull(rowView.getColumn("i_id", Integer.class))) {
        Institution institution = rowView.getRow(Institution.class);
        user.setInstitution(institution);
      }
    } catch(MappingException e) {
      //Ignore institution mapping errors, possible for new users to not have an institution
    }
    //user role join can cause duplication of data if done in tandem with joins on other tables
    //ex) The same LC can end up being repeated multiple times
    //Below only adds LC if not currently saved on the array
    try {
      if(Objects.nonNull(rowView.getColumn("lc_id", Integer.class))) {
        LibraryCard lc = rowView.getRow(LibraryCard.class);
        if(Objects.isNull(user.getLibraryCards()) || !user.getLibraryCards().stream().anyMatch(card -> card.getId() == lc.getId())) {
          user.addLibraryCard(lc);
        }
      }
    } catch(MappingException e) {
      //Ignore exceptions here, user may not have a library card issued under this instiution
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
