package org.broadinstitute.consent.http.service.dao;

import org.broadinstitute.consent.http.db.DAOTestHelper;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.Consent;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DarCollection;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataSet;
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
    serviceDAO = new DarCollectionServiceDAO(dataSetDAO, electionDAO, jdbi, userDAO);
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

    serviceDAO.createElectionsForDarCollection(user, collection);

    List<Election> createdElections =
        electionDAO.findLastElectionsByReferenceIds(List.of(dar.getReferenceId()));
    List<Vote> createdVotes =
        voteDAO.findVotesByElectionIds(
            createdElections.stream().map(Election::getElectionId).collect(Collectors.toList()));

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

    serviceDAO.createElectionsForDarCollection(user, collection);

    List<Election> createdElections =
        electionDAO.findLastElectionsByReferenceIds(List.of(dar.getReferenceId()));
    List<Vote> createdVotes =
        voteDAO.findVotesByElectionIds(
            createdElections.stream().map(Election::getElectionId).collect(Collectors.toList()));

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
    Integer datasetId = dar.get().getData().getDatasetIds().get(0);
    assertNotNull(datasetId);
    Optional<Dac> dac = dacDAO.findDacsForDatasetIds(List.of(datasetId)).stream().findFirst();
    assertTrue(dac.isPresent());
    List<User> dacUsers = dacDAO.findMembersByDacId(dac.get().getDacId());
    Optional<User> chair = dacUsers.stream().filter(u -> u.hasUserRole(UserRoles.CHAIRPERSON)).findFirst();
    assertTrue(chair.isPresent());

    serviceDAO.createElectionsForDarCollection(chair.get(), collection);

    List<Election> createdElections =
        electionDAO.findLastElectionsByReferenceIds(List.of(dar.get().getReferenceId()));
    List<Vote> createdVotes =
        voteDAO.findVotesByElectionIds(
            createdElections.stream().map(Election::getElectionId).collect(Collectors.toList()));

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
    assertFalse(dar.get().getData().getDatasetIds().isEmpty());
    Integer datasetId = dar.get().getData().getDatasetIds().get(0);
    assertNotNull(datasetId);
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

    serviceDAO.createElectionsForDarCollection(chair.get(), collection);

    List<Election> createdElections =
        electionDAO.findLastElectionsByReferenceIds(List.of(dar.get().getReferenceId()));
    List<Vote> createdVotes =
        voteDAO.findVotesByElectionIds(
            createdElections.stream().map(Election::getElectionId).collect(Collectors.toList()));

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
  public void testCreateElectionsForDarCollectionAfterCancelingEarlierElections() throws Exception {
    initService();

    User user = new User();
    user.addRole(new UserRole(UserRoles.ADMIN.getRoleId(), UserRoles.ADMIN.getRoleName()));
    DarCollection collection = setUpDarCollectionWithDacDataset();
    DataAccessRequest dar = collection.getDars().values().stream().findFirst().orElse(null);
    assertNotNull(dar);

    // create elections & votes:
    serviceDAO.createElectionsForDarCollection(user, collection);

    // cancel those elections:
    electionDAO.findLastElectionsByReferenceIds(List.of(dar.getReferenceId())).forEach(e ->
        electionDAO.updateElectionById(e.getElectionId(), ElectionStatus.CANCELED.getValue(), new Date()));

    // re-create elections & new votes:
    serviceDAO.createElectionsForDarCollection(user, collection);

    List<Election> createdElections =
        electionDAO.findLastElectionsByReferenceIds(List.of(dar.getReferenceId()));

    // Ensure that we have the right number of access and rp elections, i.e. 1 each
    assertFalse(createdElections.isEmpty());
    assertEquals(2, createdElections.size());
    assertEquals(1, createdElections.stream().filter(e -> e.getElectionType().equals(ElectionType.DATA_ACCESS.getValue())).count());
    assertEquals(1, createdElections.stream().filter(e -> e.getElectionType().equals(ElectionType.RP.getValue())).count());
  }

  /**
   * Helper method to generate a DarCollection with a Dac, a Dataset, and a create User
   */
  private DarCollection setUpDarCollectionWithDacDataset() {
    DarCollection collection = createDarCollectionWithSingleDataAccessRequest();
    DataAccessRequest dar = collection.getDars().values().stream().findFirst().orElse(null);
    assertNotNull(dar);
    assertNotNull(dar.getData());
    dar.getData().setDatasetIds(List.of(dar.getData().getDatasetIds().get(0)));
    Date now = new Date();
    dataAccessRequestDAO.updateDataByReferenceIdVersion2(
        dar.getReferenceId(), dar.getUserId(), now, now, now, dar.getData());
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
    DataSet dataset = createDataset();
    createAssociation(consent.getConsentId(), dataset.getDataSetId());

    // Create new DAR with Dataset and add it to the collection
    User user = createUser();
    return createDarForCollection(user, collection.getDarCollectionId(), dataset);
  }
}
