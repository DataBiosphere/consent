package org.broadinstitute.consent.http.db;

import org.apache.commons.lang3.RandomStringUtils;
import org.broadinstitute.consent.http.enumeration.RoleStatus;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.DACUser;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.Election;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.broadinstitute.consent.http.enumeration.RoleStatus.getStatusByValue;

public class UserDAOTest extends DAOTestHelper {

    @Test
    public void testFindDACUserById() {
        DACUser user = createUser();
        Assert.assertNotNull(user);
        DACUser user2 = userDAO.findDACUserById(user.getDacUserId());
        Assert.assertNotNull(user2);
        Assert.assertEquals(user.getEmail(), user2.getEmail());
    }

    @Test
    public void testFindUsers_withIdCollection() {
        DACUser user = createUser();
        Collection<DACUser> users = userDAO.findUsers(Collections.singletonList(user.getDacUserId()));
        Assert.assertNotNull(users);
        Assert.assertFalse(users.isEmpty());
        Assert.assertEquals(1, users.size());
    }

    @Test
    public void testDescribeUsersByRole() {
        createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
        createUserWithRole(UserRoles.MEMBER.getRoleId());

        List<DACUser> members = userDAO.describeUsersByRole(UserRoles.MEMBER.getRoleName());
        Assert.assertFalse(members.isEmpty());

        List<DACUser> chairs = userDAO.describeUsersByRole(UserRoles.CHAIRPERSON.getRoleName());
        Assert.assertFalse(chairs.isEmpty());

        // Only case where we don't set up users by default.
        List<DACUser> alumni = userDAO.describeUsersByRole(UserRoles.ALUMNI.getRoleName());
        Assert.assertTrue(alumni.isEmpty());

        List<DACUser> admins = userDAO.describeUsersByRole(UserRoles.ADMIN.getRoleName());
        Assert.assertFalse(admins.isEmpty());

        List<DACUser> researchers = userDAO.describeUsersByRole(UserRoles.RESEARCHER.getRoleName());
        Assert.assertFalse(researchers.isEmpty());

        List<DACUser> dataOwners = userDAO.describeUsersByRole(UserRoles.DATAOWNER.getRoleName());
        Assert.assertFalse(dataOwners.isEmpty());
    }

    @Test
    public void testCheckChairpersonUser() {
        DACUser chair = createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
        DACUser member = createUserWithRole(UserRoles.MEMBER.getRoleId());
        Assert.assertNotNull(userDAO.checkChairpersonUser(chair.getDacUserId()));
        Assert.assertNull(userDAO.checkChairpersonUser(member.getDacUserId()));
    }

    @Test
    public void testFindDACUsersEnabledToVoteByDacEmpty() {
        Dac dac = createDac();
        Collection<DACUser> users = userDAO.findDACUsersEnabledToVoteByDAC(dac.getDacId());
        Assert.assertTrue(users.isEmpty());
    }

    @Test
    public void testFindDACUsersEnabledToVoteByDacNotEmpty() {
        Dac dac = createDac();
        DACUser chair = createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
        dacDAO.addDacMember(UserRoles.CHAIRPERSON.getRoleId(), chair.getDacUserId(), dac.getDacId());
        Collection<DACUser> users = userDAO.findDACUsersEnabledToVoteByDAC(dac.getDacId());
        Assert.assertFalse(users.isEmpty());
    }

    @Test
    public void testFindNonDACUsersEnabledToVote() {
        createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
        createUserWithRole(UserRoles.MEMBER.getRoleId());
        Collection<DACUser> users = userDAO.findNonDACUsersEnabledToVote();
        Assert.assertFalse(users.isEmpty());
    }

    @Test
    public void testFindUsersWithRoles() {
        DACUser chair = createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
        Collection<Integer> userIds = Collections.singletonList(chair.getDacUserId());
        Collection<DACUser> users = userDAO.findUsersWithRoles(userIds);
        users.forEach(u -> Assert.assertFalse("User: " + u.getDacUserId() + " has no roles", u.getRoles().isEmpty()));
    }

    @Test
    public void testFindDACUserByEmail() {
        DACUser user = createUser();
        DACUser user1 = userDAO.findDACUserByEmail(user.getEmail());
        Assert.assertNotNull(user1);
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
        Assert.assertEquals(user2.getEmail(), newEmail);
    }

    @Test
    public void testDeleteDACUserByEmail() {
        // No-op ... tested in `tearDown()`
    }

    @Test
    public void testFindUsers_noArgs() {
        createUser();
        Collection<DACUser> users = userDAO.findUsers();
        Assert.assertNotNull(users);
        Assert.assertFalse(users.isEmpty());
    }

    @Test
    public void testVerifyAdminUsers() {
        createUserWithRole(UserRoles.ADMIN.getRoleId());
        Integer count = userDAO.verifyAdminUsers();
        Assert.assertNotNull(count);
        Assert.assertTrue(count > 0);
    }

    @Test
    public void testDescribeUsersByRoleAndEmailPreference() {
        DACUser researcher = createUserWithRole(UserRoles.RESEARCHER.getRoleId());
        userDAO.updateEmailPreference(true, researcher.getDacUserId());
        Collection<DACUser> researchers = userDAO.describeUsersByRoleAndEmailPreference("Researcher", true);
        Assert.assertFalse(researchers.isEmpty());

        DACUser owner = createUserWithRole(UserRoles.DATAOWNER.getRoleId());
        userDAO.updateEmailPreference(false, owner.getDacUserId());
        Collection<DACUser> dataOwners = userDAO.describeUsersByRoleAndEmailPreference("DataOwner", false);
        Assert.assertFalse(dataOwners.isEmpty());
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
        Assert.assertNotNull(user2);
        Assert.assertEquals(roleStatusName, user2.getStatus());
    }

    @Test
    public void testUpdateUserRationale() {
        DACUser user = createUser();
        String rationale = "New Rationale";
        userDAO.updateUserRationale(rationale, user.getDacUserId());
        DACUser user2 = userDAO.findDACUserById(user.getDacUserId());
        Assert.assertNotNull(user2);
        Assert.assertEquals(user2.getRationale(), rationale);
    }

    @Test
    public void testFindDACUserByEmailAndRoleId() {
        DACUser chair = createUserWithRole(UserRoles.CHAIRPERSON.getRoleId());
        DACUser user = userDAO.findDACUserByEmailAndRoleId(chair.getEmail(), UserRoles.CHAIRPERSON.getRoleId());
        Assert.assertNotNull(user);
        Assert.assertEquals(chair.getDacUserId(), user.getDacUserId());
        Assert.assertEquals(chair.getDisplayName(), user.getDisplayName());
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
        Assert.assertNotNull(users);
        Assert.assertFalse(users.isEmpty());
        Assert.assertEquals(1, users.size());
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
        Assert.assertNotNull(users);
        Assert.assertTrue(users.isEmpty());
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
        Assert.assertNotNull(users);
        Assert.assertFalse(users.isEmpty());
        Assert.assertEquals(1, users.size());
        Assert.assertEquals(user.getDacUserId(), foundUser.get().getDacUserId());
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
        Assert.assertNotNull(users);
        Assert.assertTrue(users.isEmpty());
    }

    private String getRandomEmailAddress() {
        String user = RandomStringUtils.randomAlphanumeric(20);
        String domain = RandomStringUtils.randomAlphanumeric(10);
        return user + "@" + domain + ".org";
    }

}
