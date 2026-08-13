package org.broadinstitute.consent.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.LinkedHashMap;
import java.util.Map;
import org.elasticsearch.client.Request;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Measures the two facts {@code docs/plans/es-access-contract.md} §A.3 leaves open before a
 * two-index split could be relied on as OPEN-10's remedy.
 *
 * <p>§A.3 records physical index separation as the structural option for closing OPEN-10 — the
 * residual leak by which relevance scores disclose index-wide term statistics, and therefore
 * something about documents the caller cannot read. The argument is that Lucene computes those
 * statistics per shard rather than per cluster, so putting restricted documents in a different
 * index puts them in a different statistical population. That is an expectation about
 * Elasticsearch, and §A.3's own standard is that expectations about Elasticsearch get measured. Two
 * of them:
 *
 * <ol>
 *   <li><b>Does a multi-index search keep statistics separate?</b> The whole benefit rests on it.
 *       If searching {@code /public,restricted/_search} pooled the statistics, an authorized
 *       caller's scores would be computed over restricted documents again and OPEN-10 would be
 *       reopened for exactly the callers the split was meant to protect.
 *   <li><b>Is the merged ranking usable?</b> Separate statistics are the security property and the
 *       relevance problem in one: two identical documents in differently-shaped indices are scored
 *       against different populations, so they do not arrive comparable.
 * </ol>
 *
 * <h2>How the corpus is built</h2>
 *
 * Two indices, one shard each, with deliberately opposite term statistics for the word {@code
 * cohort}, and a <b>byte-identical document present in both</b>:
 *
 * <table>
 *   <tr><th></th><th>{@link #PUBLIC_INDEX}</th><th>{@link #RESTRICTED_INDEX}</th></tr>
 *   <tr><td>documents</td><td>10</td><td>10</td></tr>
 *   <tr><td>containing "cohort"</td><td>1 (the twin)</td><td>9 (the twin + 8)</td></tr>
 *   <tr><td>so IDF("cohort") is</td><td>high</td><td>low</td></tr>
 * </table>
 *
 * <p>The twin is what makes the measurement unambiguous. It is the same text, matched by the same
 * query, so any difference in its score between the two indices is a difference in the statistics
 * it was scored against and nothing else. One shard per index removes the other variable: with a
 * single shard, "per shard" and "per index" are the same statement, so a result here cannot be an
 * artifact of how documents happened to distribute.
 */
@Tag("elasticsearch")
@DisplayName("ES access contract — index separation and score statistics (§A.3)")
class ElasticSearchIndexSeparationTest extends ElasticSearchContainerTests {

  private static final String PUBLIC_INDEX = "tier-separation-public";
  private static final String RESTRICTED_INDEX = "tier-separation-restricted";

  /** The term whose document frequency differs between the two indices. */
  private static final String TERM = "cohort";

  /** The document present in both indices, byte-identical. */
  private static final String TWIN = "TWIN";

  /**
   * One shard, so "per shard" and "per index" cannot be confused. {@code datasetName} is {@code
   * text} because IDF only applies to an analyzed field.
   */
  private static final String MAPPING =
      """
      {"settings":{"number_of_shards":1,"number_of_replicas":0},
       "mappings":{"properties":{
         "docId":{"type":"keyword"},
         "datasetName":{"type":"text"}}}}
      """;

  @BeforeAll
  static void seedBothTiers() throws Exception {
    recreateIndex(PUBLIC_INDEX, MAPPING);
    recreateIndex(RESTRICTED_INDEX, MAPPING);

    // Public tier: the twin is the ONLY document carrying the term, so IDF("cohort") is high.
    indexDocument(PUBLIC_INDEX, TWIN, document(TWIN, "public cohort study"));
    for (int i = 1; i <= 9; i++) {
      indexDocument(PUBLIC_INDEX, "P" + i, document("P" + i, "unrelated dataset alpha"));
    }

    // Restricted tier: nine of ten documents carry the term, so IDF("cohort") is low.
    indexDocument(RESTRICTED_INDEX, TWIN, document(TWIN, "public cohort study"));
    for (int i = 1; i <= 8; i++) {
      indexDocument(RESTRICTED_INDEX, "R" + i, document("R" + i, "restricted cohort study"));
    }
    indexDocument(RESTRICTED_INDEX, "R9", document("R9", "unrelated dataset alpha"));
  }

  private static String document(String id, String name) {
    return """
        {"docId":"%s","datasetName":"%s"}
        """
        .formatted(id, name);
  }

  // =============================================================================================
  // Measurement 1 — does a second index contaminate the first index's statistics?
  // =============================================================================================

  /**
   * §A.3 measurement 1: adding a second index to the search target does <b>not</b> change what the
   * first index's documents score.
   *
   * <p>This is the assertion the two-index split stands on. The twin's score is read twice — once
   * searching the public index alone, once searching both — and the two must be identical. If they
   * were not, the restricted index's document frequencies would be feeding into a public document's
   * score, which is OPEN-10 arriving by a different route and would make the split pointless.
   *
   * <p>Asserted to exact equality rather than to a tolerance, deliberately: statistics are either
   * pooled or they are not, and a near-miss would mean something subtler is happening that should
   * be understood rather than absorbed by a delta.
   */
  @Test
  void addingARestrictedIndexToTheSearchDoesNotChangePublicDocumentScores() {
    double alone = scoreOf(search(PUBLIC_INDEX), PUBLIC_INDEX, TWIN);
    double merged = scoreOf(search(PUBLIC_INDEX, RESTRICTED_INDEX), PUBLIC_INDEX, TWIN);

    assertEquals(
        alone,
        merged,
        0.0,
        """
        A public document's score changed when a restricted index was added to the search target, so \
        term statistics are pooled across indices on this cluster. That reopens OPEN-10 for exactly \
        the callers a two-index split was meant to protect, and §A.3's recommendation should be \
        re-derived: the split would buy nothing. Scored %s alone, %s merged."""
            .formatted(alone, merged));
  }

  /**
   * The control for the measurement above: {@code dfs_query_then_fetch} <b>does</b> pool the
   * statistics, which proves the equality above is a real property of the default search type
   * rather than a corpus in which the two indices happen to agree.
   *
   * <p>It is also the reason §F.2a forbids forwarding caller-supplied URL parameters, and this test
   * is what turns that from tidiness into a control with a demonstrated consequence: {@code
   * search_type} is a URL parameter, and a caller who could set it would collapse the split's
   * entire benefit with one query string.
   */
  @Test
  void dfsQueryThenFetchPoolsStatisticsAcrossIndicesAndUndoesTheSeparation() {
    double defaultSearchType = scoreOf(search(PUBLIC_INDEX, RESTRICTED_INDEX), PUBLIC_INDEX, TWIN);
    JsonObject dfs = searchWith("dfs_query_then_fetch", PUBLIC_INDEX, RESTRICTED_INDEX);

    double dfsPublicTwin = scoreOf(dfs, PUBLIC_INDEX, TWIN);
    double dfsRestrictedTwin = scoreOf(dfs, RESTRICTED_INDEX, TWIN);

    assertNotEquals(
        defaultSearchType,
        dfsPublicTwin,
        """
        dfs_query_then_fetch scored the public twin identically to the default search type, so it is \
        no longer pooling statistics across indices on this cluster. The control for measurement 1 \
        is gone: re-derive it before trusting that test, because it would then pass whether or not \
        statistics are separated.""");
    assertEquals(
        dfsPublicTwin,
        dfsRestrictedTwin,
        1e-9,
        """
        Under dfs_query_then_fetch the two copies of the identical twin should score identically, \
        because both are scored against one pooled population. They did not (%s vs %s), so this \
        search type is doing something other than the global-statistics unification assumed here."""
            .formatted(dfsPublicTwin, dfsRestrictedTwin));
  }

  // =============================================================================================
  // Measurement 2 — is the merged ranking usable?
  // =============================================================================================

  /**
   * §A.3 measurement 2: the same document scores differently depending on which tier it is in, so a
   * merged page is not ranked on a single scale.
   *
   * <p>Separate statistics are the security property and the relevance problem in one. The twin is
   * byte-identical in both indices and matched by the same query, so the ratio measured here is
   * caused entirely by the two indices' different document frequencies. Whatever that ratio is, it
   * is the factor by which a document's rank depends on its tier rather than on its content.
   *
   * <p>The assertion is deliberately weak — that a material difference exists at all — because the
   * exact ratio is a property of this corpus rather than of the product. What the test is for is
   * the failure message: it prints the number, so §A.3 can quote a measurement instead of an
   * expectation, and so a future reader can re-run it against a realistic corpus before deciding
   * whether the effect is tolerable.
   */
  @Test
  void identicalDocumentsInDifferentTiersDoNotScoreComparably() {
    JsonObject merged = search(PUBLIC_INDEX, RESTRICTED_INDEX);

    double publicTwin = scoreOf(merged, PUBLIC_INDEX, TWIN);
    double restrictedTwin = scoreOf(merged, RESTRICTED_INDEX, TWIN);
    double ratio = publicTwin / restrictedTwin;

    assertTrue(
        ratio > 1.5,
        """
        The identical twin scored comparably in both tiers (public %s, restricted %s, ratio %.2f), \
        so merged-page relevance is not distorted on this corpus and §A.3's second caveat is \
        weaker than recorded. Re-derive it before relying on either statement."""
            .formatted(publicTwin, restrictedTwin, ratio));

    // Printed rather than asserted: the number is what §A.3 quotes, and pinning it would make this
    // test fail on any scoring change without telling anyone anything useful.
    System.out.printf(
        "§A.3 measurement 2 — identical document, public tier %.4f, restricted tier %.4f, ratio %.2fx%n",
        publicTwin, restrictedTwin, ratio);
  }

  /**
   * And the consequence for paging: the merged first page is dominated by the tier with the rarer
   * term, regardless of content.
   *
   * <p>Asserted because it is the form the relevance problem actually takes in the product. The
   * public tier holds one matching document and the restricted tier holds nine, all equally
   * relevant by content — yet the single public document outranks every one of them, on statistics
   * alone.
   */
  @Test
  void theMergedFirstPageIsOrderedByTierRatherThanByContent() {
    JsonObject merged = search(PUBLIC_INDEX, RESTRICTED_INDEX);
    JsonElement first = merged.getAsJsonObject("hits").getAsJsonArray("hits").get(0);
    String topIndex = first.getAsJsonObject().get("_index").getAsString();

    assertEquals(
        PUBLIC_INDEX,
        topIndex,
        """
        The rarer-term tier no longer takes the top of the merged page, so the ranking distortion \
        §A.3 records is not reproducing on this corpus. Re-derive §A.3's second caveat.""");
  }

  // =============================================================================================
  // Execution
  // =============================================================================================

  private static JsonObject search(String... indices) {
    return searchWith(null, indices);
  }

  private static JsonObject searchWith(String searchType, String... indices) {
    String path = "/" + String.join(",", indices) + "/_search";
    if (searchType != null) {
      path += "?search_type=" + searchType;
    }
    Request request =
        ElasticSearchTestCluster.jsonRequest(
            "POST",
            path,
            """
            {"query":{"match":{"datasetName":"%s"}},"size":50}
            """
                .formatted(TERM));
    try {
      return jsonResponse(request);
    } catch (Exception e) {
      throw new IllegalStateException("search failed against " + path, e);
    }
  }

  /** The score of {@code docId} as returned from {@code index}, which must be present. */
  private static double scoreOf(JsonObject response, String index, String docId) {
    Map<String, Double> scores = new LinkedHashMap<>();
    for (JsonElement hit : response.getAsJsonObject("hits").getAsJsonArray("hits")) {
      JsonObject object = hit.getAsJsonObject();
      scores.put(
          object.get("_index").getAsString() + "/" + object.get("_id").getAsString(),
          object.get("_score").getAsDouble());
    }
    Double score = scores.get(index + "/" + docId);
    if (score == null) {
      throw new IllegalStateException(
          "no hit for %s/%s; the corpus or the query has changed. Scored: %s"
              .formatted(index, docId, scores));
    }
    return score;
  }
}
