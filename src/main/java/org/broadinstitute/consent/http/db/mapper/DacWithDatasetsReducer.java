package org.broadinstitute.consent.http.db.mapper;

import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.dto.DataSetDTO;
import org.jdbi.v3.core.mapper.MappingException;
import org.jdbi.v3.core.result.LinkedHashMapRowReducer;
import org.jdbi.v3.core.result.RowView;

import java.util.Map;
import java.util.Objects;

public class DacWithDatasetsReducer implements LinkedHashMapRowReducer<Integer, Dac> {

  @Override
  public void accumulate(Map<Integer, Dac> container, RowView rowView) {
    Dac dac =
        container.computeIfAbsent(
            rowView.getColumn("dac_id", Integer.class), id -> rowView.getRow(Dac.class));
    try {
      if (Objects.nonNull(rowView.getColumn("datasetid", Integer.class))) {
        DataSetDTO dto = rowView.getRow(DataSetDTO.class);
    
        if (Objects.nonNull(dto)) {
          dac.addDatasetDTO(dto);
        }
      }
    } catch (MappingException e) {
      // Ignore any attempt to map a column that doesn't exist
    }
  }
}
