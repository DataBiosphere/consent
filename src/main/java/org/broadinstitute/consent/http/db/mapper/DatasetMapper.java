package org.broadinstitute.consent.http.db.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DataUse;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

public class DatasetMapper implements RowMapper<Dataset>, RowMapperHelper {

  public Dataset map(ResultSet r, StatementContext ctx) throws SQLException {
      Dataset dataset = new Dataset();
      dataset.setDataSetId(r.getInt("dataset_id"));
      dataset.setObjectId(r.getString("object_id"));
      dataset.setName(r.getString("name"));
      if (hasColumn(r, "create_date")) {
          dataset.setCreateDate(r.getDate("create_date"));
      }
      if (hasColumn(r, "create_user_id")) {
          int userId = r.getInt("create_user_id");
          if (userId > 0) {
              dataset.setCreateUserId(userId);
          }
      }
      if (hasColumn(r, "update_date")) {
          dataset.setUpdateDate(r.getTimestamp("update_date"));
      }
      if (hasColumn(r, "update_user_id")) {
          int userId = r.getInt("update_user_id");
          if (userId > 0) {
              dataset.setUpdateUserId(userId);
          }
      }
      if (hasColumn(r, "data_use")) {
        dataset.setDataUse(DataUse.parseDataUse(r.getString("data_use")).orElse(null));
      }
      if (hasColumn(r, "sharing_plan_document")) {
          dataset.setSharingPlanDocument(r.getString("sharing_plan_document"));
      }
      dataset.setActive(r.getBoolean("active"));
      dataset.setAlias(r.getInt("alias"));

      return dataset;
  }
}
