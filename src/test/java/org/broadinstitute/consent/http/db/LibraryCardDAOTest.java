package org.broadinstitute.consent.http.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.List;
import org.postgresql.util.PSQLException;
import org.postgresql.util.PSQLState;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.models.LibraryCard;
import org.junit.Test;

public class LibraryCardDAOTest extends DAOTestHelper {

  @Test
  public void testInsertLibraryCard() {
    LibraryCard card = createLibraryCard();
    List<LibraryCard> all = libraryCardDAO.findAllLibraryCards();
    assertTrue(all.contains(card));
  }

  @Test
  public void testInsertLibraryCardNegative() {
    Integer userId = createUser().getDacUserId();
    Integer institutionId = createInstitution().getId();
    String stringValue = "value";
    try { 
        libraryCardDAO.insertLibraryCard(0, institutionId, stringValue, stringValue, stringValue, userId, new Date());
    } catch (Exception e) {
        assertEquals(PSQLState.FOREIGN_KEY_VIOLATION, ((PSQLException)e.getCause()).getSQLState());
    }
    try { 
        libraryCardDAO.insertLibraryCard(userId, 0, stringValue, stringValue, stringValue, userId, new Date());
    } catch (Exception e) {
        assertEquals(PSQLState.FOREIGN_KEY_VIOLATION, ((PSQLException)e.getCause()).getSQLState());
    }
    try { 
        libraryCardDAO.insertLibraryCard(userId, institutionId, stringValue, stringValue, stringValue, 0, new Date());
    } catch (Exception e) {
        assertEquals(PSQLState.FOREIGN_KEY_VIOLATION, ((PSQLException)e.getCause()).getSQLState());
    }
  }

  @Test
  public void testUpdateLibraryCardById() {
    Integer userId = createUser().getDacUserId();
    String newValue = "New Value";
    LibraryCard card = createLibraryCard();
    Integer institutionId = createInstitution().getId();
    libraryCardDAO.updateLibraryCardById(card.getId(), userId, institutionId, newValue, newValue, newValue, userId, new Date());
    LibraryCard updated = libraryCardDAO.findLibraryCardById(card.getId());
    assertEquals(updated.getName(), newValue);
    assertEquals(updated.getEmail(), newValue);
    assertEquals(updated.getUpdateUser(), userId);
  }

  @Test
  public void testUpdateLibraryCardByIdNegative() {
    Integer userId = createUser().getDacUserId();
    Integer institutionId = createInstitution().getId();
    String newValue = "New Value";
    try {
      libraryCardDAO.updateLibraryCardById(0, userId, institutionId, newValue, newValue, newValue, userId, new Date());
    } catch (Exception e) {
        assertEquals("23505", ((PSQLException)e.getCause()).getSQLState());
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
    try{
      libraryCardDAO.deleteLibraryCardById(RandomUtils.nextInt(1, 1000));
    } catch (Exception e) {
      assertEquals("23505", ((PSQLException)e.getCause()).getSQLState());
    }
  }

  @Test
  public void testFindLibraryCardById() {
    LibraryCard card = createLibraryCard();
    Integer id = card.getId();
    LibraryCard cardFromDAO = libraryCardDAO.findLibraryCardById(id);
    assertEquals(cardFromDAO.getUserId(), card.getUserId());
    assertEquals(cardFromDAO.getName(), card.getName());
    assertEquals(cardFromDAO.getEmail(), card.getEmail());
    assertEquals(cardFromDAO.getCreateUser(), card.getCreateUser());
    assertEquals(cardFromDAO.getCreateDate(), card.getCreateDate());
  }

  @Test
  public void testFindLibraryCardByIdNegative() {
    LibraryCard cardFromDAO = libraryCardDAO.findLibraryCardById(RandomUtils.nextInt(100, 200));
    assertNull(cardFromDAO);
  }

  @Test
  public void testFindAllLibraryCards() {
    List<LibraryCard> cardList = libraryCardDAO.findAllLibraryCards();
    assertEquals(0, cardList.size());
    createLibraryCard();
    List<LibraryCard> cardListUpdated = libraryCardDAO.findAllLibraryCards();
    assertEquals(1, cardListUpdated.size());
  }
}
