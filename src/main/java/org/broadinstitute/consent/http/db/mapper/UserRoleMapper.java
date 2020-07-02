package org.broadinstitute.consent.http.db.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.broadinstitute.consent.http.models.UserRole;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

public class UserRoleMapper implements RowMapper<UserRole> {
  public UserRole map(ResultSet r, StatementContext ctx) throws SQLException {
    return new UserRole(
        r.getInt("user_role_id"),
        r.getInt("user_id"),
        r.getInt("role_id"),
        r.getString("name"),
        (r.getObject("dac_id") == null) ? null : r.getInt("dac_id"));
  }
}
