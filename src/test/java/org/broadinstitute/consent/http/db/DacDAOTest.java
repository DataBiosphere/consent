package org.broadinstitute.consent.http.db;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
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
import java.util.Set;
import java.util.UUID;
import org.broadinstitute.consent.http.enumeration.AuditActions;
import org.broadinstitute.consent.http.enumeration.FileCategory;
import org.broadinstitute.consent.http.enumeration.PropertyType;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DacAudit;
import org.broadinstitute.consent.http.models.DarCollection;
import org.broadinstitute.consent.http.models.DataAccessAgreement;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.models.Dataset;
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
      dacDAO.addDacMember(
          UserRoles.CHAIRPERSON.getRoleId(), chair.getUserId(), dac.getDacId(), chair.getUserId());
      User member1 = createUser();
      dacDAO.addDacMember(
          UserRoles.MEMBER.getRoleId(), member1.getUserId(), dac.getDacId(), member1.getUserId());
      User member2 = createUser();
      dacDAO.addDacMember(
          UserRoles.MEMBER.getRoleId(), member2.getUserId(), dac.getDacId(), member2.getUserId());
    }
    List<User> allUsers = dacDAO.findAllDACUserMemberships();
    assertEquals(6, allUsers.size());
  }

  @Test
  void testFindAllDACUsersBySearchString_case1() {
    Dac dac = insertDacWithEmail();
    User chair = createUser(); // Creates a user with researcher role
    dacDAO.addDacMember(
        UserRoles.CHAIRPERSON.getRoleId(), chair.getUserId(), dac.getDacId(), chair.getUserId());

    Set<User> users = dacDAO.findAllDACUsersBySearchString(chair.getEmail());
    assertThat(users, hasSize(1));
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

    Dac dac1 = dacs.stream().filter(d -> d.getDacId().equals(dacId1)).findFirst().orElseThrow();
    assertEquals(0, dac1.getDatasetIds().size());
    assertNull(dac1.getAssociatedDaa());

    Dac dac2 = dacs.stream().filter(d -> d.getDacId().equals(dacId2)).findFirst().orElseThrow();
    assertEquals(0, dac2.getDatasetIds().size());
    assertNull(dac2.getAssociatedDaa());
  }

  @Test
  void testFindAllAlphabeticized() {
    String firstName = "A" + randomAlphabetic(20);
    String secondName = "B" + randomAlphabetic(20);
    String thirdName = "C" + randomAlphabetic(20);
    createRandomDACWithName(firstName);
    createRandomDACWithName(thirdName);
    createRandomDACWithName(secondName);
    List<Dac> dacs = dacDAO.findAll();
    assertEquals(dacs.get(0).getName(), firstName);
    assertEquals(dacs.get(1).getName(), secondName);
    assertEquals(dacs.get(2).getName(), thirdName);
  }

  @Test
  void testFindAllWithDataset() {
    Integer dacId1 = createRandomDAC();
    Integer dacId2 = createRandomDAC();
    User user = createUser();
    Integer datasetId =
        datasetDAO.insertDataset(
            randomAlphabetic(20),
            new Timestamp(new Date().getTime()),
            user.getUserId(),
            randomAlphabetic(20),
            new DataUseBuilder().setGeneralUse(true).build().toString(),
            dacId1);
    List<DatasetProperty> list = new ArrayList<>();
    DatasetProperty dsp = new DatasetProperty();
    dsp.setDatasetId(datasetId);
    dsp.setPropertyKey(1);
    dsp.setPropertyValue("Test_PropertyValue");
    dsp.setCreateDate(new Date());
    list.add(dsp);
    datasetDAO.insertDatasetProperties(list);
    Integer datasetId2 =
        datasetDAO.insertDataset(
            randomAlphabetic(20),
            new Timestamp(new Date().getTime()),
            user.getUserId(),
            randomAlphabetic(20),
            new DataUseBuilder().setGeneralUse(true).build().toString(),
            dacId2);

    List<Dac> dacs = dacDAO.findAll();

    Dac dac1 = dacs.stream().filter(d -> d.getDacId().equals(dacId1)).findFirst().orElseThrow();
    List<Integer> datasetIds = dac1.getDatasetIds();
    assertThat(datasetIds, hasSize(1));
    assertThat(datasetIds, contains(datasetId));
    assertNull(dac1.getAssociatedDaa());

    Dac dac2 = dacs.stream().filter(d -> d.getDacId().equals(dacId2)).findFirst().orElseThrow();
    List<Integer> datasetIds2 = dac2.getDatasetIds();
    assertThat(datasetIds2, hasSize(1));
    assertThat(datasetIds2, contains(datasetId2));
    assertNull(dac2.getAssociatedDaa());
  }

  @Test
  void testFindAllWithDAAs() {
    User user = createUser();

    Integer dacId1 = createRandomDAC();
    Integer daaId1 =
        daaDAO.createDaa(user.getUserId(), Instant.now(), user.getUserId(), Instant.now(), dacId1);
    createFSO(user.getUserId(), daaId1);
    daaDAO.createDacDaaRelation(dacId1, daaId1, user.getUserId());

    Integer dacId2 = createRandomDAC();
    Integer daaId2 =
        daaDAO.createDaa(user.getUserId(), Instant.now(), user.getUserId(), Instant.now(), dacId2);
    createFSO(user.getUserId(), daaId2);
    daaDAO.createDacDaaRelation(dacId2, daaId2, user.getUserId());

    dacDAO
        .findAll()
        .forEach(
            dac -> {
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
    Integer daaId =
        daaDAO.createDaa(user.getUserId(), Instant.now(), user.getUserId(), Instant.now(), id);
    DataAccessAgreement daa = daaDAO.findById(daaId);
    daaDAO.createDacDaaRelation(id, daaId, user.getUserId());
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
  void testFindByIdAfterSoftDelete_returnsNull() {
    User user = createUser();
    Integer dacId = createRandomDAC();
    assertNotNull(dacDAO.findById(dacId));

    dacDAO.deleteDac(dacId, user.getUserId());

    assertNull(dacDAO.findById(dacId));
  }

  @Test
  void testFindDeletedDacById_returnsDeletedDac() {
    User user = createUser();
    Integer dacId = createRandomDAC();
    assertNotNull(dacDAO.findById(dacId));

    dacDAO.deleteDac(dacId, user.getUserId());

    // findById returns null for soft-deleted DACs
    assertNull(dacDAO.findById(dacId));

    // findDeletedDacById returns the soft-deleted row with delete metadata set
    Dac deleted = dacDAO.findDeletedDacById(dacId);
    assertNotNull(deleted);
    assertEquals(dacId, deleted.getDacId());
    assertTrue(deleted.getDeleted());
    assertEquals(user.getUserId(), deleted.getDeleteUserId());
    assertNotNull(deleted.getDeleteDate());
  }

  @Test
  void testFindDeletedDacById_returnsNullForActiveDAC() {
    Integer dacId = createRandomDAC();

    // An active (non-deleted) DAC should not be returned by findDeletedDacById
    assertNull(dacDAO.findDeletedDacById(dacId));
  }

  @Test
  void testFindDeletedDacById_returnsNullForNonExistentId() {
    assertNull(dacDAO.findDeletedDacById(Integer.MAX_VALUE));
  }

  @Test
  void testDeleteDac_removedFromFindAll() {
    User user = createUser();
    Integer dacId = createRandomDAC();
    assertTrue(dacDAO.findAll().stream().anyMatch(d -> d.getDacId().equals(dacId)));

    dacDAO.deleteDac(dacId, user.getUserId());

    assertTrue(dacDAO.findAll().stream().noneMatch(d -> d.getDacId().equals(dacId)));
  }

  @Test
  void testDeleteDac_createsDeleteAudit() {
    User user = createUser();
    Integer dacId = createRandomDAC();

    dacDAO.deleteDac(dacId, user.getUserId());

    List<DacAudit> audits = dacDAO.findAuditsByDacId(dacId);
    // Should have a CREATE audit (from createRandomDAC) and a DELETE audit
    assertTrue(audits.stream().anyMatch(a -> AuditActions.DELETE.equals(a.action())));
    DacAudit deleteAudit =
        audits.stream()
            .filter(a -> AuditActions.DELETE.equals(a.action()))
            .findFirst()
            .orElseThrow();
    assertEquals(dacId, deleteAudit.dacId());
    assertEquals(user.getUserId(), deleteAudit.userId());
    assertNull(deleteAudit.affectedUserId());
    assertNull(deleteAudit.roleId());
    assertNotNull(deleteAudit.actionDate());
  }

  @Test
  void testDeleteDac_idempotent_doesNotOverwriteOrAddAudit() {
    User firstDeleter = createUser();
    User secondDeleter = createUser();
    Integer dacId = createRandomDAC();

    dacDAO.deleteDac(dacId, firstDeleter.getUserId());
    dacDAO.deleteDac(dacId, secondDeleter.getUserId());

    // delete_user_id must still reflect the first caller
    Dac deleted = dacDAO.findDeletedDacById(dacId);
    assertNotNull(deleted);
    assertEquals(firstDeleter.getUserId(), deleted.getDeleteUserId());

    // exactly one DELETE audit entry — the second call was a no-op
    long deleteAuditCount =
        dacDAO.findAuditsByDacId(dacId).stream()
            .filter(a -> AuditActions.DELETE.equals(a.action()))
            .count();
    assertEquals(1, deleteAuditCount);
  }

  @Test
  void testUpdateDacWithoutEmail() {
    String newValue = "New Value";
    User user = createUser();
    Integer dacId = createRandomDAC();
    dacDAO.updateDac(newValue, newValue, dacId, user.getUserId());
    Dac updatedDac = dacDAO.findById(dacId);

    assertEquals(newValue, updatedDac.getName());
    assertEquals(newValue, updatedDac.getDescription());
  }

  @Test
  void testUpdateDacWithEmail() {
    String newValue = "New Value";
    String newEmail = "new_email@test.com";
    User user = createUser();
    Dac dac = insertDacWithEmail();
    dacDAO.updateDac(newValue, newValue, newEmail, dac.getDacId(), user.getUserId());
    Dac updatedDac = dacDAO.findById(dac.getDacId());

    assertEquals(newValue, updatedDac.getName());
    assertEquals(newValue, updatedDac.getDescription());
    assertEquals(newEmail, updatedDac.getEmail());
  }

  @Test
  void testFindMembersByDacId() {
    Dac dac = insertDacWithEmail();
    Integer chairRoleId = UserRoles.CHAIRPERSON.getRoleId();
    Integer memberRoleId = UserRoles.MEMBER.getRoleId();
    User user1 = createUser();
    dacDAO.addDacMember(memberRoleId, user1.getUserId(), dac.getDacId(), user1.getUserId());
    User user2 = createUser();
    dacDAO.addDacMember(memberRoleId, user2.getUserId(), dac.getDacId(), user2.getUserId());
    User user3 = createUser();
    dacDAO.addDacMember(memberRoleId, user3.getUserId(), dac.getDacId(), user3.getUserId());
    User user4 = createUser();
    dacDAO.addDacMember(chairRoleId, user4.getUserId(), dac.getDacId(), user4.getUserId());

    List<User> dacMembers = dacDAO.findMembersByDacId(dac.getDacId());
    assertThat(dacMembers, hasSize(4));
  }

  @Test
  void testFindMembersByDacIdAndRoleId() {
    Dac dac = insertDacWithEmail();
    Integer chairRoleId = UserRoles.CHAIRPERSON.getRoleId();
    Integer memberRoleId = UserRoles.MEMBER.getRoleId();
    User user1 = createUser();
    dacDAO.addDacMember(memberRoleId, user1.getUserId(), dac.getDacId(), user1.getUserId());
    User user2 = createUser();
    dacDAO.addDacMember(memberRoleId, user2.getUserId(), dac.getDacId(), user2.getUserId());
    User user3 = createUser();
    dacDAO.addDacMember(memberRoleId, user3.getUserId(), dac.getDacId(), user3.getUserId());
    User user4 = createUser();
    dacDAO.addDacMember(chairRoleId, user4.getUserId(), dac.getDacId(), user4.getUserId());

    List<User> chairs = dacDAO.findMembersByDacIdAndRoleId(dac.getDacId(), chairRoleId);
    assertThat(chairs, hasSize(1));

    List<User> members = dacDAO.findMembersByDacIdAndRoleId(dac.getDacId(), memberRoleId);
    assertThat(members, hasSize(3));
  }

  @Test
  void testAddDacMember() {
    Dac dac = insertDacWithEmail();
    Integer roleId = UserRoles.MEMBER.getRoleId();
    User user = createUser();
    dacDAO.addDacMember(roleId, user.getUserId(), dac.getDacId(), user.getUserId());
    List<UserRole> memberRoles = userDAO.findUserById(user.getUserId()).getRoles();
    assertFalse(memberRoles.isEmpty());
    UserRole userRole =
        memberRoles.stream().filter(r -> r.getRoleId().equals(roleId)).findFirst().orElseThrow();
    assertEquals(userRole.getDacId(), dac.getDacId());
  }

  @Test
  void testAddDacChair() {
    Dac dac = insertDacWithEmail();
    Integer roleId = UserRoles.CHAIRPERSON.getRoleId();
    User user = createUser();
    dacDAO.addDacMember(roleId, user.getUserId(), dac.getDacId(), user.getUserId());
    List<UserRole> chairRoles = userDAO.findUserById(user.getUserId()).getRoles();
    assertFalse(chairRoles.isEmpty());
    UserRole userRole =
        chairRoles.stream().filter(r -> r.getRoleId().equals(roleId)).findFirst().orElseThrow();
    assertEquals(userRole.getDacId(), dac.getDacId());
  }

  @Test
  void testRemoveDacMember() {
    Dac dac = insertDacWithEmail();
    Integer chairRoleId = UserRoles.CHAIRPERSON.getRoleId();
    Integer memberRoleId = UserRoles.MEMBER.getRoleId();
    User user1 = createUser();
    dacDAO.addDacMember(memberRoleId, user1.getUserId(), dac.getDacId(), user1.getUserId());
    User user2 = createUser();
    dacDAO.addDacMember(chairRoleId, user2.getUserId(), dac.getDacId(), user2.getUserId());
    List<UserRole> userRoles = userDAO.findUserById(user2.getUserId()).getRoles();
    userRoles.forEach(
        userRole -> dacDAO.removeDacMember(userRole.getUserRoleId(), user2.getUserId()));
    List<UserRole> userRolesRemoved = userDAO.findUserById(user2.getUserId()).getRoles();
    assertNull(userRolesRemoved);
  }

  @Test
  void testGetRoleById() {
    Role chair = dacDAO.getRoleById(UserRoles.CHAIRPERSON.getRoleId());
    assertEquals(chair.getName().toLowerCase(), UserRoles.CHAIRPERSON.getRoleName().toLowerCase());
    Role member = dacDAO.getRoleById(UserRoles.MEMBER.getRoleId());
    assertEquals(member.getName().toLowerCase(), UserRoles.MEMBER.getRoleName().toLowerCase());
  }

  @Test
  void testFindUserRolesForUser() {
    Dac dac = insertDacWithEmail();
    User chair = createUser(); // Creates a user with researcher role; UserRole #1
    dacDAO.addDacMember(
        UserRoles.CHAIRPERSON.getRoleId(),
        chair.getUserId(),
        dac.getDacId(),
        chair.getUserId()); // ; UserRole #2
    List<UserRole> userRoles = userDAO.findUserById(chair.getUserId()).getRoles();
    assertEquals(2, userRoles.size());
  }

  @Test
  void testFindUserRolesForUsers() {
    Dac dac = insertDacWithEmail();
    User chair = createUser(); // Creates a user with researcher role; UserRole #1
    User member = createUser(); // Creates a user with researcher role; UserRole #2
    dacDAO.addDacMember(
        UserRoles.CHAIRPERSON.getRoleId(),
        chair.getUserId(),
        dac.getDacId(),
        chair.getUserId()); // ; UserRole #3
    dacDAO.addDacMember(
        UserRoles.MEMBER.getRoleId(),
        member.getUserId(),
        dac.getDacId(),
        member.getUserId()); // ; UserRole #4
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
    datasetDAO.insertDatasetProperties(
        List.of(
            new DatasetProperty(
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
    assertThat(results, hasSize(1));
    assertTrue(results.stream().map(Dac::getDacId).toList().contains(dac.getDacId()));
  }

  @Test
  void testFindAuditsByDacId_empty() {
    List<DacAudit> audits = dacDAO.findAuditsByDacId(1);
    assertNotNull(audits);
    assertTrue(audits.isEmpty());
  }

  @Test
  void testFindAuditsByDacId_addMemberCreatesAudit() {
    Dac dac = insertDacWithEmail();
    User user = createUser();
    dacDAO.addDacMember(
        UserRoles.MEMBER.getRoleId(), user.getUserId(), dac.getDacId(), user.getUserId());

    List<DacAudit> audits = dacDAO.findAuditsByDacId(dac.getDacId());
    assertThat(audits, hasSize(2));

    DacAudit audit = audits.getFirst();
    assertEquals(dac.getDacId(), audit.dacId());
    assertEquals(user.getUserId(), audit.userId());
    assertEquals(user.getUserId(), audit.affectedUserId());
    assertEquals(UserRoles.MEMBER.getRoleId(), audit.roleId());
    assertEquals(AuditActions.ADD, audit.action());
    assertNotNull(audit.actionDate());
  }

  @Test
  void testFindAuditsByDacId_removeMemberCreatesAudit() {
    Dac dac = insertDacWithEmail();
    User user = createUser();
    dacDAO.addDacMember(
        UserRoles.MEMBER.getRoleId(), user.getUserId(), dac.getDacId(), user.getUserId());

    // Get the user_role_id that was just created
    List<UserRole> roles =
        dacDAO.findMembersByDacId(dac.getDacId()).stream()
            .flatMap(u -> u.getRoles().stream())
            .toList();
    assertFalse(roles.isEmpty());
    Integer userRoleId = roles.getFirst().getUserRoleId();

    User actor = createUser();
    dacDAO.removeDacMember(userRoleId, actor.getUserId());

    List<DacAudit> audits = dacDAO.findAuditsByDacId(dac.getDacId());
    // Should have 2 audits: ADD from addDacMember, REMOVE from removeDacMember
    assertThat(audits, hasSize(3));

    DacAudit removeAudit = audits.getFirst(); // newest first
    assertEquals(dac.getDacId(), removeAudit.dacId());
    assertEquals(actor.getUserId(), removeAudit.userId());
    assertEquals(user.getUserId(), removeAudit.affectedUserId());
    assertEquals(UserRoles.MEMBER.getRoleId(), removeAudit.roleId());
    assertEquals(AuditActions.REMOVE, removeAudit.action());
    assertNotNull(removeAudit.actionDate());
  }

  @Test
  void testFindAuditsByDacId_insertDacAuditDirectly() {
    Dac dac = insertDacWithEmail();

    List<DacAudit> audits = dacDAO.findAuditsByDacId(dac.getDacId());
    assertThat(audits, hasSize(1));

    DacAudit audit = audits.getFirst();
    assertEquals(dac.getDacId(), audit.dacId());
    assertNull(audit.affectedUserId());
    assertNull(audit.roleId());
    assertEquals(AuditActions.CREATE, audit.action());
    assertNotNull(audit.actionDate());
  }

  @Test
  void testFindAuditsByDacId_orderedNewestFirst() throws InterruptedException {
    Dac dac = insertDacWithEmail();
    User user2 = createUser();

    // Small sleep to ensure distinct action_date timestamps
    Thread.sleep(10); // NOSONAR
    jdbi.useHandle(
        h -> {
          String insert =
              """
              INSERT INTO dac_audit (dac_id, user_id, affected_user_id, role_id, action, action_date)
              VALUES (:dacId, :userId, NULL, NULL, :action, NOW())
              """;
          h.createUpdate(insert)
              .bind("dacId", dac.getDacId())
              .bind("userId", user2.getUserId())
              .bind("action", AuditActions.UPDATE.name())
              .execute();
        });

    List<DacAudit> audits = dacDAO.findAuditsByDacId(dac.getDacId());
    assertThat(audits, hasSize(2));

    // Newest (UPDATE) should be first
    assertEquals(AuditActions.UPDATE, audits.get(0).action());
    assertEquals(AuditActions.CREATE, audits.get(1).action());
    assertTrue(
        audits.get(0).actionDate().isAfter(audits.get(1).actionDate())
            || audits.get(0).actionDate().equals(audits.get(1).actionDate()));
  }

  @Test
  void testFindAuditsByDacId_isolatedToDac() {
    Dac dac1 = insertDacWithEmail();
    Dac dac2 = insertDacWithEmail();

    List<DacAudit> audits1 = dacDAO.findAuditsByDacId(dac1.getDacId());
    List<DacAudit> audits2 = dacDAO.findAuditsByDacId(dac2.getDacId());

    assertThat(audits1, hasSize(1));
    assertThat(audits2, hasSize(1));
    assertEquals(dac1.getDacId(), audits1.getFirst().dacId());
    assertEquals(dac2.getDacId(), audits2.getFirst().dacId());
  }

  private Dac insertDacWithEmail() {
    String testEmail = "test@email.com";
    User user = createUser();
    Integer id =
        dacDAO.createDac(
            "Test_" + randomAlphabetic(20),
            "Test_" + randomAlphabetic(20),
            testEmail,
            user.getUserId());
    return dacDAO.findById(id);
  }

  private Dac createDac() {
    Integer id = createRandomDAC();
    return dacDAO.findById(id);
  }

  private DarCollection createDarCollection() {
    User user = createUserWithInstitution();
    String darCode = "DAR-" + randomInt(1, 10000);
    Integer collectionId =
        darCollectionDAO.insertDarCollection(darCode, user.getUserId(), new Date());
    Dataset dataset = createDataset();
    DataAccessRequest dar = createDataAccessRequest(user.getUserId(), collectionId);
    dataAccessRequestDAO.insertDARDatasetRelation(dar.getReferenceId(), dataset.getDatasetId());
    createDataAccessRequest(user.getUserId(), collectionId);
    return darCollectionDAO.findDARCollectionByCollectionId(collectionId);
  }

  private DataAccessRequest createDataAccessRequest(Integer userId, Integer collectionId) {
    DataAccessRequestData data = new DataAccessRequestData();
    data.setProjectTitle("Project Title: " + randomAlphabetic(50));
    data.setHmb(true);
    data.setMethods(false);
    String referenceId = UUID.randomUUID().toString();
    Date now = new Date();
    dataAccessRequestDAO.insertDataAccessRequest(
        collectionId, referenceId, userId, now, now, now, data, randomAlphabetic(10));
    return dataAccessRequestDAO.findByReferenceId(referenceId);
  }

  private void createDataAccessRequestInCollectionWithDataset(DarCollection collection, Dataset d) {
    User user = createUser();
    String randomUUID = UUID.randomUUID().toString();
    dataAccessRequestDAO.insertDataAccessRequest(
        collection.getDarCollectionId(),
        randomUUID,
        user.getUserId(),
        new Date(),
        new Date(),
        new Date(),
        new DataAccessRequestData(),
        user.getEraCommonsId());
    dataAccessRequestDAO.insertDARDatasetRelation(randomUUID, d.getDatasetId());
  }

  private Dataset createDataset() {
    User user = createUser();
    String name = "Name_" + randomAlphabetic(20);
    Timestamp now = new Timestamp(new Date().getTime());
    String objectId = "Object ID_" + randomAlphabetic(20);
    DataUse dataUse = new DataUseBuilder().setGeneralUse(true).build();
    Integer id =
        datasetDAO.insertDataset(name, now, user.getUserId(), objectId, dataUse.toString(), null);
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
    Integer id =
        datasetDAO.insertDataset(name, now, user.getUserId(), objectId, dataUse.toString(), dacId);
    createDatasetProperties(id);
    return datasetDAO.findDatasetById(id);
  }

  private Integer createRandomDACWithName(String name) {
    User user = createUser();
    return dacDAO.createDac(name, "Test_" + randomAlphabetic(20), user.getUserId());
  }

  private Integer createRandomDAC() {
    return createRandomDACWithName("Test_" + randomAlphabetic(20));
  }

  private void createFSO(Integer userId, Integer daaId) {
    fileStorageObjectDAO.insertNewFile(
        randomAlphabetic(10),
        FileCategory.DATA_ACCESS_AGREEMENT.getValue(),
        randomAlphabetic(10),
        MediaType.TEXT_PLAIN_TYPE.getType(),
        daaId.toString(),
        userId,
        Instant.now());
  }
}
