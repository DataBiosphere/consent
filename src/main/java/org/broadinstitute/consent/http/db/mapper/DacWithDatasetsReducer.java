package org.broadinstitute.consent.http.db.mapper;

import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.dto.DataSetDTO;
import org.broadinstitute.consent.http.models.dto.DataSetPropertyDTO;
import org.jdbi.v3.core.mapper.MappingException;
import org.jdbi.v3.core.result.LinkedHashMapRowReducer;
import org.jdbi.v3.core.result.RowView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        DataSetDTO dto = rowView.getRow(DataSetDTO.class);
        try {
          if (Objects.nonNull(rowView.getColumn("consent_data_use", String.class))) {
            String duStr = rowView.getColumn("consent_data_use", String.class);
            Optional<DataUse> du = DataUse.parseDataUse(duStr);
            du.ifPresent(dto::setDataUse);
          }
        } catch (MappingException e) {
          logger.warn(e.getMessage());
        }
        try {
          if (Objects.nonNull(rowView.getColumn("dataset_alias", String.class))) {
            String dsAlias = rowView.getColumn("dataset_alias", String.class);
            try {
              dto.setAlias(Integer.parseInt(dsAlias));
            } catch (Exception e) {
              logger.error("Exception parsing dataset alias: " + dsAlias, e);
            }
          }
        } catch (MappingException e) {
            logger.warn(e.getMessage());
        }
        try {
          if (Objects.nonNull(rowView.getColumn("propertyname", String.class))
              && Objects.nonNull(rowView.getColumn("propertyvalue", String.class))) {
            DataSetPropertyDTO propDTO =
                new DataSetPropertyDTO(
                    rowView.getColumn("propertyname", String.class),
                    rowView.getColumn("propertyvalue", String.class));
            dto.addProperty(propDTO);
          }
        } catch (MappingException e) {
            logger.warn(e.getMessage());
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
