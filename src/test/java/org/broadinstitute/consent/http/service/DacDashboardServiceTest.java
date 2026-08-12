package org.broadinstitute.consent.http.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.MoreExecutors;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import org.broadinstitute.consent.http.db.DacDashboardDAO;
import org.broadinstitute.consent.http.db.DacDashboardDAO.DashboardDatabaseCounts;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.DacDashboardSummary;
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
class DacDashboardServiceTest {

  @Mock private Jdbi jdbi;
  @Mock private DacDashboardDAO dashboardDAO;
  @Mock private ElasticSearchService elasticSearchService;

  private ExecutorService executorService;
  private DacDashboardService service;

  @BeforeEach
  void setUp() {
    executorService = MoreExecutors.newDirectExecutorService();
    when(jdbi.onDemand(DacDashboardDAO.class)).thenReturn(dashboardDAO);
    service =
        new DacDashboardService(
            jdbi, new DashboardSearchService(elasticSearchService), executorService);
  }

  @AfterEach
  void tearDown() {
    executorService.shutdownNow();
  }

  @Test
  void returnsChairCountsFromOneDatabaseAndOneSearchRequest() throws Exception {
    User user = userWithRole(UserRoles.CHAIRPERSON, 10, 42);
    UserRole duplicateDacRole = UserRoles.Member();
    duplicateDacRole.setDacId(42);
    user.getRoles().add(duplicateDacRole);
    UserRole otherDacRole = UserRoles.Member();
    otherDacRole.setDacId(43);
    user.getRoles().add(otherDacRole);
    when(dashboardDAO.getCounts(
            10, UserRoles.CHAIRPERSON.getRoleId(), UserRoles.MEMBER.getRoleId()))
        .thenReturn(new DashboardDatabaseCounts(5, 8, 3, 2));
    ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
    when(elasticSearchService.searchDatasetsStream(queryCaptor.capture()))
        .thenReturn(searchResponseWithAssets());

    DacDashboardSummary result = service.getSummary(user);

    assertEquals(8, result.darRequests().total());
    assertEquals(3, result.darRequests().approved());
    assertEquals(5, result.darRequests().pending());
    assertEquals(2, result.darRequests().awaitingMyVote());
    assertEquals(5, result.dacs().total());
    assertEquals(7, result.dacDatasets().total());
    assertEquals(3, result.dataLibrary().studies());
    assertEquals(4, result.dataLibrary().datasets());
    assertEquals(2, result.dataLibrary().models());
    assertEquals(1, result.dataLibrary().workspaces());
    String query = queryCaptor.getValue();
    assertTrue(query.contains("\"dacId\": [42,43]"));
    assertTrue(query.contains("\"study.publicVisibility\": true"));
    assertTrue(query.contains("\"minimum_should_match\": 1"));
  }

  @Test
  void includesPublicAndAssignedPrivateStudiesForMembersAndOmitsChairOnlyCounts() throws Exception {
    User user = userWithRole(UserRoles.MEMBER, 11, 44);
    when(dashboardDAO.getCounts(
            11, UserRoles.CHAIRPERSON.getRoleId(), UserRoles.MEMBER.getRoleId()))
        .thenReturn(new DashboardDatabaseCounts(99, 3, 1, 1));
    ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
    when(elasticSearchService.searchDatasetsStream(queryCaptor.capture()))
        .thenReturn(emptySearchResponse(88));

    DacDashboardSummary result = service.getSummary(user);

    assertEquals(0, result.dacs().total());
    assertEquals(0, result.dacDatasets().total());
    assertEquals(2, result.darRequests().pending());
    assertTrue(queryCaptor.getValue().contains("\"study.publicVisibility\": true"));
    assertTrue(queryCaptor.getValue().contains("\"dacId\": [44]"));
    assertTrue(queryCaptor.getValue().contains("\"minimum_should_match\": 1"));
    assertFalse(queryCaptor.getValue().contains("all_datasets"));
  }

  @Test
  void usesMatchNoneWhenAChairHasNoDacAssociations() throws Exception {
    UserRole chairRole =
        new UserRole(UserRoles.CHAIRPERSON.getRoleId(), UserRoles.CHAIRPERSON.getRoleName());
    User user = new User();
    user.setUserId(12);
    user.setRoles(new ArrayList<>(List.of(chairRole)));
    when(dashboardDAO.getCounts(
            12, UserRoles.CHAIRPERSON.getRoleId(), UserRoles.MEMBER.getRoleId()))
        .thenReturn(new DashboardDatabaseCounts(0, 0, 0, 0));
    ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
    when(elasticSearchService.searchDatasetsStream(queryCaptor.capture()))
        .thenReturn(emptySearchResponse(0));

    service.getSummary(user);

    assertTrue(queryCaptor.getValue().contains("\"match_none\""));
  }

  @Test
  void treatsMissingOrMalformedStudyAssetsAsEmpty() throws Exception {
    User user = userWithRole(UserRoles.CHAIRPERSON, 13, 45);
    when(dashboardDAO.getCounts(
            13, UserRoles.CHAIRPERSON.getRoleId(), UserRoles.MEMBER.getRoleId()))
        .thenReturn(new DashboardDatabaseCounts(0, 0, 0, 0));
    when(elasticSearchService.searchDatasetsStream(anyString()))
        .thenReturn(searchResponseWithMissingAssets());

    DacDashboardSummary result = service.getSummary(user);

    assertEquals(0, result.dataLibrary().models());
    assertEquals(0, result.dataLibrary().workspaces());
  }

  @Test
  void failsWholeSummaryWhenSearchAggregationFails() throws Exception {
    User user = userWithRole(UserRoles.CHAIRPERSON, 14, 46);
    when(dashboardDAO.getCounts(
            14, UserRoles.CHAIRPERSON.getRoleId(), UserRoles.MEMBER.getRoleId()))
        .thenReturn(new DashboardDatabaseCounts(0, 0, 0, 0));
    when(elasticSearchService.searchDatasetsStream(anyString()))
        .thenThrow(new IOException("Elasticsearch unavailable"));

    IllegalStateException exception =
        assertThrows(IllegalStateException.class, () -> service.getSummary(user));

    assertEquals("Unable to load DAC dashboard search statistics", exception.getMessage());
  }

  @Test
  void preservesCompletionExceptionForNonRuntimeAsyncFailure() throws Exception {
    User user = userWithRole(UserRoles.CHAIRPERSON, 15, 47);
    when(dashboardDAO.getCounts(
            15, UserRoles.CHAIRPERSON.getRoleId(), UserRoles.MEMBER.getRoleId()))
        .thenThrow(new AssertionError("database failure"));
    when(elasticSearchService.searchDatasetsStream(anyString())).thenReturn(emptySearchResponse(0));

    assertThrows(CompletionException.class, () -> service.getSummary(user));
  }

  private static User userWithRole(UserRoles role, int userId, int dacId) {
    UserRole userRole = new UserRole(role.getRoleId(), role.getRoleName());
    userRole.setDacId(dacId);
    User user = new User();
    user.setUserId(userId);
    user.setRoles(new ArrayList<>(List.of(userRole)));
    return user;
  }

  private static ByteArrayInputStream searchResponseWithAssets() {
    String response =
        """
        {"aggregations":{
          "total_studies":{"value":3},
          "datasets_count":{"doc_count":4},
          "studies":{"buckets":[
            {"study_details":{"hits":{"hits":[{"_source":{"study":{"assets":{
              "models":[{},{}],"workspaces":[{}]
            }}}}]}}}
          ]},
          "all_datasets":{"my_dac_datasets":{"doc_count":7}}
        }}
        """;
    return new ByteArrayInputStream(response.getBytes(StandardCharsets.UTF_8));
  }

  private static ByteArrayInputStream searchResponseWithMissingAssets() {
    String response =
        """
        {"aggregations":{
          "total_studies":{"value":4},
          "datasets_count":{"doc_count":0},
          "studies":{"buckets":[
            {"study_details":{"hits":{"hits":[]}}},
            {"study_details":{"hits":{"hits":[{}]}}},
            {"study_details":{"hits":{"hits":[{"_source":{}}]}}},
            {"study_details":{"hits":{"hits":[{"_source":{"study":{}}}]}}},
            {"study_details":{"hits":{"hits":[{"_source":{"study":{"assets":"invalid"}}}]}}},
            {"study_details":{"hits":{"hits":[{"_source":{"study":{"assets":{
              "models":"invalid","other":[]
            }}}}]}}}
          ]},
          "all_datasets":{"my_dac_datasets":{"doc_count":0}}
        }}
        """;
    return new ByteArrayInputStream(response.getBytes(StandardCharsets.UTF_8));
  }

  private static ByteArrayInputStream emptySearchResponse(long dacDatasets) {
    String response =
        """
        {"aggregations":{
          "total_studies":{"value":0},
          "datasets_count":{"doc_count":0},
          "studies":{"buckets":[]},
          "all_datasets":{"my_dac_datasets":{"doc_count":%d}}
        }}
        """
            .formatted(dacDatasets);
    return new ByteArrayInputStream(response.getBytes(StandardCharsets.UTF_8));
  }
}
