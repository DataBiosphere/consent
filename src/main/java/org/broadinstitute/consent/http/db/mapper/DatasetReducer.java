package org.broadinstitute.consent.http.db.mapper;

import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.Dataset;
import org.jdbi.v3.core.result.LinkedHashMapRowReducer;
import org.jdbi.v3.core.result.RowView;

import java.util.Map;
import java.util.Objects;

public class DatasetReducer implements LinkedHashMapRowReducer<Integer, Dataset> {
  @Override
  public void accumulate(Map<Integer, Dataset> map, RowView rowView) {
    Dataset dataset =
        map.computeIfAbsent(
            rowView.getColumn("datasetid", Integer.class), id -> rowView.getRow(Dataset.class));

    if (Objects.nonNull(rowView.getColumn("datause", String.class))) {
      dataset.setDataUse(
          DataUse.parseDataUse(rowView.getColumn("datause", String.class)).orElse(null));
    }
    if (Objects.nonNull(rowView.getColumn("translateduserestriction", String.class))) {
      dataset.setTranslatedUseRestriction(
        rowView.getColumn("translateduserestriction", String.class)
      );
    }
  }
}
