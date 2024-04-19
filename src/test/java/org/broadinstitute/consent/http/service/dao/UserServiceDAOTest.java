package org.broadinstitute.consent.http.service.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.Optional;
import java.util.Random;
import org.apache.commons.lang3.RandomStringUtils;
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

// This is a utility test to verify a pattern for Database Transactions continues to be supported and works as expected.
// It should be updated to include new patterns that are developed.
@ExtendWith(MockitoExtension.class)
class UserServiceDAOTest extends DAOTestHelper {

  private UserServiceDAO serviceDAO;

  @BeforeEach
  void setup() {
    serviceDAO = new UserServiceDAO(jdbi, userDAO, userRoleDAO);
  }

  @Test
  void testTransactionPatternHappyPathInActualService() {
    User testUser = createUser();
    Institution institution = createInstitution();
    assertTrue(Optional.ofNullable(testUser.getInstitutionId()).isEmpty());
    UserRole userRole = new UserRole(UserRoles.RESEARCHER.getRoleId(),
        UserRoles.RESEARCHER.getRoleName());
    serviceDAO.insertRoleAndInstitutionTxn(userRole, institution.getId(), testUser.getUserId());
    User fetchedUser = userDAO.findUserById(testUser.getUserId());
    assertEquals(fetchedUser.getUserId(), testUser.getUserId());
    assertEquals(fetchedUser.getInstitutionId(), institution.getId());
  }

  @Test
  void testTransactionRollbackAfterMultipleInserts() {
    boolean exceptionCaught = false;
    User testUser = createUser();
    Institution institution = createInstitution();
    assertTrue(Optional.ofNullable(testUser.getInstitutionId()).isEmpty());
    UserRole userRole = new UserRole(UserRoles.SIGNINGOFFICIAL.getRoleId(),
        UserRoles.SIGNINGOFFICIAL.getRoleName());
    try {
      //it's necessary to copy the code in from the service dao layer because we're testing that the transaction
      //does indeed roll back from postgres.  mocking won't confirm that behavior.
      jdbi.useTransaction(transactionHandle -> {
        UserDAO userDAOT = transactionHandle.attach(UserDAO.class);
        UserRoleDAO userRoleDAOT = transactionHandle.attach(UserRoleDAO.class);
        userDAOT.updateInstitutionId(testUser.getUserId(), institution.getId());
        userRoleDAOT.insertSingleUserRole(userRole.getRoleId(), testUser.getUserId());
        throw new RuntimeException("interrupt the transaction.");
      });
    } catch (Exception e) {
      User fetchedUser = userDAO.findUserById(testUser.getUserId());
      assertEquals(fetchedUser.getUserId(), testUser.getUserId());
      assertEquals(1, fetchedUser.getRoles().size());
      assertEquals(UserRoles.RESEARCHER.getRoleId(),
          fetchedUser.getRoles().get(0).getRoleId());
      assertNotEquals(fetchedUser.getInstitutionId(), institution.getId());
      assertTrue(Optional.ofNullable(fetchedUser.getInstitutionId()).isEmpty());
      exceptionCaught = true;
    }
    // Should this test fail because of this assert, something has changed with transaction
    // support in JDBI that warrants attention.
    assertTrue(exceptionCaught);
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

}
