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
import java.util.Objects;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.DashboardSummary.DataLibrary;
import org.broadinstitute.consent.http.models.DashboardSummary.InstitutionLibrary;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.util.gson.GsonUtil;

/**
 * Runs and parses the Elasticsearch aggregations shared by the role-specific dashboards.
 *
 * <p>Keeping these queries together prevents the Data Library counts and malformed-asset handling
 * from drifting as new dashboards are added.
 */
public class DashboardSearchService {

  private static final String ASSETS_FIELD = "assets";
  private static final List<String> LIBRARY_ASSET_KEYS = List.of("models", "workspaces");
  private static final List<String> SUBMISSION_ASSET_KEYS =
      List.of(
          "models",
          "workspaces",
          "clinicalTrials",
          "biospecimens",
          "publications",
          "presentations",
          "intellectualProperties");
  private static final List<UserRoles> UNRESTRICTED_VISIBILITY_ROLES =
      List.of(
          UserRoles.CHAIRPERSON,
          UserRoles.DATASUBMITTER,
          UserRoles.ADMIN,
          UserRoles.SIGNINGOFFICIAL);

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

  private final ElasticSearchService elasticSearchService;

  @Inject
  public DashboardSearchService(ElasticSearchService elasticSearchService) {
    this.elasticSearchService = elasticSearchService;
  }

  public DataLibrary getDataLibraryCounts(User user) {
    return getDataLibraryCounts(isRestrictedToPublicVisibility(user));
  }

  public DataLibrary getDataLibraryCounts(boolean restrictToPublicVisibility) {
    String visibilityClause =
        restrictToPublicVisibility ? ", {\"term\": {\"study.publicVisibility\": true}}" : "";
    return countTabs(
            visibilityClause,
            APPROVED_DATASETS_FILTER,
            LIBRARY_ASSET_KEYS,
            "",
            "Unable to load data library statistics")
        .dataLibrary();
  }

  /** Sums the nine tabs shown by My Data Submissions for records owned or managed by the user. */
  public long getDataSubmissionCount(User user) {
    if (!user.hasUserRole(UserRoles.DATASUBMITTER)) {
      return 0;
    }
    String ownershipClause =
        """
        , {
          "bool": {
            "should": [
              {"term": {"createUserId": %d}},
              {"term": {"study.dataSubmitterId": %d}},
              {"term": {"study.dataCustodianEmail": %s}}
            ],
            "minimum_should_match": 1
          }
        }
        """
            .formatted(user.getUserId(), user.getUserId(), toJsonString(user.getEmail()));
    TabCounts counts =
        countTabs(
            ownershipClause,
            ALL_DATASETS_FILTER,
            SUBMISSION_ASSET_KEYS,
            "",
            "Unable to load data submission statistics");
    return counts.dataLibrary().studies()
        + counts.dataLibrary().datasets()
        + counts.assetTotals().stream().mapToLong(Long::longValue).sum();
  }

  /**
   * Gets Data Library counts scoped to public studies or the caller's DACs and, for chairs only,
   * the dataset count shown by My DAC's Datasets in one Elasticsearch request. Members skip the
   * global chair-only aggregation entirely.
   */
  public DacSearchCounts getDacSearchCounts(boolean isChair, List<Integer> dacIds) {
    String dacIdsJson = String.join(",", dacIds.stream().map(String::valueOf).toList());
    String dacDatasetAggregation = "";
    if (isChair) {
      String dacDatasetFilter =
          dacIds.isEmpty()
              ? "{\"match_none\": {}}"
              : "{\"terms\": {\"dacId\": [%s]}}".formatted(dacIdsJson);
      dacDatasetAggregation =
          """
          ,
          "all_datasets": {
            "global": {},
            "aggs": {"my_dac_datasets": {"filter": %s}}
          }
          """
              .formatted(dacDatasetFilter);
    }

    String dacVisibilityOption =
        dacIds.isEmpty() ? "" : ", {\"terms\": {\"dacId\": [%s]}}".formatted(dacIdsJson);
    String visibilityClause =
        """
        , {
          "bool": {
            "should": [
              {"term": {"study.publicVisibility": true}}%s
            ],
            "minimum_should_match": 1
          }
        }
        """
            .formatted(dacVisibilityOption);
    TabCounts counts =
        countTabs(
            visibilityClause,
            APPROVED_DATASETS_FILTER,
            LIBRARY_ASSET_KEYS,
            dacDatasetAggregation,
            "Unable to load DAC dashboard search statistics");
    long dacDatasets =
        isChair
            ? counts
                .aggregations()
                .getAsJsonObject("all_datasets")
                .getAsJsonObject("my_dac_datasets")
                .get("doc_count")
                .getAsLong()
            : 0;
    return new DacSearchCounts(counts.dataLibrary(), dacDatasets);
  }

  public InstitutionLibrary getInstitutionLibraryCounts(Integer institutionId) {
    String query =
        """
        {
          "size": 0,
          "track_total_hits": true,
          "query": {"match_phrase": {"submitter.institution.id": %d}},
          "aggs": {"total_studies": {"cardinality": {"field": "study.studyId"}}}
        }
        """
            .formatted(institutionId);
    JsonObject response = search(query, "Unable to load institution library statistics");
    long datasets =
        response.getAsJsonObject("hits").getAsJsonObject("total").get("value").getAsLong();
    long studies =
        response
            .getAsJsonObject("aggregations")
            .getAsJsonObject("total_studies")
            .get("value")
            .getAsLong();
    return new InstitutionLibrary(datasets, studies);
  }

  private TabCounts countTabs(
      String extraMustClauses,
      String datasetsFilter,
      List<String> assetKeys,
      String additionalAggregations,
      String failureMessage) {
    String assetSources =
        String.join(", ", assetKeys.stream().map("\"study.assets.%s\""::formatted).toList());
    String query =
        """
        {
          "size": 0,
          "query": {"bool": {"must": [{"exists": {"field": "study"}}%s]}},
          "aggs": {
            "total_studies": {"cardinality": {"field": "study.studyId"}},
            "datasets_count": {"filter": %s},
            "studies": {
              "terms": {"field": "study.studyId", "size": 10000},
              "aggs": {"study_details": {"top_hits": {"size": 1, "_source": [%s]}}}
            }%s
          }
        }
        """
            .formatted(extraMustClauses, datasetsFilter, assetSources, additionalAggregations);
    JsonObject aggregations = search(query, failureMessage).getAsJsonObject("aggregations");
    List<Long> assetTotals = new ArrayList<>(Collections.nCopies(assetKeys.size(), 0L));
    for (JsonElement bucket : aggregations.getAsJsonObject("studies").getAsJsonArray("buckets")) {
      JsonObject assets = studyAssets(bucket.getAsJsonObject());
      for (int i = 0; i < assetKeys.size(); i++) {
        assetTotals.set(i, assetTotals.get(i) + assetCount(assets, assetKeys.get(i)));
      }
    }
    DataLibrary dataLibrary =
        new DataLibrary(
            aggregations.getAsJsonObject("total_studies").get("value").getAsLong(),
            aggregations.getAsJsonObject("datasets_count").get("doc_count").getAsLong(),
            assetTotals.get(0),
            assetTotals.get(1));
    return new TabCounts(dataLibrary, assetTotals, aggregations);
  }

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

  private String toJsonString(String value) {
    return GsonUtil.getInstance().toJson(Objects.toString(value, ""));
  }

  public record DacSearchCounts(DataLibrary dataLibrary, long dacDatasets) {}

  private record TabCounts(
      DataLibrary dataLibrary, List<Long> assetTotals, JsonObject aggregations) {}
}
