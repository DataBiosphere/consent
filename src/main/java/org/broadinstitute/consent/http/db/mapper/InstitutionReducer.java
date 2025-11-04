package org.broadinstitute.consent.http.db.mapper;

import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.consent.http.models.Institution;
import org.jdbi.v3.core.result.LinkedHashMapRowReducer;
import org.jdbi.v3.core.result.RowView;

public class InstitutionReducer
    implements LinkedHashMapRowReducer<Integer, Institution>, RowMapperHelper {

  @Override
  public void accumulate(Map<Integer, Institution> map, RowView rowView) {
    Institution institution =
        map.computeIfAbsent(
            rowView.getColumn("institution_id", Integer.class),
            id -> rowView.getRow(Institution.class));
    if (hasColumn(rowView, "domain", String.class)) {
      String domain = rowView.getColumn("domain", String.class);
      if (!StringUtils.isBlank(domain)) {
        institution.addDomain(domain);
      }
    }
  }
}
