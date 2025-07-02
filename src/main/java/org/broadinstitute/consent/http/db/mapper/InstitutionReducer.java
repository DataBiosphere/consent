package org.broadinstitute.consent.http.db.mapper;

import java.util.Map;
import org.broadinstitute.consent.http.models.Institution;
import org.jdbi.v3.core.result.LinkedHashMapRowReducer;
import org.jdbi.v3.core.result.RowView;

public class InstitutionReducer implements LinkedHashMapRowReducer<Integer, Institution>,
    RowMapperHelper {

  @Override
  public void accumulate(Map<Integer, Institution> map, RowView rowView) {
    Institution institution = map.computeIfAbsent(
        rowView.getColumn("institution_id", Integer.class),
        id -> rowView.getRow(Institution.class));
    institution.addDomain(rowView.getColumn("domain", String.class));
  }
}
