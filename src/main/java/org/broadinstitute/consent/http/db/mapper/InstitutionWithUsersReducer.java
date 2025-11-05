package org.broadinstitute.consent.http.db.mapper;

import java.sql.Timestamp;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.service.UserService.SimplifiedUser;
import org.jdbi.v3.core.result.LinkedHashMapRowReducer;
import org.jdbi.v3.core.result.RowView;

/** Set the create user and update user on an institution */
public class InstitutionWithUsersReducer
    implements LinkedHashMapRowReducer<Integer, Institution>, RowMapperHelper {

  @Override
  public void accumulate(Map<Integer, Institution> map, RowView rowView) {

    Institution institution =
        map.computeIfAbsent(
            rowView.getColumn("institution_id", Integer.class),
            id -> rowView.getRow(Institution.class));

    User createUser = new User();
    if (Objects.nonNull(rowView.getColumn("u_user_id", Integer.class))) {
      createUser = rowView.getRow(User.class);
    }

    User updateUser = new User();
    updateUser.setUserId(rowView.getColumn("u2_user_id", Integer.class));
    updateUser.setEmail(rowView.getColumn("u2_email", String.class));
    updateUser.setDisplayName(rowView.getColumn("u2_display_name", String.class));
    updateUser.setCreateDate(rowView.getColumn("u2_create_date", Timestamp.class));
    updateUser.setEmailPreference(rowView.getColumn("u2_email_preference", Boolean.class));
    updateUser.setEraCommonsId(rowView.getColumn("u2_era_commons_id", String.class));

    institution.setCreateUser(createUser);
    institution.setUpdateUser(updateUser);

    if (Objects.nonNull(rowView.getColumn("so_user_id", Integer.class))) {
      SimplifiedUser signingOfficial = rowView.getRow(SimplifiedUser.class);
      signingOfficial.setInstitutionId(institution.getId());
      institution.addSigningOfficial(signingOfficial);
    }
    if (hasColumn(rowView, "domain", String.class)) {
      String domain = rowView.getColumn("domain", String.class);
      if (!StringUtils.isBlank(domain)) {
        institution.addDomain(domain);
      }
    }
  }
}
