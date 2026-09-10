package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.broadinstitute.consent.http.enumeration.MatchAlgorithm;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetProperty;
import org.broadinstitute.consent.http.models.Match;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.matchmigration.MatchMigrationPopulation;
import org.broadinstitute.consent.http.models.matchmigration.SnapshotReconciliation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MatchMigrationDAOTest extends DAOTestHelper {

  @Test
  void testFindPopulationCountsOnlyWhatTheConstraintsWouldReject() {
    Dataset dataset = createDataset();
    String legacy = createSubmittedDar(dataset, false);
    String current = createSubmittedDar(dataset, false);
    insertMatch(legacy, null, MatchAlgorithm.V1.getVersion());
    insertMatch(current, dataset.getDatasetId(), MatchAlgorithm.V5.getVersion());

    MatchMigrationPopulation population = matchMigrationDAO.findPopulation();
    assertEquals(1, population.affectedMatches());
    assertEquals(1, population.affectedPurposes());
    assertEquals(1, population.versionV1());
    assertEquals(1, population.missingDatasetId());
    assertTrue(population.blocksConstraints());
  }

  @Test
  void testFindPopulationCountsV2AndNullVersionsSeparately() {
    Dataset dataset = createDataset();
    insertMatch(createSubmittedDar(dataset, false), dataset.getDatasetId(), "v2");
    insertMatch(createSubmittedDar(dataset, false), dataset.getDatasetId(), null);

    MatchMigrationPopulation population = matchMigrationDAO.findPopulation();
    assertEquals(2, population.affectedMatches());
    assertEquals(1, population.versionV2());
    assertEquals(1, population.versionNull());
    // Both carry a dataset id already; only their stamps make them affected
    assertEquals(0, population.missingDatasetId());
  }

  @Test
  void testFindPopulationSeparatesArchivedPurposesFromResolvableOnes() {
    Dataset dataset = createDataset();
    insertMatch(createSubmittedDar(dataset, false), null, MatchAlgorithm.V1.getVersion());
    insertMatch(createSubmittedDar(dataset, true), null, MatchAlgorithm.V1.getVersion());

    MatchMigrationPopulation population = matchMigrationDAO.findPopulation();
    assertEquals(2, population.affectedPurposes());
    assertEquals(1, population.resolvablePurposes());
    assertEquals(1, population.archivedOrMissingPurposes());
  }

  @Test
  void testFindPopulationCountsPurposesHoldingMoreThanOneAffectedMatch() {
    Dataset dataset = createDataset();
    String purposeId = createSubmittedDar(dataset, false);
    insertMatch(purposeId, null, MatchAlgorithm.V1.getVersion());
    insertMatch(purposeId, null, MatchAlgorithm.V1.getVersion());

    MatchMigrationPopulation population = matchMigrationDAO.findPopulation();
    assertEquals(2, population.affectedMatches());
    assertEquals(1, population.affectedPurposes());
    assertEquals(1, population.purposesWithMultipleAffectedMatches());
  }

  @Test
  void testFindPopulationCountsExistingDuplicatePurposeDatasetPairs() {
    Dataset dataset = createDataset();
    String purposeId = createSubmittedDar(dataset, false);
    // Two current rows on the same pair: nothing this migration created, but the uniqueness
    // constraint would still refuse them
    insertMatch(purposeId, dataset.getDatasetId(), MatchAlgorithm.V5.getVersion());
    insertMatch(purposeId, dataset.getDatasetId(), MatchAlgorithm.V5.getVersion());

    MatchMigrationPopulation population = matchMigrationDAO.findPopulation();
    assertEquals(0, population.affectedMatches());
    assertEquals(1, population.duplicatePurposeDatasetPairs());
    // Affected count is clear, but the pair collision still blocks
    assertTrue(population.blocksConstraints());
  }

  @Test
  void testResolvableAndUnresolvablePurposesPartitionTheAffectedSet() {
    Dataset dataset = createDataset();
    String resolvable = createSubmittedDar(dataset, false);
    String archived = createSubmittedDar(dataset, true);
    insertMatch(resolvable, null, MatchAlgorithm.V1.getVersion());
    insertMatch(archived, null, MatchAlgorithm.V1.getVersion());

    assertEquals(List.of(resolvable), matchMigrationDAO.findResolvablePurposes());
    assertEquals(List.of(archived), matchMigrationDAO.findUnresolvablePurposes());
  }

  @Test
  void testUnresolvablePurposesIncludesMatchesWithNoDarAtAll() {
    // A match whose purpose has no data_access_request row: findByReferenceId returns null for it,
    // so a reprocess would delete the rows and insert nothing
    String orphaned = UUID.randomUUID().toString();
    insertMatch(orphaned, null, MatchAlgorithm.V1.getVersion());

    assertTrue(matchMigrationDAO.findResolvablePurposes().isEmpty());
    assertEquals(List.of(orphaned), matchMigrationDAO.findUnresolvablePurposes());
  }

  @Test
  void testSnapshotCapturesAffectedRowsAndTheirRationales() {
    Dataset dataset = createDataset();
    String purposeId = createSubmittedDar(dataset, false);
    Integer affected = insertMatch(purposeId, null, MatchAlgorithm.V1.getVersion());
    matchDAO.insertRationale(affected, "legacy rationale");
    insertMatch(
        createSubmittedDar(dataset, false), dataset.getDatasetId(), MatchAlgorithm.V5.getVersion());

    assertEquals(1, matchMigrationDAO.snapshotAffectedMatches());
    assertEquals(1, matchMigrationDAO.snapshotAffectedRationales());
    assertEquals(List.of(affected), snapshottedMatchIds());
    assertEquals(List.of("legacy rationale"), snapshottedRationales(affected));
  }

  @Test
  void testSnapshotCapturesCurrentRowsSharingAReprocessedPurpose() {
    // A reprocess deletes every row for the purpose, not just the affected one, so a current row
    // sitting alongside a legacy one has to be captured or it is destroyed with no way back
    Dataset legacyDataset = createDataset();
    Dataset currentDataset = createDataset();
    String purposeId = createSubmittedDar(legacyDataset, false);
    Integer legacy = insertMatch(purposeId, null, MatchAlgorithm.V1.getVersion());
    Integer current =
        insertMatch(purposeId, currentDataset.getDatasetId(), MatchAlgorithm.V5.getVersion());

    assertEquals(2, matchMigrationDAO.snapshotAffectedMatches());
    assertEquals(List.of(legacy, current), snapshottedMatchIds());
  }

  @Test
  void testSnapshotLeavesCurrentRowsOnPurposesThatAreNotReprocessed() {
    // The widening is scoped to purposes a run will touch; everything else stays out
    Dataset dataset = createDataset();
    insertMatch(
        createSubmittedDar(dataset, false), dataset.getDatasetId(), MatchAlgorithm.V5.getVersion());

    assertEquals(0, matchMigrationDAO.snapshotAffectedMatches());
    assertEquals(List.of(), snapshottedMatchIds());
  }

  @Test
  void testSnapshotKeepsTwoRationalesThatShareTheirText() {
    // Keyed on each row's own id, so identical text is still two rows and a restore rebuilds two
    Dataset dataset = createDataset();
    String purposeId = createSubmittedDar(dataset, false);
    Integer matchId = insertMatch(purposeId, null, MatchAlgorithm.V1.getVersion());
    matchDAO.insertRationale(matchId, "same text");
    matchDAO.insertRationale(matchId, "same text");

    matchMigrationDAO.snapshotAffectedMatches();
    assertEquals(2, matchMigrationDAO.snapshotAffectedRationales());
    assertEquals(List.of("same text", "same text"), snapshottedRationales(matchId));

    // And a second pass still captures nothing new
    assertEquals(0, matchMigrationDAO.snapshotAffectedRationales());
  }

  @Test
  void testSnapshotIsRerunnableAndKeepsTheFirstCapture() {
    Dataset dataset = createDataset();
    String purposeId = createSubmittedDar(dataset, false);
    Integer matchId = insertMatch(purposeId, null, MatchAlgorithm.V1.getVersion());
    matchDAO.insertRationale(matchId, "first");
    matchMigrationDAO.snapshotAffectedMatches();
    matchMigrationDAO.snapshotAffectedRationales();

    // A second pass captures nothing new and leaves the original capture in place
    assertEquals(0, matchMigrationDAO.snapshotAffectedMatches());
    assertEquals(0, matchMigrationDAO.snapshotAffectedRationales());
    assertEquals(List.of(matchId), snapshottedMatchIds());
    assertEquals(List.of("first"), snapshottedRationales(matchId));
  }

  @Test
  void testSnapshotSkipsRationalesOfMatchesItDidNotCapture() {
    Dataset dataset = createDataset();
    Integer current =
        insertMatch(
            createSubmittedDar(dataset, false),
            dataset.getDatasetId(),
            MatchAlgorithm.V5.getVersion());
    matchDAO.insertRationale(current, "current rationale");

    assertEquals(0, matchMigrationDAO.snapshotAffectedMatches());
    assertEquals(0, matchMigrationDAO.snapshotAffectedRationales());
  }

  @Test
  void testReconcileCountsARowStillPresentAsUnhandled() {
    Dataset dataset = createDataset();
    String purposeId = createSubmittedDar(dataset, false);
    insertMatch(purposeId, null, MatchAlgorithm.V1.getVersion());
    matchMigrationDAO.snapshotAffectedMatches();

    SnapshotReconciliation reconciliation = matchMigrationDAO.reconcile();
    assertEquals(1, reconciliation.snapshotted());
    assertEquals(1, reconciliation.snapshottedRowsRemaining());
    assertEquals(0, reconciliation.snapshottedRowsGone());
    assertEquals(1, reconciliation.stillAffected());
    // Resolvable, so it should have been reprocessed; nothing was skipped
    assertEquals(0, reconciliation.unresolvableRows());
    assertFalse(reconciliation.reconciles());
  }

  @Test
  void testReconcileTreatsAReplacedRowAsHandled() {
    Dataset dataset = createDataset();
    String purposeId = createSubmittedDar(dataset, false);
    insertMatch(purposeId, null, MatchAlgorithm.V1.getVersion());
    matchMigrationDAO.snapshotAffectedMatches();

    // Stands in for a reprocess: the old row goes, a replacement arrives under a new id
    matchDAO.deleteMatchesByPurposeId(purposeId);
    insertMatch(purposeId, dataset.getDatasetId(), MatchAlgorithm.V5.getVersion());

    SnapshotReconciliation reconciliation = matchMigrationDAO.reconcile();
    assertEquals(1, reconciliation.snapshottedRowsGone());
    assertEquals(0, reconciliation.snapshottedRowsRemaining());
    assertEquals(1, reconciliation.purposesHoldingMatches());
    assertEquals(0, reconciliation.purposesHoldingNoMatches());
    assertEquals(0, reconciliation.stillAffected());
    assertTrue(reconciliation.reconciles());
  }

  @Test
  void testReconcileTreatsAPurposeRebuiltToNothingAsHandled() {
    Dataset dataset = createDataset();
    String purposeId = createSubmittedDar(dataset, false);
    insertMatch(purposeId, null, MatchAlgorithm.V1.getVersion());
    matchMigrationDAO.snapshotAffectedMatches();

    // A DAR with no dataset associations rebuilds to nothing, which deletes its rows
    matchDAO.deleteMatchesByPurposeId(purposeId);

    SnapshotReconciliation reconciliation = matchMigrationDAO.reconcile();
    assertEquals(1, reconciliation.snapshottedRowsGone());
    assertEquals(1, reconciliation.purposesHoldingNoMatches());
    assertEquals(0, reconciliation.purposesHoldingMatches());
    assertTrue(reconciliation.reconciles());
  }

  @Test
  void testReconcileAcceptsSkippedRowsThatAreStillAffected() {
    Dataset dataset = createDataset();
    String archived = createSubmittedDar(dataset, true);
    insertMatch(archived, null, MatchAlgorithm.V1.getVersion());
    matchMigrationDAO.snapshotAffectedMatches();

    // Never reprocessed, by design. The run is still complete: what remains is what it skipped.
    SnapshotReconciliation reconciliation = matchMigrationDAO.reconcile();
    assertEquals(1, reconciliation.snapshottedRowsRemaining());
    assertEquals(1, reconciliation.unresolvableRows());
    assertEquals(1, reconciliation.stillAffected());
    // Still holds its original rows, which is why this counter is named for the state
    assertEquals(1, reconciliation.purposesHoldingMatches());
    assertTrue(reconciliation.reconciles());
  }

  private Integer insertMatch(String purposeId, Integer datasetId, String algorithmVersion) {
    Match match = new Match();
    match.setConsent("DUOS-" + randomInt(1, 999999));
    match.setDatasetId(datasetId);
    match.setPurpose(purposeId);
    match.setMatch(true);
    match.setFailed(false);
    match.setAbstain(false);
    match.setCreateDate(FIXED_DATE);
    match.setAlgorithmVersion(algorithmVersion);
    return matchDAO.insertMatch(match);
  }

  /** A submitted DAR carrying one dataset association, archived or not. */
  private String createSubmittedDar(Dataset dataset, boolean archived) {
    User user = createUser();
    String darCode = "DAR-" + randomInt(1, 999999999);
    Integer collectionId =
        darCollectionDAO.insertDarCollection(darCode, user.getUserId(), new Date());
    DataAccessRequestData data = new DataAccessRequestData();
    data.setProjectTitle("Project Title: " + randomAlphabetic(20));
    if (archived) {
      data.setStatus("Archived");
    }
    String referenceId = UUID.randomUUID().toString();
    Date now = new Date();
    dataAccessRequestDAO.insertDataAccessRequest(
        collectionId, referenceId, user.getUserId(), now, now, now, data, randomAlphabetic(10));
    dataAccessRequestDAO.insertDARDatasetRelation(referenceId, dataset.getDatasetId());
    return referenceId;
  }

  private static List<Integer> snapshottedMatchIds() {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery("SELECT match_id FROM match_migration_snapshot ORDER BY match_id")
                .mapTo(Integer.class)
                .list());
  }

  private static List<String> snapshottedRationales(Integer matchId) {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery(
                    """
                    SELECT rationale FROM match_migration_rationale_snapshot
                    WHERE match_id = :matchId ORDER BY rationale
                    """)
                .bind("matchId", matchId)
                .mapTo(String.class)
                .list());
  }

  private Dataset createDataset() {
    User user = createUser();
    String name = "Name_" + randomAlphanumeric(20);
    String objectId = "Object ID_" + randomAlphanumeric(20);
    DataUse dataUse = new DataUseBuilder().setGeneralUse(true).build();
    Integer id =
        datasetDAO.insertDataset(
            name, FIXED_TIMESTAMP, user.getUserId(), objectId, dataUse.toString(), null);
    List<DatasetProperty> properties = new ArrayList<>();
    DatasetProperty property = new DatasetProperty();
    property.setDatasetId(id);
    property.setPropertyKey(1);
    property.setPropertyValue("Test_PropertyValue");
    property.setCreateDate(FIXED_DATE);
    properties.add(property);
    datasetDAO.insertDatasetProperties(properties);
    return datasetDAO.findDatasetById(id);
  }
}
