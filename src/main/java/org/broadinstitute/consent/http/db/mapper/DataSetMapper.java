package org.broadinstitute.consent.http.db.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.broadinstitute.consent.http.models.DataSet;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

public class DataSetMapper implements RowMapper<DataSet>, RowMapperHelper {

  public DataSet map(ResultSet r, StatementContext ctx) throws SQLException {
      DataSet dataset = new DataSet();
      dataset.setDataSetId(r.getInt("dataSetId"));
      dataset.setObjectId(r.getString("objectId"));
      dataset.setName(r.getString("name"));
      if (hasColumn(r, "createdate")) {
          dataset.setCreateDate(r.getDate("createdate"));
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
      dataset.setActive(r.getBoolean("active"));
      dataset.setAlias(r.getInt("alias"));

      return dataset;
  }
}
