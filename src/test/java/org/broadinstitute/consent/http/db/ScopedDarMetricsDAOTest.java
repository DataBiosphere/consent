package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.broadinstitute.consent.http.enumeration.AccessEndReason;
import org.broadinstitute.consent.http.enumeration.DecidedVia;
import org.broadinstitute.consent.http.enumeration.DecisionState;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.DecisionBucketCount;
import org.broadinstitute.consent.http.models.ExpirationBucket;
import org.broadinstitute.consent.http.models.RenewalBucket;
import org.broadinstitute.consent.http.models.TurnaroundBucket;
import org.broadinstitute.consent.http.models.VolumeBucketCount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ScopedDarMetricsDAOTest extends DarLifecycleDAOTestHelper {

  private static final List<Integer> ALL_DACS = null;

  private static final Instant FROM = Instant.parse("2000-01-01T00:00:00Z");
  private static final Instant TO = Instant.parse("2100-01-01T00:00:00Z");
  private static final Instant SUBMITTED = Instant.parse("2026-02-10T12:00:00Z");
  private static final Duration TERM = Duration.ofDays(365);

  private final Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
  private ScopedDarMetricsDAO dao;
  private DarMetricsDAO adminDao;

  @BeforeEach
  void setUpDaos() {
    dao = jdbi.onDemand(ScopedDarMetricsDAO.class);
    adminDao = jdbi.onDemand(DarMetricsDAO.class);
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

  /** A study's or dataset's report is totals alone, so nothing counts who asked or from where. */
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

  @Test
  void aCollectionEndsWhenAccessToTheDatasetsInScopeDoes() {
    Integer lapsed = createDataset();
    Integer renewed = createDataset();
    Instant submitted = now.minus(Duration.ofDays(400));
    String dar = createDar(submitted, lapsed, renewed);
    approve(dar, lapsed, submitted.plus(Duration.ofDays(5)));
    approve(dar, renewed, submitted.plus(Duration.ofDays(5)));
    Instant reported = now.minus(Duration.ofDays(100));
    approve(createProgressReport(dar, reported, renewed), renewed, reported);

    assertTrue(expirations(List.of(renewed)).isEmpty());
    assertTrue(expirations(List.of(lapsed, renewed)).isEmpty());
    List<ExpirationBucket> ended = expirations(List.of(lapsed));
    assertEquals(1, ended.size());
    assertEquals(AccessEndReason.EXPIRED, ended.getFirst().reason());
    assertEquals(1, ended.getFirst().count());
  }

  @Test
  void aCloseoutBeforeTheTermRunsOutIsCountedAsClosedOut() {
    Integer dataset = createDataset();
    Instant submitted = now.minus(Duration.ofDays(100));
    String dar = createDar(submitted, dataset);
    approve(dar, dataset, submitted.plus(Duration.ofDays(5)));
    createCloseout(dar, now.minus(Duration.ofDays(10)));
    Integer stray = createDataset();
    approve(createDar(closeoutData(), submitted, stray), stray, submitted);

    List<ExpirationBucket> ended = expirations(List.of(dataset));
    assertEquals(1, ended.size());
    assertEquals(AccessEndReason.CLOSED_OUT, ended.getFirst().reason());
    assertTrue(
        expirations(List.of(stray)).isEmpty(), "A supplement on an original DAR closes nothing");
  }

  @Test
  void aCloseoutFiledAfterTheTermRanOutIsCountedAsExpired() {
    Integer dataset = createDataset();
    Instant submitted = now.minus(Duration.ofDays(500));
    String dar = createDar(submitted, dataset);
    approve(dar, dataset, submitted.plus(Duration.ofDays(5)));
    // The closeout carries a dataset and an approval, which must not start a new term
    String closeout = createCloseout(dar, now.minus(Duration.ofDays(10)));
    dataAccessRequestDAO.insertDARDatasetRelation(closeout, dataset);
    approve(closeout, dataset, now.minus(Duration.ofDays(9)));

    List<ExpirationBucket> ended = expirations(List.of(dataset));
    assertEquals(1, ended.size());
    assertEquals(AccessEndReason.EXPIRED, ended.getFirst().reason());
  }

  @Test
  void archivedDarsGrantNoAccessToExpire() {
    Integer dataset = createDataset();
    Instant submitted = now.minus(Duration.ofDays(400));
    approve(createDar(withStatus("Archived"), submitted, dataset), dataset, submitted);

    assertTrue(expirations(List.of(dataset)).isEmpty());
  }

  @Test
  void expirationsAreCountedInTheRangeAndOnlyOnceTheyHaveHappened() {
    Integer dataset = createDataset();
    Instant submitted = now.minus(Duration.ofDays(400));
    approve(createDar(submitted, dataset), dataset, submitted);
    Instant end = submitted.plus(TERM);
    List<Integer> scope = List.of(dataset);

    assertEquals(1, dao.countExpirations(end, end.plusSeconds(1), scope, now, "quarter").size());
    assertTrue(
        dao.countExpirations(end.minus(Duration.ofDays(1)), end, scope, now, "quarter").isEmpty());
    assertTrue(dao.countExpirations(FROM, TO, scope, end.minusSeconds(1), "quarter").isEmpty());
  }

  @Test
  void renewalsCountOnlyDatasetsInScope() {
    Integer inScope = createDataset();
    Integer outside = createDataset();
    Integer denied = createDataset();
    String dar = createDar(now.minus(Duration.ofDays(400)), inScope, outside, denied);
    Instant reported = now.minus(Duration.ofDays(100));
    String report = createProgressReport(dar, reported, inScope, outside, denied);
    approve(report, inScope, reported.plus(Duration.ofDays(1)));
    approve(report, outside, reported.plus(Duration.ofDays(1)));
    decide(report, denied, false, reported.plus(Duration.ofDays(1)));

    RenewalBucket bucket = onlyRenewal(List.of(inScope, denied));
    assertEquals(1, bucket.renewalCount());
    assertEquals(1, bucket.collectionCount());
    assertEquals(2, onlyRenewal(List.of(inScope, outside)).renewalCount());
    assertTrue(dao.countRenewals(FROM, TO, List.of(denied), "month").isEmpty());
  }

  @Test
  void originalDarsPendingReportsAndCloseoutsAreNotRenewals() {
    Integer dataset = createDataset();
    Instant submitted = now.minus(Duration.ofDays(400));
    String dar = createDar(submitted, dataset);
    approve(dar, dataset, submitted);
    String report = createProgressReport(dar, now.minus(Duration.ofDays(100)), dataset);
    election(report, dataset, ElectionStatus.OPEN);
    // A closeout carrying a dataset and an approval, so only its being a closeout rules it out
    String closeout = createCloseout(report, now.minus(Duration.ofDays(10)));
    dataAccessRequestDAO.insertDARDatasetRelation(closeout, dataset);
    approve(closeout, dataset, now.minus(Duration.ofDays(9)));

    assertTrue(dao.countRenewals(FROM, TO, List.of(dataset), "month").isEmpty());
  }

  @ParameterizedTest
  @ValueSource(strings = {"Canceled", "Archived"})
  void canceledAndArchivedProgressReportsAreNotRenewals(String status) {
    Integer dataset = createDataset();
    Instant submitted = now.minus(Duration.ofDays(400));
    String dar = createDar(submitted, dataset);
    approve(dar, dataset, submitted);
    Instant reported = now.minus(Duration.ofDays(100));
    approve(createProgressReport(dar, withStatus(status), reported, dataset), dataset, reported);

    assertTrue(dao.countRenewals(FROM, TO, List.of(dataset), "month").isEmpty());
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
    assertTrue(dao.countExpirations(FROM, TO, none, now, bucket).isEmpty());
    assertTrue(dao.countRenewals(FROM, TO, none, bucket).isEmpty());
    assertFalse(dao.countDarVolume(FROM, TO, List.of(dataset), bucket).isEmpty());
  }

  /**
   * The scoped queries restate the admin reports' rules rather than share their SQL, so this holds
   * the two together: scoped to every dataset, each scoped report must total what the admin report
   * does over every DAC.
   */
  @ParameterizedTest
  @ValueSource(strings = {"day", "week", "month", "quarter"})
  void scopedToEveryDatasetTheTotalsMatchTheAdminReports(String bucket) {
    List<Integer> everyDataset = createVariedHistory();

    // Every report is compared, so one run names each query that has drifted
    assertAll(
        () ->
            assertEquals(
                adminDao.countPairDecisions(FROM, TO, ALL_DACS, bucket),
                dao.countPairDecisions(FROM, TO, everyDataset, bucket),
                "decisions"),
        () ->
            assertEquals(
                adminDao.countPairTurnaround(FROM, TO, ALL_DACS, bucket),
                dao.countPairTurnaround(FROM, TO, everyDataset, bucket),
                "turnaround"),
        () ->
            assertEquals(
                adminDao.countExpirations(FROM, TO, ALL_DACS, now, bucket),
                dao.countExpirations(FROM, TO, everyDataset, now, bucket),
                "expirations"),
        () ->
            assertEquals(
                adminDao.countRenewals(FROM, TO, ALL_DACS, bucket),
                dao.countRenewals(FROM, TO, everyDataset, bucket),
                "renewals"),
        () ->
            assertEquals(
                adminDao.countDarVolume(FROM, TO, ALL_DACS, bucket).stream()
                    .map(
                        b ->
                            new VolumeBucketCount(
                                b.bucketStart(), b.darCount(), null, null, b.datasetCount()))
                    .toList(),
                dao.countDarVolume(FROM, TO, everyDataset, bucket),
                "volume"));
  }

  /** Collections in every state the reports tell apart, returning every dataset they request. */
  private List<Integer> createVariedHistory() {
    Integer expired = createDataset();
    Instant longAgo = now.minus(Duration.ofDays(800));
    approve(createDar(longAgo, expired), expired, longAgo.plus(Duration.ofDays(3)));

    Integer renewed = createDataset();
    Integer dropped = createDataset();
    String renewedDar = createDar(now.minus(Duration.ofDays(500)), renewed, dropped);
    approve(renewedDar, renewed, now.minus(Duration.ofDays(490)));
    decide(renewedDar, dropped, VoteType.RADAR_APPROVE, true, now.minus(Duration.ofDays(499)));
    Instant reported = now.minus(Duration.ofDays(200));
    approve(createProgressReport(renewedDar, reported, renewed), renewed, reported.plusSeconds(90));

    // The closeout carries a dataset and an approval, so only its being a closeout keeps it from
    // counting as a renewal
    Integer closed = createDataset();
    String closedDar = createDar(now.minus(Duration.ofDays(100)), closed);
    approve(closedDar, closed, null);
    String closeout = createCloseout(closedDar, now.minus(Duration.ofDays(10)));
    dataAccessRequestDAO.insertDARDatasetRelation(closeout, closed);
    approve(closeout, closed, now.minus(Duration.ofDays(9)));

    Integer denied = createDataset();
    Integer pending = createDataset();
    Integer canceled = createDataset();
    Integer unopened = createDataset();
    String mixedDar =
        createDar(now.minus(Duration.ofDays(40)), denied, pending, canceled, unopened);
    decide(mixedDar, denied, false, now.minus(Duration.ofDays(33)));
    election(mixedDar, pending, ElectionStatus.OPEN);
    election(mixedDar, canceled, ElectionStatus.CANCELED);

    Integer firstSibling = createDataset();
    Integer secondSibling = createDataset();
    String sibling = createDar(now.minus(Duration.ofDays(700)), firstSibling);
    approve(sibling, firstSibling, now.minus(Duration.ofDays(690)));
    approve(
        createSiblingDar(sibling, now.minus(Duration.ofDays(600)), secondSibling),
        secondSibling,
        now.minus(Duration.ofDays(580)));

    // Approved, then reopened with no new vote: pending and untimed, but still a grant
    Integer reopened = createDataset();
    String reopenedDar = createDar(now.minus(Duration.ofDays(450)), reopened);
    approve(reopenedDar, reopened, now.minus(Duration.ofDays(440)));
    election(reopenedDar, reopened, ElectionStatus.OPEN);

    // Closed out after the term ran out, by a closeout that also carries a dataset and an
    // approval: expired, since a closeout starts no new term
    Integer lateClosed = createDataset();
    Instant lateClosedOn = now.minus(Duration.ofDays(500));
    String lateClosedDar = createDar(lateClosedOn, lateClosed);
    approve(lateClosedDar, lateClosed, lateClosedOn.plus(Duration.ofDays(2)));
    String lateCloseout = createCloseout(lateClosedDar, now.minus(Duration.ofDays(12)));
    dataAccessRequestDAO.insertDARDatasetRelation(lateCloseout, lateClosed);
    approve(lateCloseout, lateClosed, now.minus(Duration.ofDays(11)));

    // A grant and its renewal, each approved amid votes and elections that decide nothing
    Integer noisy = createDataset();
    Instant noisyOn = now.minus(Duration.ofDays(460));
    String noisyDar = createDar(noisyOn, noisy);
    approveAmidNoise(noisyDar, noisy, noisyOn.plus(Duration.ofDays(4)));
    Instant noisyReportOn = now.minus(Duration.ofDays(380));
    approveAmidNoise(
        createProgressReport(noisyDar, noisyReportOn, noisy),
        noisy,
        noisyReportOn.plus(Duration.ofDays(4)));

    // A legacy submission with no collection
    Integer uncollected = createDataset();
    Instant uncollectedOn = now.minus(Duration.ofDays(650));
    approve(createDarWithoutCollection(uncollectedOn, uncollected), uncollected, uncollectedOn);

    List<Integer> datasets =
        new ArrayList<>(
            List.of(
                expired,
                renewed,
                dropped,
                closed,
                denied,
                pending,
                canceled,
                unopened,
                firstSibling,
                secondSibling,
                reopened,
                lateClosed,
                noisy,
                uncollected));

    // Canceled and archived submissions, each approved before its status changed
    for (String status : List.of("Canceled", "Archived")) {
      Integer withdrawn = createDataset();
      Instant withdrawnOn = now.minus(Duration.ofDays(420));
      approve(createDar(withStatus(status), withdrawnOn, withdrawn), withdrawn, withdrawnOn);

      Integer withdrawnRenewal = createDataset();
      Instant parentOn = now.minus(Duration.ofDays(820));
      String parent = createDar(parentOn, withdrawnRenewal);
      approve(parent, withdrawnRenewal, parentOn.plus(Duration.ofDays(2)));
      Instant reportOn = now.minus(Duration.ofDays(430));
      approve(
          createProgressReport(parent, withStatus(status), reportOn, withdrawnRenewal),
          withdrawnRenewal,
          reportOn.plus(Duration.ofDays(2)));

      datasets.add(withdrawn);
      datasets.add(withdrawnRenewal);
    }
    return datasets;
  }

  /**
   * Approves the pair by a final vote, then adds what every real election also holds and what must
   * decide nothing: a later member vote against, a final vote never cast, and a research-purpose
   * election closed later with a final vote against.
   */
  private void approveAmidNoise(String referenceId, Integer datasetId, Instant on) {
    Integer electionId = election(referenceId, datasetId, ElectionStatus.CLOSED);
    castVote(electionId, VoteType.FINAL, true, on);
    castVote(electionId, VoteType.DAC, false, on.plus(Duration.ofDays(1)));
    voteDAO.insertVote(user.getUserId(), electionId, VoteType.FINAL.getValue());
    Integer researchPurposeElection =
        electionDAO.insertElection(
            ElectionType.RP.getValue(),
            ElectionStatus.CLOSED.getValue(),
            new Date(),
            referenceId,
            datasetId);
    castVote(researchPurposeElection, VoteType.FINAL, false, on.plus(Duration.ofDays(2)));
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

  private RenewalBucket onlyRenewal(List<Integer> datasetIds) {
    List<RenewalBucket> buckets = dao.countRenewals(FROM, TO, datasetIds, "month");
    assertEquals(1, buckets.size());
    return buckets.getFirst();
  }

  private List<ExpirationBucket> expirations(List<Integer> datasetIds) {
    return dao.countExpirations(FROM, TO, datasetIds, now, "quarter");
  }
}
