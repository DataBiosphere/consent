package org.broadinstitute.consent.http.db.mapper;

import java.sql.Timestamp;
import java.util.Map;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataAccessAgreement;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.FileStorageObject;
import org.broadinstitute.consent.http.util.ConsentLogger;
import org.jdbi.v3.core.mapper.MappingException;
import org.jdbi.v3.core.result.LinkedHashMapRowReducer;
import org.jdbi.v3.core.result.RowView;

public class DacReducer implements LinkedHashMapRowReducer<Integer, Dac>,
    ConsentLogger, RowMapperHelper {

  private final DataUseParser dataUseParser = new DataUseParser();

  @Override
  public void accumulate(Map<Integer, Dac> container, RowView rowView) {

    try {

      Dac dac =
          container.computeIfAbsent(
              rowView.getColumn("dac_id", Integer.class), id -> rowView.getRow(Dac.class));

      if (hasColumn(rowView, "daa_daa_id", Integer.class)
          && rowView.getColumn("daa_daa_id", Integer.class) > 0) {
        DataAccessAgreement daa = new DataAccessAgreement();

        if (rowView.getColumn("daa_daa_id", Integer.class) != null) {
          daa = rowView.getRow(DataAccessAgreement.class);
        }

        if (hasNonZeroColumn(rowView, "fso_file_storage_object_id")) {
          FileStorageObject fso = rowView.getRow(FileStorageObject.class);
          daa.setFile(fso);
        }

        if (daa.getDaaId() != null) {
          dac.setAssociatedDaa(daa);
        }

      }

      if (hasColumn(rowView, "dataset_id", Integer.class)
          && rowView.getColumn("dataset_id", Integer.class) > 0) {
        Dataset dataset = rowView.getRow(Dataset.class);

        //aliased columns must be set directly
        String dsAlias = rowView.getColumn("dataset_alias", String.class);
        if (dsAlias != null) {
          try {
            dataset.setAlias(Integer.parseInt(dsAlias));
          } catch (Exception e) {
            logException("Exception parsing dataset alias: " + dsAlias, e);
          }
        }

        Timestamp createDate = rowView.getColumn("dataset_create_date", Timestamp.class);
        if (createDate != null) {
          dataset.setCreateDate(createDate);
        }

        Timestamp updateDate = rowView.getColumn("dataset_update_date", Timestamp.class);
        if (updateDate != null) {
          dataset.setUpdateDate(updateDate);
        }

        String duStr = rowView.getColumn("dataset_data_use", String.class);
        if (duStr != null) {
          dataset.setDataUse(dataUseParser.parseDataUse(duStr));
        }

        if (dataset != null) {
          dac.addDataset(dataset);
        }

      }


    } catch (MappingException e) {
      logWarn(e.getMessage());
    }
  }
}
