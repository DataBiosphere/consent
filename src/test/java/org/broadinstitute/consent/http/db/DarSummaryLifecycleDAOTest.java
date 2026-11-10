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
