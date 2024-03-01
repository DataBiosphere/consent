package org.broadinstitute.consent.http.service.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.MockitoAnnotations.openMocks;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import javax.xml.crypto.Data;
import org.apache.commons.lang3.RandomStringUtils;
import org.broadinstitute.consent.http.db.DAOTestHelper;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.db.UserRoleDAO;
import org.broadinstitute.consent.http.enumeration.OrganizationType;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataAccessAgreement;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

// This is a utility test to verify a pattern for Database Transactions continues to be supported and works as expected.
// It should be updated to include new patterns that are developed.
public class DaaServiceDAOTest extends DAOTestHelper {

  private DaaServiceDAO serviceDAO;

  @BeforeEach
  public void setup() {
    openMocks(this);
    serviceDAO = new DaaServiceDAO(jdbi, daaDAO);
  }

  @Test
  public void testTransactionPatternHappyPathInActualService() {
    Integer userId = userDAO.insertUser("blah", "blah", new Date());
    Integer dacId = dacDAO.createDac("blah", "blah", "",  new Date());
    Integer dacId2 = dacDAO.createDac("blah", "blah", "",  new Date());
    Integer daaId1 = daaDAO.createDaa(userId, new Date().toInstant(), userId, new Date().toInstant(), dacId);
    DataAccessAgreement daa = daaDAO.findByDacId(1);
    List<Dac> dacList = new ArrayList<>();
    dacList.add(dacDAO.findById(dacId));
    dacList.add(dacDAO.findById(dacId2));
    serviceDAO.insertDAAWithAssociations(daa, dacList);
    DataAccessAgreement fetchedDAA = daaDAO.findByDacId(dacId);
    assertEquals(dacList, fetchedDAA.getAssociatedDacs());
    assertEquals(fetchedDAA.getId(), daaId1);
  }

//  @Test
//  public void testTransactionRollbackAfterMultipleInserts() {
//    boolean exceptionCaught = false;
//    User testUser = createUser();
//    Institution institution = createInstitution();
//    assertTrue(Optional.ofNullable(testUser.getInstitutionId()).isEmpty());
//    UserRole userRole = new UserRole(UserRoles.SIGNINGOFFICIAL.getRoleId(),
//        UserRoles.SIGNINGOFFICIAL.getRoleName());
//    try {
//      //it's necessary to copy the code in from the service dao layer because we're testing that the transaction
//      //does indeed roll back from postgres.  mocking won't confirm that behavior.
//      jdbi.useTransaction(transactionHandle -> {
//        UserDAO userDAOT = transactionHandle.attach(UserDAO.class);
//        UserRoleDAO userRoleDAOT = transactionHandle.attach(UserRoleDAO.class);
//        userDAOT.updateInstitutionId(testUser.getUserId(), institution.getId());
//        userRoleDAOT.insertSingleUserRole(userRole.getRoleId(), testUser.getUserId());
//        throw new RuntimeException("interrupt the transaction.");
//      });
//    } catch (Exception e) {
//      User fetchedUser = userDAO.findUserById(testUser.getUserId());
//      assertEquals(fetchedUser.getUserId(), testUser.getUserId());
//      assertEquals(1, fetchedUser.getRoles().size());
//      assertEquals(UserRoles.RESEARCHER.getRoleId(),
//          fetchedUser.getRoles().get(0).getRoleId());
//      assertNotEquals(fetchedUser.getInstitutionId(), institution.getId());
//      assertTrue(Optional.ofNullable(fetchedUser.getInstitutionId()).isEmpty());
//      exceptionCaught = true;
//    }
//    // Should this test fail because of this assert, something has changed with transaction
//    // support in JDBI that warrants attention.
//    assertTrue(exceptionCaught);
//  }

}
