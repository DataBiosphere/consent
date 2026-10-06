package org.broadinstitute.consent.http.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.broadinstitute.consent.http.AbstractTestHelper;
import org.broadinstitute.consent.http.db.AccountMetricsDAO;
import org.broadinstitute.consent.http.db.DarMetricsDAO;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.db.StudyRecommendationDAO;
import org.broadinstitute.consent.http.enumeration.AccessEndReason;
import org.broadinstitute.consent.http.enumeration.DarKind;
import org.broadinstitute.consent.http.enumeration.DecidedVia;
import org.broadinstitute.consent.http.enumeration.DecisionState;
import org.broadinstitute.consent.http.enumeration.InstitutionSource;
import org.broadinstitute.consent.http.enumeration.MetricsBucket;
import org.broadinstitute.consent.http.enumeration.SoApprovalStatus;
import org.broadinstitute.consent.http.models.CreatedBucket;
import org.broadinstitute.consent.http.models.DarDatasetTurnaround;
import org.broadinstitute.consent.http.models.DarDecision;
import org.broadinstitute.consent.http.models.DarMetricsSummary;
import org.broadinstitute.consent.http.models.DarTurnaround;
import org.broadinstitute.consent.http.models.DarVolume;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DecisionBucketCount;
import org.broadinstitute.consent.http.models.DecisionReport;
import org.broadinstitute.consent.http.models.ExpirationBucket;
import org.broadinstitute.consent.http.models.ExpirationReport;
import org.broadinstitute.consent.http.models.ExpiredCollection;
import org.broadinstitute.consent.http.models.InstitutionDarCount;
import org.broadinstitute.consent.http.models.InstitutionReport;
import org.broadinstitute.consent.http.models.IntellectualProperty;
import org.broadinstitute.consent.http.models.Presentation;
import org.broadinstitute.consent.http.models.Publication;
import org.broadinstitute.consent.http.models.Renewal;
import org.broadinstitute.consent.http.models.RenewalBucket;
import org.broadinstitute.consent.http.models.RenewalReport;
import org.broadinstitute.consent.http.models.ResearcherDarCount;
import org.broadinstitute.consent.http.models.RoleUserCount;
import org.broadinstitute.consent.http.models.SoApproval;
import org.broadinstitute.consent.http.models.SoApprovalBucket;
import org.broadinstitute.consent.http.models.SoApprovalReport;
import org.broadinstitute.consent.http.models.Study;
import org.broadinstitute.consent.http.models.StudyRecommendation;
import org.broadinstitute.consent.http.models.StudyResearchOutputs;
import org.broadinstitute.consent.http.models.TurnaroundBucket;
import org.broadinstitute.consent.http.models.TurnaroundReport;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserReport;
import org.broadinstitute.consent.http.models.VolumeBucketCount;
import org.broadinstitute.consent.http.models.VolumeReport;
import org.broadinstitute.consent.http.service.DatasetService.DatasetRead;
import org.broadinstitute.consent.http.service.DatasetService.DatasetReadBasis;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MetricsServiceTest extends AbstractTestHelper {

  private static final List<Integer> ALL_DACS = null;

  @Mock private Jdbi jdbi;

  @Mock private DataAccessRequestDAO darDAO;

  @Mock private StudyRecommendationDAO recommendationDAO;

  @Mock private DarMetricsDAO darMetricsDAO;

  @Mock private AccountMetricsDAO accountMetricsDAO;

  @Mock private DatasetService datasetService;

  private final User user = new User();

  private MetricsService service;

  @BeforeEach
  void initService() {
    when(jdbi.onDemand(DataAccessRequestDAO.class)).thenReturn(darDAO);
    when(jdbi.onDemand(StudyRecommendationDAO.class)).thenReturn(recommendationDAO);
    when(jdbi.onDemand(DarMetricsDAO.class)).thenReturn(darMetricsDAO);
    when(jdbi.onDemand(AccountMetricsDAO.class)).thenReturn(accountMetricsDAO);
    service = new MetricsService(jdbi, datasetService);
  }

  /** The study exists and the requesting user may read it. */
  private void studyIsVisible() {
    when(datasetService.requireReadableStudy(eq(1), any())).thenReturn(new Study());
  }

  /**
   * The study is not readable by this caller - either it does not exist, or it is not publicly
   * visible and the caller is neither a custodian nor an admin. The shared gate answers both the
   * same way, so the study's metrics cannot be used to tell one case from the other.
   */
  private void studyIsNotReadable() {
    when(datasetService.requireReadableStudy(eq(1), any()))
        .thenThrow(new NotFoundException("Study not found"));
  }

  @Test
  void testGenerateDarSummaries() {
    DarMetricsSummary summary = generateDarMetricsSummary();
    Dataset dataset = generateDataset();
    dataset.setStudyId(10);

    when(datasetService.findDatasetByIdForReadWithBasis(user, dataset.getDatasetId()))
        .thenReturn(new DatasetRead(dataset, DatasetReadBasis.STUDY_READABLE));
    when(darDAO.findSummaryMetricApprovedDARsByDatasetIdIncludesExpired(any()))
        .thenReturn(List.of(summary));

    List<DarMetricsSummary> metrics = service.generateDarSummaries(dataset.getDatasetId(), user);

    assertEquals(summary.projectTitle(), metrics.getFirst().projectTitle());
    assertEquals(summary.darCode(), metrics.getFirst().darCode());
    verify(datasetService).findDatasetByIdForReadWithBasis(user, dataset.getDatasetId());
    verify(darDAO).findSummaryMetricApprovedDARsByDatasetIdIncludesExpired(dataset.getDatasetId());
  }

  /**
   * A dataset that belongs to a study was gated on that study's visibility, so the requester it
   * names was already readable by this caller.
   */
  @Test
  void testGenerateDarSummariesKeepsRequesterIdentityForAStudysDataset() {
    Dataset dataset = generateDataset();
    dataset.setStudyId(10);
    DarMetricsSummary summary =
        new DarMetricsSummary(null, null, "Project", "DAR-1", null, null, "ref-1", "Broad", false);

    when(datasetService.findDatasetByIdForReadWithBasis(user, dataset.getDatasetId()))
        .thenReturn(new DatasetRead(dataset, DatasetReadBasis.STUDY_READABLE));
    when(darDAO.findSummaryMetricApprovedDARsByDatasetIdIncludesExpired(any()))
        .thenReturn(List.of(summary));

    List<DarMetricsSummary> metrics = service.generateDarSummaries(dataset.getDatasetId(), user);

    assertEquals("Broad", metrics.getFirst().institutionName());
  }

  /**
   * Nothing about this caller was checked: a dataset with no study is returned to everyone because
   * there is no visibility to test. The request itself is readable under that rule; the requester's
   * affiliation would otherwise be harvestable by walking dataset ids.
   */
  @Test
  void testGenerateDarSummariesWithholdsRequesterIdentityWithoutAStudy() {
    Dataset dataset = generateDataset();
    DarMetricsSummary summary =
        new DarMetricsSummary(null, null, "Project", "DAR-1", null, null, "ref-1", "Broad", false);

    when(datasetService.findDatasetByIdForReadWithBasis(user, dataset.getDatasetId()))
        .thenReturn(new DatasetRead(dataset, DatasetReadBasis.NO_STUDY));
    when(darDAO.findSummaryMetricApprovedDARsByDatasetIdIncludesExpired(any()))
        .thenReturn(List.of(summary));

    List<DarMetricsSummary> metrics = service.generateDarSummaries(dataset.getDatasetId(), user);

    assertNull(metrics.getFirst().institutionName());
    // The request itself still comes through
    assertEquals("Project", metrics.getFirst().projectTitle());
    assertEquals("DAR-1", metrics.getFirst().darCode());
  }

  /**
   * The old rule keyed on the dataset having no study, which is a proxy for "nothing was checked" -
   * and wrong for anyone allowed in on their own merits. An admin reading a study-less dataset was
   * losing the institution for no reason.
   */
  @Test
  void testGenerateDarSummariesKeepsInstitutionForAnAdminOnAStudylessDataset() {
    Dataset dataset = generateDataset();
    DarMetricsSummary summary =
        new DarMetricsSummary(null, null, "Project", "DAR-1", null, null, "ref-1", "Broad", false);

    when(datasetService.findDatasetByIdForReadWithBasis(user, dataset.getDatasetId()))
        .thenReturn(new DatasetRead(dataset, DatasetReadBasis.ADMIN));
    when(darDAO.findSummaryMetricApprovedDARsByDatasetIdIncludesExpired(any()))
        .thenReturn(List.of(summary));

    List<DarMetricsSummary> metrics = service.generateDarSummaries(dataset.getDatasetId(), user);

    assertEquals("Broad", metrics.getFirst().institutionName());
  }

  /** Likewise the person who created the dataset. */
  @Test
  void testGenerateDarSummariesKeepsInstitutionForTheDatasetCreator() {
    Dataset dataset = generateDataset();
    DarMetricsSummary summary =
        new DarMetricsSummary(null, null, "Project", "DAR-1", null, null, "ref-1", "Broad", false);

    when(datasetService.findDatasetByIdForReadWithBasis(user, dataset.getDatasetId()))
        .thenReturn(new DatasetRead(dataset, DatasetReadBasis.DATASET_CREATOR));
    when(darDAO.findSummaryMetricApprovedDARsByDatasetIdIncludesExpired(any()))
        .thenReturn(List.of(summary));

    List<DarMetricsSummary> metrics = service.generateDarSummaries(dataset.getDatasetId(), user);

    assertEquals("Broad", metrics.getFirst().institutionName());
  }

  /**
   * The copy is positional, so this asserts every surviving field - including the two timestamps,
   * which are the same type and adjacent, and so the pair a reorder would swap without the compiler
   * noticing. Distinct values, because equal ones would survive a swap.
   */
  @Test
  void testWithoutRequesterIdentityKeepsEverythingElse() {
    Timestamp updated = new Timestamp(2_000_000_000L);
    Timestamp submitted = new Timestamp(1_000_000_000L);
    DarMetricsSummary summary =
        new DarMetricsSummary(
            updated, submitted, "Project", "DAR-1", "Summary", "RUS", "ref-1", "Broad", true);

    DarMetricsSummary redacted = summary.withoutRequesterIdentity();

    assertNull(redacted.institutionName());
    assertEquals(updated, redacted.updateDate());
    assertEquals(submitted, redacted.submissionDate());
    assertEquals("Project", redacted.projectTitle());
    assertEquals("DAR-1", redacted.darCode());
    // Adjacent and both String, so a reorder would swap them without the compiler noticing
    assertEquals("Summary", redacted.nonTechRus());
    assertEquals("RUS", redacted.rus());
    assertEquals("ref-1", redacted.referenceId());
    assertEquals(true, redacted.expired());
  }

  /** A dataset the caller may not read yields no summaries, rather than its DAR project titles. */
  @Test
  void testGenerateDarSummariesIsGatedOnReadingTheDataset() {
    when(datasetService.findDatasetByIdForReadWithBasis(user, 1))
        .thenThrow(new ForbiddenException("User does not have permission"));

    assertThrows(ForbiddenException.class, () -> service.generateDarSummaries(1, user));
    verify(darDAO, never()).findSummaryMetricApprovedDARsByDatasetIdIncludesExpired(any());
  }

  @Test
  void testGenerateDarSummariesNotFound() {
    when(datasetService.findDatasetByIdForReadWithBasis(eq(user), any()))
        .thenThrow(new NotFoundException("Entity not found"));

    assertThrows(NotFoundException.class, () -> service.generateDarSummaries(1, user));
  }

  @Test
  void testGenerateStudyDarSummariesStudyNotFound() {
    studyIsNotReadable();

    assertThrows(NotFoundException.class, () -> service.generateStudyDarSummaries(1, user));
  }

  @Test
  void testGenerateStudyDarSummariesStudyWithoutDatasets() {
    studyIsVisible();
    when(darDAO.findSummaryMetricApprovedDARsByStudyIdIncludesExpired(1)).thenReturn(List.of());

    assertTrue(service.generateStudyDarSummaries(1, user).isEmpty());
  }

  @Test
  void testGenerateStudyDarSummariesUsesTheStudyScopedQuery() {
    DarMetricsSummary summary = generateDarMetricsSummary();
    studyIsVisible();
    when(darDAO.findSummaryMetricApprovedDARsByStudyIdIncludesExpired(1))
        .thenReturn(List.of(summary));

    assertEquals(List.of(summary), service.generateStudyDarSummaries(1, user));

    // One round trip for the whole study. The service holds no DatasetDAO at all now, so it
    // cannot fan out per dataset even by accident.
    verify(darDAO).findSummaryMetricApprovedDARsByStudyIdIncludesExpired(1);
  }

  @Test
  void testStudyMetricsAreHiddenWhenTheStudyIsNotVisible() {
    studyIsNotReadable();

    assertThrows(NotFoundException.class, () -> service.generateStudyDarSummaries(1, user));
    assertThrows(NotFoundException.class, () -> service.generateStudyResearchOutputs(1, user));
    assertThrows(NotFoundException.class, () -> service.getSimilarStudies(1, user));
    assertThrows(NotFoundException.class, () -> service.getFrequentlyRequestedWith(1, user));
  }

  @Test
  void testGenerateStudyResearchOutputsNotFound() {
    studyIsNotReadable();

    assertThrows(NotFoundException.class, () -> service.generateStudyResearchOutputs(1, user));
  }

  @Test
  void testGenerateStudyResearchOutputsAggregatesAcrossReports() {
    Presentation presentation =
        new Presentation(
            null,
            null,
            null,
            null,
            null,
            null,
            UUID.randomUUID().toString(),
            null,
            null,
            null,
            null,
            null,
            null,
            null);
    Publication publication =
        new Publication(
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            UUID.randomUUID().toString(),
            null,
            null,
            null,
            null,
            null,
            null);
    IntellectualProperty ip =
        new IntellectualProperty(
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            UUID.randomUUID().toString(),
            null,
            null);

    DataAccessRequestData dataWithOutputs = new DataAccessRequestData();
    dataWithOutputs.setPresentations(List.of(presentation));
    dataWithOutputs.setPublications(List.of(publication));
    dataWithOutputs.setIntellectualProperties(List.of(ip));
    DataAccessRequest reportWithOutputs = new DataAccessRequest();
    reportWithOutputs.setId(1);
    reportWithOutputs.setSubmissionDate(Timestamp.from(Instant.now()));
    reportWithOutputs.setData(dataWithOutputs);

    // A report carrying no data at all must not break the aggregation
    DataAccessRequest reportWithoutData = new DataAccessRequest();
    reportWithoutData.setId(2);
    reportWithoutData.setSubmissionDate(Timestamp.from(Instant.now().minusSeconds(60)));

    studyIsVisible();
    when(darDAO.findProgressReportsByStudyId(1))
        .thenReturn(List.of(reportWithOutputs, reportWithoutData));

    StudyResearchOutputs outputs = service.generateStudyResearchOutputs(1, user);

    assertEquals(List.of(presentation), outputs.presentations());
    assertEquals(List.of(publication), outputs.publications());
    assertEquals(List.of(ip), outputs.intellectualProperties());
  }

  @Test
  void testGetSimilarStudies() {
    StudyRecommendation recommendation = generateStudyRecommendation();
    studyIsVisible();
    when(recommendationDAO.findSimilar(1)).thenReturn(List.of(recommendation));

    assertEquals(List.of(recommendation), service.getSimilarStudies(1, user));
  }

  @Test
  void testGetSimilarStudiesNotFound() {
    studyIsNotReadable();

    assertThrows(NotFoundException.class, () -> service.getSimilarStudies(1, user));
  }

  @Test
  void testGetFrequentlyRequestedWith() {
    StudyRecommendation recommendation = generateStudyRecommendation();
    studyIsVisible();
    when(recommendationDAO.findFrequentlyRequestedWith(1)).thenReturn(List.of(recommendation));

    assertEquals(List.of(recommendation), service.getFrequentlyRequestedWith(1, user));
  }

  @Test
  void testGetFrequentlyRequestedWithNotFound() {
    studyIsNotReadable();

    assertThrows(NotFoundException.class, () -> service.getFrequentlyRequestedWith(1, user));
  }

  private DarMetricsSummary generateDarMetricsSummary() {
    return new DarMetricsSummary(
        null,
        UUID.randomUUID().toString(),
        "DAR-" + randomInt(1, 100),
        null,
        UUID.randomUUID().toString(),
        false);
  }

  private StudyRecommendation generateStudyRecommendation() {
    return new StudyRecommendation(
        randomInt(2, 100),
        UUID.randomUUID().toString(),
        UUID.randomUUID().toString(),
        UUID.randomUUID().toString(),
        "Human",
        UUID.randomUUID().toString(),
        List.of("Genomic"),
        1L,
        List.of(randomInt(1, 100)),
        100L,
        0,
        0,
        List.of("open"),
        List.of("GRU"));
  }

  private Dataset generateDataset() {
    Dataset d = new Dataset();
    d.setAlias(1);
    d.setDatasetId(1);
    d.setName(UUID.randomUUID().toString());
    return d;
  }

  @Test
  void darDecisionsCoverWholeDaysAndTotalTheBuckets() {
    Instant start = startOfDay(LocalDate.of(2026, 1, 1));
    Instant end = startOfDay(LocalDate.of(2026, 4, 1));
    List<DecisionBucketCount> buckets =
        List.of(
            new DecisionBucketCount(start, DecisionState.APPROVED, null, 3L),
            new DecisionBucketCount(start, DecisionState.PENDING, null, 2L));
    DarDecision row = new DarDecision("ref", 1, start, 1, DecisionState.PENDING, null, null);
    when(darMetricsDAO.countDarDecisions(start, end, ALL_DACS, "quarter")).thenReturn(buckets);
    when(darMetricsDAO.findDarDecisions(start, end, ALL_DACS, 10, 20)).thenReturn(List.of(row));

    DecisionReport<DarDecision> report =
        service.getDarDecisions(
            LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31), MetricsBucket.QUARTER, 10, 20);

    assertEquals(5, report.total());
    assertEquals(buckets, report.buckets());
    assertEquals(List.of(row), report.rows());
    assertEquals("2026-03-31", report.to());
  }

  @Test
  void decisionTurnaroundTotalsMeasuredAndUnmeasuredDecisions() {
    Instant start = startOfDay(LocalDate.of(2026, 1, 1));
    Instant end = startOfDay(LocalDate.of(2026, 7, 1));
    List<TurnaroundBucket> buckets =
        List.of(
            new TurnaroundBucket(start, 3L, 1L, 4.0, 3.0, 3),
            new TurnaroundBucket(start.plus(Duration.ofDays(90)), 2L, 0L, 1.5, 1.5, 1));
    DarTurnaround row = new DarTurnaround("ref", 1, start, start, DecidedVia.MANUAL, 0.0);
    DarDatasetTurnaround pair =
        new DarDatasetTurnaround("ref", 1, 2, start, start, DecidedVia.RADAR, 0.0);
    when(darMetricsDAO.countDarTurnaround(start, end, ALL_DACS, "quarter")).thenReturn(buckets);
    when(darMetricsDAO.findDarTurnaround(start, end, ALL_DACS, 10, 20)).thenReturn(List.of(row));
    when(darMetricsDAO.countPairTurnaround(start, end, ALL_DACS, "quarter")).thenReturn(buckets);
    when(darMetricsDAO.findPairTurnaround(start, end, ALL_DACS, 10, 20)).thenReturn(List.of(pair));
    LocalDate from = LocalDate.of(2026, 1, 1);
    LocalDate to = LocalDate.of(2026, 6, 30);

    TurnaroundReport<DarTurnaround> dars =
        service.getDarDecisionTurnaround(from, to, MetricsBucket.QUARTER, 10, 20);
    TurnaroundReport<DarDatasetTurnaround> pairs =
        service.getDarDatasetDecisionTurnaround(from, to, MetricsBucket.QUARTER, 10, 20);

    assertEquals(5, dars.total());
    assertEquals(1, dars.unmeasured());
    assertEquals(buckets, dars.buckets());
    assertEquals(List.of(row), dars.rows());
    assertEquals(5, pairs.total());
    assertEquals(List.of(pair), pairs.rows());
  }

  @Test
  void soApprovalsCoverWholeDaysAndTotalTheBuckets() {
    Instant start = startOfDay(LocalDate.of(2026, 6, 1));
    Instant end = startOfDay(LocalDate.of(2026, 7, 1));
    List<SoApprovalBucket> buckets =
        List.of(
            new SoApprovalBucket(
                start, DarKind.ORIGINAL, SoApprovalStatus.APPROVED, 3L, 0L, 2.0, 2.0, 2),
            new SoApprovalBucket(
                start, DarKind.CLOSEOUT, SoApprovalStatus.PENDING, 1L, 0L, null, null, null));
    SoApproval row =
        new SoApproval("ref", 1, DarKind.ORIGINAL, start, SoApprovalStatus.SKIPPED, null, null);
    when(darMetricsDAO.countSoApprovals(start, end, "month")).thenReturn(buckets);
    when(darMetricsDAO.findSoApprovals(start, end, 10, 20)).thenReturn(List.of(row));

    SoApprovalReport report =
        service.getDarSoApprovals(
            LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), MetricsBucket.MONTH, 10, 20);

    assertEquals(4, report.total());
    assertEquals(buckets, report.buckets());
    assertEquals(List.of(row), report.rows());
  }

  @Test
  void expirationsCoverWholeDaysAndTotalTheBuckets() {
    Instant start = startOfDay(LocalDate.of(2026, 1, 1));
    Instant end = startOfDay(LocalDate.of(2026, 7, 1));
    List<ExpirationBucket> buckets =
        List.of(
            new ExpirationBucket(start, AccessEndReason.EXPIRED, 3L),
            new ExpirationBucket(start, AccessEndReason.CLOSED_OUT, 2L));
    ExpiredCollection row = new ExpiredCollection(1, "DAR-1", start, AccessEndReason.EXPIRED);
    ArgumentCaptor<Instant> countedAsOf = ArgumentCaptor.forClass(Instant.class);
    ArgumentCaptor<Instant> foundAsOf = ArgumentCaptor.forClass(Instant.class);
    when(darMetricsDAO.countExpirations(
            eq(start), eq(end), isNull(), countedAsOf.capture(), eq("quarter")))
        .thenReturn(buckets);
    when(darMetricsDAO.findExpirations(
            eq(start), eq(end), isNull(), foundAsOf.capture(), eq(10), eq(20)))
        .thenReturn(List.of(row));

    ExpirationReport report =
        service.getDarExpirations(
            LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30), MetricsBucket.QUARTER, 10, 20);

    assertEquals(5, report.total());
    assertEquals(buckets, report.buckets());
    assertEquals(List.of(row), report.rows());
    assertEquals(countedAsOf.getValue(), foundAsOf.getValue());
  }

  @Test
  void renewalsCoverWholeDaysAndTotalTheBuckets() {
    Instant start = startOfDay(LocalDate.of(2026, 1, 1));
    Instant end = startOfDay(LocalDate.of(2026, 7, 1));
    List<RenewalBucket> buckets =
        List.of(
            new RenewalBucket(start, 3L, 2L),
            new RenewalBucket(start.plus(Duration.ofDays(90)), 1L, 1L));
    Renewal row = new Renewal("ref", 1, 2, start, DecidedVia.MANUAL, start);
    when(darMetricsDAO.countRenewals(start, end, ALL_DACS, "quarter")).thenReturn(buckets);
    when(darMetricsDAO.findRenewals(start, end, ALL_DACS, 10, 20)).thenReturn(List.of(row));

    RenewalReport report =
        service.getDarRenewals(
            LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30), MetricsBucket.QUARTER, 10, 20);

    assertEquals(4, report.total());
    assertEquals(buckets, report.buckets());
    assertEquals(List.of(row), report.rows());
  }

  @Test
  void pairDecisionsCoverWholeDays() {
    Instant start = startOfDay(LocalDate.of(2026, 5, 1));
    Instant end = startOfDay(LocalDate.of(2026, 5, 2));
    when(darMetricsDAO.countPairDecisions(start, end, ALL_DACS, "day")).thenReturn(List.of());
    when(darMetricsDAO.findPairDecisions(start, end, ALL_DACS, 100, 0)).thenReturn(List.of());

    var report =
        service.getDarDatasetDecisions(
            LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 1), MetricsBucket.DAY, 100, 0);

    assertEquals(0, report.total());
    assertTrue(report.rows().isEmpty());
  }

  @Test
  void darVolumeCoversWholeDaysAndTotalsTheBuckets() {
    Instant start = startOfDay(LocalDate.of(2026, 1, 1));
    Instant end = startOfDay(LocalDate.of(2026, 4, 1));
    List<VolumeBucketCount> buckets =
        List.of(
            new VolumeBucketCount(start, 3L, 2L, 2L, 4L),
            new VolumeBucketCount(end, 1L, 1L, 1L, 1L));
    DarVolume row =
        new DarVolume("ref", 1, 2, start, 3, "Broad", InstitutionSource.RECORDED, 1, 0, 0);
    when(darMetricsDAO.countDarVolume(start, end, ALL_DACS, "quarter")).thenReturn(buckets);
    List<InstitutionDarCount> institutions = List.of(new InstitutionDarCount(3, "Broad", 4L, 3L));
    List<ResearcherDarCount> researchers = List.of(new ResearcherDarCount(2, 4L));
    when(darMetricsDAO.findDarVolume(start, end, ALL_DACS, 10, 20)).thenReturn(List.of(row));
    when(darMetricsDAO.countDarsByInstitution(start, end, ALL_DACS)).thenReturn(institutions);
    when(darMetricsDAO.countDarsByResearcher(start, end, ALL_DACS)).thenReturn(researchers);

    VolumeReport report =
        service.getDarVolume(
            LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31), MetricsBucket.QUARTER, 10, 20);

    assertEquals(4, report.total());
    assertEquals(buckets, report.buckets());
    assertEquals(institutions, report.institutions());
    assertEquals(researchers, report.researchers());
    assertEquals(List.of(row), report.rows());
    assertEquals("2026-03-31", report.to());
  }

  private static Instant startOfDay(LocalDate date) {
    return date.atStartOfDay(ZoneId.systemDefault()).toInstant();
  }

  @Test
  void usersAndInstitutionsTotalTheirBucketsOverTheWholeLastDay() {
    LocalDate from = LocalDate.of(2026, 1, 1);
    LocalDate to = LocalDate.of(2026, 3, 31);
    Instant start = from.atStartOfDay(ZoneId.systemDefault()).toInstant();
    Instant end = LocalDate.of(2026, 4, 1).atStartOfDay(ZoneId.systemDefault()).toInstant();
    List<CreatedBucket> users =
        List.of(new CreatedBucket(start, 3), new CreatedBucket(start.plusSeconds(86400 * 31L), 2));
    List<CreatedBucket> institutions = List.of(new CreatedBucket(start, 1));
    List<RoleUserCount> roles = List.of(new RoleUserCount("Researcher", 5));
    when(accountMetricsDAO.countUsersCreated(start, end, "month")).thenReturn(users);
    when(accountMetricsDAO.countUsersByRole(start, end)).thenReturn(roles);
    when(accountMetricsDAO.countInstitutionsCreated(start, end, "month")).thenReturn(institutions);

    UserReport userReport = service.getUsers(from, to, MetricsBucket.MONTH);
    InstitutionReport institutionReport = service.getInstitutions(from, to, MetricsBucket.MONTH);

    assertEquals("2026-01-01", userReport.from());
    assertEquals("2026-03-31", userReport.to());
    assertEquals(5, userReport.total());
    assertEquals(users, userReport.buckets());
    assertEquals(roles, userReport.roles());
    assertEquals(1, institutionReport.total());
    assertEquals(institutions, institutionReport.buckets());
  }
}
