package org.broadinstitute.consent.http.db.mapper;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
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
    Timestamp expirationDate = termEnd(rs.getTimestamp("expiration_basis_date"));
    Timestamp closeoutDate = endedEarly(rs.getTimestamp("closeout_date"), expirationDate);
    return new DarMetricsSummary(
        rs.getTimestamp("update_date"),
        submissionDate,
        rs.getString("project_title"),
        rs.getString("dar_code"),
        rs.getString("non_tech_rus"),
        rs.getString("rus"),
        rs.getString("reference_id"),
        rs.getString("institution_name"),
        hasLapsed(closeoutDate != null ? closeoutDate : expirationDate),
        rs.getTimestamp("approval_date"),
        timestamps(rs.getArray("progress_report_approval_dates")),
        expirationDate,
        closeoutDate);
  }

  /**
   * A grant's term runs {@link DataAccessRequest#EXPIRATION_DURATION_MILLIS} from the submission
   * date of the newest approved submission, original or progress report, not from the day the DAC
   * approved it. This is the rule every grant query applies; it is computed here so the pages never
   * restate it.
   */
  private static Timestamp termEnd(Timestamp latestApprovedSubmission) {
    return latestApprovedSubmission == null
        ? null
        : new Timestamp(
            latestApprovedSubmission.getTime() + DataAccessRequest.EXPIRATION_DURATION_MILLIS);
  }

  /**
   * A closeout revokes access the day it is filed, so it ends the grant even though the term it cut
   * short may still have had time left to run. One filed after the term had already run out ended
   * nothing: that access expired, which is also how the expirations report counts it.
   */
  private static Timestamp endedEarly(Timestamp closeoutDate, Timestamp termEnd) {
    if (closeoutDate == null || (termEnd != null && !closeoutDate.before(termEnd))) {
      return null;
    }
    return closeoutDate;
  }

  /** Access ends when its term runs out or a closeout cuts it short, whichever happened. */
  private static boolean hasLapsed(Timestamp accessEnd) {
    return accessEnd != null && accessEnd.getTime() < System.currentTimeMillis();
  }

  private static List<Timestamp> timestamps(Array array) throws SQLException {
    return array == null ? List.of() : List.of((Timestamp[]) array.getArray());
  }
}
