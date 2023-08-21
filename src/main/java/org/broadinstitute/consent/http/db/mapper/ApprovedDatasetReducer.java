package org.broadinstitute.consent.http.db.mapper;

import java.util.Map;
import org.broadinstitute.consent.http.models.ApprovedDataset;
import org.jdbi.v3.core.result.LinkedHashMapRowReducer;
import org.jdbi.v3.core.result.RowView;

public class ApprovedDatasetReducer
    implements LinkedHashMapRowReducer<String, ApprovedDataset>, RowMapperHelper {

  @Override
  public void accumulate(Map<String, ApprovedDataset> map, RowView rowView) {
    ApprovedDataset f = rowView.getRow(ApprovedDataset.class);
    String darDatasetApproval = f.getDarCode() + "-" + f.getAlias();
    map.put(darDatasetApproval, f);
  }
}
