package org.broadinstitute.consent.http.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.NotFoundException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.broadinstitute.consent.http.AbstractTestHelper;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.StudyRecommendationDAO;
import org.broadinstitute.consent.http.models.DarMetricsSummary;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.IntellectualProperty;
import org.broadinstitute.consent.http.models.Presentation;
import org.broadinstitute.consent.http.models.Publication;
import org.broadinstitute.consent.http.models.Study;
import org.broadinstitute.consent.http.models.StudyRecommendation;
import org.broadinstitute.consent.http.models.StudyResearchOutputs;
import org.broadinstitute.consent.http.models.User;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MetricsServiceTest extends AbstractTestHelper {

  @Mock private Jdbi jdbi;

  @Mock private DatasetDAO dataSetDAO;

  @Mock private DataAccessRequestDAO darDAO;

  @Mock private StudyRecommendationDAO recommendationDAO;

  @Mock private DatasetService datasetService;

  private final User user = new User();

  private MetricsService service;

  @BeforeEach
  void initService() {
    when(jdbi.onDemand(DatasetDAO.class)).thenReturn(dataSetDAO);
    when(jdbi.onDemand(DataAccessRequestDAO.class)).thenReturn(darDAO);
    when(jdbi.onDemand(StudyRecommendationDAO.class)).thenReturn(recommendationDAO);
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

    when(dataSetDAO.findDatasetIdById(dataset.getDatasetId())).thenReturn(dataset.getDatasetId());
    when(darDAO.findSummaryMetricApprovedDARsByDatasetIdIncludesExpired(any()))
        .thenReturn(List.of(summary));

    List<DarMetricsSummary> metrics = service.generateDarSummaries(dataset.getDatasetId());

    assertEquals(summary.projectTitle(), metrics.getFirst().projectTitle());
    assertEquals(summary.darCode(), metrics.getFirst().darCode());
    verify(dataSetDAO).findDatasetIdById(dataset.getDatasetId());
    verify(darDAO).findSummaryMetricApprovedDARsByDatasetIdIncludesExpired(dataset.getDatasetId());
  }

  @Test
  void testGenerateDarSummariesNotFound() {
    when(dataSetDAO.findDatasetIdById(any())).thenReturn(null);

    assertThrows(NotFoundException.class, () -> service.generateDarSummaries(1));
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

    // One round trip for the whole study, rather than one query per dataset
    verify(darDAO).findSummaryMetricApprovedDARsByStudyIdIncludesExpired(1);
    verify(dataSetDAO, never()).findDatasetIdsByStudyId(any());
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

  private DarMetricsSummary generateDarMetricsSummary(
      String referenceId, Timestamp submissionDate) {
    return new DarMetricsSummary(
        null,
        submissionDate,
        UUID.randomUUID().toString(),
        "DAR-" + randomInt(1, 100),
        null,
        referenceId,
        null,
        null,
        false);
  }

  private StudyRecommendation generateStudyRecommendation() {
    return new StudyRecommendation(
        randomInt(2, 100),
        UUID.randomUUID().toString(),
        UUID.randomUUID().toString(),
        UUID.randomUUID().toString(),
        1L,
        List.of(randomInt(1, 100)));
  }

  private Dataset generateDataset() {
    Dataset d = new Dataset();
    d.setAlias(1);
    d.setDatasetId(1);
    d.setName(UUID.randomUUID().toString());
    return d;
  }
}
