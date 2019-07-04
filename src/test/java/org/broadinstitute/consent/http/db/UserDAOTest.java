package org.broadinstitute.consent.http.db;

import com.google.common.io.Resources;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.apache.commons.lang3.RandomStringUtils;
import org.broadinstitute.consent.http.AbstractTest;
import org.broadinstitute.consent.http.ConsentApplication;
import org.broadinstitute.consent.http.configurations.ConsentConfiguration;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.User;
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

public class UserDAOTest extends AbstractTest {

    @SuppressWarnings("UnstableApiUsage")
    @ClassRule
    public static final DropwizardAppRule<ConsentConfiguration> RULE = new DropwizardAppRule<>(
            ConsentApplication.class, Resources.getResource("consent-config.yml").getFile());

    @Override
    public DropwizardAppRule<ConsentConfiguration> rule() {
        return RULE;
    }

    private UserDAO userDAO;
    private UserRoleDAO userRoleDAO;

    @Before
    public void setUp() {
        userDAO = getApplicationJdbi().onDemand(UserDAO.class);
        userRoleDAO = getApplicationJdbi().onDemand(UserRoleDAO.class);
    }

    @After
    public void tearDown() {

    }

    @Test
    public void testFindDACUserById() {
        User user = userDAO.findDACUserById(1);
        Assert.assertNotNull(user);

        User user2 = userDAO.findDACUserById(100000);
        Assert.assertNull(user2);
    }

    @Test
    public void testFindUsers_withIdCollection() {
        Collection<User> users = userDAO.findUsers(Collections.singletonList(1));
        Assert.assertNotNull(users);
        Assert.assertFalse(users.isEmpty());
        Assert.assertEquals(1, users.size());
    }

    @Test
    public void testFindChairpersonUser() {
        User user = userDAO.findChairpersonUser();
        Assert.assertNotNull(user);
        Assert.assertNotNull(user.getRoles());
        Assert.assertFalse(user.getRoles().isEmpty());
        List<String> roleNames = user.getRoles().
                stream().
                map(UserRole::getName).
                map(String::toLowerCase).
                collect(Collectors.toList());
        Assert.assertTrue(roleNames.contains(UserRoles.CHAIRPERSON.getValue().toLowerCase()));
    }

    @Test
    public void testDescribeUsersByRole() {
        List<User> members = userDAO.describeUsersByRole("Member");
        Assert.assertFalse(members.isEmpty());

        List<User> chairs = userDAO.describeUsersByRole("Chairperson");
        Assert.assertFalse(chairs.isEmpty());

        // Only case where we don't set up users by default.
        List<User> alumni = userDAO.describeUsersByRole("Alumni");
        Assert.assertTrue(alumni.isEmpty());

        List<User> admins = userDAO.describeUsersByRole("Admin");
        Assert.assertFalse(admins.isEmpty());

        List<User> researchers = userDAO.describeUsersByRole("Researcher");
        Assert.assertFalse(researchers.isEmpty());

        List<User> dataOwners = userDAO.describeUsersByRole("DataOwner");
        Assert.assertFalse(dataOwners.isEmpty());
    }

    @Test
    public void testCheckChairpersonUser() {
        // insert.sql sets up users 1 and 5 as chair persons
        Assert.assertNotNull(userDAO.checkChairpersonUser(1));
        Assert.assertNull(userDAO.checkChairpersonUser(2));
        Assert.assertNull(userDAO.checkChairpersonUser(3));
        Assert.assertNull(userDAO.checkChairpersonUser(4));
        Assert.assertNotNull(userDAO.checkChairpersonUser(5));
        Assert.assertNull(userDAO.checkChairpersonUser(6));
    }

    @Test
    public void testFindDACUsersEnabledToVote() {
        Collection<User> users = userDAO.findDACUsersEnabledToVote();
        Assert.assertFalse(users.isEmpty());
    }

    @Test
    public void testFindUsersWithRoles() {
        Collection<Integer> userIds = Arrays.asList(1, 2, 3, 4, 5, 6);
        Collection<User> users = userDAO.findUsersWithRoles(userIds);
        users.forEach(u -> Assert.assertFalse("User: " + u.getUserId() + " has no roles", u.getRoles().isEmpty()));
    }

    @Test
    public void testFindDACUserByEmail() {
        User user = userDAO.findDACUserByEmail("test@broad.com");
        Assert.assertNotNull(user);

        User user2 = userDAO.findDACUserByEmail("no.one@nowhere.com");
        Assert.assertNull(user2);
    }

    @Test
    public void testInsertDACUser() {
        String email = getRandomEmailAddress();
        Integer userId = userDAO.insertDACUser(email, "Dac User Test", new Date());
        Assert.assertNotNull(userId);
        User user = userDAO.findDACUserById(userId);
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
        User user = userDAO.findDACUserById(userId);
        Assert.assertEquals(user.getEmail(), newEmail);
    }

    @Test
    public void testDeleteDACUserByEmail() {
        Integer userId = userDAO.insertDACUser("delete_test@broad.org", "Dac User Delete Test", new Date());
        Assert.assertNotNull(userId);
        User user = userDAO.findDACUserById(userId);
        Assert.assertNotNull(user);
        userDAO.deleteDACUserByEmail(user.getEmail());
        User deletedUser = userDAO.findDACUserById(userId);
        Assert.assertNull(deletedUser);
    }

    @Test
    public void testFindDACUserIdByRole() {
        int userId = 5;
        int dataOwnerId = 6;
        Integer foundUserId = userDAO.findDACUserIdByRole(userId, dataOwnerId);
        Assert.assertNotNull(foundUserId);
        Assert.assertNotEquals(userId, foundUserId.intValue());
    }

    @Test
    public void testFindUsers_noArgs() {
        Collection<User> users = userDAO.findUsers();
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
        userRoleDAO.insertSingleUserRole(5, userId, true);
        Collection<User> researchers = userDAO.describeUsersByRoleAndEmailPreference("Researcher", true);
        Assert.assertFalse(researchers.isEmpty());

        String email2 = getRandomEmailAddress();
        Integer userId2 = userDAO.insertDACUser(email2, "Dac User Test", new Date());
        userRoleDAO.insertSingleUserRole(6, userId2, false);
        Collection<User> dataOwners = userDAO.describeUsersByRoleAndEmailPreference("DataOwner", false);
        Assert.assertFalse(dataOwners.isEmpty());
    }

    @Test
    public void testGetMembersApprovedToReplace() {
        // There are 3 Members set up in insert.sql, users 2, 3, and 4

        Collection<User> users2 = userDAO.getMembersApprovedToReplace(2, Collections.singletonList(1));
        Assert.assertFalse(users2.isEmpty());

        Collection<User> users3 = userDAO.getMembersApprovedToReplace(3, Collections.singletonList(1));
        Assert.assertFalse(users3.isEmpty());

        Collection<User> users4 = userDAO.getMembersApprovedToReplace(4, Collections.singletonList(1));
        Assert.assertFalse(users4.isEmpty());
    }

    @Test
    public void testGetDataOwnersApprovedToReplace() {
        // There are 3 DataOwners set up in insert.sql, users 1, 2, and 5

        Collection<User> users2 = userDAO.getDataOwnersApprovedToReplace(1);
        Assert.assertFalse(users2.isEmpty());

        Collection<User> users3 = userDAO.getDataOwnersApprovedToReplace(2);
        Assert.assertFalse(users3.isEmpty());

        Collection<User> users4 = userDAO.getDataOwnersApprovedToReplace(5);
        Assert.assertFalse(users4.isEmpty());
    }

    @Test
    public void testUpdateDACUser_case2() {
        String email = getRandomEmailAddress();
        Integer userId = userDAO.insertDACUser(email, "Dac User Test", new Date());
        Assert.assertNotNull(userId);
        userDAO.updateDACUser("Updated Dac User Test", userId);
        User user = userDAO.findDACUserById(userId);
        Assert.assertEquals(user.getDisplayName(), "Updated Dac User Test");
    }


    private String getRandomEmailAddress() {
        String user = RandomStringUtils.randomAlphanumeric(20);
        String domain = RandomStringUtils.randomAlphanumeric(10);
        return user + "@" + domain + ".org";
    }

}
