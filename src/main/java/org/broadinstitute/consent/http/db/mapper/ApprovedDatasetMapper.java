package org.broadinstitute.consent.http.db.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import org.broadinstitute.consent.http.models.ApprovedDataset;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

public class ApprovedDatasetMapper implements RowMapper<ApprovedDataset> {

  @Override
  public ApprovedDataset map(ResultSet rs, StatementContext ctx) throws SQLException {
    return new ApprovedDataset(
        rs.getInt("alias"),
        rs.getString("dar_code"),
        rs.getString("dataset_name"),
        rs.getString("dac_name"),
        Timestamp.from(
            Instant.ofEpochMilli(
                DataAccessRequest.EXPIRATION_DURATION_MILLIS
                    + rs.getTimestamp("submission_date").getTime())));
  }
}
