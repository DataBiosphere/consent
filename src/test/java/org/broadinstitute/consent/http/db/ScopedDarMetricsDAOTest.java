package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.broadinstitute.consent.http.enumeration.DecidedVia;
import org.broadinstitute.consent.http.enumeration.DecisionState;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.DecisionBucketCount;
import org.broadinstitute.consent.http.models.TurnaroundBucket;
import org.broadinstitute.consent.http.models.VolumeBucketCount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ScopedDarMetricsDAOTest extends DarLifecycleDAOTestHelper {

  private static final Instant FROM = Instant.parse("2000-01-01T00:00:00Z");
  private static final Instant TO = Instant.parse("2100-01-01T00:00:00Z");
  private static final Instant SUBMITTED = Instant.parse("2026-02-10T12:00:00Z");
  private static final Duration TERM = Duration.ofDays(365);

  private final Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
  private ScopedDarMetricsDAO dao;

  @BeforeEach
  void setUpDaos() {
    dao = jdbi.onDemand(ScopedDarMetricsDAO.class);
  }

  @Test
  void volumeCountsDarsRequestingADatasetInScopeAndOnlyThoseDatasets() {
    Integer first = createDataset();
    Integer second = createDataset();
    Integer other = createDataset();
    createDar(SUBMITTED, first, second, other);
    createDar(SUBMITTED, other);

    VolumeBucketCount bucket = onlyVolume(List.of(first, second));
    assertEquals(1, bucket.darCount());
    assertEquals(2, bucket.datasetCount());
  }

  /** The pages are open beyond the DAC roles, so nothing counts who asked or from where. */
  @Test
  void volumeCountsNoResearchersOrInstitutions() {
    Integer dataset = createDataset();
    createDar(SUBMITTED, dataset);

    VolumeBucketCount bucket = onlyVolume(List.of(dataset));
    assertNull(bucket.researcherCount());
    assertNull(bucket.institutionCount());
  }

  @Test
  void volumeCountsASubmissionSavedAsOneDarPerDatasetOnceUnderItsEarliestDarInScope() {
    Integer outside = createDataset();
    Integer first = createDataset();
    Integer second = createDataset();
    String dar = createDar(Instant.parse("2026-01-15T12:00:00Z"), outside);
    Instant inScope = Instant.parse("2026-03-15T12:00:00Z");
    createSiblingDar(dar, inScope, first);
    createSiblingDar(dar, Instant.parse("2026-05-15T12:00:00Z"), second);

    VolumeBucketCount bucket = onlyVolume(List.of(first, second));
    assertEquals(1, bucket.darCount());
    assertEquals(2, bucket.datasetCount());
    assertEquals(
        onlyVolume(List.of(first)).bucketStart(),
        bucket.bucketStart(),
        "Reported in the month of its earliest DAR requesting a dataset in scope");
  }

  @Test
  void volumeCountsOnlySubmittedOriginalDars() {
    Integer dataset = createDataset();
    String dar = createDar(SUBMITTED, dataset);
    createProgressReport(dar, SUBMITTED.plus(Duration.ofDays(30)), dataset);
    for (String status : List.of("Canceled", "Archived")) {
      createDar(withStatus(status), SUBMITTED, dataset);
    }
    String draft = createDar(SUBMITTED, dataset);
    jdbi.useHandle(
        h ->
            h.createUpdate(
                    "UPDATE data_access_request SET submission_date = NULL"
                        + " WHERE reference_id = :ref")
                .bind("ref", draft)
                .execute());

    VolumeBucketCount bucket = onlyVolume(List.of(dataset));
    assertEquals(1, bucket.darCount());
    assertEquals(1, bucket.datasetCount(), "The progress report's dataset is not a new request");
  }

  @Test
  void volumeIsBucketedOnTheSubmissionDateWithinTheRange() {
    Integer dataset = createDataset();
    createDar(Instant.parse("2026-01-15T12:00:00Z"), dataset);
    createDar(Instant.parse("2026-03-15T12:00:00Z"), dataset);
    createDar(Instant.parse("2026-03-16T12:00:00Z"), dataset);
    List<Integer> scope = List.of(dataset);

    assertEquals(
        List.of(1L, 2L),
        dao.countDarVolume(FROM, TO, scope, "month").stream()
            .map(VolumeBucketCount::darCount)
            .toList());
    Instant march = Instant.parse("2026-03-01T00:00:00Z");
    assertEquals(
        List.of(2L),
        dao.countDarVolume(march, TO, scope, "month").stream()
            .map(VolumeBucketCount::darCount)
            .toList());
    assertTrue(dao.countDarVolume(FROM, FROM.plus(TERM), scope, "month").isEmpty());
  }

  @Test
  void decisionsCountOnlyPairsOnDatasetsInScope() {
    Integer approved = createDataset();
    Integer denied = createDataset();
    String dar = createDar(SUBMITTED, approved, denied);
    approve(dar, approved, SUBMITTED.plus(Duration.ofDays(5)));
    decide(dar, denied, false, SUBMITTED.plus(Duration.ofDays(5)));

    List<DecisionBucketCount> buckets =
        dao.countPairDecisions(FROM, TO, List.of(approved), "month");
    assertEquals(1, buckets.size());
    assertEquals(DecisionState.APPROVED, buckets.getFirst().state());
    assertEquals(DecidedVia.MANUAL, buckets.getFirst().decidedVia());
    assertEquals(1, buckets.getFirst().count());
  }

  @Test
  void decisionsCarryTheStateOfEachPairsLatestElection() {
    Integer reopened = createDataset();
    Integer canceled = createDataset();
    Integer unopened = createDataset();
    Integer radar = createDataset();
    String dar = createDar(SUBMITTED, reopened, canceled, unopened, radar);
    approve(dar, reopened, SUBMITTED.plus(Duration.ofDays(5)));
    election(dar, reopened, ElectionStatus.OPEN);
    election(dar, canceled, ElectionStatus.CANCELED);
    decide(dar, radar, VoteType.RADAR_APPROVE, true, SUBMITTED.plus(Duration.ofDays(1)));

    assertEquals(DecisionState.PENDING, onlyDecision(reopened).state());
    assertNull(onlyDecision(reopened).decidedVia());
    assertEquals(DecisionState.CANCELED, onlyDecision(canceled).state());
    assertEquals(DecisionState.NO_ELECTION, onlyDecision(unopened).state());
    assertEquals(DecisionState.APPROVED, onlyDecision(radar).state());
    assertEquals(DecidedVia.RADAR, onlyDecision(radar).decidedVia());
  }

  @Test
  void decisionsAndTurnaroundLeaveOutProgressReportsAndCanceledOrArchivedDars() {
    Integer dataset = createDataset();
    String dar = createDar(SUBMITTED, dataset);
    approve(dar, dataset, SUBMITTED.plus(Duration.ofDays(5)));
    String report = createProgressReport(dar, SUBMITTED.plus(Duration.ofDays(300)), dataset);
    approve(report, dataset, SUBMITTED.plus(Duration.ofDays(305)));
    for (String status : List.of("Canceled", "Archived")) {
      String withdrawn = createDar(withStatus(status), SUBMITTED, dataset);
      approve(withdrawn, dataset, SUBMITTED.plus(Duration.ofDays(5)));
    }

    assertEquals(1, onlyDecision(dataset).count());
    assertEquals(1, onlyTurnaround(List.of(dataset)).count());
  }

  @Test
  void turnaroundTimesOnlyDecisionsOnDatasetsInScope() {
    Integer quick = createDataset();
    Integer slow = createDataset();
    String dar = createDar(SUBMITTED, quick, slow);
    approve(dar, quick, SUBMITTED.plus(Duration.ofDays(10)));
    decide(dar, slow, false, SUBMITTED.plus(Duration.ofDays(30)));

    TurnaroundBucket bucket = onlyTurnaround(List.of(quick));
    assertEquals(1, bucket.count());
    assertEquals(0, bucket.unmeasured());
    assertEquals(10.0, bucket.meanDays());
    assertEquals(10.0, bucket.medianDays());
    assertEquals(10, bucket.modeDays());
    assertEquals(20.0, onlyTurnaround(List.of(quick, slow)).meanDays());
  }

  @Test
  void turnaroundFollowsEachPairsLatestElection() {
    Integer reopened = createDataset();
    Integer redecided = createDataset();
    String dar = createDar(SUBMITTED, reopened, redecided);
    approve(dar, reopened, SUBMITTED.plus(Duration.ofDays(5)));
    election(dar, reopened, ElectionStatus.OPEN);
    decide(dar, redecided, false, SUBMITTED.plus(Duration.ofDays(5)));
    approve(dar, redecided, SUBMITTED.plus(Duration.ofDays(20)));

    assertTrue(
        dao.countPairTurnaround(FROM, TO, List.of(reopened), "month").isEmpty(),
        "A pair reopened with no new vote is undecided");
    TurnaroundBucket bucket = onlyTurnaround(List.of(redecided));
    assertEquals(1, bucket.count());
    assertEquals(20.0, bucket.meanDays());
  }

  @Test
  void anUndatedDecisionIsCountedButNotTimed() {
    Integer dataset = createDataset();
    Integer pending = createDataset();
    String dar = createDar(SUBMITTED, dataset, pending);
    approve(dar, dataset, null);
    election(dar, pending, ElectionStatus.OPEN);

    TurnaroundBucket bucket = onlyTurnaround(List.of(dataset, pending));
    assertEquals(0, bucket.count());
    assertEquals(1, bucket.unmeasured());
    assertNull(bucket.meanDays());
  }

  @ParameterizedTest
  @ValueSource(strings = {"day", "week", "month", "quarter"})
  void anEmptyScopeReadsNothing(String bucket) {
    Integer dataset = createDataset();
    Instant submitted = now.minus(Duration.ofDays(800));
    String dar = createDar(submitted, dataset);
    approve(dar, dataset, submitted.plus(Duration.ofDays(5)));
    Instant reported = now.minus(Duration.ofDays(500));
    approve(createProgressReport(dar, reported, dataset), dataset, reported);
    List<Integer> none = List.of();

    assertTrue(dao.countDarVolume(FROM, TO, none, bucket).isEmpty());
    assertTrue(dao.countPairDecisions(FROM, TO, none, bucket).isEmpty());
    assertTrue(dao.countPairTurnaround(FROM, TO, none, bucket).isEmpty());
    assertFalse(dao.countDarVolume(FROM, TO, List.of(dataset), bucket).isEmpty());
  }

  private static DataAccessRequestData withStatus(String status) {
    DataAccessRequestData data = new DataAccessRequestData();
    data.setStatus(status);
    return data;
  }

  private VolumeBucketCount onlyVolume(List<Integer> datasetIds) {
    List<VolumeBucketCount> buckets = dao.countDarVolume(FROM, TO, datasetIds, "month");
    assertEquals(1, buckets.size());
    return buckets.getFirst();
  }

  private DecisionBucketCount onlyDecision(Integer datasetId) {
    List<DecisionBucketCount> buckets =
        dao.countPairDecisions(FROM, TO, List.of(datasetId), "month");
    assertEquals(1, buckets.size());
    return buckets.getFirst();
  }

  private TurnaroundBucket onlyTurnaround(List<Integer> datasetIds) {
    List<TurnaroundBucket> buckets = dao.countPairTurnaround(FROM, TO, datasetIds, "month");
    assertEquals(1, buckets.size());
    return buckets.getFirst();
  }
}
