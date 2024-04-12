package org.broadinstitute.consent.http.db.mapper;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
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
        daa.setDaaId(rowView.getColumn("daa_id", Integer.class));
      }
    } catch (MappingException e) {
    }

    try {
      if (Objects.nonNull(card) && Objects.nonNull(
          rowView.getColumn("create_user_id", Integer.class))) {
        daa.setCreateUserId(rowView.getColumn("create_user_id", Integer.class));
      }
    } catch (MappingException e) {
    }

    try {
      if (Objects.nonNull(card) && Objects.nonNull(
          rowView.getColumn("create_date", Instant.class))) {
        daa.setCreateDate(rowView.getColumn("create_date", Instant.class));
      }
    } catch (MappingException e) {
    }

    try {
      if (Objects.nonNull(card) && Objects.nonNull(
          rowView.getColumn("update_user_id", Integer.class))) {
        daa.setUpdateUserId(rowView.getColumn("update_user_id", Integer.class));
      }
    } catch (MappingException e) {
    }

    try {
      if (Objects.nonNull(card) && Objects.nonNull(
          rowView.getColumn("create_date", Instant.class))) {
        daa.setCreateDate(rowView.getColumn("create_date", Instant.class));
      }
    } catch (MappingException e) {
    }

    try {
      if (Objects.nonNull(card) && Objects.nonNull(
          rowView.getColumn("initial_dac_id", Integer.class))) {
        daa.setInitialDacId(rowView.getColumn("initial_dac_id", Integer.class));
      }
    } catch (MappingException e) {
    }

    if (daa != null) {
      card.addDaaObject(daa);
    }
  }
}