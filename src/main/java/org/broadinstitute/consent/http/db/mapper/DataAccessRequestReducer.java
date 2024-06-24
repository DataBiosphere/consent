package org.broadinstitute.consent.http.db.mapper;

import java.util.Map;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.jdbi.v3.core.result.LinkedHashMapRowReducer;
import org.jdbi.v3.core.result.RowView;

public class DataAccessRequestReducer
    implements LinkedHashMapRowReducer<Integer, DataAccessRequest>, RowMapperHelper {

  @Override
  public void accumulate(Map<Integer, DataAccessRequest> map, RowView rowView) {
    DataAccessRequest dar =
        map.computeIfAbsent(
            rowView.getColumn("id", Integer.class), id -> rowView.getRow(DataAccessRequest.class));
    if (hasNonZeroColumn(rowView, "dataset_id")) {
      dar.addDatasetId(rowView.getColumn("dataset_id", Integer.class));
    }
  }
}
