package org.broadinstitute.consent.http.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import jakarta.annotation.Priority;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import org.broadinstitute.consent.http.enumeration.DarReportAudience;
import org.broadinstitute.consent.http.enumeration.MetricsBucket;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.DecisionReport;
import org.broadinstitute.consent.http.models.DuosUser;
import org.broadinstitute.consent.http.models.ExpirationReport;
import org.broadinstitute.consent.http.models.RenewalReport;
import org.broadinstitute.consent.http.models.TurnaroundReport;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.VolumeReport;
import org.broadinstitute.consent.http.service.MetricsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;

/**
 * The study and dataset DAR trend reports, requested through Jersey's own routing. Their paths take
 * the report as a parameter and sit beside routes that end the same way, so calling the resource
 * methods directly could not show which route a request reaches.
 */
@ExtendWith(DropwizardExtensionsSupport.class)
class ScopedDarReportEndpointTest {

  private static final User USER = new User();
  private static final List<Integer> SCOPE = List.of(11, 12);
  private static final LocalDate FROM = LocalDate.of(2026, 1, 1);
  private static final LocalDate TO = LocalDate.of(2026, 3, 31);
  private static final String RANGE = "?from=2026-01-01&to=2026-03-31";

  private final MetricsService service = mock(MetricsService.class);

  // Each test gets a fresh extension, and so a fresh mock.
  private final ResourceExtension resource =
      ResourceExtension.builder()
          .addResource(new MetricsResource(service))
          .addProvider(new AuthValueFactoryProvider.Binder<>(DuosUser.class))
          .addProvider(new SignedIn())
          .build();

  /** Stands in for authentication, which these routes need but this test is not about. */
  @Priority(Priorities.AUTHENTICATION)
  private static class SignedIn implements ContainerRequestFilter {
    @Override
    public void filter(ContainerRequestContext requestContext) {
      DuosUser principal = new DuosUser(new AuthUser("researcher@example.org"), USER);
      requestContext.setSecurityContext(
          new SecurityContext() {
            @Override
            public Principal getUserPrincipal() {
              return principal;
            }

            @Override
            public boolean isUserInRole(String role) {
              return false;
            }

            @Override
            public boolean isSecure() {
              return true;
            }

            @Override
            public String getAuthenticationScheme() {
              return "test";
            }
          });
    }
  }

  /**
   * Volume, decisions and turnaround count denied and pending requests, so each is read with the
   * DAC in charge's scope. Expirations and renewals cover granted access, so each is read with the
   * scope of whoever can read the DAR history. The two scopes differ here to show which is used.
   */
  @Test
  void eachStudyReportReadsTheDatasetsItsAudienceMayRead() {
    List<Integer> dacScope = List.of(11);
    when(service.resolveStudyDarScope(5, USER, DarReportAudience.DAC_IN_CHARGE))
        .thenReturn(dacScope);
    when(service.resolveStudyDarScope(5, USER, DarReportAudience.HISTORY_READERS))
        .thenReturn(SCOPE);
    stubReports();

    for (String report : reports()) {
      assertEquals(200, get("/api/metrics/" + report + "/study/5" + RANGE + "&bucket=month"));
    }

    verify(service).getScopedDarVolume(FROM, TO, dacScope, MetricsBucket.MONTH);
    verify(service).getScopedDarDatasetDecisions(FROM, TO, dacScope, MetricsBucket.MONTH);
    verify(service).getScopedDarDatasetDecisionTurnaround(FROM, TO, dacScope, MetricsBucket.MONTH);
    verify(service).getScopedDarExpirations(FROM, TO, SCOPE, MetricsBucket.MONTH);
    verify(service).getScopedDarRenewals(FROM, TO, SCOPE, MetricsBucket.MONTH);
  }

  /** Whoever reads the history but is not the DAC in charge sees granted access and no more. */
  @ParameterizedTest
  @CsvSource({"study/5", "dataset/7"})
  void aReaderWhoIsNotTheDacInChargeGetsOnlyTheReportsOnGrantedAccess(String target) {
    ForbiddenException refusal = new ForbiddenException("Only the DAC in charge and admins");
    when(service.resolveStudyDarScope(5, USER, DarReportAudience.DAC_IN_CHARGE)).thenThrow(refusal);
    when(service.resolveDatasetDarScope(7, USER, DarReportAudience.DAC_IN_CHARGE))
        .thenThrow(refusal);
    when(service.resolveStudyDarScope(5, USER, DarReportAudience.HISTORY_READERS))
        .thenReturn(SCOPE);
    when(service.resolveDatasetDarScope(7, USER, DarReportAudience.HISTORY_READERS))
        .thenReturn(List.of(7));
    stubReports();

    for (String report :
        List.of("dar-volume", "dar-dataset-decisions", "dar-dataset-decision-turnaround")) {
      assertEquals(403, get("/api/metrics/" + report + "/" + target + RANGE));
    }
    for (String report : List.of("dar-expirations", "dar-renewals")) {
      assertEquals(200, get("/api/metrics/" + report + "/" + target + RANGE));
    }

    verify(service, never()).getScopedDarVolume(any(), any(), any(), any());
    verify(service, never()).getScopedDarDatasetDecisions(any(), any(), any(), any());
    verify(service, never()).getScopedDarDatasetDecisionTurnaround(any(), any(), any(), any());
  }

  @Test
  void eachDatasetReportReadsThatDataset() {
    when(service.resolveDatasetDarScope(eq(7), eq(USER), any())).thenReturn(List.of(7));
    stubReports();

    for (String report : reports()) {
      assertEquals(200, get("/api/metrics/" + report + "/dataset/7" + RANGE));
    }

    // The bucket defaults to quarter, as on the admin reports
    verify(service).getScopedDarVolume(FROM, TO, List.of(7), MetricsBucket.QUARTER);
    verify(service).getScopedDarDatasetDecisions(FROM, TO, List.of(7), MetricsBucket.QUARTER);
    verify(service)
        .getScopedDarDatasetDecisionTurnaround(FROM, TO, List.of(7), MetricsBucket.QUARTER);
    verify(service).getScopedDarExpirations(FROM, TO, List.of(7), MetricsBucket.QUARTER);
    verify(service).getScopedDarRenewals(FROM, TO, List.of(7), MetricsBucket.QUARTER);
    verify(service, times(3)).resolveDatasetDarScope(7, USER, DarReportAudience.DAC_IN_CHARGE);
    verify(service, times(2)).resolveDatasetDarScope(7, USER, DarReportAudience.HISTORY_READERS);
  }

  /** The caller's access is decided before any report is read. */
  @Test
  void theScopeIsResolvedBeforeTheReportIsQueried() {
    when(service.resolveStudyDarScope(5, USER, DarReportAudience.DAC_IN_CHARGE)).thenReturn(SCOPE);
    stubReports();

    get("/api/metrics/dar-volume/study/5" + RANGE);

    InOrder order = inOrder(service);
    order.verify(service).resolveStudyDarScope(5, USER, DarReportAudience.DAC_IN_CHARGE);
    order.verify(service).getScopedDarVolume(any(), any(), any(), any());
  }

  /** The page sends the admin report's paging and DAC parameters; none of them applies here. */
  @Test
  void pagingAndDacParametersAreAcceptedAndIgnored() {
    when(service.resolveStudyDarScope(5, USER, DarReportAudience.DAC_IN_CHARGE)).thenReturn(SCOPE);
    stubReports();

    assertEquals(
        200,
        get("/api/metrics/dar-volume/study/5" + RANGE + "&limit=5000&offset=-1&dacId=3&dacId=x"));

    verify(service).getScopedDarVolume(FROM, TO, SCOPE, MetricsBucket.QUARTER);
    verify(service).resolveStudyDarScope(5, USER, DarReportAudience.DAC_IN_CHARGE);
    verifyNoMoreInteractions(service);
  }

  @Test
  void aStudyTheCallerCannotReadServesNoReport() {
    when(service.resolveStudyDarScope(eq(5), eq(USER), any()))
        .thenThrow(new NotFoundException("Study not found"));

    for (String report : reports()) {
      assertEquals(404, get("/api/metrics/" + report + "/study/5" + RANGE));
    }

    verify(service, times(reports().size())).resolveStudyDarScope(eq(5), eq(USER), any());
    verifyNoMoreInteractions(service);
  }

  @Test
  void aDatasetTheCallerCannotReadServesNoReport() {
    when(service.resolveDatasetDarScope(eq(7), eq(USER), any()))
        .thenThrow(new ForbiddenException("User does not have permission"));

    for (String report : reports()) {
      assertEquals(403, get("/api/metrics/" + report + "/dataset/7" + RANGE));
    }

    verify(service, times(reports().size())).resolveDatasetDarScope(eq(7), eq(USER), any());
    verifyNoMoreInteractions(service);
  }

  /**
   * Reports that roll up whole DARs, whose other datasets can belong to other studies, and reports
   * that aren't about DARs, are not served for a study or dataset.
   */
  @ParameterizedTest
  @ValueSource(
      strings = {
        "dar-decisions",
        "dar-decision-turnaround",
        "dar-so-approvals",
        "users",
        "institutions",
        "datasets",
        "studies",
        "elections",
        "dar-terms",
        "DAR-VOLUME",
        "unknown"
      })
  void otherReportsAreNotFoundForAStudyOrDataset(String report) {
    assertEquals(404, get("/api/metrics/" + report + "/study/5" + RANGE));
    assertEquals(404, get("/api/metrics/" + report + "/dataset/7" + RANGE));

    verifyNoInteractions(service);
  }

  @ParameterizedTest
  @CsvSource({
    "?to=2026-03-31",
    "?from=2026-01-01",
    "?from=2026-02-01&to=2026-01-01",
    "?from=01/01/2026&to=2026-03-31",
    "?from=2026-01-01&to=2026-03-31&bucket=year"
  })
  void aBadRangeOrBucketIsRejectedBeforeAnythingIsRead(String query) {
    assertEquals(400, get("/api/metrics/dar-volume/study/5" + query));
    assertEquals(400, get("/api/metrics/dar-renewals/dataset/7" + query));

    verifyNoInteractions(service);
  }

  @Test
  void aNonIntegerIdIsNotFound() {
    assertEquals(404, get("/api/metrics/dar-volume/study/abc" + RANGE));
    assertEquals(404, get("/api/metrics/dar-volume/dataset/abc" + RANGE));

    verifyNoInteractions(service);
  }

  /** The new routes take any report name, so they must not capture the routes beside them. */
  @Test
  void theRoutesBesideThemStillReachTheirOwnHandlers() {
    get("/api/metrics/dar-summaries/study/5");
    get("/api/metrics/research-outputs/study/5");
    get("/api/metrics/dar-summaries/7");
    when(service.resolveDacScope(any(), any())).thenReturn(null);
    get("/api/metrics/dar-volume" + RANGE);

    verify(service).generateStudyDarSummaries(5, USER);
    verify(service).generateStudyResearchOutputs(5, USER);
    verify(service).generateDarSummaries(7, USER);
    verify(service).resolveDacScope(any(), any());
    verify(service).getDarVolume(FROM, TO, null, MetricsBucket.QUARTER, 100, 0);
    verifyNoMoreInteractions(service);
  }

  private static List<String> reports() {
    return List.of(
        "dar-volume",
        "dar-dataset-decisions",
        "dar-dataset-decision-turnaround",
        "dar-expirations",
        "dar-renewals");
  }

  private void stubReports() {
    MetricsBucket bucket = MetricsBucket.QUARTER;
    when(service.getScopedDarVolume(any(), any(), any(), any()))
        .thenReturn(VolumeReport.of(FROM, TO, bucket, List.of(), List.of(), List.of(), List.of()));
    when(service.getScopedDarDatasetDecisions(any(), any(), any(), any()))
        .thenReturn(DecisionReport.of(FROM, TO, bucket, List.of(), List.of()));
    when(service.getScopedDarDatasetDecisionTurnaround(any(), any(), any(), any()))
        .thenReturn(TurnaroundReport.of(FROM, TO, bucket, List.of(), List.of()));
    when(service.getScopedDarExpirations(any(), any(), any(), any()))
        .thenReturn(ExpirationReport.of(FROM, TO, bucket, List.of(), List.of()));
    when(service.getScopedDarRenewals(any(), any(), any(), any()))
        .thenReturn(RenewalReport.of(FROM, TO, bucket, List.of(), List.of()));
  }

  private int get(String pathAndQuery) {
    int queryStart = pathAndQuery.indexOf('?');
    String path = queryStart < 0 ? pathAndQuery : pathAndQuery.substring(0, queryStart);
    var target = resource.target(path);
    if (queryStart >= 0) {
      for (String pair : pathAndQuery.substring(queryStart + 1).split("&")) {
        String[] parts = pair.split("=", 2);
        target = target.queryParam(parts[0], parts[1]);
      }
    }
    try (Response response = target.request().get()) {
      return response.getStatus();
    }
  }
}
