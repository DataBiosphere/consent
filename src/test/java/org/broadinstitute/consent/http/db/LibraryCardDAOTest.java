package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.models.LibraryCard;
import org.broadinstitute.consent.http.models.User;
import org.junit.jupiter.api.Test;
import org.postgresql.util.PSQLException;
import org.postgresql.util.PSQLState;

public class LibraryCardDAOTest extends DAOTestHelper {

  @Test
  public void testInsertLibraryCard() {
    LibraryCard card = createLibraryCard();
    List<LibraryCard> all = libraryCardDAO.findAllLibraryCards();
    assertTrue(all.contains(card));
  }

  @Test
  public void testInsertLibraryCardNegative() {
    Integer userId = createUser().getUserId();
    Integer institutionId = createInstitution().getId();
    String stringValue = "value";
    try {
      libraryCardDAO.insertLibraryCard(0, institutionId, stringValue, stringValue, stringValue,
          userId, new Date());
    } catch (Exception e) {
      assertEquals(PSQLState.FOREIGN_KEY_VIOLATION.getState(),
          ((PSQLException) e.getCause()).getSQLState());
    }
    try {
      libraryCardDAO.insertLibraryCard(userId, 0, stringValue, stringValue, stringValue, userId,
          new Date());
    } catch (Exception e) {
      assertEquals(PSQLState.FOREIGN_KEY_VIOLATION.getState(),
          ((PSQLException) e.getCause()).getSQLState());
    }
    try {
      libraryCardDAO.insertLibraryCard(userId, institutionId, stringValue, stringValue, stringValue,
          0, new Date());
    } catch (Exception e) {
      assertEquals(PSQLState.FOREIGN_KEY_VIOLATION.getState(),
          ((PSQLException) e.getCause()).getSQLState());
    }
  }

  @Test
  public void testUpdateLibraryCardById() {
    Integer userId = createUser().getUserId();
    String newValue = "New Value";
    LibraryCard card = createLibraryCard();
    Integer id = card.getId();
    card.setUserName("name");
    Integer institutionId = createInstitution().getId();
    libraryCardDAO.updateLibraryCardById(id, userId, institutionId, newValue, newValue, newValue,
        userId, new Date());
    LibraryCard updated = libraryCardDAO.findLibraryCardById(id);
    assertEquals(newValue, updated.getEraCommonsId());
    assertEquals(institutionId, updated.getInstitutionId());
    assertEquals(newValue, updated.getUserName());
    assertEquals(userId, updated.getUpdateUserId());
  }

  @Test
  public void testUpdateLibraryCardByIdNegative() {
    Integer userId = createUser().getUserId();
    Integer institutionId = createInstitution().getId();
    String newValue = "New Value";
    try {
      libraryCardDAO.updateLibraryCardById(0, userId, institutionId, newValue, newValue, newValue,
          userId, new Date());
    } catch (Exception e) {
      assertEquals(PSQLState.UNIQUE_VIOLATION.getState(),
          ((PSQLException) e.getCause()).getSQLState());
    }
  }

  @Test
  public void testDeleteLibraryCardById() {
    LibraryCard card = createLibraryCard();
    Integer id = card.getId();
    libraryCardDAO.deleteLibraryCardById(id);
    assertNull(libraryCardDAO.findLibraryCardById(id));
  }

  @Test
  public void testDeleteLbraryCardByIdNegative() {
    try {
      libraryCardDAO.deleteLibraryCardById(RandomUtils.nextInt(1, 1000));
    } catch (Exception e) {
      assertEquals(PSQLState.UNIQUE_VIOLATION.getState(),
          ((PSQLException) e.getCause()).getSQLState());
    }
  }

  @Test
  public void testFindLibraryCardById() {
    LibraryCard card = createLibraryCard();
    Integer id = card.getId();
    LibraryCard cardFromDAO = libraryCardDAO.findLibraryCardById(id);
    assertEquals(cardFromDAO.getUserId(), card.getUserId());
    assertEquals(cardFromDAO.getUserName(), card.getUserName());
    assertEquals(cardFromDAO.getUserEmail(), card.getUserEmail());
    assertEquals(cardFromDAO.getCreateUserId(), card.getCreateUserId());
    assertEquals(cardFromDAO.getCreateDate(), card.getCreateDate());
  }

  @Test
  public void testFindLibraryCardByIdNegative() {
    LibraryCard cardFromDAO = libraryCardDAO.findLibraryCardById(RandomUtils.nextInt(100, 200));
    assertNull(cardFromDAO);
  }

  @Test
  public void testFindLibraryCardByInstitutionId() {
    LibraryCard libraryCard = createLibraryCard();
    List<LibraryCard> cardsFromDAO = libraryCardDAO.findLibraryCardsByInstitutionId(
        libraryCard.getInstitutionId());

    assertNotNull(cardsFromDAO);
    assertEquals(cardsFromDAO.size(), 1);
    assertEquals(cardsFromDAO.get(0).getId(), libraryCard.getId());
  }

  @Test
  public void testFindAllLibraryCards() {
    List<LibraryCard> cardList = libraryCardDAO.findAllLibraryCards();
    assertEquals(0, cardList.size());
    Institution institution = createInstitution();
    createLibraryCardForIndex(institution.getId());
    List<LibraryCard> cardListUpdated = libraryCardDAO.findAllLibraryCards();
    assertEquals(1, cardListUpdated.size());
    LibraryCard card = cardListUpdated.get(0);
    Institution cardInstitution = card.getInstitution();
    assertEquals(institution.getId(), cardInstitution.getId());
    assertEquals(institution.getName(), cardInstitution.getName());
  }

  @Test
  public void testFindAllLibraryCardsByUserEmail() {
    User user = createUser();
    LibraryCard libraryCard = createLibraryCard(user);
    List<LibraryCard> libraryCards = libraryCardDAO.findAllLibraryCardsByUserEmail(user.getEmail());
    assertNotNull(libraryCards);
    assertEquals(1, libraryCards.size());
    assertEquals(user.getEmail(), libraryCards.get(0).getUserEmail());
    assertEquals(libraryCard.getId(), libraryCards.get(0).getId());
  }

  @Test
  public void testFindAllLibraryCardsByUserId() {
    User user = createUser();
    LibraryCard one = createLibraryCard(user);
    LibraryCard two = createLibraryCard(user);
    List<LibraryCard> libraryCards = libraryCardDAO.findLibraryCardsByUserId(user.getUserId());
    assertNotNull(libraryCards);
    assertEquals(2, libraryCards.size());
    assertEquals(one.getId(), libraryCards.get(0).getId());
    assertEquals(two.getId(), libraryCards.get(1).getId());
  }

  @Test
  public void testUpdateEraCommonsForUser() {
    User user = createUser();
    LibraryCard card = createLibraryCard(user);
    assertEquals("value", card.getEraCommonsId());
    libraryCardDAO.updateEraCommonsForUser(user.getUserId(), "newEraCommonsId");
    assertEquals("newEraCommonsId",
        libraryCardDAO.findLibraryCardById(card.getId()).getEraCommonsId());
  }

  @Test
  public void testDeleteLibraryCardByUserId() {
    User user = createUser();
    LibraryCard card = createLibraryCard(user);
    assertEquals("value", card.getEraCommonsId());
    libraryCardDAO.deleteAllLibraryCardsByUser(user.getUserId());
    assertNull(libraryCardDAO.findLibraryCardById(card.getId()));
  }
}

