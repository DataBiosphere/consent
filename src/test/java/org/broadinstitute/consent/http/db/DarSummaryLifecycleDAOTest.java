package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.models.DarMetricsSummary;
import org.junit.jupiter.api.Test;

/** The lifecycle dates the study and dataset pages show on each granted DAR. */
class DarSummaryLifecycleDAOTest extends DarLifecycleDAOTestHelper {

  private static final Duration TERM = Duration.ofDays(365);

  private final Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);

  @Test
  void theApprovalDateIsWhenTheDacDecidedNotWhenTheDarWasSubmitted() {
    Integer studyId = createStudy();
    Integer dataset = createStudyDataset(studyId);
    Instant submitted = now.minus(Duration.ofDays(30));
    Instant approved = now.minus(Duration.ofDays(20));
    approve(createDar(submitted, dataset), dataset, approved);

    for (DarMetricsSummary summary : List.of(onlyForDataset(dataset), onlyForStudy(studyId))) {
      assertEquals(Timestamp.from(submitted), summary.submissionDate());
      assertEquals(Timestamp.from(approved), summary.approvalDate());
      assertTrue(summary.progressReportApprovalDates().isEmpty());
      assertNull(summary.closeoutDate());
    }
  }

  @Test
  void anUndatedApprovalSendsNoApprovalDate() {
    Integer dataset = createDataset();
    approve(createDar(now.minus(Duration.ofDays(30)), dataset), dataset, null);

    assertNull(onlyForDataset(dataset).approvalDate());
  }

  /**
   * An undated approval predates recorded decision dates, so it may be the first: the earliest
   * dated one must not be reported in its place.
   */
  @Test
  void anUndatedApprovalAmongDatedOnesLeavesTheFirstApprovalUnknown() {
    Integer studyId = createStudy();
    Integer undated = createStudyDataset(studyId);
    Integer dated = createStudyDataset(studyId);
    String dar = createDar(now.minus(Duration.ofDays(400)), undated, dated);
    approve(dar, undated, null);
    Instant approved = now.minus(Duration.ofDays(390));
    approve(dar, dated, approved);
    String report = createProgressReport(dar, now.minus(Duration.ofDays(100)), undated, dated);
    approve(report, undated, null);
    approve(report, dated, now.minus(Duration.ofDays(90)));

    DarMetricsSummary study = onlyForStudy(studyId);
    assertNull(study.approvalDate());
    assertTrue(study.progressReportApprovalDates().isEmpty());
    // Each dataset still reports what is known about its own approvals
    assertEquals(Timestamp.from(approved), onlyForDataset(dated).approvalDate());
    assertNull(onlyForDataset(undated).approvalDate());
  }

  @Test
  void aSubmissionSavedAsOneDarPerDatasetHasNoApprovalDateIfOneApprovalIsUndated() {
    Integer studyId = createStudy();
    Integer undated = createStudyDataset(studyId);
    Integer dated = createStudyDataset(studyId);
    Instant submitted = now.minus(Duration.ofDays(30));
    String dar = createDar(submitted, undated);
    approve(dar, undated, null);
    approve(createSiblingDar(dar, submitted, dated), dated, now.minus(Duration.ofDays(20)));

    assertNull(onlyForStudy(studyId).approvalDate());
  }

  @Test
  void aReopenedAndRedecidedDarIsApprovedOnItsLatestDecision() {
    Integer dataset = createDataset();
    String dar = createDar(now.minus(Duration.ofDays(30)), dataset);
    decide(dar, dataset, false, now.minus(Duration.ofDays(20)));
    Instant approved = now.minus(Duration.ofDays(10));
    approve(dar, dataset, approved);

    assertEquals(Timestamp.from(approved), onlyForDataset(dataset).approvalDate());
  }

  @Test
  void aStudyDarIsApprovedWhenItsFirstDatasetIs() {
    Integer studyId = createStudy();
    Integer first = createStudyDataset(studyId);
    Integer second = createStudyDataset(studyId);
    String dar = createDar(now.minus(Duration.ofDays(30)), first, second);
    Instant firstApproved = now.minus(Duration.ofDays(20));
    Instant secondApproved = now.minus(Duration.ofDays(10));
    approve(dar, second, secondApproved);
    approve(dar, first, firstApproved);

    assertEquals(Timestamp.from(firstApproved), onlyForStudy(studyId).approvalDate());
    assertEquals(Timestamp.from(secondApproved), onlyForDataset(second).approvalDate());
  }

  @Test
  void aSubmissionSavedAsOneDarPerDatasetIsApprovedWhenItsFirstDarIs() {
    Integer studyId = createStudy();
    Integer first = createStudyDataset(studyId);
    Integer second = createStudyDataset(studyId);
    Instant submitted = now.minus(Duration.ofDays(30));
    String dar = createDar(submitted, first);
    String sibling = createSiblingDar(dar, submitted, second);
    Instant firstApproved = now.minus(Duration.ofDays(20));
    approve(sibling, second, now.minus(Duration.ofDays(10)));
    approve(dar, first, firstApproved);

    DarMetricsSummary summary = onlyForStudy(studyId);
    assertEquals(Timestamp.from(firstApproved), summary.approvalDate());
    assertTrue(summary.progressReportApprovalDates().isEmpty());
  }

  @Test
  void eachApprovedProgressReportAddsARenewalDateOldestFirst() {
    Integer studyId = createStudy();
    Integer dataset = createStudyDataset(studyId);
    String dar = createDar(now.minus(Duration.ofDays(900)), dataset);
    Instant approved = now.minus(Duration.ofDays(890));
    approve(dar, dataset, approved);
    String firstReport = createProgressReport(dar, now.minus(Duration.ofDays(600)), dataset);
    String secondReport =
        createProgressReport(firstReport, now.minus(Duration.ofDays(300)), dataset);
    Instant firstRenewed = now.minus(Duration.ofDays(590));
    Instant secondRenewed = now.minus(Duration.ofDays(290));
    // Decided out of order, so the list is ordered by date and not by when the rows were written
    approve(secondReport, dataset, secondRenewed);
    approve(firstReport, dataset, firstRenewed);

    for (DarMetricsSummary summary : List.of(onlyForDataset(dataset), onlyForStudy(studyId))) {
      assertEquals(Timestamp.from(approved), summary.approvalDate());
      assertEquals(
          List.of(Timestamp.from(firstRenewed), Timestamp.from(secondRenewed)),
          summary.progressReportApprovalDates());
    }
  }

  @Test
  void aProgressReportApprovedOnTwoStudyDatasetsIsOneRenewal() {
    Integer studyId = createStudy();
    Integer first = createStudyDataset(studyId);
    Integer second = createStudyDataset(studyId);
    String dar = createDar(now.minus(Duration.ofDays(400)), first, second);
    approve(dar, first, now.minus(Duration.ofDays(390)));
    approve(dar, second, now.minus(Duration.ofDays(390)));
    String report = createProgressReport(dar, now.minus(Duration.ofDays(100)), first, second);
    Instant renewed = now.minus(Duration.ofDays(90));
    approve(report, first, renewed);
    approve(report, second, now.minus(Duration.ofDays(80)));

    assertEquals(
        List.of(Timestamp.from(renewed)), onlyForStudy(studyId).progressReportApprovalDates());
  }

  @Test
  void pendingDeniedAndUndatedProgressReportsAddNoRenewalDate() {
    Integer dataset = createDataset();
    String dar = createDar(now.minus(Duration.ofDays(200)), dataset);
    approve(dar, dataset, now.minus(Duration.ofDays(190)));
    Instant undatedSubmitted = now.minus(Duration.ofDays(40));
    String undated = createProgressReport(dar, undatedSubmitted, dataset);
    approve(undated, dataset, null);
    String denied = createProgressReport(undated, now.minus(Duration.ofDays(30)), dataset);
    decide(denied, dataset, false, now.minus(Duration.ofDays(25)));
    String pending = createProgressReport(denied, now.minus(Duration.ofDays(20)), dataset);
    election(pending, dataset, ElectionStatus.OPEN);

    DarMetricsSummary summary = onlyForDataset(dataset);
    assertTrue(summary.progressReportApprovalDates().isEmpty());
    // The undated report was still approved, so it is the one that sets the term
    assertEquals(Timestamp.from(undatedSubmitted.plus(TERM)), summary.expirationDate());
  }

  @Test
  void accessExpiresATermAfterTheNewestApprovedSubmissionNotItsApproval() {
    Integer dataset = createDataset();
    Instant submitted = now.minus(Duration.ofDays(400));
    String dar = createDar(submitted, dataset);
    approve(dar, dataset, now.minus(Duration.ofDays(300)));

    DarMetricsSummary lapsed = onlyForDataset(dataset);
    assertEquals(Timestamp.from(submitted.plus(TERM)), lapsed.expirationDate());
    assertTrue(lapsed.expired());

    Instant reported = now.minus(Duration.ofDays(50));
    approve(createProgressReport(dar, reported, dataset), dataset, now.minus(Duration.ofDays(5)));

    DarMetricsSummary renewed = onlyForDataset(dataset);
    assertEquals(Timestamp.from(reported.plus(TERM)), renewed.expirationDate());
    assertFalse(renewed.expired());
  }

  @Test
  void aPendingProgressReportDoesNotMoveTheExpiration() {
    Integer dataset = createDataset();
    Instant submitted = now.minus(Duration.ofDays(100));
    String dar = createDar(submitted, dataset);
    approve(dar, dataset, now.minus(Duration.ofDays(90)));
    election(
        createProgressReport(dar, now.minus(Duration.ofDays(10)), dataset),
        dataset,
        ElectionStatus.OPEN);

    assertEquals(Timestamp.from(submitted.plus(TERM)), onlyForDataset(dataset).expirationDate());
  }

  @Test
  void aCloseoutBeforeTheTermRunsOutEndsAccessOnItsFilingDate() {
    Integer studyId = createStudy();
    Integer dataset = createStudyDataset(studyId);
    Instant submitted = now.minus(Duration.ofDays(100));
    String dar = createDar(submitted, dataset);
    approve(dar, dataset, now.minus(Duration.ofDays(90)));
    Instant filed = now.minus(Duration.ofDays(10));
    createCloseout(dar, filed);

    for (DarMetricsSummary summary : List.of(onlyForDataset(dataset), onlyForStudy(studyId))) {
      assertEquals(Timestamp.from(filed), summary.closeoutDate());
      assertEquals(Timestamp.from(submitted.plus(TERM)), summary.expirationDate());
      assertTrue(summary.expired());
    }
  }

  @Test
  void aCloseoutFiledAfterTheTermRanOutLeavesTheDarExpired() {
    Integer dataset = createDataset();
    Instant submitted = now.minus(Duration.ofDays(500));
    String dar = createDar(submitted, dataset);
    approve(dar, dataset, now.minus(Duration.ofDays(490)));
    createCloseout(dar, now.minus(Duration.ofDays(10)));

    DarMetricsSummary summary = onlyForDataset(dataset);
    assertNull(summary.closeoutDate());
    assertEquals(Timestamp.from(submitted.plus(TERM)), summary.expirationDate());
    assertTrue(summary.expired());
  }

  @Test
  void aSupplementOnAnOriginalDarIsNotACloseout() {
    Integer dataset = createDataset();
    String dar = createDar(closeoutData(), now.minus(Duration.ofDays(100)), dataset);
    approve(dar, dataset, now.minus(Duration.ofDays(90)));

    DarMetricsSummary summary = onlyForDataset(dataset);
    assertNull(summary.closeoutDate());
    assertFalse(summary.expired());
  }

  private DarMetricsSummary onlyForDataset(Integer datasetId) {
    List<DarMetricsSummary> summaries =
        dataAccessRequestDAO.findSummaryMetricApprovedDARsByDatasetIdIncludesExpired(datasetId);
    assertEquals(1, summaries.size());
    return summaries.getFirst();
  }

  private DarMetricsSummary onlyForStudy(Integer studyId) {
    List<DarMetricsSummary> summaries =
        dataAccessRequestDAO.findSummaryMetricApprovedDARsByStudyIdIncludesExpired(studyId);
    assertEquals(1, summaries.size());
    return summaries.getFirst();
  }
}
