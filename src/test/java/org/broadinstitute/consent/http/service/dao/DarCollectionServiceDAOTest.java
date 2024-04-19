package org.broadinstitute.consent.http.service.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.db.DAOTestHelper;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DarCollection;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetProperty;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.models.Vote;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DarCollectionServiceDAOTest extends DAOTestHelper {

  private DarCollectionServiceDAO serviceDAO;

  @BeforeEach
  public void initService() {
    serviceDAO = new DarCollectionServiceDAO(datasetDAO, electionDAO, jdbi, userDAO);
  }

  /**
   * This test covers the case where: - User is an admin - Collection has 1 DAR/Dataset combinations
   * - Elections created should be for the DAR/Dataset for the user
   */
  @Test
  void testCreateElectionsForDarCollectionAdmin() throws Exception {
    User user = new User();
    user.addRole(new UserRole(UserRoles.ADMIN.getRoleId(), UserRoles.ADMIN.getRoleName()));
    DarCollection collection = setUpDarCollectionWithDacDataset();
    DataAccessRequest dar = collection.getDars().values().stream().findFirst().orElse(null);
    assertNotNull(dar);

    List<String> referenceIds = serviceDAO.createElectionsForDarCollection(user, collection);

    List<Election> createdElections =
        electionDAO.findLastElectionsByReferenceIds(List.of(dar.getReferenceId()));
    List<Vote> createdVotes =
        voteDAO.findVotesByElectionIds(
            createdElections.stream().map(Election::getElectionId).collect(Collectors.toList()));

    assertTrue(referenceIds.contains(dar.getReferenceId()));
    assertFalse(createdElections.isEmpty());
    assertFalse(createdVotes.isEmpty());

    // Ensure that we have all primary vote types for each election type
    // Data Access Elections have Chair, Dac, Final, and Agreement votes
    Optional<Election> daElectionOption = createdElections.stream()
        .filter(e -> ElectionType.DATA_ACCESS.getValue().equals(e.getElectionType())).findFirst();
    assertTrue(daElectionOption.isPresent());
    assertTrue(createdVotes
        .stream()
        .filter(v -> v.getElectionId().equals(daElectionOption.get().getElectionId()))
        .anyMatch(v -> v.getType().equals(VoteType.CHAIRPERSON.getValue())));
    assertTrue(createdVotes
        .stream()
        .filter(v -> v.getElectionId().equals(daElectionOption.get().getElectionId()))
        .anyMatch(v -> v.getType().equals(VoteType.DAC.getValue())));
    assertTrue(createdVotes
        .stream()
        .filter(v -> v.getElectionId().equals(daElectionOption.get().getElectionId()))
        .anyMatch(v -> v.getType().equals(VoteType.FINAL.getValue())));
    assertTrue(createdVotes
        .stream()
        .filter(v -> v.getElectionId().equals(daElectionOption.get().getElectionId()))
        .anyMatch(v -> v.getType().equals(VoteType.AGREEMENT.getValue())));

    // RP Elections have Chair and Dac votes
    Optional<Election> rpElectionOption = createdElections.stream()
        .filter(e -> ElectionType.RP.getValue().equals(e.getElectionType())).findFirst();
    assertTrue(rpElectionOption.isPresent());
    assertTrue(createdVotes
        .stream()
        .filter(v -> v.getElectionId().equals(rpElectionOption.get().getElectionId()))
        .anyMatch(v -> v.getType().equals(VoteType.CHAIRPERSON.getValue())));
    assertTrue(createdVotes
        .stream()
        .filter(v -> v.getElectionId().equals(rpElectionOption.get().getElectionId()))
        .anyMatch(v -> v.getType().equals(VoteType.DAC.getValue())));
  }

  /**
   * This test covers the case where: - User is an admin - Collection has 2 DAR/Dataset combinations
   * - User is an Admin - Elections created should only be for ALL the DAR/Dataset combinations
   */
  @Test
  void testCreateElectionsForDarCollectionWithMultipleDatasetsForAdmin() throws Exception {
    User user = new User();
    user.addRole(new UserRole(UserRoles.ADMIN.getRoleId(), UserRoles.ADMIN.getRoleName()));
    DarCollection collection = setUpDarCollectionWithDacDataset();
    DataAccessRequest dar = collection.getDars().values().stream().findFirst().orElse(null);
    assertNotNull(dar);

    // Add another DAR with Dataset in a separate DAC to the collection
    DataAccessRequest dar2 = addDARWithDacAndDatasetToCollection(collection);
    // refresh the collection
    collection = darCollectionDAO.findDARCollectionByCollectionId(collection.getDarCollectionId());

    List<String> referenceIds = serviceDAO.createElectionsForDarCollection(user, collection);

    List<Election> createdElections =
        electionDAO.findLastElectionsByReferenceIds(List.of(dar.getReferenceId()));
    List<Vote> createdVotes =
        voteDAO.findVotesByElectionIds(
            createdElections.stream().map(Election::getElectionId).collect(Collectors.toList()));

    assertTrue(referenceIds.contains(dar.getReferenceId()));
    assertTrue(referenceIds.contains(dar2.getReferenceId()));
    // Ensure that we have an access and rp election
    assertFalse(createdElections.isEmpty());
    assertTrue(
        createdElections.stream()
            .anyMatch(e -> e.getElectionType().equals(ElectionType.DATA_ACCESS.getValue())));
    assertTrue(
        createdElections.stream()
            .anyMatch(e -> e.getElectionType().equals(ElectionType.RP.getValue())));
    // Ensure that we have primary vote types
    assertFalse(createdVotes.isEmpty());
    assertTrue(
        createdVotes.stream().anyMatch(v -> v.getType().equals(VoteType.CHAIRPERSON.getValue())));
    assertTrue(
        createdVotes.stream().anyMatch(v -> v.getType().equals(VoteType.FINAL.getValue())));
    assertTrue(
        createdVotes.stream().anyMatch(v -> v.getType().equals(VoteType.DAC.getValue())));
    assertTrue(
        createdVotes.stream().anyMatch(v -> v.getType().equals(VoteType.AGREEMENT.getValue())));

    // Make sure that we DID create elections for the additional DAR/Dataset combination
    List<Election> additionalCreatedElections =
        electionDAO.findLastElectionsByReferenceIds(List.of(dar2.getReferenceId()));
    assertFalse(additionalCreatedElections.isEmpty());
  }

  /**
   * This test covers the case where: - User is a chairperson - Collection has 1 DAR/Dataset
   * combinations - Elections created should be for the DAR/Dataset for the user
   */
  @Test
  void testCreateElectionsForDarCollectionChair() throws Exception {
    DarCollection collection = setUpDarCollectionWithDacDataset();
    Optional<DataAccessRequest> dar = collection.getDars().values().stream().findFirst();
    assertTrue(dar.isPresent());
    Integer datasetId = dar.get().getDatasetIds().get(0);
    assertNotNull(datasetId);
    Optional<Dac> dac = dacDAO.findDacsForDatasetIds(List.of(datasetId)).stream().findFirst();
    assertTrue(dac.isPresent());
    List<User> dacUsers = dacDAO.findMembersByDacId(dac.get().getDacId());
    Optional<User> chair = dacUsers.stream().filter(u -> u.hasUserRole(UserRoles.CHAIRPERSON))
        .findFirst();
    assertTrue(chair.isPresent());

    List<String> referenceIds = serviceDAO.createElectionsForDarCollection(chair.get(), collection);

    List<Election> createdElections =
        electionDAO.findLastElectionsByReferenceIds(List.of(dar.get().getReferenceId()));
    List<Vote> createdVotes =
        voteDAO.findVotesByElectionIds(
            createdElections.stream().map(Election::getElectionId).collect(Collectors.toList()));

    assertTrue(referenceIds.contains(dar.get().getReferenceId()));
    // Ensure that we have an access and rp election
    assertFalse(createdElections.isEmpty());
    assertTrue(
        createdElections.stream()
            .anyMatch(e -> e.getElectionType().equals(ElectionType.DATA_ACCESS.getValue())));
    assertTrue(
        createdElections.stream()
            .anyMatch(e -> e.getElectionType().equals(ElectionType.RP.getValue())));
    // Ensure that we have primary vote types
    assertFalse(createdVotes.isEmpty());
    assertTrue(
        createdVotes.stream().anyMatch(v -> v.getType().equals(VoteType.CHAIRPERSON.getValue())));
    assertTrue(
        createdVotes.stream().anyMatch(v -> v.getType().equals(VoteType.FINAL.getValue())));
    assertTrue(
        createdVotes.stream().anyMatch(v -> v.getType().equals(VoteType.DAC.getValue())));
    assertTrue(
        createdVotes.stream().anyMatch(v -> v.getType().equals(VoteType.AGREEMENT.getValue())));
  }

  /**
   * This test covers the case where: - User is a chairperson - Collection has 2 DAR/Dataset
   * combinations - User is a DAC chair for only one of the DAR/Dataset combinations - Elections
   * created should only be for the DAR/Dataset for the user
   */
  @Test
  void testCreateElectionsForDarCollectionWithMultipleDatasetsForChair() throws Exception {
    // Start off with a collection and a single DAR
    DarCollection collection = setUpDarCollectionWithDacDataset();
    Optional<DataAccessRequest> dar = collection.getDars().values().stream().findFirst();
    assertTrue(dar.isPresent());
    assertFalse(dar.get().getDatasetIds().isEmpty());
    Integer datasetId = dar.get().getDatasetIds().get(0);
    assertNotNull(datasetId);

    // Find the dac chairperson for the current DAR/Dataset combination
    Optional<Dac> dac = dacDAO.findDacsForDatasetIds(List.of(datasetId)).stream().findFirst();
    assertTrue(dac.isPresent());
    List<User> dacUsers = dacDAO.findMembersByDacId(dac.get().getDacId());
    Optional<User> chair = dacUsers.stream().filter(u -> u.hasUserRole(UserRoles.CHAIRPERSON))
        .findFirst();
    assertTrue(chair.isPresent());

    // Add another DAR with Dataset to collection.
    // This one should not be available to the chairperson created above.
    DataAccessRequest dar2 = addDARWithDacAndDatasetToCollection(collection);
    // refresh the collection
    collection = darCollectionDAO.findDARCollectionByCollectionId(collection.getDarCollectionId());

    List<String> referenceIds = serviceDAO.createElectionsForDarCollection(chair.get(), collection);

    List<Election> createdElections =
        electionDAO.findLastElectionsByReferenceIds(List.of(dar.get().getReferenceId()));
    List<Vote> createdVotes =
        voteDAO.findVotesByElectionIds(
            createdElections.stream().map(Election::getElectionId).collect(Collectors.toList()));

    assertTrue(referenceIds.contains(dar.get().getReferenceId()));
    assertFalse(referenceIds.contains(dar2.getReferenceId()));

    // Ensure that we have an access and rp election
    assertFalse(createdElections.isEmpty());
    assertTrue(
        createdElections.stream()
            .anyMatch(e -> e.getElectionType().equals(ElectionType.DATA_ACCESS.getValue())));
    assertTrue(
        createdElections.stream()
            .anyMatch(e -> e.getElectionType().equals(ElectionType.RP.getValue())));
    // Ensure that we have primary vote types
    assertFalse(createdVotes.isEmpty());
    assertTrue(
        createdVotes.stream().anyMatch(v -> v.getType().equals(VoteType.CHAIRPERSON.getValue())));
    assertTrue(
        createdVotes.stream().anyMatch(v -> v.getType().equals(VoteType.FINAL.getValue())));
    assertTrue(
        createdVotes.stream().anyMatch(v -> v.getType().equals(VoteType.DAC.getValue())));
    assertTrue(
        createdVotes.stream().anyMatch(v -> v.getType().equals(VoteType.AGREEMENT.getValue())));

    // Make sure we did not create elections for the DAR/Dataset that the chair does not have access to.
    List<Election> nonCreatedElections =
        electionDAO.findLastElectionsByReferenceIds(List.of(dar2.getReferenceId()));
    assertTrue(nonCreatedElections.isEmpty());
  }

  /**
   * This test covers the case where: - User is an admin - Elections have been created for a
   * Collection - Elections are then canceled - Elections re-created correctly - Previous canceled
   * elections are correctly archived
   */
  @Test
  void testCreateElectionsForDarCollectionAfterCancelingEarlierElectionsAsAdmin()
      throws Exception {
    User user = new User();
    user.addRole(new UserRole(UserRoles.ADMIN.getRoleId(), UserRoles.ADMIN.getRoleName()));
    DarCollection collection = setUpDarCollectionWithDacDataset();
    DataAccessRequest dar = collection.getDars().values().stream().findFirst().orElse(null);
    assertNotNull(dar);

    // create elections & votes:
    List<String> referenceIds = serviceDAO.createElectionsForDarCollection(user, collection);

    // cancel those elections:
    List<Integer> canceledElectionIds = electionDAO.findLastElectionsByReferenceIds(
            List.of(dar.getReferenceId()))
        .stream()
        .map(Election::getElectionId)
        .toList();
    canceledElectionIds.forEach(id ->
        electionDAO.updateElectionById(id, ElectionStatus.CANCELED.getValue(), new Date()));

    // re-create elections & new votes:
    referenceIds.addAll(serviceDAO.createElectionsForDarCollection(user, collection));

    List<Election> createdElections =
        electionDAO.findLastElectionsByReferenceIds(List.of(dar.getReferenceId()));

    assertTrue(referenceIds.contains(dar.getReferenceId()));

    // Ensure that we have the right number of access and rp elections, i.e. 1 each
    assertFalse(createdElections.isEmpty());
    assertEquals(2, createdElections.size());
    assertEquals(1, createdElections.stream().filter(e -> e.getElectionType().equals(
        ElectionType.DATA_ACCESS.getValue())).count());
    assertEquals(1, createdElections.stream().filter(e -> e.getElectionType().equals(
        ElectionType.RP.getValue())).count());

    // Check that the canceled elections are archived
    List<Election> canceledElections = electionDAO.findElectionsByIds(canceledElectionIds);
    canceledElections.forEach(e -> assertTrue(e.getArchived()));
  }

  /**
   * This test covers the case where: - User is a chair - Collection has 2 DAR/Dataset combinations
   * - Elections have been created for a Collection - User is a DAC chair for only one of the
   * DAR/Dataset combinations - All elections are canceled - Chair specific elections are re-created
   * correctly - Elections created should only be for the DAR/Dataset for the user
   */
  @Test
  void testCreateElectionsForDarCollectionAfterCancelingEarlierElectionsAsChair()
      throws Exception {
    // Start off with a collection and a single DAR
    DarCollection collection = setUpDarCollectionWithDacDataset();
    Optional<DataAccessRequest> dar = collection.getDars().values().stream().findFirst();
    assertTrue(dar.isPresent());
    assertFalse(dar.get().getDatasetIds().isEmpty());
    Integer datasetId = dar.get().getDatasetIds().get(0);
    assertNotNull(datasetId);

    // Find the dac chairperson for the current DAR/Dataset combination
    Optional<Dac> dac = dacDAO.findDacsForDatasetIds(List.of(datasetId)).stream().findFirst();
    assertTrue(dac.isPresent());
    List<User> dacUsers = dacDAO.findMembersByDacId(dac.get().getDacId());
    Optional<User> chair = dacUsers.stream().filter(u -> u.hasUserRole(UserRoles.CHAIRPERSON))
        .findFirst();
    assertTrue(chair.isPresent());

    // Add another DAR with Dataset to collection.
    // This one should not be available to the chairperson created above.
    DataAccessRequest dar2 = addDARWithDacAndDatasetToCollection(collection);

    // refresh the collection
    collection = darCollectionDAO.findDARCollectionByCollectionId(collection.getDarCollectionId());

    // create elections & votes:
    List<String> referenceIds = serviceDAO.createElectionsForDarCollection(chair.get(), collection);

    // cancel elections for all DARs in the collection:
    collection.getDars().values().forEach(d ->
        electionDAO.findLastElectionsByReferenceIds(List.of(d.getReferenceId())).forEach(e ->
            electionDAO.updateElectionById(e.getElectionId(), ElectionStatus.CANCELED.getValue(),
                new Date()))
    );

    // re-create elections & new votes:
    referenceIds.addAll(serviceDAO.createElectionsForDarCollection(chair.get(), collection));

    List<Election> createdElections =
        electionDAO.findLastElectionsByReferenceIds(List.of(dar.get().getReferenceId()));

    // Ensure that we have the right number of access and rp elections, i.e. 1 each
    assertFalse(createdElections.isEmpty());
    assertEquals(2, createdElections.size());
    assertEquals(1, createdElections.stream().filter(e -> e.getElectionType().equals(
        ElectionType.DATA_ACCESS.getValue())).count());
    assertEquals(1, createdElections.stream().filter(e -> e.getElectionType().equals(
        ElectionType.RP.getValue())).count());

    // Make sure we did not create elections for the DAR/Dataset that the chair does not have access to.
    List<Election> nonCreatedElections =
        electionDAO.findLastElectionsByReferenceIds(List.of(dar2.getReferenceId()));
    assertTrue(nonCreatedElections.isEmpty());
  }

  /**
   * Helper method to generate a DarCollection with a Dac, a Dataset, and a create User
   */
  private DarCollection setUpDarCollectionWithDacDataset() {
    User user = createUser();
    String darCode = "DAR-" + RandomUtils.nextInt(100, 1000);
    Dac dac = createDac();
    createUserWithRoleInDac(UserRoles.CHAIRPERSON.getRoleId(), dac.getDacId());
    createUserWithRoleInDac(UserRoles.MEMBER.getRoleId(), dac.getDacId());
    Dataset dataset = createDatasetWithDac(dac.getDacId());
    Integer collectionId = darCollectionDAO.insertDarCollection(darCode, user.getUserId(),
        new Date());
    createDarForCollection(user, collectionId, dataset);
    DarCollection collection = darCollectionDAO.findDARCollectionByCollectionId(collectionId);
    DataAccessRequest dar = collection.getDars().values().stream().findFirst().orElse(null);
    assertNotNull(dar);
    assertNotNull(dar.getData());
    dataAccessRequestDAO.insertDARDatasetRelation(dar.getReferenceId(), dataset.getDataSetId());
    Date now = new Date();
    dataAccessRequestDAO.updateDataByReferenceId(
        dar.getReferenceId(), dar.getUserId(), now, now, now, dar.getData());
    return darCollectionDAO.findDARCollectionByReferenceId(dar.getReferenceId());
  }

  /**
   * Helper method to add a DAR to a collection. This creates a new DAC/Dataset/Chair/Member to
   * facilitate the creation.
   */
  private DataAccessRequest addDARWithDacAndDatasetToCollection(DarCollection collection) {
    // Create new DAC and Dataset:
    Dac dac = createDac();
    createUserWithRoleInDac(UserRoles.CHAIRPERSON.getRoleId(), dac.getDacId());
    createUserWithRoleInDac(UserRoles.MEMBER.getRoleId(), dac.getDacId());
    Dataset dataset = createDataset();

    // Create new DAR with Dataset and add it to the collection
    User user = createUser();
    return createDarForCollection(user, collection.getDarCollectionId(), dataset);
  }

  private Dac createDac() {
    Integer id = dacDAO.createDac(
        "Test_" + RandomStringUtils.random(20, true, true),
        "Test_" + RandomStringUtils.random(20, true, true),
        new Date());
    return dacDAO.findById(id);
  }

  private Dataset createDataset() {
    User user = createUser();
    String name = "Name_" + RandomStringUtils.random(20, true, true);
    Timestamp now = new Timestamp(new Date().getTime());
    String objectId = "Object ID_" + RandomStringUtils.random(20, true, true);
    DataUse dataUse = new DataUseBuilder().setGeneralUse(true).build();
    Integer id = datasetDAO.insertDataset(name, now, user.getUserId(), objectId,
        dataUse.toString(), null);
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

  private DataAccessRequest createDarForCollection(User user, Integer collectionId,
      Dataset dataset) {
    Date now = new Date();
    DataAccessRequest dar = new DataAccessRequest();
    dar.setReferenceId(UUID.randomUUID().toString());
    DataAccessRequestData data = new DataAccessRequestData();
    dar.setData(data);
    dataAccessRequestDAO.insertDraftDataAccessRequest(dar.getReferenceId(), user.getUserId(), now,
        now, now, now, data);
    dataAccessRequestDAO.updateDraftForCollection(collectionId, dar.getReferenceId());
    dataAccessRequestDAO.updateDraftByReferenceId(dar.getReferenceId(), false);
    dataAccessRequestDAO.insertDARDatasetRelation(dar.getReferenceId(), dataset.getDataSetId());
    return dataAccessRequestDAO.findByReferenceId(dar.getReferenceId());
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

  private User createUserWithRoleInDac(Integer roleId, Integer dacId) {
    User user = createUserWithRole(roleId);
    dacDAO.addDacMember(roleId, user.getUserId(), dacId);
    return user;
  }

  private User createUserWithRole(Integer roleId) {
    int i1 = RandomUtils.nextInt(5, 10);
    int i2 = RandomUtils.nextInt(5, 10);
    int i3 = RandomUtils.nextInt(3, 5);
    String email = RandomStringUtils.randomAlphabetic(i1) +
        "@" +
        RandomStringUtils.randomAlphabetic(i2) +
        "." +
        RandomStringUtils.randomAlphabetic(i3);
    Integer userId = userDAO.insertUser(email, "display name", new Date());
    userRoleDAO.insertSingleUserRole(roleId, userId);
    return userDAO.findUserById(userId);
  }

}
