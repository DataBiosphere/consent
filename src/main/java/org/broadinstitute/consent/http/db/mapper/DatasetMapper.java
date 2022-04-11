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
      dataset.setDataSetId(r.getInt("dataSetId"));
      dataset.setObjectId(r.getString("objectId"));
      dataset.setName(r.getString("name"));
      if (hasColumn(r, "dac_id")) {
        int dacId = r.getInt("dac_id");
        if (dacId > 0) {
          dataset.setDacId(dacId);
        }
      }
      if (hasColumn(r, "consentid")) {
        String consentId = r.getString("consentid");
      if (Objects.nonNull(consentId)) {
          dataset.setConsentId(consentId);
        }
      }
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
      if(hasColumn(r, "dataUse")) {
        dataset.setDataUse(DataUse.parseDataUse(r.getString("datause")).orElse(null));
      }
      dataset.setActive(r.getBoolean("active"));
      dataset.setAlias(r.getInt("alias"));

      return dataset;
  }
}
