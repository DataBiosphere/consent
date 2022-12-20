package org.broadinstitute.consent.http.db.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DataUse;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

public class DatasetMapper implements RowMapper<Dataset>, RowMapperHelper {

  public Dataset map(ResultSet r, StatementContext ctx) throws SQLException {
      Dataset dataset = new Dataset();
      dataset.setDataSetId(r.getInt("dataset_id"));

      if (hasColumn(r, "dac_id")) {
          dataset.setDacId(r.getInt("dac_id"));
      }

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
      if (hasColumn(r, "dac_approval")) {
        String boolString = r.getString("dac_approval");
        Boolean value = Objects.isNull(boolString) ? null : r.getBoolean("dac_approval");
        dataset.setDacApproval(value);
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
      dataset.setActive(r.getBoolean("active"));
      dataset.setAlias(r.getInt("alias"));

      return dataset;
  }
}
