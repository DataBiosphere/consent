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
   * {@code fields} is a response channel at request level, but inside {@code multi_match} and
   * {@code highlight} it is the clause's own field list — which the product uses on every library
   * search and every highlighted column. Stripping it there does not fail loudly. It silently
   * widens the search to every field in the mapping, silently disables highlighting, and removes
   * the very reference the §F.1 validator needs in order to refuse {@code "fields": ["*"]}.
   *
   * <p>So the rule has to be context-aware as well as depth-aware: strip {@code fields} where it is
   * a response channel, keep it where it names query targets, and validate it there.
   *
   * <p>E-1's criteria name three more clauses with a legitimate {@code fields} member — {@code
   * query_string}, {@code simple_query_string} and {@code combined_fields}. They are absent here
   * because {@link #SUPPORTED_QUERY_CLAUSES} refuses all three outright, so their {@code fields}
   * member never reaches validation. This set stays a separate list rather than being derived from
   * that one: stripping runs before shape validation and must be readable without it.
   */
  private static final Set<String> CLAUSES_WITH_A_LEGITIMATE_FIELDS_MEMBER =
      Set.of("multi_match", "highlight");

  /** Sort keys that name a scoring pseudo-field rather than a document field. */
  private static final Set<String> SORT_PSEUDO_FIELDS = Set.of("_score", "_doc", "_id", "_shard");

  // --- §F.1 rule 0 — the closed shape allowlist ------------------------------------------------

  /**
   * Request-level members the mediator understands. Everything else is refused.
   *
   * <p>This is the correction the PoC's finding 5 forces, and it is a change of principle rather
   * than an addition to a list. §F.1 rule 2 says to validate "the remaining field references",
   * which presumes every field reference can be found — true only for the shapes somebody
   * enumerated. The DSL has several where it is false: {@code query_string} hides its references in
   * query <em>text</em>, {@code knn} carries a {@code field} at request level, {@code pit} replaces
   * the target index outright, and a {@code terms} lookup reads its values out of another document
   * entirely. A reference collector that walks known positions returns nothing for any of them and
   * reports success.
   *
   * <p>So the mediator states what it supports instead of what it forbids, on both axes: this set
   * at request level, {@link #SUPPORTED_QUERY_CLAUSES} inside {@code query}, and a member allowlist
   * within each clause. A shape nobody enumerated is refused, which is the same allowlist principle
   * §B.5 applies to fields and §F.2 applies to response channels, applied to request grammar.
   *
   * <p>It also changes what {@link #STRIPPED_AT_EVERY_DEPTH} is for. The two lists overlap
   * completely at request level — every key on the strip list is outside this set — but they are
   * not redundant, and the order they run in is the reason. Stripping is the <em>compatibility</em>
   * layer: it quietly removes the channels the server owns, so a client sending today's {@code
   * _source} and {@code aggs} keeps working. This set is the <em>security</em> layer: it refuses
   * whatever the first pass did not account for. Reverse the order and duos-ui's current request
   * body is rejected on its first call; drop the second layer and the mediator is back to guessing
   * about shapes nobody enumerated. The strip list also keeps doing work no member allowlist can,
   * at <em>depth</em>, where §B.5b's nested {@code _source} lives.
   */
  private static final Set<String> SUPPORTED_REQUEST_MEMBERS =
      Set.of("query", "sort", "highlight", "from", "size", "search_after", "track_total_hits");

  /**
   * Query clauses the mediator supports, and the members each one may carry.
   *
   * <p>Chosen from what the product sends, not from what Elasticsearch offers. Three exclusions are
   * the substance of finding 5 and deserve naming, because each looks harmless next to clauses that
   * are on the list:
   *
   * <ul>
   *   <li>{@code query_string} — its {@code query} member is a mini-language with fielded terms
   *       ({@code accessPolicy.custodianEmails:x@example.org}), {@code _exists_}, ranges and
   *       grouping. Extracting field references from it means implementing a parser for a grammar
   *       Lucene extends, and rule 3 already refuses wildcards rather than approximate a grammar.
   *   <li>{@code simple_query_string} and {@code combined_fields} — no fielded syntax, but both
   *       fall back to {@code index.query.default_field} when {@code fields} is omitted, and that
   *       defaults to {@code *}. A clause with no field reference at all therefore searches every
   *       field in the mapping. This is why {@link #validateMultiMatch} requires {@code fields}
   *       rather than treating its absence as nothing to validate.
   *   <li>{@code more_like_this} — its {@code like} member takes document <em>ids</em>, so the
   *       clause reads term vectors out of documents the caller cannot see and searches with them.
   *       No field reference is involved at all.
   * </ul>
   *
   * <p>Adding a clause here is a security decision, not a convenience one: the question to answer
   * first is where the clause can name a field, and whether every such position is a member name or
   * a string in a fixed slot.
   */
  private static final Map<String, Set<String>> SUPPORTED_QUERY_CLAUSES =
      Map.of(
          "match_all", Set.of("boost"),
          "match_none", Set.of(),
          "bool", Set.of("must", "should", "filter", "must_not", "minimum_should_match", "boost"),
          "exists", Set.of("field"),
          "multi_match",
              Set.of(
                  "query",
                  "fields",
                  "type",
                  "operator",
                  "minimum_should_match",
                  "tie_breaker",
                  "boost"),
          "term", Set.of("value", "boost"),
          "terms", Set.of(),
          "match", Set.of("query", "operator", "fuzziness", "minimum_should_match", "boost"),
          "match_phrase", Set.of("query", "slop", "boost"),
          "range", Set.of("gt", "gte", "lt", "lte", "boost", "format"));

  /**
   * Clauses whose single member name is a field path, keyed to the options that member may carry.
   */
  private static final Set<String> FIELD_KEYED_CLAUSES =
      Set.of("term", "terms", "match", "match_phrase", "range");

  /** {@code bool} members that are themselves queries, as opposed to scalar options. */
  private static final Set<String> BOOL_QUERY_MEMBERS =
      Set.of("must", "should", "filter", "must_not");

  /**
   * Options a {@code sort} entry may carry.
   *
   * <p>Deliberately excludes {@code nested}, {@code _script} and {@code _geo_distance}, each of
   * which embeds a query or a script inside the sort and so carries field references the {@code
   * sort}-key walk never looks at.
   */
  private static final Set<String> SORT_OPTIONS =
      Set.of("order", "mode", "missing", "unmapped_type", "numeric_type", "format");

  /**
   * Options {@code highlight} and each of its per-field entries may carry.
   *
   * <p>Excludes {@code highlight_query} and {@code matched_fields}, which are the two members that
   * name fields somewhere other than the {@code highlight.fields} keys §F.1 rule 2 walks.
   */
  private static final Set<String> HIGHLIGHT_OPTIONS =
      Set.of(
          "pre_tags",
          "post_tags",
          "fragment_size",
          "number_of_fragments",
          "require_field_match",
          "no_match_size",
          "order",
          "type");

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
   *   <li><b>Refuse</b> any request member, query clause, clause member or clause value shape
   *       outside {@link #SUPPORTED_REQUEST_MEMBERS} and {@link #SUPPORTED_QUERY_CLAUSES}. This
   *       step is what makes the next one meaningful: a field-reference walk can only be trusted
   *       over shapes whose field positions are known, and several DSL clauses hide references
   *       where no walk reaches them (finding 5).
   *   <li><b>Validate</b> every field reference the walk collected against {@link
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
    return mediate(callerDsl, caller, mode, serverTab, true);
  }

  /**
   * {@link #mediate} with its two request-side validations switched off — the request-shape walk
   * and the aggregation-vocabulary check — but everything else intact: the strip list still runs,
   * E-2's filter is still injected, the server still owns the projection and the aggregations, and
   * E-3 still filters the response.
   *
   * <p>This is the "control removed" leg the PoC's teeth assertions need. Two of the leaks measured
   * here carry no marker and produce a perfectly ordinary-looking response — a query-shape oracle
   * discloses by hit count and an aggregation bucket key is indistinguishable from a legitimate
   * facet — so {@code assertNoLeak}'s scan cannot see them, and a test that only asserted "the
   * fixed configuration is clean" would pass whether or not the validation existed.
   */
  static String mediateWithoutRequestValidation(
      String callerDsl, Caller caller, EnforcementMode mode, String serverTab) {
    return mediate(callerDsl, caller, mode, serverTab, false);
  }

  private static String mediate(
      String callerDsl,
      Caller caller,
      EnforcementMode mode,
      String serverTab,
      boolean validateRequest) {
    JsonObject root = JsonParser.parseString(callerDsl).getAsJsonObject().deepCopy();

    stripAtEveryDepth(root, null);

    if (validateRequest) {
      Set<String> references = new LinkedHashSet<>();
      validateRequest(root, references);
      for (String reference : references) {
        String path = normalizeFieldReference(reference);
        if (!QUERYABLE.contains(path)) {
          throw new RejectedQueryException(
              "query references a field that is not QUERYABLE: '%s' (normalized to '%s')"
                  .formatted(reference, path));
        }
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
      root.add(
          "aggs",
          validateRequest ? serverAggregations(serverTab) : unvalidatedAggregations(serverTab));
      root.addProperty("size", 0);
    }

    // hits.total must be exact for Decision 1's counting guarantee to be checkable at all: the
    // default 10,000-document estimate would make a total assertion meaningless at scale.
    root.addProperty("track_total_hits", true);
    return root.toString();
  }

  /**
   * Removes the caller's {@code query} node, returning {@code match_all} when the request carried
   * none. Substituting {@code match_all} is safe because the authorization filter is applied as a
   * sibling and does not depend on what the caller asked for.
   *
   * <p>Note that a request whose query the strip pass <em>emptied</em> — one whose entire query was
   * a {@code script} clause, say — does not reach here: an empty clause object names no query type,
   * and {@link #validateQuery} refuses it. That is the §F.1 reject-rather-than-drop rule applied to
   * the strip list's own output, and it is a change from substituting {@code match_all} there,
   * which silently broadened such a request to the caller's whole authorized set.
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
   * Walks the request against the closed shape allowlist, collecting every path that will be
   * resolved against the mapping and refusing anything the walk cannot account for.
   *
   * <p>Validation and collection are one traversal on purpose. Separating them is what allowed
   * finding 5: a collector that returns an empty set for a shape it does not recognize is
   * indistinguishable from one that correctly found no references, so the caller of a separate
   * collector has no way to tell "nothing to check" from "no idea what this is". Here a reference
   * can only be missed by a clause that reached {@code default} in one of the switches below, and
   * {@code default} throws.
   */
  private static void validateRequest(JsonObject root, Set<String> references) {
    for (Map.Entry<String, JsonElement> member : root.entrySet()) {
      String name = member.getKey();
      if (!SUPPORTED_REQUEST_MEMBERS.contains(name)) {
        throw new RejectedQueryException(
            "unsupported request member: '%s'. Supported: %s"
                .formatted(name, new java.util.TreeSet<>(SUPPORTED_REQUEST_MEMBERS)));
      }
      switch (name) {
        case "query" -> validateQuery(member.getValue(), references);
        case "sort" -> validateSort(member.getValue(), references);
        case "highlight" -> validateHighlight(member.getValue(), references);
        // from/size/search_after/track_total_hits carry paging and counting values, never a path.
        default -> {}
      }
    }
  }

  /** Validates one query clause — an object naming exactly one supported clause type. */
  private static void validateQuery(JsonElement node, Set<String> references) {
    JsonObject clause = requireObject(node, "query");
    if (clause.size() != 1) {
      throw new RejectedQueryException(
          "a query clause names exactly one query type, found " + clause.keySet());
    }
    Map.Entry<String, JsonElement> only = clause.entrySet().iterator().next();
    String type = only.getKey();
    Set<String> supportedMembers = SUPPORTED_QUERY_CLAUSES.get(type);
    if (supportedMembers == null) {
      throw new RejectedQueryException(
          "unsupported query clause: '%s'. Supported: %s"
              .formatted(type, new java.util.TreeSet<>(SUPPORTED_QUERY_CLAUSES.keySet())));
    }

    JsonObject body = requireObject(only.getValue(), type);
    if (FIELD_KEYED_CLAUSES.contains(type)) {
      validateFieldKeyedClause(type, body, supportedMembers, references);
      return;
    }
    switch (type) {
      case "bool" -> validateBool(body, supportedMembers, references);
      case "exists" -> {
        requireMembers(type, body, supportedMembers);
        references.add(stringMember(body, "field", type));
      }
      case "multi_match" -> validateMultiMatch(body, supportedMembers, references);
      // match_all / match_none: no field reference, and requireMembers refuses everything but
      // boost.
      default -> requireMembers(type, body, supportedMembers);
    }
  }

  private static void validateBool(
      JsonObject bool, Set<String> supportedMembers, Set<String> references) {
    requireMembers("bool", bool, supportedMembers);
    for (Map.Entry<String, JsonElement> member : bool.entrySet()) {
      if (!BOOL_QUERY_MEMBERS.contains(member.getKey())) {
        continue;
      }
      JsonElement value = member.getValue();
      if (value.isJsonArray()) {
        value.getAsJsonArray().forEach(element -> validateQuery(element, references));
      } else {
        validateQuery(value, references);
      }
    }
  }

  /**
   * Validates a clause whose single member name is the field path, and whose member <em>value</em>
   * is the thing being matched.
   *
   * <p>The value shape is checked as well as the field name, which the reference-collector design
   * never did. {@code terms} is the reason: its value may be a literal array or a <em>lookup</em>
   * ({@code {"index":…,"id":…,"path":…}}) that fetches the terms out of another document. A lookup
   * names its source field in {@code path}, in a position no field-reference walk covers, and it
   * reads that field from a document the caller has no authorization for at all — the DLS query and
   * the injected filter both bound the <em>search</em>, not the lookup's GET. Refusing the shape is
   * the only control that applies.
   */
  private static void validateFieldKeyedClause(
      String type, JsonObject clause, Set<String> supportedOptions, Set<String> references) {
    if (clause.size() != 1) {
      throw new RejectedQueryException(
          "a %s clause names exactly one field, found %s".formatted(type, clause.keySet()));
    }
    Map.Entry<String, JsonElement> only = clause.entrySet().iterator().next();
    references.add(only.getKey());
    JsonElement value = only.getValue();

    if ("terms".equals(type)) {
      boolean literalValues =
          value.isJsonArray()
              && value.getAsJsonArray().asList().stream().allMatch(JsonElement::isJsonPrimitive);
      if (!literalValues) {
        throw new RejectedQueryException(
            "a terms clause must carry a literal array of values; the terms-lookup form reads a "
                + "field out of a document the caller may not be authorized for");
      }
      return;
    }
    if (value.isJsonPrimitive()) {
      return;
    }
    requireMembers(type, requireObject(value, type), supportedOptions);
  }

  /**
   * Validates a {@code multi_match}, requiring it to name its fields.
   *
   * <p>An omitted {@code fields} is not "no references to check": Elasticsearch falls back to
   * {@code index.query.default_field}, which defaults to {@code *}, so the clause searches every
   * field in the mapping — including every RESPONSE-INTERNAL one — and the caller learns which
   * documents matched. This is the same failure E-1's "strip {@code fields} at every depth"
   * instruction would have produced on every library search (§F.1 rule 1), arriving by a different
   * route.
   */
  private static void validateMultiMatch(
      JsonObject clause, Set<String> supportedMembers, Set<String> references) {
    requireMembers("multi_match", clause, supportedMembers);
    if (!clause.has("fields")) {
      throw new RejectedQueryException(
          "a multi_match clause must name its fields; without them it falls back to "
              + "index.query.default_field ('*') and searches every field in the mapping");
    }
    for (JsonElement field : clause.getAsJsonArray("fields")) {
      references.add(field.getAsString());
    }
  }

  private static void validateSort(JsonElement sort, Set<String> references) {
    if (sort.isJsonArray()) {
      sort.getAsJsonArray().forEach(element -> validateSort(element, references));
      return;
    }
    if (sort.isJsonPrimitive()) {
      addSortKey(sort.getAsString(), references);
      return;
    }
    for (Map.Entry<String, JsonElement> entry : requireObject(sort, "sort").entrySet()) {
      addSortKey(entry.getKey(), references);
      if (!entry.getValue().isJsonPrimitive()) {
        requireMembers("sort", requireObject(entry.getValue(), "sort"), SORT_OPTIONS);
      }
    }
  }

  private static void addSortKey(String key, Set<String> references) {
    if (!SORT_PSEUDO_FIELDS.contains(key)) {
      references.add(key);
    }
  }

  private static void validateHighlight(JsonElement node, Set<String> references) {
    JsonObject highlight = requireObject(node, "highlight");
    for (Map.Entry<String, JsonElement> member : highlight.entrySet()) {
      if (HIGHLIGHT_OPTIONS.contains(member.getKey())) {
        continue;
      }
      if (!"fields".equals(member.getKey())) {
        throw new RejectedQueryException(
            "unsupported highlight member: '%s'".formatted(member.getKey()));
      }
      for (Map.Entry<String, JsonElement> field :
          requireObject(member.getValue(), "highlight.fields").entrySet()) {
        references.add(field.getKey());
        requireMembers(
            "highlight.fields",
            requireObject(field.getValue(), "highlight.fields"),
            HIGHLIGHT_OPTIONS);
      }
    }
  }

  private static JsonObject requireObject(JsonElement node, String what) {
    if (node == null || !node.isJsonObject()) {
      throw new RejectedQueryException("'%s' must be a JSON object".formatted(what));
    }
    return node.getAsJsonObject();
  }

  private static void requireMembers(String what, JsonObject object, Set<String> supported) {
    for (String member : object.keySet()) {
      if (!supported.contains(member)) {
        throw new RejectedQueryException(
            "unsupported member '%s' of '%s'. Supported: %s"
                .formatted(member, what, new java.util.TreeSet<>(supported)));
      }
    }
  }

  private static String stringMember(JsonObject object, String member, String what) {
    JsonElement value = object.get(member);
    if (value == null || !value.isJsonPrimitive()) {
      throw new RejectedQueryException("'%s' requires a '%s' member".formatted(what, member));
    }
    return value.getAsString();
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
    JsonObject aggregations = unvalidatedAggregations(tab);
    validateAggregationVocabulary(aggregations);
    return aggregations;
  }

  /**
   * Aggregation shapes E-0's vocabulary may express, and the parameters each may set. §F.1's shapes
   * 1 and 2 and nothing else.
   *
   * <p>Closing the parameter list matters as much as closing the type list: {@code min_doc_count},
   * {@code include}, {@code background_filter} and {@code order} are the members §F.1's rejected
   * denylist was built around, and a vocabulary that cannot express them cannot drift into them.
   */
  private static final Map<String, Set<String>> SUPPORTED_AGGREGATIONS =
      Map.of("terms", Set.of("field", "size"), "top_hits", Set.of("size", "_source"));

  /**
   * Checks a vocabulary entry before it runs — the control finding 6 shows has no response-side
   * equivalent.
   *
   * <p>§F.2 asks for {@code aggregations.**.buckets[*].key} to be "projected defensively" against
   * RESPONSE-VISIBLE. It cannot be. A bucket key is a bare value with no field name attached, and
   * it is structurally identical to the {@code accessManagement} keys the filter panel is built
   * from, so nothing in the response distinguishes a legitimate facet key from an internal one —
   * the same argument §F.2 already accepts for the sort channel. {@link #filterResponse} therefore
   * passes bucket keys through, and the PoC asserts that it does.
   *
   * <p>What is available instead is the request. The vocabulary is server-owned (§F.1), so its
   * field targets are known before execution, and checking them there covers both drift shapes at
   * once: a {@code terms} target becomes a bucket key and a {@code top_hits} projection becomes a
   * document, so both are checked against RESPONSE-VISIBLE rather than QUERYABLE.
   *
   * <p>The vocabulary is a constant, so in production this belongs in a test over every entry as
   * well as on the request path — the check here fails closed at runtime, but a build-time failure
   * is what catches a drifted enumeration before it reaches an environment.
   */
  static void validateAggregationVocabulary(JsonObject aggregations) {
    for (Map.Entry<String, JsonElement> named : aggregations.entrySet()) {
      JsonObject body = requireObject(named.getValue(), "aggregation '" + named.getKey() + "'");
      for (Map.Entry<String, JsonElement> member : body.entrySet()) {
        String type = member.getKey();
        if ("aggs".equals(type) || "aggregations".equals(type)) {
          validateAggregationVocabulary(requireObject(member.getValue(), type));
          continue;
        }
        Set<String> parameters = SUPPORTED_AGGREGATIONS.get(type);
        if (parameters == null) {
          throw new RejectedQueryException(
              "aggregation type outside the vocabulary: '%s' in '%s'. Supported: %s"
                  .formatted(
                      type,
                      named.getKey(),
                      new java.util.TreeSet<>(SUPPORTED_AGGREGATIONS.keySet())));
        }
        JsonObject aggregation = requireObject(member.getValue(), type);
        requireMembers(type, aggregation, parameters);
        if ("terms".equals(type)) {
          requireResponseVisible(
              stringMember(aggregation, "field", type), "a terms bucket key is the field's value");
        } else {
          if (!aggregation.has("_source")) {
            throw new RejectedQueryException(
                "a top_hits entry must set its own _source; without one it returns whole documents");
          }
          for (JsonElement leaf : aggregation.getAsJsonArray("_source")) {
            requireResponseVisible(leaf.getAsString(), "top_hits returns whole documents");
          }
        }
      }
    }
  }

  private static void requireResponseVisible(String reference, String why) {
    String path = normalizeFieldReference(reference);
    if (!RESPONSE_VISIBLE.contains(path)) {
      throw new RejectedQueryException(
          "the aggregation vocabulary targets '%s', which is not RESPONSE-VISIBLE (%s)"
              .formatted(reference, why));
    }
  }

  /** The vocabulary entry for {@code tab}, exactly as written. */
  private static JsonObject unvalidatedAggregations(String tab) {
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
      case DRIFTED_BUCKET_KEY_TAB -> driftedBucketKeyAggregations();
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

  /**
   * A second drifted vocabulary entry, and the one that shows §F.2's bucket-key row cannot be
   * implemented as written: its {@code terms} aggregations target RESPONSE-INTERNAL paths directly,
   * so the <em>bucket keys</em> are the internal values.
   *
   * <p>§F.2 says to "project defensively" {@code aggregations.**.buckets[*].key}. There is nothing
   * to project against. A bucket key is a bare value carrying no field name — structurally
   * identical to the {@code accessManagement} facet keys the filter panel depends on — so a
   * response-side allowlist has no way to tell a legitimate facet key from an internal one, and
   * {@link #filterResponse} passes both through. That is the same argument §F.2 already accepts for
   * the sort channel, applied to a row §F.2 did not apply it to.
   *
   * <p>Which leaves the request side. The field targets are known there, they belong to the server,
   * and checking them is {@link #validateAggregationVocabulary}. Both fields below are exactly the
   * §F.2 case: {@code accessPolicy.custodianEmails} is the example the row itself gives, and {@code
   * study.throughBioId} carries an {@code INTERNAL-*} marker so the leak is visible to the PoC's
   * scan rather than only to a reader who knows what the values mean.
   */
  static final String DRIFTED_BUCKET_KEY_TAB = "models-with-a-drifted-bucket-key";

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

  /** {@link #DRIFTED_BUCKET_KEY_TAB}'s facets: ordinary {@code terms} aggs on internal paths. */
  private static JsonObject driftedBucketKeyAggregations() {
    JsonObject aggs = new JsonObject();
    aggs.add("custodians", termsAggregation("accessPolicy.custodianEmails", 100));
    aggs.add("bioIds", termsAggregation("study.throughBioId", 100));
    return aggs;
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
