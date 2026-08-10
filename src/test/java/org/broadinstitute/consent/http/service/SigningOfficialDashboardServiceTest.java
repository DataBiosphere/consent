package org.broadinstitute.consent.http.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.MoreExecutors;
import jakarta.ws.rs.BadRequestException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import org.broadinstitute.consent.http.db.SigningOfficialDashboardDAO;
import org.broadinstitute.consent.http.db.SigningOfficialDashboardDAO.DashboardDatabaseCounts;
import org.broadinstitute.consent.http.models.SigningOfficialDashboardSummary;
import org.broadinstitute.consent.http.models.User;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SigningOfficialDashboardServiceTest {
  @Mock private Jdbi jdbi;
  @Mock private SigningOfficialDashboardDAO dashboardDAO;
  @Mock private ElasticSearchService elasticSearchService;

  private ExecutorService executorService;
  private SigningOfficialDashboardService service;
  private User user;

  @BeforeEach
  void setUp() {
    executorService = MoreExecutors.newDirectExecutorService();
    when(jdbi.onDemand(SigningOfficialDashboardDAO.class)).thenReturn(dashboardDAO);
    service = new SigningOfficialDashboardService(jdbi, elasticSearchService, executorService);
    user = new User();
    user.setUserId(10);
    user.setInstitutionId(42);
    user.setEmail("so@example.org");
  }

  @AfterEach
  void tearDown() {
    executorService.shutdownNow();
  }

  @Test
  void returnsCombinedDatabaseAndLibraryCounts() throws Exception {
    when(dashboardDAO.getCounts(42, "10", "so@example.org"))
        .thenReturn(new DashboardDatabaseCounts(3, 2, 8, 4, 1, 5, 2, 6, 9, 4));
    String esResponse =
        """
        {"hits":{"total":{"value":12}},"aggregations":{"total_studies":{"value":7}}}
        """;
    when(elasticSearchService.searchDatasetsStream(org.mockito.ArgumentMatchers.anyString()))
        .thenReturn(new ByteArrayInputStream(esResponse.getBytes(StandardCharsets.UTF_8)));

    SigningOfficialDashboardSummary result = service.getSummary(user);

    assertEquals(3, result.researcherStatus().active());
    assertEquals(8, result.darRequests().total());
    assertEquals(4, result.darRequests().approved());
    assertEquals(1, result.darRequests().canceled());
    assertEquals(3, result.darRequests().inProcess());
    assertEquals(12, result.institutionLibrary().datasets());
    assertEquals(7, result.institutionLibrary().studies());
    assertEquals(4, result.daaAssociations().researchersApproved());
  }

  @Test
  void countsDatasetsWithoutNarrowingSoTheyMatchTheInstitutionDataLibraryPage() throws Exception {
    when(dashboardDAO.getCounts(42, "10", "so@example.org"))
        .thenReturn(new DashboardDatabaseCounts(0, 0, 0, 0, 0, 0, 0, 0, 0, 0));
    ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
    String esResponse =
        """
        {"hits":{"total":{"value":0}},"aggregations":{"total_studies":{"value":0}}}
        """;
    when(elasticSearchService.searchDatasetsStream(queryCaptor.capture()))
        .thenReturn(new ByteArrayInputStream(esResponse.getBytes(StandardCharsets.UTF_8)));

    service.getSummary(user);

    String query = queryCaptor.getValue();
    assertTrue(query.contains("\"submitter.institution.id\": 42"));
    assertTrue(query.contains("\"track_total_hits\": true"));
    assertFalse(query.contains("dacApproval"));
    assertFalse(query.contains("accessManagement"));
    assertFalse(query.contains("exists"));
  }

  @Test
  void rejectsSigningOfficialWithoutInstitution() {
    user.setInstitutionId(null);
    assertThrows(BadRequestException.class, () -> service.getSummary(user));
  }

  @Test
  void failsWholeSummaryWhenLibraryAggregationFails() throws Exception {
    when(dashboardDAO.getCounts(42, "10", "so@example.org"))
        .thenReturn(new DashboardDatabaseCounts(0, 0, 0, 0, 0, 0, 0, 0, 0, 0));
    when(elasticSearchService.searchDatasetsStream(org.mockito.ArgumentMatchers.anyString()))
        .thenThrow(new IOException("Elasticsearch unavailable"));

    assertThrows(IllegalStateException.class, () -> service.getSummary(user));
  }

  @Test
  void preservesCompletionExceptionForNonRuntimeAsyncFailure() throws Exception {
    when(dashboardDAO.getCounts(42, "10", "so@example.org"))
        .thenThrow(new AssertionError("database failure"));
    String esResponse =
        """
        {"hits":{"total":{"value":0}},"aggregations":{"total_studies":{"value":0}}}
        """;
    when(elasticSearchService.searchDatasetsStream(org.mockito.ArgumentMatchers.anyString()))
        .thenReturn(new ByteArrayInputStream(esResponse.getBytes(StandardCharsets.UTF_8)));

    assertThrows(CompletionException.class, () -> service.getSummary(user));
  }
}
