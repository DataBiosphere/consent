package org.broadinstitute.consent.http.db.mapper;

import java.util.Map;

import org.broadinstitute.consent.http.models.LibraryCard;
import org.broadinstitute.consent.http.models.User;
import org.jdbi.v3.core.mapper.MappingException;
import org.jdbi.v3.core.result.LinkedHashMapRowReducer;
import org.jdbi.v3.core.result.RowView;

public class UnregisteredUsersWithCardsReducer implements LinkedHashMapRowReducer<Integer, User> {
  @Override
  public void accumulate(Map<Integer, User> map, RowView rowView) {
    //mapping function will use lc id to map blank user object for unregistered users
    User user = map.computeIfAbsent(rowView.getColumn("id", Integer.class), id -> new User());

    try{
      LibraryCard card = rowView.getRow(LibraryCard.class);
      user.setEmail(card.getUserEmail());
      user.addLibraryCard(card);      
    } catch(MappingException e) {
      //Ignore mapping errors
    }
  }
}
