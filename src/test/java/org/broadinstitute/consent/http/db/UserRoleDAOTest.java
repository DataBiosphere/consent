package org.broadinstitute.consent.http.db;

import com.google.common.io.Resources;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.broadinstitute.consent.http.AbstractTest;
import org.broadinstitute.consent.http.ConsentApplication;
import org.broadinstitute.consent.http.configurations.ConsentConfiguration;
import org.broadinstitute.consent.http.enumeration.UserRoles;
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
import java.util.stream.Collectors;

public class UserRoleDAOTest extends AbstractTest {

    private static final int TEST_USER_1_ID = 1;
    private static final int TEST_USER_2_ID = 2;
    private static final int TEST_USER_3_ID = 3;
    private static final int TEST_USER_4_ID = 4;
    private static final int TEST_USER_5_ID = 5;

    @SuppressWarnings("UnstableApiUsage")
    @ClassRule
    public static final DropwizardAppRule<ConsentConfiguration> RULE = new DropwizardAppRule<>(
            ConsentApplication.class, Resources.getResource("consent-config.yml").getFile());

    @Override
    public DropwizardAppRule<ConsentConfiguration> rule() {
        return RULE;
    }

    private UserRoleDAO userRoleDAO;
    private DACUserDAO dacUserDAO;

    private void resetUserRoleTable() {
        // Many legacy tests use a set of roles defined in `insert.sql`
        // For the purposes of these unit tests, we will reset the table to its initial state
        // for every run, which should leave the system ready for other tests.
        // TODO: In concept, I don't like this approach and hope to migrate all db tests to be
        // independent of one another
        userRoleDAO.deleteAllUserRoles();
        userRoleDAO.insertSingleUserRole(UserRoles.CHAIRPERSON.getRoleId(), TEST_USER_1_ID);
        userRoleDAO.insertSingleUserRole(UserRoles.RESEARCHER.getRoleId(), TEST_USER_1_ID);
        dacUserDAO.updateUserStatus(TEST_USER_5_ID, UserRoles.MEMBER.getRoleId());
        userRoleDAO.insertSingleUserRole(UserRoles.MEMBER.getRoleId(), TEST_USER_2_ID);
        userRoleDAO.insertSingleUserRole(UserRoles.MEMBER.getRoleId(), TEST_USER_3_ID);
        userRoleDAO.insertSingleUserRole(UserRoles.MEMBER.getRoleId(), TEST_USER_4_ID);
        userRoleDAO.insertSingleUserRole(UserRoles.ADMIN.getRoleId(), TEST_USER_4_ID);
        userRoleDAO.insertSingleUserRole(UserRoles.DATAOWNER.getRoleId(), TEST_USER_1_ID);
        userRoleDAO.insertSingleUserRole(UserRoles.DATAOWNER.getRoleId(), TEST_USER_2_ID);
        userRoleDAO.insertSingleUserRole(UserRoles.DATAOWNER.getRoleId(), TEST_USER_5_ID);
        userRoleDAO.insertSingleUserRole(UserRoles.RESEARCHER.getRoleId(), TEST_USER_5_ID);
        userRoleDAO.insertSingleUserRole(UserRoles.ADMIN.getRoleId(), TEST_USER_5_ID);
        userRoleDAO.insertSingleUserRole(UserRoles.CHAIRPERSON.getRoleId(), TEST_USER_5_ID);
    }

    @Before
    public void setUp() {
        userRoleDAO = getApplicationJdbi().onDemand(UserRoleDAO.class);
        dacUserDAO = getApplicationJdbi().onDemand(DACUserDAO.class);
        resetUserRoleTable();
    }

    @After
    public void tearDown() {
        resetUserRoleTable();
    }

    @Test
    public void testFindRolesByUserId() {
        // See `insert.sql` - that adds 3 roles for user id 1
        List<UserRole> roles = userRoleDAO.findRolesByUserId(TEST_USER_1_ID);
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
        r.setRoleId(UserRoles.MEMBER.getRoleId());
        userRoleDAO.insertUserRoles(Collections.singletonList(r), TEST_USER_1_ID);
    }

    @Test
    public void testUpdateUserRoles() {
        List<UserRole> currentRoles = userRoleDAO.findRolesByUserId(TEST_USER_3_ID);
        userRoleDAO.updateUserRoles(UserRoles.CHAIRPERSON.getRoleId(), TEST_USER_3_ID, UserRoles.MEMBER.getRoleId());
        List<UserRole> newRoles = userRoleDAO.findRolesByUserId(TEST_USER_3_ID);
        Assert.assertFalse(Arrays.equals(currentRoles.toArray(), newRoles.toArray()));
    }

    @Test
    public void testRemoveUserRoles() {
        List<UserRole> currentRoles = userRoleDAO.findRolesByUserId(TEST_USER_5_ID);
        Assert.assertFalse(currentRoles.isEmpty());
        List<Integer> roleIds = userRoleDAO.findRoles().stream().map(Role::getRoleId).collect(Collectors.toList());
        userRoleDAO.removeUserRoles(TEST_USER_5_ID, roleIds);
        List<UserRole> newRoles = userRoleDAO.findRolesByUserId(TEST_USER_5_ID);
        Assert.assertTrue(newRoles.isEmpty());
    }

    @Test
    public void testInsertSingleUserRole() {
        // No-op: tested in set up and tear down
    }

    @Test
    public void testRemoveSingleUserRole() {
        List<UserRole> userRoles = userRoleDAO.findRolesByUserId(TEST_USER_2_ID);
        Assert.assertFalse(userRoles.isEmpty());
        List<Role> roles = userRoleDAO.findRoles();
        roles.forEach(r ->
                userRoleDAO.removeSingleUserRole(TEST_USER_2_ID, r.getRoleId())
        );

        List<UserRole> newUserRoles = userRoleDAO.findRolesByUserId(TEST_USER_2_ID);
        Assert.assertTrue(newUserRoles.isEmpty());
    }

    @Test
    public void testFindRoleByNameAndUser() {
        Integer roleId = userRoleDAO.findRoleByNameAndUser("Chairperson", TEST_USER_1_ID);
        Assert.assertNotNull(roleId);

        Integer invalidRoleId = userRoleDAO.findRoleByNameAndUser("Chairperson", TEST_USER_2_ID);
        Assert.assertNull(invalidRoleId);
    }

    @Test
    public void testFindRoleByUserIdAndRoleId() {
        UserRole userRole = userRoleDAO.findRoleByUserIdAndRoleId(TEST_USER_5_ID, UserRoles.DATAOWNER.getRoleId());
        Assert.assertNotNull(userRole);
    }

    @Test
    public void testConvertJsonToUserRole() {
        String json = "{\"roleId\": 1, \"name\":\"name\", \"what\": \"Huh?\", \"rationale\": \"rationale\", \"status\": \"pending\"}";
        UserRole userRole = new UserRole(json);
        Assert.assertNotNull(userRole);
        Assert.assertEquals(userRole.getRoleId().intValue(), 1);
        Assert.assertEquals(userRole.getName(), "name");
        Assert.assertEquals(userRole.getRationale(), "rationale");
        Assert.assertEquals(userRole.getStatus(), "pending");
        System.out.println(userRole.toString());
    }

}
