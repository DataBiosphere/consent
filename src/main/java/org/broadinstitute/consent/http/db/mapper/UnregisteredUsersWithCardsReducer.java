package org.broadinstitute.consent.http.db.mapper;

import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.models.LibraryCard;
import org.broadinstitute.consent.http.models.User;
import org.jdbi.v3.core.mapper.MappingException;
import org.jdbi.v3.core.result.LinkedHashMapRowReducer;
import org.jdbi.v3.core.result.RowView;

import java.util.Map;
import java.util.Objects;

public class UnregisteredUsersWithCardsReducer implements LinkedHashMapRowReducer<Integer, User> {
  @Override
  public void accumulate(Map<Integer, User> map, RowView rowView) {
    // mapping function will use lc id to map blank user object for unregistered users
    User user = map.computeIfAbsent(rowView.getColumn("id", Integer.class), id -> new User());

    try {
      LibraryCard card = rowView.getRow(LibraryCard.class);
      try {
        if (Objects.nonNull(rowView.getColumn("lci_id", Integer.class))) {
          Institution institution = rowView.getRow(Institution.class);
          card.setInstitution(institution);
        }
      } catch (MappingException e) {
        // Ignore institution mapping errors
      }
      user.setEmail(card.getUserEmail());
      user.addLibraryCard(card);
    } catch (MappingException e) {
      // Ignore mapping errors
    }
  }
}
