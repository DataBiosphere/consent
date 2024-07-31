package org.broadinstitute.consent.http.db.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetStudySummary;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

public class DatasetStudySummaryMapper implements RowMapper<DatasetStudySummary>, RowMapperHelper {

  @Override
  public DatasetStudySummary map(ResultSet rs, StatementContext ctx) throws SQLException {
    Integer datasetId = hasNonZeroColumn(rs, "dataset_id") ? rs.getInt("dataset_id") : null;
    String datasetName = rs.getString("dataset_name");
    String studyName = rs.getString("study_name");
    Integer studyId = hasNonZeroColumn(rs, "study_id") ? rs.getInt("study_id") : null;
    Integer alias = hasNonZeroColumn(rs, "alias") ? rs.getInt("alias") : 0;
    String identifier = Dataset.parseAliasToIdentifier(alias);
    return new DatasetStudySummary(datasetId, datasetName, identifier, studyId, studyName);
  }
}
