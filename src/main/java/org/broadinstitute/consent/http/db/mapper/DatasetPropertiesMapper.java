package org.broadinstitute.consent.http.db.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.dto.DatasetDTO;
import org.broadinstitute.consent.http.models.dto.DatasetPropertyDTO;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

public class DatasetPropertiesMapper implements RowMapper<DatasetDTO>, RowMapperHelper {

  private final Map<Integer, DatasetDTO> datasets = new LinkedHashMap<>();
  private static final String PROPERTY_KEY = "key";
  private static final String PROPERTY_PROPERTYVALUE = "propertyValue";

  public DatasetDTO map(ResultSet r, StatementContext ctx) throws SQLException {

    DatasetDTO dataSetDTO;
    Integer dataSetId = r.getInt("dataSetId");
    String consentId = r.getString("consentId");
    Integer alias = r.getInt("alias");
    if (!datasets.containsKey(dataSetId)) {
      dataSetDTO = new DatasetDTO(new ArrayList<>());
      if (hasColumn(r, "dac_id")) {
        int dacId = r.getInt("dac_id");
        if (dacId > 0) {
          dataSetDTO.setDacId(dacId);
        }
      }
      dataSetDTO.setConsentId(consentId);
      dataSetDTO.setAlias(alias);
      dataSetDTO.setDataSetId(dataSetId);
      dataSetDTO.setActive(r.getBoolean("active"));
      dataSetDTO.setTranslatedUseRestriction(r.getString("translatedUseRestriction"));
      if (hasColumn(r, "datause")) {
        dataSetDTO.setDataUse(DataUse.parseDataUse(r.getString("datause")).orElse(null));
      }
      if (hasColumn(r, "createdate")) {
          dataSetDTO.setCreateDate(r.getDate("createdate"));
      }
      if (hasColumn(r, "create_user_id")) {
          int userId = r.getInt("create_user_id");
          if (userId > 0) {
              dataSetDTO.setCreateUserId(userId);
          }
      }
      if (hasColumn(r, "update_date")) {
          dataSetDTO.setUpdateDate(r.getTimestamp("update_date"));
      }
      if (hasColumn(r, "update_user_id")) {
          int userId = r.getInt("update_user_id");
          if (userId > 0) {
              dataSetDTO.setUpdateUserId(userId);
          }
      }
      DatasetPropertyDTO property = new DatasetPropertyDTO("Dataset Name", r.getString("name"));
      dataSetDTO.addProperty(property);
      property =
          new DatasetPropertyDTO(r.getString(PROPERTY_KEY), r.getString(PROPERTY_PROPERTYVALUE));
      if (property.getPropertyName() != null) {
        dataSetDTO.addProperty(property);
      }
      dataSetDTO.setNeedsApproval(r.getBoolean("needs_approval"));
      dataSetDTO.setObjectId(r.getString("objectId"));
      datasets.put(dataSetId, dataSetDTO);
    } else {
      dataSetDTO = datasets.get(dataSetId);
      DatasetPropertyDTO property =
          new DatasetPropertyDTO(r.getString(PROPERTY_KEY), r.getString(PROPERTY_PROPERTYVALUE));
      if (property.getPropertyName() != null) {
        dataSetDTO.addProperty(property);
      }
    }
    return dataSetDTO;
  }
}
