package org.broadinstitute.consent.http.db.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetSummary;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

public class DatasetSummaryMapper implements RowMapper<DatasetSummary>, RowMapperHelper {

  @Override
  public DatasetSummary map(ResultSet rs, StatementContext ctx) throws SQLException {
    if (hasNonZeroColumn(rs, "dataset_id") && hasColumn(rs, "name") && hasNonZeroColumn(rs,
        "alias")) {
      String identifier = Dataset.parseAliasToIdentifier(rs.getInt("alias"));
      return new DatasetSummary(rs.getInt("dataset_id"), identifier, rs.getString("name"));
    }
    return null;
  }
}
