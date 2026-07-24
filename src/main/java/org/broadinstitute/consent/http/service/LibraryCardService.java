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
import org.broadinstitute.consent.http.mail.message.NewLibraryCardIssuedMessage;
import org.broadinstitute.consent.http.models.DataAccessAgreement;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.models.LibraryCard;
import org.broadinstitute.consent.http.models.LibraryCardDaaAudit;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.util.ConsentLogger;
import org.jdbi.v3.core.Jdbi;

public class LibraryCardService implements ConsentLogger {

  private final DaaDAO daaDAO;
  private final LibraryCardDAO libraryCardDAO;
  private final InstitutionDAO institutionDAO;
  private final InstitutionService institutionService;
  private final UserDAO userDAO;
  private final EmailService emailService;

  @Inject
  public LibraryCardService(
      Jdbi jdbi, InstitutionService institutionService, EmailService emailService) {
    this.daaDAO = jdbi.onDemand(DaaDAO.class);
    this.libraryCardDAO = jdbi.onDemand(LibraryCardDAO.class);
    this.institutionDAO = jdbi.onDemand(InstitutionDAO.class);
    this.institutionService = institutionService;
    this.userDAO = jdbi.onDemand(UserDAO.class);
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
        sendNewLibraryCardIssuedMessage(toUser);
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

  /**
   * Lightweight existence/id check for a user's library card, avoiding the DAA/audit joins of
   * {@link #findLibraryCardByUserId}. Returns the card id, or null when the user has no card.
   */
  public Integer findLibraryCardIdByUserId(Integer userId) {
    return libraryCardDAO.findLibraryCardIdByUserId(userId);
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
    requireSigningOfficialInstitution(signingOfficial);
    LibraryCard libraryCard = libraryCardDAO.findLibraryCardByUserId(user.getUserId());
    if (libraryCard == null) {
      libraryCard = createLibraryCardForSigningOfficial(user, signingOfficial);
    }
    addDaaToLibraryCard(user.getUserId(), signingOfficial.getUserId(), libraryCard.getId(), daaId);
    return libraryCardDAO.findLibraryCardByUserId(user.getUserId());
  }

  /**
   * The signing-official-level guard that {@link #addDaaToUserLibraryCard} applies to every
   * pre-authorization regardless of whether a library card ends up being created. The atomic bulk
   * pre-authorization add flows (see {@code DaaServiceDAO}) call this once, up front, so a bulk
   * "Approve All" is rejected in the same case the single-link flow would be — even when every
   * targeted user already has a card and no card creation is triggered.
   */
  public void requireSigningOfficialInstitution(User signingOfficial) {
    if (signingOfficial.getInstitutionId() == null) {
      throw new BadRequestException("This signing official does not have an institution.");
    }
  }

  /**
   * Runs — without inserting anything — exactly the validations that {@link #createLibraryCard}
   * performs before persisting a new library card: no card may already exist for the user, the user
   * must exist with a matching email, and the user's email must map to their institution.
   *
   * <p>The atomic bulk pre-authorization flows create library cards inside their own database
   * transaction (see {@code DaaServiceDAO}) and therefore cannot route through {@link
   * #createLibraryCard}; they call this for each user who would have a card auto-created so a bulk
   * "Approve All" rejects that user in every case the per-researcher flow would. The
   * signing-official guard is applied separately and unconditionally via {@link
   * #requireSigningOfficialInstitution}.
   */
  public void validateNewLibraryCardCreation(User user, User signingOfficial) {
    LibraryCard payload = new LibraryCard();
    payload.setUserId(user.getUserId());
    payload.setUserName(user.getDisplayName());
    payload.setUserEmail(user.getEmail());
    payload.setCreateUserId(signingOfficial.getUserId());
    checkIfCardExists(payload);
    processUserOnNewLC(payload);
    checkForValidInstitution(user.getInstitutionId(), payload.getUserEmail());
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

  /**
   * Send a message to the user when they are issued a library card
   *
   * @param toUser The user to send the message to
   * @throws TemplateException Template processing exception
   * @throws IOException IOException when processing the template or sending the email
   */
  public void sendNewLibraryCardIssuedMessage(User toUser) throws TemplateException, IOException {
    emailService.sendMessage(new NewLibraryCardIssuedMessage(toUser), toUser.getUserId());
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
