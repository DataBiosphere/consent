package org.broadinstitute.consent.http.db.mapper;

import java.util.Map;
import java.util.Objects;
import org.broadinstitute.consent.http.enumeration.PropertyType;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.models.Study;
import org.broadinstitute.consent.http.models.StudyProperty;
import org.jdbi.v3.core.result.LinkedHashMapRowReducer;
import org.jdbi.v3.core.result.RowView;

public class StudyReducer implements LinkedHashMapRowReducer<Integer, Study>, RowMapperHelper {

  @Override
  public void accumulate(Map<Integer, Study> map, RowView rowView) {
    Study study =
        map.computeIfAbsent(
            rowView.getColumn("study_id", Integer.class), id -> rowView.getRow(Study.class));

    reduceStudy(study, rowView);
  }

  public void reduceStudy(Study study, RowView rowView) {

    if (study.getPiInstitution() == null && hasNonZeroColumn(rowView, "pi_institution_id")) {
      Institution institution = new Institution();
      institution.setId(rowView.getColumn("pi_institution_id", Integer.class));
      // Not every query that selects the study's columns joins in the institution name.
      institution.setName(
          hasOptionalColumn(rowView, "pi_institution_name", String.class).orElse(null));
      study.setPiInstitution(institution);
    }

    if (hasNonZeroColumn(rowView, "sp_study_property_id")) {
      Integer studyPropertyId = rowView.getColumn("sp_study_property_id", Integer.class);
      String keyName = rowView.getColumn("sp_key", String.class);
      String propVal = rowView.getColumn("sp_value", String.class);
      Integer studyId = rowView.getColumn("sp_study_id", Integer.class);
      PropertyType propType = PropertyType.String;
      if (hasColumn(rowView, "sp_type", String.class)) {
        propType = PropertyType.parse(rowView.getColumn("sp_type", String.class));
      }

      if (Objects.nonNull(keyName) && Objects.nonNull(propVal)) {
        try {
          StudyProperty prop = new StudyProperty();
          prop.setStudyPropertyId(studyPropertyId);
          prop.setStudyId(studyId);
          prop.setValue(propType.coerce(propVal));
          prop.setKey(keyName);
          prop.setType(propType);

          study.addProperty(prop);
        } catch (Exception e) {
          // do nothing.
        }
      }
    }
  }
}
