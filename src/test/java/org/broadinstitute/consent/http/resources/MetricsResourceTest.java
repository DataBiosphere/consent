package org.broadinstitute.consent.http.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.google.api.client.http.HttpStatusCodes;
import com.google.gson.JsonParser;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.broadinstitute.consent.http.AbstractTestHelper;
import org.broadinstitute.consent.http.enumeration.MetricsBucket;
import org.broadinstitute.consent.http.models.DarMetricsSummary;
import org.broadinstitute.consent.http.models.DecisionReport;
import org.broadinstitute.consent.http.models.DuosUser;
import org.broadinstitute.consent.http.models.StudyResearchOutputs;
import org.broadinstitute.consent.http.service.MetricsService;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
    when(service.generateDarSummaries(any(), any()))
        .thenReturn(List.of(generateDarMetricsSummary()));

    Response response = resource.getDarSummaryData(duosUser, 1);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  void testGenerateDarSummariesNotFound() {
    when(service.generateDarSummaries(any(), any())).thenThrow(new NotFoundException());

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

  /**
   * The requester's name is not in this payload, and the assertion is on the serialized response so
   * that it can still fail: pinning it to a record component would only restate the shape of a type
   * that no longer carries one. Comparing the whole field set means re-adding a name, under any
   * spelling, breaks this rather than passing unnoticed.
   */
  @Test
  void testDarSummariesCarryNoRequesterName() {
    Timestamp now = new Timestamp(System.currentTimeMillis());
    DarMetricsSummary summary =
        new DarMetricsSummary(
            now, now, "Project", "DAR-1", "Summary", "RUS", "ref-1", "Broad", false);
    when(service.generateDarSummaries(any(), any())).thenReturn(List.of(summary));

    Response response = resource.getDarSummaryData(duosUser, 1);
    String json = GsonUtil.getInstance().toJson(response.getEntity());
    Set<String> fields =
        JsonParser.parseString(json).getAsJsonArray().get(0).getAsJsonObject().keySet();

    assertEquals(
        Set.of(
            "updateDate",
            "submissionDate",
            "projectTitle",
            "darCode",
            "nonTechRus",
            "rus",
            "referenceId",
            "institutionName",
            "expired"),
        fields);
    assertFalse(json.toLowerCase().contains("piname"), "No PI name is served with a DAR summary");
  }

  @Test
  void darDatasetDecisionsParseTheRangeBucketAndPage() {
    DecisionReport<?> report =
        new DecisionReport<>(
            "2026-01-01", "2026-01-01", MetricsBucket.DAY, 0, List.of(), List.of());
    when(service.getDarDatasetDecisions(
            eq(LocalDate.of(2026, 1, 1)),
            eq(LocalDate.of(2026, 1, 1)),
            eq(MetricsBucket.DAY),
            anyInt(),
            anyInt()))
        .thenAnswer(i -> report);

    Response response =
        resource.getDarDatasetDecisions(duosUser, "2026-01-01", "2026-01-01", "day", 100, 0);

    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @ParameterizedTest
  @CsvSource(
      nullValues = "null",
      value = {
        "null, 2026-01-01, quarter, 100, 0",
        "'  ', 2026-01-01, quarter, 100, 0",
        "2026-01-01, null, quarter, 100, 0",
        "01/01/2026, 2026-02-01, quarter, 100, 0",
        "2026-02-01, 2026-01-01, quarter, 100, 0",
        "2026-01-01, 2026-02-01, year, 100, 0",
        "2026-01-01, 2026-02-01, quarter, 0, 0",
        "2026-01-01, 2026-02-01, quarter, 1001, 0",
        "2026-01-01, 2026-02-01, quarter, 100, -1"
      })
  void decisionReportsRejectBadParameters(
      String from, String to, String bucket, Integer limit, Integer offset) {
    assertEquals(
        HttpStatusCodes.STATUS_CODE_BAD_REQUEST,
        resource.getDarDatasetDecisions(duosUser, from, to, bucket, limit, offset).getStatus());
    verifyNoInteractions(service);
  }

  @Test
  void decisionReportsAreAdminOnly() throws NoSuchMethodException {
    for (String name : List.of("getDarDatasetDecisions")) {
      RolesAllowed roles =
          MetricsResource.class
              .getMethod(
                  name,
                  DuosUser.class,
                  String.class,
                  String.class,
                  String.class,
                  Integer.class,
                  Integer.class)
              .getAnnotation(RolesAllowed.class);
      assertEquals(List.of(Resource.ADMIN), List.of(roles.value()));
    }
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
