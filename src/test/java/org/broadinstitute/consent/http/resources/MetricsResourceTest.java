package org.broadinstitute.consent.http.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.google.api.client.http.HttpStatusCodes;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;
import org.broadinstitute.consent.http.AbstractTestHelper;
import org.broadinstitute.consent.http.models.DarMetricsSummary;
import org.broadinstitute.consent.http.models.DuosUser;
import org.broadinstitute.consent.http.models.StudyResearchOutputs;
import org.broadinstitute.consent.http.service.MetricsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MetricsResourceTest extends AbstractTestHelper {

  @Mock private MetricsService service;
  @Mock private DuosUser duosUser;

  private MetricsResource resource;

  @BeforeEach
  void setUp() {
    resource = new MetricsResource(service);
  }

  @Test
  void testGenerateDarSummaries() {
    when(service.generateDarSummaries(any())).thenReturn(List.of(generateDarMetricsSummary()));

    Response response = resource.getDarSummaryData(duosUser, 1);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  void testGenerateDarSummariesNotFound() {
    when(service.generateDarSummaries(any())).thenThrow(new NotFoundException());

    Response response = resource.getDarSummaryData(duosUser, 1);
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  @Test
  void testGenerateStudyDarSummaries() {
    when(service.generateStudyDarSummaries(any(), any()))
        .thenReturn(List.of(generateDarMetricsSummary()));

    Response response = resource.getStudyDarSummaryData(duosUser, 1);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  void testGenerateStudyDarSummariesNotFound() {
    when(service.generateStudyDarSummaries(any(), any())).thenThrow(new NotFoundException());

    Response response = resource.getStudyDarSummaryData(duosUser, 1);
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  @Test
  void testGetStudyResearchOutputs() {
    when(service.generateStudyResearchOutputs(any(), any()))
        .thenReturn(new StudyResearchOutputs(List.of(), List.of(), List.of()));

    Response response = resource.getStudyResearchOutputs(duosUser, 1);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  void testGetStudyResearchOutputsNotFound() {
    when(service.generateStudyResearchOutputs(any(), any())).thenThrow(new NotFoundException());

    Response response = resource.getStudyResearchOutputs(duosUser, 1);
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  @Test
  void testGetSimilarStudies() {
    when(service.getSimilarStudies(any(), any())).thenReturn(List.of());

    Response response = resource.getSimilarStudies(duosUser, 1);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  void testGetSimilarStudiesNotFound() {
    when(service.getSimilarStudies(any(), any())).thenThrow(new NotFoundException());

    Response response = resource.getSimilarStudies(duosUser, 1);
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  @Test
  void testGetFrequentlyRequestedWith() {
    when(service.getFrequentlyRequestedWith(any(), any())).thenReturn(List.of());

    Response response = resource.getFrequentlyRequestedWith(duosUser, 1);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  void testGetFrequentlyRequestedWithNotFound() {
    when(service.getFrequentlyRequestedWith(any(), any())).thenThrow(new NotFoundException());

    Response response = resource.getFrequentlyRequestedWith(duosUser, 1);
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
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
}
