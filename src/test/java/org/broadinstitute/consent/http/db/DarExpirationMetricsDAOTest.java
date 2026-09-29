package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.broadinstitute.consent.http.enumeration.AccessEndReason;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.CloseoutSupplement;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.ExpirationBucket;
import org.broadinstitute.consent.http.models.ExpiredCollection;
import org.broadinstitute.consent.http.models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DarExpirationMetricsDAOTest extends DAOTestHelper {

  private static final Instant FROM = Instant.parse("2000-01-01T00:00:00Z");
  private static final Instant TO = Instant.parse("2100-01-01T00:00:00Z");
  private static final Duration TERM = Duration.ofDays(365);

  private final Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
  private DarMetricsDAO dao;
  private User user;

  @BeforeEach
  void setUpDao() {
    dao = jdbi.onDemand(DarMetricsDAO.class);
    user = createUserWithInstitution();
  }

  @Test
  void accessExpires365DaysAfterTheApprovedSubmission() {
    Integer expired = createDataset();
    Instant submitted = now.minus(TERM).minus(Duration.ofHours(2));
    approve(createDar(submitted, expired), expired);
    Integer live = createDataset();
    approve(createDar(now.minus(TERM).plus(Duration.ofHours(2)), live), live);

    ExpiredCollection row = only();
    assertEquals(submitted.plus(TERM), row.accessEnd());
    assertEquals(AccessEndReason.EXPIRED, row.reason());
  }

  @Test
  void anApprovedProgressReportKeepsTheCollectionLive() {
    Integer dataset = createDataset();
    String parent = createDar(now.minus(Duration.ofDays(400)), dataset);
    approve(parent, dataset);
    approve(createProgressReport(parent, now.minus(Duration.ofDays(100)), dataset), dataset);

    assertTrue(dao.findExpirations(FROM, TO, 10, 0).isEmpty());
  }

  @Test
  void aPendingProgressReportDoesNotExtendAccess() {
    Integer dataset = createDataset();
    Instant submitted = now.minus(Duration.ofDays(400));
    String parent = createDar(submitted, dataset);
    approve(parent, dataset);
    String report = createProgressReport(parent, now.minus(Duration.ofDays(100)), dataset);
    election(report, dataset, ElectionStatus.OPEN);

    assertEquals(submitted.plus(TERM), only().accessEnd());
  }

  @Test
  void aProgressReportReopenedWithNoNewVoteDoesNotExtendAccess() {
    Integer dataset = createDataset();
    Instant submitted = now.minus(Duration.ofDays(400));
    String parent = createDar(submitted, dataset);
    approve(parent, dataset);
    String report = createProgressReport(parent, now.minus(Duration.ofDays(100)), dataset);
    approve(report, dataset);
    election(report, dataset, ElectionStatus.OPEN);

    assertEquals(submitted.plus(TERM), only().accessEnd());
  }

  @Test
  void aParentReopenedWithNoNewVoteHasNoAccessEnd() {
    Integer dataset = createDataset();
    String parent = createDar(now.minus(Duration.ofDays(400)), dataset);
    approve(parent, dataset);
    election(parent, dataset, ElectionStatus.OPEN);

    assertTrue(dao.findExpirations(FROM, TO, 10, 0).isEmpty());
  }

  @Test
  void aReopenedParentEndsWithItsApprovedProgressReport() {
    Integer dataset = createDataset();
    String parent = createDar(now.minus(Duration.ofDays(500)), dataset);
    approve(parent, dataset);
    election(parent, dataset, ElectionStatus.OPEN);
    Instant renewed = now.minus(Duration.ofDays(400));
    approve(createProgressReport(parent, renewed, dataset), dataset);

    assertEquals(renewed.plus(TERM), only().accessEnd());
  }

  @Test
  void aRenewalAfterTheRangeKeepsTheCollectionLive() {
    Integer dataset = createDataset();
    Instant submitted = now.minus(Duration.ofDays(400));
    String parent = createDar(submitted, dataset);
    approve(parent, dataset);
    Instant lapsed = submitted.plus(TERM);
    approve(createProgressReport(parent, now.minus(Duration.ofDays(10)), dataset), dataset);

    assertTrue(dao.findExpirations(FROM, lapsed.plusSeconds(1), 10, 0).isEmpty());
  }

  @Test
  void aCloseoutEndsAccessOnItsFilingDate() {
    Integer dataset = createDataset();
    String parent = createDar(now.minus(Duration.ofDays(100)), dataset);
    approve(parent, dataset);
    Instant filed = now.minus(Duration.ofDays(10));
    createCloseout(parent, filed);

    ExpiredCollection row = only();
    assertEquals(filed, row.accessEnd());
    assertEquals(AccessEndReason.CLOSED_OUT, row.reason());
  }

  @Test
  void aCollectionEndsOnceEveryDatasetsAccessHas() {
    Integer renewed = createDataset();
    Integer lapsed = createDataset();
    String parent = createDar(now.minus(Duration.ofDays(400)), renewed, lapsed);
    approve(parent, renewed);
    approve(parent, lapsed);
    approve(createProgressReport(parent, now.minus(Duration.ofDays(100)), renewed), renewed);

    assertTrue(dao.findExpirations(FROM, TO, 10, 0).isEmpty());
  }

  @Test
  void endedAccessMatchesTheStudyPage() {
    Integer expired = createDataset();
    approve(createDar(now.minus(Duration.ofDays(400)), expired), expired);
    Integer live = createDataset();
    approve(createDar(now.minus(Duration.ofDays(100)), live), live);
    Integer closed = createDataset();
    String closedDar = createDar(now.minus(Duration.ofDays(100)), closed);
    approve(closedDar, closed);
    createCloseout(closedDar, now.minus(Duration.ofDays(10)));

    List<Integer> ended =
        dao.findExpirations(FROM, TO, 10, 0).stream().map(ExpiredCollection::collectionId).toList();
    for (Integer dataset : List.of(expired, live, closed)) {
      var summary =
          dataAccessRequestDAO
              .findSummaryMetricApprovedDARsByDatasetIdIncludesExpired(dataset)
              .getFirst();
      Integer collectionId =
          dataAccessRequestDAO.findByReferenceId(summary.referenceId()).getCollectionId();
      assertEquals(summary.expired(), ended.contains(collectionId), "dataset " + dataset);
    }
  }

  @Test
  void theRangeIsOnTheAccessEndDate() {
    Integer dataset = createDataset();
    Instant submitted = now.minus(Duration.ofDays(400));
    approve(createDar(submitted, dataset), dataset);
    Instant end = submitted.plus(TERM);

    assertEquals(1, dao.findExpirations(end, end.plusSeconds(1), 10, 0).size());
    assertTrue(dao.findExpirations(end.minus(Duration.ofDays(1)), end, 10, 0).isEmpty());
  }

  @Test
  void archivedDarsGrantNoAccess() {
    Integer dataset = createDataset();
    DataAccessRequestData data = new DataAccessRequestData();
    data.setStatus("Archived");
    approve(createDar(data, now.minus(Duration.ofDays(400)), dataset), dataset);

    assertTrue(dao.findExpirations(FROM, TO, 10, 0).isEmpty());
  }

  @Test
  void bucketsCountCollectionsByReason() {
    for (int i = 0; i < 2; i++) {
      Integer dataset = createDataset();
      approve(createDar(now.minus(Duration.ofDays(400)), dataset), dataset);
    }
    Integer dataset = createDataset();
    String closed = createDar(now.minus(Duration.ofDays(100)), dataset);
    approve(closed, dataset);
    createCloseout(closed, now.minus(Duration.ofDays(10)));

    List<ExpirationBucket> buckets = dao.countExpirations(FROM, TO, "quarter");
    assertEquals(
        2,
        buckets.stream()
            .filter(b -> b.reason() == AccessEndReason.EXPIRED)
            .mapToLong(ExpirationBucket::count)
            .sum());
    assertEquals(
        1,
        buckets.stream()
            .filter(b -> b.reason() == AccessEndReason.CLOSED_OUT)
            .mapToLong(ExpirationBucket::count)
            .sum());
  }

  private ExpiredCollection only() {
    List<ExpiredCollection> rows = dao.findExpirations(FROM, TO, 10, 0);
    assertEquals(1, rows.size());
    return rows.getFirst();
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

  private String createDar(Instant submitted, Integer... datasetIds) {
    return createDar(new DataAccessRequestData(), submitted, datasetIds);
  }

  private String createDar(DataAccessRequestData data, Instant submitted, Integer... datasetIds) {
    Date on = Date.from(submitted);
    Integer collectionId =
        darCollectionDAO.insertDarCollection("DAR-" + UUID.randomUUID(), user.getUserId(), on);
    String referenceId = UUID.randomUUID().toString();
    dataAccessRequestDAO.insertDataAccessRequest(
        collectionId, referenceId, user.getUserId(), on, on, on, data, "era");
    for (Integer datasetId : datasetIds) {
      dataAccessRequestDAO.insertDARDatasetRelation(referenceId, datasetId);
    }
    return referenceId;
  }

  private String createProgressReport(String parentReferenceId, Instant on, Integer datasetId) {
    String referenceId = createChild(parentReferenceId, new DataAccessRequestData(), on);
    dataAccessRequestDAO.insertDARDatasetRelation(referenceId, datasetId);
    return referenceId;
  }

  private void createCloseout(String parentReferenceId, Instant on) {
    DataAccessRequestData data = new DataAccessRequestData();
    data.setCloseoutSupplement(
        new CloseoutSupplement(List.of("Research complete"), null, user.getUserId()));
    createChild(parentReferenceId, data, on);
  }

  private String createChild(String parentReferenceId, DataAccessRequestData data, Instant on) {
    var parent = dataAccessRequestDAO.findByReferenceId(parentReferenceId);
    String referenceId = UUID.randomUUID().toString();
    dataAccessRequestDAO.insertProgressReport(
        parent.getId(), parent.getCollectionId(), referenceId, user.getUserId(), data, "era");
    jdbi.useHandle(
        h ->
            h.createUpdate(
                    "UPDATE data_access_request SET submission_date = :on"
                        + " WHERE reference_id = :ref")
                .bind("on", Timestamp.from(on))
                .bind("ref", referenceId)
                .execute());
    return referenceId;
  }

  private Integer election(String referenceId, Integer datasetId, ElectionStatus status) {
    return electionDAO.insertElection(
        ElectionType.DATA_ACCESS.getValue(), status.getValue(), new Date(), referenceId, datasetId);
  }

  private void approve(String referenceId, Integer datasetId) {
    Integer electionId = election(referenceId, datasetId, ElectionStatus.CLOSED);
    Integer voteId = voteDAO.insertVote(user.getUserId(), electionId, VoteType.FINAL.getValue());
    Date on = new Date();
    updateVote(true, "", on, voteId, false, electionId, on, false);
  }
}
