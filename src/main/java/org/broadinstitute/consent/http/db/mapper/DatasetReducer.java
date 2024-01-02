package org.broadinstitute.consent.http.db.mapper;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.broadinstitute.consent.http.enumeration.PropertyType;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetProperty;
import org.broadinstitute.consent.http.models.FileStorageObject;
import org.broadinstitute.consent.http.models.Study;
import org.broadinstitute.consent.http.models.User;
import org.jdbi.v3.core.result.LinkedHashMapRowReducer;
import org.jdbi.v3.core.result.RowView;

public class DatasetReducer implements LinkedHashMapRowReducer<Integer, Dataset>, RowMapperHelper {

  private final DataUseParser dataUseParser = new DataUseParser();

  @Override
  public void accumulate(Map<Integer, Dataset> map, RowView rowView) {
    Dataset dataset =
        map.computeIfAbsent(
            rowView.getColumn("dataset_id", Integer.class), id -> rowView.getRow(Dataset.class));
    if (hasColumn(rowView, "dac_id", Integer.class)) {
      dataset.setDacId(rowView.getColumn("dac_id", Integer.class));
    }
    if (hasColumn(rowView, "data_use", String.class)) {
      String dataUseString = rowView.getColumn("data_use", String.class);
      Optional<DataUse> dataUseOptional = Optional.ofNullable(
          dataUseParser.parseDataUse(dataUseString));
      dataUseOptional.ifPresent(dataset::setDataUse);
    }
    hasOptionalColumn(rowView, "translated_data_use", String.class)
        .ifPresent(dataset::setTranslatedDataUse);
    if (hasColumn(rowView, "in_use", Integer.class)) {
      Integer dsIdInUse = rowView.getColumn("in_use", Integer.class);
      dataset.setDeletable(Objects.isNull(dsIdInUse));
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
          if (hasColumn(rowView, "property_id", Integer.class)) {
            prop.setPropertyId(rowView.getColumn("property_id", Integer.class));
          }
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

    dataset.setDatasetName(dataset.getName());
    dataset.setDatasetIdentifier();
  }

  private boolean isFileNewer(FileStorageObject incomingFile, FileStorageObject existingFile) {
    return Objects.isNull(existingFile) || incomingFile.getLatestUpdateDate()
        .isAfter(existingFile.getLatestUpdateDate());
  }
}
