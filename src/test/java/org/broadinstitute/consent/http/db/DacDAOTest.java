package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.ws.rs.core.MediaType;
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
import org.broadinstitute.consent.http.enumeration.FileCategory;
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
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@TestMethodOrder(MethodOrderer.Random.class)
class DacDAOTest extends DAOTestHelper {

  @Test
  void testInsertWithoutEmail() {
    Integer dacId = createRandomDAC();
    Dac dac = dacDAO.findById(dacId);
    assertNotNull(dac);
  }

  @Test
  void testInsertWithEmail() {
    Dac dac = insertDacWithEmail();
    assertNotNull(dac);
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
  void testFindAllNoDatasets() {
    Integer dacId1 = createRandomDAC();
    Integer dacId2 = createRandomDAC();
    List<Dac> dacs = dacDAO.findAll();

    Dac dac1 = dacs.get(0);
    assertEquals(dacId1, dac1.getDacId());
    assertEquals(0, dac1.getDatasetIds().size());
    assertNull(dac1.getAssociatedDaa());

    Dac dac2 = dacs.get(1);
    assertEquals(dacId2, dac2.getDacId());
    assertEquals(0, dac2.getDatasetIds().size());
    assertNull(dac2.getAssociatedDaa());
  }

  @Test
  void testFindAllWithDataset() {
    Integer dacId1 = createRandomDAC();
    Integer dacId2 = createRandomDAC();
    User user = createUser();
    Integer datasetId = datasetDAO.insertDataset(randomAlphabetic(20),
        new Timestamp(new Date().getTime()), user.getUserId(), randomAlphabetic(20),
        new DataUseBuilder().setGeneralUse(true).build().toString(), dacId1);
    List<DatasetProperty> list = new ArrayList<>();
    DatasetProperty dsp = new DatasetProperty();
    dsp.setDatasetId(datasetId);
    dsp.setPropertyKey(1);
    dsp.setPropertyValue("Test_PropertyValue");
    dsp.setCreateDate(new Date());
    list.add(dsp);
    datasetDAO.insertDatasetProperties(list);
    Integer datasetId2 = datasetDAO.insertDataset(randomAlphabetic(20),
        new Timestamp(new Date().getTime()), user.getUserId(), randomAlphabetic(20),
        new DataUseBuilder().setGeneralUse(true).build().toString(), dacId2);

    List<Dac> dacs = dacDAO.findAll();

    Dac dac1 = dacs.get(0);
    List<Integer> datasetIds = dac1.getDatasetIds();
    assertEquals(dacId1, dac1.getDacId());
    assertEquals(1, datasetIds.size());
    assertEquals(datasetId, datasetIds.get(0));
    assertNull(dac1.getAssociatedDaa());

    Dac dac2 = dacs.get(1);
    List<Integer> datasetIds2 = dac2.getDatasetIds();
    assertEquals(dacId2, dac2.getDacId());
    assertEquals(1, datasetIds2.size());
    assertEquals(datasetId2, datasetIds2.get(0));
    assertNull(dac2.getAssociatedDaa());
  }

  @Test
  void testFindAllWithDAAs() {
    User user = createUser();

    Integer dacId1 = createRandomDAC();
    Integer daaId1 = daaDAO.createDaa(user.getUserId(), new Date().toInstant(), user.getUserId(),
        new Date().toInstant(), dacId1);
    createFSO(user.getUserId(), daaId1);
    daaDAO.createDacDaaRelation(dacId1, daaId1);

    Integer dacId2 = createRandomDAC();
    Integer daaId2 = daaDAO.createDaa(user.getUserId(), new Date().toInstant(), user.getUserId(),
        new Date().toInstant(), dacId2);
    createFSO(user.getUserId(), daaId2);
    daaDAO.createDacDaaRelation(dacId2, daaId2);

    dacDAO.findAll().forEach(dac -> {
      assertNotNull(dac.getAssociatedDaa());
      assertNotNull(dac.getAssociatedDaa().getFile());
    });
  }

  @Test
  void testFindByIdNoDaa() {
    Integer id = createRandomDAC();
    Dac dac = dacDAO.findById(id);
    assertEquals(id, dac.getDacId());
    assertNull(dac.getAssociatedDaa());
  }

  @Test
  void testFindByIdWithDaa() {
    Integer id = createRandomDAC();
    User user = createUser();
    Integer daaId = daaDAO.createDaa(user.getUserId(), new Date().toInstant(), user.getUserId(),
        new Date().toInstant(), id);
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
    Integer id = createRandomDAC();
    Dac dac = dacDAO.findById(id);
    assertEquals(dac.getDacId(), id);
  }

  @Test
  void testUpdateDacWithoutEmail() {
    String newValue = "New Value";
    Integer dacId = createRandomDAC();
    dacDAO.updateDac(newValue, newValue, new Date(), dacId);
    Dac updatedDac = dacDAO.findById(dacId);

    assertEquals(newValue, updatedDac.getName());
    assertEquals(newValue, updatedDac.getDescription());
  }

  @Test
  void testUpdateDacWithEmail() {
    String newValue = "New Value";
    String newEmail = "new_email@test.com";
    Dac dac = insertDacWithEmail();
    dacDAO.updateDac(newValue, newValue, newEmail, new Date(), dac.getDacId());
    Dac updatedDac = dacDAO.findById(dac.getDacId());

    assertEquals(newValue, updatedDac.getName());
    assertEquals(newValue, updatedDac.getDescription());
    assertEquals(newEmail, updatedDac.getEmail());
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
    assertEquals(4, dacMembers.size());
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
    assertEquals(1, chairs.size());

    List<User> members = dacDAO.findMembersByDacIdAndRoleId(dac.getDacId(), memberRoleId);
    assertNotNull(members);
    assertFalse(members.isEmpty());
    assertEquals(3, members.size());
  }

  @Test
  void testAddDacMember() {
    Dac dac = insertDacWithEmail();
    Integer roleId = UserRoles.MEMBER.getRoleId();
    User user = createUser();
    dacDAO.addDacMember(roleId, user.getUserId(), dac.getDacId());
    List<UserRole> memberRoles = userDAO.findUserById(user.getUserId()).getRoles();
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
    List<UserRole> chairRoles = userDAO.findUserById(user.getUserId()).getRoles();
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
    List<UserRole> userRoles = userDAO.findUserById(user2.getUserId()).getRoles();
    userRoles.forEach(userRole -> dacDAO.removeDacMember(userRole.getUserRoleId()));
    List<UserRole> userRolesRemoved = userDAO.findUserById(user2.getUserId()).getRoles();
    assertNull(userRolesRemoved);
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
    List<UserRole> userRoles = userDAO.findUserById(chair.getUserId()).getRoles();
    assertEquals(2, userRoles.size());
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
    assertEquals(4, userRoles.size());
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
        datasetSuggestedDac.getDatasetId(),
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
    assertTrue(results.stream().map(Dac::getDacId).toList().contains(dac.getDacId()));
  }

  private Dac insertDacWithEmail() {
    String testEmail = "test@email.com";
    Integer id = dacDAO.createDac(
        "Test_" + randomAlphabetic(20),
        "Test_" + randomAlphabetic(20),
        testEmail,
        new Date());
    return dacDAO.findById(id);
  }

  private Dac createDac() {
    Integer id = createRandomDAC();
    return dacDAO.findById(id);
  }

  private User createUserWithInstitution() {
    int i1 = randomInt(5, 10);
    String email = randomAlphabetic(i1);
    String name = randomAlphabetic(10);
    Integer userId = userDAO.insertUser(email, name, new Date());
    Integer institutionId = institutionDAO.insertInstitution(randomAlphabetic(20),
        "itDirectorName",
        "itDirectorEmail",
        randomAlphabetic(10),
        new Random().nextInt(),
        randomAlphabetic(10),
        randomAlphabetic(10),
        randomAlphabetic(10),
        OrganizationType.NON_PROFIT.getValue(),
        userId,
        new Date());
    userDAO.updateUser(name, userId, institutionId);
    userRoleDAO.insertSingleUserRole(7, userId);
    return userDAO.findUserById(userId);
  }

  private DarCollection createDarCollection() {
    User user = createUserWithInstitution();
    String darCode = "DAR-" + randomInt(1, 10000);
    Integer collectionId = darCollectionDAO.insertDarCollection(darCode, user.getUserId(),
        new Date());
    Dataset dataset = createDataset();
    DataAccessRequest dar = createDataAccessRequest(user.getUserId(), collectionId);
    dataAccessRequestDAO.insertDARDatasetRelation(dar.getReferenceId(), dataset.getDatasetId());
    createDataAccessRequest(user.getUserId(), collectionId);
    return darCollectionDAO.findDARCollectionByCollectionId(collectionId);
  }

  private DataAccessRequest createDataAccessRequest(Integer userId, Integer collectionId) {
    DataAccessRequestData data = new DataAccessRequestData();
    data.setProjectTitle("Project Title: " + randomAlphabetic(50));
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

  private void createDataAccessRequestInCollectionWithDataset(
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
    dataAccessRequestDAO.insertDARDatasetRelation(randomUUID, d.getDatasetId());
  }

  private Dataset createDataset() {
    User user = createUser();
    String name = "Name_" + randomAlphabetic(20);
    Timestamp now = new Timestamp(new Date().getTime());
    String objectId = "Object ID_" + randomAlphabetic(20);
    DataUse dataUse = new DataUseBuilder().setGeneralUse(true).build();
    Integer id = datasetDAO.insertDataset(name, now, user.getUserId(), objectId, dataUse.toString(),
        null);
    createDatasetProperties(id);
    return datasetDAO.findDatasetById(id);
  }

  private void createDatasetProperties(Integer datasetId) {
    List<DatasetProperty> list = new ArrayList<>();
    DatasetProperty dsp = new DatasetProperty();
    dsp.setDatasetId(datasetId);
    dsp.setPropertyKey(1);
    dsp.setPropertyValue("Test_PropertyValue");
    dsp.setCreateDate(new Date());
    list.add(dsp);
    datasetDAO.insertDatasetProperties(list);
  }

  private Dataset createDatasetWithDac(Integer dacId) {
    User user = createUser();
    String name = "Name_" + randomAlphabetic(20);
    Timestamp now = new Timestamp(new Date().getTime());
    String objectId = "Object ID_" + randomAlphabetic(20);
    DataUse dataUse = new DataUseBuilder().setGeneralUse(true).build();
    Integer id = datasetDAO.insertDataset(name, now, user.getUserId(), objectId,
        dataUse.toString(), dacId);
    createDatasetProperties(id);
    return datasetDAO.findDatasetById(id);
  }

  private Integer createRandomDAC() {
    return dacDAO.createDac(
        "Test_" + randomAlphabetic(20),
        "Test_" + randomAlphabetic(20),
        new Date());
  }

  private void createFSO(Integer userId, Integer daaId) {
    fileStorageObjectDAO.insertNewFile(
        randomAlphabetic(10),
        FileCategory.DATA_ACCESS_AGREEMENT.getValue(),
        randomAlphabetic(10),
        MediaType.TEXT_PLAIN_TYPE.getType(),
        daaId.toString(),
        userId,
        Instant.now()
    );

  }
}
