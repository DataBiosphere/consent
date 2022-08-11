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

public class DatasetDTOWithPropertiesMapper implements RowMapper<DatasetDTO>, RowMapperHelper {

  private final Map<Integer, DatasetDTO> datasetDTOs = new LinkedHashMap<>();
  private static final String PROPERTY_KEY = "key";
  private static final String PROPERTY_PROPERTYVALUE = "propertyValue";

  public DatasetDTO map(ResultSet r, StatementContext ctx) throws SQLException {

    DatasetDTO datasetDTO;
    Integer dataSetId = r.getInt("dataSetId");
    String consentId = r.getString("consentId");
    Integer alias = r.getInt("alias");
    if (!datasetDTOs.containsKey(dataSetId)) {
      datasetDTO = new DatasetDTO(new ArrayList<>());
      if (hasColumn(r, "dac_id")) {
        int dacId = r.getInt("dac_id");
        if (dacId > 0) {
          datasetDTO.setDacId(dacId);
        }
      }
      datasetDTO.setConsentId(consentId);
      datasetDTO.setAlias(alias);
      datasetDTO.setDataSetId(dataSetId);
      datasetDTO.setActive(r.getBoolean("active"));
      datasetDTO.setTranslatedUseRestriction(r.getString("translatedUseRestriction"));
      // Consents store DataUse in `datause` while Datasets store it in `data_use`. Capture both cases for safety.
      if (hasColumn(r, "datause")) {
        datasetDTO.setDataUse(DataUse.parseDataUse(r.getString("datause")).orElse(null));
      }
      if (hasColumn(r, "data_use")) {
        datasetDTO.setDataUse(DataUse.parseDataUse(r.getString("data_use")).orElse(null));
      }
      if (hasColumn(r, "createdate")) {
          datasetDTO.setCreateDate(r.getDate("createdate"));
      }
      if (hasColumn(r, "create_user_id")) {
          int userId = r.getInt("create_user_id");
          if (userId > 0) {
              datasetDTO.setCreateUserId(userId);
          }
      }
      if (hasColumn(r, "update_date")) {
          datasetDTO.setUpdateDate(r.getTimestamp("update_date"));
      }
      if (hasColumn(r, "update_user_id")) {
          int userId = r.getInt("update_user_id");
          if (userId > 0) {
              datasetDTO.setUpdateUserId(userId);
          }
      }
      DatasetPropertyDTO property = new DatasetPropertyDTO("Dataset Name", r.getString("name"));
      datasetDTO.addProperty(property);
      property =
          new DatasetPropertyDTO(r.getString(PROPERTY_KEY), r.getString(PROPERTY_PROPERTYVALUE));
      if (property.getPropertyName() != null) {
        datasetDTO.addProperty(property);
      }
      datasetDTO.setNeedsApproval(r.getBoolean("needs_approval"));
      datasetDTO.setObjectId(r.getString("objectId"));
      datasetDTOs.put(dataSetId, datasetDTO);
    } else {
      datasetDTO = datasetDTOs.get(dataSetId);
      DatasetPropertyDTO property =
          new DatasetPropertyDTO(r.getString(PROPERTY_KEY), r.getString(PROPERTY_PROPERTYVALUE));
      if (property.getPropertyName() != null) {
        datasetDTO.addProperty(property);
      }
    }
    return datasetDTO;
  }
}
