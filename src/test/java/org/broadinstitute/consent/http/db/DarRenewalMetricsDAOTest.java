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
import org.broadinstitute.consent.http.enumeration.DecidedVia;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.CloseoutSupplement;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.Renewal;
import org.broadinstitute.consent.http.models.RenewalBucket;
import org.broadinstitute.consent.http.models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DarRenewalMetricsDAOTest extends DAOTestHelper {

  private static final Instant FROM = Instant.parse("2000-01-01T00:00:00Z");
  private static final Instant TO = Instant.parse("2100-01-01T00:00:00Z");
  private static final Instant SUBMITTED = Instant.parse("2025-01-10T00:00:00Z");
  private static final Instant RENEWED = Instant.parse("2026-01-15T00:00:00Z");
  private static final Date VOTED = Date.from(Instant.parse("2026-02-01T00:00:00Z"));

  private DarMetricsDAO dao;
  private User user;

  @BeforeEach
  void setUpDao() {
    dao = jdbi.onDemand(DarMetricsDAO.class);
    user = createUserWithInstitution();
  }

  @Test
  void anApprovedProgressReportIsARenewal() {
    Integer dataset = createDataset();
    String parent = createDar(dataset);
    approve(parent, dataset);
    String report = createProgressReport(parent, RENEWED, dataset);
    approve(report, dataset);

    Renewal row = only();
    assertEquals(report, row.referenceId());
    assertEquals(
        dataAccessRequestDAO.findByReferenceId(parent).getCollectionId(), row.collectionId());
    assertEquals(dataset, row.datasetId());
    assertEquals(RENEWED, row.submissionDate());
    assertEquals(DecidedVia.MANUAL, row.decidedVia());
    assertEquals(VOTED.toInstant(), row.approvalDate());
  }

  @Test
  void originalDarsAreNotRenewals() {
    Integer dataset = createDataset();
    approve(createDar(dataset), dataset);

    assertTrue(dao.findRenewals(FROM, TO, 10, 0).isEmpty());
  }

  @Test
  void pendingAndDeniedProgressReportsAreNotRenewals() {
    Integer pending = createDataset();
    String pendingParent = createDar(pending);
    approve(pendingParent, pending);
    election(createProgressReport(pendingParent, RENEWED, pending), pending, ElectionStatus.OPEN);
    Integer denied = createDataset();
    String deniedParent = createDar(denied);
    approve(deniedParent, denied);
    decide(createProgressReport(deniedParent, RENEWED, denied), denied, VoteType.FINAL, false);

    assertTrue(dao.findRenewals(FROM, TO, 10, 0).isEmpty());
  }

  @Test
  void aCloseoutIsNotARenewal() {
    Integer dataset = createDataset();
    String parent = createDar(dataset);
    approve(parent, dataset);
    DataAccessRequestData data = new DataAccessRequestData();
    data.setCloseoutSupplement(
        new CloseoutSupplement(List.of("Research complete"), null, user.getUserId()));
    String closeout = createChild(parent, data, RENEWED);
    dataAccessRequestDAO.insertDARDatasetRelation(closeout, dataset);
    approve(closeout, dataset);

    assertTrue(dao.findRenewals(FROM, TO, 10, 0).isEmpty());
  }

  @Test
  void aProgressReportCoveringOneOfTwoDatasetsCountsOneRenewal() {
    Integer renewed = createDataset();
    Integer lapsed = createDataset();
    String parent = createDar(renewed, lapsed);
    approve(parent, renewed);
    approve(parent, lapsed);
    approve(createProgressReport(parent, RENEWED, renewed), renewed);

    assertEquals(renewed, only().datasetId());
  }

  @Test
  void theLatestElectionDecides() {
    Integer revoked = createDataset();
    String revokedParent = createDar(revoked);
    approve(revokedParent, revoked);
    String revokedReport = createProgressReport(revokedParent, RENEWED, revoked);
    approve(revokedReport, revoked);
    decide(revokedReport, revoked, VoteType.FINAL, false);
    Integer granted = createDataset();
    String grantedParent = createDar(granted);
    approve(grantedParent, granted);
    String grantedReport = createProgressReport(grantedParent, RENEWED, granted);
    decide(grantedReport, granted, VoteType.FINAL, false);
    approve(grantedReport, granted);

    assertEquals(grantedReport, only().referenceId());
  }

  @Test
  void aReopenWithNoNewVoteIsNotARenewal() {
    Integer dataset = createDataset();
    String parent = createDar(dataset);
    approve(parent, dataset);
    String report = createProgressReport(parent, RENEWED, dataset);
    approve(report, dataset);
    election(report, dataset, ElectionStatus.OPEN);

    assertTrue(dao.findRenewals(FROM, TO, 10, 0).isEmpty());
    assertTrue(dao.countRenewals(FROM, TO, "month").isEmpty());
  }

  @Test
  void theLastVoteCastInTheElectionDecides() {
    Integer dataset = createDataset();
    String parent = createDar(dataset);
    approve(parent, dataset);
    String report = createProgressReport(parent, RENEWED, dataset);
    Integer electionId = election(report, dataset, ElectionStatus.CLOSED);
    castVote(electionId, VoteType.FINAL, true, VOTED);
    castVote(electionId, VoteType.FINAL, false, Date.from(VOTED.toInstant().plusSeconds(60)));

    assertTrue(dao.findRenewals(FROM, TO, 10, 0).isEmpty());
  }

  @Test
  void aRadarApprovalIsARenewalDecidedByRadar() {
    Integer dataset = createDataset();
    String parent = createDar(dataset);
    approve(parent, dataset);
    decide(createProgressReport(parent, RENEWED, dataset), dataset, VoteType.RADAR_APPROVE, true);

    assertEquals(DecidedVia.RADAR, only().decidedVia());
  }

  @Test
  void anUndatedApprovalIsARenewalWithNoApprovalDate() {
    Integer dataset = createDataset();
    String parent = createDar(dataset);
    approve(parent, dataset);
    String report = createProgressReport(parent, RENEWED, dataset);
    castVote(election(report, dataset, ElectionStatus.CLOSED), VoteType.FINAL, true, null);

    Renewal row = only();
    assertEquals(RENEWED, row.submissionDate());
    assertNull(row.approvalDate());
  }

  @ParameterizedTest
  @ValueSource(strings = {"Canceled", "archived"})
  void canceledAndArchivedProgressReportsAreExcluded(String status) {
    Integer dataset = createDataset();
    String parent = createDar(dataset);
    approve(parent, dataset);
    DataAccessRequestData data = new DataAccessRequestData();
    data.setStatus(status);
    String report = createChild(parent, data, RENEWED);
    dataAccessRequestDAO.insertDARDatasetRelation(report, dataset);
    approve(report, dataset);

    assertTrue(dao.findRenewals(FROM, TO, 10, 0).isEmpty());
  }

  @Test
  void theRangeIsOnTheProgressReportsSubmissionDate() {
    Integer dataset = createDataset();
    String parent = createDar(dataset);
    approve(parent, dataset);
    approve(createProgressReport(parent, RENEWED, dataset), dataset);

    assertEquals(1, dao.findRenewals(RENEWED, RENEWED.plusSeconds(1), 10, 0).size());
    assertTrue(dao.findRenewals(SUBMITTED, RENEWED, 10, 0).isEmpty());
  }

  @Test
  void bucketsCountRenewalsAndTheCollectionsTheyRenewed() {
    Integer first = createDataset();
    Integer second = createDataset();
    String twoDatasets = createDar(first, second);
    approve(twoDatasets, first);
    approve(twoDatasets, second);
    String bothRenewed = createProgressReport(twoDatasets, RENEWED, first);
    dataAccessRequestDAO.insertDARDatasetRelation(bothRenewed, second);
    approve(bothRenewed, first);
    approve(bothRenewed, second);
    Integer third = createDataset();
    String oneDataset = createDar(third);
    approve(oneDataset, third);
    approve(createProgressReport(oneDataset, RENEWED.plus(Duration.ofDays(3)), third), third);

    List<RenewalBucket> buckets = dao.countRenewals(FROM, TO, "month");
    assertEquals(1, buckets.size());
    assertEquals(3, buckets.getFirst().renewalCount());
    assertEquals(2, buckets.getFirst().collectionCount());
  }

  private Renewal only() {
    List<Renewal> rows = dao.findRenewals(FROM, TO, 10, 0);
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

  private String createDar(Integer... datasetIds) {
    Date on = Date.from(SUBMITTED);
    Integer collectionId =
        darCollectionDAO.insertDarCollection("DAR-" + UUID.randomUUID(), user.getUserId(), on);
    String referenceId = UUID.randomUUID().toString();
    dataAccessRequestDAO.insertDataAccessRequest(
        collectionId,
        referenceId,
        user.getUserId(),
        on,
        on,
        on,
        new DataAccessRequestData(),
        "era");
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
        ElectionType.DATA_ACCESS.getValue(), status.getValue(), VOTED, referenceId, datasetId);
  }

  private void approve(String referenceId, Integer datasetId) {
    decide(referenceId, datasetId, VoteType.FINAL, true);
  }

  private void decide(String referenceId, Integer datasetId, VoteType type, boolean value) {
    castVote(election(referenceId, datasetId, ElectionStatus.CLOSED), type, value, VOTED);
  }

  private void castVote(Integer electionId, VoteType type, boolean value, Date on) {
    Integer voteId = voteDAO.insertVote(user.getUserId(), electionId, type.getValue());
    updateVote(value, "", on, voteId, false, electionId, on == null ? VOTED : on, false);
  }
}
