package org.broadinstitute.consent.http.db.mapper;

import java.util.Map;
import java.util.Objects;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataAccessAgreement;
import org.broadinstitute.consent.http.models.FileStorageObject;
import org.broadinstitute.consent.http.util.ConsentLogger;
import org.jdbi.v3.core.mapper.MappingException;
import org.jdbi.v3.core.result.LinkedHashMapRowReducer;
import org.jdbi.v3.core.result.RowView;

public class DacWithDaasReducer implements LinkedHashMapRowReducer<Integer, Dac>,
    ConsentLogger {


  @Override
  public void accumulate(Map<Integer, Dac> container, RowView rowView) {

    try {

      Dac dac =
          container.computeIfAbsent(
              rowView.getColumn("dac_id", Integer.class), id -> rowView.getRow(Dac.class));

      if (Objects.nonNull(rowView.getColumn("daa_daa_id", Integer.class))) {
        DataAccessAgreement daa = new DataAccessAgreement();

        try {
          if (dac != null && rowView.getColumn("daa_daa_id", Integer.class) != null) {
            daa = rowView.getRow(DataAccessAgreement.class);
          }
        } catch (MappingException e) {
        }

        try {
          if (dac != null && rowView.getColumn("file_storage_object_id", String.class) != null) {
            FileStorageObject fso = rowView.getRow(FileStorageObject.class);
            daa.setFile(fso);
          }
        } catch (MappingException e) {
        }

        if (daa.getDaaId() != null) {
          dac.setAssociatedDaa(daa);
        }

      }
    } catch (MappingException e) {
      logWarn(e.getMessage());
    }
  }
}
