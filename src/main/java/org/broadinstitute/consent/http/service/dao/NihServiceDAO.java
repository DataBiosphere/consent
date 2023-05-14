package org.broadinstitute.consent.http.service.dao;

import com.google.inject.Inject;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import org.broadinstitute.consent.http.db.LibraryCardDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.db.UserPropertyDAO;
import org.broadinstitute.consent.http.enumeration.UserFields;
import org.broadinstitute.consent.http.models.NIHUserAccount;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserProperty;
import org.broadinstitute.consent.http.util.ConsentLogger;
import org.jdbi.v3.core.Jdbi;

public class NihServiceDAO implements ConsentLogger {

  private final Jdbi jdbi;

  @Inject
  public NihServiceDAO(Jdbi jdbi) {
    this.jdbi = jdbi;
  }

  public void updateUserNihStatus(User user, NIHUserAccount nihAccount)
      throws IllegalArgumentException {
    // fail fast
    if (Objects.isNull(nihAccount) || Objects.isNull(nihAccount.getStatus()) || Objects.isNull(
        nihAccount.getEraExpiration())) {
      throw new IllegalArgumentException("Invalid NIH account information");
    }
    jdbi.useTransaction(handler -> {
      LibraryCardDAO libraryCardDAO = handler.attach(LibraryCardDAO.class);
      UserDAO userDAO = handler.attach(UserDAO.class);
      UserPropertyDAO userPropertyDAO = handler.attach(UserPropertyDAO.class);
      Collection<UserProperty> properties = List.of(
          new UserProperty(user.getUserId(), UserFields.ERA_STATUS.getValue(),
              nihAccount.getStatus().toString()),
          new UserProperty(user.getUserId(), UserFields.ERA_EXPIRATION_DATE.getValue(),
              nihAccount.getEraExpiration())
      );
      userPropertyDAO.insertAll(properties);
      libraryCardDAO.updateEraCommonsForUser(user.getUserId(), nihAccount.getNihUsername());
      userDAO.updateEraCommonsId(user.getUserId(), nihAccount.getNihUsername());
    });
  }
}
