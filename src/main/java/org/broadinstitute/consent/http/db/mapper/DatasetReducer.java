package org.broadinstitute.consent.http.db.mapper;

import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetProperty;
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
    if (Objects.nonNull(rowView.getColumn("deletable", Boolean.class))) {
      dataset.setDeletable(rowView.getColumn("deletable", Boolean.class));
    }
    if (Objects.nonNull(rowView.getColumn("key", String.class)) &&
      Objects.nonNull(rowView.getColumn("propertyvalue", String.class)) &&
      Objects.nonNull(rowView.getColumn("propertykey", Integer.class)) &&
      Objects.nonNull(rowView.getColumn("propertyid", Integer.class))
      ) {
        String key = rowView.getColumn("key", String.class);
        String propVal =  rowView.getColumn("propertyvalue", String.class);
        Integer propKey = rowView.getColumn("propertykey", Integer.class);
        Integer propId = rowView.getColumn("propertyid", Integer.class);
        DatasetProperty prop = new DatasetProperty();
        prop.setDataSetId(dataset.getDataSetId());
        prop.setPropertyId(propId);
        prop.setPropertyValue(propVal);
        prop.setPropertyKey(propKey);
        prop.setPropertyKeyName(key);
        prop.setCreateDate(dataset.getCreateDate());
        dataset.addProperty(prop);
    }
  }
}
