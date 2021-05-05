package org.broadinstitute.consent.http.db.mapper;

//import org.broadinstitute.consent.http.enumeration.RoleStatus;
import org.broadinstitute.consent.http.models.Institution;
//import org.broadinstitute.consent.http.models.User;
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

    // User createUser = new User();

    // if (hasColumn(resultSet, "dacUserId")) {
    //   createUser.setDacUserId(resultSet.getInt("dacUserId"));
    // } 
    // if (hasColumn(resultSet, "email")) {
    //   createUser.setEmail(resultSet.getString("email"));
    // }
    // if (hasColumn(resultSet, "displayName")) {
    //   createUser.setDisplayName(resultSet.getString("displayName"));
    // }
    // if (hasColumn(resultSet, "createDate")) {
    //   createUser.setCreateDate(resultSet.getDate("createDate"));
    // }
    // if (hasColumn(resultSet, "additional_email")) {
    //   createUser.setAdditionalEmail(resultSet.getString("additional_email"));
    // }
    // if (hasColumn(resultSet, "email_preference")) {
    //   createUser.setEmailPreference(resultSet.getBoolean("email_preference"));
    // }
    // if (hasColumn(resultSet, "status")) {
    //   createUser.setStatus(getCreateStatus(resultSet));
    // }
    // if (hasColumn(resultSet, "rationale")) {
    //   createUser.setRationale(resultSet.getString("rationale"));
    // }

    // User updateUser = new User();

    // if (hasColumn(resultSet, "updateUserId")) {
    //   updateUser.setDacUserId(resultSet.getInt("updateUserId"));
    // } 
    // if (hasColumn(resultSet, "updateUserEmail")) {
    //   updateUser.setEmail(resultSet.getString("updateUserEmail"));
    // }
    // if (hasColumn(resultSet, "updateUserName")) {
    //   updateUser.setDisplayName(resultSet.getString("updateUserName"));
    // }
    // if (hasColumn(resultSet, "updateUserCreateDate")) {
    //   updateUser.setCreateDate(resultSet.getDate("updateUserCreateDate"));
    // }
    // if (hasColumn(resultSet, "updateUserAdditionalEmail")) {
    //   updateUser.setAdditionalEmail(resultSet.getString("updateUserAdditionalEmail"));
    // }
    // if (hasColumn(resultSet, "updateUserEmailPreference")) {
    //   updateUser.setEmailPreference(resultSet.getBoolean("updateUserEmailPreference"));
    // }
    // if (hasColumn(resultSet, "updateUserStatus")) {
    //   updateUser.setStatus(getUpdateStatus(resultSet));
    // }
    // if (hasColumn(resultSet, "updateUserRationale")) {
    //   updateUser.setRationale(resultSet.getString("updateUserRationale"));
    // }

    // institution.setCreateUser(createUser);
    // institution.setUpdateUser(updateUser);

    institutionMap.put(institution.getId(), institution);
    return institution;
  }

  // private String getCreateStatus(ResultSet r) {
  //   try {
  //     return RoleStatus.getStatusByValue(r.getInt("status"));
  //   } catch (Exception e) {
  //     return null;
  //   }
  // }

  // private String getUpdateStatus(ResultSet r) {
  //   try {
  //     return RoleStatus.getStatusByValue(r.getInt("updateUserStatus"));
  //   } catch (Exception e) {
  //     return null;
  //   }
  // }

}
