package org.broadinstitute.consent.http.db.mapper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Stream;
import org.jdbi.v3.core.result.RowReducer;
import org.jdbi.v3.core.result.RowView;

public class DaaDatasetReducer
    implements RowReducer<Map<Integer, Set<Integer>>, Map.Entry<Integer, Set<Integer>>> {

  @Override
  public Map<Integer, Set<Integer>> container() {
    return new HashMap<>();
  }

  @Override
  public void accumulate(Map<Integer, Set<Integer>> container, RowView rowView) {
    int daaId = rowView.getColumn("daa_id", Integer.class);
    Integer datasetId = rowView.getColumn("dataset_id", Integer.class);
    container.computeIfAbsent(daaId, k -> new HashSet<>()).add(datasetId);
  }

  @Override
  public Stream<Entry<Integer, Set<Integer>>> stream(Map<Integer, Set<Integer>> container) {
    return container.entrySet().stream();
  }
}
