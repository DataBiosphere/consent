package org.broadinstitute.consent.http.db.mapper;

import org.broadinstitute.consent.http.enumeration.PropertyType;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetProperty;
import org.broadinstitute.consent.http.models.FileStorageObject;
import org.broadinstitute.consent.http.models.Study;
import org.broadinstitute.consent.http.models.User;
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
            rowView.getColumn("dataset_id", Integer.class), id -> rowView.getRow(Dataset.class));
    if (hasColumn(rowView, "dac_id", Integer.class)) {
      dataset.setDacId(rowView.getColumn("dac_id", Integer.class));
    }
    if (hasColumn(rowView, "consent_id", String.class)) {
      dataset.setConsentId(rowView.getColumn("consent_id", String.class));
    }
    if (hasColumn(rowView, "data_use", String.class)) {
      dataset.setDataUse(
          DataUse.parseDataUse(rowView.getColumn("data_use", String.class)).orElse(null));
    }
    if (hasColumn(rowView, "translated_use_restriction", String.class)) {
      dataset.setTranslatedUseRestriction(
          rowView.getColumn("translated_use_restriction", String.class));
    }
    if (hasColumn(rowView, "in_use", Integer.class)) {
      Integer dsIdInUse = rowView.getColumn("in_use", Integer.class);
      dataset.setDeletable(Objects.isNull(dsIdInUse));
    }
    if (hasColumn(rowView, "needs_approval", Boolean.class)) {
      Boolean needsApproval = rowView.getColumn("needs_approval", Boolean.class);
      dataset.setNeedsApproval(needsApproval);
    }
    if (hasColumn(rowView, "dac_approval", Boolean.class)) {
      dataset.setDacApproval(rowView.getColumn("dac_approval", Boolean.class));
    }
    if (hasColumn(rowView, "key", String.class)
        && hasColumn(rowView, "property_value", String.class)) {
      String keyName = rowView.getColumn("key", String.class);
      String propVal = rowView.getColumn("property_value", String.class);
      PropertyType propType = PropertyType.String;
      if (hasColumn(rowView, "property_type", String.class)) {
          propType = PropertyType.parse(rowView.getColumn("property_type", String.class));
      }

      if (Objects.nonNull(keyName) && Objects.nonNull(propVal)) {
        try {
          DatasetProperty prop = new DatasetProperty();
          prop.setDataSetId(dataset.getDataSetId());
          prop.setPropertyValue(propType.coerce(propVal));
          prop.setPropertyName(keyName);
          prop.setPropertyType(propType);
          if (hasColumn(rowView, "schema_property", String.class)) {
            prop.setSchemaProperty(rowView.getColumn("schema_property", String.class));
          }
          dataset.addProperty(prop);
        } catch (Exception e) {
          // do nothing.
        }
      }
    }

    if (hasColumn(rowView, "s_study_id", Integer.class)) {
      if (Objects.isNull(dataset.getStudy())) {
        dataset.setStudy(rowView.getRow(Study.class));
      }
      new StudyReducer().reduceStudy(dataset.getStudy(), rowView);
    }

    if (hasColumn(rowView, "fso_file_storage_object_id", Integer.class)
      && Objects.nonNull(rowView.getColumn("fso_file_storage_object_id", Integer.class))
    ) {
      FileStorageObject fileStorageObject = rowView.getRow(FileStorageObject.class);

      switch (fileStorageObject.getCategory()) {
        case NIH_INSTITUTIONAL_CERTIFICATION -> {
          if (isFileNewer(fileStorageObject, dataset.getNihInstitutionalCertificationFile())) {
            dataset.setNihInstitutionalCertificationFile(fileStorageObject);
          }
        }
        default -> {
        }
      }
    }

    if (hasColumn(rowView, "u_user_id", Integer.class)) {
      User user = rowView.getRow(User.class);
      dataset.setCreateUser(user);
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
      name.setPropertyType(PropertyType.String);
      dataset.addProperty(name);
    }
    dataset.setDatasetName(dataset.getName());
    dataset.setDatasetIdentifier();
  }

  private boolean isFileNewer(FileStorageObject incomingFile, FileStorageObject existingFile) {
    return Objects.isNull(existingFile) || incomingFile.getLatestUpdateDate().isAfter(existingFile.getLatestUpdateDate());
  }
}
