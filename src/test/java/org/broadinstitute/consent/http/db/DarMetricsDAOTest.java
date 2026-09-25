package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.DarDatasetDecision;
import org.broadinstitute.consent.http.models.DarDecision;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.DecisionBucketCount;
import org.broadinstitute.consent.http.models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DarMetricsDAOTest extends DAOTestHelper {

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

    List<DarDatasetDecision> pairs = dao.findPairDecisions(FROM, TO, 10, 0);
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

    assertTrue(dao.findPairDecisions(FROM, TO, 10, 0).isEmpty());
    assertTrue(dao.countDarDecisions(FROM, TO, "month").isEmpty());
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
    assertEquals(1, dao.findPairDecisions(submitted, submitted.plusSeconds(1), 10, 0).size());
    assertTrue(dao.findPairDecisions(submitted.plusSeconds(1), TO, 10, 0).isEmpty());
  }

  @Test
  void bucketsCountByStateAndVoteType() {
    Integer first = createDataset();
    Integer second = createDataset();
    String dar = createDar(first, second);
    decide(dar, first, VoteType.RADAR_APPROVE, true, DAY_1);

    List<DecisionBucketCount> buckets = dao.countPairDecisions(FROM, TO, "month");
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

    List<DecisionBucketCount> buckets = dao.countDarDecisions(FROM, TO, "quarter");

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

    assertEquals(1, dao.findDarDecisions(FROM, TO, 1, 0).size());
    assertEquals(1, dao.findDarDecisions(FROM, TO, 1, 1).size());
    assertTrue(dao.findDarDecisions(FROM, TO, 1, 2).isEmpty());
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

  private DarDatasetDecision onlyPair() {
    List<DarDatasetDecision> pairs = dao.findPairDecisions(FROM, TO, 10, 0);
    assertEquals(1, pairs.size());
    return pairs.getFirst();
  }

  private DarDecision onlyDar() {
    List<DarDecision> dars = dao.findDarDecisions(FROM, TO, 10, 0);
    assertEquals(1, dars.size());
    return dars.getFirst();
  }

  private static DarDatasetDecision pairFor(List<DarDatasetDecision> pairs, Integer datasetId) {
    return pairs.stream().filter(p -> p.datasetId().equals(datasetId)).findFirst().orElseThrow();
  }

  private Integer createDataset() {
    return datasetDAO.insertDataset(
        "Dataset " + UUID.randomUUID(),
        FIXED_TIMESTAMP,
        user.getUserId(),
        UUID.randomUUID().toString(),
        "{}",
        null);
  }

  private String createDar(Integer... datasetIds) {
    return createDar(new DataAccessRequestData(), SUBMITTED, datasetIds);
  }

  private String createDar(DataAccessRequestData data, Date submitted, Integer... datasetIds) {
    Integer collectionId =
        darCollectionDAO.insertDarCollection(
            "DAR-" + UUID.randomUUID(), user.getUserId(), submitted);
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
