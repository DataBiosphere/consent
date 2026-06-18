package org.broadinstitute.consent.http.service.dao;

import com.google.inject.Inject;
import java.util.Date;
import org.broadinstitute.consent.http.db.LibraryCardDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.db.UserRoleDAO;
import org.broadinstitute.consent.http.models.LibraryCard;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.jdbi.v3.core.Jdbi;

public class UserServiceDAO {

  private final Jdbi jdbi;
  private final LibraryCardDAO libraryCardDAO;
  private final UserDAO userDAO;
  private final UserRoleDAO userRoleDAO;

  @Inject
  public UserServiceDAO(
      Jdbi jdbi, LibraryCardDAO libraryCardDAO, UserDAO userDAO, UserRoleDAO userRoleDAO) {
    this.jdbi = jdbi;
    this.libraryCardDAO = libraryCardDAO;
    this.userDAO = userDAO;
    this.userRoleDAO = userRoleDAO;
  }

  public void insertRoleAndInstitutionTxn(UserRole role, Integer institutionId, Integer userId) {
    jdbi.useTransaction(
        transactionHandle -> {
          UserDAO userDAOT = transactionHandle.attach(UserDAO.class);
          UserRoleDAO userRoleDAOT = transactionHandle.attach(UserRoleDAO.class);
          userDAOT.updateInstitutionId(userId, institutionId);
          userRoleDAOT.insertSingleUserRole(role.getRoleId(), userId);
        });
  }

  public void updateInstitutionAndClearLibraryCardForUser(Integer userId, Integer institutionId) {
    jdbi.useTransaction(
        transactionHandle -> {
          UserDAO userDAOT = transactionHandle.attach(UserDAO.class);
          LibraryCardDAO libraryCardDAOT = transactionHandle.attach(LibraryCardDAO.class);
          userDAOT.updateInstitutionId(userId, institutionId);
          libraryCardDAOT.deleteAllLibraryCardsByUser(userId);
        });
  }

  public User createUser(User user) {
    return jdbi.inTransaction(
        transactionHandle -> {
          if (user.getRoles() == null || user.getRoles().isEmpty()) {
            throw new IllegalArgumentException("At least one role must be specified for the user.");
          }
          UserDAO userDAOT = transactionHandle.attach(UserDAO.class);
          UserRoleDAO userRoleDAOT = transactionHandle.attach(UserRoleDAO.class);
          LibraryCardDAO libraryCardDAOT = transactionHandle.attach(LibraryCardDAO.class);
          Integer userId =
              userDAOT.insertUser(
                  user.getEmail(), user.getDisplayName(), user.getInstitutionId(), new Date());
          userDAOT.updateEmailPreference(userId, user.getEmailPreference());
          for (UserRole role : user.getRoles()) {
            userRoleDAOT.insertSingleUserRole(role.getRoleId(), userId);
          }
          LibraryCard libraryCard = libraryCardDAOT.findLibraryCardByUserEmail(user.getEmail());
          if (libraryCard != null) {
            libraryCardDAOT.updateLibraryCardById(
                libraryCard.getId(),
                userId,
                user.getDisplayName(),
                user.getEmail(),
                userId,
                new Date());
          }
          return userDAOT.findUserById(userId);
        });
  }
}
