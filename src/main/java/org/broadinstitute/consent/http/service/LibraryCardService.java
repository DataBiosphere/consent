package org.broadinstitute.consent.http.service;

import com.google.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import org.broadinstitute.consent.http.db.InstitutionDAO;
import org.broadinstitute.consent.http.db.LibraryCardDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.exceptions.ConsentConflictException;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.models.LibraryCard;
import org.broadinstitute.consent.http.models.User;

public class LibraryCardService {

  private final LibraryCardDAO libraryCardDAO;
  private final InstitutionDAO institutionDAO;
  private final InstitutionService institutionService;
  private final UserDAO userDAO;

  @Inject
  public LibraryCardService(LibraryCardDAO libraryCardDAO, InstitutionDAO institutionDAO,
      InstitutionService institutionService,
      UserDAO userDAO) {
    this.libraryCardDAO = libraryCardDAO;
    this.institutionDAO = institutionDAO;
    this.institutionService = institutionService;
    this.userDAO = userDAO;
  }

  public LibraryCard createLibraryCard(LibraryCard libraryCard, User user) {
    throwIfNull(libraryCard);
    boolean isAdmin = checkIsAdmin(user);
    checkIfCardExists(libraryCard);
    processUserOnNewLC(libraryCard);
    if (!isAdmin) {
      checkForValidInstitution(user.getInstitutionId(), libraryCard.getUserEmail());
    }
    Date createDate = new Date();
    Integer id = libraryCardDAO.insertLibraryCard(
        libraryCard.getUserId(),
        libraryCard.getUserName(),
        libraryCard.getUserEmail(),
        libraryCard.getCreateUserId(),
        createDate);
    return libraryCardDAO.findLibraryCardById(id);
  }

  public void deleteLibraryCardById(Integer id) {
    LibraryCard card = findLibraryCardById(id);
    throwIfNull(card);
    libraryCardDAO.deleteLibraryCardById(id);
  }

  public List<LibraryCard> findAllLibraryCards() {
    return libraryCardDAO.findAllLibraryCards();
  }

  public LibraryCard findLibraryCardByUserId(Integer userId) {
    return libraryCardDAO.findLibraryCardByUserId(userId);
  }

  public List<LibraryCard> findLibraryCardsByInstitutionId(Integer institutionId) {
    return libraryCardDAO.findLibraryCardsByInstitutionId(institutionId);
  }

  public LibraryCard findLibraryCardById(Integer libraryCardId) {
    LibraryCard libraryCard = libraryCardDAO.findLibraryCardById(libraryCardId);
    throwIfNull(libraryCard);
    return libraryCard;
  }

  public LibraryCard findLibraryCardWithDaasById(Integer libraryCardId) {
    LibraryCard libraryCard = libraryCardDAO.findLibraryCardDaaById(libraryCardId);
    throwIfNull(libraryCard);
    return libraryCard;
  }

  public void addDaaToLibraryCard(Integer libraryCardId, Integer daaId) {
    libraryCardDAO.createLibraryCardDaaRelation(libraryCardId, daaId);
  }

  public void removeDaaFromLibraryCard(Integer libraryCardId, Integer daaId) {
    libraryCardDAO.deleteLibraryCardDaaRelation(libraryCardId, daaId);
  }

  public LibraryCard addDaaToUserLibraryCardByInstitution(User user, User signingOfficial, Integer daaId) {
    if (signingOfficial.getInstitutionId() == null) {
      throw new BadRequestException("This signing official does not have an institution.");
    }
    LibraryCard libraryCard = libraryCardDAO.findLibraryCardByUserId(user.getUserId());
    if (libraryCard == null) {
      libraryCard = createLibraryCardForSigningOfficial(user, signingOfficial);
    }
    // typically there should be one library card per user per institution
    addDaaToLibraryCard(libraryCard.getId(), daaId);
    return libraryCardDAO.findLibraryCardByUserId(user.getUserId());
  }

  public LibraryCard removeDaaFromUserLibraryCard(User user, Integer daaId) {
    LibraryCard libraryCard = findLibraryCardByUserId(user.getUserId());
    // typically there should be one library card per user
    if (libraryCard != null) {
      removeDaaFromLibraryCard(libraryCard.getId(), daaId);
    }
    return findLibraryCardByUserId(user.getUserId());
  }

  public LibraryCard createLibraryCardForSigningOfficial(User user, User signingOfficial) {
    LibraryCard lc = new LibraryCard();
    lc.setUserId(user.getUserId());
    lc.setUserName(user.getDisplayName());
    lc.setUserEmail(user.getEmail());
    lc.setCreateUserId(signingOfficial.getUserId());
    LibraryCard createdLc = createLibraryCard(lc, user);
    return createdLc;
  }

  private void checkForValidInstitution(Integer institutionId, String userEmail) {
    checkInstitutionId(institutionId);
    Institution institution = institutionDAO.findInstitutionById(institutionId);

    if (Objects.isNull(institution)) {
      throw new IllegalArgumentException("Invalid Institution Id");
    }

    var userInstitution = institutionService.findInstitutionForEmail(userEmail);
    if (userInstitution == null || !userInstitution.getId().equals(institutionId)) {
      throw new BadRequestException(
          "User email %s does not match institution %s".formatted(userEmail, institution.getName()));
    }
  }

  private void checkInstitutionId(Integer institutionId) {
    if (Objects.isNull(institutionId)) {
      throw new IllegalArgumentException("Institution ID is a required parameter");
    }
  }

  private void throwIfNull(LibraryCard libraryCard) {
    if (Objects.isNull(libraryCard)) {
      throw new NotFoundException("LibraryCard not found.");
    }
  }

  //helper method for create method, checks to see if card already exists
  private void checkIfCardExists(LibraryCard payload) {
    LibraryCard result = null;

    if (payload.getUserId() != null) {
      result = libraryCardDAO.findLibraryCardByUserId(payload.getUserId());
    } else if (payload.getUserEmail() != null) {
      result = libraryCardDAO.findLibraryCardByUserEmail(payload.getUserEmail());
    } else {
      throw new BadRequestException();
    }

    if (result != null) {
      Boolean sameUserId = payload.getUserId() != null && result.getUserId().equals(payload.getUserId());
      Boolean sameUserEmail = payload.getUserEmail() != null && result.getUserEmail().equalsIgnoreCase(payload.getUserEmail());
      if (sameUserId || sameUserEmail) {
        throw new ConsentConflictException("Library card already exists for this user.");
      }
    }
  }

  // Helper method to process user data on create LC payload.
  // Needed since CREATE has a unique situation where admins can create LCs without an active
  // user (save with userEmail instead).
  private void processUserOnNewLC(LibraryCard card) {
    if (card.getUserId() == null) {
      // No user ID is provided, email must exist in card request.
      if (card.getUserEmail() == null) {
        throw new BadRequestException();
      }
      // If a user is found, update the card to have the correct userId associated.
      User user = userDAO.findUserByEmail(card.getUserEmail());
      if (user != null) {
        card.setUserId(user.getUserId());
      }
    } else {
      // check if userId exists
      User user = userDAO.findUserById(card.getUserId());
      if (user == null) {
        throw new BadRequestException();
      }
      if (card.getUserEmail() == null) {
        // if no email is provided in the card request, use the one from the user.
        card.setUserEmail(user.getEmail());
      } else if (!(user.getEmail().equalsIgnoreCase(card.getUserEmail()))) {
        // Emails do not match, throw an error.
        throw new ConsentConflictException();
      }
      card.setUserName(user.getDisplayName());
    }
  }

  private boolean checkIsAdmin(User user) {
    return user.getRoles()
        .stream()
        .anyMatch(role -> role.getName().equalsIgnoreCase(UserRoles.ADMIN.getRoleName()));
  }
}
