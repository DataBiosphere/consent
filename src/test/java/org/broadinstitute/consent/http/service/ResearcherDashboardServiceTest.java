package org.broadinstitute.consent.http.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import org.broadinstitute.consent.http.db.ResearcherDashboardDAO;
import org.broadinstitute.consent.http.db.ResearcherDashboardDAO.DashboardDatabaseCounts;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.ResearcherDashboardSummary;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResearcherDashboardServiceTest {

  private static final String LIBRARY_RESPONSE =
      """
      {
        "hits": {"total": {"value": 40}},
        "aggregations": {
          "total_studies": {"value": 7},
          "datasets_count": {"doc_count": 12},
          "studies": {"buckets": [
            {"key": 1, "study_details": {"hits": {"hits": [
              {"_source": {"study": {"assets": {"models": [{}, {}], "workspaces": [{}]}}}}
            ]}}},
            {"key": 2, "study_details": {"hits": {"hits": [
              {"_source": {"study": {"assets": {"models": [{}]}}}}
            ]}}},
            {"key": 3, "study_details": {"hits": {"hits": [
              {"_source": {"study": {}}}
            ]}}}
          ]}
        }
      }
      """;

  // Two studies and five datasets, plus eleven study assets spread across the seven asset tabs.
  private static final String SUBMISSIONS_RESPONSE =
      """
      {
        "hits": {"total": {"value": 5}},
        "aggregations": {
          "total_studies": {"value": 2},
          "datasets_count": {"doc_count": 5},
          "studies": {"buckets": [
            {"key": 1, "study_details": {"hits": {"hits": [
              {"_source": {"study": {"assets": {
                "models": [{}, {}],
                "workspaces": [{}],
                "clinicalTrials": [{}],
                "biospecimens": [{}, {}],
                "publications": [{}],
                "presentations": [{}],
                "intellectualProperties": [{}]
              }}}}
            ]}}},
            {"key": 2, "study_details": {"hits": {"hits": [
              {"_source": {"study": {"assets": {"publications": [{}, {}]}}}}
            ]}}}
          ]}
        }
      }
      """;

  @Mock private Jdbi jdbi;
  @Mock private ResearcherDashboardDAO dashboardDAO;
  @Mock private ElasticSearchService elasticSearchService;

  private ExecutorService executorService;
  private ResearcherDashboardService service;
  private User user;

  @BeforeEach
  void setUp() {
    executorService = MoreExecutors.newDirectExecutorService();
    when(jdbi.onDemand(ResearcherDashboardDAO.class)).thenReturn(dashboardDAO);
    service = new ResearcherDashboardService(jdbi, elasticSearchService, executorService);
    user = new User();
    user.setUserId(10);
    user.setEmail("researcher@example.org");
    user.setRoles(
        List.of(
            new UserRole(UserRoles.RESEARCHER.getRoleId(), UserRoles.RESEARCHER.getRoleName())));
  }

  @AfterEach
  void tearDown() {
    executorService.shutdownNow();
  }

  private static InputStream stream(String json) {
    return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
  }

  private void stubDatabaseCounts(DashboardDatabaseCounts counts) {
    when(dashboardDAO.getCounts(eq(10), anyInt(), anyInt())).thenReturn(counts);
  }

  private void stubLibrarySearch() throws IOException {
    when(elasticSearchService.searchDatasetsStream(
            argThat(q -> q != null && q.contains("total_studies"))))
        .thenReturn(stream(LIBRARY_RESPONSE));
  }

  private void stubSubmissionSearch() throws IOException {
    when(elasticSearchService.searchDatasetsStream(
            argThat(q -> q != null && q.contains("dataSubmitterId"))))
        .thenReturn(stream(SUBMISSIONS_RESPONSE));
  }

  private void makeDataSubmitter() {
    user.setRoles(
        List.of(
            new UserRole(UserRoles.RESEARCHER.getRoleId(), UserRoles.RESEARCHER.getRoleName()),
            new UserRole(
                UserRoles.DATASUBMITTER.getRoleId(), UserRoles.DATASUBMITTER.getRoleName())));
  }

  @Test
  void combinesDatabaseLibraryAndSubmissionCounts() throws Exception {
    makeDataSubmitter();
    stubDatabaseCounts(new DashboardDatabaseCounts(8, 3, 1, 6, 2, 5));
    stubLibrarySearch();
    stubSubmissionSearch();

    ResearcherDashboardSummary summary = service.getSummary(user);

    assertEquals(7, summary.dataLibrary().studies());
    assertEquals(12, summary.dataLibrary().datasets());
    // Summed once per study, skipping studies with no assets.
    assertEquals(3, summary.dataLibrary().models());
    assertEquals(1, summary.dataLibrary().workspaces());
    assertEquals(8, summary.darRequests().total());
    assertEquals(3, summary.darRequests().approved());
    assertEquals(1, summary.darRequests().canceled());
    assertEquals(4, summary.darRequests().inProcess());
    assertEquals(6, summary.datasetApprovals().active());
    assertEquals(2, summary.datasetApprovals().expiringSoon());
    assertEquals(5, summary.datasetApprovals().expired());
    // Studies (2) + datasets (5) + the seven asset tabs (11), as the submissions page tabs count.
    assertEquals(18, summary.dataSubmissions().total());
  }

  @Test
  void countsExpiredApprovalsSeparatelyFromActiveOnes() throws Exception {
    stubDatabaseCounts(new DashboardDatabaseCounts(0, 0, 0, 4, 1, 11));
    stubLibrarySearch();

    ResearcherDashboardSummary summary = service.getSummary(user);

    assertEquals(4, summary.datasetApprovals().active());
    assertEquals(11, summary.datasetApprovals().expired());
  }

  @Test
  void restrictsLibraryToPublicStudiesForUnprivilegedResearcher() throws Exception {
    stubDatabaseCounts(new DashboardDatabaseCounts(0, 0, 0, 0, 0, 0));
    stubLibrarySearch();

    service.getSummary(user);

    ArgumentCaptor<String> query = ArgumentCaptor.forClass(String.class);
    verify(elasticSearchService).searchDatasetsStream(query.capture());
    assertTrue(query.getValue().contains("study.publicVisibility"));
  }

  @Test
  void showsAllStudiesToPrivilegedUser() throws Exception {
    makeDataSubmitter();
    stubDatabaseCounts(new DashboardDatabaseCounts(0, 0, 0, 0, 0, 0));
    stubLibrarySearch();
    stubSubmissionSearch();

    service.getSummary(user);

    ArgumentCaptor<String> queries = ArgumentCaptor.forClass(String.class);
    verify(elasticSearchService, times(2)).searchDatasetsStream(queries.capture());
    assertFalse(
        queries.getAllValues().stream().anyMatch(q -> q.contains("study.publicVisibility")));
  }

  @Test
  void skipsSubmissionSearchForNonSubmitters() throws Exception {
    stubDatabaseCounts(new DashboardDatabaseCounts(0, 0, 0, 0, 0, 0));
    stubLibrarySearch();

    ResearcherDashboardSummary summary = service.getSummary(user);

    assertEquals(0, summary.dataSubmissions().total());
    verify(elasticSearchService, never())
        .searchDatasetsStream(argThat(q -> q != null && q.contains("dataSubmitterId")));
  }

  @Test
  void treatsStudiesWithMissingOrMalformedAssetsAsZero() throws Exception {
    // Studies with no top hit, no _source, no study, a non-object assets field, and a non-array
    // asset all have to count as nothing rather than fail the whole dashboard.
    String response =
        """
        {
          "aggregations": {
            "total_studies": {"value": 5},
            "datasets_count": {"doc_count": 5},
            "studies": {"buckets": [
              {"key": 1, "study_details": {"hits": {"hits": []}}},
              {"key": 2, "study_details": {"hits": {"hits": [{"_source": {}}]}}},
              {"key": 5, "study_details": {"hits": {"hits": [{}]}}},
              {"key": 3, "study_details": {"hits": {"hits": [
                {"_source": {"study": {"assets": "none"}}}
              ]}}},
              {"key": 4, "study_details": {"hits": {"hits": [
                {"_source": {"study": {"assets": {"models": 3, "workspaces": [{}]}}}}
              ]}}}
            ]}
          }
        }
        """;
    stubDatabaseCounts(new DashboardDatabaseCounts(0, 0, 0, 0, 0, 0));
    when(elasticSearchService.searchDatasetsStream(anyString())).thenReturn(stream(response));

    ResearcherDashboardSummary summary = service.getSummary(user);

    assertEquals(0, summary.dataLibrary().models());
    assertEquals(1, summary.dataLibrary().workspaces());
  }

  @Test
  void keepsTheSubmissionsQueryValidWhenAnEmailNeedsEscaping() throws Exception {
    makeDataSubmitter();
    user.setEmail("od\"d\\na\tme\n@example.org");
    stubDatabaseCounts(new DashboardDatabaseCounts(0, 0, 0, 0, 0, 0));
    stubLibrarySearch();
    stubSubmissionSearch();

    service.getSummary(user);

    ArgumentCaptor<String> queries = ArgumentCaptor.forClass(String.class);
    verify(elasticSearchService, times(2)).searchDatasetsStream(queries.capture());
    String submissionQuery =
        queries.getAllValues().stream()
            .filter(q -> q.contains("dataSubmitterId"))
            .findFirst()
            .orElseThrow();
    JsonObject parsed =
        assertDoesNotThrow(() -> JsonParser.parseString(submissionQuery).getAsJsonObject());
    assertEquals(user.getEmail(), custodianEmailTerm(parsed));
  }

  /** Reads back the email the submissions query filters on, as Elastic Search would parse it. */
  private String custodianEmailTerm(JsonObject query) {
    JsonArray must = query.getAsJsonObject("query").getAsJsonObject("bool").getAsJsonArray("must");
    for (JsonElement clause : must) {
      if (!clause.getAsJsonObject().has("bool")) {
        continue;
      }
      for (JsonElement should :
          clause.getAsJsonObject().getAsJsonObject("bool").getAsJsonArray("should")) {
        JsonObject term = should.getAsJsonObject().getAsJsonObject("term");
        if (term != null && term.has("study.dataCustodianEmail")) {
          return term.get("study.dataCustodianEmail").getAsString();
        }
      }
    }
    throw new AssertionError("no dataCustodianEmail term in query");
  }

  @Test
  void toleratesADataSubmitterWithNoEmail() throws Exception {
    makeDataSubmitter();
    user.setEmail(null);
    stubDatabaseCounts(new DashboardDatabaseCounts(0, 0, 0, 0, 0, 0));
    stubLibrarySearch();
    stubSubmissionSearch();

    assertEquals(18, service.getSummary(user).dataSubmissions().total());
  }

  @Test
  void wrapsElasticSearchFailures() throws Exception {
    stubDatabaseCounts(new DashboardDatabaseCounts(0, 0, 0, 0, 0, 0));
    when(elasticSearchService.searchDatasetsStream(anyString()))
        .thenThrow(new IOException("index unavailable"));

    IllegalStateException thrown =
        assertThrows(IllegalStateException.class, () -> service.getSummary(user));
    assertTrue(thrown.getMessage().contains("data library statistics"));
  }
}
