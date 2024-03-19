package org.broadinstitute.consent.http.db.mapper;

import java.util.Map;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataAccessAgreement;
import org.broadinstitute.consent.http.models.FileStorageObject;
import org.broadinstitute.consent.http.util.ConsentLogger;
import org.jdbi.v3.core.result.LinkedHashMapRowReducer;
import org.jdbi.v3.core.result.RowView;

public class DataAccessAgreementReducer
    implements LinkedHashMapRowReducer<Integer, DataAccessAgreement>, RowMapperHelper,
    ConsentLogger {

  @Override
  public void accumulate(Map<Integer, DataAccessAgreement> map, RowView rowView) {
    var daaId = hasColumn(rowView, "daa_id", Integer.class) ?
        rowView.getColumn("daa_id", Integer.class) :
        rowView.getColumn("daa_daa_id", Integer.class);
      map.computeIfAbsent(
          daaId,
          id -> rowView.getRow(DataAccessAgreement.class)
      );
      if (hasColumn(rowView, "file_storage_object_id", String.class)) {
        FileStorageObject fso = rowView.getRow(FileStorageObject.class);
        map.get(daaId).setFile(fso);
      }
      if (hasColumn(rowView, "dac_id", Integer.class)) {
        map.get(daaId).addDac(rowView.getRow(Dac.class));
      }
  }
}
