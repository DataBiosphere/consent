package org.broadinstitute.consent.http.db.mapper;

import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.dto.DatasetDTO;
import org.jdbi.v3.core.mapper.MappingException;
import org.jdbi.v3.core.result.LinkedHashMapRowReducer;
import org.jdbi.v3.core.result.RowView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class DacWithDatasetsReducer implements LinkedHashMapRowReducer<Integer, Dac> {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Override
  public void accumulate(Map<Integer, Dac> container, RowView rowView) {

    try {

      Dac dac =
          container.computeIfAbsent(
              rowView.getColumn("dac_id", Integer.class), id -> rowView.getRow(Dac.class));

      if (Objects.nonNull(rowView.getColumn("datasetid", Integer.class))) {
        DatasetDTO dto = rowView.getRow(DatasetDTO.class);

        try { 
          //aliased columns must be set directly 
          
          if (Objects.nonNull(rowView.getColumn("dataset_alias", String.class))) {
            String dsAlias = rowView.getColumn("dataset_alias", String.class);
            try {
              dto.setAlias(Integer.parseInt(dsAlias));
            } catch (Exception e) {
              logger.error("Exception parsing dataset alias: " + dsAlias, e);
            }
          }

          if (Objects.nonNull(rowView.getColumn("dataset_create_date", Date.class))) {
            Date createDate = rowView.getColumn("dataset_create_date", Date.class);
            dto.setCreateDate(createDate);
          }

          if (Objects.nonNull(rowView.getColumn("dataset_update_date", Timestamp.class))) {
            Timestamp updateDate = rowView.getColumn("dataset_update_date", Timestamp.class);
            dto.setUpdateDate(updateDate);
          }

        } catch (Exception e) {
          //no values for these columns
        }
        
        if (Objects.nonNull(rowView.getColumn("consent_data_use", String.class))) {
          String duStr = rowView.getColumn("consent_data_use", String.class);
          Optional<DataUse> du = DataUse.parseDataUse(duStr);
          du.ifPresent(dto::setDataUse);
        }

        if (Objects.nonNull(dto)) {
          dac.addDatasetDTO(dto);
        }

      }
    } catch (MappingException e) {
        logger.warn(e.getMessage());
    }
  }
}
