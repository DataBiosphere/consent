package org.broadinstitute.consent.http.db.mapper;

import java.util.Map;
import org.broadinstitute.consent.http.models.LibraryCard;
import org.jdbi.v3.core.mapper.MappingException;
import org.jdbi.v3.core.result.LinkedHashMapRowReducer;
import org.jdbi.v3.core.result.RowView;

public class LibraryCardReducer implements LinkedHashMapRowReducer<Integer, LibraryCard>,
    RowMapperHelper {

  @Override
  public void accumulate(Map<Integer, LibraryCard> map, RowView rowView) {
    LibraryCard card = map.computeIfAbsent(
        rowView.getColumn("id", Integer.class),
        id -> rowView.getRow(LibraryCard.class));
    try {
      if (hasNonZeroColumn(rowView,"daa_id")) {
        card.addDaa(rowView.getColumn("daa_id", Integer.class));
      }
    } catch (MappingException e) {
      logWarn("Error adding DAA to Library Card", e);
    }
  }
}