package org.broadinstitute.consent.http.db.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.broadinstitute.consent.http.enumeration.AuditActions;
import org.broadinstitute.consent.http.models.LibraryCardDaaAudit;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

public class LibraryCardDaaAuditMapper implements RowMapper<LibraryCardDaaAudit>, RowMapperHelper {
  @Override
  public LibraryCardDaaAudit map(ResultSet rs, StatementContext ctx) throws SQLException {
    int lcId = hasColumn(rs, "lc_id") ? rs.getInt("lc_id") : 0;
    return new LibraryCardDaaAudit(
        rs.getLong("id"),
        rs.getInt("daa_id"),
        lcId > 0 ? lcId : null,
        rs.getInt("user_id"),
        AuditActions.valueOf(rs.getString("action").toUpperCase()),
        rs.getTimestamp("action_date").toInstant());
  }
}
