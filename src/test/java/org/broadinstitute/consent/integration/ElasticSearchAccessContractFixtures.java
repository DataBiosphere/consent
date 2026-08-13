package org.broadinstitute.consent.integration;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Index, documents, allowlists and callers for {@link ElasticSearchLeakDefensePocTest}.
 *
 * <p>This is the corpus the proof-of-concept asserts against, and it is deliberately built so that
 * a leak is unambiguous rather than a matter of interpretation. Two devices do that:
 *
 * <ul>
 *   <li><b>Restricted-document markers.</b> Each non-public dataset carries a unique token in a
 *       field that is RESPONSE-VISIBLE by classification — {@code study.piName}. Because the field
 *       is visible, no field filter can be credited with hiding it: the token can only be absent
 *       from a response if the <em>document</em> was excluded. Finding {@code PI-RESTRICTED-*}
 *       anywhere in a response to a caller not authorized for that dataset is a Decision 1
 *       violation, whatever channel carried it.
 *   <li><b>Universal internal markers.</b> Every document carries tokens in paths that are
 *       RESPONSE-INTERNAL for <em>every</em> caller including admins ({@code study.throughBioId},
 *       the dynamic property maps, {@code submitter.institution.name}, {@code
 *       dataUse.primary.internalRationale}). Finding {@code INTERNAL-*} in any response is a §B
 *       violation regardless of who asked.
 * </ul>
 *
 * <p>The two marker families make the central assertion a single string scan over the entire
 * serialized response (see {@code assertNoLeak}), which is what lets the test cover response
 * channels neither it nor the filter enumerates — the §F.2 "unrecognized channels fail closed"
 * requirement, checked from the outside.
 *
 * <p>Field paths and tiers below are transcribed from {@code docs/plans/es-access-contract.md} §B.
 * The set is a representative subset, not the whole index: one field from each §B combination that
 * carries a distinct enforcement obligation.
 */
final class ElasticSearchAccessContractFixtures {

  private ElasticSearchAccessContractFixtures() {}

  /** Index used by the PoC, separate from the {@code dataset-test} index the other classes seed. */
  static final String INDEX = "dataset-leak-defense-poc";

  // ---------------------------------------------------------------------------------------------
  // Field classification — contract §B
  // ---------------------------------------------------------------------------------------------

  /**
   * RESPONSE-VISIBLE literal paths (contract §B). This is simultaneously D-3's constant FLS grant
   * and E-3's response allowlist — Decision 2 means there is one bundle, so one list serves both,
   * and using the same constant for both paths is what makes the parity assertion meaningful.
   *
   * <p>Enumerated leaves only, no wildcards (§B.5). {@code dataUse.primary.*} and {@code
   * study.assets.models.*} are here to exercise the array-of-objects and dynamic-map cases, which
   * are the two shapes a naive projection gets wrong.
   */
  static final Set<String> RESPONSE_VISIBLE =
      Set.of(
          "datasetId",
          "datasetIdentifier",
          "datasetName",
          "participantCount",
          "accessManagement",
          "dataUse.primary.code",
          "dataUse.primary.description",
          "study.studyId",
          "study.studyName",
          "study.piName",
          "study.dataTypes",
          "study.dataCustodianEmail",
          "study.assets.models.name",
          "study.assets.models.license",
          "submitter.displayName");

  /**
   * QUERYABLE literal paths (contract §B, query axis). A superset of {@link #RESPONSE_VISIBLE} in
   * exactly three places, and each one is load-bearing:
   *
   * <ul>
   *   <li>{@code createUserId} and {@code study.dataSubmitterId} — RESPONSE-INTERNAL but QUERYABLE,
   *       because {@code DatasetSubmissions.tsx} builds "My Data Submissions" from {@code term}
   *       clauses on both and renders neither (§B.0a). These are the paths OPEN-8 is about.
   *   <li>{@code study.publicVisibility} — QUERYABLE until G-1 removes {@code
   *       useLibraryData.ts:44}.
   * </ul>
   */
  static final Set<String> QUERYABLE = queryable();

  private static Set<String> queryable() {
    Set<String> paths = new LinkedHashSet<>(RESPONSE_VISIBLE);
    paths.add("createUserId");
    paths.add("study.dataSubmitterId");
    paths.add("study.publicVisibility");
    return Set.copyOf(paths);
  }

  /**
   * Paths that must never be returned to any caller, used only to give assertion failures a useful
   * message. Enforcement is by allowlist ({@link #RESPONSE_VISIBLE}); this list is documentation.
   */
  static final List<String> RESPONSE_INTERNAL_EXAMPLES =
      List.of(
          "accessPolicy",
          "createUserId",
          "study.dataSubmitterId",
          "study.publicVisibility",
          "study.throughBioId",
          "study.data",
          "data",
          "submitter.institution",
          "updateUser",
          "dataUse.primary.internalRationale");

  // ---------------------------------------------------------------------------------------------
  // Callers — contract §A
  // ---------------------------------------------------------------------------------------------

  /**
   * A search caller, carrying exactly what C-1 is specified to resolve: user id, email
   * <em>unnormalized</em> for the exact matching §A.2 preserves, and the global admin role. No DAC,
   * institution, allowlist or policy-tag context — rows 9–14 are DEFERred.
   *
   * @param visibleIdentifiers the {@code datasetIdentifier}s this caller is authorized to read,
   *     derived by hand from §A rows 1–3 and 5–8 against {@link #documents()}
   * @param forbiddenMarkers restricted-document markers this caller must never receive
   */
  record Caller(
      String label,
      int userId,
      String email,
      boolean admin,
      Set<String> visibleIdentifiers,
      Set<String> forbiddenMarkers) {}

  /** Marker in the PI name of each non-public study, one per document. */
  static final String MARKER_RESTRICTED_CUSTODIAN_STUDY = "PI-RESTRICTED-VERMILION";

  static final String MARKER_RESTRICTED_UNRELATED_STUDY = "PI-RESTRICTED-CINNABAR";
  static final String MARKER_RESTRICTED_OWN_SUBMISSION = "PI-RESTRICTED-OCHRE";
  static final String MARKER_PUBLIC_STUDY = "PI-PUBLIC-AZURE";

  /** Prefix shared by every marker in a RESPONSE-INTERNAL path, in every document. */
  static final String INTERNAL_MARKER_PREFIX = "INTERNAL-";

  static final Set<String> ALL_RESTRICTED_MARKERS =
      Set.of(
          MARKER_RESTRICTED_CUSTODIAN_STUDY,
          MARKER_RESTRICTED_UNRELATED_STUDY,
          MARKER_RESTRICTED_OWN_SUBMISSION);

  /**
   * A caller with no relationship to any dataset. The primary adversary: everything the contract
   * restricts is restricted from this caller, so a single run over the attack corpus as STRANGER
   * exercises every document rule at once.
   */
  static final Caller STRANGER =
      new Caller(
          "stranger",
          99,
          "stranger@example.org",
          false,
          Set.of("DUOS-00001", "DUOS-00004"),
          ALL_RESTRICTED_MARKERS);

  /** Custodian of the restricted study behind DUOS-00002, by email (§A row 8). */
  static final Caller CUSTODIAN =
      new Caller(
          "custodian",
          42,
          "custodian@example.org",
          false,
          Set.of("DUOS-00001", "DUOS-00002", "DUOS-00004"),
          Set.of(MARKER_RESTRICTED_UNRELATED_STUDY, MARKER_RESTRICTED_OWN_SUBMISSION));

  /** Creator of the restricted DUOS-00005 (§A rows 6/7) — the "My Data Submissions" caller. */
  static final Caller SUBMITTER =
      new Caller(
          "submitter",
          7,
          "submitter@example.org",
          false,
          Set.of("DUOS-00001", "DUOS-00004", "DUOS-00005"),
          Set.of(MARKER_RESTRICTED_CUSTODIAN_STUDY, MARKER_RESTRICTED_UNRELATED_STUDY));

  /**
   * Admin: full document bypass, no projection bypass (§A row 1, §B.7). Every document is visible,
   * so no restricted marker is forbidden — but the internal markers still are.
   */
  static final Caller ADMIN =
      new Caller(
          "admin",
          1,
          "admin@example.org",
          true,
          Set.of("DUOS-00001", "DUOS-00002", "DUOS-00003", "DUOS-00004", "DUOS-00005"),
          Set.of());

  static List<Caller> allCallers() {
    return List.of(STRANGER, CUSTODIAN, SUBMITTER, ADMIN);
  }

  /**
   * A case-mismatched variant of {@link #CUSTODIAN}'s email. §A.2 PRESERVEs today's case-sensitive
   * comparison, so this caller must <em>not</em> be authorized for DUOS-00002 — the contract ships
   * that defect on purpose rather than newly authorizing through search only (OPEN-6).
   */
  static final Caller CASE_MISMATCHED_CUSTODIAN =
      new Caller(
          "case-mismatched-custodian",
          43,
          "Custodian@Example.org",
          false,
          Set.of("DUOS-00001", "DUOS-00004"),
          ALL_RESTRICTED_MARKERS);

  // ---------------------------------------------------------------------------------------------
  // Index mapping
  // ---------------------------------------------------------------------------------------------

  /**
   * Mapping for the PoC index. {@code study.piName} and {@code study.dataCustodianEmail} are {@code
   * keyword} because the §F.1 aggregation attacks enumerate their global term dictionaries, which
   * is only possible on an aggregatable field — indexing them as {@code text} would make those
   * attacks fail for the wrong reason.
   */
  static final String MAPPING =
      """
      {"mappings":{"properties":{
        "datasetId":{"type":"integer"},
        "datasetIdentifier":{"type":"keyword"},
        "datasetName":{"type":"text","fields":{"keyword":{"type":"keyword"}}},
        "participantCount":{"type":"integer"},
        "accessManagement":{"type":"keyword"},
        "createUserId":{"type":"integer"},
        "dataUse":{"properties":{"primary":{"properties":{
          "code":{"type":"keyword"},
          "description":{"type":"text"},
          "internalRationale":{"type":"keyword"}}}}},
        "data":{"properties":{"internalNote":{"type":"keyword"}}},
        "submitter":{"properties":{
          "displayName":{"type":"keyword"},
          "institution":{"properties":{
            "id":{"type":"integer"},
            "name":{"type":"keyword"}}}}},
        "updateUser":{"properties":{"displayName":{"type":"keyword"}}},
        "study":{"properties":{
          "studyId":{"type":"integer"},
          "studyName":{"type":"text","fields":{"keyword":{"type":"keyword"}}},
          "piName":{"type":"keyword"},
          "dataTypes":{"type":"keyword"},
          "dataCustodianEmail":{"type":"keyword"},
          "dataSubmitterId":{"type":"integer"},
          "publicVisibility":{"type":"boolean"},
          "throughBioId":{"type":"keyword"},
          "data":{"properties":{"internalNote":{"type":"keyword"}}},
          "assets":{"properties":{"models":{"properties":{
            "name":{"type":"keyword"},
            "license":{"type":"keyword"},
            "internalCheckpointUri":{"type":"keyword"}}}}}}},
        "accessPolicy":{"properties":{
          "publicVisibility":{"type":"boolean"},
          "hasStudy":{"type":"boolean"},
          "datasetCreatorUserId":{"type":"integer"},
          "studyCreatorUserId":{"type":"integer"},
          "custodianEmails":{"type":"keyword"}}}}}}
      """;

  // ---------------------------------------------------------------------------------------------
  // A second index, for the two mapping constructs §B does not constrain
  // ---------------------------------------------------------------------------------------------

  /**
   * Index for the mapping-hazard measurement. Separate from {@link #INDEX} on purpose: the point is
   * what a <em>mapping</em> can do to an FLS grant, so the mapping has to be the variable.
   */
  static final String MAPPING_HAZARD_INDEX = "dataset-leak-defense-poc-mapping-hazards";

  /**
   * A mapping carrying the two constructs §B's field tables cannot express, both of which make a
   * non-granted field reachable under a granted name.
   *
   * <ul>
   *   <li>{@code copy_to} — {@code internalSecret}'s content is copied into {@code publicNote}'s
   *       index at index time. Granting {@code publicNote} therefore grants the ability to
   *       <em>search</em> {@code internalSecret}'s values, whatever the grant says about the field
   *       itself. Elastic's rule is that a user "cannot perform operations that effectively make
   *       contents accessible under another name"; a {@code copy_to} does exactly that, at index
   *       time, before any role is consulted.
   *   <li>{@code alias} — a second name for a concrete field. Elastic states plainly that "field
   *       level security should not be set on alias fields. To secure a concrete field, its field
   *       name must be used directly."
   * </ul>
   *
   * <p>Neither is hypothetical for DUOS: {@code copy_to} is the standard way to build an all-in-one
   * search field, which is exactly what a "search everything" box invites, and an alias is the
   * standard way to rename a field without reindexing.
   */
  static final String MAPPING_HAZARD_MAPPING =
      """
      {"mappings":{"properties":{
        "datasetIdentifier":{"type":"keyword"},
        "publicNote":{"type":"text"},
        "internalSecret":{"type":"keyword","copy_to":"publicNote"},
        "internalSecretAlias":{"type":"alias","path":"internalSecret"}}}}
      """;

  static final String MAPPING_HAZARD_DOCUMENT =
      """
      {"datasetIdentifier":"DUOS-90001","publicNote":"an ordinary description",
       "internalSecret":"INTERNAL-COPIED-VALUE"}
      """;

  /** The grant a §B-derived {@code FlsGrantBuilder} would produce for the mapping above. */
  static final Set<String> MAPPING_HAZARD_GRANT = Set.of("datasetIdentifier", "publicNote");

  // ---------------------------------------------------------------------------------------------
  // Documents — one per §A row that carries a distinct rule
  // ---------------------------------------------------------------------------------------------

  /** {@code _id} to document body, in index order. */
  record Document(String id, String body) {}

  static List<Document> documents() {
    return List.of(
        new Document("1", PUBLIC_DATASET),
        new Document("2", RESTRICTED_DATASET_WITH_CUSTODIAN),
        new Document("3", RESTRICTED_DATASET_UNRELATED),
        new Document("4", DATASET_WITHOUT_STUDY),
        new Document("5", RESTRICTED_DATASET_OWN_SUBMISSION));
  }

  /** §A row 2 — {@code publicVisibility = TRUE}. Readable by every authenticated caller. */
  private static final String PUBLIC_DATASET =
      document(
          1,
          "DUOS-00001",
          "Public cohort",
          500,
          MARKER_PUBLIC_STUDY,
          100,
          true,
          500,
          500,
          "public-custodian@example.org");

  /** §A row 8 — non-public, reachable only by its custodian (and admin). */
  private static final String RESTRICTED_DATASET_WITH_CUSTODIAN =
      document(
          2,
          "DUOS-00002",
          "Restricted cohort",
          501,
          MARKER_RESTRICTED_CUSTODIAN_STUDY,
          200,
          false,
          501,
          501,
          "custodian@example.org");

  /** §A row 3 — non-public with no relationship to any test caller. Admin-only. */
  private static final String RESTRICTED_DATASET_UNRELATED =
      document(
          3,
          "DUOS-00003",
          "Unrelated restricted cohort",
          502,
          MARKER_RESTRICTED_UNRELATED_STUDY,
          300,
          false,
          502,
          502,
          "nobody@example.org");

  /** §A rows 6/7 — non-public, created by {@link #SUBMITTER}. Drives "My Data Submissions". */
  private static final String RESTRICTED_DATASET_OWN_SUBMISSION =
      document(
          5,
          "DUOS-00005",
          "Own submission cohort",
          7,
          MARKER_RESTRICTED_OWN_SUBMISSION,
          500,
          false,
          7,
          7,
          "nobody@example.org");

  /**
   * §A row 5 — a dataset with no study, readable by everyone. Note {@code
   * accessPolicy.publicVisibility} is explicitly {@code false} here: what makes this document
   * visible is {@code accessPolicy.hasStudy: false} and nothing else, which is precisely the
   * property D-3's no-study clause has to carry (§A.1). A filter that leaned on a null or missing
   * {@code publicVisibility} to mean "visible" would pass its own tests and deny this document in
   * production.
   */
  private static final String DATASET_WITHOUT_STUDY =
      """
      {"datasetId":4,"datasetIdentifier":"DUOS-00004","datasetName":"Study-less dataset",
       "participantCount":10,"accessManagement":"open","createUserId":503,
       "dataUse":{"primary":[{"code":"HMB","description":"Health/medical/biomedical",
         "internalRationale":"INTERNAL-RATIONALE-4"}]},
       "data":{"internalNote":"INTERNAL-DYNAMIC-4"},
       "submitter":{"displayName":"Sam Submitter",
         "institution":{"id":9,"name":"INTERNAL-INSTITUTION-4"}},
       "updateUser":{"displayName":"INTERNAL-UPDATE-USER-4"},
       "accessPolicy":{"publicVisibility":false,"hasStudy":false,
         "datasetCreatorUserId":503,"custodianEmails":[]}}
      """;

  /**
   * Builds a dataset document with a study. Every RESPONSE-INTERNAL path carries an {@code
   * INTERNAL-*} marker so that a projection failure anywhere is detectable by the same scan.
   */
  private static String document(
      int datasetId,
      String identifier,
      String name,
      int createUserId,
      String piName,
      int studyId,
      boolean publicVisibility,
      int datasetCreatorUserId,
      int studyCreatorUserId,
      String custodianEmail) {
    return """
        {"datasetId":%d,"datasetIdentifier":"%s","datasetName":"%s",
         "participantCount":%d,"accessManagement":"controlled","createUserId":%d,
         "dataUse":{"primary":[{"code":"GRU","description":"General research use",
           "internalRationale":"INTERNAL-RATIONALE-%d"}]},
         "data":{"internalNote":"INTERNAL-DYNAMIC-ROOT-%d"},
         "submitter":{"displayName":"Sam Submitter",
           "institution":{"id":9,"name":"INTERNAL-INSTITUTION-%d"}},
         "updateUser":{"displayName":"INTERNAL-UPDATE-USER-%d"},
         "study":{"studyId":%d,"studyName":"%s study","piName":"%s",
           "dataTypes":["Genome"],"dataCustodianEmail":["%s"],
           "dataSubmitterId":%d,"publicVisibility":%b,
           "throughBioId":"INTERNAL-BIO-%d",
           "data":{"internalNote":"INTERNAL-DYNAMIC-STUDY-%d"},
           "assets":{"models":[{"name":"model-%d","license":"MIT",
             "internalCheckpointUri":"INTERNAL-CHECKPOINT-%d"}]}},
         "accessPolicy":{"publicVisibility":%b,"hasStudy":true,
           "datasetCreatorUserId":%d,"studyCreatorUserId":%d,
           "custodianEmails":["%s"]}}
        """
        .formatted(
            datasetId,
            identifier,
            name,
            datasetId * 10,
            createUserId,
            datasetId,
            datasetId,
            datasetId,
            datasetId,
            studyId,
            name,
            piName,
            custodianEmail,
            createUserId,
            publicVisibility,
            datasetId,
            datasetId,
            datasetId,
            datasetId,
            publicVisibility,
            datasetCreatorUserId,
            studyCreatorUserId,
            custodianEmail);
  }

  // ---------------------------------------------------------------------------------------------
  // Small Gson helpers, shared with the enforcement model
  // ---------------------------------------------------------------------------------------------

  static JsonArray stringArray(Iterable<String> values) {
    JsonArray array = new JsonArray();
    values.forEach(array::add);
    return array;
  }

  static JsonObject object(String key, com.google.gson.JsonElement value) {
    JsonObject object = new JsonObject();
    object.add(key, value);
    return object;
  }
}
