package org.broadinstitute.consent.http.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.Role;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.junit.Assert;
import org.junit.Test;

public class DacDAOTest extends DAOTestHelper {

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
    public void testFindAll() {
        int count = 4;
        for (int i = 1; i <= count; i++) createDac();

        List<Dac> dacList = dacDAO.findAll();
        Assert.assertEquals(count, dacList.size());
    }

    @Test
    public void testFindDacsForEmail() {
        Dac dac = createDac();
        User chair = createUser();
        dacDAO.addDacMember(UserRoles.CHAIRPERSON.getRoleId(), chair.getDacUserId(), dac.getDacId());

        List<Dac> dacs = dacDAO.findDacsForEmail(chair.getEmail());
        Assert.assertEquals(1, dacs.size());
    }

    @Test
    public void testFindAllDacMemberships() {
        List<Dac> dacs = new ArrayList<>();
        dacs.add(createDac());
        dacs.add(createDac());
        for (Dac dac : dacs) {
            User chair = createUser();
            dacDAO.addDacMember(UserRoles.CHAIRPERSON.getRoleId(), chair.getDacUserId(), dac.getDacId());
            User member1 = createUser();
            dacDAO.addDacMember(UserRoles.MEMBER.getRoleId(), member1.getDacUserId(), dac.getDacId());
            User member2 = createUser();
            dacDAO.addDacMember(UserRoles.MEMBER.getRoleId(), member2.getDacUserId(), dac.getDacId());
        }
        List<User> allUsers = dacDAO.findAllDACUserMemberships();
        Assert.assertEquals(6, allUsers.size());
    }

    @Test
    public void testFindAllDACUsersBySearchString_case1() {
        Dac dac = createDac();
        User chair = createUser();
        dacDAO.addDacMember(UserRoles.CHAIRPERSON.getRoleId(), chair.getDacUserId(), dac.getDacId());

        List<User> users = dacDAO.findAllDACUsersBySearchString(chair.getEmail());
        assertFalse(users.isEmpty());
        Assert.assertEquals(1, users.size());
    }

    @Test
    public void testFindAllDACUsersBySearchString_case2() {
        List<User> users = dacDAO.findAllDACUsersBySearchString("random");
        Assert.assertTrue(users.isEmpty());
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
        User user1 = createUser();
        dacDAO.addDacMember(memberRoleId, user1.getDacUserId(), dac.getDacId());
        User user2 = createUser();
        dacDAO.addDacMember(memberRoleId, user2.getDacUserId(), dac.getDacId());
        User user3 = createUser();
        dacDAO.addDacMember(memberRoleId, user3.getDacUserId(), dac.getDacId());
        User user4 = createUser();
        dacDAO.addDacMember(chairRoleId, user4.getDacUserId(), dac.getDacId());

        List<User> dacMembers = dacDAO.findMembersByDacId(dac.getDacId());
        Assert.assertNotNull(dacMembers);
        assertFalse(dacMembers.isEmpty());
        Assert.assertEquals(dacMembers.size(), 4);
    }

    @Test
    public void testFindMembersByDacIdAndRoleId() {
        Dac dac = createDac();
        Integer chairRoleId = UserRoles.CHAIRPERSON.getRoleId();
        Integer memberRoleId = UserRoles.MEMBER.getRoleId();
        User user1 = createUser();
        dacDAO.addDacMember(memberRoleId, user1.getDacUserId(), dac.getDacId());
        User user2 = createUser();
        dacDAO.addDacMember(memberRoleId, user2.getDacUserId(), dac.getDacId());
        User user3 = createUser();
        dacDAO.addDacMember(memberRoleId, user3.getDacUserId(), dac.getDacId());
        User user4 = createUser();
        dacDAO.addDacMember(chairRoleId, user4.getDacUserId(), dac.getDacId());

        List<User> chairs = dacDAO.findMembersByDacIdAndRoleId(dac.getDacId(), chairRoleId);
        Assert.assertNotNull(chairs);
        assertFalse(chairs.isEmpty());
        Assert.assertEquals(chairs.size(), 1);

        List<User> members = dacDAO.findMembersByDacIdAndRoleId(dac.getDacId(), memberRoleId);
        Assert.assertNotNull(members);
        assertFalse(members.isEmpty());
        Assert.assertEquals(members.size(), 3);
    }

    @Test
    public void testAddDacMember() {
        Dac dac = createDac();
        Integer roleId = UserRoles.MEMBER.getRoleId();
        User user = createUser();
        dacDAO.addDacMember(roleId, user.getDacUserId(), dac.getDacId());
        List<UserRole> memberRoles = dacDAO.findUserRolesForUser(user.getDacUserId());
        assertFalse(memberRoles.isEmpty());
        UserRole userRole = memberRoles.get(0);
        Assert.assertEquals(userRole.getDacId(), dac.getDacId());
        Assert.assertEquals(userRole.getRoleId(), roleId);
    }

    @Test
    public void testRemoveDacMember() {
        // No-op ... tested in `tearDown()`
    }

    @Test
    public void testAddDacChair() {
        Dac dac = createDac();
        Integer roleId = UserRoles.CHAIRPERSON.getRoleId();
        User user = createUser();
        dacDAO.addDacMember(roleId, user.getDacUserId(), dac.getDacId());
        List<UserRole> chairRoles = dacDAO.findUserRolesForUser(user.getDacUserId());
        assertFalse(chairRoles.isEmpty());
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
    public void testFindUserById() {
        // No-op ... tested in `createUser()`
    }

    @Test
    public void testFindUserRolesForUser() {
        // No-op ... tested in `testAddDacChair()`
    }

    @Test
    public void testFindUserRolesForUsers() {
        Dac dac = createDac();
        User chair = createUser();
        User member = createUser();
        dacDAO.addDacMember(UserRoles.CHAIRPERSON.getRoleId(), chair.getDacUserId(), dac.getDacId());
        dacDAO.addDacMember(UserRoles.MEMBER.getRoleId(), member.getDacUserId(), dac.getDacId());
        List<Integer> userIds = Arrays.asList(chair.getDacUserId(), member.getDacUserId());
        List<UserRole> userRoles = dacDAO.findUserRolesForUsers(userIds);
        Assert.assertEquals(userRoles.size(), 2);
    }

    @Test
    public void testFindDacsForDatasetIds() {
        Dac dac = createDac();
        Consent consent1 = createConsent(dac.getDacId());
        DataSet dataSet1 = createDataset();
        createAssociation(consent1.getConsentId(), dataSet1.getDataSetId());

        Consent consent2 = createConsent(dac.getDacId());
        DataSet dataSet2 = createDataset();
        createAssociation(consent2.getConsentId(), dataSet2.getDataSetId());
        Set<Dac> dacs = dacDAO.findDacsForDatasetIds(Arrays.asList(dataSet1.getDataSetId(), dataSet2.getDataSetId()));
        assertFalse(dacs.isEmpty());
        assertEquals(1, dacs.size());
    }
}
