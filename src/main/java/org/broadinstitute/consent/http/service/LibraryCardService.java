package org.broadinstitute.consent.http.service;

import com.google.inject.Inject;
import freemarker.template.TemplateException;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import org.broadinstitute.consent.http.db.DaaDAO;
import org.broadinstitute.consent.http.db.InstitutionDAO;
import org.broadinstitute.consent.http.db.LibraryCardDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.exceptions.ConsentConflictException;
import org.broadinstitute.consent.http.models.DataAccessAgreement;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.models.LibraryCard;
import org.broadinstitute.consent.http.models.LibraryCardDaaAudit;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.util.ConsentLogger;

public class LibraryCardService implements ConsentLogger {

  private final DaaDAO daaDAO;
  private final LibraryCardDAO libraryCardDAO;
  private final InstitutionDAO institutionDAO;
  private final InstitutionService institutionService;
  private final UserDAO userDAO;
  private final EmailService emailService;

  @Inject
  public LibraryCardService(
      DaaDAO daaDAO,
      LibraryCardDAO libraryCardDAO,
      InstitutionDAO institutionDAO,
      InstitutionService institutionService,
      UserDAO userDAO,
      EmailService emailService) {
    this.daaDAO = daaDAO;
    this.libraryCardDAO = libraryCardDAO;
    this.institutionDAO = institutionDAO;
    this.institutionService = institutionService;
    this.userDAO = userDAO;
    this.emailService = emailService;
  }

  public LibraryCard createLibraryCard(LibraryCard libraryCard, User user) {
    throwIfNull(libraryCard);
    checkIfCardExists(libraryCard);
    processUserOnNewLC(libraryCard);
    checkForValidInstitution(user.getInstitutionId(), libraryCard.getUserEmail());
    Date createDate = new Date();
    Integer id =
        libraryCardDAO.insertLibraryCard(
            libraryCard.getUserId(),
            libraryCard.getUserName(),
            libraryCard.getUserEmail(),
            libraryCard.getCreateUserId(),
            createDate);
    User toUser = userDAO.findUserByEmail(libraryCard.getUserEmail());
    if (toUser != null) {
      try {
        emailService.sendNewLibraryCardIssuedMessage(toUser);
      } catch (IOException | TemplateException e) {
        logWarn(
            "Failed to send library card issuance notification for user " + user.getUserId(), e);
      }
    }
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

  public void addDaaToLibraryCard(
      Integer lcUserId, Integer userId, Integer libraryCardId, Integer daaId) {
    User lcUser = userDAO.findUserById(lcUserId);
    if (lcUser == null) {
      throw new NotFoundException("User with id " + lcUserId + " not found");
    }
    LibraryCard lc = libraryCardDAO.findLibraryCardById(libraryCardId);
    if (lc == null) {
      throw new NotFoundException("Library card with id " + libraryCardId + " not found");
    }
    DataAccessAgreement daa = daaDAO.findById(daaId);
    if (daa == null) {
      throw new NotFoundException("Data Access Agreeement id " + daaId + " not found");
    }
    libraryCardDAO.createLibraryCardDaaRelation(lcUserId, userId, libraryCardId, daaId);
  }

  public void removeDaaFromLibraryCard(
      Integer lcUserId, Integer userId, Integer libraryCardId, Integer daaId) {
    libraryCardDAO.deleteLibraryCardDaaRelation(lcUserId, userId, libraryCardId, daaId);
  }

  public LibraryCard addDaaToUserLibraryCard(User user, User signingOfficial, Integer daaId) {
    if (signingOfficial.getInstitutionId() == null) {
      throw new BadRequestException("This signing official does not have an institution.");
    }
    LibraryCard libraryCard = libraryCardDAO.findLibraryCardByUserId(user.getUserId());
    if (libraryCard == null) {
      libraryCard = createLibraryCardForSigningOfficial(user, signingOfficial);
    }
    addDaaToLibraryCard(user.getUserId(), signingOfficial.getUserId(), libraryCard.getId(), daaId);
    return libraryCardDAO.findLibraryCardByUserId(user.getUserId());
  }

  public LibraryCard removeDaaFromUserLibraryCard(
      User lcUser, User signingOfficial, Integer daaId) {
    LibraryCard libraryCard = findLibraryCardByUserId(lcUser.getUserId());
    // typically there should be one library card per user
    if (libraryCard != null) {
      removeDaaFromLibraryCard(
          lcUser.getUserId(), signingOfficial.getUserId(), libraryCard.getId(), daaId);
    }
    return findLibraryCardByUserId(lcUser.getUserId());
  }

  public LibraryCard createLibraryCardForSigningOfficial(User user, User signingOfficial) {
    LibraryCard lc = new LibraryCard();
    lc.setUserId(user.getUserId());
    lc.setUserName(user.getDisplayName());
    lc.setUserEmail(user.getEmail());
    lc.setCreateUserId(signingOfficial.getUserId());
    return createLibraryCard(lc, user);
  }

  public List<LibraryCardDaaAudit> findLibraryCardDaaAuditsByUserId(Integer userId) {
    return libraryCardDAO.findAuditsByLcUserId(userId);
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
          "User email %s does not match institution %s"
              .formatted(userEmail, institution.getName()));
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

  // helper method for create method, checks to see if card already exists
  private void checkIfCardExists(LibraryCard payload) {
    LibraryCard result;

    if (payload.getUserId() != null) {
      result = libraryCardDAO.findLibraryCardByUserId(payload.getUserId());
    } else if (payload.getUserEmail() != null) {
      result = libraryCardDAO.findLibraryCardByUserEmail(payload.getUserEmail());
    } else {
      throw new BadRequestException();
    }

    if (result != null) {
      Boolean sameUserId =
          payload.getUserId() != null && result.getUserId().equals(payload.getUserId());
      Boolean sameUserEmail =
          payload.getUserEmail() != null
              && result.getUserEmail().equalsIgnoreCase(payload.getUserEmail());
      if (sameUserId || sameUserEmail) {
        throw new ConsentConflictException("Library card already exists for this user.");
      }
    }
  }

  // Helper method to process user data on create LC payload.
  private void processUserOnNewLC(LibraryCard card) {
    // Both userId and userEmail should always be present
    if (card.getUserId() == null || card.getUserEmail() == null) {
      throw new BadRequestException();
    }

    // Verify user exists
    User user = userDAO.findUserById(card.getUserId());
    if (user == null) {
      throw new BadRequestException();
    }

    // Verify emails match
    if (!user.getEmail().equalsIgnoreCase(card.getUserEmail())) {
      throw new ConsentConflictException();
    }

    card.setUserName(user.getDisplayName());
  }
}
