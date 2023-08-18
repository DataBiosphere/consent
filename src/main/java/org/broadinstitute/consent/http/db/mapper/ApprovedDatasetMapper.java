package org.broadinstitute.consent.http.db.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.broadinstitute.consent.http.models.ApprovedDataset;
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
      rs.getDate("update_date")
    );
  }
}
