package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import org.broadinstitute.consent.http.enumeration.AuditActions;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.DataAccessAgreement;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.models.LibraryCard;
import org.broadinstitute.consent.http.models.LibraryCardDaaAudit;
import org.broadinstitute.consent.http.models.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
  void testInsertLibraryCardFKConstraintErrors() {
    User user = createUser();
    // Test FK on library_card.user_id
    try {
      libraryCardDAO.insertLibraryCard(
          0, user.getDisplayName(), user.getEmail(), user.getUserId(), new Date());
      fail("Should have thrown a FOREIGN_KEY_VIOLATION exception");
    } catch (Exception e) {
      assertEquals(
          PSQLState.FOREIGN_KEY_VIOLATION.getState(), ((PSQLException) e.getCause()).getSQLState());
    }
    // Test FK on library_card.create_user_id
    try {
      libraryCardDAO.insertLibraryCard(
          user.getUserId(), user.getDisplayName(), user.getEmail(), 0, new Date());
      fail("Should have thrown a FOREIGN_KEY_VIOLATION exception");
    } catch (Exception e) {
      assertEquals(
          PSQLState.FOREIGN_KEY_VIOLATION.getState(), ((PSQLException) e.getCause()).getSQLState());
    }
  }

  @Test
  void testInsertLibraryCardUniqueConstraintErrors() {
    User user1 = createUser();
    // Set up LC that will trigger the unique constraints on subsequent inserts
    libraryCardDAO.insertLibraryCard(
        user1.getUserId(), user1.getDisplayName(), user1.getEmail(), user1.getUserId(), new Date());
    // Test Unique on library_card.user_id
    try {
      libraryCardDAO.insertLibraryCard(
          user1.getUserId(),
          user1.getDisplayName(),
          user1.getEmail(),
          user1.getUserId(),
          new Date());
      fail("Should have thrown a UNIQUE_VIOLATION exception");
    } catch (Exception e) {
      assertEquals(
          PSQLState.UNIQUE_VIOLATION.getState(), ((PSQLException) e.getCause()).getSQLState());
    }
    User user2 = createUser();
    // Test Unique on library_card.user_email - note that we're using the same email as user1
    try {
      libraryCardDAO.insertLibraryCard(
          user2.getUserId(),
          user2.getDisplayName(),
          user1.getEmail(),
          user2.getUserId(),
          new Date());
      fail("Should have thrown a UNIQUE_VIOLATION exception");
    } catch (Exception e) {
      assertEquals(
          PSQLState.UNIQUE_VIOLATION.getState(), ((PSQLException) e.getCause()).getSQLState());
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
  void testDeleteLibraryCardByIdNegative() {
    try {
      libraryCardDAO.deleteLibraryCardById(randomInt(1, 1000));
    } catch (Exception e) {
      assertEquals(
          PSQLState.UNIQUE_VIOLATION.getState(), ((PSQLException) e.getCause()).getSQLState());
    }
  }

  @Test
  void testDeleteLibraryCardWithDaaRelationships() {
    // This test creates several relationships:
    // 1. Library Card for a user as a top level object that will be deleted
    // 2. Dac so we can create a DAA
    // 3. DAA so we can link it to a user's Library Card
    // 4. Library Card <-> DAA relationship that represents a Signing Official's acceptance of a DAA
    // for the user
    User signingOfficial = createUser();
    LibraryCard card = createLibraryCard();
    int dacId = dacDAO.createDac(randomAlphabetic(10), randomAlphabetic(10), new Date());
    int daaId =
        daaDAO.createDaa(
            card.getCreateUserId(), Instant.now(), card.getCreateUserId(), Instant.now(), dacId);
    daaDAO.createDacDaaRelation(dacId, daaId, card.getUserId());
    libraryCardDAO.createLibraryCardDaaRelation(
        card.getUserId(), signingOfficial.getUserId(), card.getId(), daaId);

    libraryCardDAO.deleteLibraryCardById(card.getId());
    assertNull(libraryCardDAO.findLibraryCardById(card.getId()));
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
    LibraryCard cardFromDAO = libraryCardDAO.findLibraryCardById(randomInt(100, 200));
    assertNull(cardFromDAO);
  }

  @Test
  void testFindLibraryCardDaaByIdMultipleDaas() {
    User signingOfficial = createUser();
    LibraryCard card = createLibraryCard();
    Integer userId = createUser().getUserId();
    Integer dacId = dacDAO.createDac(randomAlphabetic(5), randomAlphabetic(5), "", new Date());
    Integer daaId1 = daaDAO.createDaa(userId, Instant.now(), userId, Instant.now(), dacId);
    Integer daaId2 = daaDAO.createDaa(userId, Instant.now(), userId, Instant.now(), dacId);
    DataAccessAgreement daa1 = daaDAO.findById(daaId1);
    DataAccessAgreement daa2 = daaDAO.findById(daaId2);
    card.addDaa(daa1.getDaaId());
    card.addDaa(daa2.getDaaId());
    card.addDaaObject(daa1);
    card.addDaaObject(daa2);
    libraryCardDAO.createLibraryCardDaaRelation(
        card.getUserId(), signingOfficial.getUserId(), card.getId(), daaId1);
    libraryCardDAO.createLibraryCardDaaRelation(
        card.getUserId(), signingOfficial.getUserId(), card.getId(), daaId2);
    Integer id = card.getId();
    LibraryCard cardFromDAO = libraryCardDAO.findLibraryCardDaaById(id);

    assertEquals(cardFromDAO.getUserId(), card.getUserId());
    assertEquals(cardFromDAO.getUserName(), card.getUserName());
    assertEquals(cardFromDAO.getUserEmail(), card.getUserEmail());
    assertEquals(cardFromDAO.getCreateUserId(), card.getCreateUserId());
    assertEquals(cardFromDAO.getCreateDate(), card.getCreateDate());
    assertEquals(cardFromDAO.getDaaIds(), card.getDaaIds());

    DataAccessAgreement daaFromDAO1 = cardFromDAO.getDaas().getFirst();
    assertEquals(daaFromDAO1.getDaaId(), daa1.getDaaId());
    assertEquals(daaFromDAO1.getCreateUserId(), daa1.getCreateUserId());
    assertEquals(daaFromDAO1.getCreateDate(), daa1.getCreateDate());
    assertEquals(daaFromDAO1.getUpdateUserId(), daa1.getUpdateUserId());
    assertEquals(daaFromDAO1.getUpdateDate(), daa1.getUpdateDate());
    assertEquals(daaFromDAO1.getInitialDacId(), daa1.getInitialDacId());

    DataAccessAgreement daaFromDAO2 = cardFromDAO.getDaas().get(1);
    assertEquals(daaFromDAO2.getDaaId(), daa2.getDaaId());
    assertEquals(daaFromDAO2.getCreateUserId(), daa2.getCreateUserId());
    assertEquals(daaFromDAO2.getCreateDate(), daa2.getCreateDate());
    assertEquals(daaFromDAO2.getUpdateUserId(), daa2.getUpdateUserId());
    assertEquals(daaFromDAO2.getUpdateDate(), daa2.getUpdateDate());
    assertEquals(daaFromDAO2.getInitialDacId(), daa2.getInitialDacId());
  }

  @Test
  void testFindLibraryCardDaaByIdNoDaas() {
    LibraryCard card = createLibraryCard();
    Integer id = card.getId();
    LibraryCard cardFromDAO = libraryCardDAO.findLibraryCardDaaById(id);

    assertEquals(cardFromDAO.getUserId(), card.getUserId());
    assertEquals(cardFromDAO.getUserName(), card.getUserName());
    assertEquals(cardFromDAO.getUserEmail(), card.getUserEmail());
    assertEquals(cardFromDAO.getCreateUserId(), card.getCreateUserId());
    assertEquals(cardFromDAO.getCreateDate(), card.getCreateDate());
    assertEquals(cardFromDAO.getDaaIds(), card.getDaaIds());
    assertNull(cardFromDAO.getDaas());
  }

  @Test
  void testFindLibraryCardDaaByIdNegative() {
    LibraryCard cardFromDAO = libraryCardDAO.findLibraryCardDaaById(randomInt(100, 200));
    assertNull(cardFromDAO);
  }

  @Test
  void testFindLibraryCardByInstitutionId() {
    Institution institution = createInstitution();
    int userId = institution.getCreateUserId();
    String stringValue = "value";
    Integer lcId =
        libraryCardDAO.insertLibraryCard(userId, stringValue, stringValue, userId, new Date());
    userDAO.updateInstitutionId(userId, institution.getId());
    List<LibraryCard> cardsFromDAO =
        libraryCardDAO.findLibraryCardsByInstitutionId(institution.getId());

    assertNotNull(cardsFromDAO);
    assertEquals(1, cardsFromDAO.size());
    assertEquals(cardsFromDAO.getFirst().getId(), lcId);
    assertTrue(cardsFromDAO.getFirst().getDaaIds().isEmpty());
  }

  @Test
  void testFindLibraryCardByUserIdInstitutionId() {
    User signingOfficial = createUser();
    LibraryCard libraryCard = createLibraryCard();
    int dacId =
        dacDAO.createDac(randomAlphabetic(5), randomAlphabetic(5), randomAlphabetic(5), new Date());
    Instant now = Instant.now();
    int daaId = daaDAO.createDaa(libraryCard.getUserId(), now, libraryCard.getUserId(), now, dacId);
    daaDAO.createDacDaaRelation(dacId, daaId, libraryCard.getUserId());
    libraryCardDAO.createLibraryCardDaaRelation(
        libraryCard.getUserId(), signingOfficial.getUserId(), libraryCard.getId(), daaId);
    LibraryCard cardFromDAO = libraryCardDAO.findLibraryCardByUserId(libraryCard.getUserId());
    assertNotNull(cardFromDAO);
    assertEquals(cardFromDAO, libraryCard);
  }

  @Test
  void testFindAllLibraryCards() {
    List<LibraryCard> cardList = libraryCardDAO.findAllLibraryCards();
    assertEquals(0, cardList.size());
    createLibraryCardForIndex();
    List<LibraryCard> cardListUpdated = libraryCardDAO.findAllLibraryCards();
    assertEquals(1, cardListUpdated.size());
    LibraryCard card = cardListUpdated.getFirst();
    assertTrue(card.getDaaIds().isEmpty());
  }

  @Test
  void testFindLibraryCardByUserEmail() {
    User user = createUser();
    LibraryCard libraryCard = createLibraryCard(user);
    LibraryCard cardFromDAO = libraryCardDAO.findLibraryCardByUserEmail(user.getEmail());
    assertNotNull(cardFromDAO);
    assertEquals(cardFromDAO, libraryCard);
  }

  @Test
  void testFindLibraryCardByUserId() {
    User user = createUser();
    LibraryCard one = createLibraryCard(user);
    LibraryCard cardFromDAO = libraryCardDAO.findLibraryCardByUserId(user.getUserId());
    assertNotNull(cardFromDAO);
    assertEquals(cardFromDAO, one);
  }

  @Test
  void testDeleteLibraryCardByUserId() {
    User user = createUser();
    LibraryCard card = createLibraryCard(user);
    libraryCardDAO.deleteAllLibraryCardsByUser(user.getUserId());
    assertNull(libraryCardDAO.findLibraryCardById(card.getId()));
  }

  @Test
  void testCreateLibraryCardDaaAssociation() {
    User signingOfficial = createUser();
    User user = createUser();
    User user2 = createUser();
    LibraryCard card = createLibraryCard(user);
    LibraryCard card2 = createLibraryCard(user2);
    Integer userId = user.getUserId();
    Integer dacId = dacDAO.createDac(randomAlphabetic(5), randomAlphabetic(5), "", new Date());
    Integer dacId2 = dacDAO.createDac(randomAlphabetic(5), randomAlphabetic(5), "", new Date());
    Integer daaId1 = daaDAO.createDaa(userId, Instant.now(), userId, Instant.now(), dacId);
    Integer daaId2 = daaDAO.createDaa(userId, Instant.now(), userId, Instant.now(), dacId2);
    libraryCardDAO.createLibraryCardDaaRelation(
        card.getUserId(), signingOfficial.getUserId(), card.getId(), daaId1);
    libraryCardDAO.createLibraryCardDaaRelation(
        card.getUserId(), signingOfficial.getUserId(), card.getId(), daaId2);
    libraryCardDAO.createLibraryCardDaaRelation(
        card2.getUserId(), signingOfficial.getUserId(), card2.getId(), daaId1);

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
    User signingOfficial = createUser();
    User user = createUser();
    LibraryCard card = createLibraryCard(user);
    Integer userId = user.getUserId();
    Integer dacId = dacDAO.createDac(randomAlphabetic(5), randomAlphabetic(5), "", new Date());
    Integer daaId1 = daaDAO.createDaa(userId, Instant.now(), userId, Instant.now(), dacId);

    try {
      libraryCardDAO.createLibraryCardDaaRelation(
          card.getUserId(), signingOfficial.getUserId(), card.getId(), 2);
    } catch (Exception e) {
      assertEquals(
          PSQLState.FOREIGN_KEY_VIOLATION.getState(), ((PSQLException) e.getCause()).getSQLState());
    }

    try {
      libraryCardDAO.createLibraryCardDaaRelation(
          card.getUserId(), signingOfficial.getUserId(), 2, daaId1);
    } catch (Exception e) {
      assertEquals(
          PSQLState.FOREIGN_KEY_VIOLATION.getState(), ((PSQLException) e.getCause()).getSQLState());
    }

    List<LibraryCard> lcs = libraryCardDAO.findAllLibraryCards();
    LibraryCard lc1 = lcs.getFirst();
    assertTrue(lc1.getDaaIds().isEmpty());
  }

  @Test
  void testDeleteLibraryCardDaaAssociation() {
    User signingOfficial = createUser();
    User user = createUser();
    User user2 = createUser();
    LibraryCard card = createLibraryCard(user);
    createLibraryCard(user2);
    Integer userId = user.getUserId();
    Integer dacId = dacDAO.createDac(randomAlphabetic(5), randomAlphabetic(5), "", new Date());
    Integer dacId2 = dacDAO.createDac(randomAlphabetic(5), randomAlphabetic(5), "", new Date());
    Integer daaId1 = daaDAO.createDaa(userId, Instant.now(), userId, Instant.now(), dacId);
    Integer daaId2 = daaDAO.createDaa(userId, Instant.now(), userId, Instant.now(), dacId2);
    libraryCardDAO.createLibraryCardDaaRelation(
        card.getUserId(), signingOfficial.getUserId(), card.getId(), daaId1);
    libraryCardDAO.createLibraryCardDaaRelation(
        card.getUserId(), signingOfficial.getUserId(), card.getId(), daaId2);

    List<LibraryCard> lcs = libraryCardDAO.findAllLibraryCards();
    LibraryCard lc1 = lcs.getFirst();
    assertEquals(2, lc1.getDaaIds().size());

    libraryCardDAO.deleteLibraryCardDaaRelation(
        card.getUserId(), signingOfficial.getUserId(), card.getId(), daaId1);
    lcs = libraryCardDAO.findAllLibraryCards();
    lc1 = lcs.getFirst();
    assertEquals(1, lc1.getDaaIds().size());

    libraryCardDAO.deleteLibraryCardDaaRelation(
        card.getUserId(), signingOfficial.getUserId(), card.getId(), daaId2);
    lcs = libraryCardDAO.findAllLibraryCards();
    lc1 = lcs.getFirst();
    assertTrue(lc1.getDaaIds().isEmpty());
  }

  @Test
  void testFindAuditsByLibraryCardUserId() {
    // Create a user, signing official, and library card
    User user = createUser();
    User signingOfficial = createUser();
    Integer lcId =
        libraryCardDAO.insertLibraryCard(
            user.getUserId(),
            user.getDisplayName(),
            user.getEmail(),
            signingOfficial.getUserId(),
            new Date());

    // SO associates user to access agreement 1
    User chairperson1 = createUser();
    Integer dacId1 =
        dacDAO.createDac(
            randomAlphabetic(5), randomAlphabetic(5), chairperson1.getEmail(), new Date());
    Integer daaId1 =
        daaDAO.createDaa(
            chairperson1.getUserId(),
            Instant.now(),
            chairperson1.getUserId(),
            Instant.now(),
            dacId1);
    libraryCardDAO.createLibraryCardDaaRelation(
        user.getUserId(), signingOfficial.getUserId(), lcId, daaId1);

    // SO associates user to access agreement 2
    User chairperson2 = createUser();
    Integer dacId2 =
        dacDAO.createDac(
            randomAlphabetic(5), randomAlphabetic(5), chairperson2.getEmail(), new Date());
    Integer daaId2 =
        daaDAO.createDaa(
            chairperson1.getUserId(),
            Instant.now(),
            chairperson2.getUserId(),
            Instant.now(),
            dacId2);
    libraryCardDAO.createLibraryCardDaaRelation(
        user.getUserId(), signingOfficial.getUserId(), lcId, daaId2);

    List<LibraryCardDaaAudit> audits = libraryCardDAO.findAuditsByLcUserId(user.getUserId());
    assertFalse(audits.isEmpty());
    assertEquals(2, audits.size());
    assertTrue(
        audits.stream()
            .map(LibraryCardDaaAudit::action)
            .allMatch(action -> action == AuditActions.ADD));

    // Dissociate user from DAA 1 and check that we have a REMOVE audit.
    libraryCardDAO.deleteLibraryCardDaaRelation(
        user.getUserId(), signingOfficial.getUserId(), lcId, daaId1);
    List<LibraryCardDaaAudit> audits2 = libraryCardDAO.findAuditsByLcUserId(user.getUserId());
    assertFalse(audits2.isEmpty());
    assertEquals(3, audits2.size());
    assertTrue(audits2.stream().anyMatch(audit -> audit.action() == AuditActions.REMOVE));
  }

  @Test
  void testEnsureAuditInsertsSkippedOnConflict_Add() {
    // Create a user, signing official, and library card
    User user = createUser();
    User signingOfficial = createUser();
    Integer lcId =
        libraryCardDAO.insertLibraryCard(
            user.getUserId(),
            user.getDisplayName(),
            user.getEmail(),
            signingOfficial.getUserId(),
            new Date());

    // SO associates user to data access agreement
    User chairperson = createUser();
    Integer dacId =
        dacDAO.createDac(
            randomAlphabetic(5), randomAlphabetic(5), chairperson.getEmail(), new Date());
    Integer daaId =
        daaDAO.createDaa(
            chairperson.getUserId(), Instant.now(), chairperson.getUserId(), Instant.now(), dacId);
    libraryCardDAO.createLibraryCardDaaRelation(
        user.getUserId(), signingOfficial.getUserId(), lcId, daaId);

    // Repeat the same association which should trigger the ON CONFLICT in the SQL and ensure that
    // we do not insert a duplicate audit record.
    libraryCardDAO.createLibraryCardDaaRelation(
        user.getUserId(), signingOfficial.getUserId(), lcId, daaId);

    List<LibraryCardDaaAudit> audits = libraryCardDAO.findAuditsByLcUserId(user.getUserId());
    assertFalse(audits.isEmpty());
    assertEquals(1, audits.size());
    assertSame(AuditActions.ADD, audits.getFirst().action());
  }

  @Test
  void testEnsureAuditInsertsSkippedOnConflict_Remove() {
    // Create a user, signing official, and library card
    User user = createUser();
    User signingOfficial = createUser();
    Integer lcId =
        libraryCardDAO.insertLibraryCard(
            user.getUserId(),
            user.getDisplayName(),
            user.getEmail(),
            signingOfficial.getUserId(),
            new Date());

    // SO associates user to data access agreement
    User chairperson = createUser();
    Integer dacId =
        dacDAO.createDac(
            randomAlphabetic(5), randomAlphabetic(5), chairperson.getEmail(), new Date());
    Integer daaId =
        daaDAO.createDaa(
            chairperson.getUserId(), Instant.now(), chairperson.getUserId(), Instant.now(), dacId);
    libraryCardDAO.createLibraryCardDaaRelation(
        user.getUserId(), signingOfficial.getUserId(), lcId, daaId);

    // SO Removes association
    libraryCardDAO.deleteLibraryCardDaaRelation(
        user.getUserId(), signingOfficial.getUserId(), lcId, daaId);

    // Repeat the same delection which should trigger the ON CONFLICT in the SQL and ensure that
    // we do not insert a duplicate audit record.
    libraryCardDAO.deleteLibraryCardDaaRelation(
        user.getUserId(), signingOfficial.getUserId(), lcId, daaId);

    List<LibraryCardDaaAudit> audits =
        libraryCardDAO.findAuditsByLcUserId(user.getUserId()).stream()
            .filter(audit -> audit.action() == AuditActions.REMOVE)
            .toList();
    assertFalse(audits.isEmpty());
    assertEquals(1, audits.size());
    assertSame(AuditActions.REMOVE, audits.getFirst().action());
  }

  @Test
  void testFindByUserEmails() {
    User user1 = createUser();
    User user2 = createUser();
    User user3 = createUser();
    LibraryCard card1 = createLibraryCard(user1);
    LibraryCard card2 = createLibraryCard(user2);
    LibraryCard card3 = createLibraryCard(user3);
    // This card will not be returned since its email is not in the list
    LibraryCard card4 = createLibraryCard(createUser());

    List<LibraryCard> cardsFromDAO =
        libraryCardDAO.findByUserEmails(
            List.of(
                user1.getEmail().toLowerCase(), user2.getEmail().toUpperCase(), user3.getEmail()));

    assertEquals(3, cardsFromDAO.size());
    assertTrue(cardsFromDAO.contains(card1));
    assertTrue(cardsFromDAO.contains(card2));
    assertTrue(cardsFromDAO.contains(card3));
    assertFalse(cardsFromDAO.contains(card4));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "",
        " ",
        "invalid-email",
        "user@domain",
        "user@domain.",
        "@domain.com",
        "user@.com",
        "user_@domain.com",
        "user%@domain.com",
        "user*@domain.com",
        "user.@domain.com",
        "user-@domain.com"
      })
  void testFindByUserEmailsWithSpecialCharacters(String email) {
    Integer userId = userDAO.insertUser(email, "display name", null, new Date());
    userRoleDAO.insertSingleUserRole(UserRoles.RESEARCHER.getRoleId(), userId);
    User user = userDAO.findUserById(userId);
    LibraryCard card1 = createLibraryCard(user);
    List<LibraryCard> cardsFromDAO = libraryCardDAO.findByUserEmails(List.of(user.getEmail()));
    assertTrue(cardsFromDAO.contains(card1));
  }

  @Test
  void testFindByUserEmailsWithConflictingSpecialCharacters() {
    List<String> emails =
        List.of(
            "user_@domain.com",
            "user%@domain.com",
            "user*@domain.com",
            "user.@domain.com",
            "user-@domain.com");
    // Create users with emails that have special characters that could conflict with SQL queries
    emails.forEach(
        email -> {
          Integer userId =
              userDAO.insertUser(email, "display name " + randomAlphabetic(25), null, new Date());
          userRoleDAO.insertSingleUserRole(UserRoles.RESEARCHER.getRoleId(), userId);
          User user = userDAO.findUserById(userId);
          createLibraryCard(user);
        });

    // Fetch library cards by emails and ensure that multiples (due to regexes in emails) are not
    // returned
    emails.forEach(
        email -> {
          List<LibraryCard> cards = libraryCardDAO.findByUserEmails(List.of(email));
          assertEquals(1, cards.size(), "Should return exactly one card for email: " + email);
        });
  }

  @Test
  void testFindByUserIds() {
    User user1 = createUser();
    User user2 = createUser();
    User user3 = createUser();
    LibraryCard card1 = createLibraryCard(user1);
    LibraryCard card2 = createLibraryCard(user2);
    LibraryCard card3 = createLibraryCard(user3);
    // This card will not be returned since its email is not in the list
    LibraryCard card4 = createLibraryCard(createUser());

    List<LibraryCard> cardsFromDAO =
        libraryCardDAO.findLibraryCardsByUserIds(
            List.of(user1.getUserId(), user2.getUserId(), user3.getUserId()));

    assertEquals(3, cardsFromDAO.size());
    assertTrue(cardsFromDAO.contains(card1));
    assertTrue(cardsFromDAO.contains(card2));
    assertTrue(cardsFromDAO.contains(card3));
    assertFalse(cardsFromDAO.contains(card4));
  }

  private void createLibraryCardForIndex() {
    Integer userId = createUser().getUserId();
    String stringValue = "value";
    libraryCardDAO.insertLibraryCard(userId, stringValue, stringValue, userId, new Date());
  }

  private Institution createInstitution() {
    return getUserInstitution(createUserWithInstitution());
  }

  private LibraryCard createLibraryCard() {
    Institution institution = createInstitution();
    int userId = institution.getCreateUserId();

    String stringValue = "value";
    Integer id =
        libraryCardDAO.insertLibraryCard(userId, stringValue, stringValue, userId, new Date());
    return libraryCardDAO.findLibraryCardById(id);
  }

  private LibraryCard createLibraryCard(User user) {
    Integer id =
        libraryCardDAO.insertLibraryCard(
            user.getUserId(), user.getDisplayName(), user.getEmail(), user.getUserId(), new Date());
    return libraryCardDAO.findLibraryCardById(id);
  }
}
