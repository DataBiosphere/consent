package org.broadinstitute.consent.http.db.mapper;

import java.util.Map;
import org.broadinstitute.consent.http.models.DataAccessAgreement;
import org.jdbi.v3.core.result.LinkedHashMapRowReducer;
import org.jdbi.v3.core.result.RowView;

public class DataAccessAgreementReducer
    implements LinkedHashMapRowReducer<Integer, DataAccessAgreement>, RowMapperHelper {

  // idk if this is right?
  @Override
  public void accumulate(Map<Integer, DataAccessAgreement> map, RowView rowView) {
    DataAccessAgreement daa =
        map.computeIfAbsent(
            rowView.getColumn("id", Integer.class), id -> rowView.getRow(DataAccessAgreement.class));
    if (hasColumn(rowView, "initial_dac_id", Integer.class)) {
      daa.setInitialDacId(rowView.getColumn("initial_dac_id", Integer.class));
    }
  }
}
