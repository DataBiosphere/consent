package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.enumeration.OrganizationType;
import org.broadinstitute.consent.http.enumeration.PropertyType;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DarCollection;
import org.broadinstitute.consent.http.models.DataAccessAgreement;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetEntry;
import org.broadinstitute.consent.http.models.DatasetProperty;
import org.broadinstitute.consent.http.models.Role;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DacDAOTest extends DAOTestHelper {

  @Test
  void testInsertWithoutEmail() {
    Dac dac = insertDac();
    assertNotNull(dac);
  }

  @Test
  void testInsertWithEmail() {
    Dac dac = insertDacWithEmail();
    assertNotNull(dac);
  }

  @Test
  void testFindDacsForEmail() {
    Dac dac = insertDacWithEmail();
    User chair = createUser();
    dacDAO.addDacMember(UserRoles.CHAIRPERSON.getRoleId(), chair.getUserId(), dac.getDacId());

    List<Dac> dacs = dacDAO.findDacsForEmail(chair.getEmail());
    assertEquals(1, dacs.size());
  }

  @Test
  void testFindAllDacMemberships() {
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
  void testFindAllDACUsersBySearchString_case1() {
    Dac dac = insertDacWithEmail();
    User chair = createUser(); // Creates a user with researcher role
    dacDAO.addDacMember(UserRoles.CHAIRPERSON.getRoleId(), chair.getUserId(), dac.getDacId());

    Set<User> users = dacDAO.findAllDACUsersBySearchString(chair.getEmail());
    assertFalse(users.isEmpty());
    assertEquals(1, users.size());
  }

  @Test
  void testFindAllDACUsersBySearchString_case2() {
    Set<User> users = dacDAO.findAllDACUsersBySearchString("random");
    assertTrue(users.isEmpty());
  }

  @Test
  void testFindByIdNoDaa() {
    Integer id = dacDAO.createDac(
        "Test_" + RandomStringUtils.random(20, true, true),
        "Test_" + RandomStringUtils.random(20, true, true),
        new Date());
    User user = createUser();
    Integer daaId = daaDAO.createDaa(user.getUserId(), new Date().toInstant(), user.getUserId(), new Date().toInstant(), id);
    DataAccessAgreement daa = daaDAO.findById(daaId);
    Dac dac = dacDAO.findById(id);
    assertEquals(id, dac.getDacId());
    assertNull(dac.getAssociatedDaa());
  }

  @Test
  void testFindByIdWithDaa() {
    Integer id = dacDAO.createDac(
        "Test_" + RandomStringUtils.random(20, true, true),
        "Test_" + RandomStringUtils.random(20, true, true),
        new Date());
    User user = createUser();
    Integer daaId = daaDAO.createDaa(user.getUserId(), new Date().toInstant(), user.getUserId(), new Date().toInstant(), id);
    DataAccessAgreement daa = daaDAO.findById(daaId);
    daaDAO.createDacDaaRelation(id, daaId);
    Dac dac = dacDAO.findById(id);
    DataAccessAgreement dacDaa = dac.getAssociatedDaa();
    assertEquals(id, dac.getDacId());
    assertEquals(daa.getDaaId(), dacDaa.getDaaId());
    assertEquals(daa.getCreateUserId(), dacDaa.getCreateUserId());
    assertEquals(daa.getCreateDate(), dacDaa.getCreateDate());
    assertEquals(daa.getUpdateUserId(), dacDaa.getUpdateUserId());
    assertEquals(daa.getUpdateDate(), dacDaa.getUpdateDate());
    assertEquals(daa.getInitialDacId(), dacDaa.getInitialDacId());
  }

  @Test
  void testCreateDac() {
    Integer id = dacDAO.createDac(
        "Test_" + RandomStringUtils.random(20, true, true),
        "Test_" + RandomStringUtils.random(20, true, true),
        new Date());
    Dac dac = dacDAO.findById(id);
    assertEquals(dac.getDacId(), id);
  }

  @Test
  void testUpdateDacWithoutEmail() {
    String newValue = "New Value";
    Dac dac = insertDac();
    dacDAO.updateDac(newValue, newValue, new Date(), dac.getDacId());
    Dac updatedDac = dacDAO.findById(dac.getDacId());

    assertEquals(updatedDac.getName(), newValue);
    assertEquals(updatedDac.getDescription(), newValue);
  }

  @Test
  void testUpdateDacWithEmail() {
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
  void testDeleteDacMembers() {
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
  void testDeleteDac() {
    Dac dac = insertDacWithEmail();
    assertNotNull(dac.getDacId());

    dacDAO.deleteDac(dac.getDacId());
    Dac deletedDac = dacDAO.findById(dac.getDacId());
    assertNull(deletedDac);
  }

  @Test
  void testFindMembersByDacId() {
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
  void testFindMembersByDacIdAndRoleId() {
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
  void testAddDacMember() {
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
  void testAddDacChair() {
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
  void testRemoveDacMember() {
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
  void testGetRoleById() {
    Role chair = dacDAO.getRoleById(UserRoles.CHAIRPERSON.getRoleId());
    assertEquals(chair.getName().toLowerCase(),
        UserRoles.CHAIRPERSON.getRoleName().toLowerCase());
    Role member = dacDAO.getRoleById(UserRoles.MEMBER.getRoleId());
    assertEquals(member.getName().toLowerCase(),
        UserRoles.MEMBER.getRoleName().toLowerCase());
  }

  @Test
  void testFindUserRolesForUser() {
    Dac dac = insertDacWithEmail();
    User chair = createUser(); // Creates a user with researcher role; UserRole #1
    dacDAO.addDacMember(UserRoles.CHAIRPERSON.getRoleId(), chair.getUserId(),
        dac.getDacId()); // ; UserRole #2
    List<UserRole> userRoles = dacDAO.findUserRolesForUser(chair.getUserId()).stream().distinct()
        .toList();
    assertEquals(userRoles.size(), 2);
  }

  @Test
  void testFindUserRolesForUsers() {
    Dac dac = insertDacWithEmail();
    User chair = createUser(); // Creates a user with researcher role; UserRole #1
    User member = createUser(); // Creates a user with researcher role; UserRole #2
    dacDAO.addDacMember(UserRoles.CHAIRPERSON.getRoleId(), chair.getUserId(),
        dac.getDacId()); // ; UserRole #3
    dacDAO.addDacMember(UserRoles.MEMBER.getRoleId(), member.getUserId(),
        dac.getDacId()); // ; UserRole #4
    List<Integer> userIds = Arrays.asList(chair.getUserId(), member.getUserId());
    List<UserRole> userRoles = dacDAO.findUserRolesForUsers(userIds).stream().distinct().toList();
    assertEquals(userRoles.size(), 4);
  }

  @Test
  void testFindDatasetsAssociatedWithDac_NoAssociated() {
    Dac dac = insertDacWithEmail();

    List<Dataset> results = datasetDAO.findDatasetsAssociatedWithDac(dac.getDacId());
    assertEquals(0, results.size());
  }

  @Test
  void testFindDatasetsAssociatedWithDac_AssignedDacId() {
    Dac dac = insertDacWithEmail();
    Dataset datasetAssignedDac = createDatasetWithDac(dac.getDacId());

    List<Dataset> results = datasetDAO.findDatasetsAssociatedWithDac(dac.getDacId());
    assertEquals(1, results.size());
    assertTrue(results.contains(datasetAssignedDac));
  }

  @Test
  void testFindDatasetsAssociatedWithDac_SuggestedDacId() {
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

  @Test
  void testFindDacsForCollectionId() {
    Dac dac = insertDacWithEmail();
    Dataset d1 = createDatasetWithDac(dac.getDacId());
    DarCollection collection = createDarCollection();
    createDataAccessRequestInCollectionWithDataset(collection, d1);

    Collection<Dac> results = dacDAO.findDacsForCollectionId(collection.getDarCollectionId());
    assertEquals(1, results.size());
    assertTrue(results.stream().map(d -> d.getDacId()).toList().contains(dac.getDacId()));
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

  private Dac createDac() {
    Integer id = dacDAO.createDac(
        "Test_" + RandomStringUtils.random(20, true, true),
        "Test_" + RandomStringUtils.random(20, true, true),
        new Date());
    return dacDAO.findById(id);
  }

  private User createUserWithInstitution() {
    int i1 = RandomUtils.nextInt(5, 10);
    String email = RandomStringUtils.randomAlphabetic(i1);
    String name = RandomStringUtils.randomAlphabetic(10);
    Integer userId = userDAO.insertUser(email, name, new Date());
    Integer institutionId = institutionDAO.insertInstitution(RandomStringUtils.randomAlphabetic(20),
        "itDirectorName",
        "itDirectorEmail",
        RandomStringUtils.randomAlphabetic(10),
        new Random().nextInt(),
        RandomStringUtils.randomAlphabetic(10),
        RandomStringUtils.randomAlphabetic(10),
        RandomStringUtils.randomAlphabetic(10),
        OrganizationType.NON_PROFIT.getValue(),
        userId,
        new Date());
    userDAO.updateUser(name, userId, institutionId);
    userRoleDAO.insertSingleUserRole(7, userId);
    return userDAO.findUserById(userId);
  }

  private DarCollection createDarCollection() {
    User user = createUserWithInstitution();
    String darCode = "DAR-" + RandomUtils.nextInt(1, 10000);
    Integer collection_id = darCollectionDAO.insertDarCollection(darCode, user.getUserId(),
        new Date());
    Dataset dataset = createDataset();
    DataAccessRequest dar = createDataAccessRequest(user.getUserId(), collection_id, darCode);
    dataAccessRequestDAO.insertDARDatasetRelation(dar.getReferenceId(), dataset.getDataSetId());
    createDataAccessRequest(user.getUserId(), collection_id, darCode);
    return darCollectionDAO.findDARCollectionByCollectionId(collection_id);
  }

  private DataAccessRequest createDataAccessRequest(Integer userId, Integer collectionId,
      String darCode) {
    DataAccessRequestData data = new DataAccessRequestData();
    data.setProjectTitle("Project Title: " + RandomStringUtils.random(50, true, false));
    data.setDarCode(darCode);
    DatasetEntry entry = new DatasetEntry();
    entry.setKey("key");
    entry.setValue("value");
    entry.setLabel("label");
    data.setDatasets(List.of(entry));
    data.setHmb(true);
    data.setMethods(false);
    String referenceId = UUID.randomUUID().toString();
    Date now = new Date();
    dataAccessRequestDAO.insertDataAccessRequest(
        collectionId,
        referenceId,
        userId,
        now, now, now, now,
        data);
    return dataAccessRequestDAO.findByReferenceId(referenceId);
  }

  private DataAccessRequest createDataAccessRequestInCollectionWithDataset(
      DarCollection collection,
      Dataset d
  ) {
    User user = createUser();
    String randomUUID = UUID.randomUUID().toString();
    dataAccessRequestDAO.insertDataAccessRequest(
        collection.getDarCollectionId(),
        randomUUID,
        user.getUserId(),
        new Date(),
        new Date(),
        new Date(),
        new Date(),
        new DataAccessRequestData()
    );
    dataAccessRequestDAO.insertDARDatasetRelation(randomUUID, d.getDataSetId());
    return dataAccessRequestDAO.findByReferenceId(randomUUID);
  }

  private Dataset createDataset() {
    User user = createUser();
    String name = "Name_" + RandomStringUtils.random(20, true, true);
    Timestamp now = new Timestamp(new Date().getTime());
    String objectId = "Object ID_" + RandomStringUtils.random(20, true, true);
    DataUse dataUse = new DataUseBuilder().setGeneralUse(true).build();
    Integer id = datasetDAO.insertDataset(name, now, user.getUserId(), objectId, dataUse.toString(), null);
    createDatasetProperties(id);
    return datasetDAO.findDatasetById(id);
  }

  private void createDatasetProperties(Integer datasetId) {
    List<DatasetProperty> list = new ArrayList<>();
    DatasetProperty dsp = new DatasetProperty();
    dsp.setDataSetId(datasetId);
    dsp.setPropertyKey(1);
    dsp.setPropertyValue("Test_PropertyValue");
    dsp.setCreateDate(new Date());
    list.add(dsp);
    datasetDAO.insertDatasetProperties(list);
  }

  private Dataset createDatasetWithDac(Integer dacId) {
    User user = createUser();
    String name = "Name_" + RandomStringUtils.random(20, true, true);
    Timestamp now = new Timestamp(new Date().getTime());
    String objectId = "Object ID_" + RandomStringUtils.random(20, true, true);
    DataUse dataUse = new DataUseBuilder().setGeneralUse(true).build();
    Integer id = datasetDAO.insertDataset(name, now, user.getUserId(), objectId,
        dataUse.toString(), dacId);
    createDatasetProperties(id);
    return datasetDAO.findDatasetById(id);
  }

}
