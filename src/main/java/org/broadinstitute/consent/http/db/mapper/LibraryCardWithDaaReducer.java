package org.broadinstitute.consent.http.db.mapper;

import java.util.Map;
import org.broadinstitute.consent.http.models.DataAccessAgreement;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.models.LibraryCard;
import org.jdbi.v3.core.mapper.MappingException;
import org.jdbi.v3.core.result.LinkedHashMapRowReducer;
import org.jdbi.v3.core.result.RowView;

public class LibraryCardWithDaaReducer implements LinkedHashMapRowReducer<Integer, LibraryCard> {

  @Override
  public void accumulate(Map<Integer, LibraryCard> map, RowView rowView) {
    Institution institution = null;
    LibraryCard card = map.computeIfAbsent(
        rowView.getColumn("id", Integer.class),
        id -> rowView.getRow(LibraryCard.class));
    DataAccessAgreement daa = new DataAccessAgreement();

    try {
      if (card != null && rowView.getColumn("i_institution_id", Integer.class) != null) {
        institution = rowView.getRow(Institution.class);
        institution.setId(rowView.getColumn("i_institution_id", Integer.class));
      }
    } catch (MappingException e) {
    }

    if (institution != null) {
      card.setInstitution(institution);
    }

    try {
      if (card != null && rowView.getColumn("daa_id", Integer.class) != null) {
        daa = rowView.getRow(DataAccessAgreement.class);
        card.addDaa(rowView.getColumn("daa_id", Integer.class));
      }
    } catch (MappingException e) {
    }

    if (daa.getDaaId() != null) {
      card.addDaaObject(daa);
    }
  }
}