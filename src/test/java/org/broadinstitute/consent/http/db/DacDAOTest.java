package org.broadinstitute.consent.http.db;

import com.google.common.io.Resources;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.AbstractTest;
import org.broadinstitute.consent.http.ConsentApplication;
import org.broadinstitute.consent.http.configurations.ConsentConfiguration;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.DACUser;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.Role;
import org.broadinstitute.consent.http.models.UserRole;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class DacDAOTest extends AbstractTest {

    @SuppressWarnings("UnstableApiUsage")
    @ClassRule
    public static final DropwizardAppRule<ConsentConfiguration> RULE = new DropwizardAppRule<>(
            ConsentApplication.class, Resources.getResource("consent-config.yml").getFile());

    @Override
    public DropwizardAppRule<ConsentConfiguration> rule() {
        return RULE;
    }

    private DacDAO dacDAO;
    private DACUserDAO userDAO;

    @Before
    public void setUp() {
        dacDAO = getApplicationJdbi().onDemand(DacDAO.class);
        userDAO = getApplicationJdbi().onDemand(DACUserDAO.class);
    }

    @After
    public void tearDown() {
        // Teardown also tests the delete function
        dacDAO.findAll().forEach(dac -> {
            dacDAO.deleteDacMembers(dac.getDacId());
            dacDAO.deleteDac(dac.getDacId());
        });
    }

    @Test
    public void testCreate() {
        // No-op ... tested in `createDac()`
    }

    @Test
    public void testFindById() {
        // No-op ... tested in `createDac()`
    }

    @Test
    public void testDeleteDacMembers() {
        // No-op ... tested in `tearDown()`
    }

    @Test
    public void testDelete() {
        // No-op ... tested in `tearDown()`
    }

    @Test
    public void testFindAllDacMembers() {
        List<Dac> dacs = new ArrayList<>();
        dacs.add(createDac());
        dacs.add(createDac());
        for (Dac dac : dacs) {
            DACUser chair = createUser();
            dacDAO.addDacMember(UserRoles.CHAIRPERSON.getRoleId(), chair.getDacUserId(), dac.getDacId());
            DACUser member1 = createUser();
            dacDAO.addDacMember(UserRoles.MEMBER.getRoleId(), member1.getDacUserId(), dac.getDacId());
            DACUser member2 = createUser();
            dacDAO.addDacMember(UserRoles.MEMBER.getRoleId(), member2.getDacUserId(), dac.getDacId());
        }
        List<DACUser> allDacUsers = dacDAO.findAllDACUserMemberships();
        Assert.assertEquals(6, allDacUsers.size());
    }

    @Test
    public void testFindAll() {
        int count = 4;
        for (int i = 1; i <= count; i++) createDac();

        List<Dac> dacList = dacDAO.findAll();
        Assert.assertEquals(count, dacList.size());
    }

    @Test
    public void testUpdateDac() {
        String newValue = "New Value";
        Dac dac = createDac();
        dacDAO.updateDac(newValue, newValue, new Date(), dac.getDacId());
        Dac updatedDac = dacDAO.findById(dac.getDacId());

        Assert.assertEquals(updatedDac.getName(), newValue);
        Assert.assertEquals(updatedDac.getDescription(), newValue);
    }

    @Test
    public void testFindMembersByDacId() {
        Dac dac = createDac();
        Integer chairRoleId = UserRoles.CHAIRPERSON.getRoleId();
        Integer memberRoleId = UserRoles.MEMBER.getRoleId();
        DACUser user1 = createUser();
        dacDAO.addDacMember(memberRoleId, user1.getDacUserId(), dac.getDacId());
        DACUser user2 = createUser();
        dacDAO.addDacMember(memberRoleId, user2.getDacUserId(), dac.getDacId());
        DACUser user3 = createUser();
        dacDAO.addDacMember(memberRoleId, user3.getDacUserId(), dac.getDacId());
        DACUser user4 = createUser();
        dacDAO.addDacMember(chairRoleId, user4.getDacUserId(), dac.getDacId());
        List<DACUser> dacMembers = dacDAO.findMembersByDacId(dac.getDacId());
        Assert.assertNotNull(dacMembers);
        Assert.assertFalse(dacMembers.isEmpty());
        Assert.assertEquals(dacMembers.size(), 4);
    }

    @Test
    public void testRemoveDacMember() {
        // TODO
    }

    @Test
    public void testAddDacMember() {
        Dac dac = createDac();
        Integer roleId = UserRoles.MEMBER.getRoleId();
        DACUser user = createUser();
        dacDAO.addDacMember(roleId, user.getDacUserId(), dac.getDacId());
        List<UserRole> memberRoles = dacDAO.findUserRolesForUser(user.getDacUserId());
        Assert.assertFalse(memberRoles.isEmpty());
        UserRole userRole = memberRoles.get(0);
        Assert.assertEquals(userRole.getDacId(), dac.getDacId());
        Assert.assertEquals(userRole.getRoleId(), roleId);
    }

    @Test
    public void testAddDacChair() {
        Dac dac = createDac();
        Integer roleId = UserRoles.CHAIRPERSON.getRoleId();
        DACUser user = createUser();
        dacDAO.addDacMember(roleId, user.getDacUserId(), dac.getDacId());
        List<UserRole> chairRoles = dacDAO.findUserRolesForUser(user.getDacUserId());
        Assert.assertFalse(chairRoles.isEmpty());
        UserRole userRole = chairRoles.get(0);
        Assert.assertEquals(userRole.getDacId(), dac.getDacId());
        Assert.assertEquals(userRole.getRoleId(), roleId);
    }

    @Test
    public void testGetRoleById() {
        Role chair = dacDAO.getRoleById(UserRoles.CHAIRPERSON.getRoleId());
        Assert.assertEquals(
                chair.getName().toLowerCase(),
                UserRoles.CHAIRPERSON.getRoleName().toLowerCase());
        Role member = dacDAO.getRoleById(UserRoles.MEMBER.getRoleId());
        Assert.assertEquals(
                member.getName().toLowerCase(),
                UserRoles.MEMBER.getRoleName().toLowerCase());
    }

    @Test
    public void testFindUserRolesForUser() {
        // No-op ... tested in `testAddDacChair()`
    }

    @Test
    public void testFindUserRolesForUsers() {
        Dac dac = createDac();
        DACUser chair = createUser();
        DACUser member = createUser();
        dacDAO.addDacMember(UserRoles.CHAIRPERSON.getRoleId(), chair.getDacUserId(), dac.getDacId());
        dacDAO.addDacMember(UserRoles.MEMBER.getRoleId(), member.getDacUserId(), dac.getDacId());
        List<Integer> userIds = Arrays.asList(chair.getDacUserId(), member.getDacUserId());
        List<UserRole> userRoles = dacDAO.findUserRolesForUsers(userIds);
        Assert.assertEquals(userRoles.size(), 2);
    }

    private Dac createDac() {
        Integer dacId = dacDAO.createDac(RandomStringUtils.random(10), RandomStringUtils.random(10), new Date());
        return dacDAO.findById(dacId);
    }

    private DACUser createUser() {
        int i1 = RandomUtils.nextInt(5, 10);
        int i2 = RandomUtils.nextInt(5, 10);
        int i3 = RandomUtils.nextInt(3, 5);
        String email = RandomStringUtils.randomAlphabetic(i1) +
                "@" +
                RandomStringUtils.randomAlphabetic(i2) +
                "." +
                RandomStringUtils.randomAlphabetic(i3);
        Integer userId = userDAO.insertDACUser(email, "display name", new Date());
        return dacDAO.findUserById(userId);
    }

}
