package org.broadinstitute.consent.http.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.google.api.client.http.HttpStatusCodes;
import com.google.gson.JsonParser;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.broadinstitute.consent.http.AbstractTestHelper;
import org.broadinstitute.consent.http.enumeration.InstitutionSource;
import org.broadinstitute.consent.http.enumeration.MetricsBucket;
import org.broadinstitute.consent.http.models.DarDatasetTurnaround;
import org.broadinstitute.consent.http.models.DarMetricsSummary;
import org.broadinstitute.consent.http.models.DarTurnaround;
import org.broadinstitute.consent.http.models.DarVolume;
import org.broadinstitute.consent.http.models.DecisionReport;
import org.broadinstitute.consent.http.models.DuosUser;
import org.broadinstitute.consent.http.models.ExpirationReport;
import org.broadinstitute.consent.http.models.RenewalReport;
import org.broadinstitute.consent.http.models.SoApprovalReport;
import org.broadinstitute.consent.http.models.StudyRecommendation;
import org.broadinstitute.consent.http.models.StudyResearchOutputs;
import org.broadinstitute.consent.http.models.TurnaroundReport;
import org.broadinstitute.consent.http.models.VolumeReport;
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

  private static final List<Integer> SCOPE = List.of(7);

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

  /** The UI renders a study card from each recommendation, so every card field is served. */
  @Test
  void testRecommendationsCarryStudyCardFields() {
    StudyRecommendation recommendation =
        new StudyRecommendation(
            2,
            "Study",
            "Description",
            "PI",
            "Human",
            "Cancer",
            List.of("Genomic"),
            1L,
            List.of(3),
            100L,
            1,
            2,
            List.of("open"),
            List.of("GRU", "HMB"));
    when(service.getSimilarStudies(any(), any())).thenReturn(List.of(recommendation));

    Response response = resource.getSimilarStudies(duosUser, 1);
    String json = GsonUtil.getInstance().toJson(response.getEntity());
    Set<String> fields =
        JsonParser.parseString(json).getAsJsonArray().get(0).getAsJsonObject().keySet();

    assertEquals(
        Set.of(
            "studyId",
            "studyName",
            "studyDescription",
            "piName",
            "species",
            "phenotype",
            "dataTypes",
            "datasetCount",
            "datasetIds",
            "totalParticipants",
            "modelCount",
            "workspaceCount",
            "accessTypes",
            "dataUseCodes"),
        fields);
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
  void darDecisionsParseTheRangeBucketAndPage() {
    when(service.resolveDacScope(any(), eq(List.of()))).thenReturn(SCOPE);
    DecisionReport<?> report =
        new DecisionReport<>(
            "2026-01-01", "2026-03-31", MetricsBucket.MONTH, 0, List.of(), List.of());
    when(service.getDarDecisions(
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 3, 31),
            SCOPE,
            MetricsBucket.MONTH,
            50,
            100))
        .thenAnswer(i -> report);

    Response response =
        resource.getDarDecisions(duosUser, "2026-01-01", "2026-03-31", "Month", 50, 100, List.of());

    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    assertEquals(report, response.getEntity());
  }

  @Test
  void decisionTurnaroundReportsParseTheRangeBucketAndPage() {
    when(service.resolveDacScope(any(), eq(List.of()))).thenReturn(SCOPE);
    TurnaroundReport<DarTurnaround> dars =
        new TurnaroundReport<>(
            "2026-01-01", "2026-03-31", MetricsBucket.MONTH, 0, 0, List.of(), List.of());
    TurnaroundReport<DarDatasetTurnaround> pairs =
        new TurnaroundReport<>(
            "2026-01-01", "2026-03-31", MetricsBucket.MONTH, 0, 0, List.of(), List.of());
    LocalDate from = LocalDate.of(2026, 1, 1);
    LocalDate to = LocalDate.of(2026, 3, 31);
    when(service.getDarDecisionTurnaround(from, to, SCOPE, MetricsBucket.MONTH, 50, 100))
        .thenReturn(dars);
    when(service.getDarDatasetDecisionTurnaround(from, to, SCOPE, MetricsBucket.MONTH, 50, 100))
        .thenReturn(pairs);

    Response darResponse =
        resource.getDarDecisionTurnaround(
            duosUser, "2026-01-01", "2026-03-31", "month", 50, 100, List.of());
    Response pairResponse =
        resource.getDarDatasetDecisionTurnaround(
            duosUser, "2026-01-01", "2026-03-31", "month", 50, 100, List.of());

    assertEquals(HttpStatusCodes.STATUS_CODE_OK, darResponse.getStatus());
    assertEquals(dars, darResponse.getEntity());
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, pairResponse.getStatus());
    assertEquals(pairs, pairResponse.getEntity());
  }

  @Test
  void darSoApprovalsParseTheRangeBucketAndPage() {
    SoApprovalReport report =
        new SoApprovalReport(
            "2026-06-01", "2026-06-30", MetricsBucket.WEEK, 0, List.of(), List.of());
    when(service.getDarSoApprovals(
            LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), MetricsBucket.WEEK, 25, 50))
        .thenReturn(report);

    Response response =
        resource.getDarSoApprovals(duosUser, "2026-06-01", "2026-06-30", "week", 25, 50);

    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    assertEquals(report, response.getEntity());
  }

  @Test
  void darRenewalsParseTheRangeBucketAndPage() {
    when(service.resolveDacScope(any(), eq(List.of()))).thenReturn(SCOPE);
    RenewalReport report =
        new RenewalReport("2026-01-01", "2026-06-30", MetricsBucket.MONTH, 0, List.of(), List.of());
    when(service.getDarRenewals(
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 6, 30),
            SCOPE,
            MetricsBucket.MONTH,
            25,
            50))
        .thenReturn(report);

    Response response =
        resource.getDarRenewals(duosUser, "2026-01-01", "2026-06-30", "month", 25, 50, List.of());

    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    assertEquals(report, response.getEntity());
  }

  @Test
  void darExpirationsParseTheRangeBucketAndPage() {
    when(service.resolveDacScope(any(), eq(List.of()))).thenReturn(SCOPE);
    ExpirationReport report =
        new ExpirationReport(
            "2026-01-01", "2026-06-30", MetricsBucket.MONTH, 0, List.of(), List.of());
    when(service.getDarExpirations(
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 6, 30),
            SCOPE,
            MetricsBucket.MONTH,
            25,
            50))
        .thenReturn(report);

    Response response =
        resource.getDarExpirations(
            duosUser, "2026-01-01", "2026-06-30", "month", 25, 50, List.of());

    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    assertEquals(report, response.getEntity());
  }

  @Test
  void darDatasetDecisionsParseTheRangeBucketAndPage() {
    when(service.resolveDacScope(any(), eq(List.of()))).thenReturn(SCOPE);
    DecisionReport<?> report =
        new DecisionReport<>(
            "2026-01-01", "2026-01-01", MetricsBucket.DAY, 0, List.of(), List.of());
    when(service.getDarDatasetDecisions(
            eq(LocalDate.of(2026, 1, 1)),
            eq(LocalDate.of(2026, 1, 1)),
            eq(SCOPE),
            eq(MetricsBucket.DAY),
            anyInt(),
            anyInt()))
        .thenAnswer(i -> report);

    Response response =
        resource.getDarDatasetDecisions(
            duosUser, "2026-01-01", "2026-01-01", "day", 100, 0, List.of());

    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  void darVolumeParsesTheRangeBucketAndPage() {
    when(service.resolveDacScope(any(), eq(List.of()))).thenReturn(SCOPE);
    VolumeReport report =
        new VolumeReport(
            "2026-01-01",
            "2026-03-31",
            MetricsBucket.MONTH,
            0,
            List.of(),
            List.of(),
            List.of(),
            List.of());
    when(service.getDarVolume(
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 3, 31),
            SCOPE,
            MetricsBucket.MONTH,
            50,
            100))
        .thenReturn(report);

    Response response =
        resource.getDarVolume(duosUser, "2026-01-01", "2026-03-31", "Month", 50, 100, List.of());

    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    assertEquals(report, response.getEntity());
  }

  /** Compares the whole field set, so a collaborator name or email added under any key fails. */
  @Test
  void darVolumeRowsCarryNoCollaboratorDetails() {
    when(service.resolveDacScope(any(), eq(List.of()))).thenReturn(SCOPE);
    DarVolume row =
        new DarVolume("ref", 1, 2, Instant.EPOCH, 3, "Broad", InstitutionSource.RECORDED, 1, 2, 3);
    VolumeReport report =
        new VolumeReport(
            "2026-01-01",
            "2026-01-01",
            MetricsBucket.DAY,
            1,
            List.of(),
            List.of(),
            List.of(),
            List.of(row));
    when(service.getDarVolume(any(), any(), eq(SCOPE), any(), anyInt(), anyInt()))
        .thenReturn(report);

    Response response =
        resource.getDarVolume(duosUser, "2026-01-01", "2026-01-01", "day", 100, 0, List.of());
    String json = GsonUtil.getInstance().toJson(response.getEntity());
    Set<String> fields =
        JsonParser.parseString(json)
            .getAsJsonObject()
            .getAsJsonArray("rows")
            .get(0)
            .getAsJsonObject()
            .keySet();

    assertEquals(
        Set.of(
            "referenceId",
            "collectionId",
            "userId",
            "submissionDate",
            "institutionId",
            "institutionName",
            "institutionSource",
            "datasetCount",
            "labStaffCount",
            "internalCollaboratorCount"),
        fields);
  }

  @ParameterizedTest
  @CsvSource(
      nullValues = "null",
      value = {
        "null, 2026-01-01, quarter, 100, 0",
        "'  ', 2026-01-01, quarter, 100, 0",
        "2026-01-01, null, quarter, 100, 0",
        "01/01/2026, 2026-02-01, quarter, 100, 0",
        "1899-12-31, 2026-02-01, quarter, 100, 0",
        "2026-01-01, +999999999-12-31, quarter, 100, 0",
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
        resource.getDarDecisions(duosUser, from, to, bucket, limit, offset, List.of()).getStatus());
    assertEquals(
        HttpStatusCodes.STATUS_CODE_BAD_REQUEST,
        resource
            .getDarDatasetDecisions(duosUser, from, to, bucket, limit, offset, List.of())
            .getStatus());
    assertEquals(
        HttpStatusCodes.STATUS_CODE_BAD_REQUEST,
        resource.getDarVolume(duosUser, from, to, bucket, limit, offset, List.of()).getStatus());
    assertEquals(
        HttpStatusCodes.STATUS_CODE_BAD_REQUEST,
        resource
            .getDarDecisionTurnaround(duosUser, from, to, bucket, limit, offset, List.of())
            .getStatus());
    assertEquals(
        HttpStatusCodes.STATUS_CODE_BAD_REQUEST,
        resource
            .getDarDatasetDecisionTurnaround(duosUser, from, to, bucket, limit, offset, List.of())
            .getStatus());
    assertEquals(
        HttpStatusCodes.STATUS_CODE_BAD_REQUEST,
        resource.getDarSoApprovals(duosUser, from, to, bucket, limit, offset).getStatus());
    assertEquals(
        HttpStatusCodes.STATUS_CODE_BAD_REQUEST,
        resource
            .getDarExpirations(duosUser, from, to, bucket, limit, offset, List.of())
            .getStatus());
    assertEquals(
        HttpStatusCodes.STATUS_CODE_BAD_REQUEST,
        resource.getDarRenewals(duosUser, from, to, bucket, limit, offset, List.of()).getStatus());
    verifyNoInteractions(service);
  }

  @Test
  void dacIdsAreParsedAndResolvedBeforeTheReportRuns() {
    when(service.resolveDacScope(any(), eq(List.of(3, 5)))).thenReturn(List.of(3, 5));

    resource.getDarDecisions(
        duosUser, "2026-01-01", "2026-03-31", "quarter", 100, 0, List.of("3", "5", "3"));

    verify(service)
        .getDarDecisions(
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 3, 31),
            List.of(3, 5),
            MetricsBucket.QUARTER,
            100,
            0);
  }

  @Test
  void aNonIntegerDacIdIsRejected() {
    Response response =
        resource.getDarVolume(
            duosUser, "2026-01-01", "2026-03-31", "quarter", 100, 0, List.of("x"));

    assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    verifyNoInteractions(service);
  }

  @Test
  void aDacTheCallerIsNotOnIsForbidden() {
    when(service.resolveDacScope(any(), eq(List.of(9)))).thenThrow(new ForbiddenException());

    Response response =
        resource.getDarRenewals(
            duosUser, "2026-01-01", "2026-03-31", "quarter", 100, 0, List.of("9"));

    assertEquals(HttpStatusCodes.STATUS_CODE_FORBIDDEN, response.getStatus());
  }

  @Test
  void dacScopedReportsAdmitChairsAndMembers() throws NoSuchMethodException {
    for (String name :
        List.of(
            "getDarDecisions",
            "getDarDatasetDecisions",
            "getDarVolume",
            "getDarDecisionTurnaround",
            "getDarDatasetDecisionTurnaround",
            "getDarExpirations",
            "getDarRenewals")) {
      RolesAllowed roles =
          MetricsResource.class
              .getMethod(
                  name,
                  DuosUser.class,
                  String.class,
                  String.class,
                  String.class,
                  Integer.class,
                  Integer.class,
                  List.class)
              .getAnnotation(RolesAllowed.class);
      assertEquals(
          List.of(Resource.ADMIN, Resource.CHAIRPERSON, Resource.MEMBER), List.of(roles.value()));
    }
  }

  @Test
  void soApprovalsAreAdminOnly() throws NoSuchMethodException {
    RolesAllowed roles =
        MetricsResource.class
            .getMethod(
                "getDarSoApprovals",
                DuosUser.class,
                String.class,
                String.class,
                String.class,
                Integer.class,
                Integer.class)
            .getAnnotation(RolesAllowed.class);
    assertEquals(List.of(Resource.ADMIN), List.of(roles.value()));
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
