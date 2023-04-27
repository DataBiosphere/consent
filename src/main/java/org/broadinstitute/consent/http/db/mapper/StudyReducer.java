package org.broadinstitute.consent.http.db.mapper;

import org.broadinstitute.consent.http.enumeration.PropertyType;
import org.broadinstitute.consent.http.models.FileStorageObject;
import org.broadinstitute.consent.http.models.Study;
import org.broadinstitute.consent.http.models.StudyProperty;
import org.jdbi.v3.core.result.LinkedHashMapRowReducer;
import org.jdbi.v3.core.result.RowView;

import java.util.Map;
import java.util.Objects;

public class StudyReducer implements LinkedHashMapRowReducer<Integer, Study>, RowMapperHelper {

  @Override
  public void accumulate(Map<Integer, Study> map, RowView rowView) {
    Study study =
            map.computeIfAbsent(
                    rowView.getColumn("study_id", Integer.class), id -> rowView.getRow(Study.class));

    reduceStudy(study, rowView);
  }


  public void reduceStudy(Study study, RowView rowView) {
    if (hasColumn(rowView, "sp_key", String.class)
            && hasColumn(rowView, "sp_property_value", String.class)) {
      String keyName = rowView.getColumn("sp_key", String.class);
      String propVal = rowView.getColumn("sp_value", String.class);
      PropertyType propType = PropertyType.String;
      if (hasColumn(rowView, "sp_property_type", String.class)) {
        propType = PropertyType.parse(rowView.getColumn("sp_property_type", String.class));
      }

      if (Objects.nonNull(keyName) && Objects.nonNull(propVal)) {
        try {
          StudyProperty prop = new StudyProperty();
          prop.setStudyId(study.getStudyId());
          prop.setValue(propType.coerce(propVal));
          prop.setName(keyName);
          prop.setType(propType);

          study.addProperty(prop);
        } catch (Exception e) {
          // do nothing.
        }
      }
    }

    if (hasColumn(rowView, "fso_file_storage_object_id", Integer.class)
            && Objects.nonNull(rowView.getColumn("fso_file_storage_object_id", Integer.class))
    ) {
      FileStorageObject fileStorageObject = rowView.getRow(FileStorageObject.class);

      switch (fileStorageObject.getCategory()) {
        case ALTERNATIVE_DATA_SHARING_PLAN -> {
          if (isFileNewer(fileStorageObject, study.getAlternativeDataSharingPlan())) {
            study.setAlternativeDataSharingPlan(fileStorageObject);
          }
        }
        default -> {
        }
      }
    }
  }

  private boolean isFileNewer(FileStorageObject incomingFile, FileStorageObject existingFile) {
    return Objects.isNull(existingFile) || incomingFile.getLatestUpdateDate().isAfter(existingFile.getLatestUpdateDate());
  }
}
