package org.broadinstitute.consent.http.db.mapper;

import org.broadinstitute.consent.http.models.LibraryCard;
import org.jdbi.v3.core.result.LinkedHashMapRowReducer;
import org.jdbi.v3.core.result.RowView;

import java.util.Map;

public class LibraryCardReducer implements LinkedHashMapRowReducer<Integer, LibraryCard> {

  @Override
  public void accumulate(Map<Integer, LibraryCard> map, RowView rowView) {
        map.computeIfAbsent(
            rowView.getColumn("id", Integer.class),
            id -> rowView.getRow(LibraryCard.class));
  }

}