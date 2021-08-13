package org.broadinstitute.consent.http.db.mapper;

import java.util.Map;
import java.util.Objects;
import java.util.ArrayList;
import org.broadinstitute.consent.http.enumeration.RoleStatus;
import org.broadinstitute.consent.http.models.LibraryCard;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.jdbi.v3.core.mapper.MappingException;
import org.jdbi.v3.core.result.LinkedHashMapRowReducer;
import org.jdbi.v3.core.result.RowView;

public class UsersAndCardsReducer implements LinkedHashMapRowReducer<String, User> {
  @Override
  public void accumulate(Map<String, User> map, RowView rowView) {
    //Reduces addresses situation where SO console lists LCs in the context of the associated user
    //At the same time, SOs can issue cards to users that have yet to join DUOS (custom email input)
    //As such userIds alone can't be used as a map index
    //Also userIds and LC ids can't be used interchangebly as an index either (possible conflict)
    //Emails are unique among users and lcs, and an email tied to an lc corresponds to the email on the user model
    //As such, emails are ideal for the map index.
    String lcEmail = rowView.getColumn("lc_user_email", String.class);
    String userEmail = rowView.getColumn("email", String.class);
    String email = Objects.nonNull(userEmail) ? userEmail : lcEmail; 
    User user = map.computeIfAbsent(email, id -> rowView.getRow(User.class));

    //Unlike the other user reducers, a null user should be processed due to an accompanied LC
    //As such use the LC to initialize some attributes (user email and LC array)
    if(Objects.isNull(user.getEmail())) {
      user.setEmail(email);
    }
    if(Objects.isNull(user.getLibraryCards())) {
      user.setLibraryCards(new ArrayList<LibraryCard>());
    }

    try {
      if (Objects.nonNull(rowView.getColumn("status", Integer.class))) {
        user.setStatus(getStatus(rowView));
      }
    } catch (MappingException e) {
      // Ignore any attempt to map a column that doesn't exist
    }
    try {
      if (Objects.nonNull(rowView.getColumn("user_role_id", Integer.class))) {
        UserRole ur = rowView.getRow(UserRole.class);
        user.addRole(ur);
      }
    } catch (MappingException e) {
      // Ignore any attempt to map a column that doesn't exist
    }
    try {
      if (Objects.nonNull(rowView.getColumn("i_id", Integer.class))) {
        Institution institution = rowView.getRow(Institution.class);
        user.setInstitution(institution);
      }
    } catch (MappingException e) {
      // Ignore institution mapping errors, possible for new users to not have an institution
    }
    try {
      if (Objects.nonNull(rowView.getColumn("lc_id", Integer.class))) {
        LibraryCard lc = rowView.getRow(LibraryCard.class);
        //findUsersByInstitutions can introduce copies of a card due to multiple user roles
        //filter out duplicates to prevent duplicates from being introduced
        if (!user.getLibraryCards().stream().anyMatch(card -> card.getId() == lc.getId())) {
          user.getLibraryCards().add(lc);
        }
      }
    } catch (MappingException e) {
      // Ignore exceptions here, user may not have a library card issued
    }
  }

  private String getStatus(RowView r) {
    try {
      return RoleStatus.getStatusByValue(r.getColumn("status", Integer.class));
    } catch (Exception e) {
      return null;
    }
  }
};
