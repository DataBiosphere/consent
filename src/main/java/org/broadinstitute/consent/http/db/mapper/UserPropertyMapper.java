package org.broadinstitute.consent.http.db.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.broadinstitute.consent.http.models.UserProperty;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

public class UserPropertyMapper implements RowMapper<UserProperty> {

  @Override
  public UserProperty map(ResultSet r, StatementContext statementContext)
      throws SQLException {
    return new UserProperty(
        r.getInt("propertyId"),
        r.getInt("userId"),
        r.getString("propertyKey"),
        r.getString("propertyValue"));
  }
}
