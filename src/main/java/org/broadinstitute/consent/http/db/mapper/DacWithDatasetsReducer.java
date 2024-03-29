package org.broadinstitute.consent.http.db.mapper;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.Map;
import java.util.Objects;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.util.ConsentLogger;
import org.jdbi.v3.core.mapper.MappingException;
import org.jdbi.v3.core.result.LinkedHashMapRowReducer;
import org.jdbi.v3.core.result.RowView;

public class DacWithDatasetsReducer implements LinkedHashMapRowReducer<Integer, Dac>,
    ConsentLogger {

  private final DataUseParser dataUseParser = new DataUseParser();

  @Override
  public void accumulate(Map<Integer, Dac> container, RowView rowView) {

    try {

      Dac dac =
          container.computeIfAbsent(
              rowView.getColumn("dac_id", Integer.class), id -> rowView.getRow(Dac.class));

      if (Objects.nonNull(rowView.getColumn("dataset_id", Integer.class))) {
        Dataset dataset = rowView.getRow(Dataset.class);

        try {
          //aliased columns must be set directly
          String dsAlias = rowView.getColumn("dataset_alias", String.class);
          if (dsAlias != null) {
            try {
              dataset.setAlias(Integer.parseInt(dsAlias));
            } catch (Exception e) {
              logException("Exception parsing dataset alias: " + dsAlias, e);
            }
          }

          Date createDate = rowView.getColumn("dataset_create_date", Date.class);
          if (createDate != null) {
            dataset.setCreateDate(createDate);
          }

          Timestamp updateDate = rowView.getColumn("dataset_update_date", Timestamp.class);
          if (updateDate != null) {
            dataset.setUpdateDate(updateDate);
          }

        } catch (Exception e) {
          //no values for these columns
        }

        String duStr = rowView.getColumn("dataset_data_use", String.class);
        if (duStr != null) {
          dataset.setDataUse(dataUseParser.parseDataUse(duStr));
        }

        if (Objects.nonNull(dataset)) {
          dac.addDataset(dataset);
        }

      }
    } catch (MappingException e) {
      logWarn(e.getMessage());
    }
  }
}
