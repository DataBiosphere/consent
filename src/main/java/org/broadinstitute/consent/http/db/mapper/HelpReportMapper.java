package org.broadinstitute.consent.http.db.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.broadinstitute.consent.http.models.HelpReport;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

public class HelpReportMapper implements RowMapper<HelpReport> {

  public HelpReport map(ResultSet r, StatementContext ctx) throws SQLException {
    return new HelpReport(
        r.getInt("report_id"),
        r.getString("displayName"),
        r.getDate("create_date"),
        r.getString("subject"),
        r.getString("description"));
  }
}
