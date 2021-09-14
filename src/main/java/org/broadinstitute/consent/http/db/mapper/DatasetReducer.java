package org.broadinstitute.consent.http.db.mapper;

import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.DataUse;

import org.jdbi.v3.core.result.LinkedHashMapRowReducer;
import org.jdbi.v3.core.result.RowView;
import java.util.Objects;

import java.util.Map;

public class DatasetReducer implements LinkedHashMapRowReducer<Integer, DataSet> {
  @Override
  public void accumulate(Map<Integer, DataSet> map, RowView rowView) {
    DataSet dataset = map.computeIfAbsent(
      rowView.getColumn("datasetid", Integer.class),
      id -> rowView.getRow(DataSet.class));
    
      if(Objects.nonNull(rowView.getColumn("datause", String.class))) {
        dataset.setDataUse(
          DataUse.parseDataUse(
            rowView.getColumn("datause", String.class)
          ).orElse(null)
        );
      }
  }
}
