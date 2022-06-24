package org.broadinstitute.consent.http.db.mapper;

import java.sql.Timestamp;
import java.util.Map;
import java.util.Objects;

import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.service.UserService.SimplifiedUser;
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
    if (Objects.nonNull(rowView.getColumn("u_user_id", Integer.class))) {
        create_user = rowView.getRow(User.class);
    }


    User update_user = new User();
    update_user.setUserId(rowView.getColumn("u2_user_id", Integer.class));
    update_user.setEmail(rowView.getColumn("u2_email", String.class));
    update_user.setDisplayName(rowView.getColumn("u2_display_name", String.class));
    update_user.setCreateDate(rowView.getColumn("u2_create_date", Timestamp.class));
    update_user.setAdditionalEmail(rowView.getColumn("u2_additional_email", String.class));
    update_user.setEmailPreference(rowView.getColumn("u2_email_preference", Boolean.class));
    update_user.setEraCommonsId(rowView.getColumn("u2_era_commons_id", String.class));


    institution.setCreateUser(create_user);
    institution.setUpdateUser(update_user);

    if (Objects.nonNull(rowView.getColumn("so_user_id", Integer.class))) {
      SimplifiedUser so_user = rowView.getRow(SimplifiedUser.class);
      institution.addSigningOfficial(so_user);
    }

  }

}