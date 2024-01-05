package org.broadinstitute.consent.http.db.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import org.broadinstitute.consent.http.models.dto.DatasetDTO;
import org.broadinstitute.consent.http.models.dto.DatasetPropertyDTO;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

public class DatasetDTOWithPropertiesMapper implements RowMapper<DatasetDTO>, RowMapperHelper {

  private final Map<Integer, DatasetDTO> datasetDTOs = new LinkedHashMap<>();
  private static final String PROPERTY_KEY = "key";
  private static final String PROPERTY_PROPERTYVALUE = "property_value";
  private final DataUseParser dataUseParser = new DataUseParser();

  public DatasetDTO map(ResultSet r, StatementContext ctx) throws SQLException {

    DatasetDTO datasetDTO;
    Integer dataSetId = r.getInt("dataset_id");
    Integer alias = r.getInt("alias");
    if (!datasetDTOs.containsKey(dataSetId)) {
      datasetDTO = new DatasetDTO(new ArrayList<>());
      if (hasColumn(r, "dac_id")) {
        int dacId = r.getInt("dac_id");
        if (dacId > 0) {
          datasetDTO.setDacId(dacId);
        }
      }
      datasetDTO.setAlias(alias);
      datasetDTO.setDataSetId(dataSetId);
      if (hasColumn(r, "data_use")) {
        datasetDTO.setDataUse(dataUseParser.parseDataUse(r.getString("data_use")));
      }
      if (hasColumn(r, "create_date")) {
        datasetDTO.setCreateDate(r.getDate("create_date"));
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
      datasetDTO.setObjectId(r.getString("object_id"));
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
