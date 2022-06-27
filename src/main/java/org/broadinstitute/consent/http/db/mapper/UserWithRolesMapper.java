package org.broadinstitute.consent.http.db.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

/**
 * This class works well for collections. When a query returns a single user, all
 * rows are not iterated over regardless of the number of records returned by the query.
 * See @UserReducer for mapping results on a single user.
 */
public class UserWithRolesMapper implements RowMapper<User>, RowMapperHelper {

  private final Map<Integer, User> users = new HashMap<>();

  public User map(ResultSet r, StatementContext ctx) throws SQLException {
    User user;
    if (!users.containsKey(r.getInt("user_id"))) {
      user = new User();
      user.setUserId(r.getInt("user_id"));
      user.setEmail(r.getString("email"));
      user.setDisplayName(r.getString("display_name"));
      user.setCreateDate(r.getDate("create_date"));
      user.setAdditionalEmail(r.getString("additional_email"));
      user.setEmailPreference(r.getBoolean("email_preference"));
      user.setRoles(new ArrayList<>());
      if (hasColumn(r, "completed")) {
        user.setProfileCompleted(Boolean.valueOf(r.getString("completed")));
      }
      if (hasColumn(r, "era_commons_id")) {
        user.setEraCommonsId(r.getString("era_commons_id"));
      }
    } else {
      user = users.get(r.getInt("user_id"));
    }
    addRole(r, user);
    // Populate for backwards compatibility.
    user.setDacUserId();
    users.put(user.getUserId(), user);
    return user;
  }

  private void addRole(ResultSet r, User user) throws SQLException {
    if (r.getObject("user_role_id") != null
        && r.getObject("user_id") != null
        && r.getObject("role_id") != null) {
      Integer dacId = (r.getObject("dac_id") == null) ? null : r.getInt("dac_id");
      UserRole role =
          new UserRole(
              r.getInt("user_role_id"),
              r.getInt("user_id"),
              r.getInt("role_id"),
              r.getString("name"),
              dacId);
      user.addRole(role);
    }
  }
}
