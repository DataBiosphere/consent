package org.broadinstitute.consent.http.db;

import com.google.common.io.Resources;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.broadinstitute.consent.http.AbstractTest;
import org.broadinstitute.consent.http.ConsentApplication;
import org.broadinstitute.consent.http.configurations.ConsentConfiguration;
import org.broadinstitute.consent.http.models.Role;
import org.broadinstitute.consent.http.models.UserRole;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.broadinstitute.consent.http.enumeration.RoleStatus.APPROVED;
import static org.broadinstitute.consent.http.enumeration.RoleStatus.getStatusByValue;
import static org.broadinstitute.consent.http.enumeration.RoleStatus.getValueByStatus;

public class UserRoleDAOTest extends AbstractTest {

    @SuppressWarnings("UnstableApiUsage")
    @ClassRule
    public static final DropwizardAppRule<ConsentConfiguration> RULE = new DropwizardAppRule<>(
            ConsentApplication.class, Resources.getResource("consent-config.yml").getFile());

    @Override
    public DropwizardAppRule<ConsentConfiguration> rule() {
        return RULE;
    }

    private UserRoleDAO userRoleDAO;

    private void resetUserRoleTable() {
        // Many legacy tests use a set of roles defined in `insert.sql`
        // For the purposes of these unit tests, we will reset the table to its initial state
        // for every run, which should leave the system ready for other tests.
        // TODO: In concept, I don't like this approach and hope to migrate all db tests to be
        // independant of one another
        userRoleDAO.deleteAllUserRoles();
        userRoleDAO.insertSingleUserRole(2, 1, false);
        userRoleDAO.insertSingleUserRole(5, 1, false);
        userRoleDAO.updateUserRoleStatus(5, 1, 0, "");
        userRoleDAO.insertSingleUserRole(1, 2, false);
        userRoleDAO.insertSingleUserRole(1, 3, false);
        userRoleDAO.insertSingleUserRole(1, 4, false);
        userRoleDAO.insertSingleUserRole(4, 4, false);
        userRoleDAO.insertSingleUserRole(6, 1, false);
        userRoleDAO.insertSingleUserRole(6, 2, false);
        userRoleDAO.insertSingleUserRole(6, 5, false);
        userRoleDAO.insertSingleUserRole(5, 5, false);
        userRoleDAO.insertSingleUserRole(4, 5, false);
        userRoleDAO.insertSingleUserRole(2, 5, false);
    }

    @Before
    public void setUp() {
        userRoleDAO = getApplicationJdbi().onDemand(UserRoleDAO.class);
        resetUserRoleTable();
    }

    @After
    public void tearDown() {
        resetUserRoleTable();
    }

    @Test
    public void testFindRolesByUserId() {
        // See `insert.sql` - that adds 3 roles for user id 1
        List<UserRole> roles = userRoleDAO.findRolesByUserId(1);
        Assert.assertEquals(3, roles.size());
    }

    @Test
    public void testFindRolesByUserEmail() {
        // See `insert.sql` - that adds 3 roles for user email "test@broad.com"
        List<UserRole> roles = userRoleDAO.findRolesByUserEmail("test@broad.com");
        Assert.assertEquals(3, roles.size());
    }

    @Test
    public void testFindRoles() {
        List<Role> roles = userRoleDAO.findRoles();
        Assert.assertFalse(roles.isEmpty());
    }

    @Test
    public void testFindRoleIdByName() {
        List<String> roleNames =  userRoleDAO.
                findRoles().
                stream().
                map(Role::getName).
                collect(Collectors.toList());
        roleNames.forEach(r -> Assert.assertNotNull(userRoleDAO.findRoleIdByName(r)));
        Assert.assertNull(userRoleDAO.findRoleIdByName("Not a real role"));
    }

    @Test
    public void testInsertUserRoles() {
        UserRole r = new UserRole();
        r.setEmailPreference(false);
        r.setRoleId(1);
        userRoleDAO.insertUserRoles(Collections.singletonList(r), 1);
    }

    @Test
    public void testUpdateUserRoles() {
        List<UserRole> currentRoles = userRoleDAO.findRolesByUserId(3);
        userRoleDAO.updateUserRoles(2, 3, 1);
        List<UserRole> newRoles = userRoleDAO.findRolesByUserId(3);
        Assert.assertFalse(Arrays.equals(currentRoles.toArray(), newRoles.toArray()));
    }

    @Test
    public void testRemoveUserRoles() {
        List<UserRole> currentRoles = userRoleDAO.findRolesByUserId(5);
        Assert.assertFalse(currentRoles.isEmpty());
        List<Integer> roleIds = userRoleDAO.findRoles().stream().map(Role::getRoleId).collect(Collectors.toList());
        userRoleDAO.removeUserRoles(5, roleIds);
        List<UserRole> newRoles = userRoleDAO.findRolesByUserId(5);
        Assert.assertTrue(newRoles.isEmpty());
    }

    @Test
    public void testInsertSingleUserRole() {
        // No-op: tested in set up and tear down
    }

    @Test
    public void testUpdateEmailPreferenceUserRole() {
        List<UserRole> currentRoles = userRoleDAO.findRolesByUserId(1);
        currentRoles.forEach(r -> r.setEmailPreference(true));
        currentRoles.forEach(r -> userRoleDAO.updateEmailPreferenceUserRole(r, 1));
        List<UserRole> newRoles = userRoleDAO.findRolesByUserId(1);
        newRoles.forEach(r -> Assert.assertTrue(r.getEmailPreference()));
    }

    @Test
    public void testRemoveSingleUserRole() {
        List<UserRole> userRoles = userRoleDAO.findRolesByUserId(2);
        Assert.assertFalse(userRoles.isEmpty());
        List<Role> roles = userRoleDAO.findRoles();
        roles.forEach(r ->
                userRoleDAO.removeSingleUserRole(2, r.getRoleId())
        );

        List<UserRole> newUserRoles = userRoleDAO.findRolesByUserId(2);
        Assert.assertTrue(newUserRoles.isEmpty());
    }

    @Test
    public void testFindRoleByNameAndUser() {
        Integer roleId = userRoleDAO.findRoleByNameAndUser("Chairperson", 1);
        Assert.assertNotNull(roleId);

        Integer invalidRoleId = userRoleDAO.findRoleByNameAndUser("Chairperson", 2);
        Assert.assertNull(invalidRoleId);
    }

    @Test
    public void testUpdateUserRoleStatus() {
        int dataOwnerId = 6;
        int userId = 5;
        int roleStatusId = getValueByStatus(APPROVED.name());
        String roleStatusName = getStatusByValue(roleStatusId);
        String rationale = "Approved";
        userRoleDAO.updateUserRoleStatus(
                userId,
                dataOwnerId,
                roleStatusId,
                rationale);

        Optional<UserRole> urOption = userRoleDAO.
                findRolesByUserId(userId).
                stream().
                filter(r -> r.getRoleId() == dataOwnerId).
                findFirst();
        Assert.assertTrue(urOption.isPresent());
        Assert.assertEquals(urOption.get().getStatus(), roleStatusName);
    }

    @Test
    public void testFindRoleByUserIdAndRoleId() {
        // TODO: flesh this out
    }

}
