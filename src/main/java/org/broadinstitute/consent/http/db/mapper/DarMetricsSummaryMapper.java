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
    // When access began, which is the whole collection's earliest approved submission rather than
    // the latest renewal's: the section reports how long a dataset has been in use.
    Timestamp submissionDate = rs.getTimestamp("submission_date");
    return new DarMetricsSummary(
        rs.getTimestamp("update_date"),
        submissionDate,
        rs.getString("project_title"),
        rs.getString("dar_code"),
        rs.getString("non_tech_rus"),
        rs.getString("reference_id"),
        rs.getString("pi_name"),
        rs.getString("institution_name"),
        hasLapsed(rs.getTimestamp("expiration_basis_date"), rs.getTimestamp("closeout_date")));
  }

  /**
   * Access ends either when its term runs out or when the researcher closes it out, whichever
   * actually happened. A closeout revokes access the day it is filed, so it ends the grant even
   * though the term it cut short may still have had time left to run.
   */
  private boolean hasLapsed(Timestamp latestRenewal, Timestamp closeoutDate) {
    if (closeoutDate != null) {
      return closeoutDate.getTime() < System.currentTimeMillis();
    }
    return latestRenewal != null
        && latestRenewal.getTime() + DataAccessRequest.EXPIRATION_DURATION_MILLIS
            < System.currentTimeMillis();
  }
}
