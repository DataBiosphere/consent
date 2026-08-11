package org.broadinstitute.consent.http.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Inject;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import org.broadinstitute.consent.http.db.ResearcherDashboardDAO;
import org.broadinstitute.consent.http.db.ResearcherDashboardDAO.DashboardDatabaseCounts;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.ResearcherDashboardSummary;
import org.broadinstitute.consent.http.models.ResearcherDashboardSummary.DarRequests;
import org.broadinstitute.consent.http.models.ResearcherDashboardSummary.DataLibrary;
import org.broadinstitute.consent.http.models.ResearcherDashboardSummary.DataSubmissions;
import org.broadinstitute.consent.http.models.ResearcherDashboardSummary.DatasetApprovals;
import org.broadinstitute.consent.http.models.User;
import org.jdbi.v3.core.Jdbi;

public class ResearcherDashboardService {

  /** Matches the "Expiring in 30 Days" stat on the My Dataset Approvals tile. */
  private static final int EXPIRING_SOON_DAYS = 30;

  private static final int EXPIRATION_DAYS =
      (int) TimeUnit.MILLISECONDS.toDays(DataAccessRequest.EXPIRATION_DURATION_MILLIS);

  /** Roles that see every study; everyone else sees only publicly visible ones, as in the UI. */
  private static final List<UserRoles> UNRESTRICTED_VISIBILITY_ROLES =
      List.of(
          UserRoles.CHAIRPERSON,
          UserRoles.DATASUBMITTER,
          UserRoles.ADMIN,
          UserRoles.SIGNINGOFFICIAL);

  /** Field on each indexed study holding the asset arrays the tab counts are summed from. */
  private static final String ASSETS_FIELD = "assets";

  /** Asset arrays behind the Data Library's AI Models and Workspaces tiles. */
  private static final List<String> LIBRARY_ASSET_KEYS = List.of("models", "workspaces");

  /**
   * Asset arrays behind the seven study-asset tabs on My Data Submissions. With the Studies and
   * Datasets tabs, these are the nine tabs the Data Submissions total sums.
   */
  private static final List<String> SUBMISSION_ASSET_KEYS =
      List.of(
          "models",
          "workspaces",
          "clinicalTrials",
          "biospecimens",
          "publications",
          "presentations",
          "intellectualProperties");

  /** Counts every dataset in scope, matching the submissions view's showAllControlled behavior. */
  private static final String ALL_DATASETS_FILTER = "{\"match_all\": {}}";

  /** Open/external datasets always count; controlled ones only once approved. */
  private static final String APPROVED_DATASETS_FILTER =
      """
      {
        "bool": {
          "should": [
            {"bool": {"must_not": [{"term": {"accessManagement": "controlled"}}]}},
            {"bool": {"must": [
              {"term": {"accessManagement": "controlled"}},
              {"term": {"dacApproval": true}}
            ]}}
          ],
          "minimum_should_match": 1
        }
      }
      """;

  private final ResearcherDashboardDAO dashboardDAO;
  private final ElasticSearchService elasticSearchService;
  private final ExecutorService executorService;

  @Inject
  public ResearcherDashboardService(
      Jdbi jdbi, ElasticSearchService elasticSearchService, ExecutorService executorService) {
    this.dashboardDAO = jdbi.onDemand(ResearcherDashboardDAO.class);
    this.elasticSearchService = elasticSearchService;
    this.executorService = executorService;
  }

  public ResearcherDashboardSummary getSummary(User user) {
    CompletableFuture<DashboardDatabaseCounts> databaseCounts =
        CompletableFuture.supplyAsync(
            () -> dashboardDAO.getCounts(user.getUserId(), EXPIRATION_DAYS, EXPIRING_SOON_DAYS),
            executorService);
    CompletableFuture<DataLibrary> libraryCounts =
        CompletableFuture.supplyAsync(() -> getDataLibraryCounts(user), executorService);
    CompletableFuture<DataSubmissions> submissionCounts =
        CompletableFuture.supplyAsync(() -> getDataSubmissionCounts(user), executorService);
    try {
      DashboardDatabaseCounts db = databaseCounts.join();
      long inProcess = db.darTotal() - db.darApproved() - db.darCanceled();
      return new ResearcherDashboardSummary(
          libraryCounts.join(),
          new DarRequests(db.darTotal(), db.darApproved(), db.darCanceled(), inProcess),
          new DatasetApprovals(
              db.approvalsActive(), db.approvalsExpiringSoon(), db.approvalsExpired()),
          submissionCounts.join());
    } catch (CompletionException e) {
      if (e.getCause() instanceof RuntimeException runtimeException) throw runtimeException;
      throw e;
    }
  }

  /**
   * Counts have to match the tab badges at /datalibrary, so this reuses that library's query.
   * Models and workspaces live in each study's assets, so they are summed once per study from the
   * same study aggregation the library's own tab counts use.
   */
  private DataLibrary getDataLibraryCounts(User user) {
    String visibilityClause =
        isRestrictedToPublicVisibility(user)
            ? ", {\"term\": {\"study.publicVisibility\": true}}"
            : "";
    TabCounts counts =
        countTabs(
            visibilityClause,
            APPROVED_DATASETS_FILTER,
            LIBRARY_ASSET_KEYS,
            "Unable to load data library statistics");
    return new DataLibrary(
        counts.studies(), counts.datasets(), counts.assets().get(0), counts.assets().get(1));
  }

  /**
   * Sums what My Data Submissions lists, one tab at a time: studies, datasets, and the seven
   * study-asset tabs. Scoped to datasets the researcher registered or is data custodian for, and
   * counting controlled datasets whether or not they are approved, as that page does.
   */
  private DataSubmissions getDataSubmissionCounts(User user) {
    if (!user.hasUserRole(UserRoles.DATASUBMITTER)) {
      return new DataSubmissions(0);
    }
    String ownershipClause =
        """
        , {
          "bool": {
            "should": [
              {"term": {"createUserId": %d}},
              {"term": {"study.dataSubmitterId": %d}},
              {"term": {"study.dataCustodianEmail": "%s"}}
            ],
            "minimum_should_match": 1
          }
        }
        """
            .formatted(user.getUserId(), user.getUserId(), escapeJson(user.getEmail()));
    TabCounts counts =
        countTabs(
            ownershipClause,
            ALL_DATASETS_FILTER,
            SUBMISSION_ASSET_KEYS,
            "Unable to load data submission statistics");
    long total =
        counts.studies()
            + counts.datasets()
            + counts.assets().stream().mapToLong(Long::longValue).sum();
    return new DataSubmissions(total);
  }

  /**
   * Runs one library-shaped search and returns the count each tab would show: distinct studies,
   * datasets matching {@code datasetsFilter}, and the length of each requested asset array summed
   * across studies (in the order requested).
   */
  private TabCounts countTabs(
      String extraMustClauses,
      String datasetsFilter,
      List<String> assetKeys,
      String failureMessage) {
    String assetSources =
        String.join(", ", assetKeys.stream().map("\"study.assets.%s\""::formatted).toList());
    String query =
        """
        {
          "size": 0,
          "track_total_hits": true,
          "query": {"bool": {"must": [{"exists": {"field": "study"}}%s]}},
          "aggs": {
            "total_studies": {"cardinality": {"field": "study.studyId"}},
            "datasets_count": {"filter": %s},
            "studies": {
              "terms": {"field": "study.studyId", "size": 10000},
              "aggs": {"study_details": {"top_hits": {"size": 1, "_source": [%s]}}}
            }
          }
        }
        """
            .formatted(extraMustClauses, datasetsFilter, assetSources);
    JsonObject aggregations = search(query, failureMessage).getAsJsonObject("aggregations");
    List<Long> assetTotals = new ArrayList<>(Collections.nCopies(assetKeys.size(), 0L));
    for (JsonElement bucket : aggregations.getAsJsonObject("studies").getAsJsonArray("buckets")) {
      JsonObject assets = studyAssets(bucket.getAsJsonObject());
      for (int i = 0; i < assetKeys.size(); i++) {
        assetTotals.set(i, assetTotals.get(i) + assetCount(assets, assetKeys.get(i)));
      }
    }
    return new TabCounts(
        aggregations.getAsJsonObject("total_studies").get("value").getAsLong(),
        aggregations.getAsJsonObject("datasets_count").get("doc_count").getAsLong(),
        assetTotals);
  }

  private record TabCounts(long studies, long datasets, List<Long> assets) {}

  private JsonObject search(String query, String failureMessage) {
    try (InputStream stream = elasticSearchService.searchDatasetsStream(query);
        InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
      return JsonParser.parseReader(reader).getAsJsonObject();
    } catch (Exception e) {
      throw new IllegalStateException(failureMessage, e);
    }
  }

  private JsonObject studyAssets(JsonObject bucket) {
    JsonArray hits =
        bucket.getAsJsonObject("study_details").getAsJsonObject("hits").getAsJsonArray("hits");
    if (hits.isEmpty()) {
      return null;
    }
    JsonObject source = hits.get(0).getAsJsonObject().getAsJsonObject("_source");
    if (source == null || !source.has("study")) {
      return null;
    }
    JsonObject study = source.getAsJsonObject("study");
    return study.has(ASSETS_FIELD) && study.get(ASSETS_FIELD).isJsonObject()
        ? study.getAsJsonObject(ASSETS_FIELD)
        : null;
  }

  private long assetCount(JsonObject assets, String assetName) {
    if (assets == null || !assets.has(assetName) || !assets.get(assetName).isJsonArray()) {
      return 0;
    }
    return assets.getAsJsonArray(assetName).size();
  }

  private boolean isRestrictedToPublicVisibility(User user) {
    return !user.hasAnyUserRole(UNRESTRICTED_VISIBILITY_ROLES);
  }

  private String escapeJson(String value) {
    return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
  }
}
