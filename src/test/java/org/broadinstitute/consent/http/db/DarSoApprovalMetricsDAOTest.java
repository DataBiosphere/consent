package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.broadinstitute.consent.http.enumeration.DarKind;
import org.broadinstitute.consent.http.enumeration.SoApprovalStatus;
import org.broadinstitute.consent.http.models.CloseoutSupplement;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.SoApproval;
import org.broadinstitute.consent.http.models.SoApprovalBucket;
import org.broadinstitute.consent.http.models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DarSoApprovalMetricsDAOTest extends DAOTestHelper {

  private static final Instant FROM = Instant.parse("2000-01-01T00:00:00Z");
  private static final Instant TO = Instant.parse("2100-01-01T00:00:00Z");
  private static final Instant JUNE = Instant.parse("2026-06-01T12:00:00Z");
  private static final Instant BEFORE_SKIPS = Instant.parse("2026-05-19T12:00:00Z");
  private static final Instant BEFORE_CLOSEOUT_APPROVALS = Instant.parse("2025-06-04T12:00:00Z");

  private DarMetricsDAO dao;
  private User user;

  @BeforeEach
  void setUpDao() {
    dao = jdbi.onDemand(DarMetricsDAO.class);
    user = createUserWithInstitution();
  }

  @Test
  void anApprovedDarIsTimedFromSubmissionToApproval() {
    String dar = createDar(JUNE);
    requireSo(dar);
    approve(dar, JUNE.plus(Duration.ofHours(60)));

    SoApproval row = only();
    assertEquals(DarKind.ORIGINAL, row.kind());
    assertEquals(SoApprovalStatus.APPROVED, row.status());
    assertEquals(JUNE.plus(Duration.ofHours(60)), row.approvalDate());
    assertEquals(2.5, row.elapsedDays());
  }

  @Test
  void aDarWaitingOnItsSoIsPending() {
    requireSo(createDar(JUNE));

    SoApproval row = only();
    assertEquals(SoApprovalStatus.PENDING, row.status());
    assertNull(row.approvalDate());
    assertNull(row.elapsedDays());
  }

  @Test
  void aDarThatNeverRequiredAnSoIsSkipped() {
    createDar(JUNE);

    assertEquals(SoApprovalStatus.SKIPPED, only().status());
  }

  @Test
  void anExplicitFalseIsSkippedLikeNull() {
    String dar = createDar(JUNE);
    dataAccessRequestDAO.updateRequiresSOApproval(false, dar);

    assertEquals(SoApprovalStatus.SKIPPED, only().status());
  }

  @Test
  void aDarBeforeSkipsWereRecordedIsNotDetermined() {
    createDar(BEFORE_SKIPS);

    assertEquals(SoApprovalStatus.NOT_DETERMINED, only().status());
  }

  @Test
  void aPreAuthorizedProgressReportIsSkipped() {
    String parent = createDar(JUNE);
    requireSo(parent);
    approve(parent, JUNE);
    String report = createChild(parent, new DataAccessRequestData(), JUNE.plusSeconds(60));

    SoApproval row = rowFor(report);
    assertEquals(DarKind.PROGRESS_REPORT, row.kind());
    assertEquals(SoApprovalStatus.SKIPPED, row.status());
  }

  @Test
  void aProgressReportRequiringAnSoIsPending() {
    String parent = createDar(JUNE);
    String report = createChild(parent, new DataAccessRequestData(), JUNE.plusSeconds(60));
    requireSo(report);

    assertEquals(SoApprovalStatus.PENDING, rowFor(report).status());
  }

  @Test
  void aCloseoutGoesToAnSoAndIsNeverSkipped() {
    String pending = createChild(createDar(JUNE), closeout(), JUNE.plusSeconds(60));
    String approved = createChild(createDar(JUNE), closeout(), JUNE.plusSeconds(60));
    approve(approved, JUNE.plus(Duration.ofDays(1)));

    SoApproval pendingRow = rowFor(pending);
    assertEquals(DarKind.CLOSEOUT, pendingRow.kind());
    assertEquals(SoApprovalStatus.PENDING, pendingRow.status());
    assertEquals(SoApprovalStatus.APPROVED, rowFor(approved).status());
  }

  @Test
  void aCloseoutBeforeCloseoutApprovalsWereRecordedIsNotDetermined() {
    String closeout =
        createChild(createDar(BEFORE_CLOSEOUT_APPROVALS), closeout(), BEFORE_CLOSEOUT_APPROVALS);

    assertEquals(SoApprovalStatus.NOT_DETERMINED, rowFor(closeout).status());
  }

  @Test
  void anApprovalBeforeTheSubmissionDateIsCountedButNotMeasured() {
    String dar = createDar(JUNE);
    requireSo(dar);
    approve(dar, JUNE.minus(Duration.ofDays(1)));

    assertNull(only().elapsedDays());
    SoApprovalBucket bucket = dao.countSoApprovals(FROM, TO, "quarter").getFirst();
    assertEquals(1, bucket.count());
    assertEquals(1, bucket.unmeasured());
    assertNull(bucket.meanDays());
  }

  @Test
  void aSubmissionSavedAsOneDarPerDatasetCountsOnce() {
    Instant legacy = Instant.parse("2021-03-01T12:00:00Z");
    String first = createDar(legacy);
    Integer collectionId = dataAccessRequestDAO.findByReferenceId(first).getCollectionId();
    Date later = Date.from(legacy.plusSeconds(60));
    dataAccessRequestDAO.insertDataAccessRequest(
        collectionId,
        UUID.randomUUID().toString(),
        user.getUserId(),
        later,
        later,
        later,
        new DataAccessRequestData(),
        "era");

    SoApproval row = only();
    assertEquals(first, row.referenceId());
    assertEquals(legacy, row.submissionDate());
    assertEquals(1, dao.countSoApprovals(FROM, TO, "quarter").getFirst().count());
  }

  @ParameterizedTest
  @ValueSource(strings = {"Canceled", "archived"})
  void canceledAndArchivedDarsAreExcluded(String status) {
    DataAccessRequestData data = new DataAccessRequestData();
    data.setStatus(status);
    createDar(data, JUNE);

    assertTrue(dao.findSoApprovals(FROM, TO, 10, 0).isEmpty());
  }

  @Test
  void bucketsSummarizeApprovalTimeByKindAndStatus() {
    for (int days : List.of(1, 2, 2, 7)) {
      String dar = createDar(JUNE);
      requireSo(dar);
      approve(dar, JUNE.plus(Duration.ofDays(days)));
    }
    requireSo(createDar(JUNE));
    createDar(JUNE);

    List<SoApprovalBucket> buckets = dao.countSoApprovals(FROM, TO, "quarter");
    assertEquals(3, buckets.size());
    SoApprovalBucket approved = bucketFor(buckets, SoApprovalStatus.APPROVED);
    assertEquals(4, approved.count());
    assertEquals(0, approved.unmeasured());
    assertEquals(3.0, approved.meanDays());
    assertEquals(2.0, approved.medianDays());
    assertEquals(2, approved.modeDays());
    SoApprovalBucket pending = bucketFor(buckets, SoApprovalStatus.PENDING);
    assertEquals(1, pending.count());
    assertNull(pending.meanDays());
    assertEquals(1, bucketFor(buckets, SoApprovalStatus.SKIPPED).count());
  }

  @Test
  void theModeGroupsWholeDaysWhileMeanAndMedianKeepFractions() {
    for (Duration elapsed : List.of(Duration.ofHours(30), Duration.ofHours(42))) {
      String dar = createDar(JUNE);
      requireSo(dar);
      approve(dar, JUNE.plus(elapsed));
    }

    SoApprovalBucket bucket = dao.countSoApprovals(FROM, TO, "quarter").getFirst();
    assertEquals(1.5, bucket.meanDays());
    assertEquals(1.5, bucket.medianDays());
    assertEquals(1, bucket.modeDays());
  }

  @Test
  void theRangeIsOnSubmissionDateIncludingItsStart() {
    String dar = createDar(JUNE);
    requireSo(dar);
    approve(dar, JUNE.plus(Duration.ofDays(40)));

    assertEquals(1, dao.findSoApprovals(JUNE, JUNE.plusSeconds(1), 10, 0).size());
    assertTrue(dao.findSoApprovals(JUNE.minusSeconds(1), JUNE, 10, 0).isEmpty());
    Instant approvalMonth = JUNE.plus(Duration.ofDays(35));
    assertTrue(
        dao.findSoApprovals(approvalMonth, approvalMonth.plus(Duration.ofDays(10)), 10, 0)
            .isEmpty());
  }

  @Test
  void rowsArePaged() {
    createDar(JUNE);
    createDar(JUNE.plusSeconds(60));

    assertEquals(1, dao.findSoApprovals(FROM, TO, 1, 0).size());
    assertEquals(1, dao.findSoApprovals(FROM, TO, 1, 1).size());
    assertTrue(dao.findSoApprovals(FROM, TO, 1, 2).isEmpty());
  }

  private SoApproval only() {
    List<SoApproval> rows = dao.findSoApprovals(FROM, TO, 10, 0);
    assertEquals(1, rows.size());
    return rows.getFirst();
  }

  private SoApproval rowFor(String referenceId) {
    return dao.findSoApprovals(FROM, TO, 10, 0).stream()
        .filter(r -> r.referenceId().equals(referenceId))
        .findFirst()
        .orElseThrow();
  }

  private static SoApprovalBucket bucketFor(
      List<SoApprovalBucket> buckets, SoApprovalStatus status) {
    return buckets.stream()
        .filter(b -> b.kind() == DarKind.ORIGINAL && b.status() == status)
        .findFirst()
        .orElseThrow();
  }

  private CloseoutSupplement closeout() {
    return new CloseoutSupplement(List.of("Research complete"), null, user.getUserId());
  }

  private DataAccessRequestData closeoutData(CloseoutSupplement supplement) {
    DataAccessRequestData data = new DataAccessRequestData();
    data.setCloseoutSupplement(supplement);
    return data;
  }

  private String createDar(Instant submitted) {
    return createDar(new DataAccessRequestData(), submitted);
  }

  private String createDar(DataAccessRequestData data, Instant submitted) {
    Date on = Date.from(submitted);
    Integer collectionId =
        darCollectionDAO.insertDarCollection("DAR-" + UUID.randomUUID(), user.getUserId(), on);
    String referenceId = UUID.randomUUID().toString();
    dataAccessRequestDAO.insertDataAccessRequest(
        collectionId, referenceId, user.getUserId(), on, on, on, data, "era");
    return referenceId;
  }

  private String createChild(String parentReferenceId, CloseoutSupplement supplement, Instant on) {
    return createChild(parentReferenceId, closeoutData(supplement), on);
  }

  private String createChild(String parentReferenceId, DataAccessRequestData data, Instant on) {
    var parent = dataAccessRequestDAO.findByReferenceId(parentReferenceId);
    String referenceId = UUID.randomUUID().toString();
    dataAccessRequestDAO.insertProgressReport(
        parent.getId(), parent.getCollectionId(), referenceId, user.getUserId(), data, "era");
    setTimestamp("submission_date", referenceId, on);
    return referenceId;
  }

  private void requireSo(String referenceId) {
    dataAccessRequestDAO.updateRequiresSOApproval(true, referenceId);
  }

  private void approve(String referenceId, Instant on) {
    dataAccessRequestDAO.updateDarApprovalSO(user.getUserId(), referenceId);
    setTimestamp("approving_so_timestamp", referenceId, on);
  }

  private void setTimestamp(String column, String referenceId, Instant on) {
    jdbi.useHandle(
        h ->
            h.createUpdate(
                    "UPDATE data_access_request SET " + column + " = :on WHERE reference_id = :ref")
                .bind("on", Timestamp.from(on))
                .bind("ref", referenceId)
                .execute());
  }
}
