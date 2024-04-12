package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.List;
import java.util.Random;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.enumeration.OrganizationType;
import org.broadinstitute.consent.http.models.DataAccessAgreement;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.models.LibraryCard;
import org.broadinstitute.consent.http.models.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.postgresql.util.PSQLException;
import org.postgresql.util.PSQLState;

@ExtendWith(MockitoExtension.class)
class LibraryCardDAOTest extends DAOTestHelper {

  @Test
  void testInsertLibraryCard() {
    LibraryCard card = createLibraryCard();
    List<LibraryCard> all = libraryCardDAO.findAllLibraryCards();
    assertTrue(all.contains(card));
  }

  @Test
  void testInsertLibraryCardNegative() {
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
  void testUpdateLibraryCardById() {
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
  void testUpdateLibraryCardByIdNegative() {
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
  void testDeleteLibraryCardById() {
    LibraryCard card = createLibraryCard();
    Integer id = card.getId();
    libraryCardDAO.deleteLibraryCardById(id);
    assertNull(libraryCardDAO.findLibraryCardById(id));
  }

  @Test
  void testDeleteLbraryCardByIdNegative() {
    try {
      libraryCardDAO.deleteLibraryCardById(RandomUtils.nextInt(1, 1000));
    } catch (Exception e) {
      assertEquals(PSQLState.UNIQUE_VIOLATION.getState(),
          ((PSQLException) e.getCause()).getSQLState());
    }
  }

  @Test
  void testFindLibraryCardById() {
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
  void testFindLibraryCardByIdNegative() {
    LibraryCard cardFromDAO = libraryCardDAO.findLibraryCardById(RandomUtils.nextInt(100, 200));
    assertNull(cardFromDAO);
  }

  @Test
  void testFindLibraryCardDaaById() {
    LibraryCard card = createLibraryCard();
    Integer dacId = dacDAO.createDac(RandomStringUtils.randomAlphabetic(5), RandomStringUtils.randomAlphabetic(5), "",  new Date());
    System.out.println("dacId: " + dacId);
    Integer daaId = daaDAO.createDaa(card.getUserId(), new Date().toInstant(), card.getUserId(), new Date().toInstant(), dacId);
    DataAccessAgreement daa = daaDAO.findById(daaId);
    System.out.println("daaId: " + daa.getDaaId());
    System.out.println("create user id: " + daa.getCreateUserId());
    System.out.println("create date: " + daa.getCreateDate());
    System.out.println("update user id: " + daa.getUpdateUserId());
    System.out.println("update date: " + daa.getUpdateDate());
    System.out.println("initial dac id: " + daa.getInitialDacId());
    card.addDaa(daa.getDaaId());
    card.addDaaObject(daa);

    libraryCardDAO.createLibraryCardDaaRelation(card.getId(), daaId);

    Integer id = card.getId();
    LibraryCard cardFromDAO = libraryCardDAO.findLibraryCardDaaById(id);
//    cardFromDAO.addDaa(daa.getDaaId());
    assertEquals(cardFromDAO.getUserId(), card.getUserId());
    assertEquals(cardFromDAO.getUserName(), card.getUserName());
    assertEquals(cardFromDAO.getUserEmail(), card.getUserEmail());
    assertEquals(cardFromDAO.getCreateUserId(), card.getCreateUserId());
    assertEquals(cardFromDAO.getCreateDate(), card.getCreateDate());
    assertEquals(cardFromDAO.getDaaIds(), card.getDaaIds());
    DataAccessAgreement daaFromDAO = cardFromDAO.getDaas().get(0);
    assertEquals(daaFromDAO.getDaaId(), daa.getDaaId());
    assertEquals(daaFromDAO.getCreateUserId(), daa.getCreateUserId());
    assertEquals(daaFromDAO.getCreateDate(), daa.getCreateDate()); //fails
    assertEquals(daaFromDAO.getUpdateUserId(), daa.getUpdateUserId()); // fails
    assertEquals(daaFromDAO.getUpdateDate(), daa.getUpdateDate()); // fails
    assertEquals(daaFromDAO.getInitialDacId(), daa.getInitialDacId()); // fails
  }

  @Test
  void testFindLibraryCardDaaByIdNegative() {
    LibraryCard cardFromDAO = libraryCardDAO.findLibraryCardDaaById(RandomUtils.nextInt(100, 200));
    assertNull(cardFromDAO);
  }

  @Test
  void testFindLibraryCardByInstitutionId() {
    LibraryCard libraryCard = createLibraryCard();
    List<LibraryCard> cardsFromDAO = libraryCardDAO.findLibraryCardsByInstitutionId(
        libraryCard.getInstitutionId());

    assertNotNull(cardsFromDAO);
    assertEquals(cardsFromDAO.size(), 1);
    assertEquals(cardsFromDAO.get(0).getId(), libraryCard.getId());
    assertEquals(cardsFromDAO.get(0).getDaaIds().size(),0);
  }

  @Test
  void testFindAllLibraryCards() {
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
    assertEquals(card.getDaaIds().size(),0);
  }

  @Test
  void testFindAllLibraryCardsByUserEmail() {
    User user = createUser();
    LibraryCard libraryCard = createLibraryCard(user);
    List<LibraryCard> libraryCards = libraryCardDAO.findAllLibraryCardsByUserEmail(user.getEmail());
    assertNotNull(libraryCards);
    assertEquals(1, libraryCards.size());
    assertEquals(user.getEmail(), libraryCards.get(0).getUserEmail());
    assertEquals(libraryCard.getId(), libraryCards.get(0).getId());
  }

  @Test
  void testFindAllLibraryCardsByUserId() {
    User user = createUser();
    LibraryCard one = createLibraryCard(user);
    LibraryCard two = createLibraryCard(user);
    List<LibraryCard> libraryCards = libraryCardDAO.findLibraryCardsByUserId(user.getUserId());
    assertNotNull(libraryCards);
    assertEquals(2, libraryCards.size());
    assertEquals(one.getId(), libraryCards.get(0).getId());
    assertEquals(two.getId(), libraryCards.get(1).getId());
    assertEquals(one.getDaaIds().size(),0);
    assertEquals(two.getDaaIds().size(),0);
  }

  @Test
  void testUpdateEraCommonsForUser() {
    User user = createUser();
    LibraryCard card = createLibraryCard(user);
    assertEquals("value", card.getEraCommonsId());
    libraryCardDAO.updateEraCommonsForUser(user.getUserId(), "newEraCommonsId");
    assertEquals("newEraCommonsId",
        libraryCardDAO.findLibraryCardById(card.getId()).getEraCommonsId());
  }

  @Test
  void testDeleteLibraryCardByUserId() {
    User user = createUser();
    LibraryCard card = createLibraryCard(user);
    assertEquals("value", card.getEraCommonsId());
    libraryCardDAO.deleteAllLibraryCardsByUser(user.getUserId());
    assertNull(libraryCardDAO.findLibraryCardById(card.getId()));
  }

  @Test
  void testCreateLibraryCardDaaAssociation() {
    User user = createUser();
    User user2 = createUser();
    LibraryCard card = createLibraryCard(user);
    LibraryCard card2 = createLibraryCard(user2);
    Integer userId = user.getUserId();
    Integer dacId = dacDAO.createDac(RandomStringUtils.randomAlphabetic(5), RandomStringUtils.randomAlphabetic(5), "",  new Date());
    Integer dacId2 = dacDAO.createDac(RandomStringUtils.randomAlphabetic(5), RandomStringUtils.randomAlphabetic(5), "",  new Date());
    Integer daaId1 = daaDAO.createDaa(userId, new Date().toInstant(), userId, new Date().toInstant(), dacId);
    Integer daaId2 = daaDAO.createDaa(userId, new Date().toInstant(), userId, new Date().toInstant(), dacId2);
    libraryCardDAO.createLibraryCardDaaRelation(card.getId(), daaId1);
    libraryCardDAO.createLibraryCardDaaRelation(card.getId(), daaId2);
    libraryCardDAO.createLibraryCardDaaRelation(card2.getId(), daaId1);

    List<LibraryCard> lcs = libraryCardDAO.findAllLibraryCards();
    LibraryCard lc1 = lcs.get(0);
    LibraryCard lc2 = lcs.get(1);
    assertNotNull(lc1.getDaaIds());
    assertEquals(2, lc1.getDaaIds().size());
    assertNotNull(lc2.getDaaIds());
    assertEquals(1, lc2.getDaaIds().size());
  }

  @Test
  void testCreateLibraryCardDaaAssociationInvalid() {
    User user = createUser();
    LibraryCard card = createLibraryCard(user);
    Integer userId = user.getUserId();
    Integer dacId = dacDAO.createDac(RandomStringUtils.randomAlphabetic(5), RandomStringUtils.randomAlphabetic(5), "",  new Date());
    Integer daaId1 = daaDAO.createDaa(userId, new Date().toInstant(), userId, new Date().toInstant(), dacId);

    try {
      libraryCardDAO.createLibraryCardDaaRelation(card.getId(), 2);
    } catch (Exception e) {
      assertEquals(PSQLState.FOREIGN_KEY_VIOLATION.getState(),
          ((PSQLException) e.getCause()).getSQLState());
    }

    try {
      libraryCardDAO.createLibraryCardDaaRelation(2, daaId1);
    } catch (Exception e) {
      assertEquals(PSQLState.FOREIGN_KEY_VIOLATION.getState(),
          ((PSQLException) e.getCause()).getSQLState());
    }

    List<LibraryCard> lcs = libraryCardDAO.findAllLibraryCards();
    LibraryCard lc1 = lcs.get(0);
    assertEquals(lc1.getDaaIds().size(),0);
  }

  @Test
  void testDeleteLibraryCardDaaAssociation() {
    User user = createUser();
    User user2 = createUser();
    LibraryCard card = createLibraryCard(user);
    LibraryCard card2 = createLibraryCard(user2);
    Integer userId = user.getUserId();
    Integer dacId = dacDAO.createDac(RandomStringUtils.randomAlphabetic(5), RandomStringUtils.randomAlphabetic(5), "",  new Date());
    Integer dacId2 = dacDAO.createDac(RandomStringUtils.randomAlphabetic(5), RandomStringUtils.randomAlphabetic(5), "", new Date());
    Integer daaId1 = daaDAO.createDaa(userId, new Date().toInstant(), userId, new Date().toInstant(), dacId);
    Integer daaId2 = daaDAO.createDaa(userId, new Date().toInstant(), userId, new Date().toInstant(), dacId2);
    libraryCardDAO.createLibraryCardDaaRelation(card.getId(), daaId1);
    libraryCardDAO.createLibraryCardDaaRelation(card.getId(), daaId2);

    List<LibraryCard> lcs = libraryCardDAO.findAllLibraryCards();
    LibraryCard lc1 = lcs.get(0);
    assertEquals(2, lc1.getDaaIds().size());

    libraryCardDAO.deleteLibraryCardDaaRelation(card.getId(), daaId1);lcs = libraryCardDAO.findAllLibraryCards();
    lc1 = lcs.get(0);
    assertEquals(1, lc1.getDaaIds().size());

    libraryCardDAO.deleteLibraryCardDaaRelation(card.getId(), daaId2);lcs = libraryCardDAO.findAllLibraryCards();
    lc1 = lcs.get(0);
    assertEquals(lc1.getDaaIds().size(),0);
  }


  private LibraryCard createLibraryCardForIndex(Integer institutionId) {
    Integer userId = createUser().getUserId();
    String stringValue = "value";
    Integer id = libraryCardDAO.insertLibraryCard(userId, institutionId, stringValue, stringValue,
        stringValue,
        userId, new Date());
    return libraryCardDAO.findLibraryCardById(id);
  }

  private Institution createInstitution() {
    User createUser = createUser();
    Integer id = institutionDAO.insertInstitution(RandomStringUtils.randomAlphabetic(20),
        "itDirectorName",
        "itDirectorEmail",
        RandomStringUtils.randomAlphabetic(10),
        new Random().nextInt(),
        RandomStringUtils.randomAlphabetic(10),
        RandomStringUtils.randomAlphabetic(10),
        RandomStringUtils.randomAlphabetic(10),
        OrganizationType.NON_PROFIT.getValue(),
        createUser.getUserId(),
        createUser.getCreateDate());
    Institution institution = institutionDAO.findInstitutionById(id);
    User updateUser = createUser();
    institutionDAO.updateInstitutionById(
        id,
        institution.getName(),
        institution.getItDirectorEmail(),
        institution.getItDirectorName(),
        institution.getInstitutionUrl(),
        institution.getDunsNumber(),
        institution.getOrgChartUrl(),
        institution.getVerificationUrl(),
        institution.getVerificationFilename(),
        institution.getOrganizationType().getValue(),
        updateUser.getUserId(),
        new Date()
    );
    return institutionDAO.findInstitutionById(id);
  }

  private LibraryCard createLibraryCard() {
    Integer institutionId = createInstitution().getId();
    String email = RandomStringUtils.randomAlphabetic(11);
    Integer userId = userDAO.insertUser(email, "displayName", new Date());
    userDAO.updateUser(email, userId, institutionId);
    String stringValue = "value";
    Integer id = libraryCardDAO.insertLibraryCard(userId, institutionId, stringValue, stringValue,
        stringValue, userId, new Date());
    return libraryCardDAO.findLibraryCardById(id);
  }

  private LibraryCard createLibraryCard(User user) {
    Integer institutionId = createInstitution().getId();
    String stringValue = "value";
    Integer id = libraryCardDAO.insertLibraryCard(user.getUserId(), institutionId, stringValue,
        user.getDisplayName(), user.getEmail(), user.getUserId(), new Date());
    return libraryCardDAO.findLibraryCardById(id);
  }

}

