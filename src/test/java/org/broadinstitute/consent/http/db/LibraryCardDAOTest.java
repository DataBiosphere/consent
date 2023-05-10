package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.models.LibraryCard;
import org.broadinstitute.consent.http.models.User;
import org.junit.jupiter.api.Assertions;
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
            libraryCardDAO.insertLibraryCard(0, institutionId, stringValue, stringValue, stringValue, userId, new Date());
        } catch (Exception e) {
            Assertions.assertEquals(PSQLState.FOREIGN_KEY_VIOLATION.getState(),
                ((PSQLException) e.getCause()).getSQLState());
        }
        try {
            libraryCardDAO.insertLibraryCard(userId, 0, stringValue, stringValue, stringValue, userId, new Date());
        } catch (Exception e) {
            Assertions.assertEquals(PSQLState.FOREIGN_KEY_VIOLATION.getState(),
                ((PSQLException) e.getCause()).getSQLState());
        }
        try {
            libraryCardDAO.insertLibraryCard(userId, institutionId, stringValue, stringValue, stringValue, 0, new Date());
        } catch (Exception e) {
            Assertions.assertEquals(PSQLState.FOREIGN_KEY_VIOLATION.getState(),
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
        libraryCardDAO.updateLibraryCardById(id, userId, institutionId, newValue, newValue, newValue, userId, new Date());
        LibraryCard updated = libraryCardDAO.findLibraryCardById(id);
        Assertions.assertEquals(newValue, updated.getEraCommonsId());
        Assertions.assertEquals(institutionId, updated.getInstitutionId());
        Assertions.assertEquals(newValue, updated.getUserName());
        Assertions.assertEquals(userId, updated.getUpdateUserId());
    }

    @Test
    public void testUpdateLibraryCardByIdNegative() {
        Integer userId = createUser().getUserId();
        Integer institutionId = createInstitution().getId();
        String newValue = "New Value";
        try {
            libraryCardDAO.updateLibraryCardById(0, userId, institutionId, newValue, newValue, newValue, userId, new Date());
        } catch (Exception e) {
            Assertions.assertEquals(PSQLState.UNIQUE_VIOLATION.getState(),
                ((PSQLException) e.getCause()).getSQLState());
        }
    }

    @Test
    public void testDeleteLibraryCardById() {
        LibraryCard card = createLibraryCard();
        Integer id = card.getId();
        libraryCardDAO.deleteLibraryCardById(id);
        Assertions.assertNull(libraryCardDAO.findLibraryCardById(id));
    }

    @Test
    public void testDeleteLbraryCardByIdNegative() {
        try {
            libraryCardDAO.deleteLibraryCardById(RandomUtils.nextInt(1, 1000));
        } catch (Exception e) {
            Assertions.assertEquals(PSQLState.UNIQUE_VIOLATION.getState(),
                ((PSQLException) e.getCause()).getSQLState());
        }
    }

    @Test
    public void testFindLibraryCardById() {
        LibraryCard card = createLibraryCard();
        Integer id = card.getId();
        LibraryCard cardFromDAO = libraryCardDAO.findLibraryCardById(id);
        Assertions.assertEquals(cardFromDAO.getUserId(), card.getUserId());
        Assertions.assertEquals(cardFromDAO.getUserName(), card.getUserName());
        Assertions.assertEquals(cardFromDAO.getUserEmail(), card.getUserEmail());
        Assertions.assertEquals(cardFromDAO.getCreateUserId(), card.getCreateUserId());
        Assertions.assertEquals(cardFromDAO.getCreateDate(), card.getCreateDate());
    }

    @Test
    public void testFindLibraryCardByIdNegative() {
        LibraryCard cardFromDAO = libraryCardDAO.findLibraryCardById(RandomUtils.nextInt(100, 200));
        Assertions.assertNull(cardFromDAO);
    }

    @Test
    public void testFindLibraryCardByInstitutionId() {
        LibraryCard libraryCard = createLibraryCard();
        List<LibraryCard> cardsFromDAO = libraryCardDAO.findLibraryCardsByInstitutionId(libraryCard.getInstitutionId());

        Assertions.assertNotNull(cardsFromDAO);
        Assertions.assertEquals(cardsFromDAO.size(), 1);
        Assertions.assertEquals(cardsFromDAO.get(0).getId(), libraryCard.getId());
    }

    @Test
    public void testFindAllLibraryCards() {
        List<LibraryCard> cardList = libraryCardDAO.findAllLibraryCards();
        Assertions.assertEquals(0, cardList.size());
        Institution institution = createInstitution();
        createLibraryCardForIndex(institution.getId());
        List<LibraryCard> cardListUpdated = libraryCardDAO.findAllLibraryCards();
        Assertions.assertEquals(1, cardListUpdated.size());
        LibraryCard card = cardListUpdated.get(0);
        Institution cardInstitution = card.getInstitution();
        Assertions.assertEquals(institution.getId(), cardInstitution.getId());
        Assertions.assertEquals(institution.getName(), cardInstitution.getName());
    }

    @Test
    public void testFindAllLibraryCardsByUserEmail() {
        User user = createUser();
        LibraryCard libraryCard = createLibraryCard(user);
        List<LibraryCard> libraryCards = libraryCardDAO.findAllLibraryCardsByUserEmail(user.getEmail());
        Assertions.assertNotNull(libraryCards);
        Assertions.assertEquals(1, libraryCards.size());
        Assertions.assertEquals(user.getEmail(), libraryCards.get(0).getUserEmail());
        Assertions.assertEquals(libraryCard.getId(), libraryCards.get(0).getId());
    }

    @Test
    public void testFindAllLibraryCardsByUserId() {
        User user = createUser();
        LibraryCard one = createLibraryCard(user);
        LibraryCard two = createLibraryCard(user);
        List<LibraryCard> libraryCards = libraryCardDAO.findLibraryCardsByUserId(user.getUserId());
        Assertions.assertNotNull(libraryCards);
        Assertions.assertEquals(2, libraryCards.size());
        Assertions.assertEquals(one.getId(), libraryCards.get(0).getId());
        Assertions.assertEquals(two.getId(), libraryCards.get(1).getId());
    }

    @Test
    public void testUpdateEraCommonsForUser() {
        User user = createUser();
        LibraryCard card = createLibraryCard(user);
        Assertions.assertEquals("value", card.getEraCommonsId());
        libraryCardDAO.updateEraCommonsForUser(user.getUserId(), "newEraCommonsId");
        Assertions.assertEquals("newEraCommonsId",
            libraryCardDAO.findLibraryCardById(card.getId()).getEraCommonsId());
    }

    @Test
    public void testDeleteLibraryCardByUserId() {
        User user = createUser();
        LibraryCard card = createLibraryCard(user);
        Assertions.assertEquals("value", card.getEraCommonsId());
        libraryCardDAO.deleteAllLibraryCardsByUser(user.getUserId());
        Assertions.assertNull(libraryCardDAO.findLibraryCardById(card.getId()));
    }
}

