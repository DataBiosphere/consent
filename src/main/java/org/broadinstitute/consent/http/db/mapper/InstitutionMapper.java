package org.broadinstitute.consent.http.db.mapper;

import org.broadinstitute.consent.http.enumeration.RoleStatus;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.models.User;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class InstitutionMapper implements RowMapper<Institution>, RowMapperHelper {

  private final Map<Integer, Institution>  institutionMap = new HashMap<>();


  @Override
  public Institution map(ResultSet resultSet, StatementContext statementContext) throws SQLException {

    Institution institution;
    int institutionId = resultSet.getInt("institution_id");

    if (institutionMap.containsKey(institutionId)) {
      institution = institutionMap.get(institutionId);
    } else {
      institution = new Institution();
      institution.setId(institutionId);
    }
    if (hasColumn(resultSet, "institution_name")) {
      institution.setName(resultSet.getString("institution_name"));
    }
    if (hasColumn(resultSet, "it_director_name")) {
      institution.setItDirectorName(resultSet.getString("it_director_name"));
    }
    if (hasColumn(resultSet, "it_director_email")) {
      institution.setItDirectorEmail(resultSet.getString("it_director_email"));
    }
    if (hasColumn(resultSet, "create_user")) {
      institution.setCreateUserId(resultSet.getInt("create_user"));
    }
    if (hasColumn(resultSet, "create_date")) {
      institution.setCreateDate(resultSet.getDate("create_date"));
    } 
    if (hasColumn(resultSet, "update_user")) {
      institution.setUpdateUserId(resultSet.getInt("update_user"));
    }
    if (hasColumn(resultSet, "update_date")) {
      institution.setUpdateDate(resultSet.getDate("update_date"));
    }

    User user = new User();;

    if (hasColumn(resultSet, "dacUserId")) {
      user.setDacUserId(resultSet.getInt("dacUserId"));
    } 
    if (hasColumn(resultSet, "email")) {
      user.setEmail(resultSet.getString("email"));
    }
    if (hasColumn(resultSet, "displayName")) {
      user.setDisplayName(resultSet.getString("displayName"));
    }
    if (hasColumn(resultSet, "createDate")) {
      user.setCreateDate(resultSet.getDate("createDate"));
    }
    if (hasColumn(resultSet, "additional_email")) {
      user.setAdditionalEmail(resultSet.getString("additional_email"));
    }
    if (hasColumn(resultSet, "email_preference")) {
      user.setEmailPreference(resultSet.getBoolean("email_preference"));
    }
    if (hasColumn(resultSet, "status")) {
      user.setStatus(getStatus(resultSet));
    }
    if (hasColumn(resultSet, "rationale")) {
      user.setRationale(resultSet.getString("rationale"));
    }
    //user model does not currently have an institutionId field or methods
    // if (hasColumn(resultSet, "institute")) {
    //   user.setInstitutionId(resultSet.getInt("update_user"));
    // }

    institution.setCreateUser(user);

    institutionMap.put(institution.getId(), institution);
    return institution;
  }

  private String getStatus(ResultSet r) {
    try {
      return RoleStatus.getStatusByValue(r.getInt("status"));
    } catch (Exception e) {
      return null;
    }
  }
}
