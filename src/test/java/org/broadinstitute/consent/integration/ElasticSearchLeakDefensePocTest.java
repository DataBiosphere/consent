package org.broadinstitute.consent.integration;

import static org.broadinstitute.consent.integration.ElasticSearchAccessContractFixtures.ADMIN;
import static org.broadinstitute.consent.integration.ElasticSearchAccessContractFixtures.CASE_MISMATCHED_CUSTODIAN;
import static org.broadinstitute.consent.integration.ElasticSearchAccessContractFixtures.CUSTODIAN;
import static org.broadinstitute.consent.integration.ElasticSearchAccessContractFixtures.INDEX;
import static org.broadinstitute.consent.integration.ElasticSearchAccessContractFixtures.INTERNAL_MARKER_PREFIX;
import static org.broadinstitute.consent.integration.ElasticSearchAccessContractFixtures.MARKER_RESTRICTED_CUSTODIAN_STUDY;
import static org.broadinstitute.consent.integration.ElasticSearchAccessContractFixtures.MARKER_RESTRICTED_OWN_SUBMISSION;
import static org.broadinstitute.consent.integration.ElasticSearchAccessContractFixtures.STRANGER;
import static org.broadinstitute.consent.integration.ElasticSearchAccessContractFixtures.SUBMITTER;
import static org.broadinstitute.consent.integration.ElasticSearchAccessContractModel.EnforcementMode.FALLBACK;
import static org.broadinstitute.consent.integration.ElasticSearchAccessContractModel.EnforcementMode.NATIVE;
import static org.broadinstitute.consent.integration.ElasticSearchAccessContractModel.EnforcementMode.NATIVE_UNMEDIATED;
import static org.broadinstitute.consent.integration.ElasticSearchAccessContractModel.EnforcementMode.UNMEDIATED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import org.broadinstitute.consent.integration.ElasticSearchAccessContractFixtures.Caller;
import org.broadinstitute.consent.integration.ElasticSearchAccessContractFixtures.Document;
import org.broadinstitute.consent.integration.ElasticSearchAccessContractModel.EnforcementMode;
import org.broadinstitute.consent.integration.ElasticSearchAccessContractModel.RejectedQueryException;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.ResponseException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Proof of concept for {@code docs/plans/es-access-contract.md}: an adversarial end-to-end test
 * that runs one corpus of data-exfiltration attempts against a real Elasticsearch cluster under
 * four enforcement configurations, and asserts what each one does and does not leak.
 *
 * <p>The question this answers is not "does the enforcement code work" — none of it exists yet —
 * but "does the contract's design actually stop the attacks it claims to stop, and are the facts it
 * asserts about Elasticsearch true of the version we run." So the enforcement is modeled in {@link
 * ElasticSearchAccessContractModel} and everything else is real: a real cluster on a real license,
 * real documents, real DSL, real API keys carrying real DLS/FLS role descriptors.
 *
 * <h2>The four configurations</h2>
 *
 * <table>
 *   <tr><th>Mode</th><th>What it is</th><th>Result</th></tr>
 *   <tr><td>{@link EnforcementMode#UNMEDIATED}</td>
 *       <td>Today's {@code DatasetResource.searchDatasetIndex}</td>
 *       <td>Leaks, by every route tried.</td></tr>
 *   <tr><td>{@link EnforcementMode#NATIVE_UNMEDIATED}</td>
 *       <td>Epic D as first specified: DLS/FLS with §B's grant, caller DSL passed through</td>
 *       <td>Still leaks index-wide statistics.</td></tr>
 *   <tr><td>{@link EnforcementMode#NATIVE}</td>
 *       <td>Epic D + §F mediation, with the grant and response filter this PoC's measurements
 *           force</td>
 *       <td>No leak.</td></tr>
 *   <tr><td>{@link EnforcementMode#FALLBACK}</td>
 *       <td>Epic E: mediation + injected filter + response filter</td>
 *       <td>No leak, and identical results to {@code NATIVE}.</td></tr>
 * </table>
 *
 * <p>The first two matter as much as the last two. A security test that only shows the fixed
 * configuration passing proves nothing about whether the attacks were ever capable of succeeding,
 * so {@link #todaysSearchEndpointLeaksEveryClassOfData} asserts that each attack marked {@code
 * mustLeakUnmediated} does in fact leak as things stand. If a change to the fixtures neutralizes an
 * attack by accident, that test fails and says so rather than letting the corresponding "defended"
 * assertion pass vacuously.
 *
 * <h2>How a leak is detected</h2>
 *
 * {@link #assertNoLeak} scans the <em>entire</em> serialized response rather than inspecting named
 * paths. That is deliberate: §F.2 requires unrecognized channels to fail closed, and a test that
 * checked {@code hits.hits[*]._source} would have exactly the blind spot it is meant to catch — the
 * nested {@code top_hits} {@code _source} of §B.5b is the worked example. Scanning the whole
 * document means a marker arriving through a bucket key, a sort array, a highlight snippet, a
 * runtime field or a channel nobody has thought of is still a failure. Statistical disclosure,
 * which no marker scan can see, is checked structurally alongside it.
 *
 * <h2>Findings this PoC produced</h2>
 *
 * Four measurements contradict or sharpen what the contract currently states. Each has its own test
 * so the contract can be corrected against a citable result:
 *
 * <ol>
 *   <li>{@link #minDocCountZeroDoesNotEnumerateRestrictedTermsOnThisVersion} — §1.1's headline
 *       example does not reproduce on 9.4.4.
 *   <li>{@link #nativeDlsFlsAloneStillLeaksIndexWideStatistics} — but two other routes do, so
 *       §1.1's conclusion holds on different evidence.
 *   <li>{@link #open8_aPathOutsideTheFlsGrantIsNotQueryable} — OPEN-8 resolves restrictively, and
 *       affects three paths rather than the two §B.0a names.
 *   <li>{@link #aGrantedMultiFieldDoesNotCarryItsKeywordSubfield} — the FLS grant and §F.1's {@code
 *       .keyword} normalization disagree, and the failure is silent.
 * </ol>
 *
 * @see ElasticSearchAccessContractFixtures for the documents and the marker scheme
 * @see ElasticSearchAccessContractModel for the modeled enforcement
 */
@Tag("elasticsearch")
@DisplayName("ES access contract — data leak defense (proof of concept)")
class ElasticSearchLeakDefensePocTest extends ElasticSearchContainerTests {

  /**
   * Every {@code datasetIdentifier} in the corpus, for computing what a caller must not receive.
   */
  private static final Set<String> ALL_IDENTIFIERS =
      Set.of("DUOS-00001", "DUOS-00002", "DUOS-00003", "DUOS-00004", "DUOS-00005");

  /** API keys, one per caller and enforcement mode, minted on first use and reused thereafter. */
  private static final Map<String, String> API_KEYS = new ConcurrentHashMap<>();

  @BeforeAll
  static void seedCluster() throws Exception {
    activateTrialLicense();
    recreateIndex(INDEX, ElasticSearchAccessContractFixtures.MAPPING);
    for (Document document : ElasticSearchAccessContractFixtures.documents()) {
      indexDocument(INDEX, document.id(), document.body());
    }
  }

  // =============================================================================================
  // The attack corpus
  // =============================================================================================

  /**
   * One request, adversarial or legitimate.
   *
   * @param tab the asset tab a legitimate caller would name; {@code null} for a plain hit search.
   *     This is the only caller influence over aggregations that E-0 permits.
   * @param mustLeakUnmediated whether this attack is required to succeed against today's endpoint.
   *     {@code false} where the route is real but its disclosure is not mechanically detectable, or
   *     where the cluster refuses the request for its own reasons.
   * @param mustBeRejected whether the mediator must <em>reject</em> the query rather than
   *     neutralize it. True exactly when the surviving DSL references a non-QUERYABLE path: §F.1
   *     requires rejection there, because dropping a {@code filter} clause broadens a query and
   *     dropping a {@code sort} changes paging — a silent drop returns more than the caller asked
   *     for.
   * @param intendedCaller the caller a legitimate request is made by; {@code null} for attacks,
   *     which run as {@link ElasticSearchAccessContractFixtures#STRANGER}.
   */
  record Attack(
      String name,
      String dsl,
      String tab,
      boolean mustLeakUnmediated,
      boolean mustBeRejected,
      Caller intendedCaller,
      String contractReference) {

    Caller caller() {
      return intendedCaller == null ? STRANGER : intendedCaller;
    }

    @Override
    public String toString() {
      return name;
    }
  }

  private static Attack neutralized(
      String name, String contractReference, boolean mustLeakUnmediated, String dsl) {
    return new Attack(name, dsl, null, mustLeakUnmediated, false, null, contractReference);
  }

  private static Attack rejected(
      String name, String contractReference, boolean mustLeakUnmediated, String dsl) {
    return new Attack(name, dsl, null, mustLeakUnmediated, true, null, contractReference);
  }

  private static Attack legitimate(
      String name, String contractReference, Caller caller, String tab, String dsl) {
    return new Attack(name, dsl, tab, false, false, caller, contractReference);
  }

  /**
   * The corpus, grouped by the contract clause each attack tests so its coverage can be checked
   * against the document rather than taken on trust.
   */
  static List<Attack> corpus() {
    return List.of(
        // ---- Decision 1: restricted documents are invisible, and uncounted -------------------
        neutralized(
            "plain match_all",
            "Decision 1",
            true,
            """
            {"query":{"match_all":{}},"size":50}
            """),

        // ---- §B.4 / §B.5: no wildcard projection, dynamic maps are internal -------------------
        neutralized(
            "_source wildcard",
            "§B.5 — grants may not contain wildcards",
            true,
            """
            {"query":{"match_all":{}},"size":50,"_source":["*"]}
            """),
        neutralized(
            "_source naming internal subtrees",
            "§B.4 — accessPolicy is internal for every caller",
            true,
            """
            {"query":{"match_all":{}},"size":50,
             "_source":["accessPolicy.*","study.throughBioId","study.data.*","data.*"]}
            """),

        // ---- §B.5b: the nested _source the client itself sends --------------------------------
        neutralized(
            "nested _source wildcard inside aggs.top_hits",
            "§B.5b — a root-level strip leaves this untouched",
            true,
            """
            {"size":0,"query":{"match_all":{}},
             "aggs":{"studies":{"terms":{"field":"study.studyId","size":100},
               "aggs":{"study_details":{"top_hits":{"size":1,"_source":["*"]}}}}}}
            """),

        // ---- §1.1 / §F.1: count-and-existence leaks through aggregations ---------------------
        neutralized(
            "terms agg with min_doc_count 0 on a visible field",
            "§1.1 — claimed to enumerate terms from unreadable documents",
            true,
            """
            {"size":0,"query":{"match_all":{}},
             "aggs":{"pi_names":{"terms":{"field":"study.piName","size":100,"min_doc_count":0}}}}
            """),
        neutralized(
            "terms agg with min_doc_count 0 enumerating identifiers",
            "§F.1 — existence leak on a legitimately queryable field",
            true,
            """
            {"size":0,"query":{"match_all":{}},
             "aggs":{"ids":{"terms":{"field":"datasetIdentifier","size":100,"min_doc_count":0}}}}
            """),
        neutralized(
            "significant_terms background set",
            "§F.1 — the background set is index-wide by design",
            true,
            """
            {"size":0,"query":{"match_all":{}},
             "aggs":{"sig":{"significant_terms":{"field":"study.piName","size":100,
               "min_doc_count":1}}}}
            """),
        neutralized(
            "ordinary terms agg bucket keys",
            "§F.2 — a terms bucket key is the field value",
            true,
            """
            {"size":0,"query":{"match_all":{}},
             "aggs":{"pis":{"terms":{"field":"study.piName","size":100}}}}
            """),
        neutralized(
            "cardinality and value_count magnitude probe",
            "§1.1 — claimed to leak magnitude",
            false,
            """
            {"size":0,"query":{"match_all":{}},
             "aggs":{"studies":{"cardinality":{"field":"study.studyId"}},
                     "datasets":{"value_count":{"field":"datasetId"}}}}
            """),
        neutralized(
            "global agg escaping the query",
            "§F.1 — an aggregation scope the query does not bound",
            true,
            """
            {"size":0,"query":{"term":{"datasetIdentifier":"DUOS-00001"}},
             "aggs":{"everything":{"global":{},
               "aggs":{"pis":{"terms":{"field":"study.piName","size":100}}}}}}
            """),

        // ---- §B.4: probing enforcement input directly ----------------------------------------
        rejected(
            "term query on accessPolicy",
            "§B.4 — enforcement input is not queryable",
            true,
            """
            {"query":{"term":{"accessPolicy.custodianEmails":"custodian@example.org"}},"size":50}
            """),
        rejected(
            "exists query on accessPolicy",
            "§B.4 — existence probe on enforcement input",
            true,
            """
            {"query":{"exists":{"field":"accessPolicy.custodianEmails"}},"size":50}
            """),
        rejected(
            "term query on a dynamic property map",
            "§B.5 — dynamic maps are internal",
            true,
            """
            {"query":{"term":{"study.data.internalNote":"INTERNAL-DYNAMIC-STUDY-2"}},"size":50}
            """),
        rejected(
            "prefix query on an internal identifier field",
            "§B.2 — study.throughBioId is internal",
            true,
            """
            {"query":{"prefix":{"study.throughBioId":{"value":"INTERNAL"}}},"size":50}
            """),

        // ---- §F.1 point 3: normalization and evasion -----------------------------------------
        rejected(
            "'.keyword' suffix evasion of the field allowlist",
            "§F.1 — resolve .keyword before matching",
            false,
            """
            {"query":{"term":{"accessPolicy.custodianEmails.keyword":"custodian@example.org"}},
             "size":50}
            """),
        rejected(
            "wildcard field reference in multi_match",
            "§F.1 — wildcards are refused, not expanded",
            true,
            """
            {"query":{"multi_match":{"query":"INTERNAL-BIO-2","fields":["*"]}},"size":50}
            """),

        // ---- §F.2: the response channels other than hits._source -----------------------------
        rejected(
            "sort on an internal field",
            "§F.2 — sort values echo the sorted field",
            true,
            """
            {"query":{"match_all":{}},"size":50,
             "sort":[{"study.throughBioId":{"order":"asc"}}]}
            """),
        neutralized(
            "sort on a QUERYABLE but RESPONSE-INTERNAL field",
            "§B.0a + §F.2 — the two axes overlap in the sort channel",
            true,
            """
            {"query":{"match_all":{}},"size":50,"sort":[{"createUserId":{"order":"asc"}}]}
            """),
        rejected(
            "highlight on an internal field",
            "§F.2 — highlight returns field content",
            true,
            """
            {"query":{"match":{"study.throughBioId":"INTERNAL-BIO-2"}},"size":50,
             "highlight":{"fields":{"study.throughBioId":{}}}}
            """),
        neutralized(
            "'fields' request parameter",
            "§F.2 — the fields channel, absent from E-1's original strip list",
            true,
            """
            {"query":{"match_all":{}},"size":50,
             "fields":["accessPolicy.*","study.throughBioId","study.data.*"]}
            """),
        neutralized(
            "docvalue_fields on internal paths",
            "§F.2 — another per-hit value channel",
            true,
            """
            {"query":{"match_all":{}},"size":50,
             "docvalue_fields":["study.throughBioId","accessPolicy.custodianEmails"]}
            """),
        neutralized(
            "collapse with inner_hits _source wildcard",
            "§F.2 — inner_hits has the same exposure as top_hits",
            true,
            """
            {"query":{"match_all":{}},"size":50,
             "collapse":{"field":"accessManagement",
               "inner_hits":{"name":"docs","size":10,"_source":["*"]}}}
            """),

        // ---- script and scoring surfaces ------------------------------------------------------
        neutralized(
            "script_fields reading doc values",
            "E-1 — script surfaces",
            true,
            """
            {"query":{"match_all":{}},"size":50,
             "script_fields":{"leak":{"script":{"source":
               "doc['study.throughBioId'].size()==0 ? 'none' : doc['study.throughBioId'].value"}}}}
            """),
        neutralized(
            "runtime_mappings copying an internal value into a returnable field",
            "E-1 revised — runtime_mappings was missing from the original strip list",
            true,
            """
            {"query":{"match_all":{}},"size":50,
             "runtime_mappings":{"leak":{"type":"keyword","script":{"source":
               "emit(params._source.study == null ? 'none' : params._source.study.throughBioId)"}}},
             "fields":["leak"]}
            """),
        neutralized(
            "explain reading index-wide term statistics",
            "§F.1 / OPEN-10 — direct read of the statistics behind relevance",
            true,
            """
            {"query":{"match":{"datasetName":"cohort"}},"size":50,"explain":true}
            """),
        neutralized(
            "profile reading query execution detail",
            "E-1 — rejected by the cluster under DLS, but not on the fallback path",
            false,
            """
            {"query":{"match":{"datasetName":"cohort"}},"size":0,"profile":true}
            """));
  }

  /**
   * Requests the product legitimately makes, each attributed to the caller who makes it. These must
   * be <em>accepted</em> and must return data — without them the corpus above would be passed by a
   * mediator that rejected everything, which would be perfectly secure and completely unshippable.
   */
  static List<Attack> controls() {
    return List.of(
        legitimate(
            "My Data Submissions",
            "§B.0a — RESPONSE-INTERNAL but QUERYABLE",
            SUBMITTER,
            null,
            """
            {"query":{"bool":{"should":[{"term":{"createUserId":7}},
              {"term":{"study.dataSubmitterId":7}}],"minimum_should_match":1}},"size":50}
            """),
        legitimate(
            "restrictToPublicVisibility term clause",
            "§B.2 — QUERYABLE until G-1",
            CUSTODIAN,
            null,
            """
            {"query":{"term":{"study.publicVisibility":true}},"size":50}
            """),
        legitimate(
            "library text search with a field boost",
            "§F.1 — strip field^boost before matching",
            CUSTODIAN,
            null,
            """
            {"query":{"multi_match":{"query":"cohort",
              "fields":["datasetName","study.studyName^2"]}},"size":50}
            """),
        legitimate(
            "sort and page on a visible field",
            "§F.1 — .keyword normalization on a permitted path",
            ADMIN,
            null,
            """
            {"query":{"match_all":{}},"from":0,"size":2,
             "sort":[{"datasetName.keyword":{"order":"asc"}},"_score"]}
            """),
        legitimate(
            "highlight on a visible field",
            "§F.2 — projected, not dropped",
            CUSTODIAN,
            null,
            """
            {"query":{"match":{"datasetName":"cohort"}},"size":50,
             "highlight":{"fields":{"datasetName":{}}}}
            """),
        legitimate(
            "study-asset tab (server-owned STUDIES_AGG)",
            "§F.1 shape 2 / E-0",
            CUSTODIAN,
            "models",
            """
            {"query":{"match_all":{}}}
            """),
        legitimate(
            "filter facet counts (server-owned FILTER_AGGS)",
            "§F.1 shape 1 / E-0",
            CUSTODIAN,
            "datasets",
            """
            {"query":{"match_all":{}}}
            """));
  }

  static Stream<Arguments> corpusUnderContractEnforcement() {
    return corpus().stream()
        .flatMap(attack -> Stream.of(NATIVE, FALLBACK).map(mode -> Arguments.of(attack, mode)));
  }

  static Stream<Arguments> controlsUnderContractEnforcement() {
    return controls().stream()
        .flatMap(attack -> Stream.of(NATIVE, FALLBACK).map(mode -> Arguments.of(attack, mode)));
  }

  static Stream<Arguments> callersAndModes() {
    return ElasticSearchAccessContractFixtures.allCallers().stream()
        .flatMap(caller -> Stream.of(NATIVE, FALLBACK).map(mode -> Arguments.of(caller, mode)));
  }

  static Stream<Arguments> attacksAndCallers() {
    return Stream.concat(corpus().stream(), controls().stream())
        .flatMap(
            attack ->
                ElasticSearchAccessContractFixtures.allCallers().stream()
                    .map(caller -> Arguments.of(attack, caller)));
  }

  // =============================================================================================
  // 1. The corpus has teeth: today's endpoint leaks
  // =============================================================================================

  /**
   * Every attack marked {@code mustLeakUnmediated} succeeds against the endpoint as it exists
   * today.
   *
   * <p>This is the test that makes the rest of the class meaningful, and it is asserted in one
   * place over the whole corpus so that an attack which stops working cannot quietly become a
   * passing "defended" assertion. Reported in aggregate rather than one failure at a time: the
   * useful output when this breaks is which attacks stopped working, not the first one.
   */
  @Test
  void todaysSearchEndpointLeaksEveryClassOfData() {
    List<String> notLeaking = new ArrayList<>();
    for (Attack attack : corpus()) {
      if (attack.mustLeakUnmediated() && !leaks(execute(attack, STRANGER, UNMEDIATED), STRANGER)) {
        notLeaking.add(attack.name());
      }
    }
    assertTrue(
        notLeaking.isEmpty(),
        """
        These attacks no longer leak against the unmediated endpoint, so the corresponding \
        "defended" assertions in this class now pass for the wrong reason. Either the fixtures \
        stopped exercising the attack, or the endpoint changed and the corpus should be updated: \
        """
            + notLeaking);
  }

  // =============================================================================================
  // 2. §1.1 — native DLS/FLS alone does not deliver Decision 1
  // =============================================================================================

  /**
   * FINDING 1 — §1.1's headline example does not reproduce on Elasticsearch 9.4.4.
   *
   * <p>§1.1 states, on Elastic's documentation, that "a {@code terms} aggregation with {@code
   * min_doc_count: 0} enumerates terms that occur only in documents the caller cannot read".
   * Measured against a DLS-restricted API key on 9.4.4, it does not: the aggregation returns only
   * terms present in documents the caller may read. The same holds with {@code shard_min_doc_count:
   * 0}, with an {@code include} regex, ordered by {@code _key}, for {@code rare_terms}, and inside
   * a {@code global} aggregation — and the {@code cardinality}/{@code value_count} magnitude leak
   * §1.1 also cites does not reproduce either.
   *
   * <p>This does not weaken §1.1's conclusion, which the next test establishes on other evidence.
   * It does mean the contract should cite the routes that reproduce rather than the ones that do
   * not, because a security argument resting on a stale example invites someone to re-derive it,
   * find this result, and conclude the mediator is unnecessary.
   *
   * <p>Pinned as an assertion rather than left as a note so that an Elasticsearch upgrade which
   * reopens the leak fails here.
   */
  @Test
  void minDocCountZeroDoesNotEnumerateRestrictedTermsOnThisVersion() {
    Outcome outcome =
        execute(attack("terms agg with min_doc_count 0 on a"), STRANGER, NATIVE_UNMEDIATED);

    assertFalse(outcome.rejected(), "the cluster refused the probe: " + outcome.rejectionReason());
    for (String marker : ElasticSearchAccessContractFixtures.ALL_RESTRICTED_MARKERS) {
      assertFalse(
          outcome.contains(marker),
          """
          min_doc_count:0 DID enumerate a term from a document DLS hides, on this Elasticsearch \
          version. §1.1's example is live again after all, and this test — which records that it \
          was not reproducible on 9.4.4 — should be updated along with the contract. Response: \
          """
              + outcome.summary());
    }
  }

  /**
   * FINDING 2 — but native DLS/FLS alone <em>does</em> still leak, by two other routes, so §1.1's
   * conclusion stands.
   *
   * <p>With DLS and FLS both active and the caller's DSL passed through — Epic D exactly as first
   * specified — a caller authorized for 2 of 5 documents can read:
   *
   * <ul>
   *   <li><b>the size of the whole index</b>, from {@code significant_terms}' {@code bg_count},
   *       which is computed against an index-wide background set by design and reports 5;
   *   <li><b>index-wide term statistics</b>, from {@code _explanation}, which reports {@code "N,
   *       total number of documents with field": 5} and {@code "n, number of documents containing
   *       term": 4} for a search whose visible result set is one document.
   * </ul>
   *
   * <p>Both are exactly what §1.1 claims in substance: document filtering does not isolate
   * index-wide statistics, so the invisibility guarantee is a property of document filtering
   * <em>plus</em> query mediation. That is what moves {@code SearchQueryMediator} from "the
   * fallback's compensating control" to a shared prerequisite, and it is why D-3 must not ship
   * without E-1.
   *
   * <p>It also makes stripping {@code explain} load-bearing rather than tidy. Note that {@code
   * profile} is refused by the cluster itself once DLS is active ("A search request cannot be
   * profiled if document level security is enabled") — but that protection does not exist on the
   * fallback path, which runs with privileged credentials and no DLS, so E-1 must still strip it.
   */
  @Test
  void nativeDlsFlsAloneStillLeaksIndexWideStatistics() {
    Outcome viaSignificantTerms =
        execute(attack("significant_terms background set"), STRANGER, NATIVE_UNMEDIATED);
    assertFalse(viaSignificantTerms.rejected(), viaSignificantTerms.rejectionReason());
    JsonElement backgroundCount =
        path(viaSignificantTerms.response(), "aggregations", "sig", "bg_count");
    assertEquals(
        ALL_IDENTIFIERS.size(),
        backgroundCount == null ? -1 : backgroundCount.getAsInt(),
        """
        significant_terms' background count was expected to report the size of the whole index \
        despite DLS. If it now reports only the documents this caller may read, this route is \
        closed and §1.1 should be re-derived from whatever still reproduces. Response: \
        """
            + viaSignificantTerms.summary());

    Outcome viaExplain =
        execute(attack("explain reading index-wide term statistics"), STRANGER, NATIVE_UNMEDIATED);
    assertFalse(viaExplain.rejected(), viaExplain.rejectionReason());
    assertTrue(
        viaExplain.contains("\"N, total number of documents with field\""),
        """
        _explanation was expected to disclose index-wide term statistics despite DLS. Response: \
        """
            + viaExplain.summary());
  }

  /**
   * The same two routes produce nothing once the mediator is in front of them, on both enforcement
   * paths. Together with the test above, this is the PoC's central result: the leak is closed by
   * the server owning the query surface, not by filtering documents.
   */
  @ParameterizedTest(name = "{0}")
  @EnumSource(
      value = EnforcementMode.class,
      names = {"NATIVE", "FALLBACK"})
  void mediationClosesTheStatisticalLeaksOnBothPaths(EnforcementMode mode) {
    for (String name :
        List.of("significant_terms background set", "explain reading index-wide term statistics")) {
      Attack attack = attack(name);
      Outcome outcome = execute(attack, STRANGER, mode);
      assertNoLeak(outcome, STRANGER, attack, mode);
      assertFalse(
          outcome.contains("bg_count"),
          "the background-set channel survived mediation: " + outcome.summary());
      assertFalse(
          outcome.contains("_explanation"),
          "the explanation channel survived mediation: " + outcome.summary());
    }
  }

  // =============================================================================================
  // 3. The contract's enforcement defeats the whole corpus, on both paths
  // =============================================================================================

  /**
   * Every attack, against both enforcement paths, as the caller with no relationship to anything.
   */
  @ParameterizedTest(name = "{1}: {0}")
  @MethodSource("corpusUnderContractEnforcement")
  void contractEnforcementDefeatsEveryAttack(Attack attack, EnforcementMode mode) {
    assertNoLeak(execute(attack, STRANGER, mode), STRANGER, attack, mode);
  }

  /**
   * The attacks that reference a non-QUERYABLE path are <em>rejected</em>, not silently repaired.
   *
   * <p>§F.1's reject-rather-than-drop rule is a correctness requirement as much as a security one:
   * a dropped {@code filter} clause broadens the result set and a dropped {@code sort} key changes
   * which page the caller is on, so a mediator that quietly removed the offending clause would
   * return more than was asked for and call it success.
   */
  @ParameterizedTest(name = "{0}")
  @MethodSource("corpus")
  void nonQueryableFieldReferencesAreRejectedRatherThanDropped(Attack attack) {
    if (!attack.mustBeRejected()) {
      return;
    }
    Outcome outcome = execute(attack, STRANGER, FALLBACK);
    assertTrue(
        outcome.rejected(),
        "expected the mediator to reject this query outright (%s), but it ran: %s"
            .formatted(attack.contractReference(), outcome.summary()));
    assertTrue(
        outcome.rejectionReason().startsWith("mediator:"),
        "expected rejection by the mediator rather than by Elasticsearch: "
            + outcome.rejectionReason());
  }

  /** Every caller, not just the stranger: each sees exactly the documents §A authorizes. */
  @ParameterizedTest(name = "{1}: {0}")
  @MethodSource("callersAndModes")
  void eachCallerSeesExactlyTheDocumentsTheContractAuthorizes(Caller caller, EnforcementMode mode) {
    Attack matchAll = corpus().getFirst();
    Outcome outcome = execute(matchAll, caller, mode);

    assertFalse(outcome.rejected(), "match_all was rejected: " + outcome.rejectionReason());
    assertEquals(
        sorted(caller.visibleIdentifiers()),
        sorted(identifiersIn(outcome.response())),
        "document visibility does not match §A for caller " + caller.label());
    assertNoLeak(outcome, caller, matchAll, mode);
  }

  /**
   * Decision 1's counting half: {@code hits.total} excludes restricted documents, it does not
   * merely omit them from the returned page.
   *
   * <p>Asserted separately from visibility because the two failures look different in production —
   * a filter applied after counting returns the right documents and the wrong total, and the total
   * is what tells a caller how many datasets exist that they cannot see.
   */
  @ParameterizedTest(name = "{1}: {0}")
  @MethodSource("callersAndModes")
  void restrictedDocumentsDoNotContributeToTheTotalHitCount(Caller caller, EnforcementMode mode) {
    Outcome outcome = execute(corpus().getFirst(), caller, mode);
    assertEquals(
        caller.visibleIdentifiers().size(),
        totalHits(outcome.response()),
        "hits.total leaks the existence of documents this caller may not read");
  }

  // =============================================================================================
  // 4. Parity — the two paths must answer identically
  // =============================================================================================

  /**
   * For every request and every caller, the native path and the fallback path agree on which
   * documents are visible and on the total count.
   *
   * <p>§1.1's warning is that if the two enforcement mechanisms drift apart the fallback becomes a
   * hole, and the drift would be invisible in whichever environment runs the other path. This is
   * the check that catches it, and it is a real check rather than two hand-written expectations
   * agreeing by construction: the visibility predicate is shared, but the mechanisms are genuinely
   * different — a cluster-side DLS query on one side, a server-injected {@code filter} clause plus
   * a response projection on the other — so a semantic difference between them surfaces here.
   *
   * <p>Writing this test is what turned up findings 3 and 4: both first appeared as one path
   * returning documents the other did not.
   */
  @ParameterizedTest(name = "{1}: {0}")
  @MethodSource("attacksAndCallers")
  void theNativeAndFallbackPathsAgree(Attack attack, Caller caller) {
    Outcome viaNative = execute(attack, caller, NATIVE);
    Outcome viaFallback = execute(attack, caller, FALLBACK);

    assertEquals(
        viaNative.rejected(),
        viaFallback.rejected(),
        "one path accepted a query the other rejected — native: %s / fallback: %s"
            .formatted(viaNative.summary(), viaFallback.summary()));
    if (viaNative.rejected()) {
      return;
    }
    assertEquals(
        sorted(identifiersIn(viaNative.response())),
        sorted(identifiersIn(viaFallback.response())),
        "the two enforcement paths disagree about which documents are visible");
    assertEquals(
        totalHits(viaNative.response()),
        totalHits(viaFallback.response()),
        "the two enforcement paths disagree about hits.total");
  }

  // =============================================================================================
  // 5. The product still works
  // =============================================================================================

  /** Legitimate requests are accepted and return data under both enforcement paths. */
  @ParameterizedTest(name = "{1}: {0}")
  @MethodSource("controlsUnderContractEnforcement")
  void legitimateRequestsAreAcceptedAndStillReturnData(Attack control, EnforcementMode mode) {
    Outcome outcome = execute(control, control.caller(), mode);

    assertFalse(
        outcome.rejected(),
        "the mediator rejected a request the product legitimately makes (%s): %s"
            .formatted(control.contractReference(), outcome.rejectionReason()));
    assertTrue(
        returnsData(outcome.response()),
        "the request was accepted but returned neither a hit nor a bucket: " + outcome.summary());
    assertNoLeak(outcome, control.caller(), control, mode);
  }

  /**
   * "My Data Submissions" keeps working: the submitter's own non-public dataset comes back, and
   * {@code createUserId} — the field the query filtered on — does not.
   *
   * <p>The single most demanding requirement in §B, because it needs one path to be queryable and
   * not returnable at the same time. On the fallback path that is straightforward. On the native
   * path it is only possible because {@link EnforcementMode#NATIVE} grants the field and then
   * strips it in the response filter, which is the remedy OPEN-8 names and which {@link
   * #open8_aPathOutsideTheFlsGrantIsNotQueryable} shows is not optional.
   */
  @ParameterizedTest(name = "{0}")
  @EnumSource(
      value = EnforcementMode.class,
      names = {"NATIVE", "FALLBACK"})
  void myDataSubmissionsReturnsTheCallersOwnRestrictedDatasetWithoutTheFilterField(
      EnforcementMode mode) {
    Attack myDataSubmissions = attack("My Data Submissions");
    Outcome outcome = execute(myDataSubmissions, SUBMITTER, mode);

    assertFalse(outcome.rejected(), outcome.rejectionReason());
    assertEquals(
        Set.of("DUOS-00005"),
        identifiersIn(outcome.response()),
        "the caller's own non-public submission must be visible to them (§A rows 6/7)");
    assertFalse(
        outcome.contains("createUserId"),
        "createUserId is RESPONSE-INTERNAL and must not be returned even when it was queried");
    assertTrue(
        outcome.contains(MARKER_RESTRICTED_OWN_SUBMISSION),
        "the caller is authorized for this document, so its visible fields must be served");
  }

  /**
   * §B.7 — admin bypasses document filtering and nothing else.
   *
   * <p>Every document is returned, and not one internal field is. The failure this guards against
   * is a wildcard admin grant, which is the natural implementation and would serve {@code
   * accessPolicy.*} and both dynamic property maps to the same UI that has no use for them.
   */
  @ParameterizedTest(name = "{0}")
  @EnumSource(
      value = EnforcementMode.class,
      names = {"NATIVE", "FALLBACK"})
  void adminBypassesDocumentFilteringButNotProjection(EnforcementMode mode) {
    Outcome outcome = execute(corpus().getFirst(), ADMIN, mode);

    assertEquals(
        sorted(ALL_IDENTIFIERS),
        sorted(identifiersIn(outcome.response())),
        "admin must see every document (§A row 1)");
    assertFalse(
        outcome.contains("accessPolicy"),
        "admin is a document-visibility bypass, not a projection bypass (§B.7)");
    assertFalse(
        outcome.contains(INTERNAL_MARKER_PREFIX),
        "an internal field reached an admin caller (§B.7): " + outcome.summary());
  }

  /**
   * §A.2 — custodian email matching stays case-sensitive, so a case-mismatched custodian is not
   * authorized.
   *
   * <p>Asserting a known defect on purpose. The contract PRESERVEs today's exact matching because
   * normalizing only the index would authorize through search a user the dataset endpoints still
   * reject, and that asymmetry is worse than the defect. If OPEN-6 is approved, this test changes
   * at the same time as {@code DatasetService} and not before.
   */
  @ParameterizedTest(name = "{0}")
  @EnumSource(
      value = EnforcementMode.class,
      names = {"NATIVE", "FALLBACK"})
  void caseMismatchedCustodianIsNotAuthorized(EnforcementMode mode) {
    Outcome outcome = execute(corpus().getFirst(), CASE_MISMATCHED_CUSTODIAN, mode);

    assertFalse(
        outcome.contains(MARKER_RESTRICTED_CUSTODIAN_STUDY),
        "a case-mismatched custodian email authorized a read that DatasetService would refuse "
            + "(§A.2 / OPEN-6)");
  }

  // =============================================================================================
  // 6. OPEN-8 and the FLS-grant findings that block D-3
  // =============================================================================================

  /**
   * FINDING 3 — OPEN-8, measured: under native FLS a path outside the {@code grant} is <b>not</b>
   * usable as a query target, and it affects three paths rather than the two §B.0a names.
   *
   * <p>OPEN-8 asks whether {@code createUserId} and {@code study.dataSubmitterId} can be QUERYABLE
   * while RESPONSE-INTERNAL, notes that Elastic's documentation does not say, and requires the
   * answer be measured before D-3 ships. Measured here: with D-3's grant as specified — the §B
   * RESPONSE-VISIBLE list — a {@code term} query on either path matches nothing at all. FLS does
   * not merely redact a non-granted field on the way out; it makes it invisible to the search, so
   * the clause silently matches zero documents rather than failing.
   *
   * <p>Consequences, all of which land on D-3:
   *
   * <ul>
   *   <li>"My Data Submissions" returns an empty list on the native path. Silently — no error for
   *       an operator to notice, and correct-looking output for a user with no submissions.
   *   <li>The same applies to {@code study.publicVisibility}, which §B.2 also classifies
   *       RESPONSE-INTERNAL and QUERYABLE-until-G-1. §B.0a lesson 1 names two affected paths; there
   *       are three, and the third is on the data library's main query path.
   *   <li>The two §B axes therefore cannot both be honoured by native FLS alone. The remedy is the
   *       one OPEN-8 itself proposes as its fallback: grant the QUERYABLE paths and strip them in a
   *       response filter on the native path too — which means E-3 is required on the native path,
   *       Epic D is not "the same minus the response filter", and the plan's "D <b>or</b> E"
   *       framing is wrong for a second, independent reason.
   * </ul>
   *
   * <p>The wider-grant leg is a control: it proves the strict-grant result is FLS and not a mistake
   * in the probe.
   */
  @Test
  void open8_aPathOutsideTheFlsGrantIsNotQueryable() {
    Map<String, Attack> byAffectedPath =
        Map.of(
            "createUserId / study.dataSubmitterId", attack("My Data Submissions"),
            "study.publicVisibility", attack("restrictToPublicVisibility term clause"));

    for (Map.Entry<String, Attack> affected : byAffectedPath.entrySet()) {
      Attack request = affected.getValue();
      Outcome underGrantAsSpecified =
          executeWithGrant(request, request.caller(), ElasticSearchAccessContractModel.flsGrant());
      Outcome underWiderGrant =
          executeWithGrant(
              request, request.caller(), ElasticSearchAccessContractModel.operationalFlsGrant());

      assertTrue(
          returnsData(underWiderGrant.response()),
          """
          The control leg of the OPEN-8 probe failed for %s: even with the path inside the FLS \
          grant, the query matched nothing. Something other than FLS is wrong with this probe, so \
          its result says nothing about OPEN-8. %s"""
              .formatted(affected.getKey(), underWiderGrant.summary()));

      assertFalse(
          returnsData(underGrantAsSpecified.response()),
          """
          OPEN-8 RESOLVES PERMISSIVELY for %s on this cluster: a query on a path outside the FLS \
          grant still matched. That is the outcome the contract hoped for — the path can stay \
          RESPONSE-INTERNAL and QUERYABLE with no further work, D-3 can keep it out of the grant, \
          and §B.0a lesson 1's warning does not apply. Update OPEN-8, §B.0a and D-3 to record the \
          measurement, invert this assertion, and narrow EnforcementMode.NATIVE's grant back to \
          the RESPONSE-VISIBLE list. Response: %s"""
              .formatted(affected.getKey(), underGrantAsSpecified.summary()));
    }
  }

  /**
   * FINDING 4 — a granted multi-field does not carry its {@code .keyword} subfield, and the failure
   * is silent.
   *
   * <p>{@code datasetName} is mapped as {@code text} with a {@code keyword} subfield, which is how
   * the product sorts the catalog. Granting {@code datasetName} in the FLS grant does not grant
   * {@code datasetName.keyword}: they are separate fields as far as FLS is concerned. A sort on the
   * subfield then resolves against nothing, and Elasticsearch returns <em>a</em> page — just not
   * the one the caller asked for — with no error.
   *
   * <p>This interacts badly with §F.1's third normalization rule. The mediator resolves {@code
   * datasetName.keyword} to {@code datasetName}, finds it on the allowlist, and accepts the sort;
   * FLS then declines to grant the field the sort actually names. The allowlist and the grant
   * disagree about what a subfield is, and the disagreement produces wrong pagination rather than a
   * refusal.
   *
   * <p>Two things follow for D-3, and §B.5's no-wildcard rule makes the first non-trivial: the
   * grant must enumerate the {@code .keyword} subfield of every multi-field the product sorts or
   * exact-matches on, and §B's field tables — which list logical paths — are not a sufficient
   * source for generating it. This is OPEN-9's drift problem in a second place, and an argument for
   * generating the grant from the mapping rather than from the classification tables.
   */
  @Test
  void aGrantedMultiFieldDoesNotCarryItsKeywordSubfield() {
    Attack sortedPage = attack("sort and page on a visible field");
    List<String> correctFirstPage = List.of("DUOS-00005", "DUOS-00001");

    Outcome underGrantAsSpecified =
        executeWithGrant(sortedPage, ADMIN, ElasticSearchAccessContractModel.flsGrant());
    Outcome withSubfieldEnumerated =
        executeWithGrant(sortedPage, ADMIN, ElasticSearchAccessContractModel.operationalFlsGrant());

    assertEquals(
        correctFirstPage,
        List.copyOf(identifiersIn(withSubfieldEnumerated.response())),
        """
        The control leg failed: with datasetName.keyword enumerated in the grant, the sort should \
        produce the documents in datasetName order. """
            + withSubfieldEnumerated.summary());

    assertFalse(
        underGrantAsSpecified.rejected(),
        """
        A sort on a non-granted subfield was refused outright. That would be the good outcome — a \
        loud failure — and it means this finding no longer applies; update the contract and this \
        test. """
            + underGrantAsSpecified.rejectionReason());
    assertNotEquals(
        correctFirstPage,
        List.copyOf(identifiersIn(underGrantAsSpecified.response())),
        """
        Sorting on datasetName.keyword now works without the subfield being enumerated in the FLS \
        grant, so finding 4 no longer applies on this cluster and D-3 need not enumerate subfields. \
        Update the contract and invert this assertion. """
            + underGrantAsSpecified.summary());
  }

  /**
   * §F.2's {@code aggregations.**} channel, made falsifiable: E-3 catches an internal field that
   * the <em>server's own</em> aggregation asked for.
   *
   * <p>Every other attack in this class comes from the caller, and against a caller the strip list
   * and the field validator do the work — which means that with §F.1's server-owned aggregations in
   * place, deleting E-3's aggregation walk entirely changes no result in the rest of the corpus.
   * That was measured, not assumed, and it is a genuine observation about the design: the walk is
   * not defending the aggregation channel against callers, because callers no longer reach it.
   *
   * <p>What it does defend against is us. §B.5a puts the {@code study.assets.*} leaf enumeration in
   * duos-ui's asset definitions and OPEN-9 warns the backend copy will drift; a drifted copy asks
   * Elasticsearch for an internal leaf directly, and only a response-side projection stands between
   * that and the caller. This test is that scenario, and it is the reason to keep §F.2's walk in
   * E-3 rather than simplify it away once E-0 lands.
   */
  @Test
  void responseFilterCatchesAnInternalFieldTheServersOwnAggregationRequested() {
    Attack driftedTab =
        legitimate(
            "study-asset tab whose leaf enumeration has drifted",
            "§F.2 aggregations.** / OPEN-9",
            CUSTODIAN,
            ElasticSearchAccessContractModel.DRIFTED_ENUMERATION_TAB,
            """
            {"query":{"match_all":{}}}
            """);

    Outcome unfiltered = executeWithoutResponseFilter(driftedTab, CUSTODIAN);
    assertTrue(
        unfiltered.contains("INTERNAL-CHECKPOINT-"),
        """
        The drifted enumeration was expected to pull an internal leaf back through the aggregation \
        channel, so that the response filter has something to catch. It did not, so this test no \
        longer exercises §F.2's aggregations.** requirement. """
            + unfiltered.summary());

    Outcome filtered = execute(driftedTab, CUSTODIAN, FALLBACK);
    assertNoLeak(filtered, CUSTODIAN, driftedTab, FALLBACK);
    assertTrue(
        returnsData(filtered.response()),
        "the tab must still return its studies: " + filtered.summary());
  }

  // =============================================================================================
  // Assertions
  // =============================================================================================

  /**
   * The core assertion: nothing this caller is not authorized to receive appears anywhere in the
   * response, through any channel.
   *
   * <p>Two complementary checks, because they catch different things. The marker scan covers every
   * channel that carries a <em>value</em>, including ones neither this test nor the response filter
   * enumerates — that is how §F.2's "unrecognized channels fail closed" gets checked from the
   * outside. The structural check covers <em>statistical</em> disclosure, which carries no marker
   * at all: a background count or an explanation discloses the shape of the index without ever
   * naming a restricted document, and finding 2 is precisely that failure.
   *
   * <p>A rejected query passes trivially; that it was rejected rather than neutralized is asserted
   * separately, so a mediator that refused everything could not slip through here unnoticed.
   */
  private static void assertNoLeak(
      Outcome outcome, Caller caller, Attack attack, EnforcementMode mode) {
    if (outcome.rejected()) {
      return;
    }
    String context =
        "%s / %s / %s (%s)"
            .formatted(mode, caller.label(), attack.name(), attack.contractReference());

    for (String marker : caller.forbiddenMarkers()) {
      assertFalse(
          outcome.contains(marker),
          "%s: leaked a restricted document's visible field '%s' — Decision 1 violation.%n%s"
              .formatted(context, marker, outcome.summary()));
    }
    for (String identifier : ALL_IDENTIFIERS) {
      if (!caller.visibleIdentifiers().contains(identifier)) {
        assertFalse(
            outcome.contains(identifier),
            "%s: disclosed the existence of '%s'.%n%s"
                .formatted(context, identifier, outcome.summary()));
      }
    }
    assertFalse(
        outcome.contains(INTERNAL_MARKER_PREFIX),
        "%s: a RESPONSE-INTERNAL field reached the caller. Internal paths include %s.%n%s"
            .formatted(
                context,
                ElasticSearchAccessContractFixtures.RESPONSE_INTERNAL_EXAMPLES,
                outcome.summary()));
    assertFalse(
        outcome.contains("accessPolicy"),
        "%s: enforcement input was returned to the caller — §B.4 admits no exception.%n%s"
            .formatted(context, outcome.summary()));
    assertTrue(
        totalHits(outcome.response()) <= caller.visibleIdentifiers().size(),
        "%s: hits.total is %d but this caller may read at most %d documents.%n%s"
            .formatted(
                context,
                totalHits(outcome.response()),
                caller.visibleIdentifiers().size(),
                outcome.summary()));
    assertFalse(
        disclosesIndexWideStatistics(outcome.response(), caller),
        "%s: disclosed statistics computed over documents this caller may not read.%n%s"
            .formatted(context, outcome.summary()));
    assertFalse(
        outcome.contains("\"sort\":"),
        """
        %s: the sort channel survived into the response. Sort values are positional and carry no \
        field name, so there is nothing for a RESPONSE-VISIBLE allowlist to match on, and a sort \
        key that is QUERYABLE-but-RESPONSE-INTERNAL — createUserId, study.dataSubmitterId, \
        study.publicVisibility — then echoes its value once per hit. Dropping the array is the \
        only filter that closes it (§F.2, §B.0a).%n%s"""
            .formatted(context, outcome.summary()));
  }

  /** Whether {@code outcome} discloses anything {@code caller} is not authorized to receive. */
  private static boolean leaks(Outcome outcome, Caller caller) {
    if (outcome.rejected()) {
      return false;
    }
    return caller.forbiddenMarkers().stream().anyMatch(outcome::contains)
        || ALL_IDENTIFIERS.stream()
            .anyMatch(id -> !caller.visibleIdentifiers().contains(id) && outcome.contains(id))
        || outcome.contains(INTERNAL_MARKER_PREFIX)
        || outcome.contains("accessPolicy")
        || totalHits(outcome.response()) > caller.visibleIdentifiers().size()
        || disclosesIndexWideStatistics(outcome.response(), caller);
  }

  /**
   * Whether the response reports a statistic derived from documents the caller may not read.
   *
   * <p>Two shapes, both measured to occur in practice (finding 2): a {@code bg_count} exceeding the
   * caller's authorized document count, and an {@code _explanation}, which reports index-wide
   * document and term frequencies by construction. Skipped for a caller authorized for the whole
   * index, since for them an index-wide statistic discloses nothing.
   */
  private static boolean disclosesIndexWideStatistics(JsonObject response, Caller caller) {
    if (caller.visibleIdentifiers().size() >= ALL_IDENTIFIERS.size()) {
      return false;
    }
    return response.toString().contains("\"_explanation\"")
        || maximumBackgroundCount(response) > caller.visibleIdentifiers().size();
  }

  private static int maximumBackgroundCount(JsonElement node) {
    if (node.isJsonArray()) {
      int maximum = 0;
      for (JsonElement element : node.getAsJsonArray()) {
        maximum = Math.max(maximum, maximumBackgroundCount(element));
      }
      return maximum;
    }
    if (!node.isJsonObject()) {
      return 0;
    }
    int maximum = 0;
    for (Map.Entry<String, JsonElement> member : node.getAsJsonObject().entrySet()) {
      if ("bg_count".equals(member.getKey()) && member.getValue().isJsonPrimitive()) {
        maximum = Math.max(maximum, member.getValue().getAsInt());
      }
      maximum = Math.max(maximum, maximumBackgroundCount(member.getValue()));
    }
    return maximum;
  }

  // =============================================================================================
  // Execution
  // =============================================================================================

  /**
   * The result of running one request in one mode.
   *
   * @param rejected true when the query never reached the index, whether because the mediator
   *     refused it or because Elasticsearch did. Both are safe outcomes and are treated alike here;
   *     which of the two happened is asserted where it matters.
   */
  record Outcome(boolean rejected, String rejectionReason, JsonObject response, String sentBody) {

    static Outcome refused(String reason, String sentBody) {
      return new Outcome(true, reason, new JsonObject(), sentBody);
    }

    boolean contains(String token) {
      return response.toString().contains(token);
    }

    /**
     * Body sent and response received, so an assertion failure is diagnosable from the log alone.
     */
    String summary() {
      String body = response.toString();
      return "%n  sent:     %s%n  received: %s"
          .formatted(sentBody, body.length() > 2000 ? body.substring(0, 2000) + "…" : body);
    }
  }

  private static Outcome execute(Attack attack, Caller caller, EnforcementMode mode) {
    return executeWithKey(
        attack, caller, mode, mode.usesNativeSecurity() ? apiKey(caller, mode) : null);
  }

  /** Runs on the native path under an explicitly chosen FLS grant, for the grant findings. */
  private static Outcome executeWithGrant(Attack attack, Caller caller, Set<String> grant) {
    return executeWithKey(attack, caller, NATIVE, apiKeyWithGrant(caller, grant));
  }

  /**
   * The fallback path with E-3 switched off: mediation and E-2's filter still apply, but the
   * response is returned as Elasticsearch produced it.
   *
   * <p>Exists so a test can show what the response filter removed, rather than only that the
   * filtered response is clean — which is the difference between demonstrating a control works and
   * asserting it exists.
   */
  private static Outcome executeWithoutResponseFilter(Attack attack, Caller caller) {
    Outcome filtered = executeWithKey(attack, caller, FALLBACK, null);
    if (filtered.rejected()) {
      return filtered;
    }
    Request request =
        ElasticSearchTestCluster.jsonRequest("POST", "/" + INDEX + "/_search", filtered.sentBody());
    try {
      return new Outcome(false, null, jsonResponse(request), filtered.sentBody());
    } catch (Exception e) {
      throw new IllegalStateException("unfiltered re-run failed for " + attack, e);
    }
  }

  private static Outcome executeWithKey(
      Attack attack, Caller caller, EnforcementMode mode, String apiKey) {
    String body;
    if (mode.isMediated()) {
      try {
        body = ElasticSearchAccessContractModel.mediate(attack.dsl(), caller, mode, attack.tab());
      } catch (RejectedQueryException e) {
        return Outcome.refused("mediator: " + e.getMessage(), attack.dsl());
      }
    } else {
      body = attack.dsl();
    }

    Request request = ElasticSearchTestCluster.jsonRequest("POST", "/" + INDEX + "/_search", body);
    if (apiKey != null) {
      ElasticSearchTestCluster.asApiKey(request, apiKey);
    }

    JsonObject response;
    try {
      response = jsonResponse(request);
    } catch (ResponseException e) {
      // Elasticsearch refusing a request is a safe outcome, and a common one on the native path:
      // FLS leaves some non-granted references unresolvable rather than merely empty.
      return Outcome.refused(
          "elasticsearch: HTTP %d".formatted(e.getResponse().getStatusLine().getStatusCode()),
          body);
    } catch (Exception e) {
      throw new IllegalStateException(
          "search failed for '%s'; body was %s".formatted(attack, body), e);
    }
    return new Outcome(
        false,
        null,
        mode.isResponseFiltered()
            ? ElasticSearchAccessContractModel.filterResponse(response)
            : response,
        body);
  }

  private static String apiKey(Caller caller, EnforcementMode mode) {
    return apiKeyWithGrant(caller, mode.flsGrant());
  }

  /**
   * An API key for this caller and grant, minted once. Keyed by the grant's own content so that a
   * test choosing a different grant cannot silently reuse another's key.
   */
  private static String apiKeyWithGrant(Caller caller, Set<String> grant) {
    String cacheKey = caller.label() + "|" + grant.hashCode();
    return API_KEYS.computeIfAbsent(
        cacheKey,
        key -> {
          try {
            return createApiKey(
                "poc-" + key.replace('|', '-'),
                ElasticSearchAccessContractModel.roleDescriptors(INDEX, caller, grant));
          } catch (Exception e) {
            throw new IllegalStateException("could not mint an API key for " + caller.label(), e);
          }
        });
  }

  // =============================================================================================
  // Response readers
  // =============================================================================================

  private static Attack attack(String namePrefix) {
    return Stream.concat(corpus().stream(), controls().stream())
        .filter(candidate -> candidate.name().startsWith(namePrefix))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("no such request: " + namePrefix));
  }

  /**
   * Whether the response carries anything at all — a hit, or a non-empty aggregation bucket list.
   */
  private static boolean returnsData(JsonObject response) {
    if (totalHits(response) > 0) {
      return true;
    }
    JsonElement aggregations = response.get("aggregations");
    return aggregations != null && !bucketsIn(aggregations).isEmpty();
  }

  private static List<JsonElement> bucketsIn(JsonElement node) {
    List<JsonElement> buckets = new ArrayList<>();
    collectBuckets(node, buckets);
    return buckets;
  }

  private static void collectBuckets(JsonElement node, List<JsonElement> buckets) {
    if (node.isJsonArray()) {
      node.getAsJsonArray().forEach(element -> collectBuckets(element, buckets));
      return;
    }
    if (!node.isJsonObject()) {
      return;
    }
    for (Map.Entry<String, JsonElement> member : node.getAsJsonObject().entrySet()) {
      if ("buckets".equals(member.getKey()) && member.getValue().isJsonArray()) {
        member.getValue().getAsJsonArray().forEach(buckets::add);
      }
      collectBuckets(member.getValue(), buckets);
    }
  }

  /**
   * Every {@code datasetIdentifier} value anywhere in the response, at any depth and through any
   * channel — hits, {@code top_hits} inside aggregations, {@code inner_hits}, or a bucket key. In
   * document order, so a paging assertion can compare sequences. Collected structurally for the
   * same reason {@link #assertNoLeak} scans the whole document.
   */
  private static Set<String> identifiersIn(JsonElement node) {
    Set<String> found = new LinkedHashSet<>();
    collectIdentifiers(node, found);
    return found;
  }

  private static void collectIdentifiers(JsonElement node, Set<String> found) {
    if (node.isJsonArray()) {
      node.getAsJsonArray().forEach(element -> collectIdentifiers(element, found));
      return;
    }
    if (!node.isJsonObject()) {
      return;
    }
    for (Map.Entry<String, JsonElement> member : node.getAsJsonObject().entrySet()) {
      JsonElement value = member.getValue();
      boolean identifierBearing =
          "datasetIdentifier".equals(member.getKey()) || "key".equals(member.getKey());
      if (identifierBearing
          && value.isJsonPrimitive()
          && value.getAsString().toUpperCase(Locale.ROOT).startsWith("DUOS-")) {
        found.add(value.getAsString());
      }
      collectIdentifiers(value, found);
    }
  }

  private static int totalHits(JsonObject response) {
    JsonElement total = path(response, "hits", "total", "value");
    return total == null ? 0 : total.getAsInt();
  }

  private static List<String> sorted(Set<String> values) {
    return values.stream().sorted().toList();
  }

  /**
   * Reads a nested path, returning {@code null} rather than throwing when any segment is absent.
   */
  private static JsonElement path(JsonObject root, String... segments) {
    JsonElement current = root;
    for (String segment : segments) {
      if (current == null || !current.isJsonObject() || !current.getAsJsonObject().has(segment)) {
        return null;
      }
      current = current.getAsJsonObject().get(segment);
    }
    return current;
  }
}
