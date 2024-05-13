package org.broadinstitute.consent.http.service;

import com.google.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
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
  private final UserDAO userDAO;

  @Inject
  public LibraryCardService(LibraryCardDAO libraryCardDAO, InstitutionDAO institutionDAO,
      UserDAO userDAO) {
    this.libraryCardDAO = libraryCardDAO;
    this.institutionDAO = institutionDAO;
    this.userDAO = userDAO;
  }

  public LibraryCard createLibraryCard(LibraryCard libraryCard, User user) {
    throwIfNull(libraryCard);
    Boolean isAdmin = checkIsAdmin(user);
    //If user is not an admin, use user's institutionId rather than the value provided in the payload
    if (!isAdmin && !libraryCard.getInstitutionId().equals(user.getInstitutionId())) {
      throw new BadRequestException("Card payload not valid");
    }
    checkIfCardExists(libraryCard);
    checkForValidInstitution(libraryCard.getInstitutionId());
    LibraryCard processedCard = processUserOnNewLC(libraryCard);
    Date createDate = new Date();
    Integer id = libraryCardDAO.insertLibraryCard(
        processedCard.getUserId(),
        processedCard.getInstitutionId(),
        processedCard.getEraCommonsId(),
        processedCard.getUserName(),
        processedCard.getUserEmail(),
        processedCard.getCreateUserId(),
        createDate
    );
    return libraryCardDAO.findLibraryCardById(id);
  }

  public LibraryCard updateLibraryCard(LibraryCard libraryCard, Integer id, Integer userId) {
    LibraryCard updateCard = libraryCardDAO.findLibraryCardById(id);
    throwIfNull(updateCard);
    checkUserId(userId);
    checkForValidUser(libraryCard.getUserId());
    checkForValidInstitution(libraryCard.getInstitutionId());

    Date updateDate = new Date();
    libraryCardDAO.updateLibraryCardById(
        id,
        libraryCard.getUserId(),
        libraryCard.getInstitutionId(),
        libraryCard.getEraCommonsId(),
        libraryCard.getUserName(),
        libraryCard.getUserEmail(),
        userId,
        updateDate
    );
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

  public List<LibraryCard> findLibraryCardsByUserId(Integer userId) {
    return libraryCardDAO.findLibraryCardsByUserId(userId);
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

  public List<LibraryCard> addDaaToUserLibraryCardByInstitution(User user, User signingOfficial, Integer daaId) {
    List<LibraryCard> libraryCards = findLibraryCardsByUserId(user.getUserId());
    List<LibraryCard> matchingLibraryCards = libraryCards.stream()
        .filter(card -> Objects.equals(card.getInstitutionId(), signingOfficial.getInstitutionId()))
        .collect(Collectors.toCollection(ArrayList::new));
    if (matchingLibraryCards.isEmpty()) {
      LibraryCard lc = createLibraryCardForSigningOfficial(user, signingOfficial);
      matchingLibraryCards.add(lc);
    }
    // typically there should be one library card per user per institution
    for (LibraryCard libraryCard : matchingLibraryCards) {
      addDaaToLibraryCard(libraryCard.getId(), daaId);
    }
    return matchingLibraryCards;
  }

  public List<LibraryCard> removeDaaFromUserLibraryCardByInstitution(User user, Integer institutionId, Integer daaId) {
    List<LibraryCard> libraryCards = findLibraryCardsByUserId(user.getUserId());
    List<LibraryCard> matchingLibraryCards = libraryCards.stream()
        .filter(card -> Objects.equals(card.getInstitutionId(), institutionId))
        .toList();
    // typically there should be one library card per user per institution
    for (LibraryCard libraryCard : matchingLibraryCards) {
      removeDaaFromLibraryCard(libraryCard.getId(), daaId);
    }
    return matchingLibraryCards;
  }

  public LibraryCard createLibraryCardForSigningOfficial(User user, User signingOfficial) {
    LibraryCard lc = new LibraryCard();
    lc.setUserId(user.getUserId());
    lc.setInstitutionId(signingOfficial.getInstitutionId());
    lc.setEraCommonsId(user.getEraCommonsId());
    lc.setUserName(user.getDisplayName());
    lc.setUserEmail(user.getEmail());
    lc.setCreateUserId(signingOfficial.getUserId());
    LibraryCard createdLc = createLibraryCard(lc, user);
    return createdLc;
  }

  private void checkForValidInstitution(Integer institutionId) {
    checkInstitutionId(institutionId);
    Institution institution = institutionDAO.findInstitutionById(institutionId);

    if (Objects.isNull(institution)) {
      throw new IllegalArgumentException("Invalid Institution Id");
    }
  }

  private void checkForValidUser(Integer userId) {
    if (Objects.isNull(userId)) {
      return;
    }

    User user = userDAO.findUserById(userId);
    if (Objects.isNull(user)) {
      throw new IllegalArgumentException("Invalid User Id");
    }
  }

  private void checkInstitutionId(Integer institutionId) {
    if (Objects.isNull(institutionId)) {
      throw new IllegalArgumentException("Institution ID is a required parameter");
    }
  }

  private void checkUserId(Integer userId) {
    if (Objects.isNull(userId)) {
      throw new IllegalArgumentException("User ID is a required parameter");
    }
  }

  private void throwIfNull(LibraryCard libraryCard) {
    if (Objects.isNull(libraryCard)) {
      throw new NotFoundException("LibraryCard not found.");
    }
  }

  //helper method for create method, checks to see if card already exists
  private void checkIfCardExists(LibraryCard payload) {
    Integer userId = payload.getUserId();
    String email = payload.getUserEmail();
    Integer institutionId = payload.getInstitutionId();
    Optional<LibraryCard> foundCard;
    List<LibraryCard> results;

    if (Objects.nonNull(payload.getUserId())) {
      results = libraryCardDAO.findLibraryCardsByUserId(userId);
    } else if (Objects.nonNull(email)) {
      results = libraryCardDAO.findAllLibraryCardsByUserEmail(email);
    } else {
      throw new BadRequestException();
    }
    foundCard = results.stream().filter(card -> {
      Boolean sameUserId = Objects.nonNull(userId) && card.getUserId().equals(userId);
      Boolean sameUserEmail = Objects.nonNull(email) && card.getUserEmail().equalsIgnoreCase(email);
      Boolean sameInstitution = card.getInstitutionId().equals(institutionId);
      return (sameUserId || sameUserEmail) && sameInstitution;
    }).findFirst();

    if (foundCard.isPresent()) {
      throw new ConsentConflictException();
    }
  }

  //Helper method to process user data on create LC payload
  //Needed since CREATE has a unique situation where admins can create LCs without an active user (save with userEmail instead)
  private LibraryCard processUserOnNewLC(LibraryCard card) {
    if (Objects.isNull(card.getUserId())) {
      if (Objects.isNull(card.getUserEmail())) {
        throw new BadRequestException();
      } else {
        //If a user is found, update the card to have the correct userId associated
        User user = userDAO.findUserByEmail(card.getUserEmail());
        if (!Objects.isNull(user)) {
          Integer userId = user.getUserId();
          card.setUserId(userId);
        }
      }
    } else {
      //check if userId exists
      User user = userDAO.findUserById(card.getUserId());
      if (Objects.isNull(user)) {
        throw new NotFoundException();
      }
      if (Objects.nonNull(user.getEmail()) && !(user.getEmail()
          .equalsIgnoreCase(card.getUserEmail()))) {
        //throw error here, user is trying to associate incorrect userId with email
        throw new ConsentConflictException();
      }
      card.setUserName(user.getDisplayName());
    }
    return card;
  }

  private Boolean checkIsAdmin(User user) {
    return user.getRoles()
        .stream()
        .anyMatch(role -> role.getName().equalsIgnoreCase(UserRoles.ADMIN.getRoleName()));
  }
}
