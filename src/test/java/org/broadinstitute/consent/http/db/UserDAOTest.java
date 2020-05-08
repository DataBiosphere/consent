package org.broadinstitute.consent.http.db;

import org.apache.commons.lang3.RandomStringUtils;
import org.broadinstitute.consent.http.enumeration.ResearcherFields;
import org.broadinstitute.consent.http.enumeration.RoleStatus;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.DACUser;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.ResearcherProperty;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.broadinstitute.consent.http.enumeration.RoleStatus.getStatusByValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class UserDAOTest extends DAOTestHelper {

    @Test
    public void testFindDACUserById() {
        DACUser user = createUserWithRole(UserRoles.ADMIN.getRoleId());
        assertNotNull(user);
        assertFalse(user.getRoles().isEmpty());
        assertEquals(UserRoles.ADMIN.getRoleId(), user.getRoles().get(0).getRoleId());
        DACUser user2 = userDAO.findDACUserById(user.getDacUserId());
        assertNotNull(user2);
        assertEquals(user.getEmail(), user2.getEmail());
    }

    @Test
    public void testFindUsers_withIdCollection() {
        DACUser user = createUser();
        Collection<DACUser> users = userDAO.findUsers(Collections.singletonList(user.getDacUserId()));
        assertNotNull(users);
        assertFalse(users.isEmpty());
        assertEquals(1, users.size());
    }

    @Test
    public void testDescribeUsersByRole() {
        createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
        createUserWithRole(UserRoles.MEMBER.getRoleId());

        List<DACUser> members = userDAO.describeUsersByRole(UserRoles.MEMBER.getRoleName());
        assertFalse(members.isEmpty());

        List<DACUser> chairs = userDAO.describeUsersByRole(UserRoles.CHAIRPERSON.getRoleName());
        assertFalse(chairs.isEmpty());

        // Only case where we don't set up users by default.
        List<DACUser> alumni = userDAO.describeUsersByRole(UserRoles.ALUMNI.getRoleName());
        assertTrue(alumni.isEmpty());

        List<DACUser> admins = userDAO.describeUsersByRole(UserRoles.ADMIN.getRoleName());
        assertTrue(admins.isEmpty());

        List<DACUser> researchers = userDAO.describeUsersByRole(UserRoles.RESEARCHER.getRoleName());
        assertTrue(researchers.isEmpty());

        List<DACUser> dataOwners = userDAO.describeUsersByRole(UserRoles.DATAOWNER.getRoleName());
        assertTrue(dataOwners.isEmpty());
    }

    @Test
    public void testCheckChairpersonUser() {
        DACUser chair = createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
        DACUser member = createUserWithRole(UserRoles.MEMBER.getRoleId());
        assertNotNull(userDAO.checkChairpersonUser(chair.getDacUserId()));
        Assert.assertNull(userDAO.checkChairpersonUser(member.getDacUserId()));
    }

    @Test
    public void testFindDACUsersEnabledToVoteByDacEmpty() {
        Dac dac = createDac();
        Collection<DACUser> users = userDAO.findDACUsersEnabledToVoteByDAC(dac.getDacId());
        assertTrue(users.isEmpty());
    }

    @Test
    public void testFindDACUsersEnabledToVoteByDacNotEmpty() {
        Dac dac = createDac();
        DACUser chair = createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
        dacDAO.addDacMember(UserRoles.CHAIRPERSON.getRoleId(), chair.getDacUserId(), dac.getDacId());
        Collection<DACUser> users = userDAO.findDACUsersEnabledToVoteByDAC(dac.getDacId());
        assertFalse(users.isEmpty());
    }

    @Test
    public void testFindNonDACUsersEnabledToVote() {
        createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
        createUserWithRole(UserRoles.MEMBER.getRoleId());
        Collection<DACUser> users = userDAO.findNonDACUsersEnabledToVote();
        assertFalse(users.isEmpty());
    }

    @Test
    public void testFindUsersWithRoles() {
        DACUser chair = createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
        Collection<Integer> userIds = Collections.singletonList(chair.getDacUserId());
        Collection<DACUser> users = userDAO.findUsersWithRoles(userIds);
        users.forEach(u -> assertFalse("User: " + u.getDacUserId() + " has no roles", u.getRoles().isEmpty()));
    }

    @Test
    public void testFindUsersWithNoRoles() {
        DACUser user = createUser();
        Collection<Integer> userIds = Collections.singletonList(user.getDacUserId());
        Collection<DACUser> users = userDAO.findUsersWithRoles(userIds);
        users.forEach(u -> assertTrue("User: " + u.getDacUserId() + " has roles", u.getRoles().isEmpty()));
    }

    @Test
    public void testFindDACUserByEmail() {
        DACUser user = createUser();
        DACUser user1 = userDAO.findDACUserByEmail(user.getEmail());
        assertNotNull(user1);
        DACUser user2 = userDAO.findDACUserByEmail("no.one@nowhere.com");
        Assert.assertNull(user2);
    }

    @Test
    public void testInsertDACUser() {
        // No-op ... tested in `createUser()`
    }

    @Test
    public void testUpdateDACUser_case1() {
        DACUser user = createUser();
        String newEmail = getRandomEmailAddress();
        userDAO.updateDACUser(
                newEmail,
                "Dac User Test",
                user.getDacUserId(),
                newEmail);
        DACUser user2 = userDAO.findDACUserById(user.getDacUserId());
        assertEquals(user2.getEmail(), newEmail);
    }

    @Test
    public void testDeleteDACUserByEmail() {
        DACUser user = createUser();
        userDAO.deleteDACUserByEmail(user.getEmail());
        DACUser foundUser = userDAO.findDACUserByEmail(user.getEmail());
        assertNull(foundUser);
    }

    @Test
    public void testDeleteDACUserById() {
        // No-op ... tested in `tearDown()`
    }

    @Test
    public void testFindUsers_noArgs() {
        createUser();
        Collection<DACUser> users = userDAO.findUsers();
        assertNotNull(users);
        assertFalse(users.isEmpty());
    }

    @Test
    public void testFindUsersWithProfileCompleted() {
        DACUser u = createUser();
        ResearcherProperty p = new ResearcherProperty();
        p.setPropertyKey(ResearcherFields.COMPLETED.getValue());
        p.setPropertyValue("true");
        p.setUserId(u.getDacUserId());
        researcherPropertyDAO.insertAll(Collections.singletonList(p));
        List<DACUser> users = new ArrayList<>(userDAO.findUsers());
        assertNotNull(users);
        assertFalse(users.isEmpty());
        assertEquals(1, users.size());
        assertTrue(users.get(0).getProfileCompleted());
    }

    @Test
    public void testVerifyAdminUsers() {
        createUserWithRole(UserRoles.ADMIN.getRoleId());
        Integer count = userDAO.verifyAdminUsers();
        assertNotNull(count);
        assertTrue(count > 0);
    }

    @Test
    public void testDescribeUsersByRoleAndEmailPreference() {
        DACUser researcher = createUserWithRole(UserRoles.RESEARCHER.getRoleId());
        userDAO.updateEmailPreference(true, researcher.getDacUserId());
        Collection<DACUser> researchers = userDAO.describeUsersByRoleAndEmailPreference("Researcher", true);
        assertFalse(researchers.isEmpty());

        DACUser owner = createUserWithRole(UserRoles.DATAOWNER.getRoleId());
        userDAO.updateEmailPreference(false, owner.getDacUserId());
        Collection<DACUser> dataOwners = userDAO.describeUsersByRoleAndEmailPreference("DataOwner", false);
        assertFalse(dataOwners.isEmpty());
    }

    @Test
    public void testUpdateEmailPreference() {
        // No-op ... tested in `testDescribeUsersByRoleAndEmailPreference()`
    }

    @Test
    public void testUpdateUserStatus() {
        DACUser user = createUser();
        Integer roleStatusId = RoleStatus.getValueByStatus(RoleStatus.APPROVED.name());
        String roleStatusName = getStatusByValue(roleStatusId);
        userDAO.updateUserStatus(roleStatusId, user.getDacUserId());
        DACUser user2 = userDAO.findDACUserById(user.getDacUserId());
        assertNotNull(user2);
        assertEquals(roleStatusName, user2.getStatus());
    }

    @Test
    public void testUpdateUserRationale() {
        DACUser user = createUser();
        String rationale = "New Rationale";
        userDAO.updateUserRationale(rationale, user.getDacUserId());
        DACUser user2 = userDAO.findDACUserById(user.getDacUserId());
        assertNotNull(user2);
        assertEquals(user2.getRationale(), rationale);
    }

    @Test
    public void testFindDACUserByEmailAndRoleId() {
        DACUser chair = createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
        DACUser user = userDAO.findDACUserByEmailAndRoleId(chair.getEmail(), UserRoles.CHAIRPERSON.getRoleId());
        assertNotNull(user);
        assertEquals(chair.getDacUserId(), user.getDacUserId());
        assertEquals(chair.getDisplayName(), user.getDisplayName());
    }

    @Test
    public void testFindUsersForElectionsByRoles() {
        DataSet dataset = createDataset();
        Dac dac = createDac();
        DACUser user = createUserWithRoleInDac(UserRoles.CHAIRPERSON.getRoleId(), dac.getDacId());
        Consent consent = createConsent(dac.getDacId());
        Election election = createElection(consent.getConsentId(), dataset.getDataSetId());
        createDacVote(user.getDacUserId(), election.getElectionId());

        Set<DACUser> users = userDAO.findUsersForElectionsByRoles(
                Collections.singletonList(election.getElectionId()),
                Collections.singletonList(UserRoles.CHAIRPERSON.getRoleName()));
        assertNotNull(users);
        assertFalse(users.isEmpty());
        assertEquals(1, users.size());
    }

    @Test
    public void testFindUsersForElectionsByRolesNotFound() {
        DataSet dataset = createDataset();
        Dac dac = createDac();
        DACUser user = createUserWithRoleInDac(UserRoles.MEMBER.getRoleId(), dac.getDacId());
        Consent consent = createConsent(dac.getDacId());
        Election election = createElection(consent.getConsentId(), dataset.getDataSetId());
        createDacVote(user.getDacUserId(), election.getElectionId());

        Set<DACUser> users = userDAO.findUsersForElectionsByRoles(
                Collections.singletonList(election.getElectionId()),
                Collections.singletonList(UserRoles.CHAIRPERSON.getRoleName()));
        assertNotNull(users);
        assertTrue(users.isEmpty());
    }

    @Test
    public void testFindUsersForDatasetsByRole() {
        DataSet dataset = createDataset();
        Dac dac = createDac();
        DACUser user = createUserWithRoleInDac(UserRoles.CHAIRPERSON.getRoleId(), dac.getDacId());
        Consent consent = createConsent(dac.getDacId());
        createAssociation(consent.getConsentId(), dataset.getDataSetId());

        Set<DACUser> users = userDAO.findUsersForDatasetsByRole(
                Collections.singletonList(dataset.getDataSetId()),
                Collections.singletonList(UserRoles.CHAIRPERSON.getRoleName()));
        Optional<DACUser> foundUser = users.stream().findFirst();
        assertNotNull(users);
        assertFalse(users.isEmpty());
        assertEquals(1, users.size());
        assertEquals(user.getDacUserId(), foundUser.get().getDacUserId());
    }

    @Test
    public void testFindUsersForDatasetsByRoleNotFound() {
        DataSet dataset = createDataset();
        Dac dac = createDac();
        createUserWithRoleInDac(UserRoles.MEMBER.getRoleId(), dac.getDacId());
        Consent consent = createConsent(dac.getDacId());
        createAssociation(consent.getConsentId(), dataset.getDataSetId());

        Set<DACUser> users = userDAO.findUsersForDatasetsByRole(
                Collections.singletonList(dataset.getDataSetId()),
                Collections.singletonList(UserRoles.CHAIRPERSON.getRoleName()));
        assertNotNull(users);
        assertTrue(users.isEmpty());
    }

    private String getRandomEmailAddress() {
        String user = RandomStringUtils.randomAlphanumeric(20);
        String domain = RandomStringUtils.randomAlphanumeric(10);
        return user + "@" + domain + ".org";
    }

}
