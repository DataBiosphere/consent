package org.broadinstitute.consent.http.db.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.broadinstitute.consent.http.enumeration.AuditActions;
import org.broadinstitute.consent.http.models.DaaAudit;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

public class DaaAuditMapper implements RowMapper<DaaAudit>, RowMapperHelper {
  @Override
  public DaaAudit map(ResultSet rs, StatementContext ctx) throws SQLException {
    int dacId = hasColumn(rs, "dac_id") ? rs.getInt("dac_id") : 0;
    return new DaaAudit(
        rs.getLong("id"),
        rs.getInt("daa_id"),
        dacId > 0 ? dacId : null,
        rs.getInt("user_id"),
        AuditActions.valueOf(rs.getString("action").toUpperCase()),
        rs.getTimestamp("action_date").toInstant());
  }
}
