package org.broadinstitute.consent.http.service.feature;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import java.util.concurrent.ExecutorService;
import org.broadinstitute.consent.http.db.InstitutionDAO;
import org.broadinstitute.consent.http.db.LibraryCardDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.service.dao.UserServiceDAO;
import org.broadinstitute.consent.http.util.ConsentLogger;
import org.broadinstitute.consent.http.util.ThreadUtils;
import org.jdbi.v3.core.Jdbi;
import org.jetbrains.annotations.NotNull;

/**
 * Service that implements a set of rules in order to ensure Library Card and Institution matching
 * rules are adhered to for users of the system.
 */
public class InstitutionAndLibraryCardEnforcement implements ConsentLogger {

  private final ExecutorService executorService =
      new ThreadUtils().getExecutorService(InstitutionAndLibraryCardEnforcement.class);
  private final InstitutionDAO institutionDAO;
  private final LibraryCardDAO libraryCardDAO;
  private final UserDAO userDAO;
  private final UserServiceDAO userServiceDAO;

  @Inject
  public InstitutionAndLibraryCardEnforcement(Jdbi jdbi, UserServiceDAO userServiceDAO) {
    this.institutionDAO = jdbi.onDemand(InstitutionDAO.class);
    this.libraryCardDAO = jdbi.onDemand(LibraryCardDAO.class);
    this.userDAO = jdbi.onDemand(UserDAO.class);
    this.userServiceDAO = userServiceDAO;
  }

  /**
   * Compliance method that implements a set of rules in order to ensure Library Card and
   * Institution matching rules are adhered to when authorizing users of the system.
   *
   * @param email of the user being evaluated
   * @return user with the Institution and Library Card rules applied or null if the requestor isn't
   *     a DUOS user.
   * @throws NotFoundException when the user isn't found in our database
   */
  public User enforceInstitutionAndLibraryCardRules(String email) {
    User user = userDAO.findUserByEmail(email);
    if (user == null) {
      throw new NotFoundException(email);
    }
    return enforceInstitutionAndLibraryCardRules(user);
  }

  public void asyncEnforceInstitutionAndLibraryCardRulesForAllUsers() {
    ListeningExecutorService listeningExecutorService =
        MoreExecutors.listeningDecorator(executorService);
    ListenableFuture<User> enforceRulesFuture =
        listeningExecutorService.submit(
            () -> {
              for (User user : userDAO.findUsersWithLCsAndInstitution()) {
                try {
                  User updatedUser = enforceInstitutionAndLibraryCardRules(user);
                  logInfo("Enforced institution and LC rules for user: " + updatedUser.getEmail());
                } catch (Exception e) {
                  logWarn(
                      "Error enforcing institution and LC rules for user: " + user.getEmail(), e);
                }
              }
              return null;
            });
    Futures.addCallback(
        enforceRulesFuture,
        new FutureCallback<>() {
          @Override
          public void onSuccess(User result) {
            logInfo("Completed enforcing institution and LC rules for all users.");
          }

          @Override
          public void onFailure(@NotNull Throwable t) {
            logWarn("Error completing enforcement of institution and LC rules for all users.", t);
          }
        },
        listeningExecutorService);
  }

  public String trimmedEmailDomain(String email) {
    String trimmedEmail = email.trim();
    return trimmedEmail.substring(trimmedEmail.indexOf('@') + 1);
  }

  /**
   * Core method that implements a set of rules in order to ensure Library Card and Institution
   * matching rules are adhered to when authorizing users of the system.
   *
   * @param user The DUOS User
   * @return The modified user if any changes were made, otherwise the original user.
   */
  @VisibleForTesting
  protected User enforceInstitutionAndLibraryCardRules(User user) {
    Integer institutionId = findInstitutionIdForEmail(user.getEmail());
    boolean modifiedUser = false;

    if (institutionId != null) {
      if (handleUserWithInstitutionInMap(user, institutionId)) {
        modifiedUser = true;
      }
    } else {
      if (handleUserWithoutInstitutionInMap(user)) {
        modifiedUser = true;
      }
    }

    if (modifiedUser) {
      return userDAO.findUserByEmail(user.getEmail());
    } else {
      return user;
    }
  }

  @VisibleForTesting
  protected boolean handleUserWithInstitutionInMap(User user, Integer institutionId) {
    boolean needsLCRemoved = needsLibraryCardRemovedForUser(user, institutionId);
    boolean needsInstitutionAssigned = !institutionId.equals(user.getInstitutionId());

    if (needsInstitutionAssigned && needsLCRemoved) {
      userServiceDAO.updateInstitutionAndClearLibraryCardForUser(user.getUserId(), institutionId);
    } else if (needsInstitutionAssigned) {
      userDAO.updateInstitutionId(user.getUserId(), institutionId);
    } else if (needsLCRemoved) {
      libraryCardDAO.deleteAllLibraryCardsByUser(user.getUserId());
    }

    return needsLCRemoved || needsInstitutionAssigned;
  }

  @VisibleForTesting
  protected boolean needsLibraryCardRemovedForUser(User user, Integer userInstitutionId) {
    boolean needsLCRemoved = false;
    if (hasLibraryCard(user)) {
      try {
        User lcIssuer = userDAO.findUserById(user.getLibraryCard().getCreateUserId());
        if (lcIssuer == null) {
          return true;
        }
        Institution lcIssuerInstitution = findInstitutionForEmail(lcIssuer.getEmail());
        if (lcIssuerInstitution == null || !userInstitutionId.equals(lcIssuerInstitution.getId())) {
          needsLCRemoved = true;
        }
      } catch (NotFoundException nfe) {
        needsLCRemoved = true;
      }
    }
    return needsLCRemoved;
  }

  @VisibleForTesting
  protected boolean handleUserWithoutInstitutionInMap(User user) {
    if (hasLibraryCard(user)) {
      dropLCAndInstitutionForUser(user);
      return true;
    } else {
      if (user.getInstitutionId() != null) {
        userDAO.updateInstitutionId(user.getUserId(), null);
        return true;
      }
    }
    return false;
  }

  @VisibleForTesting
  protected boolean hasLibraryCard(User user) {
    return user.getLibraryCard() != null;
  }

  @VisibleForTesting
  protected boolean hasMatchingInstitutionInDatabase(
      Institution institutionFromEmail, Institution institutionFromDatabase) {
    if (institutionFromEmail == null || institutionFromDatabase == null) {
      return false;
    }
    return institutionFromDatabase.equals(institutionFromEmail);
  }

  private void dropLCAndInstitutionForUser(User user) {
    userServiceDAO.updateInstitutionAndClearLibraryCardForUser(user.getUserId(), null);
  }

  private Institution findInstitutionForEmail(String email) {
    return institutionDAO.findInstitutionByDomain(trimmedEmailDomain(email));
  }

  private Integer findInstitutionIdForEmail(String email) {
    return institutionDAO.findInstitutionIdByDomain(trimmedEmailDomain(email));
  }
}
