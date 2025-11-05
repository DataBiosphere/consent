package org.broadinstitute.consent.http.service.dao;

import com.google.inject.Inject;
import org.broadinstitute.consent.http.db.LibraryCardDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.db.UserRoleDAO;
import org.broadinstitute.consent.http.models.UserRole;
import org.jdbi.v3.core.Jdbi;

public class UserServiceDAO {

  Jdbi jdbi;
  LibraryCardDAO libraryCardDAO;
  UserDAO userDAO;
  UserRoleDAO userRoleDAO;

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
}
