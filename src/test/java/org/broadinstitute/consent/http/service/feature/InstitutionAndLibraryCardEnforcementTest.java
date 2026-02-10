package org.broadinstitute.consent.http.service.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.broadinstitute.consent.http.AbstractTestHelper;
import org.broadinstitute.consent.http.db.InstitutionDAO;
import org.broadinstitute.consent.http.db.LibraryCardDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.models.LibraryCard;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.service.dao.UserServiceDAO;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class InstitutionAndLibraryCardEnforcementTest extends AbstractTestHelper {

  @Mock private InstitutionDAO institutionDAO;
  @Mock private LibraryCardDAO libraryCardDAO;
  @Mock private UserDAO userDAO;
  @Mock private UserServiceDAO userServiceDAO;
  @Mock private Jdbi jdbi;

  private InstitutionAndLibraryCardEnforcement service;

  @BeforeEach
  void setUp() {
    when(jdbi.onDemand(InstitutionDAO.class)).thenReturn(institutionDAO);
    when(jdbi.onDemand(LibraryCardDAO.class)).thenReturn(libraryCardDAO);
    when(jdbi.onDemand(UserDAO.class)).thenReturn(userDAO);
    service = new InstitutionAndLibraryCardEnforcement(jdbi, userServiceDAO);
  }

  @Test
  void testAsyncEnforceInstitutionAndLibraryCardRulesForAllUsers() {
    List<User> allUsers =
        IntStream.rangeClosed(1, 10)
            .mapToObj(InstitutionAndLibraryCardEnforcementTest::generateUser)
            .toList();
    when(userDAO.findUsersWithLCsAndInstitution()).thenReturn(allUsers);
    allUsers.forEach(u -> when(userDAO.findUserByEmail(u.getEmail())).thenReturn(u));

    InstitutionAndLibraryCardEnforcement spy = spy(service);
    spy.asyncEnforceInstitutionAndLibraryCardRulesForAllUsers();
    allUsers.forEach(u -> verify(spy, timeout(1000)).enforceInstitutionAndLibraryCardRules(u));
  }

  @Test
  void hasLibraryCard() {
    User testUser = generateUser(1);
    testUser.setLibraryCard(new LibraryCard());
    assertTrue(service.hasLibraryCard(testUser));
  }

  @Test
  void hasLibraryCard_NoLibraryCard() {
    User testUser = generateUser(1);
    assertFalse(service.hasLibraryCard(testUser));
  }

  @Test
  void hasMatchingInstitutionInDatabase() {
    Institution institution = new Institution();
    institution.setId(1);
    assertTrue(service.hasMatchingInstitutionInDatabase(institution, institution));
  }

  @Test
  void hasMatchingInstitutionInDatabase_NoInstitutionInDB() {
    Institution institution = new Institution();
    institution.setId(1);
    assertFalse(service.hasMatchingInstitutionInDatabase(null, institution));
  }

  @Test
  void hasMatchingInstitutionInDatabase_NoInstitutionEmailMap() {
    Institution institution = new Institution();
    institution.setId(1);
    assertFalse(service.hasMatchingInstitutionInDatabase(institution, null));
  }

  @Test
  void handleUserWithInstitutionInMap() {
    User testUser = generateUser(1);
    Institution institutionFromEmail = new Institution();
    institutionFromEmail.setId(1);
    testUser.setInstitutionId(1);
    assertFalse(service.handleUserWithInstitutionInMap(testUser, institutionFromEmail.getId()));
  }

  @Test
  void handleUserWithInstitutionInMap_DifferentInDatabase() {
    User testUser = generateUser(1);
    Institution institutionFromEmail = new Institution();
    institutionFromEmail.setId(1);
    testUser.setInstitutionId(2);
    assertTrue(service.handleUserWithInstitutionInMap(testUser, institutionFromEmail.getId()));
    verify(userDAO).updateInstitutionId(testUser.getUserId(), institutionFromEmail.getId());
  }

  @Test
  void handleUserWithInstitutionInMap_DifferentInDatabaseWithLibraryCard() {
    User testUser = generateUser(1);
    User signingOfficial = generateUser(2);
    LibraryCard lc = new LibraryCard();
    lc.setCreateUserId(signingOfficial.getUserId());
    testUser.setLibraryCard(lc);
    Institution institutionFromEmail = new Institution();
    institutionFromEmail.setId(1);
    testUser.setInstitution(institutionFromEmail);
    Institution institutionFromDatabase = new Institution();
    institutionFromDatabase.setId(2);
    testUser.setInstitutionId(institutionFromDatabase.getId());
    String soDomain = service.trimmedEmailDomain(signingOfficial.getEmail());

    when(userDAO.findUserById(signingOfficial.getUserId())).thenReturn(signingOfficial);
    when(institutionDAO.findInstitutionByDomain(soDomain)).thenReturn(institutionFromDatabase);

    assertTrue(service.handleUserWithInstitutionInMap(testUser, institutionFromEmail.getId()));
    verify(userServiceDAO)
        .updateInstitutionAndClearLibraryCardForUser(
            testUser.getUserId(), institutionFromEmail.getId());
  }

  @Test
  void handleUserWithInstitutionInMap_DifferentInDatabaseWithLibraryCard_SO_NFE() {
    User testUser = new User();
    testUser.setUserId(1);
    testUser.setEmail("user1@example.org");
    testUser.setDisplayName("User One");
    testUser.setInstitutionId(2);

    User signingOfficial = new User();
    signingOfficial.setUserId(2);
    signingOfficial.setEmail("so@example.org");
    signingOfficial.setDisplayName("Signing Official");

    LibraryCard lc = new LibraryCard();
    lc.setCreateUserId(signingOfficial.getUserId());
    testUser.setLibraryCard(lc);

    Institution institutionFromEmail = new Institution();
    institutionFromEmail.setId(1);
    institutionFromEmail.setName("Institution One");
    testUser.setInstitution(institutionFromEmail);

    Institution institutionFromDatabase = new Institution();
    institutionFromDatabase.setId(2);
    institutionFromDatabase.setName("Institution Two");

    // Mock SO not found
    when(userDAO.findUserById(signingOfficial.getUserId())).thenReturn(null);

    assertTrue(service.handleUserWithInstitutionInMap(testUser, institutionFromEmail.getId()));
    verify(userServiceDAO)
        .updateInstitutionAndClearLibraryCardForUser(
            testUser.getUserId(), institutionFromEmail.getId());
  }

  @Test
  void handleUserWithInstitutionInMap_SameInDatabaseWithLC() {
    User testUser = generateUser(1);
    User signingOfficial = generateUser(2);
    LibraryCard lc = new LibraryCard();
    lc.setCreateUserId(signingOfficial.getUserId());
    testUser.setLibraryCard(lc);
    Institution institutionFromEmail = new Institution();
    institutionFromEmail.setId(1);
    testUser.setInstitution(institutionFromEmail);
    testUser.setInstitutionId(1);
    String soDomain = service.trimmedEmailDomain(signingOfficial.getEmail());

    when(userDAO.findUserById(signingOfficial.getUserId())).thenReturn(signingOfficial);
    when(institutionDAO.findInstitutionByDomain(soDomain)).thenReturn(institutionFromEmail);

    assertFalse(service.handleUserWithInstitutionInMap(testUser, institutionFromEmail.getId()));
  }

  @Test
  void handleUserWithInstitutionInMap_SameInDatabaseWithLCFromDifferentOrg() {
    User testUser = generateUser(1);
    User signingOfficial = generateUser(2);
    LibraryCard lc = new LibraryCard();
    lc.setCreateUserId(signingOfficial.getUserId());
    testUser.setLibraryCard(lc);
    Institution institutionFromEmail = new Institution();
    institutionFromEmail.setId(1);
    testUser.setInstitution(institutionFromEmail);
    testUser.setInstitutionId(1);
    String soDomain = service.trimmedEmailDomain(signingOfficial.getEmail());

    when(userDAO.findUserById(signingOfficial.getUserId())).thenReturn(signingOfficial);
    when(institutionDAO.findInstitutionByDomain(soDomain)).thenReturn(null);

    assertTrue(service.handleUserWithInstitutionInMap(testUser, institutionFromEmail.getId()));
    verify(userDAO, times(0)).updateInstitutionId(any(), any());
    verify(libraryCardDAO).deleteAllLibraryCardsByUser(testUser.getUserId());
  }

  @Test
  void needsLibraryCardRemovedForUser() {
    User testUser = generateUser(1);
    Institution institution = new Institution();
    assertFalse(service.needsLibraryCardRemovedForUser(testUser, institution.getId()));
  }

  @Test
  void needsLibraryCardRemovedForUser_SO_NFE() {
    User testUser = generateUser(1);
    Institution institution = new Institution();
    institution.setId(testUser.getInstitutionId());
    User signingOfficial = generateUser(2);
    LibraryCard lc = new LibraryCard();
    lc.setCreateUserId(signingOfficial.getUserId());
    testUser.setLibraryCard(lc);

    when(userDAO.findUserById(signingOfficial.getUserId())).thenReturn(null);
    assertTrue(service.needsLibraryCardRemovedForUser(testUser, institution.getId()));
  }

  @Test
  void needsLibraryCardRemovedForUser_SO_DifferentInstitution() {
    User testUser = generateUser(1);
    User signingOfficial = generateUser(2);
    LibraryCard lc = new LibraryCard();
    lc.setCreateUserId(signingOfficial.getUserId());
    testUser.setLibraryCard(lc);
    Institution institutionFromEmail = new Institution();
    institutionFromEmail.setId(1);
    testUser.setInstitution(institutionFromEmail);
    String soDomain = service.trimmedEmailDomain(signingOfficial.getEmail());
    Institution soInstitution = new Institution();
    soInstitution.setId(2);

    when(userDAO.findUserById(signingOfficial.getUserId())).thenReturn(signingOfficial);
    when(institutionDAO.findInstitutionByDomain(soDomain)).thenReturn(soInstitution);
    assertTrue(service.needsLibraryCardRemovedForUser(testUser, institutionFromEmail.getId()));
  }

  @Test
  void validateEmailsFromSameInstitution_SameInstitution() {
    Institution institution = new Institution();
    institution.setId(1);
    institution.setName("Test Institution");

    String existingEmail = "user1@example.com";
    String newEmail = "user2@example.com";

    when(institutionDAO.findInstitutionByDomain("example.com")).thenReturn(institution);

    // Should not throw exception
    service.validateEmailsFromSameInstitution(existingEmail, newEmail);
  }

  @Test
  void validateEmailsFromSameInstitution_DifferentInstitutions() {
    Institution institution1 = new Institution();
    institution1.setId(1);
    institution1.setName("Institution One");
    institution1.setDomains(List.of("example.com"));

    Institution institution2 = new Institution();
    institution2.setId(2);
    institution2.setName("Institution Two");
    institution2.setDomains(List.of("different.com"));

    String existingEmail = "user1@example.com";
    String newEmail = "user2@different.com";

    when(institutionDAO.findInstitutionByDomain("example.com")).thenReturn(institution1);
    when(institutionDAO.findInstitutionByDomain("different.com")).thenReturn(institution2);

    ForbiddenException exception =
        assertThrows(
            ForbiddenException.class,
            () -> service.validateEmailsFromSameInstitution(existingEmail, newEmail));

    assertTrue(
        exception
            .getMessage()
            .contains(
                "You can only create users with email addresses from your institutional domains: "
                    + institution1.getDomains()));
  }

  public static Stream<Arguments> testEnforceInstitutionAndLibraryCardVariations() {
    User testUser = generateUser(1);
    Institution institution1 = new Institution();
    institution1.setId(1);
    Institution institution2 = new Institution();
    institution2.setId(2);
    LibraryCard libraryCards1 = new LibraryCard();
    LibraryCard libraryCard2 = new LibraryCard();
    libraryCard2.setCreateUserId(1);
    return Stream.of(
        Arguments.of(institution1, testUser, libraryCards1, true),
        Arguments.of(institution2, testUser, libraryCards1, true),
        Arguments.of(institution1, testUser, libraryCard2, false),
        Arguments.of(institution1, testUser, null, false),
        Arguments.of(null, testUser, libraryCards1, true),
        Arguments.of(null, testUser, null, false));
  }

  @ParameterizedTest
  @MethodSource
  void testEnforceInstitutionAndLibraryCardVariations(
      Institution institutionFromMap, User testUser, LibraryCard card, boolean expectsUserMod) {
    testUser.setLibraryCard(card);
    User alteredUser = new User();
    alteredUser.setEmail(testUser.getEmail());
    String domain = service.trimmedEmailDomain(testUser.getEmail());
    if (institutionFromMap != null) {
      when(institutionDAO.findInstitutionIdByDomain(domain)).thenReturn(institutionFromMap.getId());
    } else {
      when(institutionDAO.findInstitutionIdByDomain(domain)).thenReturn(null);
    }
    if (expectsUserMod) {
      when(userDAO.findUserByEmail(testUser.getEmail())).thenReturn(testUser, alteredUser);
      validateAlteredUserIsReturned(
          testUser, service.enforceInstitutionAndLibraryCardRules(testUser.getEmail()));
    } else {
      when(userDAO.findUserByEmail(testUser.getEmail())).thenReturn(testUser);
      validateUserIsUnmodified(
          testUser, service.enforceInstitutionAndLibraryCardRules(testUser.getEmail()));
    }
  }

  @Test
  void testEnforceInstitutionAndLibraryCardThrowsNotFoundExceptionForNewUser() {
    when(userDAO.findUserByEmail(any())).thenReturn(null);
    assertThrows(
        NotFoundException.class,
        () -> service.enforceInstitutionAndLibraryCardRules("hello world!"));
  }

  private void validateUserIsUnmodified(User left, User right) {
    assertEquals(left.getEmail(), right.getEmail());
    assertEquals(left.getInstitutionId(), right.getInstitutionId());
    assertEquals(left.getLibraryCard(), right.getLibraryCard());
    assertEquals(left.getInstitutionId(), right.getInstitutionId());
  }

  private void validateAlteredUserIsReturned(User left, User right) {
    assertEquals(left.getEmail(), right.getEmail());
    assertNotEquals(left.getInstitutionId(), right.getInstitutionId());
  }

  private static User generateUser(int id) {
    User u = new User();
    int i1 = randomInt(10, 50);
    int i2 = randomInt(10, 50);
    int i3 = randomInt(5, 25);
    String email = randomAlphabetic(i1) + "@" + randomAlphabetic(i2) + "." + randomAlphabetic(i3);
    String displayName = randomAlphabetic(i1) + " " + randomAlphabetic(i2);
    u.setEmail(email);
    u.setEraCommonsId(email);
    u.setDisplayName(displayName);
    u.setUserId(id);
    u.setInstitutionId(randomInt(1, 10000));
    u.setInstitution(generateInstitution(u.getInstitutionId()));
    return u;
  }

  private static Institution generateInstitution(Integer id) {
    Institution inst = new Institution();
    inst.setId(id);
    inst.setName(randomAlphabetic(10));
    return inst;
  }
}
