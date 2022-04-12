package org.broadinstitute.consent.http.db.mapper;

import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetProperty;
import org.jdbi.v3.core.result.LinkedHashMapRowReducer;
import org.jdbi.v3.core.result.RowView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class DatasetReducer implements LinkedHashMapRowReducer<Integer, Dataset>, RowMapperHelper {

  Logger logger = LoggerFactory.getLogger(DatasetReducer.class);

  @Override
  public void accumulate(Map<Integer, Dataset> map, RowView rowView) {
    Dataset dataset =
        map.computeIfAbsent(
            rowView.getColumn("datasetid", Integer.class), id -> rowView.getRow(Dataset.class));
    if (hasColumn(rowView, "dac_id", Integer.class)) {
      dataset.setDacId(rowView.getColumn("dac_id", Integer.class));
    }
    if (hasColumn(rowView, "consentid", String.class)) {
      dataset.setConsentId(rowView.getColumn("consentid", String.class));
    }
    if (hasColumn(rowView, "datause", String.class)) {
      dataset.setDataUse(DataUse.parseDataUse(rowView.getColumn("datause", String.class)).orElse(null));
    }
    if (hasColumn(rowView, "translateduserestriction", String.class)) {
      dataset.setTranslatedUseRestriction(rowView.getColumn("translateduserestriction", String.class));
    }
    if (hasColumn(rowView, "deletable", Boolean.class)) {
      dataset.setDeletable(rowView.getColumn("deletable", Boolean.class));
    }
    try {
      if (hasColumn(rowView, "key", String.class)
          && hasColumn(rowView, "propertyvalue", String.class)
          && hasColumn(rowView, "propertykey", Integer.class)
          && hasColumn(rowView, "propertyid", Integer.class)) {
        String key = rowView.getColumn("key", String.class);
        String propVal = rowView.getColumn("propertyvalue", String.class);
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
    } catch (Exception e) {
      logger.warn(e.getMessage(), e);
    }
  }
}
