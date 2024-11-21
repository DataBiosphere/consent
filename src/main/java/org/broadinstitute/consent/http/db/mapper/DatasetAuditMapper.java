package org.broadinstitute.consent.http.db.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.broadinstitute.consent.http.models.DatasetAudit;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

public class DatasetAuditMapper implements RowMapper<DatasetAudit>, RowMapperHelper {

  @Override
  public DatasetAudit map(ResultSet rs, StatementContext ctx) throws SQLException {
    DatasetAudit datasetAudit = new DatasetAudit();
    if (hasNonZeroColumn(rs, "dataset_audit_id")) {
      datasetAudit.setDataSetAuditId(rs.getInt("dataset_audit_id"));
    }
    if (hasNonZeroColumn(rs, "dataset_id")) {
      datasetAudit.setDatasetId(rs.getInt("dataset_id"));
    }
    if (hasColumn(rs, "change_action")) {
      datasetAudit.setAction(rs.getString("change_action"));
    }
    if (hasNonZeroColumn(rs, "modified_by_user")) {
      datasetAudit.setUser(rs.getInt("modified_by_user"));
    }
    if (hasColumn(rs, "modification_date")) {
      datasetAudit.setDate(rs.getDate("modification_date"));
    }
    if (hasColumn(rs, "object_id")) {
      datasetAudit.setObjectId(rs.getString("object_id"));
    }
    if (hasColumn(rs, "name")) {
      datasetAudit.setName(rs.getString("name"));
    }
    return datasetAudit;
  }
}
