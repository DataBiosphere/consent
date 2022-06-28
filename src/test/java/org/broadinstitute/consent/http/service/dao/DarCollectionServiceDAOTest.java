package org.broadinstitute.consent.http.service.dao;

import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.db.DAOTestHelper;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DarCollection;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.models.Vote;
import org.junit.Test;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class DarCollectionServiceDAOTest extends DAOTestHelper {

  private DarCollectionServiceDAO serviceDAO;

  private void initService() {
    serviceDAO = new DarCollectionServiceDAO(datasetDAO, electionDAO, jdbi, userDAO, dataAccessRequestDAO);
  }

  /**
   * This test covers the case where:
   *  - User is an admin
   *  - Collection has 1 DAR/Dataset combinations
   *  - Elections created should be for the DAR/Dataset for the user
   */
  @Test
  public void testCreateElectionsForDarCollectionAdmin() throws Exception {
    initService();

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
    Optional<Election> daElectionOption = createdElections.stream().filter(e -> ElectionType.DATA_ACCESS.getValue().equals(e.getElectionType())).findFirst();
    assertTrue(daElectionOption.isPresent());
    assertTrue(createdVotes
      .stream()
      .filter(v -> v.getElectionId().equals(daElectionOption.get().getElectionId()))
      .anyMatch(v -> v.getType().equals(VoteType.CHAIRPERSON.getValue()))
    );
    assertTrue(createdVotes
      .stream()
      .filter(v -> v.getElectionId().equals(daElectionOption.get().getElectionId()))
      .anyMatch(v -> v.getType().equals(VoteType.DAC.getValue()))
    );
    assertTrue(createdVotes
      .stream()
      .filter(v -> v.getElectionId().equals(daElectionOption.get().getElectionId()))
      .anyMatch(v -> v.getType().equals(VoteType.FINAL.getValue()))
    );
    assertTrue(createdVotes
      .stream()
      .filter(v -> v.getElectionId().equals(daElectionOption.get().getElectionId()))
      .anyMatch(v -> v.getType().equals(VoteType.AGREEMENT.getValue()))
    );

    // RP Elections have Chair and Dac votes
    Optional<Election> rpElectionOption = createdElections.stream().filter(e -> ElectionType.RP.getValue().equals(e.getElectionType())).findFirst();
    assertTrue(rpElectionOption.isPresent());
    assertTrue(createdVotes
      .stream()
      .filter(v -> v.getElectionId().equals(rpElectionOption.get().getElectionId()))
      .anyMatch(v -> v.getType().equals(VoteType.CHAIRPERSON.getValue()))
    );
    assertTrue(createdVotes
      .stream()
      .filter(v -> v.getElectionId().equals(rpElectionOption.get().getElectionId()))
      .anyMatch(v -> v.getType().equals(VoteType.DAC.getValue()))
    );
  }

  /**
   * This test covers the case where:
   *  - User is an admin
   *  - Collection has 2 DAR/Dataset combinations
   *  - User is an Admin
   *  - Elections created should only be for ALL the DAR/Dataset combinations
   */
  @Test
  public void testCreateElectionsForDarCollectionWithMultipleDatasetsForAdmin() throws Exception {
    initService();

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
    assertTrue(createdElections.stream().anyMatch(e -> e.getElectionType().equals(ElectionType.DATA_ACCESS.getValue())));
    assertTrue(createdElections.stream().anyMatch(e -> e.getElectionType().equals(ElectionType.RP.getValue())));
    // Ensure that we have primary vote types
    assertFalse(createdVotes.isEmpty());
    assertTrue(createdVotes.stream().anyMatch(v -> v.getType().equals(VoteType.CHAIRPERSON.getValue())));
    assertTrue(createdVotes.stream().anyMatch(v -> v.getType().equals(VoteType.FINAL.getValue())));
    assertTrue(createdVotes.stream().anyMatch(v -> v.getType().equals(VoteType.DAC.getValue())));
    assertTrue(createdVotes.stream().anyMatch(v -> v.getType().equals(VoteType.AGREEMENT.getValue())));

    // Make sure that we DID create elections for the additional DAR/Dataset combination
    List<Election> additionalCreatedElections =
        electionDAO.findLastElectionsByReferenceIds(List.of(dar2.getReferenceId()));
    assertFalse(additionalCreatedElections.isEmpty());
  }

  /**
   * This test covers the case where:
   *  - User is a chairperson
   *  - Collection has 1 DAR/Dataset combinations
   *  - Elections created should be for the DAR/Dataset for the user
   */
  @Test
  public void testCreateElectionsForDarCollectionChair() throws Exception {
    initService();

    DarCollection collection = setUpDarCollectionWithDacDataset();
    Optional<DataAccessRequest> dar = collection.getDars().values().stream().findFirst();
    assertTrue(dar.isPresent());
    Integer datasetId = dataAccessRequestDAO.findDARDatasetRelations(dar.get().getReferenceId()).get(0);
    assertNotNull(datasetId);
    Optional<Dac> dac = dacDAO.findDacsForDatasetIds(List.of(datasetId)).stream().findFirst();
    assertTrue(dac.isPresent());
    List<User> dacUsers = dacDAO.findMembersByDacId(dac.get().getDacId());
    Optional<User> chair = dacUsers.stream().filter(u -> u.hasUserRole(UserRoles.CHAIRPERSON)).findFirst();
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
    assertTrue(createdElections.stream().anyMatch(e -> e.getElectionType().equals(ElectionType.DATA_ACCESS.getValue())));
    assertTrue(createdElections.stream().anyMatch(e -> e.getElectionType().equals(ElectionType.RP.getValue())));
    // Ensure that we have primary vote types
    assertFalse(createdVotes.isEmpty());
    assertTrue(createdVotes.stream().anyMatch(v -> v.getType().equals(VoteType.CHAIRPERSON.getValue())));
    assertTrue(createdVotes.stream().anyMatch(v -> v.getType().equals(VoteType.FINAL.getValue())));
    assertTrue(createdVotes.stream().anyMatch(v -> v.getType().equals(VoteType.DAC.getValue())));
    assertTrue(createdVotes.stream().anyMatch(v -> v.getType().equals(VoteType.AGREEMENT.getValue())));
  }

  /**
   * This test covers the case where:
   *  - User is a chairperson
   *  - Collection has 2 DAR/Dataset combinations
   *  - User is a DAC chair for only one of the DAR/Dataset combinations
   *  - Elections created should only be for the DAR/Dataset for the user
   */
  @Test
  public void testCreateElectionsForDarCollectionWithMultipleDatasetsForChair() throws Exception {
    initService();

    // Start off with a collection and a single DAR
    DarCollection collection = setUpDarCollectionWithDacDataset();
    Optional<DataAccessRequest> dar = collection.getDars().values().stream().findFirst();
    assertTrue(dar.isPresent());
    List<Integer> datasetIds = dataAccessRequestDAO.findDARDatasetRelations(dar.get().getReferenceId());
    assertFalse(datasetIds.isEmpty());
    Integer datasetId = datasetIds.get(0);
    assertNotNull(datasetId);

    // Find the dac chairperson for the current DAR/Dataset combination
    Optional<Dac> dac = dacDAO.findDacsForDatasetIds(List.of(datasetId)).stream().findFirst();
    assertTrue(dac.isPresent());
    List<User> dacUsers = dacDAO.findMembersByDacId(dac.get().getDacId());
    Optional<User> chair = dacUsers.stream().filter(u -> u.hasUserRole(UserRoles.CHAIRPERSON)).findFirst();
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
    assertTrue(createdElections.stream().anyMatch(e -> e.getElectionType().equals(ElectionType.DATA_ACCESS.getValue())));
    assertTrue(createdElections.stream().anyMatch(e -> e.getElectionType().equals(ElectionType.RP.getValue())));
    // Ensure that we have primary vote types
    assertFalse(createdVotes.isEmpty());
    assertTrue(createdVotes.stream().anyMatch(v -> v.getType().equals(VoteType.CHAIRPERSON.getValue())));
    assertTrue(createdVotes.stream().anyMatch(v -> v.getType().equals(VoteType.FINAL.getValue())));
    assertTrue(createdVotes.stream().anyMatch(v -> v.getType().equals(VoteType.DAC.getValue())));
    assertTrue(createdVotes.stream().anyMatch(v -> v.getType().equals(VoteType.AGREEMENT.getValue())));

    // Make sure we did not create elections for the DAR/Dataset that the chair does not have access to.
    List<Election> nonCreatedElections =
        electionDAO.findLastElectionsByReferenceIds(List.of(dar2.getReferenceId()));
    assertTrue(nonCreatedElections.isEmpty());
  }

  /**
   * This test covers the case where:
   *  - User is an admin
   *  - Elections have been created for a Collection
   *  - Elections are then canceled
   *  - Elections re-created correctly
   */
  @Test
  public void testCreateElectionsForDarCollectionAfterCancelingEarlierElectionsAsAdmin() throws Exception {
    initService();

    User user = new User();
    user.addRole(new UserRole(UserRoles.ADMIN.getRoleId(), UserRoles.ADMIN.getRoleName()));
    DarCollection collection = setUpDarCollectionWithDacDataset();
    DataAccessRequest dar = collection.getDars().values().stream().findFirst().orElse(null);
    assertNotNull(dar);

    // create elections & votes:
    List<String> referenceIds = serviceDAO.createElectionsForDarCollection(user, collection);

    // cancel those elections:
    electionDAO.findLastElectionsByReferenceIds(List.of(dar.getReferenceId())).forEach(e ->
        electionDAO.updateElectionById(e.getElectionId(), ElectionStatus.CANCELED.getValue(), new Date()));

    // re-create elections & new votes:
    referenceIds.addAll(serviceDAO.createElectionsForDarCollection(user, collection));

    List<Election> createdElections =
        electionDAO.findLastElectionsByReferenceIds(List.of(dar.getReferenceId()));

    assertTrue(referenceIds.contains(dar.getReferenceId()));

    // Ensure that we have the right number of access and rp elections, i.e. 1 each
    assertFalse(createdElections.isEmpty());
    assertEquals(2, createdElections.size());
    assertEquals(1, createdElections.stream().filter(e -> e.getElectionType().equals(ElectionType.DATA_ACCESS.getValue())).count());
    assertEquals(1, createdElections.stream().filter(e -> e.getElectionType().equals(ElectionType.RP.getValue())).count());
  }

  /**
   * This test covers the case where:
   *  - User is a chair
   *  - Collection has 2 DAR/Dataset combinations
   *  - Elections have been created for a Collection
   *  - User is a DAC chair for only one of the DAR/Dataset combinations
   *  - All elections are canceled
   *  - Chair specific elections are re-created correctly
   *  - Elections created should only be for the DAR/Dataset for the user
   */
  @Test
  public void testCreateElectionsForDarCollectionAfterCancelingEarlierElectionsAsChair() throws Exception {
    initService();

    // Start off with a collection and a single DAR
    DarCollection collection = setUpDarCollectionWithDacDataset();
    Optional<DataAccessRequest> dar = collection.getDars().values().stream().findFirst();
    assertTrue(dar.isPresent());
    List<Integer> datasetIds = dataAccessRequestDAO.findDARDatasetRelations(dar.get().getReferenceId());
    assertFalse(datasetIds.isEmpty());
    Integer datasetId = datasetIds.get(0);
    assertNotNull(datasetId);

    // Find the dac chairperson for the current DAR/Dataset combination
    Optional<Dac> dac = dacDAO.findDacsForDatasetIds(List.of(datasetId)).stream().findFirst();
    assertTrue(dac.isPresent());
    List<User> dacUsers = dacDAO.findMembersByDacId(dac.get().getDacId());
    Optional<User> chair = dacUsers.stream().filter(u -> u.hasUserRole(UserRoles.CHAIRPERSON)).findFirst();
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
          electionDAO.updateElectionById(e.getElectionId(), ElectionStatus.CANCELED.getValue(), new Date()))
    );

    // re-create elections & new votes:
    referenceIds.addAll(serviceDAO.createElectionsForDarCollection(chair.get(), collection));

    List<Election> createdElections =
        electionDAO.findLastElectionsByReferenceIds(List.of(dar.get().getReferenceId()));

    // Ensure that we have the right number of access and rp elections, i.e. 1 each
    assertFalse(createdElections.isEmpty());
    assertEquals(2, createdElections.size());
    assertEquals(1, createdElections.stream().filter(e -> e.getElectionType().equals(ElectionType.DATA_ACCESS.getValue())).count());
    assertEquals(1, createdElections.stream().filter(e -> e.getElectionType().equals(ElectionType.RP.getValue())).count());

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
    Consent consent = createConsent(dac.getDacId());
    Dataset dataset = createDataset();
    consentDAO.insertConsentAssociation(consent.getConsentId(), ASSOCIATION_TYPE_TEST, dataset.getDataSetId());
    Integer collectionId = darCollectionDAO.insertDarCollection(darCode, user.getDacUserId(), new Date());
    createDarForCollection(user, collectionId, dataset);
    DarCollection collection = darCollectionDAO.findDARCollectionByCollectionId(collectionId);
    DataAccessRequest dar = collection.getDars().values().stream().findFirst().orElse(null);
    assertNotNull(dar);
    assertNotNull(dar.getData());
    Date now = new Date();
    dataAccessRequestDAO.updateDataByReferenceIdVersion2(
        dar.getReferenceId(), dar.getUserId(), now, now, now, dar.getData());
    dataAccessRequestDAO.insertDARDatasetRelation(dar.getReferenceId(), dataset.getDataSetId());
    return darCollectionDAO.findDARCollectionByReferenceId(dar.getReferenceId());
  }

  /**
   * Helper method to add a DAR to a collection. This creates a new
   * DAC/Dataset/Chair/Member to facilitate the creation.
   */
  private DataAccessRequest addDARWithDacAndDatasetToCollection(DarCollection collection) {
    // Create new DAC and Dataset:
    Dac dac = createDac();
    createUserWithRoleInDac(UserRoles.CHAIRPERSON.getRoleId(), dac.getDacId());
    createUserWithRoleInDac(UserRoles.MEMBER.getRoleId(), dac.getDacId());
    Consent consent = createConsent(dac.getDacId());
    Dataset dataset = createDataset();
    consentDAO.insertConsentAssociation(consent.getConsentId(), ASSOCIATION_TYPE_TEST, dataset.getDataSetId());

    // Create new DAR with Dataset and add it to the collection
    User user = createUser();
    return createDarForCollection(user, collection.getDarCollectionId(), dataset);
  }
}
