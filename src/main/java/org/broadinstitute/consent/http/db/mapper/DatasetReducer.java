package org.broadinstitute.consent.http.db.mapper;

import org.broadinstitute.consent.http.enumeration.DatasetPropertyType;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetProperty;
import org.broadinstitute.consent.http.service.DatasetService;
import org.jdbi.v3.core.result.LinkedHashMapRowReducer;
import org.jdbi.v3.core.result.RowView;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class DatasetReducer implements LinkedHashMapRowReducer<Integer, Dataset>, RowMapperHelper {

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
      dataset.setDataUse(
          DataUse.parseDataUse(rowView.getColumn("datause", String.class)).orElse(null));
    }
    if (hasColumn(rowView, "translateduserestriction", String.class)) {
      dataset.setTranslatedUseRestriction(
          rowView.getColumn("translateduserestriction", String.class));
    }
    if (hasColumn(rowView, "in_use", Integer.class)) {
      Integer dsIdInUse = rowView.getColumn("in_use", Integer.class);
      dataset.setDeletable(Objects.isNull(dsIdInUse));
    }
    if (hasColumn(rowView, "key", String.class)
        && hasColumn(rowView, "propertyvalue", String.class)) {
      String keyName = rowView.getColumn("key", String.class);
      String propVal = rowView.getColumn("propertyvalue", String.class);
      DatasetPropertyType propType = DatasetPropertyType.String;
      if (hasColumn(rowView, "propertytype", String.class)) {
          propType = DatasetPropertyType.parse(rowView.getColumn("propertytype", String.class));
      }

      if (Objects.nonNull(keyName) && Objects.nonNull(propVal)) {
        try {
          DatasetProperty prop = new DatasetProperty();
          prop.setDataSetId(dataset.getDataSetId());
          prop.setPropertyValue(propType.coerce(propVal));
          prop.setPropertyName(keyName);
          prop.setPropertyType(propType);
          dataset.addProperty(prop);
        } catch (Exception e) {
          // do nothing.
        }
      }
    }
    // The name property doesn't always come through, add it manually:
    Optional<DatasetProperty> nameProp =
      Objects.isNull(dataset.getProperties()) ?
        Optional.empty() :
        dataset.getProperties()
          .stream()
          .filter(p -> Objects.nonNull(p.getPropertyName()))
          .filter(p -> p.getPropertyName().equals(DatasetService.DATASET_NAME_KEY))
          .findFirst();
    if (nameProp.isEmpty()) {
      DatasetProperty name = new DatasetProperty();
      name.setPropertyName(DatasetService.DATASET_NAME_KEY);
      name.setPropertyValue(dataset.getName());
      name.setDataSetId(dataset.getDataSetId());
      dataset.addProperty(name);
    }
    dataset.setDatasetName(dataset.getName());
    dataset.setDatasetIdentifier();
  }
}
