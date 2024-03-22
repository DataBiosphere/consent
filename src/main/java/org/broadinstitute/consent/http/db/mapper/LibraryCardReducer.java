package org.broadinstitute.consent.http.db.mapper;

import java.util.Map;
import java.util.Objects;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.models.LibraryCard;
import org.jdbi.v3.core.mapper.MappingException;
import org.jdbi.v3.core.result.LinkedHashMapRowReducer;
import org.jdbi.v3.core.result.RowView;

public class LibraryCardReducer implements LinkedHashMapRowReducer<Integer, LibraryCard> {

  @Override
  public void accumulate(Map<Integer, LibraryCard> map, RowView rowView) {
    Institution institution = null;
    LibraryCard card = map.computeIfAbsent(
        rowView.getColumn("id", Integer.class),
        id -> rowView.getRow(LibraryCard.class));

    try {
      if (Objects.nonNull(card) && Objects.nonNull(
          rowView.getColumn("i_institution_id", Integer.class))) {
        institution = rowView.getRow(Institution.class);
        institution.setId(rowView.getColumn("i_institution_id", Integer.class));
      }
    } catch (MappingException e) {
    }

    if (Objects.nonNull(institution)) {
      card.setInstitution(institution);
    }

    try {
      if (Objects.nonNull(card) && Objects.nonNull(
          rowView.getColumn("daa_id", Integer.class))) {
        card.addDaa(rowView.getColumn("daa_id", Integer.class));
      }
    } catch (MappingException e) {
    }
  }
}