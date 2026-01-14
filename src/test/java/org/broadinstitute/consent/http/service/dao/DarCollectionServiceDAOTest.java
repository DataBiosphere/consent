package org.broadinstitute.consent.http.service.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
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
import org.broadinstitute.consent.http.models.Vote;
import org.broadinstitute.consent.http.rules.DACAutomationRule;
import org.broadinstitute.consent.http.rules.DACAutomationRuleType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DarCollectionServiceDAOTest extends DAOTestHelper {

  private static DarCollectionServiceDAO serviceDAO;

  @BeforeAll
  static void initService() {
    serviceDAO = new DarCollectionServiceDAO(datasetDAO, electionDAO, jdbi, userDAO);
  }

  /**
   * This test covers the case where: - User is an admin - Collection has 1 DAR/Dataset combinations
   * - Elections created should be for the DAR/Dataset for the user
   */
  @Test
  void testCreateElectionsForDarByUserAdmin() throws Exception {
    User user = new User();
    user.setAdminRole();
    DarCollection collection = setUpDarCollectionWithDacDataset();
    DataAccessRequest dar = collection.getDars().values().stream().findFirst().orElse(null);
    assertNotNull(dar);

    List<String> referenceIds = serviceDAO.createElectionsForDarByUser(user, dar);

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
    Optional<Election> daElectionOption =
        createdElections.stream()
            .filter(e -> ElectionType.DATA_ACCESS.getValue().equals(e.getElectionType()))
            .findFirst();
    assertTrue(daElectionOption.isPresent());
    assertTrue(
        createdVotes.stream()
            .filter(v -> v.getElectionId().equals(daElectionOption.get().getElectionId()))
            .anyMatch(v -> v.getType().equals(VoteType.CHAIRPERSON.getValue())));
    assertTrue(
        createdVotes.stream()
            .filter(v -> v.getElectionId().equals(daElectionOption.get().getElectionId()))
            .anyMatch(v -> v.getType().equals(VoteType.DAC.getValue())));
    assertTrue(
        createdVotes.stream()
            .filter(v -> v.getElectionId().equals(daElectionOption.get().getElectionId()))
            .anyMatch(v -> v.getType().equals(VoteType.FINAL.getValue())));
    assertTrue(
        createdVotes.stream()
            .filter(v -> v.getElectionId().equals(daElectionOption.get().getElectionId()))
            .anyMatch(v -> v.getType().equals(VoteType.AGREEMENT.getValue())));

    // RP Elections have Chair and Dac votes
    Optional<Election> rpElectionOption =
        createdElections.stream()
            .filter(e -> ElectionType.RP.getValue().equals(e.getElectionType()))
            .findFirst();
    assertTrue(rpElectionOption.isPresent());
    assertTrue(
        createdVotes.stream()
            .filter(v -> v.getElectionId().equals(rpElectionOption.get().getElectionId()))
            .anyMatch(v -> v.getType().equals(VoteType.CHAIRPERSON.getValue())));
    assertTrue(
        createdVotes.stream()
            .filter(v -> v.getElectionId().equals(rpElectionOption.get().getElectionId()))
            .anyMatch(v -> v.getType().equals(VoteType.DAC.getValue())));
  }

  /**
   * This test covers the case where: - User is an admin - Collection has 2 DAR/Dataset combinations
   * - User is an Admin - Elections created should only be for ALL the DAR/Dataset combinations
   */
  @Test
  void testCreateElectionsForDarCollectionWithMultipleDatasetsForAdminBy() throws Exception {
    User user = new User();
    user.setAdminRole();
    DarCollection collection = setUpDarCollectionWithDacDataset();
    DataAccessRequest dar = collection.getDars().values().stream().findFirst().orElse(null);
    assertNotNull(dar);

    // refresh the collection
    collection = darCollectionDAO.findDARCollectionByCollectionId(collection.getDarCollectionId());

    List<String> referenceIds = serviceDAO.createElectionsForDarByUser(user, dar);

    List<Election> createdElections =
        electionDAO.findLastElectionsByReferenceIds(List.of(dar.getReferenceId()));
    List<Vote> createdVotes =
        voteDAO.findVotesByElectionIds(
            createdElections.stream().map(Election::getElectionId).toList());

    assertTrue(referenceIds.contains(dar.getReferenceId()));
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
    assertTrue(createdVotes.stream().anyMatch(v -> v.getType().equals(VoteType.FINAL.getValue())));
    assertTrue(createdVotes.stream().anyMatch(v -> v.getType().equals(VoteType.DAC.getValue())));
    assertTrue(
        createdVotes.stream().anyMatch(v -> v.getType().equals(VoteType.AGREEMENT.getValue())));
  }

  /**
   * This test covers the case where: - User is a chairperson - Collection has 1 DAR/Dataset
   * combinations - Elections created should be for the DAR/Dataset for the user
   */
  @Test
  void testCreateElectionsForDarByUserChair() throws Exception {
    DarCollection collection = setUpDarCollectionWithDacDataset();
    DataAccessRequest dar = collection.getDars().values().stream().findFirst().orElseThrow();
    Integer datasetId = dar.getDatasetIds().getFirst();
    assertNotNull(datasetId);
    Optional<Dac> dac = dacDAO.findDacsForDatasetIds(List.of(datasetId)).stream().findFirst();
    assertTrue(dac.isPresent());
    List<User> dacUsers = dacDAO.findMembersByDacId(dac.get().getDacId());
    Optional<User> chair =
        dacUsers.stream().filter(u -> u.hasUserRole(UserRoles.CHAIRPERSON)).findFirst();
    assertTrue(chair.isPresent());

    List<String> referenceIds = serviceDAO.createElectionsForDarByUser(chair.get(), dar);

    List<Election> createdElections =
        electionDAO.findLastElectionsByReferenceIds(List.of(dar.getReferenceId()));
    List<Vote> createdVotes =
        voteDAO.findVotesByElectionIds(
            createdElections.stream().map(Election::getElectionId).collect(Collectors.toList()));

    assertTrue(referenceIds.contains(dar.getReferenceId()));
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
    assertTrue(createdVotes.stream().anyMatch(v -> v.getType().equals(VoteType.FINAL.getValue())));
    assertTrue(createdVotes.stream().anyMatch(v -> v.getType().equals(VoteType.DAC.getValue())));
    assertTrue(
        createdVotes.stream().anyMatch(v -> v.getType().equals(VoteType.AGREEMENT.getValue())));
  }

  /**
   * This test covers the case where: - User is a chairperson - Collection has 2 DAR/Dataset
   * combinations - User is a DAC chair for only one of the DAR/Dataset combinations - Elections
   * created should only be for the DAR/Dataset for the user
   */
  @Test
  void testCreateElectionsForDarCollectionWithMultipleDatasetsForChairBy() throws Exception {
    // Start off with a collection and a single DAR
    DarCollection collection = setUpDarCollectionWithDacDataset();
    DataAccessRequest dar = collection.getMostRecentDar();
    assertNotNull(dar);
    assertFalse(dar.getDatasetIds().isEmpty());
    Integer datasetId1 = dar.getDatasetIds().get(0);
    Integer datasetId2 = dar.getDatasetIds().get(1);
    assertEquals(2, dar.getDatasetIds().size());

    // Find the dac chairperson for the current DAR/Dataset combination
    Dac dac = dacDAO.findDacsForDatasetIds(List.of(datasetId1)).stream().findFirst().orElseThrow();
    assertNotNull(dac);
    List<User> dacUsers = dacDAO.findMembersByDacId(dac.getDacId());
    User chair =
        dacUsers.stream()
            .filter(u -> u.hasUserRole(UserRoles.CHAIRPERSON))
            .findFirst()
            .orElseThrow();

    // refresh the collection
    collection = darCollectionDAO.findDARCollectionByCollectionId(collection.getDarCollectionId());

    List<String> referenceIds = serviceDAO.createElectionsForDarByUser(chair, dar);

    List<Election> createdElections =
        electionDAO.findLastElectionsByReferenceIds(List.of(dar.getReferenceId()));
    List<Vote> createdVotes =
        voteDAO.findVotesByElectionIds(
            createdElections.stream().map(Election::getElectionId).collect(Collectors.toList()));

    assertTrue(referenceIds.contains(dar.getReferenceId()));
    assertEquals(2, createdElections.size()); //
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
    assertTrue(createdVotes.stream().anyMatch(v -> v.getType().equals(VoteType.FINAL.getValue())));
    assertTrue(createdVotes.stream().anyMatch(v -> v.getType().equals(VoteType.DAC.getValue())));
    assertTrue(
        createdVotes.stream().anyMatch(v -> v.getType().equals(VoteType.AGREEMENT.getValue())));

    assertEquals(
        8,
        createdVotes.size()); // 1 dataset X 2 elections/dataset X 4 votes/election each = 8 votes.

    // Find the dac chairperson for the second Dataset in the DAR.

    Dac dac2 = dacDAO.findDacsForDatasetIds(List.of(datasetId2)).stream().findFirst().orElseThrow();
    assertNotNull(dac2);
    List<User> dacUsers2 = dacDAO.findMembersByDacId(dac2.getDacId());
    User chair2 =
        dacUsers2.stream()
            .filter(u -> u.hasUserRole(UserRoles.CHAIRPERSON))
            .findFirst()
            .orElseThrow();
    List<String> referenceIds2 = serviceDAO.createElectionsForDarByUser(chair2, dar);
    assertTrue(referenceIds2.contains(dar.getReferenceId()));

    createdElections = electionDAO.findElectionsByReferenceId(dar.getReferenceId());
    assertEquals(4, createdElections.size());

    // Verify we have elections for both DACs
    assertTrue(
        createdElections.stream()
            .anyMatch(e -> e.getDatasetId().equals(dac.getDatasetIds().getFirst())));
    assertTrue(
        createdElections.stream()
            .anyMatch(e -> e.getDatasetId().equals(dac2.getDatasetIds().getFirst())));

    // Verify we have open votes for both datasets on the DAR.
    createdVotes =
        voteDAO.findVotesByElectionIds(
            createdElections.stream().map(Election::getElectionId).toList());
    assertEquals(
        16,
        createdVotes
            .size()); // 2 datasets X 2 elections/dataset X 4 votes/election each = 16 votes.
  }

  @Test
  void testCreateElectionsForProgressReportWithMultipleDatasets() throws SQLException {
    // Start off with a collection and a single DAR
    DarCollection collection = setUpDarCollectionWithDacDataset();
    DataAccessRequest dar = collection.getDars().values().stream().findFirst().orElseThrow();
    assertFalse(dar.getDatasetIds().isEmpty());
    Integer datasetId = dar.getDatasetIds().getFirst();

    // Find the dac chairperson for the current DAR/Dataset combination
    Dac dac = dacDAO.findDacsForDatasetIds(List.of(datasetId)).stream().findFirst().orElseThrow();
    List<User> dacUsers = dacDAO.findMembersByDacId(dac.getDacId());
    User chair =
        dacUsers.stream()
            .filter(u -> u.hasUserRole(UserRoles.CHAIRPERSON))
            .findFirst()
            .orElseThrow();

    // Refresh the collection
    collection = darCollectionDAO.findDARCollectionByCollectionId(collection.getDarCollectionId());

    // Create the election for one dataset.
    serviceDAO.createElectionsForDarByUser(chair, dar);

    DataAccessRequest progressReport = createProgressReportFromDAR(dar);
    // Refresh the collection to get the version with the progress report.
    collection = darCollectionDAO.findDARCollectionByCollectionId(collection.getDarCollectionId());

    List<String> electionReferenceIds =
        serviceDAO.createElectionsForDarByUser(chair, collection.getMostRecentDar());
    assertTrue(electionReferenceIds.contains(progressReport.getReferenceId()));
  }

  /**
   * This test covers the case where: - User is an admin - Elections have been created for a
   * Collection - Elections are then canceled - Elections re-created correctly - Previous canceled
   * elections are correctly archived
   */
  @Test
  void testCreateElectionsForDarCollectionAfterCancelingEarlierElectionsAsAdminBy()
      throws Exception {
    User user = new User();
    user.setAdminRole();
    DarCollection collection = setUpDarCollectionWithDacDataset();
    DataAccessRequest dar = collection.getDars().values().stream().findFirst().orElse(null);
    assertNotNull(dar);

    // create elections & votes:
    List<String> referenceIds = serviceDAO.createElectionsForDarByUser(user, dar);

    // cancel those elections:
    List<Integer> canceledElectionIds =
        electionDAO.findLastElectionsByReferenceIds(List.of(dar.getReferenceId())).stream()
            .map(Election::getElectionId)
            .toList();
    canceledElectionIds.forEach(
        id -> electionDAO.updateElectionById(id, ElectionStatus.CANCELED.getValue(), new Date()));

    // re-create elections & new votes:
    referenceIds.addAll(serviceDAO.createElectionsForDarByUser(user, dar));

    List<Election> createdElections =
        electionDAO.findLastElectionsByReferenceIds(List.of(dar.getReferenceId()));

    assertTrue(referenceIds.contains(dar.getReferenceId()));

    // Ensure that we have the right number of access and rp elections, i.e. 1 each
    assertFalse(createdElections.isEmpty());
    assertEquals(2, createdElections.size());
    assertEquals(
        1,
        createdElections.stream()
            .filter(e -> e.getElectionType().equals(ElectionType.DATA_ACCESS.getValue()))
            .count());
    assertEquals(
        1,
        createdElections.stream()
            .filter(e -> e.getElectionType().equals(ElectionType.RP.getValue()))
            .count());

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
  void testCreateElectionsForDarCollectionAfterCancelingEarlierElectionsAsChairBy()
      throws Exception {
    // Start off with a collection and a single DAR
    DarCollection collection = setUpDarCollectionWithDacDataset();
    DataAccessRequest dar = collection.getDars().values().stream().findFirst().orElse(null);
    assertNotNull(dar);
    assertFalse(dar.getDatasetIds().isEmpty());
    Integer datasetId = dar.getDatasetIds().getFirst();
    assertNotNull(datasetId);

    // Find the dac chairperson for the current DAR/Dataset combination
    Optional<Dac> dac = dacDAO.findDacsForDatasetIds(List.of(datasetId)).stream().findFirst();
    assertTrue(dac.isPresent());
    List<User> dacUsers = dacDAO.findMembersByDacId(dac.get().getDacId());
    Optional<User> chair =
        dacUsers.stream().filter(u -> u.hasUserRole(UserRoles.CHAIRPERSON)).findFirst();
    assertTrue(chair.isPresent());

    // refresh the collection
    collection = darCollectionDAO.findDARCollectionByCollectionId(collection.getDarCollectionId());

    // create elections & votes:
    List<String> referenceIds = serviceDAO.createElectionsForDarByUser(chair.get(), dar);

    // cancel elections for all DARs in the collection:
    collection
        .getDars()
        .values()
        .forEach(
            d ->
                electionDAO
                    .findLastElectionsByReferenceIds(List.of(d.getReferenceId()))
                    .forEach(
                        e ->
                            electionDAO.updateElectionById(
                                e.getElectionId(),
                                ElectionStatus.CANCELED.getValue(),
                                new Date())));

    // re-create elections & new votes:
    referenceIds.addAll(serviceDAO.createElectionsForDarByUser(chair.get(), dar));

    List<Election> createdElections =
        electionDAO.findLastElectionsByReferenceIds(List.of(dar.getReferenceId()));

    // Ensure that we have the right number of access and rp elections, i.e. 1 each
    assertFalse(createdElections.isEmpty());
    assertEquals(2, createdElections.size());
    assertEquals(
        1,
        createdElections.stream()
            .filter(e -> e.getElectionType().equals(ElectionType.DATA_ACCESS.getValue()))
            .count());
    assertEquals(
        1,
        createdElections.stream()
            .filter(e -> e.getElectionType().equals(ElectionType.RP.getValue()))
            .count());

    // create progress report
    createProgressReportFromDAR(dar);
    collection = darCollectionDAO.findDARCollectionByCollectionId(collection.getDarCollectionId());
    List<String> createdElectionsForProgressReport =
        serviceDAO.createElectionsForDarByUser(chair.get(), collection.getMostRecentDar());
    assertFalse(createdElectionsForProgressReport.isEmpty());
    assertEquals(1, createdElectionsForProgressReport.size());
  }

  ///
  /// This test covers the case where:
  /// - User is a Signing Official for the DAR under test
  /// - Collection has 1 DAR/Dataset combinations
  /// - Elections created should be for the DAR/Dataset
  ///
  @Test
  void testCreateElectionsForDarByUserSigningOfficial() throws Exception {
    DarCollection collection = setUpDarCollectionWithDacDataset();
    DataAccessRequest dar = collection.getDars().values().stream().findFirst().orElseThrow();
    Integer datasetId = dar.getDatasetIds().getFirst();
    assertNotNull(datasetId);
    Optional<Dac> dac = dacDAO.findDacsForDatasetIds(List.of(datasetId)).stream().findFirst();
    assertTrue(dac.isPresent());
    List<User> dacUsers = dacDAO.findMembersByDacId(dac.get().getDacId());
    Optional<User> chair =
        dacUsers.stream().filter(u -> u.hasUserRole(UserRoles.CHAIRPERSON)).findFirst();
    assertTrue(chair.isPresent());

    // Set up Signing Official for DAR
    User signingOfficial = createUserWithRole(UserRoles.SIGNINGOFFICIAL.getRoleId());
    dar.getData().setSigningOfficial(signingOfficial.getDisplayName());
    dar.getData().setSigningOfficialEmail(signingOfficial.getEmail());

    // Set up DAC Automation Rule
    // TODO: This needs to be `REQUIRE_SO_DAR_APPROVAL` when DT-2786 and DT-2787 are complete
    List<DACAutomationRule> rules =
        dacAutomationRuleDAO.findAll().stream().filter(r -> r.ruleType().equals(
            DACAutomationRuleType.GRU_V1)).toList();
    assertFalse(rules.isEmpty());

    dacAutomationRuleDAO.auditedInsertDACRuleSetting(
        dac.get().getDacId(), rules.getFirst().id(), chair.get().getUserId(), Instant.now());

    List<String> referenceIds = serviceDAO.createElectionsForDarByUser(signingOfficial, dar);

    List<Election> createdElections =
        electionDAO.findLastElectionsByReferenceIds(List.of(dar.getReferenceId()));
    List<Vote> createdVotes =
        voteDAO.findVotesByElectionIds(
            createdElections.stream().map(Election::getElectionId).toList());

    assertTrue(referenceIds.contains(dar.getReferenceId()));
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
    assertTrue(createdVotes.stream().anyMatch(v -> v.getType().equals(VoteType.FINAL.getValue())));
    assertTrue(createdVotes.stream().anyMatch(v -> v.getType().equals(VoteType.DAC.getValue())));
    assertTrue(
        createdVotes.stream().anyMatch(v -> v.getType().equals(VoteType.AGREEMENT.getValue())));
  }

  ///
  /// This test covers the case where:
  /// - User is NOT a Signing Official for the DAR under test
  /// - Collection has 1 DAR/Dataset combinations
  /// - No elections should be created
  ///
  @Test
  void testCreateElectionsForDarByUserSigningOfficialNotInDAR() throws Exception {
    DarCollection collection = setUpDarCollectionWithDacDataset();
    DataAccessRequest dar = collection.getDars().values().stream().findFirst().orElseThrow();
    Integer datasetId = dar.getDatasetIds().getFirst();
    assertNotNull(datasetId);
    Optional<Dac> dac = dacDAO.findDacsForDatasetIds(List.of(datasetId)).stream().findFirst();
    assertTrue(dac.isPresent());
    List<User> dacUsers = dacDAO.findMembersByDacId(dac.get().getDacId());
    Optional<User> chair =
        dacUsers.stream().filter(u -> u.hasUserRole(UserRoles.CHAIRPERSON)).findFirst();
    assertTrue(chair.isPresent());

    // Set up Signing Official so they are NOT the SO for the DAR
    User signingOfficial = createUserWithRole(UserRoles.SIGNINGOFFICIAL.getRoleId());
    User otherSO = createUserWithRole(UserRoles.SIGNINGOFFICIAL.getRoleId());
    dar.getData().setSigningOfficial(otherSO.getDisplayName());
    dar.getData().setSigningOfficialEmail(otherSO.getEmail());

    // Set up DAC Automation Rule
    // TODO: This needs to be `REQUIRE_SO_DAR_APPROVAL` when DT-2786 and DT-2787 are complete
    List<DACAutomationRule> rules =
        dacAutomationRuleDAO.findAll().stream().filter(r -> r.ruleType().equals(
            DACAutomationRuleType.GRU_V1)).toList();
    assertFalse(rules.isEmpty());

    dacAutomationRuleDAO.auditedInsertDACRuleSetting(
        dac.get().getDacId(), rules.getFirst().id(), chair.get().getUserId(), Instant.now());

    List<String> referenceIds = serviceDAO.createElectionsForDarByUser(signingOfficial, dar);
    assertTrue(referenceIds.isEmpty());
  }

  private DataAccessRequest createProgressReportFromDAR(DataAccessRequest dar) {
    String referenceId = UUID.randomUUID().toString();
    dataAccessRequestDAO.insertProgressReport(
        dar.getId(),
        dar.getCollectionId(),
        referenceId,
        dar.getUserId(),
        dar.getData(),
        randomAlphabetic(8));
    DataAccessRequest progressReport = dataAccessRequestDAO.findByReferenceId(referenceId);
    dar.getDatasetIds()
        .forEach(
            datasetId -> dataAccessRequestDAO.insertDARDatasetRelation(referenceId, datasetId));
    return progressReport;
  }

  /** Helper method to generate a DarCollection with a Dac, a Dataset, and a create User */
  private DarCollection setUpDarCollectionWithDacDataset() {
    User user = createUser();
    String darCode = "DAR-" + randomInt(100, 1000);
    DacAndDataset dacAndDataset = createDacAndDataset();
    DacAndDataset dacAndDataset2 = createDacAndDataset();
    Integer collectionId =
        darCollectionDAO.insertDarCollection(darCode, user.getUserId(), new Date());
    createDarForCollection(user, collectionId, dacAndDataset.dataset);
    DarCollection collection = darCollectionDAO.findDARCollectionByCollectionId(collectionId);
    DataAccessRequest dar = collection.getDars().values().stream().findFirst().orElseThrow();
    assertNotNull(dar.getData());
    dataAccessRequestDAO.insertDARDatasetRelation(
        dar.getReferenceId(), dacAndDataset.dataset.getDatasetId());
    dataAccessRequestDAO.insertDARDatasetRelation(
        dar.getReferenceId(), dacAndDataset2.dataset.getDatasetId());
    Date now = new Date();
    dataAccessRequestDAO.updateDataByReferenceId(
        dar.getReferenceId(), dar.getUserId(), now, now, dar.getData(), user.getEraCommonsId());
    return darCollectionDAO.findDARCollectionByReferenceId(dar.getReferenceId());
  }

  private record DacAndDataset(Dac dac, Dataset dataset) {}

  private DacAndDataset createDacAndDataset() {
    Dac dac = createDac();
    createUserWithRoleInDac(UserRoles.CHAIRPERSON.getRoleId(), dac.getDacId());
    createUserWithRoleInDac(UserRoles.MEMBER.getRoleId(), dac.getDacId());
    Dataset dataset = createDatasetWithDac(dac.getDacId());
    return new DacAndDataset(dac, dataset);
  }

  private Dac createDac() {
    Integer id =
        dacDAO.createDac(
            "Test_" + randomAlphabetic(20),
            "Test_" + randomAlphabetic(20),
            new Date());
    return dacDAO.findById(id);
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

  private DataAccessRequest createDarForCollection(
      User user, Integer collectionId, Dataset dataset) {
    Date now = new Date();
    DataAccessRequest dar = new DataAccessRequest();
    dar.setReferenceId(UUID.randomUUID().toString());
    DataAccessRequestData data = new DataAccessRequestData();
    dar.setData(data);
    dataAccessRequestDAO.insertDraftDataAccessRequest(
        dar.getReferenceId(), user.getUserId(), now, now, data);
    dataAccessRequestDAO.updateDraftToSubmittedForCollection(collectionId, dar.getReferenceId());
    dataAccessRequestDAO.updateDataByReferenceId(
        dar.referenceId, dar.userId, new Date(), new Date(), data, user.getEraCommonsId());
    dataAccessRequestDAO.insertDARDatasetRelation(dar.getReferenceId(), dataset.getDatasetId());
    return dataAccessRequestDAO.findByReferenceId(dar.getReferenceId());
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
}
