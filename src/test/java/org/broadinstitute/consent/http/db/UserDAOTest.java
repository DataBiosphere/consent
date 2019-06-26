package org.broadinstitute.consent.http.db;

import com.google.common.io.Resources;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.broadinstitute.consent.http.AbstractTest;
import org.broadinstitute.consent.http.ConsentApplication;
import org.broadinstitute.consent.http.configurations.ConsentConfiguration;
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

public class UserDAOTest extends AbstractTest {

    @SuppressWarnings("UnstableApiUsage")
    @ClassRule
    public static final DropwizardAppRule<ConsentConfiguration> RULE = new DropwizardAppRule<>(
            ConsentApplication.class, Resources.getResource("consent-config.yml").getFile());

    @Override
    public DropwizardAppRule<ConsentConfiguration> rule() {
        return RULE;
    }

    private DACUserDAO userDAO;

    @Before
    public void setUp() {
        userDAO = getApplicationJdbi().onDemand(DACUserDAO.class);
    }

    @After
    public void tearDown() {

    }

    @Test
    public void testFindDACUserById() {
        DACUser user = userDAO.findDACUserById(1);
        Assert.assertNotNull(user);

        DACUser user2 = userDAO.findDACUserById(100000);
        Assert.assertNull(user2);
    }

    @Test
    public void testFindUsers() {
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
        Assert.assertTrue(roleNames.contains(UserRoles.CHAIRPERSON.getValue().toLowerCase()));
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
        Assert.assertNotNull(userDAO.checkChairpersonUser(1));
        Assert.assertNull(userDAO.checkChairpersonUser(2));
        Assert.assertNull(userDAO.checkChairpersonUser(3));
        Assert.assertNull(userDAO.checkChairpersonUser(4));
        Assert.assertNotNull(userDAO.checkChairpersonUser(5));
        Assert.assertNull(userDAO.checkChairpersonUser(6));
    }

    @Test
    public void testFindDACUsersEnabledToVote() {
        Collection<DACUser> users = userDAO.findDACUsersEnabledToVote();
        Assert.assertFalse(users.isEmpty());
    }

    @Test
    public void testFindUsersWithRoles() {
        Collection<Integer> userIds = Arrays.asList(1, 2, 3, 4, 5, 6);
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
        Integer userId = userDAO.insertDACUser("dac_user_test@broad.org", "Dac User Test", new Date());
        Assert.assertNotNull(userId);
        DACUser user = userDAO.findDACUserById(userId);
        Assert.assertNotNull(user);
    }

}
