package org.broadinstitute.consent.http.db.mapper;

import java.util.Map;
import java.util.Objects;

import org.broadinstitute.consent.http.enumeration.RoleStatus;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.models.User;
import org.jdbi.v3.core.mapper.MappingException;
import org.jdbi.v3.core.result.LinkedHashMapRowReducer;
import org.jdbi.v3.core.result.RowView;

/**
 *  Set the create user and update user on an institution
 */
public class InstitutionWithUsersReducer implements LinkedHashMapRowReducer<Integer, Institution> {

  @Override
  public void accumulate(Map<Integer, Institution> map, RowView rowView) {

    Institution institution = map.computeIfAbsent(
        rowView.getColumn("institution_id", Integer.class),
        id -> rowView.getRow(Institution.class));

    User create_user = new User();
    if (Objects.nonNull(rowView.getColumn("dacuserid", Integer.class))) {
        create_user = rowView.getRow(User.class);
    }

    // Status is an enum type and we need to get the string value
    try {
      if (Objects.nonNull(rowView.getColumn("status", Integer.class))) {
        create_user.setStatus(getCreateStatus(rowView));
      }
    } catch (MappingException e) {
      // Ignore any attempt to map a column that doesn't exist
    }

    User update_user = new User();
    if (Objects.nonNull(rowView.getColumn("updateUserId", Integer.class))) {
        update_user = rowView.getRow(User.class);
    }

    // Status is an enum type and we need to get the string value
    try {
      if (Objects.nonNull(rowView.getColumn("updateUserStatus", Integer.class))) {
        update_user.setStatus(getUpdateStatus(rowView));
      }
    } catch (MappingException e) {
      // Ignore any attempt to map a column that doesn't exist
    }

    institution.setCreateUser(create_user);
    institution.setUpdateUser(update_user);
  }

  private String getCreateStatus(RowView r) {
    try {
      return RoleStatus.getStatusByValue(r.getColumn("status", Integer.class));
    } catch (Exception e) {
      return null;
    }
  }

  private String getUpdateStatus(RowView r) {
    try {
      return RoleStatus.getStatusByValue(r.getColumn("updateUserStatus", Integer.class));
    } catch (Exception e) {
      return null;
    }
  }

}