package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.broadinstitute.consent.http.enumeration.DecidedVia;
import org.broadinstitute.consent.http.enumeration.DecisionState;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.InstitutionSource;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.Collaborator;
import org.broadinstitute.consent.http.models.DarDatasetDecision;
import org.broadinstitute.consent.http.models.DarDatasetTurnaround;
import org.broadinstitute.consent.http.models.DarDecision;
import org.broadinstitute.consent.http.models.DarTurnaround;
import org.broadinstitute.consent.http.models.DarVolume;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.DecisionBucketCount;
import org.broadinstitute.consent.http.models.InstitutionDarCount;
import org.broadinstitute.consent.http.models.ResearcherDarCount;
import org.broadinstitute.consent.http.models.TurnaroundBucket;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.VolumeBucketCount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DarMetricsDAOTest extends DAOTestHelper {

  private static final List<Integer> ALL_DACS = null;

  private static final Instant FROM = Instant.parse("2000-01-01T00:00:00Z");
  private static final Instant TO = Instant.parse("2100-01-01T00:00:00Z");
  private static final Date SUBMITTED = Date.from(Instant.parse("2026-02-10T00:00:00Z"));
  private static final Date DAY_1 = Date.from(Instant.parse("2026-03-01T00:00:00Z"));
  private static final Date DAY_2 = Date.from(Instant.parse("2026-03-02T00:00:00Z"));
  private static final Date DAY_3 = Date.from(Instant.parse("2026-03-03T00:00:00Z"));

  private DarMetricsDAO dao;
  private User user;

  @BeforeEach
  void setUpDao() {
    dao = jdbi.onDemand(DarMetricsDAO.class);
    user = createUserWithInstitution();
  }

  @Test
  void singleDecision() {
    Integer dataset = createDataset();
    String dar = createDar(dataset);
    decide(dar, dataset, VoteType.FINAL, true, DAY_1);

    DarDatasetDecision pair = onlyPair();
    assertEquals(DecisionState.APPROVED, pair.state());
    assertEquals(DecidedVia.MANUAL, pair.decidedVia());
    assertEquals(DAY_1.toInstant(), pair.decisionDate());
    assertEquals(SUBMITTED.toInstant(), pair.submissionDate());
  }

  @Test
  void latestOfSeveralDecisionsWins() {
    Integer dataset = createDataset();
    String dar = createDar(dataset);
    decide(dar, dataset, VoteType.FINAL, false, DAY_1);
    reopenAndDecide(dar, dataset, VoteType.FINAL, true, DAY_2);
    reopenAndDecide(dar, dataset, VoteType.FINAL, false, DAY_3);

    DarDatasetDecision pair = onlyPair();
    assertEquals(DecisionState.DENIED, pair.state());
    assertEquals(DAY_3.toInstant(), pair.decisionDate());
  }

  @Test
  void reopenWithNoNewVoteIsPending() {
    Integer dataset = createDataset();
    String dar = createDar(dataset);
    decide(dar, dataset, VoteType.FINAL, true, DAY_1);
    reopen(dar, dataset, ElectionStatus.OPEN, DAY_2);

    DarDatasetDecision pair = onlyPair();
    assertEquals(DecisionState.PENDING, pair.state());
    assertNull(pair.decidedVia());
    assertNull(pair.decisionDate());
  }

  @Test
  void canceledElectionIsCanceled() {
    Integer dataset = createDataset();
    String dar = createDar(dataset);
    Integer electionId = election(dar, dataset, ElectionStatus.CANCELED, DAY_1);
    voteDAO.insertVote(user.getUserId(), electionId, VoteType.FINAL.getValue());

    assertEquals(DecisionState.CANCELED, onlyPair().state());
  }

  @Test
  void decidedThenReopenedThenCanceledIsCanceled() {
    Integer dataset = createDataset();
    String dar = createDar(dataset);
    decide(dar, dataset, VoteType.FINAL, true, DAY_1);
    reopen(dar, dataset, ElectionStatus.CANCELED, DAY_2);

    DarDatasetDecision pair = onlyPair();
    assertEquals(DecisionState.CANCELED, pair.state());
    assertNull(pair.decisionDate());
  }

  @Test
  void noElectionYet() {
    createDar(createDataset());

    assertEquals(DecisionState.NO_ELECTION, onlyPair().state());
    assertEquals(DecisionState.PENDING, onlyDar().state());
  }

  @Test
  void uncastVoteDoesNotWin() {
    Integer dataset = createDataset();
    String dar = createDar(dataset);
    Integer electionId = election(dar, dataset, ElectionStatus.CLOSED, DAY_1);
    castVote(electionId, VoteType.FINAL, true, DAY_2);
    // A second chair's vote, inserted later and never cast
    voteDAO.insertVote(user.getUserId(), electionId, VoteType.FINAL.getValue());

    assertEquals(DecisionState.APPROVED, onlyPair().state());
  }

  @Test
  void multiDatasetDarMixingRadarAndManual() {
    Integer radarDataset = createDataset();
    Integer manualDataset = createDataset();
    String dar = createDar(radarDataset, manualDataset);
    decide(dar, radarDataset, VoteType.RADAR_APPROVE, true, DAY_1);
    decide(dar, manualDataset, VoteType.FINAL, true, DAY_2);

    List<DarDatasetDecision> pairs = dao.findPairDecisions(FROM, TO, ALL_DACS, 10, 0);
    assertEquals(2, pairs.size());
    assertEquals(DecidedVia.RADAR, pairFor(pairs, radarDataset).decidedVia());
    assertEquals(DecidedVia.MANUAL, pairFor(pairs, manualDataset).decidedVia());

    DarDecision rollup = onlyDar();
    assertEquals(DecisionState.APPROVED, rollup.state());
    assertEquals(DecidedVia.MIXED, rollup.decidedVia());
    assertEquals(DAY_2.toInstant(), rollup.decisionDate());
    assertEquals(2, rollup.datasetCount());
  }

  @Test
  void allApprovedRollsUpApproved() {
    assertRollup(DecisionState.APPROVED, Outcome.APPROVE, Outcome.APPROVE);
  }

  @Test
  void allDeniedRollsUpDenied() {
    assertRollup(DecisionState.DENIED, Outcome.DENY, Outcome.DENY);
  }

  @Test
  void approvedAndDeniedRollsUpMixed() {
    assertRollup(DecisionState.MIXED, Outcome.APPROVE, Outcome.DENY);
  }

  @Test
  void onePendingPairHoldsTheDarPending() {
    assertRollup(DecisionState.PENDING, Outcome.APPROVE, Outcome.PENDING);
    assertNull(onlyDar().decidedVia());
    assertNull(onlyDar().decisionDate());
  }

  @Test
  void canceledPairDoesNotAffectTheOutcome() {
    assertRollup(DecisionState.APPROVED, Outcome.APPROVE, Outcome.CANCEL);
    assertEquals(DAY_1.toInstant(), onlyDar().decisionDate());
  }

  @Test
  void allCanceledRollsUpCanceled() {
    assertRollup(DecisionState.CANCELED, Outcome.CANCEL, Outcome.CANCEL);
    assertNull(onlyDar().decidedVia());
  }

  @Test
  void aSubmissionSavedAsOneDarPerDatasetRollsUpOnce() {
    Integer firstDataset = createDataset();
    Integer secondDataset = createDataset();
    String first = createDar(firstDataset);
    Integer collectionId = dataAccessRequestDAO.findByReferenceId(first).getCollectionId();
    String second = createDarIn(collectionId, DAY_1, secondDataset);
    decide(first, firstDataset, VoteType.FINAL, true, DAY_2);
    decide(second, secondDataset, VoteType.FINAL, false, DAY_3);

    DarDecision rollup = onlyDar();
    assertEquals(first, rollup.referenceId());
    assertEquals(SUBMITTED.toInstant(), rollup.submissionDate());
    assertEquals(2, rollup.datasetCount());
    assertEquals(DecisionState.MIXED, rollup.state());
    assertEquals(DAY_3.toInstant(), rollup.decisionDate());
  }

  @Test
  void undatedDecisionLeavesTheDarDateNull() {
    Integer dataset = createDataset();
    String dar = createDar(dataset);
    Integer electionId = election(dar, dataset, ElectionStatus.CLOSED, DAY_1);
    castVote(electionId, VoteType.FINAL, true, null);

    assertEquals(DecisionState.APPROVED, onlyDar().state());
    assertNull(onlyDar().decisionDate());
  }

  @ParameterizedTest
  @ValueSource(strings = {"Canceled", "canceled", "Archived", "ARCHIVED"})
  void canceledAndArchivedDarsAreExcluded(String status) {
    DataAccessRequestData data = new DataAccessRequestData();
    data.setStatus(status);
    createDar(data, SUBMITTED, createDataset());

    assertTrue(dao.findPairDecisions(FROM, TO, ALL_DACS, 10, 0).isEmpty());
    assertTrue(dao.countDarDecisions(FROM, TO, ALL_DACS, "month").isEmpty());
  }

  @Test
  void progressReportsAreExcluded() {
    Integer dataset = createDataset();
    String parent = createDar(dataset);
    Integer parentId = dataAccessRequestDAO.findByReferenceId(parent).getId();
    String child = UUID.randomUUID().toString();
    Integer collectionId = dataAccessRequestDAO.findByReferenceId(parent).getCollectionId();
    dataAccessRequestDAO.insertProgressReport(
        parentId, collectionId, child, user.getUserId(), new DataAccessRequestData(), "era");
    dataAccessRequestDAO.insertDARDatasetRelation(child, dataset);

    assertEquals(parent, onlyPair().referenceId());
  }

  @Test
  void rangeIsOnSubmissionDate() {
    createDar(new DataAccessRequestData(), SUBMITTED, createDataset());

    Instant submitted = SUBMITTED.toInstant();
    assertEquals(
        1, dao.findPairDecisions(submitted, submitted.plusSeconds(1), ALL_DACS, 10, 0).size());
    assertTrue(dao.findPairDecisions(submitted.plusSeconds(1), TO, ALL_DACS, 10, 0).isEmpty());
  }

  @Test
  void bucketsCountByStateAndVoteType() {
    Integer first = createDataset();
    Integer second = createDataset();
    String dar = createDar(first, second);
    decide(dar, first, VoteType.RADAR_APPROVE, true, DAY_1);

    List<DecisionBucketCount> buckets = dao.countPairDecisions(FROM, TO, ALL_DACS, "month");
    assertEquals(2, buckets.size());
    // submission_date has no zone, so buckets start on the server's local calendar
    Instant february = LocalDate.of(2026, 2, 1).atStartOfDay(ZoneId.systemDefault()).toInstant();
    assertTrue(buckets.stream().allMatch(b -> b.bucketStart().equals(february)));
    assertTrue(
        buckets.stream()
            .anyMatch(
                b ->
                    b.state() == DecisionState.APPROVED
                        && b.decidedVia() == DecidedVia.RADAR
                        && b.count() == 1));
    assertTrue(
        buckets.stream()
            .anyMatch(
                b ->
                    b.state() == DecisionState.NO_ELECTION
                        && b.decidedVia() == null
                        && b.count() == 1));
  }

  @Test
  void darBucketsCountEachDarOnce() {
    Integer first = createDataset();
    Integer second = createDataset();
    String mixed = createDar(first, second);
    decide(mixed, first, VoteType.FINAL, true, DAY_1);
    decide(mixed, second, VoteType.RADAR_APPROVE, false, DAY_2);
    createDar(createDataset());

    List<DecisionBucketCount> buckets = dao.countDarDecisions(FROM, TO, ALL_DACS, "quarter");

    assertEquals(2, buckets.size());
    assertEquals(2, buckets.stream().mapToLong(DecisionBucketCount::count).sum());
    assertTrue(
        buckets.stream()
            .anyMatch(
                b ->
                    b.state() == DecisionState.MIXED
                        && b.decidedVia() == DecidedVia.MIXED
                        && b.count() == 1));
    assertTrue(
        buckets.stream()
            .anyMatch(
                b ->
                    b.state() == DecisionState.PENDING
                        && b.decidedVia() == null
                        && b.count() == 1));
  }

  @Test
  void rowsArePaged() {
    createDar(createDataset());
    createDar(createDataset());

    assertEquals(1, dao.findDarDecisions(FROM, TO, ALL_DACS, 1, 0).size());
    assertEquals(1, dao.findDarDecisions(FROM, TO, ALL_DACS, 1, 1).size());
    assertTrue(dao.findDarDecisions(FROM, TO, ALL_DACS, 1, 2).isEmpty());
  }

  @Test
  void volumeReadsTheInstitutionRecordedAtSubmission() {
    String dar = createDar(createDataset());
    Integer recorded = user.getInstitutionId();
    dataAccessRequestDAO.updateSubmissionInstitution(dar, recorded);
    userDAO.updateInstitutionId(user.getUserId(), createUserWithInstitution().getInstitutionId());

    DarVolume row = onlyVolume();
    assertEquals(recorded, row.institutionId());
    assertEquals(InstitutionSource.RECORDED, row.institutionSource());
    assertEquals(institutionDAO.findInstitutionById(recorded).getName(), row.institutionName());
  }

  @Test
  void volumeFallsBackToTheCurrentInstitutionBeforeOneWasRecorded() {
    createDar(createDataset());

    DarVolume row = onlyVolume();
    assertEquals(user.getInstitutionId(), row.institutionId());
    assertEquals(InstitutionSource.CURRENT, row.institutionSource());
  }

  @Test
  void aRecordedNullInstitutionDoesNotFallBack() {
    String dar = createDar(createDataset());
    dataAccessRequestDAO.updateSubmissionInstitution(dar, null);

    DarVolume row = onlyVolume();
    assertNull(row.institutionId());
    assertNull(row.institutionName());
    assertEquals(InstitutionSource.RECORDED, row.institutionSource());
  }

  @Test
  void aRenamedInstitutionReportsItsCurrentName() {
    String dar = createDar(createDataset());
    dataAccessRequestDAO.updateSubmissionInstitution(dar, user.getInstitutionId());
    renameInstitution(user.getInstitutionId(), "Renamed Institute");

    assertEquals("Renamed Institute", onlyVolume().institutionName());
  }

  @Test
  void aDeletedInstitutionKeepsItsRecordedName() {
    String dar = createDar(createDataset());
    Integer institutionId = user.getInstitutionId();
    String name = institutionDAO.findInstitutionById(institutionId).getName();
    dataAccessRequestDAO.updateSubmissionInstitution(dar, institutionId);
    institutionDAO.deleteInstitutionById(institutionId);

    DarVolume row = onlyVolume();
    assertNull(row.institutionId());
    assertEquals(name, row.institutionName());
  }

  @Test
  void volumeCountsResearchersPerDarWithoutExternalCollaborators() {
    DataAccessRequestData data = new DataAccessRequestData();
    data.setLabCollaborators(List.of(collaborator(), collaborator()));
    data.setInternalCollaborators(List.of(collaborator()));
    data.setExternalCollaborators(List.of(collaborator(), collaborator(), collaborator()));
    createDar(data, SUBMITTED, createDataset(), createDataset());

    DarVolume row = onlyVolume();
    assertEquals(2, row.labStaffCount());
    assertEquals(1, row.internalCollaboratorCount());
    assertEquals(2, row.datasetCount());
  }

  @Test
  void aDarWithNoCollaboratorsOrDatasetsReportsZero() {
    createDar();

    DarVolume row = onlyVolume();
    assertEquals(0, row.labStaffCount());
    assertEquals(0, row.internalCollaboratorCount());
    assertEquals(0, row.datasetCount());
  }

  @Test
  void volumeCountsACollectionWithAProgressReportOnce() {
    Integer dataset = createDataset();
    String parent = createDar(dataset);
    var parentDar = dataAccessRequestDAO.findByReferenceId(parent);
    dataAccessRequestDAO.insertProgressReport(
        parentDar.getId(),
        parentDar.getCollectionId(),
        UUID.randomUUID().toString(),
        user.getUserId(),
        new DataAccessRequestData(),
        "era");

    assertEquals(parent, onlyVolume().referenceId());
  }

  @Test
  void volumeTotalsEachBucket() {
    createDar(createDataset());
    createDar(createDataset(), createDataset());
    createDarFor(createUserWithInstitution(), createDataset());

    List<VolumeBucketCount> buckets = dao.countDarVolume(FROM, TO, ALL_DACS, "quarter");
    assertEquals(1, buckets.size());
    assertEquals(3, buckets.getFirst().darCount());
    assertEquals(2, buckets.getFirst().researcherCount());
    assertEquals(2, buckets.getFirst().institutionCount());
    assertEquals(4, buckets.getFirst().datasetCount());
  }

  @Test
  void volumeTotalsCountADeletedInstitution() {
    String dar = createDar(createDataset());
    dataAccessRequestDAO.updateSubmissionInstitution(dar, user.getInstitutionId());
    institutionDAO.deleteInstitutionById(user.getInstitutionId());
    createDarFor(createUser(), createDataset());

    assertEquals(
        1, dao.countDarVolume(FROM, TO, ALL_DACS, "quarter").getFirst().institutionCount());
  }

  @ParameterizedTest
  @ValueSource(strings = {"Canceled", "archived"})
  void volumeExcludesCanceledAndArchivedDars(String status) {
    DataAccessRequestData data = new DataAccessRequestData();
    data.setStatus(status);
    createDar(data, SUBMITTED, createDataset());

    assertTrue(dao.findDarVolume(FROM, TO, ALL_DACS, 10, 0).isEmpty());
  }

  @Test
  void aResearcherWithNoInstitutionIsIncluded() {
    createDarFor(createUser(), createDataset());

    DarVolume row = onlyVolume();
    assertNull(row.institutionId());
    assertEquals(InstitutionSource.CURRENT, row.institutionSource());
  }

  @Test
  void volumeExcludesDrafts() {
    Integer collectionId =
        darCollectionDAO.insertDarCollection(
            "DAR-" + UUID.randomUUID(), user.getUserId(), SUBMITTED);
    dataAccessRequestDAO.insertDataAccessRequest(
        collectionId,
        UUID.randomUUID().toString(),
        user.getUserId(),
        SUBMITTED,
        null,
        SUBMITTED,
        new DataAccessRequestData(),
        "era");

    assertTrue(dao.findDarVolume(FROM, TO, ALL_DACS, 10, 0).isEmpty());
    assertTrue(dao.countDarVolume(FROM, TO, ALL_DACS, "quarter").isEmpty());
  }

  @Test
  void volumeCountsDarsPerInstitutionAndResearcher() {
    createDar(createDataset());
    createDar(createDataset());
    User other = createUserWithInstitution();
    createDarFor(other, createDataset());
    createDarFor(createUser(), createDataset());

    List<InstitutionDarCount> institutions = dao.countDarsByInstitution(FROM, TO, ALL_DACS);
    assertEquals(
        List.of(user.getInstitutionId(), other.getInstitutionId()),
        institutions.subList(0, 2).stream().map(InstitutionDarCount::institutionId).toList());
    assertEquals(2, institutions.getFirst().darCount());
    assertEquals(1, institutions.getFirst().researcherCount());
    // DARs with no institution form one group, listed after the named ones
    InstitutionDarCount none = institutions.getLast();
    assertEquals(3, institutions.size());
    assertNull(none.institutionId());
    assertNull(none.institutionName());
    assertEquals(1, none.darCount());

    List<ResearcherDarCount> researchers = dao.countDarsByResearcher(FROM, TO, ALL_DACS);
    assertEquals(3, researchers.size());
    assertEquals(user.getUserId(), researchers.getFirst().userId());
    assertEquals(2, researchers.getFirst().darCount());
  }

  @Test
  void aDeletedInstitutionKeepsItsOwnGroup() {
    String dar = createDar(createDataset());
    Integer institutionId = user.getInstitutionId();
    String name = institutionDAO.findInstitutionById(institutionId).getName();
    dataAccessRequestDAO.updateSubmissionInstitution(dar, institutionId);
    institutionDAO.deleteInstitutionById(institutionId);
    createDarFor(createUser(), createDataset());

    List<InstitutionDarCount> institutions = dao.countDarsByInstitution(FROM, TO, ALL_DACS);
    assertEquals(2, institutions.size());
    assertEquals(name, institutions.getFirst().institutionName());
    assertNull(institutions.getLast().institutionName());
  }

  @Test
  void aSubmissionSavedAsOneDarPerDatasetCountsOnce() {
    String first = createDar(createDataset());
    Integer collectionId = dataAccessRequestDAO.findByReferenceId(first).getCollectionId();
    createDarIn(collectionId, DAY_1, createDataset());

    DarVolume row = onlyVolume();
    assertEquals(first, row.referenceId());
    assertEquals(SUBMITTED.toInstant(), row.submissionDate());
    assertEquals(2, row.datasetCount());
    VolumeBucketCount bucket = dao.countDarVolume(FROM, TO, ALL_DACS, "quarter").getFirst();
    assertEquals(1, bucket.darCount());
    assertEquals(2, bucket.datasetCount());
    assertEquals(1, dao.countDarsByInstitution(FROM, TO, ALL_DACS).getFirst().darCount());
    assertEquals(1, dao.countDarsByResearcher(FROM, TO, ALL_DACS).getFirst().darCount());
  }

  @Test
  void turnaroundMeasuresManualAndRadarDecisions() {
    Integer manualDataset = createDataset();
    String manual = createDar(manualDataset);
    decide(manual, manualDataset, VoteType.FINAL, true, DAY_1);
    Integer radarDataset = createDataset();
    String radar = createDar(radarDataset);
    decide(radar, radarDataset, VoteType.RADAR_APPROVE, true, DAY_2);

    List<DarDatasetTurnaround> pairs = dao.findPairTurnaround(FROM, TO, ALL_DACS, 10, 0);
    assertEquals(2, pairs.size());
    DarDatasetTurnaround manualPair =
        pairs.stream().filter(p -> p.referenceId().equals(manual)).findFirst().orElseThrow();
    assertEquals(DecidedVia.MANUAL, manualPair.decidedVia());
    assertEquals(19.0, manualPair.elapsedDays());
    assertEquals(DAY_1.toInstant(), manualPair.decisionDate());
    DarDatasetTurnaround radarPair =
        pairs.stream().filter(p -> p.referenceId().equals(radar)).findFirst().orElseThrow();
    assertEquals(DecidedVia.RADAR, radarPair.decidedVia());
    assertEquals(20.0, radarPair.elapsedDays());
  }

  @Test
  void aDarTurnaroundRunsToItsLastPairDecision() {
    Integer first = createDataset();
    Integer second = createDataset();
    String dar = createDar(first, second);
    decide(dar, first, VoteType.FINAL, true, DAY_1);
    decide(dar, second, VoteType.FINAL, false, DAY_3);

    DarTurnaround row = onlyDarTurnaround();
    assertEquals(DAY_3.toInstant(), row.decisionDate());
    assertEquals(21.0, row.elapsedDays());
  }

  @Test
  void aReopenedAndRedecidedPairMeasuresToTheNewDecision() {
    Integer dataset = createDataset();
    String dar = createDar(dataset);
    decide(dar, dataset, VoteType.FINAL, false, DAY_1);
    reopenAndDecide(dar, dataset, VoteType.FINAL, true, DAY_3);

    List<DarDatasetTurnaround> pairs = dao.findPairTurnaround(FROM, TO, ALL_DACS, 10, 0);
    assertEquals(1, pairs.size());
    assertEquals(21.0, pairs.getFirst().elapsedDays());
  }

  @Test
  void undecidedPairsAndDarsHaveNoTurnaround() {
    Integer reopened = createDataset();
    String dar = createDar(reopened);
    decide(dar, reopened, VoteType.FINAL, true, DAY_1);
    reopen(dar, reopened, ElectionStatus.OPEN, DAY_2);
    Integer decided = createDataset();
    Integer pending = createDataset();
    String partlyDecided = createDar(decided, pending);
    decide(partlyDecided, decided, VoteType.FINAL, true, DAY_1);
    election(partlyDecided, pending, ElectionStatus.OPEN, DAY_1);

    List<DarDatasetTurnaround> pairs = dao.findPairTurnaround(FROM, TO, ALL_DACS, 10, 0);
    assertEquals(List.of(decided), pairs.stream().map(DarDatasetTurnaround::datasetId).toList());
    assertTrue(dao.findDarTurnaround(FROM, TO, ALL_DACS, 10, 0).isEmpty());
    assertTrue(dao.countDarTurnaround(FROM, TO, ALL_DACS, "quarter").isEmpty());
  }

  @Test
  void undatedDecisionsAreCountedButNotMeasured() {
    Integer dated = createDataset();
    decide(createDar(dated), dated, VoteType.FINAL, true, DAY_1);
    Integer undated = createDataset();
    String undatedDar = createDar(undated);
    castVote(
        election(undatedDar, undated, ElectionStatus.CLOSED, DAY_1), VoteType.FINAL, true, null);

    assertEquals(1, dao.findPairTurnaround(FROM, TO, ALL_DACS, 10, 0).size());
    assertEquals(1, dao.findDarTurnaround(FROM, TO, ALL_DACS, 10, 0).size());
    for (TurnaroundBucket bucket :
        List.of(
            dao.countPairTurnaround(FROM, TO, ALL_DACS, "quarter").getFirst(),
            dao.countDarTurnaround(FROM, TO, ALL_DACS, "quarter").getFirst())) {
      assertEquals(1, bucket.count());
      assertEquals(1, bucket.unmeasured());
      assertEquals(19.0, bucket.meanDays());
    }
  }

  @Test
  void aDecisionBeforeTheSubmissionDateIsCountedButNotMeasured() {
    Integer dataset = createDataset();
    Date beforeSubmission = Date.from(SUBMITTED.toInstant().minus(Duration.ofDays(1)));
    decide(createDar(dataset), dataset, VoteType.FINAL, true, beforeSubmission);

    assertTrue(dao.findPairTurnaround(FROM, TO, ALL_DACS, 10, 0).isEmpty());
    assertTrue(dao.findDarTurnaround(FROM, TO, ALL_DACS, 10, 0).isEmpty());
    for (TurnaroundBucket bucket :
        List.of(
            dao.countPairTurnaround(FROM, TO, ALL_DACS, "quarter").getFirst(),
            dao.countDarTurnaround(FROM, TO, ALL_DACS, "quarter").getFirst())) {
      assertEquals(0, bucket.count());
      assertEquals(1, bucket.unmeasured());
    }
  }

  @Test
  void aDarWithAnyUndatedPairIsCountedButNotMeasured() {
    Integer dated = createDataset();
    Integer undated = createDataset();
    String dar = createDar(dated, undated);
    decide(dar, dated, VoteType.FINAL, true, DAY_1);
    castVote(election(dar, undated, ElectionStatus.CLOSED, DAY_1), VoteType.FINAL, true, null);

    assertTrue(dao.findDarTurnaround(FROM, TO, ALL_DACS, 10, 0).isEmpty());
    TurnaroundBucket bucket = dao.countDarTurnaround(FROM, TO, ALL_DACS, "quarter").getFirst();
    assertEquals(0, bucket.count());
    assertEquals(1, bucket.unmeasured());
    assertNull(bucket.meanDays());
    assertEquals(1, dao.findPairTurnaround(FROM, TO, ALL_DACS, 10, 0).size());
  }

  @Test
  void theModeGroupsWholeDaysWhileMeanAndMedianKeepFractions() {
    for (Duration elapsed : List.of(Duration.ofHours(30), Duration.ofHours(42))) {
      Integer dataset = createDataset();
      Date on = Date.from(SUBMITTED.toInstant().plus(elapsed));
      decide(createDar(dataset), dataset, VoteType.FINAL, true, on);
    }

    TurnaroundBucket bucket = dao.countPairTurnaround(FROM, TO, ALL_DACS, "quarter").getFirst();
    assertEquals(1.5, bucket.meanDays());
    assertEquals(1.5, bucket.medianDays());
    assertEquals(1, bucket.modeDays());
  }

  @Test
  void turnaroundBucketsReportMeanMedianAndMode() {
    for (int days : List.of(1, 2, 2, 7)) {
      Integer dataset = createDataset();
      Date on = Date.from(SUBMITTED.toInstant().plus(Duration.ofDays(days)));
      decide(createDar(dataset), dataset, VoteType.FINAL, true, on);
    }

    for (TurnaroundBucket bucket :
        List.of(
            dao.countPairTurnaround(FROM, TO, ALL_DACS, "quarter").getFirst(),
            dao.countDarTurnaround(FROM, TO, ALL_DACS, "quarter").getFirst())) {
      assertEquals(4, bucket.count());
      assertEquals(0, bucket.unmeasured());
      assertEquals(3.0, bucket.meanDays());
      assertEquals(2.0, bucket.medianDays());
      assertEquals(2, bucket.modeDays());
    }
  }

  @Test
  void aTiedTurnaroundModeIsTheSmallest() {
    for (int days : List.of(3, 3, 1, 1)) {
      Integer dataset = createDataset();
      Date on = Date.from(SUBMITTED.toInstant().plus(Duration.ofDays(days)));
      decide(createDar(dataset), dataset, VoteType.FINAL, true, on);
    }

    assertEquals(1, dao.countPairTurnaround(FROM, TO, ALL_DACS, "quarter").getFirst().modeDays());
  }

  @Test
  void aDacScopeReadsOnlyThatDacsPairs() {
    Integer firstDac = createDac();
    Integer secondDac = createDac();
    Integer approved = createDataset(firstDac);
    Integer denied = createDataset(secondDac);
    String dar = createDar(approved, denied);
    decide(dar, approved, VoteType.FINAL, true, DAY_1);
    decide(dar, denied, VoteType.FINAL, false, DAY_3);

    List<DarDatasetDecision> pairs = dao.findPairDecisions(FROM, TO, List.of(firstDac), 10, 0);
    assertEquals(List.of(approved), pairs.stream().map(DarDatasetDecision::datasetId).toList());

    DarDecision first = dao.findDarDecisions(FROM, TO, List.of(firstDac), 10, 0).getFirst();
    assertEquals(DecisionState.APPROVED, first.state());
    assertEquals(DAY_1.toInstant(), first.decisionDate());
    assertEquals(1, first.datasetCount());
    DarDecision second = dao.findDarDecisions(FROM, TO, List.of(secondDac), 10, 0).getFirst();
    assertEquals(DecisionState.DENIED, second.state());
    assertEquals(DAY_3.toInstant(), second.decisionDate());
    assertEquals(DecisionState.MIXED, onlyDar().state());
    DarDecision both =
        dao.findDarDecisions(FROM, TO, List.of(firstDac, secondDac), 10, 0).getFirst();
    assertEquals(DecisionState.MIXED, both.state());
  }

  @Test
  void aDacScopedTurnaroundRunsToThatDacsLastDecision() {
    Integer dac = createDac();
    Integer early = createDataset(dac);
    Integer late = createDataset(createDac());
    String dar = createDar(early, late);
    decide(dar, early, VoteType.FINAL, true, DAY_1);
    decide(dar, late, VoteType.FINAL, true, DAY_3);

    DarTurnaround row = dao.findDarTurnaround(FROM, TO, List.of(dac), 10, 0).getFirst();
    assertEquals(DAY_1.toInstant(), row.decisionDate());
    assertEquals(1, dao.findPairTurnaround(FROM, TO, List.of(dac), 10, 0).size());
    TurnaroundBucket bucket = dao.countDarTurnaround(FROM, TO, List.of(dac), "quarter").getFirst();
    assertEquals(1, bucket.count());
    assertEquals(19, bucket.modeDays());
  }

  @Test
  void noDacsInScopeReadsNothing() {
    Integer dataset = createDataset(createDac());
    decide(createDar(dataset), dataset, VoteType.FINAL, true, DAY_1);

    assertTrue(dao.findPairDecisions(FROM, TO, List.of(), 10, 0).isEmpty());
    assertTrue(dao.countDarDecisions(FROM, TO, List.of(), "quarter").isEmpty());
    assertTrue(dao.findDarVolume(FROM, TO, List.of(), 10, 0).isEmpty());
    assertTrue(dao.findPairDecisions(FROM, TO, List.of(createDac()), 10, 0).isEmpty());
  }

  @Test
  void volumeScopedToADacCountsOnlyDarsRequestingItsDatasets() {
    Integer dac = createDac();
    Integer otherDac = createDac();
    String inScope = createDar(createDataset(dac), createDataset(otherDac), createDataset(dac));
    createDar(createDataset(otherDac));
    createDar();

    List<DarVolume> rows = dao.findDarVolume(FROM, TO, List.of(dac), 10, 0);
    assertEquals(List.of(inScope), rows.stream().map(DarVolume::referenceId).toList());
    assertEquals(2, rows.getFirst().datasetCount());
    VolumeBucketCount bucket = dao.countDarVolume(FROM, TO, List.of(dac), "quarter").getFirst();
    assertEquals(1, bucket.darCount());
    assertEquals(2, bucket.datasetCount());
    assertEquals(1, dao.countDarsByInstitution(FROM, TO, List.of(dac)).getFirst().darCount());
    assertEquals(1, dao.countDarsByResearcher(FROM, TO, List.of(dac)).getFirst().darCount());
    assertEquals(3, dao.findDarVolume(FROM, TO, ALL_DACS, 10, 0).size());
  }

  @Test
  void aDacScopedSubmissionSavedAsOneDarPerDatasetCountsOnceUnderItsEarliest() {
    Integer dac = createDac();
    String first = createDar(createDataset(createDac()));
    Integer collectionId = dataAccessRequestDAO.findByReferenceId(first).getCollectionId();
    createDarIn(collectionId, DAY_1, createDataset(dac));

    List<DarVolume> rows = dao.findDarVolume(FROM, TO, List.of(dac), 10, 0);
    assertEquals(List.of(first), rows.stream().map(DarVolume::referenceId).toList());
    assertEquals(1, rows.getFirst().datasetCount());
  }

  private DarVolume onlyVolume() {
    List<DarVolume> rows = dao.findDarVolume(FROM, TO, ALL_DACS, 10, 0);
    assertEquals(1, rows.size());
    return rows.getFirst();
  }

  private static Collaborator collaborator() {
    return new Collaborator(
        true, UUID.randomUUID() + "@example.org", null, "Name", null, null, null);
  }

  private void renameInstitution(Integer institutionId, String name) {
    jdbi.useHandle(
        h ->
            h.createUpdate(
                    "UPDATE institution SET institution_name = :name WHERE institution_id = :id")
                .bind("name", name)
                .bind("id", institutionId)
                .execute());
  }

  private String createDarFor(User submitter, Integer... datasetIds) {
    User saved = user;
    user = submitter;
    try {
      return createDar(datasetIds);
    } finally {
      user = saved;
    }
  }

  private enum Outcome {
    APPROVE,
    DENY,
    PENDING,
    CANCEL
  }

  private void assertRollup(DecisionState expected, Outcome first, Outcome second) {
    Integer firstDataset = createDataset();
    Integer secondDataset = createDataset();
    String dar = createDar(firstDataset, secondDataset);
    apply(dar, firstDataset, first);
    apply(dar, secondDataset, second);
    assertEquals(expected, onlyDar().state());
  }

  private void apply(String dar, Integer dataset, Outcome outcome) {
    switch (outcome) {
      case APPROVE -> decide(dar, dataset, VoteType.FINAL, true, DAY_1);
      case DENY -> decide(dar, dataset, VoteType.FINAL, false, DAY_1);
      case PENDING -> election(dar, dataset, ElectionStatus.OPEN, DAY_1);
      case CANCEL -> election(dar, dataset, ElectionStatus.CANCELED, DAY_1);
    }
  }

  private DarTurnaround onlyDarTurnaround() {
    List<DarTurnaround> rows = dao.findDarTurnaround(FROM, TO, ALL_DACS, 10, 0);
    assertEquals(1, rows.size());
    return rows.getFirst();
  }

  private DarDatasetDecision onlyPair() {
    List<DarDatasetDecision> pairs = dao.findPairDecisions(FROM, TO, ALL_DACS, 10, 0);
    assertEquals(1, pairs.size());
    return pairs.getFirst();
  }

  private DarDecision onlyDar() {
    List<DarDecision> dars = dao.findDarDecisions(FROM, TO, ALL_DACS, 10, 0);
    assertEquals(1, dars.size());
    return dars.getFirst();
  }

  private static DarDatasetDecision pairFor(List<DarDatasetDecision> pairs, Integer datasetId) {
    return pairs.stream().filter(p -> p.datasetId().equals(datasetId)).findFirst().orElseThrow();
  }

  private Integer createDac() {
    return dacDAO.createDac(
        "DAC " + UUID.randomUUID(), UUID.randomUUID().toString(), user.getUserId());
  }

  private Integer createDataset() {
    return createDataset(null);
  }

  private Integer createDataset(Integer dacId) {
    return datasetDAO.insertDataset(
        "Dataset " + UUID.randomUUID(),
        FIXED_TIMESTAMP,
        user.getUserId(),
        UUID.randomUUID().toString(),
        "{}",
        dacId);
  }

  private String createDar(Integer... datasetIds) {
    return createDar(new DataAccessRequestData(), SUBMITTED, datasetIds);
  }

  private String createDar(DataAccessRequestData data, Date submitted, Integer... datasetIds) {
    Integer collectionId =
        darCollectionDAO.insertDarCollection(
            "DAR-" + UUID.randomUUID(), user.getUserId(), submitted);
    return createDarIn(collectionId, data, submitted, datasetIds);
  }

  private String createDarIn(Integer collectionId, Date submitted, Integer... datasetIds) {
    return createDarIn(collectionId, new DataAccessRequestData(), submitted, datasetIds);
  }

  private String createDarIn(
      Integer collectionId, DataAccessRequestData data, Date submitted, Integer... datasetIds) {
    String referenceId = UUID.randomUUID().toString();
    dataAccessRequestDAO.insertDataAccessRequest(
        collectionId, referenceId, user.getUserId(), submitted, submitted, submitted, data, "era");
    for (Integer datasetId : datasetIds) {
      dataAccessRequestDAO.insertDARDatasetRelation(referenceId, datasetId);
    }
    return referenceId;
  }

  private Integer election(String dar, Integer dataset, ElectionStatus status, Date on) {
    return electionDAO.insertElection(
        ElectionType.DATA_ACCESS.getValue(), status.getValue(), on, dar, dataset);
  }

  private void decide(String dar, Integer dataset, VoteType type, boolean value, Date on) {
    castVote(election(dar, dataset, ElectionStatus.CLOSED, on), type, value, on);
  }

  private void reopen(String dar, Integer dataset, ElectionStatus status, Date on) {
    List<Integer> earlier =
        electionDAO.findElectionsByReferenceIdAndDatasetId(dar, dataset).stream()
            .map(e -> e.getElectionId())
            .toList();
    electionDAO.archiveElectionByIds(earlier, on);
    election(dar, dataset, status, on);
  }

  private void reopenAndDecide(String dar, Integer dataset, VoteType type, boolean value, Date on) {
    reopen(dar, dataset, ElectionStatus.CLOSED, on);
    Integer latest =
        electionDAO
            .findLastElectionByReferenceIdDatasetIdAndType(
                dar, dataset, ElectionType.DATA_ACCESS.getValue())
            .getElectionId();
    castVote(latest, type, value, on);
  }

  private void castVote(Integer electionId, VoteType type, boolean value, Date on) {
    Integer voteId = voteDAO.insertVote(user.getUserId(), electionId, type.getValue());
    updateVote(value, "", on, voteId, false, electionId, on == null ? DAY_1 : on, false);
  }
}
