package org.broadinstitute.consent.http.service.dao;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.MockitoAnnotations.openMocks;

import java.util.Optional;
import org.broadinstitute.consent.http.db.DAOTestHelper;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.db.UserRoleDAO;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

// This is a utility test to verify a pattern for Database Transactions continues to be supported and works as expected.
// It should be updated to include new patterns that are developed.
public class UserServiceDAOTest extends DAOTestHelper {

    private UserServiceDAO serviceDAO;

    @BeforeEach
    public void setup() {
        openMocks(this);
        serviceDAO = new UserServiceDAO(jdbi, userDAO, userRoleDAO);
    }

    @Test
    public void testTransactionPatternHappyPathInActualService() {
        User testUser = createUser();
        Institution institution = createInstitution();
        assertTrue(Optional.ofNullable(testUser.getInstitutionId()).isEmpty());
        UserRole userRole = new UserRole(UserRoles.RESEARCHER.getRoleId(), UserRoles.RESEARCHER.getRoleName());
        serviceDAO.insertRoleAndInstitutionTxn(userRole, institution.getId(), testUser.getUserId());
        User fetchedUser = userDAO.findUserById(testUser.getUserId());
        Assertions.assertEquals(fetchedUser.getUserId(), testUser.getUserId());
        Assertions.assertEquals(fetchedUser.getInstitutionId(), institution.getId());
    }

    @Test
    public void testTransactionRollbackAfterMultipleInserts() {
        boolean exceptionCaught = false;
        User testUser = createUser();
        Institution institution = createInstitution();
        assertTrue(Optional.ofNullable(testUser.getInstitutionId()).isEmpty());
        UserRole userRole = new UserRole(UserRoles.SIGNINGOFFICIAL.getRoleId(), UserRoles.SIGNINGOFFICIAL.getRoleName());
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
            Assertions.assertEquals(fetchedUser.getUserId(), testUser.getUserId());
            Assertions.assertEquals(1, fetchedUser.getRoles().size());
            Assertions.assertEquals(UserRoles.RESEARCHER.getRoleId(),
                fetchedUser.getRoles().get(0).getRoleId());
            Assertions.assertNotEquals(fetchedUser.getInstitutionId(), institution.getId());
            assertTrue(Optional.ofNullable(fetchedUser.getInstitutionId()).isEmpty());
            exceptionCaught = true;
        }
        // Should this test fail because of this assert, something has changed with transaction
        // support in JDBI that warrants attention.
        assertTrue(exceptionCaught);
    }
}
