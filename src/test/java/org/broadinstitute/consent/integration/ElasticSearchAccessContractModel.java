package org.broadinstitute.consent.integration;

import static org.broadinstitute.consent.integration.ElasticSearchAccessContractFixtures.QUERYABLE;
import static org.broadinstitute.consent.integration.ElasticSearchAccessContractFixtures.RESPONSE_VISIBLE;
import static org.broadinstitute.consent.integration.ElasticSearchAccessContractFixtures.stringArray;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.broadinstitute.consent.integration.ElasticSearchAccessContractFixtures.Caller;

/**
 * A working model of the enforcement described in {@code docs/plans/es-access-contract.md}, small
 * enough to read in one sitting and complete enough to be attacked.
 *
 * <p>None of the tickets this models exists in {@code src/main} yet — E-0, E-1, E-2 and E-3 are
 * unimplemented and D-3 is blocked behind OPEN-8 — so the model lives in the test tree. It is a
 * proof of the <em>design</em>, not a draft of the implementation: it runs against a real
 * Elasticsearch cluster with the real documents, so the attacks in {@link
 * ElasticSearchLeakDefensePocTest} either get through or they do not, and no part of that verdict
 * depends on a stub.
 *
 * <p>Four things are modeled, each named for the ticket that owns it:
 *
 * <table>
 *   <tr><th>Ticket</th><th>Here</th></tr>
 *   <tr><td>D-3 {@code DlsQueryBuilder} / {@code FlsGrantBuilder}</td>
 *       <td>{@link #dlsQuery} / {@link #roleDescriptors}</td></tr>
 *   <tr><td>E-0 aggregation vocabulary</td><td>{@link #serverAggregations}</td></tr>
 *   <tr><td>E-1 {@code SearchQueryMediator} + E-2 mandatory filter</td><td>{@link #mediate}</td></tr>
 *   <tr><td>E-3 {@code ResponseFieldFilter}</td><td>{@link #filterResponse}</td></tr>
 * </table>
 *
 * <p>The single most important structural property is that {@link #dlsQuery} is the <em>only</em>
 * expression of document visibility in the file, and both enforcement paths consume it — Epic D as
 * a role-descriptor {@code query}, Epic E as an injected {@code filter} clause. §1.1 warns that the
 * two paths drifting apart is how the fallback becomes a hole; sharing the predicate is what makes
 * the parity assertion in the test a real check rather than two hand-written expectations that
 * happen to agree.
 */
final class ElasticSearchAccessContractModel {

  private ElasticSearchAccessContractModel() {}

  /** Thrown where the contract says "reject rather than drop" (§F.1 point 2). */
  static class RejectedQueryException extends RuntimeException {
    RejectedQueryException(String message) {
      super(message);
    }
  }

  /**
   * The four configurations the PoC compares. The first two are the states the contract argues
   * against; the last two are what it specifies.
   */
  enum EnforcementMode {
    /**
     * Today. {@code DatasetResource.searchDatasetIndex} hands the caller's body to {@code
     * ElasticSearchService.searchDatasets}, which runs it under the application's own privileged
     * credentials and returns {@code hits} verbatim. No document filter, no field filter, no
     * mediation. Present so that every assertion below has a demonstrated failure to contrast with.
     */
    UNMEDIATED(false, false, false, false),

    /**
     * Epic D exactly as originally specified: native DLS and FLS with the §B RESPONSE-VISIBLE
     * grant, and caller DSL passed through. This is the configuration §1.1 corrects, and the PoC
     * keeps it so the correction can be measured rather than argued — DLS filters the hit set, but
     * it does not isolate every index-wide statistic the query surface exposes.
     */
    NATIVE_UNMEDIATED(true, false, false, false),

    /**
     * Epic D as the contract now specifies it — native DLS/FLS plus §F mediation — with two changes
     * this PoC's measurements force, both recorded as findings in the test class:
     *
     * <ul>
     *   <li>the FLS grant is {@link #operationalFlsGrant()} rather than §B's RESPONSE-VISIBLE list,
     *       because a path outside the grant is not matchable (OPEN-8, measured restrictive);
     *   <li>the E-3 response filter runs on this path too, to strip the paths the wider grant now
     *       returns. This is precisely the remedy OPEN-8 names as its fallback.
     * </ul>
     */
    NATIVE(true, true, true, true),

    /**
     * Epic E: privileged credentials, §F mediation, E-2's injected filter in place of DLS, and
     * E-3's response filter in place of FLS.
     */
    FALLBACK(false, true, true, false);

    private final boolean nativeSecurity;
    private final boolean mediated;
    private final boolean responseFiltered;
    private final boolean wideGrant;

    EnforcementMode(
        boolean nativeSecurity, boolean mediated, boolean responseFiltered, boolean wideGrant) {
      this.nativeSecurity = nativeSecurity;
      this.mediated = mediated;
      this.responseFiltered = responseFiltered;
      this.wideGrant = wideGrant;
    }

    /** True when the request runs under an API key carrying a DLS query and an FLS grant. */
    boolean usesNativeSecurity() {
      return nativeSecurity;
    }

    /** The FLS grant this mode's API key carries. */
    Set<String> flsGrant() {
      return wideGrant ? operationalFlsGrant() : ElasticSearchAccessContractModel.flsGrant();
    }

    /** True when caller DSL passes through {@link #mediate} first. */
    boolean isMediated() {
      return mediated;
    }

    /** True when the response passes through {@link #filterResponse}. */
    boolean isResponseFiltered() {
      return responseFiltered;
    }

    /** True when {@link #mediate} must inject E-2's mandatory filter rather than rely on DLS. */
    boolean injectsAuthorizationFilter() {
      return mediated && !nativeSecurity;
    }
  }

  // ---------------------------------------------------------------------------------------------
  // D-3 — document visibility, expressed once
  // ---------------------------------------------------------------------------------------------

  /**
   * The contract's document-visibility predicate (§A rows 1–3 and 5–8), as a query object.
   *
   * <p>ADMIN is {@code match_all} — a document bypass only (§A row 1). Everyone else gets a
   * disjunction over exactly five clauses and no more. Rows 9–14 are DEFERred, so there is
   * deliberately no DAC clause, no institution clause and no policy-tag clause: adding one would
   * grant read access that no caller has today.
   *
   * <p>Two clauses are easy to get wrong and are called out in D-3's notes:
   *
   * <ul>
   *   <li>{@code accessPolicy.hasStudy: false} carries §A row 5. A filter that instead treated a
   *       missing {@code publicVisibility} as visible would silently deny every study-less dataset,
   *       because §A.1 requires a null on a study-bearing document to fail closed.
   *   <li>Both creator clauses are present and they are different columns. Matching only one denies
   *       the other kind of creator.
   * </ul>
   *
   * <p>Custodian matching is an exact {@code terms} match on an un-normalized {@code keyword} with
   * the caller's email passed through as-is, which is what §A.2 PRESERVEs. This ships today's
   * case-sensitivity defect on purpose; OPEN-6 is where fixing it belongs.
   */
  static JsonObject dlsQuery(Caller caller) {
    if (caller.admin()) {
      return matchAll();
    }

    JsonArray should = new JsonArray();
    should.add(term("accessPolicy.publicVisibility", true));
    should.add(term("accessPolicy.hasStudy", false));
    should.add(term("accessPolicy.datasetCreatorUserId", caller.userId()));
    should.add(term("accessPolicy.studyCreatorUserId", caller.userId()));
    should.add(
        ElasticSearchAccessContractFixtures.object(
            "terms",
            ElasticSearchAccessContractFixtures.object(
                "accessPolicy.custodianEmails", stringArray(List.of(caller.email())))));

    JsonObject bool = new JsonObject();
    bool.add("should", should);
    bool.addProperty("minimum_should_match", 1);
    return ElasticSearchAccessContractFixtures.object("bool", bool);
  }

  /**
   * Inline API-key role descriptors as D-2 would mint them: the {@link #dlsQuery} as the index
   * privilege's {@code query}, and the constant FLS grant as its {@code field_security}.
   *
   * <p>Built as a Gson tree rather than a format string so the DLS query's own JSON is escaped into
   * the {@code query} string by the serializer. Hand-escaping it is where these descriptors
   * traditionally go wrong, and a mis-escaped DLS query fails open on some clusters.
   *
   * @param grant the FLS grant. Literal paths only, no wildcard and no {@code except} form (§B.5),
   *     and the same list for admins as for everyone else (§B.7).
   */
  static String roleDescriptors(String index, Caller caller, Set<String> grant) {
    JsonObject fieldSecurity = new JsonObject();
    fieldSecurity.add("grant", stringArray(new java.util.TreeSet<>(grant)));

    JsonObject indexPrivilege = new JsonObject();
    indexPrivilege.add("names", stringArray(List.of(index)));
    indexPrivilege.add("privileges", stringArray(List.of("read")));
    indexPrivilege.addProperty("query", dlsQuery(caller).toString());
    indexPrivilege.add("field_security", fieldSecurity);

    JsonArray indices = new JsonArray();
    indices.add(indexPrivilege);

    JsonObject role = new JsonObject();
    role.add("indices", indices);
    return ElasticSearchAccessContractFixtures.object("dataset_search", role).toString();
  }

  /**
   * The FLS grant as D-3 specifies it: the §B RESPONSE-VISIBLE set, constant for every caller
   * (Decision 2).
   *
   * <p>This PoC measures it to be unshippable as the whole story, and the two reasons are asserted
   * in the test class rather than asserted here. Both are consequences of the same Elasticsearch
   * behavior — a field outside the grant does not exist as far as the search is concerned, not
   * merely as far as the response is concerned:
   *
   * <ul>
   *   <li>a {@code term} query on a non-granted path matches nothing (OPEN-8), which empties "My
   *       Data Submissions" and the {@code restrictToPublicVisibility} filter;
   *   <li>a granted multi-field does not carry its {@code .keyword} subfield, so sorting on {@code
   *       datasetName.keyword} silently returns a different page rather than failing.
   * </ul>
   *
   * Kept as a named method because it is what the contract currently says, and the tests that
   * measure its consequences need to construct it.
   */
  static Set<String> flsGrant() {
    return RESPONSE_VISIBLE;
  }

  /**
   * The grant the native path has to carry in practice: RESPONSE-VISIBLE, plus every QUERYABLE
   * path, plus the {@code .keyword} subfields of the multi-fields the product sorts on.
   *
   * <p>Wider than §B's response classification on purpose, and safe only because the E-3 response
   * filter also runs on the native path in this configuration — the grant governs what the
   * <em>search</em> can resolve, and the response filter governs what the <em>caller</em> receives.
   * Splitting the two responsibilities this way is what lets both §B axes be honoured at once;
   * native FLS alone cannot do it, which is the substance of OPEN-8.
   *
   * <p>Still no wildcard and no {@code except} form (§B.5): the subfields are enumerated, so a
   * field added to the mapping later is not served by accident.
   */
  static Set<String> operationalFlsGrant() {
    Set<String> grant = new LinkedHashSet<>(RESPONSE_VISIBLE);
    grant.addAll(QUERYABLE);
    grant.add("datasetName.keyword");
    grant.add("study.studyName.keyword");
    return Set.copyOf(grant);
  }

  // ---------------------------------------------------------------------------------------------
  // E-1 / E-2 / §F.1 — query mediation
  // ---------------------------------------------------------------------------------------------

  /**
   * Keys removed at <em>every</em> depth of the caller's DSL.
   *
   * <p>{@code _source} and {@code aggs}/{@code aggregations} are here because the server owns both
   * (§F.1): the server sets its own projection and builds aggregations from a closed vocabulary, so
   * there is no legitimate caller-supplied value for either. Removing them at every depth rather
   * than at the root is what closes §B.5b — the client's own {@code STUDIES_AGG} carries a {@code
   * _source: ["study.*"]} wildcard inside {@code aggs.studies.aggs.study_details.top_hits}, which
   * E-1's original root-level {@code remove()} left untouched.
   *
   * <p>The rest are the channels §F.2 enumerates plus the script surfaces. {@code
   * runtime_mappings}, {@code collapse}, {@code inner_hits}, {@code rescore} and {@code suggest}
   * were missing from E-1's first list; {@code runtime_mappings} is the sharpest of them, since a
   * runtime field can copy an internal value into a returnable one in a single script.
   *
   * <p>{@code fields} is deliberately <b>not</b> here — see {@link
   * #CLAUSES_WITH_A_LEGITIMATE_FIELDS_MEMBER}.
   */
  private static final Set<String> STRIPPED_AT_EVERY_DEPTH =
      Set.of(
          "_source",
          "aggs",
          "aggregations",
          "collapse",
          "docvalue_fields",
          "explain",
          "indices_boost",
          "inner_hits",
          "profile",
          "rescore",
          "runtime_mappings",
          "script",
          "script_fields",
          "stored_fields",
          "suggest");

  /**
   * Clauses in which a {@code fields} member is a list of field paths rather than a response
   * channel.
   *
   * <p>E-1's revised criteria say both "add {@code fields} to the strip list" and "strip at every
   * depth". Taken together those two instructions are wrong, and wrong in the quiet direction:
   * {@code fields} is a response channel at request level, but inside {@code multi_match}, {@code
   * query_string} and {@code highlight} it is the clause's own field list — which the product uses
   * on every library search and every highlighted column. Stripping it there does not fail loudly.
   * It silently widens the search to every field in the mapping, silently disables highlighting,
   * and removes the very reference the §F.1 validator needs in order to refuse {@code "fields":
   * ["*"]}.
   *
   * <p>So the rule has to be context-aware as well as depth-aware: strip {@code fields} where it is
   * a response channel, keep it where it names query targets, and validate it there.
   */
  private static final Set<String> CLAUSES_WITH_A_LEGITIMATE_FIELDS_MEMBER =
      Set.of("multi_match", "query_string", "simple_query_string", "combined_fields", "highlight");

  /** Query clauses whose member names are field paths. */
  private static final Set<String> FIELD_KEYED_CLAUSES =
      Set.of(
          "term",
          "terms",
          "terms_set",
          "match",
          "match_phrase",
          "match_phrase_prefix",
          "match_bool_prefix",
          "prefix",
          "wildcard",
          "regexp",
          "fuzzy",
          "range",
          "span_term",
          "distance_feature");

  /** Sort keys that name a scoring pseudo-field rather than a document field. */
  private static final Set<String> SORT_PSEUDO_FIELDS = Set.of("_score", "_doc", "_id", "_shard");

  /**
   * Applies the contract's request-side enforcement and returns the body actually sent to
   * Elasticsearch.
   *
   * <p>Order matters, and each step closes something a later step cannot:
   *
   * <ol>
   *   <li><b>Strip</b> {@link #STRIPPED_AT_EVERY_DEPTH} at every depth. After this the caller
   *       controls no projection and no aggregation, so §F.1's count-and-existence leaks have no
   *       surface left — closed by construction rather than by detecting leak techniques against a
   *       grammar that grows every release.
   *   <li><b>Validate</b> every remaining field reference against {@link
   *       ElasticSearchAccessContractFixtures#QUERYABLE} and <b>reject</b> on a violation.
   *       Rejecting rather than dropping is required: a dropped {@code filter} broadens the query
   *       and a dropped {@code sort} changes paging, so silently removing an offending clause would
   *       return <em>more</em> than the caller asked for.
   *   <li><b>Wrap</b>. The caller's surviving query goes under {@code bool.must} and, on the
   *       fallback path, the authorization predicate goes under {@code bool.filter} as a sibling.
   *       Because the root {@code query} is rebuilt by the server, no caller DSL can reach outside
   *       it — that non-removability is the whole content of E-2.
   *   <li><b>Project and aggregate</b> server-side.
   * </ol>
   *
   * @param serverTab the asset tab the caller named, or {@code null} for a plain hit search. This
   *     is the entire caller influence over aggregations that E-0 allows.
   */
  static String mediate(String callerDsl, Caller caller, EnforcementMode mode, String serverTab) {
    JsonObject root = JsonParser.parseString(callerDsl).getAsJsonObject().deepCopy();

    stripAtEveryDepth(root, null);

    Set<String> references = new LinkedHashSet<>();
    collectFieldReferences(root, null, references);
    for (String reference : references) {
      String path = normalizeFieldReference(reference);
      if (!QUERYABLE.contains(path)) {
        throw new RejectedQueryException(
            "query references a field that is not QUERYABLE: '%s' (normalized to '%s')"
                .formatted(reference, path));
      }
    }

    JsonObject callerQuery = takeCallerQuery(root);

    JsonObject bool = new JsonObject();
    JsonArray must = new JsonArray();
    must.add(callerQuery);
    bool.add("must", must);
    if (mode.injectsAuthorizationFilter()) {
      JsonArray filter = new JsonArray();
      filter.add(dlsQuery(caller));
      bool.add("filter", filter);
    }
    root.add("query", ElasticSearchAccessContractFixtures.object("bool", bool));

    // On the fallback path the server sets the projection; on the native path FLS does it, and the
    // PoC deliberately leaves _source unset there so the assertions test FLS rather than this line.
    if (!mode.usesNativeSecurity()) {
      root.add("_source", stringArray(new java.util.TreeSet<>(RESPONSE_VISIBLE)));
    }

    if (serverTab != null) {
      root.add("aggs", serverAggregations(serverTab));
      root.addProperty("size", 0);
    }

    // hits.total must be exact for Decision 1's counting guarantee to be checkable at all: the
    // default 10,000-document estimate would make a total assertion meaningless at scale.
    root.addProperty("track_total_hits", true);
    return root.toString();
  }

  /**
   * Removes the caller's {@code query} node, returning {@code match_all} when nothing usable
   * survived the strip. A caller whose entire query was a {@code script} clause lands here, and
   * substituting {@code match_all} keeps the request valid — safely, because the authorization
   * filter is applied as a sibling and does not depend on what the caller asked for.
   */
  private static JsonObject takeCallerQuery(JsonObject root) {
    JsonElement query = root.remove("query");
    if (query == null || !query.isJsonObject() || query.getAsJsonObject().isEmpty()) {
      return matchAll();
    }
    return query.getAsJsonObject();
  }

  private static void stripAtEveryDepth(JsonElement node, String parentKey) {
    if (node.isJsonArray()) {
      node.getAsJsonArray().forEach(element -> stripAtEveryDepth(element, parentKey));
      return;
    }
    if (!node.isJsonObject()) {
      return;
    }
    JsonObject object = node.getAsJsonObject();
    for (String key : List.copyOf(object.keySet())) {
      if (isStripped(key, parentKey)) {
        object.remove(key);
      } else {
        stripAtEveryDepth(object.get(key), key);
      }
    }
  }

  private static boolean isStripped(String key, String parentKey) {
    if (STRIPPED_AT_EVERY_DEPTH.contains(key)) {
      return true;
    }
    // A null parentKey is the request root, where `fields` is unambiguously a response channel.
    return "fields".equals(key)
        && (parentKey == null || !CLAUSES_WITH_A_LEGITIMATE_FIELDS_MEMBER.contains(parentKey));
  }

  /**
   * Walks the DSL collecting every path that will be resolved against the mapping.
   *
   * <p>Structural rather than key-name-based wherever possible, because the leaf clauses are the
   * only place a field path is positionally identifiable. {@code sort}, {@code highlight.fields}
   * and the {@code multi_match} family each carry paths in a different position, and each is a
   * channel §F.2 shows returns values.
   */
  private static void collectFieldReferences(JsonElement node, String key, Set<String> out) {
    if (node.isJsonArray()) {
      node.getAsJsonArray().forEach(element -> collectFieldReferences(element, key, out));
      return;
    }

    if ("sort".equals(key)) {
      collectSortReferences(node, out);
      return;
    }
    if (!node.isJsonObject()) {
      return;
    }
    JsonObject object = node.getAsJsonObject();

    if (key != null && FIELD_KEYED_CLAUSES.contains(key)) {
      out.addAll(object.keySet());
      return;
    }
    if ("exists".equals(key) && object.has("field")) {
      out.add(object.get("field").getAsString());
      return;
    }
    if ("nested".equals(key) && object.has("path")) {
      out.add(object.get("path").getAsString());
    }
    if (object.has("fields") && isMultiFieldClause(key)) {
      object.getAsJsonArray("fields").forEach(field -> out.add(field.getAsString()));
    }
    if (object.has("default_field")) {
      out.add(object.get("default_field").getAsString());
    }
    if ("highlight".equals(key) && object.has("fields")) {
      out.addAll(object.getAsJsonObject("fields").keySet());
    }

    for (Map.Entry<String, JsonElement> member : object.entrySet()) {
      collectFieldReferences(member.getValue(), member.getKey(), out);
    }
  }

  private static boolean isMultiFieldClause(String key) {
    return "multi_match".equals(key)
        || "query_string".equals(key)
        || "simple_query_string".equals(key)
        || "combined_fields".equals(key);
  }

  private static void collectSortReferences(JsonElement sort, Set<String> out) {
    if (sort.isJsonPrimitive()) {
      String field = sort.getAsString();
      if (!SORT_PSEUDO_FIELDS.contains(field)) {
        out.add(field);
      }
      return;
    }
    if (sort.isJsonObject()) {
      for (String field : sort.getAsJsonObject().keySet()) {
        if (!SORT_PSEUDO_FIELDS.contains(field)) {
          out.add(field);
        }
      }
    }
  }

  /**
   * Reduces a caller's field reference to the path the allowlist is expressed in (§F.1 point 3).
   *
   * <p>Three normalizations, and the third is a refusal rather than a rewrite. A wildcard is
   * rejected outright instead of expanded: with the server owning both {@code _source} and {@code
   * aggs}, no legitimate reference contains one, and expanding a wildcard against the mapping is
   * how an allowlist check ends up approving a path nobody enumerated.
   */
  private static String normalizeFieldReference(String reference) {
    String path = reference;
    int boost = path.indexOf('^');
    if (boost >= 0) {
      path = path.substring(0, boost);
    }
    if (path.contains("*") || path.contains("?")) {
      throw new RejectedQueryException(
          "wildcard field references are refused: '" + reference + "'");
    }
    if (path.toLowerCase(Locale.ROOT).endsWith(".keyword")) {
      path = path.substring(0, path.length() - ".keyword".length());
    }
    return path;
  }

  // ---------------------------------------------------------------------------------------------
  // E-0 — the server's aggregation vocabulary
  // ---------------------------------------------------------------------------------------------

  /**
   * Aggregations built server-side, selected by tab name (§F.1 shapes 1 and 2).
   *
   * <p>The security property is not in any individual line here — it is that this method takes a
   * tab name and nothing else. There is no input by which a caller reaches {@code min_doc_count},
   * an aggregation type, a {@code background_filter} or a field target, so the count-and-existence
   * leaks of §F.1 have no expression. Shape 2's {@code top_hits} projection is the enumerated leaf
   * list for the requested tab (§B.5a), never {@code study.*}.
   */
  static JsonObject serverAggregations(String tab) {
    return switch (tab) {
      case "datasets" -> filterAggregations();
      case "models" ->
          studiesAggregation(
              List.of(
                  "study.studyId",
                  "study.studyName",
                  "study.assets.models.name",
                  "study.assets.models.license"));
      case DRIFTED_ENUMERATION_TAB ->
          studiesAggregation(
              List.of(
                  "study.studyId",
                  "study.studyName",
                  "study.assets.models.name",
                  "study.assets.models.license",
                  "study.assets.models.internalCheckpointUri"));
      default -> throw new IllegalArgumentException("no such tab in the vocabulary: " + tab);
    };
  }

  /**
   * A vocabulary entry whose {@code top_hits} projection has drifted out of step with §B — it names
   * a {@code study.assets.models} leaf that is not RESPONSE-VISIBLE.
   *
   * <p>Not a hypothetical. §B.5a hands ownership of the {@code study.assets.*} leaf enumeration to
   * duos-ui's eleven {@code AssetDefinition}s, and OPEN-9 records that a hand-maintained backend
   * copy of that list will drift — invisibly, in whichever environment runs the other enforcement
   * path. This entry is what drift looks like at the point where it does damage: the
   * <em>server</em> asks Elasticsearch for an internal field, so neither the strip list nor the
   * field-reference validator is involved. The caller did nothing wrong.
   *
   * <p>It exists so that E-3's aggregation-channel walk has something to catch. Without a case like
   * this the §F.2 {@code aggregations.**} requirement is unfalsifiable here: once the server owns
   * the aggregation, every {@code top_hits} projection is already narrow, and deleting the walk
   * changes no result. That is worth knowing on its own — the walk defends against a mistake on our
   * side of the boundary rather than against a caller — and it is the argument for keeping it,
   * drift being the likeliest way the enumeration goes wrong.
   */
  static final String DRIFTED_ENUMERATION_TAB = "models-with-a-drifted-enumeration";

  /** Shape 1 — {@code FILTER_AGGS}. Fixed facets, no parameters, default {@code min_doc_count}. */
  private static JsonObject filterAggregations() {
    JsonObject aggs = new JsonObject();
    aggs.add("accessManagement", termsAggregation("accessManagement", 20));
    aggs.add("dataUse", termsAggregation("dataUse.primary.code", 20));
    aggs.add("dataTypes", termsAggregation("study.dataTypes", 20));
    return aggs;
  }

  /** Shape 2 — {@code STUDIES_AGG}, shared unchanged by nine study-asset tabs. */
  private static JsonObject studiesAggregation(List<String> sourceLeaves) {
    JsonObject topHits = new JsonObject();
    topHits.addProperty("size", 1);
    topHits.add("_source", stringArray(sourceLeaves));

    JsonObject inner = new JsonObject();
    inner.add("study_details", ElasticSearchAccessContractFixtures.object("top_hits", topHits));

    JsonObject studies = termsAggregation("study.studyId", 100);
    studies.add("aggs", inner);
    return ElasticSearchAccessContractFixtures.object("studies", studies);
  }

  private static JsonObject termsAggregation(String field, int size) {
    JsonObject terms = new JsonObject();
    terms.addProperty("field", field);
    terms.addProperty("size", size);
    return ElasticSearchAccessContractFixtures.object("terms", terms);
  }

  // ---------------------------------------------------------------------------------------------
  // E-3 / §F.2 — response filtering, every channel
  // ---------------------------------------------------------------------------------------------

  /**
   * Hit-level keys the response filter keeps. Everything else on a hit is dropped by name-agnostic
   * default, which is how {@code inner_hits}, {@code matched_queries} and {@code _explanation} are
   * handled without being enumerated — and how §F.2's "unrecognized channels fail closed" holds for
   * channels a future Elasticsearch version adds.
   *
   * <p>{@code sort} is <b>not</b> retained, and that is a deliberate departure from §F.2 as
   * written. §F.2 says to filter the sort channel "against the RESPONSE-VISIBLE allowlist", but
   * {@code hits.hits[*].sort} is a positional array of <em>values</em> carrying no field names, so
   * there is nothing for an allowlist to match on. It matters because the two axes overlap here:
   * {@code createUserId} is QUERYABLE and RESPONSE-INTERNAL (§B.0a), so §F.1's validator accepts it
   * as a sort key and the sort channel then echoes its value once per hit. Dropping the array is
   * the only filter that closes it; the alternative is to validate sort keys against
   * RESPONSE-VISIBLE rather than QUERYABLE in the mediator. See the corresponding test.
   */
  private static final Set<String> RETAINED_HIT_KEYS =
      Set.of("_id", "_score", "_source", "highlight", "fields");

  /**
   * Hit-level channels that are keyed by field path and can therefore be projected rather than
   * dropped. Keeping them projected rather than dropping them wholesale is what lets the product go
   * on highlighting {@code datasetName} — an enforcement model that closed every channel by
   * removing it would pass every attack here and still be unshippable.
   */
  private static final Set<String> PATH_KEYED_HIT_CHANNELS = Set.of("highlight", "fields");

  /**
   * Applies the contract's response-side allowlist to an entire search response.
   *
   * <p>The traversal is structural, not path-keyed, and that is the correction §F.2 makes to E-3:
   * {@code aggregations} nests arbitrarily and its keys are caller-named, so a filter that looked
   * for {@code hits.hits[*]._source} would miss the {@code
   * aggregations.studies.study_details.hits.hits[*]._source} that is the primary response channel
   * for nine data-library tabs. Here, <em>any</em> object carrying a {@code _source} is treated as
   * a hit wherever it occurs, at any depth, which covers {@code top_hits} and {@code inner_hits} by
   * the same rule as ordinary hits.
   */
  static JsonObject filterResponse(JsonObject response) {
    JsonObject filtered = response.deepCopy();
    filterChannels(filtered);
    return filtered;
  }

  private static void filterChannels(JsonElement node) {
    if (node.isJsonArray()) {
      node.getAsJsonArray().forEach(ElasticSearchAccessContractModel::filterChannels);
      return;
    }
    if (!node.isJsonObject()) {
      return;
    }
    JsonObject object = node.getAsJsonObject();

    if (object.has("_source")) {
      for (String key : List.copyOf(object.keySet())) {
        if (!RETAINED_HIT_KEYS.contains(key)) {
          object.remove(key);
        }
      }
      prune(object.get("_source"), "", RESPONSE_VISIBLE);
      for (String channel : PATH_KEYED_HIT_CHANNELS) {
        // Keyed by full dotted path rather than nested, which prune handles by the same rule: the
        // key "study.piName" matches the allowed path "study.piName" exactly.
        if (object.has(channel)) {
          prune(object.get(channel), "", RESPONSE_VISIBLE);
        }
      }
      return;
    }

    object.entrySet().forEach(member -> filterChannels(member.getValue()));
  }

  /**
   * Projects a document onto an allowlist of dotted leaf paths, in place.
   *
   * <p>Allowlist rather than denylist, per §B.5: a key is kept only if its path is allowed exactly,
   * or is a prefix of an allowed path and survives recursion. A field added to {@code DatasetTerm}
   * without being classified in §B is therefore dropped rather than served, which is the failure
   * §B.6 asks for.
   *
   * <p>Arrays recurse at the same path, so an array of objects is projected element-wise. That is
   * what {@code dataUse.primary.code} and {@code study.assets.models.name} require, and it is the
   * case a projection written as "copy each allowed path out of the document" quietly gets wrong.
   */
  private static void prune(JsonElement node, String prefix, Set<String> allowed) {
    if (node.isJsonArray()) {
      node.getAsJsonArray().forEach(element -> prune(element, prefix, allowed));
      return;
    }
    if (!node.isJsonObject()) {
      return;
    }
    JsonObject object = node.getAsJsonObject();
    List<String> removals = new ArrayList<>();
    for (String key : List.copyOf(object.keySet())) {
      String path = prefix.isEmpty() ? key : prefix + "." + key;
      if (allowed.contains(path)) {
        continue;
      }
      if (isPrefixOfAllowedPath(path, allowed)) {
        prune(object.get(key), path, allowed);
      } else {
        removals.add(key);
      }
    }
    removals.forEach(object::remove);
  }

  private static boolean isPrefixOfAllowedPath(String path, Set<String> allowed) {
    String withSeparator = path + ".";
    return allowed.stream().anyMatch(candidate -> candidate.startsWith(withSeparator));
  }

  // ---------------------------------------------------------------------------------------------

  static JsonObject matchAll() {
    return ElasticSearchAccessContractFixtures.object("match_all", new JsonObject());
  }

  private static JsonObject term(String field, boolean value) {
    JsonObject inner = new JsonObject();
    inner.addProperty(field, value);
    return ElasticSearchAccessContractFixtures.object("term", inner);
  }

  private static JsonObject term(String field, int value) {
    JsonObject inner = new JsonObject();
    inner.addProperty(field, value);
    return ElasticSearchAccessContractFixtures.object("term", inner);
  }
}
