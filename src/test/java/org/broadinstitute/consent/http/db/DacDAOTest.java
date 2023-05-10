package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.RandomStringUtils;
import org.broadinstitute.consent.http.enumeration.PropertyType;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetProperty;
import org.broadinstitute.consent.http.models.Role;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DacDAOTest extends DAOTestHelper {

    @Test
    public void testInsertWithoutEmail() {
        Dac dac = insertDac();
        assertNotNull(dac);
    }

    @Test
    public void testInsertWithEmail() {
        Dac dac = insertDacWithEmail();
        assertNotNull(dac);
    }

    @Test
    public void testFindAll() {
        int count = 4;
        for (int i = 1; i <= count; i++) {
            Dac d = insertDacWithEmail();
            Dataset ds = createDatasetWithDac(d.getDacId());
            Consent c = createConsent();
            consentDAO.insertConsentAssociation(c.getConsentId(), ASSOCIATION_TYPE_TEST, ds.getDataSetId());
        }
        List<Dac> dacList = dacDAO.findAll();
        assertEquals(count, dacList.size());
    }

    @Test
    public void testFindDacsForEmail() {
        Dac dac = insertDacWithEmail();
        User chair = createUser();
        dacDAO.addDacMember(UserRoles.CHAIRPERSON.getRoleId(), chair.getUserId(), dac.getDacId());

        List<Dac> dacs = dacDAO.findDacsForEmail(chair.getEmail());
        assertEquals(1, dacs.size());
    }

    @Test
    public void testFindAllDacMemberships() {
        List<Dac> dacs = new ArrayList<>();
        dacs.add(createDac());
        dacs.add(createDac());
        for (Dac dac : dacs) {
            User chair = createUser();
            dacDAO.addDacMember(UserRoles.CHAIRPERSON.getRoleId(), chair.getUserId(), dac.getDacId());
            User member1 = createUser();
            dacDAO.addDacMember(UserRoles.MEMBER.getRoleId(), member1.getUserId(), dac.getDacId());
            User member2 = createUser();
            dacDAO.addDacMember(UserRoles.MEMBER.getRoleId(), member2.getUserId(), dac.getDacId());
        }
        List<User> allUsers = dacDAO.findAllDACUserMemberships();
        assertEquals(6, allUsers.size());
    }

    @Test
    public void testFindAllDACUsersBySearchString_case1() {
        Dac dac = insertDacWithEmail();
        User chair = createUser(); // Creates a user with researcher role
        dacDAO.addDacMember(UserRoles.CHAIRPERSON.getRoleId(), chair.getUserId(), dac.getDacId());

        Set<User> users = dacDAO.findAllDACUsersBySearchString(chair.getEmail());
        assertFalse(users.isEmpty());
        assertEquals(1, users.size());
    }

    @Test
    public void testFindAllDACUsersBySearchString_case2() {
        Set<User> users = dacDAO.findAllDACUsersBySearchString("random");
        assertTrue(users.isEmpty());
    }

    @Test
    public void testFindById() {
        Integer id = dacDAO.createDac(
                "Test_" + RandomStringUtils.random(20, true, true),
                "Test_" + RandomStringUtils.random(20, true, true),
                new Date());
        Dac dac = dacDAO.findById(id);
        assertEquals(id, dac.getDacId());
    }

    @Test
    public void testCreateDac() {
        Integer id = dacDAO.createDac(
                "Test_" + RandomStringUtils.random(20, true, true),
                "Test_" + RandomStringUtils.random(20, true, true),
                new Date());
        Dac dac = dacDAO.findById(id);
        assertEquals(dac.getDacId(), id);
    }

    @Test
    public void testUpdateDacWithoutEmail() {
        String newValue = "New Value";
        Dac dac = insertDac();
        dacDAO.updateDac(newValue, newValue, new Date(), dac.getDacId());
        Dac updatedDac = dacDAO.findById(dac.getDacId());

        assertEquals(updatedDac.getName(), newValue);
        assertEquals(updatedDac.getDescription(), newValue);
    }

    @Test
    public void testUpdateDacWithEmail() {
        String newValue = "New Value";
        String newEmail = "new_email@test.com";
        Dac dac = insertDacWithEmail();
        dacDAO.updateDac(newValue, newValue, newEmail, new Date(), dac.getDacId());
        Dac updatedDac = dacDAO.findById(dac.getDacId());

        assertEquals(updatedDac.getName(), newValue);
        assertEquals(updatedDac.getDescription(), newValue);
        assertEquals(updatedDac.getEmail(), newEmail);
    }

    @Test
    public void testDeleteDacMembers() {
        Dac dac = insertDacWithEmail();
        Integer memberRoleId = UserRoles.MEMBER.getRoleId();
        User user1 = createUser();
        dacDAO.addDacMember(memberRoleId, user1.getUserId(), dac.getDacId());
        User user2 = createUser();
        dacDAO.addDacMember(memberRoleId, user2.getUserId(), dac.getDacId());

        dacDAO.deleteDacMembers(dac.getDacId());
        List<User> dacMembers = dacDAO.findMembersByDacId(dac.getDacId());
        assertTrue(dacMembers.isEmpty());
    }

    @Test
    public void testDeleteDac() {
        Dac dac = insertDacWithEmail();
        assertNotNull(dac.getDacId());

        dacDAO.deleteDac(dac.getDacId());
        Dac deletedDac = dacDAO.findById(dac.getDacId());
        Assertions.assertNull(deletedDac);
    }

    @Test
    public void testFindMembersByDacId() {
        Dac dac = insertDacWithEmail();
        Integer chairRoleId = UserRoles.CHAIRPERSON.getRoleId();
        Integer memberRoleId = UserRoles.MEMBER.getRoleId();
        User user1 = createUser();
        dacDAO.addDacMember(memberRoleId, user1.getUserId(), dac.getDacId());
        User user2 = createUser();
        dacDAO.addDacMember(memberRoleId, user2.getUserId(), dac.getDacId());
        User user3 = createUser();
        dacDAO.addDacMember(memberRoleId, user3.getUserId(), dac.getDacId());
        User user4 = createUser();
        dacDAO.addDacMember(chairRoleId, user4.getUserId(), dac.getDacId());

        List<User> dacMembers = dacDAO.findMembersByDacId(dac.getDacId());
        assertNotNull(dacMembers);
        assertFalse(dacMembers.isEmpty());
        assertEquals(dacMembers.size(), 4);
    }

    @Test
    public void testFindMembersByDacIdAndRoleId() {
        Dac dac = insertDacWithEmail();
        Integer chairRoleId = UserRoles.CHAIRPERSON.getRoleId();
        Integer memberRoleId = UserRoles.MEMBER.getRoleId();
        User user1 = createUser();
        dacDAO.addDacMember(memberRoleId, user1.getUserId(), dac.getDacId());
        User user2 = createUser();
        dacDAO.addDacMember(memberRoleId, user2.getUserId(), dac.getDacId());
        User user3 = createUser();
        dacDAO.addDacMember(memberRoleId, user3.getUserId(), dac.getDacId());
        User user4 = createUser();
        dacDAO.addDacMember(chairRoleId, user4.getUserId(), dac.getDacId());

        List<User> chairs = dacDAO.findMembersByDacIdAndRoleId(dac.getDacId(), chairRoleId);
        assertNotNull(chairs);
        assertFalse(chairs.isEmpty());
        assertEquals(chairs.size(), 1);

        List<User> members = dacDAO.findMembersByDacIdAndRoleId(dac.getDacId(), memberRoleId);
        assertNotNull(members);
        assertFalse(members.isEmpty());
        assertEquals(members.size(), 3);
    }

    @Test
    public void testAddDacMember() {
        Dac dac = insertDacWithEmail();
        Integer roleId = UserRoles.MEMBER.getRoleId();
        User user = createUser();
        dacDAO.addDacMember(roleId, user.getUserId(), dac.getDacId());
        List<UserRole> memberRoles = dacDAO.findUserRolesForUser(user.getUserId());
        assertFalse(memberRoles.isEmpty());
        UserRole userRole = memberRoles.get(0);
        assertEquals(userRole.getDacId(), dac.getDacId());
        assertEquals(userRole.getRoleId(), roleId);
    }

    @Test
    public void testAddDacChair() {
        Dac dac = insertDacWithEmail();
        Integer roleId = UserRoles.CHAIRPERSON.getRoleId();
        User user = createUser();
        dacDAO.addDacMember(roleId, user.getUserId(), dac.getDacId());
        List<UserRole> chairRoles = dacDAO.findUserRolesForUser(user.getUserId());
        assertFalse(chairRoles.isEmpty());
        UserRole userRole = chairRoles.get(0);
        assertEquals(userRole.getDacId(), dac.getDacId());
        assertEquals(userRole.getRoleId(), roleId);
    }

    @Test
    public void testRemoveDacMember() {
        Dac dac = insertDacWithEmail();
        Integer chairRoleId = UserRoles.CHAIRPERSON.getRoleId();
        Integer memberRoleId = UserRoles.MEMBER.getRoleId();
        User user1 = createUser();
        dacDAO.addDacMember(memberRoleId, user1.getUserId(), dac.getDacId());
        User user2 = createUser();
        dacDAO.addDacMember(chairRoleId, user2.getUserId(), dac.getDacId());
        List<UserRole> userRoles = dacDAO.findUserRolesForUser(user2.getUserId());
        userRoles.forEach(userRole -> dacDAO.removeDacMember(userRole.getUserRoleId()));
        List<UserRole> userRolesRemoved = dacDAO.findUserRolesForUser(user2.getUserId());
        assertTrue(userRolesRemoved.isEmpty());
    }

    @Test
    public void testGetRoleById() {
        Role chair = dacDAO.getRoleById(UserRoles.CHAIRPERSON.getRoleId());
        assertEquals(chair.getName().toLowerCase(),
            UserRoles.CHAIRPERSON.getRoleName().toLowerCase());
        Role member = dacDAO.getRoleById(UserRoles.MEMBER.getRoleId());
        assertEquals(member.getName().toLowerCase(),
            UserRoles.MEMBER.getRoleName().toLowerCase());
    }

    @Test
    public void testFindUserRolesForUser() {
        Dac dac = insertDacWithEmail();
        User chair = createUser(); // Creates a user with researcher role; UserRole #1
        dacDAO.addDacMember(UserRoles.CHAIRPERSON.getRoleId(), chair.getUserId(), dac.getDacId()); // ; UserRole #2
        List<UserRole> userRoles = dacDAO.findUserRolesForUser(chair.getUserId()).stream().distinct().toList();
        assertEquals(userRoles.size(), 2);
    }

    @Test
    public void testFindUserRolesForUsers() {
        Dac dac = insertDacWithEmail();
        User chair = createUser(); // Creates a user with researcher role; UserRole #1
        User member = createUser(); // Creates a user with researcher role; UserRole #2
        dacDAO.addDacMember(UserRoles.CHAIRPERSON.getRoleId(), chair.getUserId(), dac.getDacId()); // ; UserRole #3
        dacDAO.addDacMember(UserRoles.MEMBER.getRoleId(), member.getUserId(), dac.getDacId()); // ; UserRole #4
        List<Integer> userIds = Arrays.asList(chair.getUserId(), member.getUserId());
        List<UserRole> userRoles = dacDAO.findUserRolesForUsers(userIds).stream().distinct().toList();
        assertEquals(userRoles.size(), 4);
    }

    @Test
    public void testFindDacsForDatasetIds() {
        Dac dac = insertDacWithEmail();
        Consent consent1 = createConsent();
        Dataset dataset1 = createDatasetWithDac(dac.getDacId());
        consentDAO.insertConsentAssociation(consent1.getConsentId(), ASSOCIATION_TYPE_TEST, dataset1.getDataSetId());

        Consent consent2 = createConsent();
        Dataset dataset2 = createDatasetWithDac(dac.getDacId());
        consentDAO.insertConsentAssociation(consent2.getConsentId(), ASSOCIATION_TYPE_TEST, dataset2.getDataSetId());
        Set<Dac> dacs = dacDAO.findDacsForDatasetIds(Arrays.asList(dataset1.getDataSetId(), dataset2.getDataSetId()));
        assertFalse(dacs.isEmpty());
        assertEquals(1, dacs.size());
    }

    @Test
    public void testFindDatasetsAssociatedWithDac_NoAssociated() {
        Dac dac = insertDacWithEmail();

        List<Dataset> results = datasetDAO.findDatasetsAssociatedWithDac(dac.getDacId());
        assertEquals(0, results.size());
    }

    @Test
    public void testFindDatasetsAssociatedWithDac_AssignedDacId() {
        Dac dac = insertDacWithEmail();
        Dataset datasetAssignedDac = createDatasetWithDac(dac.getDacId());

        List<Dataset> results = datasetDAO.findDatasetsAssociatedWithDac(dac.getDacId());
        assertEquals(1, results.size());
        assertTrue(results.contains(datasetAssignedDac));
    }

    @Test
    public void testFindDatasetsAssociatedWithDac_SuggestedDacId() {
        Dac dac = insertDacWithEmail();

        Dataset datasetSuggestedDac = createDataset();
        datasetDAO.insertDatasetProperties(List.of(new DatasetProperty(
                1,
                datasetSuggestedDac.getDataSetId(),
                1,
                "dataAccessCommitteeId",
                dac.getDacId().toString(),
                PropertyType.Number,
                Date.from(Instant.now()))));

        List<Dataset> results = datasetDAO.findDatasetsAssociatedWithDac(dac.getDacId());
        assertEquals(1, results.size());
        assertTrue(results.contains(datasetSuggestedDac));
    }

    private Dac insertDac() {
        Integer id = dacDAO.createDac(
                "Test_" + RandomStringUtils.random(20, true, true),
                "Test_" + RandomStringUtils.random(20, true, true),
                new Date());
        return dacDAO.findById(id);
    }

    private Dac insertDacWithEmail() {
        String testEmail = "test@email.com";
        Integer id = dacDAO.createDac(
                "Test_" + RandomStringUtils.random(20, true, true),
                "Test_" + RandomStringUtils.random(20, true, true),
                testEmail,
                new Date());
        return dacDAO.findById(id);
    }

}
