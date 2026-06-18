package org.broadinstitute.consent.http.service.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import org.broadinstitute.consent.http.db.DAOTestHelper;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.db.UserRoleDAO;
import org.broadinstitute.consent.http.enumeration.OrganizationType;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

// This is a utility test to verify a pattern for Database Transactions continues to be supported
// and works as expected.
// It should be updated to include new patterns that are developed.
@ExtendWith(MockitoExtension.class)
class UserServiceDAOTest extends DAOTestHelper {

  private UserServiceDAO serviceDAO;

  @BeforeEach
  void setup() {
    serviceDAO = new UserServiceDAO(jdbi);
  }

  @Test
  void testTransactionPatternHappyPathInActualService() throws Exception {
    User testUser = createUser();
    Institution institution = createInstitution();
    assertTrue(Optional.ofNullable(testUser.getInstitutionId()).isEmpty());
    UserRole userRole = UserRoles.Researcher();
    serviceDAO.insertRoleAndInstitutionTxn(userRole, institution.getId(), testUser.getUserId());
    User fetchedUser = userDAO.findUserById(testUser.getUserId());
    assertEquals(fetchedUser.getUserId(), testUser.getUserId());
    assertEquals(fetchedUser.getInstitutionId(), institution.getId());
  }

  @Test
  void testTransactionRollbackAfterMultipleInserts() throws Exception {
    boolean exceptionCaught = false;
    User testUser = createUser();
    Institution institution = createInstitution();
    assertTrue(Optional.ofNullable(testUser.getInstitutionId()).isEmpty());
    UserRole userRole = UserRoles.SigningOfficial();
    try {
      // it's necessary to copy the code in from the service dao layer because we're testing that
      // the transaction
      // does indeed roll back from postgres.  mocking won't confirm that behavior.
      jdbi.useTransaction(
          transactionHandle -> {
            UserDAO userDAOT = transactionHandle.attach(UserDAO.class);
            UserRoleDAO userRoleDAOT = transactionHandle.attach(UserRoleDAO.class);
            userDAOT.updateInstitutionId(testUser.getUserId(), institution.getId());
            userRoleDAOT.insertSingleUserRole(userRole.getRoleId(), testUser.getUserId());
            throw new RuntimeException("interrupt the transaction.");
          });
    } catch (Exception _) {
      User fetchedUser = userDAO.findUserById(testUser.getUserId());
      assertEquals(fetchedUser.getUserId(), testUser.getUserId());
      assertEquals(1, fetchedUser.getRoles().size());
      assertEquals(UserRoles.RESEARCHER.getRoleId(), fetchedUser.getRoles().getFirst().getRoleId());
      assertNotEquals(fetchedUser.getInstitutionId(), institution.getId());
      assertTrue(Optional.ofNullable(fetchedUser.getInstitutionId()).isEmpty());
      exceptionCaught = true;
    }
    // Should this test fail because of this assert, something has changed with transaction
    // support in JDBI that warrants attention.
    assertTrue(exceptionCaught);
  }

  private Institution createInstitution() throws Exception {
    User createUser = createUser();
    Institution i = new Institution();
    i.setName(randomAlphabetic(10));
    i.setItDirectorName(randomAlphabetic(10));
    i.setItDirectorName("itDirectorName");
    i.setItDirectorEmail("itDirectorEmail");
    i.setInstitutionUrl(randomAlphabetic(10));
    i.setDunsNumber(new Random().nextInt());
    i.setOrgChartUrl(randomAlphabetic(10));
    i.setVerificationUrl(randomAlphabetic(10));
    i.setVerificationFilename(randomAlphabetic(10));
    i.setOrganizationType(OrganizationType.NON_PROFIT);
    i.setCreateUserId(createUser.getUserId());
    i.setDomains(List.of("test.org"));
    Institution institution = institutionDAO.insertFullInstitution(i, createUser.getUserId());

    User updateUser = createUser();
    institutionDAO.updateInstitutionById(
        institution.getId(),
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
        new Date());
    return institutionDAO.findInstitutionById(institution.getId());
  }

  @Test
  void testUpdateInstitutionAndClearLibraryCardForUser() throws Exception {
    User testUser = createUser();
    Institution institution = createInstitution();

    serviceDAO.updateInstitutionAndClearLibraryCardForUser(
        testUser.getUserId(), institution.getId());
    User fetchedUser = userDAO.findUserById(testUser.getUserId());
    assertEquals(fetchedUser.getUserId(), testUser.getUserId());
    assertEquals(fetchedUser.getInstitutionId(), institution.getId());
    assertNull(libraryCardDAO.findLibraryCardByUserId(testUser.getUserId()));
  }

  @Test
  void testUpdateInstitutionAndClearLibraryCardForUser_ClearInstitution() throws Exception {
    User testUser = createUser();
    Institution institution = createInstitution();

    serviceDAO.updateInstitutionAndClearLibraryCardForUser(
        testUser.getUserId(), institution.getId());
    User fetchedUser = userDAO.findUserById(testUser.getUserId());
    assertEquals(fetchedUser.getUserId(), testUser.getUserId());
    assertEquals(fetchedUser.getInstitutionId(), institution.getId());

    serviceDAO.updateInstitutionAndClearLibraryCardForUser(testUser.getUserId(), null);
    fetchedUser = userDAO.findUserById(testUser.getUserId());
    assertEquals(fetchedUser.getUserId(), testUser.getUserId());
    assertNull(fetchedUser.getInstitutionId());
  }

  @Test
  void testUpdateInstitutionAndClearLibraryCardForUser_ClearLibraryCardAndInstitution()
      throws Exception {
    User testUser = createUser();
    Institution institution = createInstitution();
    userDAO.updateInstitutionId(testUser.getUserId(), institution.getId());
    libraryCardDAO.insertLibraryCard(
        testUser.getUserId(),
        testUser.getDisplayName(),
        testUser.getEmail(),
        testUser.getUserId(),
        Timestamp.from(Instant.now()));
    assertNotNull(libraryCardDAO.findLibraryCardByUserId(testUser.getUserId()));
    User fetchedUser = userDAO.findUserById(testUser.getUserId());
    assertEquals(fetchedUser.getUserId(), testUser.getUserId());
    assertEquals(fetchedUser.getInstitutionId(), institution.getId());

    serviceDAO.updateInstitutionAndClearLibraryCardForUser(testUser.getUserId(), null);
    assertNull(libraryCardDAO.findLibraryCardByUserId(testUser.getUserId()));
    assertNull(institutionDAO.findInstitutionById(testUser.getInstitutionId()));
  }

  @Test
  void testCreateUser() throws Exception {
    Institution i = createInstitution();
    User u = generateUnsavedUser(i);
    List<UserRole> roles = List.of(generateRole(UserRoles.RESEARCHER.getRoleId()));
    u.setRoles(roles);
    User user = serviceDAO.createUser(u);
    assertTrue(user.getUserId() > 0);
    assertEquals(u.getEmail(), user.getEmail());
    assertEquals(u.getDisplayName(), user.getDisplayName());
    assertEquals(u.getInstitutionId(), user.getInstitutionId());
    assertNotNull(user.getCreateDate());
  }

  @Test
  void testCreateUserNoInstitution() {
    User u = generateUnsavedUser(null);
    List<UserRole> roles = List.of(generateRole(UserRoles.RESEARCHER.getRoleId()));
    u.setRoles(roles);
    User user = serviceDAO.createUser(u);
    assertTrue(user.getUserId() > 0);
    assertEquals(u.getEmail(), user.getEmail());
    assertEquals(u.getDisplayName(), user.getDisplayName());
    assertNull(user.getInstitutionId());
    assertNotNull(user.getCreateDate());
  }

  @Test
  void testCreateUserWithLibraryCard() throws Exception {
    Institution i = createInstitution();
    User u = generateUnsavedUser(i);
    libraryCardDAO.insertLibraryCard(
        null, u.getDisplayName(), u.getEmail(), i.getCreateUserId(), new Date());
    List<UserRole> roles = List.of(generateRole(UserRoles.RESEARCHER.getRoleId()));
    u.setRoles(roles);
    User user = serviceDAO.createUser(u);
    assertNotNull(user.getLibraryCard());
  }

  @Test
  void testCreateUserNoRoles() throws Exception {
    Institution i = createInstitution();
    User u = generateUnsavedUser(i);
    assertThrows(IllegalArgumentException.class, () -> serviceDAO.createUser(u));
  }

  private static User generateUnsavedUser(Institution institution) {
    User u = new User();
    int i1 = randomInt(10, 50);
    int i2 = randomInt(10, 50);
    String email;
    if (institution != null) {
      assertNotNull(institution.getDomains());
      assertFalse(institution.getDomains().isEmpty());
      email = randomAlphabetic(i1) + "@" + institution.getDomains().getFirst();
    } else {
      email = randomAlphabetic(i1) + "@" + randomAlphabetic(i1) + ".org";
    }
    String displayName = randomAlphabetic(i1) + " " + randomAlphabetic(i2);
    u.setEmail(email);
    u.setEraCommonsId(email);
    u.setDisplayName(displayName);
    u.setUserId(randomInt(1, 100));
    u.setInstitutionId(institution == null ? null : institution.getId());
    return u;
  }

  private UserRole generateRole(int roleId) {
    UserRoles rolesEnum = UserRoles.getUserRoleFromId(roleId);
    assert rolesEnum != null;
    return new UserRole(rolesEnum.getRoleId(), rolesEnum.getRoleName());
  }
}
