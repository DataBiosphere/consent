package org.broadinstitute.consent.http.db.mapper;

import java.sql.Timestamp;
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
    if (Objects.nonNull(rowView.getColumn("u_dacuserid", Integer.class))) {
        create_user = rowView.getRow(User.class);
    }

    // Status is an enum type and we need to get the string value
    try {
      if (Objects.nonNull(rowView.getColumn("u_status", Integer.class))) {
        create_user.setStatus(getStatus(rowView, "u_status"));
      }
    } catch (MappingException e) {
      // Ignore any attempt to map a column that doesn't exist
    }

    User update_user = new User();
    update_user.setDacUserId(rowView.getColumn("u2_dacuserid", Integer.class));
    update_user.setEmail(rowView.getColumn("u2_email", String.class));
    update_user.setDisplayName(rowView.getColumn("u2_displayname", String.class));
    update_user.setCreateDate(rowView.getColumn("u2_createdate", Timestamp.class));
    update_user.setAdditionalEmail(rowView.getColumn("u2_additional_email", String.class));
    update_user.setEmailPreference(rowView.getColumn("u2_email_preference", Boolean.class));
    update_user.setStatus(getStatus(rowView, "u2_status"));
    update_user.setRationale(rowView.getColumn("u2_rationale", String.class));

    institution.setCreateUser(create_user);
    institution.setUpdateUser(update_user);
  }

  private String getStatus(RowView r, String columnName) {
    try {
      return RoleStatus.getStatusByValue(r.getColumn(columnName, Integer.class));
    } catch (Exception e) {
      return null;
    }
  }

}