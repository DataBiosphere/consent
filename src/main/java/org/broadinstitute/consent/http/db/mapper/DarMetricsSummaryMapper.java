package org.broadinstitute.consent.http.db.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import org.broadinstitute.consent.http.models.DarMetricsSummary;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

public class DarMetricsSummaryMapper implements RowMapper<DarMetricsSummary> {

  @Override
  public DarMetricsSummary map(ResultSet rs, StatementContext ctx) throws SQLException {
    Timestamp submissionDate = rs.getTimestamp("submission_date");
    boolean expired =
        submissionDate != null
            && submissionDate.getTime() + DataAccessRequest.EXPIRATION_DURATION_MILLIS
                < System.currentTimeMillis();
    return new DarMetricsSummary(
        rs.getTimestamp("update_date"),
        rs.getString("project_title"),
        rs.getString("dar_code"),
        rs.getString("non_tech_rus"),
        rs.getString("reference_id"),
        expired);
  }
}
