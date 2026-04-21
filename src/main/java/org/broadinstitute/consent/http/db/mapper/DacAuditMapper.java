package org.broadinstitute.consent.http.db.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.broadinstitute.consent.http.enumeration.AuditActions;
import org.broadinstitute.consent.http.models.DacAudit;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

public class DacAuditMapper implements RowMapper<DacAudit>, RowMapperHelper {
  @Override
  public DacAudit map(ResultSet rs, StatementContext ctx) throws SQLException {
    int affectedUserId = hasColumn(rs, "affected_user_id") ? rs.getInt("affected_user_id") : 0;
    int roleId = hasColumn(rs, "role_id") ? rs.getInt("role_id") : 0;
    return new DacAudit(
        rs.getLong("id"),
        rs.getInt("dac_id"),
        rs.getInt("user_id"),
        affectedUserId > 0 ? affectedUserId : null,
        roleId > 0 ? roleId : null,
        AuditActions.valueOf(rs.getString("action").toUpperCase()),
        rs.getTimestamp("action_date").toInstant());
  }
}
