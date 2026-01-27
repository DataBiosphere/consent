package org.broadinstitute.consent.http.db.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.broadinstitute.consent.http.models.FeatureFlag;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

public class FeatureFlagMapper implements RowMapper<FeatureFlag> {

  @Override
  public FeatureFlag map(ResultSet rs, StatementContext ctx) throws SQLException {
    return new FeatureFlag(
        rs.getString("id"),
        rs.getString("value"),
        rs.getTimestamp("create_date").toInstant(),
        rs.getTimestamp("update_date").toInstant());
  }
}
