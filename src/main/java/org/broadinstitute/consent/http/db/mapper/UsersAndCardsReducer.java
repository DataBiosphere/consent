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
    //possible conflict between user ids and lc ids (can share ids between the two but not be related)
    //However, email addresses do not have this issue (lc_user_email and user_email should be the same)
    //As such, the email would be a better id than the actual ids
    String lcEmail = rowView.getColumn("lc_user_email", String.class);
    String userEmail = rowView.getColumn("email", String.class);
    String id = Objects.nonNull(userEmail) ? userEmail : lcEmail; 
    User user;
    
    if(Objects.nonNull(rowView.getColumn("email", String.class))) {
      user = rowView.getRow(User.class);
    } else {
      user = new User();
      user.setEmail(lcEmail);
    }
    if(Objects.isNull(user.getLibraryCards())) {
      user.setLibraryCards(new ArrayList<LibraryCard>());
    }
    map.put(id, user);

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
      // Ignore institution mapping errors, possible for new users to not have an
      // institution
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
      // Ignore exceptions here, user may not have a library card issued under this
      // instiution
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
