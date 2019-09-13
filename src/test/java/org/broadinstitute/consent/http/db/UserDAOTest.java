package org.broadinstitute.consent.http.db;

import com.google.common.io.Resources;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.apache.commons.lang3.RandomStringUtils;
import org.broadinstitute.consent.http.AbstractTest;
import org.broadinstitute.consent.http.ConsentApplication;
import org.broadinstitute.consent.http.configurations.ConsentConfiguration;
import org.broadinstitute.consent.http.enumeration.RoleStatus;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.DACUser;
import org.broadinstitute.consent.http.models.UserRole;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static org.broadinstitute.consent.http.enumeration.RoleStatus.getStatusByValue;

public class UserDAOTest extends AbstractTest {

    private static final int TEST_USER_1_ID = 1;
    private static final int TEST_USER_2_ID = 2;
    private static final int TEST_USER_3_ID = 3;
    private static final int TEST_USER_4_ID = 4;
    private static final int TEST_USER_5_ID = 5;
    private static final int TEST_USER_6_ID = 6;

    @SuppressWarnings("UnstableApiUsage")
    @ClassRule
    public static final DropwizardAppRule<ConsentConfiguration> RULE = new DropwizardAppRule<>(
            ConsentApplication.class, Resources.getResource("consent-config.yml").getFile());

    @Override
    public DropwizardAppRule<ConsentConfiguration> rule() {
        return RULE;
    }

    private DACUserDAO userDAO;
    private UserRoleDAO userRoleDAO;

    @Before
    public void setUp() {
        userDAO = getApplicationJdbi().onDemand(DACUserDAO.class);
        userRoleDAO = getApplicationJdbi().onDemand(UserRoleDAO.class);
    }

    @After
    public void tearDown() {

    }

    @Test
    public void testFindDACUserById() {
        DACUser user = userDAO.findDACUserById(TEST_USER_1_ID);
        Assert.assertNotNull(user);

        DACUser user2 = userDAO.findDACUserById(Integer.MAX_VALUE);
        Assert.assertNull(user2);
    }

    @Test
    public void testFindUsers_withIdCollection() {
        Collection<DACUser> users = userDAO.findUsers(Collections.singletonList(1));
        Assert.assertNotNull(users);
        Assert.assertFalse(users.isEmpty());
        Assert.assertEquals(1, users.size());
    }

    @Test
    public void testFindChairpersonUser() {
        DACUser user = userDAO.findChairpersonUser();
        Assert.assertNotNull(user);
        Assert.assertNotNull(user.getRoles());
        Assert.assertFalse(user.getRoles().isEmpty());
        List<String> roleNames = user.getRoles().
                stream().
                map(UserRole::getName).
                map(String::toLowerCase).
                collect(Collectors.toList());
        Assert.assertTrue(roleNames.contains(UserRoles.CHAIRPERSON.getRoleName().toLowerCase()));
    }

    @Test
    public void testDescribeUsersByRole() {
        List<DACUser> members = userDAO.describeUsersByRole("Member");
        Assert.assertFalse(members.isEmpty());

        List<DACUser> chairs = userDAO.describeUsersByRole("Chairperson");
        Assert.assertFalse(chairs.isEmpty());

        // Only case where we don't set up users by default.
        List<DACUser> alumni = userDAO.describeUsersByRole("Alumni");
        Assert.assertTrue(alumni.isEmpty());

        List<DACUser> admins = userDAO.describeUsersByRole("Admin");
        Assert.assertFalse(admins.isEmpty());

        List<DACUser> researchers = userDAO.describeUsersByRole("Researcher");
        Assert.assertFalse(researchers.isEmpty());

        List<DACUser> dataOwners = userDAO.describeUsersByRole("DataOwner");
        Assert.assertFalse(dataOwners.isEmpty());
    }

    @Test
    public void testCheckChairpersonUser() {
        // insert.sql sets up users 1 and 5 as chair persons
        Assert.assertNotNull(userDAO.checkChairpersonUser(TEST_USER_1_ID));
        Assert.assertNull(userDAO.checkChairpersonUser(TEST_USER_2_ID));
        Assert.assertNull(userDAO.checkChairpersonUser(TEST_USER_3_ID));
        Assert.assertNull(userDAO.checkChairpersonUser(TEST_USER_4_ID));
        Assert.assertNotNull(userDAO.checkChairpersonUser(TEST_USER_5_ID));
        Assert.assertNull(userDAO.checkChairpersonUser(TEST_USER_6_ID));
    }

    @Test
    public void testFindDACUsersEnabledToVote() {
        Collection<DACUser> users = userDAO.findDACUsersEnabledToVote();
        Assert.assertFalse(users.isEmpty());
    }

    @Test
    public void testFindUsersWithRoles() {
        Collection<Integer> userIds = Arrays.asList(
                TEST_USER_1_ID,
                TEST_USER_2_ID,
                TEST_USER_3_ID,
                TEST_USER_4_ID,
                TEST_USER_5_ID,
                TEST_USER_6_ID);
        Collection<DACUser> users = userDAO.findUsersWithRoles(userIds);
        users.forEach(u -> Assert.assertFalse("User: " + u.getDacUserId() + " has no roles", u.getRoles().isEmpty()));
    }

    @Test
    public void testFindDACUserByEmail() {
        DACUser user = userDAO.findDACUserByEmail("test@broad.com");
        Assert.assertNotNull(user);

        DACUser user2 = userDAO.findDACUserByEmail("no.one@nowhere.com");
        Assert.assertNull(user2);
    }

    @Test
    public void testInsertDACUser() {
        String email = getRandomEmailAddress();
        Integer userId = userDAO.insertDACUser(email, "Dac User Test", new Date());
        Assert.assertNotNull(userId);
        DACUser user = userDAO.findDACUserById(userId);
        Assert.assertNotNull(user);
    }

    @Test
    public void testUpdateDACUser_case1() {
        String email = getRandomEmailAddress();
        String newEmail = getRandomEmailAddress();
        Integer userId = userDAO.insertDACUser(email, "Dac User Test", new Date());
        Assert.assertNotNull(userId);
        userDAO.updateDACUser(
                newEmail,
                "Dac User Test",
                userId,
                email);
        DACUser user = userDAO.findDACUserById(userId);
        Assert.assertEquals(user.getEmail(), newEmail);
    }

    @Test
    public void testDeleteDACUserByEmail() {
        Integer userId = userDAO.insertDACUser("delete_test@broad.org", "Dac User Delete Test", new Date());
        Assert.assertNotNull(userId);
        DACUser user = userDAO.findDACUserById(userId);
        Assert.assertNotNull(user);
        userDAO.deleteDACUserByEmail(user.getEmail());
        DACUser deletedUser = userDAO.findDACUserById(userId);
        Assert.assertNull(deletedUser);
    }

    @Test
    public void testFindDACUserIdByRole() {
        Integer foundUserId = userDAO.findDACUserIdByRole(TEST_USER_5_ID, UserRoles.DATAOWNER.getRoleId());
        Assert.assertNotNull(foundUserId);
        Assert.assertNotEquals(TEST_USER_5_ID, (int) foundUserId);
    }

    @Test
    public void testFindUsers_noArgs() {
        Collection<DACUser> users = userDAO.findUsers();
        Assert.assertNotNull(users);
        Assert.assertFalse(users.isEmpty());
    }

    @Test
    public void testVerifyAdminUsers() {
        Integer count = userDAO.verifyAdminUsers();
        Assert.assertNotNull(count);
        Assert.assertTrue(count > 0);
    }

    @Test
    public void testDescribeUsersByRoleAndEmailPreference() {
        String email = getRandomEmailAddress();
        Integer userId = userDAO.insertDACUser(email, "Dac User Test", new Date());
        userDAO.updateEmailPreference(true, userId);
        userRoleDAO.insertSingleUserRole(UserRoles.RESEARCHER.getRoleId(), userId);
        Collection<DACUser> researchers = userDAO.describeUsersByRoleAndEmailPreference("Researcher", true);
        Assert.assertFalse(researchers.isEmpty());

        String email2 = getRandomEmailAddress();
        Integer userId2 = userDAO.insertDACUser(email2, "Dac User Test", new Date());
        userDAO.updateEmailPreference(false, userId2);
        userRoleDAO.insertSingleUserRole(UserRoles.DATAOWNER.getRoleId(), userId2);
        Collection<DACUser> dataOwners = userDAO.describeUsersByRoleAndEmailPreference("DataOwner", false);
        Assert.assertFalse(dataOwners.isEmpty());
    }

    @Test
    public void testGetMembersApprovedToReplace() {
        // There are 3 Members set up in insert.sql, users 2, 3, and 4

        Collection<DACUser> users2 = userDAO.getMembersApprovedToReplace(TEST_USER_2_ID, Collections.singletonList(1));
        Assert.assertFalse(users2.isEmpty());

        Collection<DACUser> users3 = userDAO.getMembersApprovedToReplace(TEST_USER_3_ID, Collections.singletonList(1));
        Assert.assertFalse(users3.isEmpty());

        Collection<DACUser> users4 = userDAO.getMembersApprovedToReplace(TEST_USER_4_ID, Collections.singletonList(1));
        Assert.assertFalse(users4.isEmpty());
    }

    @Test
    public void testGetDataOwnersApprovedToReplace() {
        // There are 3 DataOwners set up in insert.sql, users 1, 2, and 5

        Collection<DACUser> users2 = userDAO.getDataOwnersApprovedToReplace(TEST_USER_1_ID);
        Assert.assertFalse(users2.isEmpty());

        Collection<DACUser> users3 = userDAO.getDataOwnersApprovedToReplace(TEST_USER_2_ID);
        Assert.assertFalse(users3.isEmpty());

        Collection<DACUser> users4 = userDAO.getDataOwnersApprovedToReplace(TEST_USER_5_ID);
        Assert.assertFalse(users4.isEmpty());
    }

    @Test
    public void testUpdateDACUser_case2() {
        String email = getRandomEmailAddress();
        Integer userId = userDAO.insertDACUser(email, "Dac User Test", new Date());
        Assert.assertNotNull(userId);
        userDAO.updateDACUser("Updated Dac User Test", userId);
        DACUser user = userDAO.findDACUserById(userId);
        Assert.assertEquals(user.getDisplayName(), "Updated Dac User Test");
    }

    @Test
    public void testUpdateEmailPreference() {
        userDAO.findUsers().forEach(u -> userDAO.updateEmailPreference(true, u.getDacUserId()));
        userDAO.findUsers().forEach(u -> Assert.assertTrue(u.getEmailPreference()));

        userDAO.findUsers().forEach(u -> userDAO.updateEmailPreference(false, u.getDacUserId()));
        userDAO.findUsers().forEach(u -> Assert.assertFalse(u.getEmailPreference()));
    }

    @Test
    public void testUpdateUserStatus() {
        Integer roleStatusId = RoleStatus.getValueByStatus(RoleStatus.APPROVED.name());
        String roleStatusName = getStatusByValue(roleStatusId);
        userDAO.updateUserStatus(roleStatusId, TEST_USER_5_ID);
        DACUser user = userDAO.findDACUserById(TEST_USER_5_ID);
        Assert.assertNotNull(user);
        Assert.assertEquals(user.getStatus(), roleStatusName);
    }

    @Test
    public void testUpdateUserRationale() {
        String rationale = "New Rationale";
        userDAO.updateUserRationale(rationale, TEST_USER_5_ID);
        DACUser user = userDAO.findDACUserById(TEST_USER_5_ID);
        Assert.assertNotNull(user);
        Assert.assertEquals(user.getRationale(), rationale);
    }

    private String getRandomEmailAddress() {
        String user = RandomStringUtils.randomAlphanumeric(20);
        String domain = RandomStringUtils.randomAlphanumeric(10);
        return user + "@" + domain + ".org";
    }

}
