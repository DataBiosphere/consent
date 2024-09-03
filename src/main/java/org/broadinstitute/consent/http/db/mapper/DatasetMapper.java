package org.broadinstitute.consent.http.db.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import org.broadinstitute.consent.http.models.Dataset;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

public class DatasetMapper implements RowMapper<Dataset>, RowMapperHelper {

  private final DataUseParser dataUseParser = new DataUseParser();

  public Dataset map(ResultSet r, StatementContext ctx) throws SQLException {
    Dataset dataset = new Dataset();
    dataset.setDataSetId(r.getInt("dataset_id"));

    if (hasNonZeroColumn(r, "dac_id")) {
      dataset.setDacId(r.getInt("dac_id"));
    }

    dataset.setObjectId(r.getString("object_id"));
    dataset.setName(r.getString("name"));
    if (hasColumn(r, "create_date")) {
      dataset.setCreateDate(r.getDate("create_date"));
    }
    if (hasNonZeroColumn(r, "create_user_id")) {
      dataset.setCreateUserId(r.getInt("create_user_id"));
    }
    if (hasColumn(r, "update_date")) {
      dataset.setUpdateDate(r.getTimestamp("update_date"));
    }
    if (hasColumn(r, "dac_approval")) {
      String boolString = r.getString("dac_approval");
      Boolean value = Objects.isNull(boolString) ? null : r.getBoolean("dac_approval");
      dataset.setDacApproval(value);
    }
    if (hasNonZeroColumn(r, "update_user_id")) {
      dataset.setUpdateUserId(r.getInt("update_user_id"));
    }
    if (hasColumn(r, "data_use")) {
      dataset.setDataUse(dataUseParser.parseDataUse(r.getString("data_use")));
    }
    if (hasColumn(r, "translated_data_use")) {
      dataset.setTranslatedDataUse(r.getString("translated_data_use"));
    }
    if (hasColumn(r, "alias")) {
      dataset.setAlias(r.getInt("alias"));
    }

    return dataset;
  }
}
