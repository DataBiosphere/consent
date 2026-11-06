package org.broadinstitute.consent.http.service;

import static org.broadinstitute.consent.http.models.StudyPatch.EXTERNAL_IDENTIFIER;
import static org.broadinstitute.consent.http.models.StudyPatch.EXTERNAL_IDENTIFIER_TYPE;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.data;

import com.google.api.client.http.HttpStatusCodes;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.inject.Inject;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.broadinstitute.consent.http.configurations.ElasticSearchConfiguration;
import org.broadinstitute.consent.http.db.DACAutomationRuleDAO;
import org.broadinstitute.consent.http.db.DacDAO;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.InstitutionDAO;
import org.broadinstitute.consent.http.db.StudyDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.enumeration.SoApprovalModel;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetProperty;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.models.Study;
import org.broadinstitute.consent.http.models.StudyAssets;
import org.broadinstitute.consent.http.models.StudyProperty;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.datause.DataUsePrimaryClassifier;
import org.broadinstitute.consent.http.models.elastic_search.DacTerm;
import org.broadinstitute.consent.http.models.elastic_search.DatasetTerm;
import org.broadinstitute.consent.http.models.elastic_search.ElasticSearchHits;
import org.broadinstitute.consent.http.models.elastic_search.ElasticSearchInfo;
import org.broadinstitute.consent.http.models.elastic_search.ElasticSearchVersion;
import org.broadinstitute.consent.http.models.elastic_search.InstitutionTerm;
import org.broadinstitute.consent.http.models.elastic_search.StudyTerm;
import org.broadinstitute.consent.http.models.elastic_search.UserTerm;
import org.broadinstitute.consent.http.models.ontology.DataUseSummary;
import org.broadinstitute.consent.http.rules.DACAutomationRuleType;
import org.broadinstitute.consent.http.rules.DACRuleAssignment;
import org.broadinstitute.consent.http.rules.Rules;
import org.broadinstitute.consent.http.service.dao.DatasetServiceDAO;
import org.broadinstitute.consent.http.util.ConsentLogger;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RestClient;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.JdbiException;

public class ElasticSearchService implements ConsentLogger {

  private final RestClient esClient;
  private final ElasticSearchConfiguration esConfig;
  private final DacDAO dacDAO;
  private final DACAutomationRuleDAO dacAutomationRuleDAO;
  private final UserDAO userDAO;
  private final OntologyService ontologyService;
  private final InstitutionDAO institutionDAO;
  private final StudyAssets studyAssets = new StudyAssets();
  private final DatasetDAO datasetDAO;
  private final DatasetServiceDAO datasetServiceDAO;
  private final StudyDAO studyDAO;

  private String indexKey;

  @Inject
  public ElasticSearchService(
      Jdbi jdbi,
      DatasetServiceDAO datasetServiceDAO,
      RestClient esClient,
      ElasticSearchConfiguration esConfig,
      OntologyService ontologyService) {
    this.esClient = esClient;
    this.esConfig = esConfig;
    this.dacDAO = jdbi.onDemand(DacDAO.class);
    this.dacAutomationRuleDAO = jdbi.onDemand(DACAutomationRuleDAO.class);
    this.userDAO = jdbi.onDemand(UserDAO.class);
    this.ontologyService = ontologyService;
    this.institutionDAO = jdbi.onDemand(InstitutionDAO.class);
    this.datasetDAO = jdbi.onDemand(DatasetDAO.class);
    this.datasetServiceDAO = datasetServiceDAO;
    this.studyDAO = jdbi.onDemand(StudyDAO.class);
  }

  private static final int MAX_RESULT_WINDOW = 10000;

  private static final String BULK_HEADER =
      """
      { "index": {"%s": "dataset", "_id": "%d"} }
      """;

  private static final String DELETE_QUERY =
      """
      { "query": { "bool": { "must": [ { "match": { "_index": "dataset" } }, { "match": { "_id": "%d" } } ] } } }
      """;

  private Response performRequest(Request request) throws IOException {
    var response = esClient.performRequest(request);
    var status = response.getStatusLine().getStatusCode();
    if (status != 200) {
      throw new IOException("Invalid Elasticsearch query");
    }
    var body = new String(response.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8);
    return Response.status(status).entity(body).build();
  }

  public String getIndexKey() {
    if (this.indexKey != null) {
      return this.indexKey;
    }
    // nosemgrep
    String defaultKey = "_index";
    try {
      Request request = new Request(HttpMethod.GET, "/");
      Response response = performRequest(request);
      ElasticSearchInfo info =
          GsonUtil.getInstance().fromJson(response.getEntity().toString(), ElasticSearchInfo.class);
      if (info != null && info.version() != null && info.version().number() != null) {
        ElasticSearchVersion version = info.version();
        int majorVersion = Integer.parseInt(version.number().split("\\.")[0]);
        String distribution = version.distribution();
        if (distribution == null && majorVersion < 7) {
          defaultKey = "_type";
        }
      }
    } catch (Exception e) {
      logWarn(
          "Unable to get Elasticsearch index key, defaulting to "
              + defaultKey
              + ": "
              + e.getMessage());
    }
    return setIndexKey(defaultKey);
  }

  String setIndexKey(String indexKey) {
    this.indexKey = indexKey;
    return this.indexKey;
  }

  public Response indexDatasetTerms(List<DatasetTerm> datasets) throws IOException {
    List<String> bulkApiCall = new ArrayList<>();

    datasets.forEach(
        dsTerm -> {
          bulkApiCall.add(BULK_HEADER.formatted(getIndexKey(), dsTerm.getDatasetId()));
          bulkApiCall.add(GsonUtil.getInstance().toJson(dsTerm) + "\n");
        });

    Request bulkRequest =
        new Request(HttpMethod.PUT, "/" + esConfig.getDatasetIndexName() + "/_bulk");

    bulkRequest.setEntity(
        new NStringEntity(String.join("", bulkApiCall) + "\n", ContentType.APPLICATION_JSON));
    Set<Integer> sent =
        datasets.stream().map(DatasetTerm::getDatasetId).collect(Collectors.toSet());
    // Stamped even if the request fails, so later syncs keep retrying the dataset.
    updateDatasetIndexDateForDatasets(sent);
    Response response = performRequest(bulkRequest);
    // A bulk request answers 200 even when Elasticsearch rejected some of its documents.
    JsonObject body = bulkBody(response);
    Optional<Set<Integer>> rejected = rejectedIds(body);
    // Without an items list the body does not show which documents Elasticsearch accepted.
    if (rejected.isPresent() && body.get("items") instanceof JsonArray) {
      confirmIndexed(
          sent.stream().filter(id -> !rejected.get().contains(id)).collect(Collectors.toSet()));
    }
    if (rejected.map(ids -> !ids.isEmpty()).orElse(true)) {
      logWarn("Elasticsearch rejected datasets %s".formatted(rejected.orElse(sent)));
      return Response.status(Status.INTERNAL_SERVER_ERROR).entity(response.getEntity()).build();
    }
    return response;
  }

  /**
   * The datasets a bulk response says Elasticsearch rejected, or empty when a rejected document
   * names no dataset id, or the body flags errors without naming a rejected document.
   */
  static Optional<Set<Integer>> rejectedIds(Response response) {
    return rejectedIds(bulkBody(response));
  }

  private static Optional<Set<Integer>> rejectedIds(JsonObject body) {
    Set<Integer> ids = new HashSet<>();
    for (JsonObject item : bulkItems(body)) {
      if (item.has("error")) {
        JsonElement id = item.get("_id");
        if (id == null || !id.isJsonPrimitive()) {
          return Optional.empty();
        }
        try {
          ids.add(Integer.valueOf(id.getAsString()));
        } catch (NumberFormatException e) {
          return Optional.empty();
        }
      }
    }
    return ids.isEmpty() && flagsErrors(body) ? Optional.empty() : Optional.of(ids);
  }

  private static JsonObject bulkBody(Response response) {
    try {
      JsonElement body = JsonParser.parseString(String.valueOf(response.getEntity()));
      return body.isJsonObject() ? body.getAsJsonObject() : new JsonObject();
    } catch (RuntimeException e) {
      return new JsonObject();
    }
  }

  private static boolean flagsErrors(JsonObject body) {
    JsonElement errors = body.get("errors");
    return errors != null
        && errors.isJsonPrimitive()
        && errors.getAsJsonPrimitive().isBoolean()
        && errors.getAsBoolean();
  }

  private static List<JsonObject> bulkItems(JsonObject body) {
    JsonElement items = body.get("items");
    if (items == null || !items.isJsonArray()) {
      return List.of();
    }
    return items.getAsJsonArray().asList().stream()
        .filter(JsonElement::isJsonObject)
        .map(item -> item.getAsJsonObject().get("index"))
        .filter(index -> index != null && index.isJsonObject())
        .map(JsonElement::getAsJsonObject)
        .toList();
  }

  public Response deleteIndex(Integer datasetId, Integer userId) throws IOException {
    Request deleteRequest =
        new Request(HttpMethod.POST, "/" + esConfig.getDatasetIndexName() + "/_delete_by_query");
    deleteRequest.addParameter("conflicts", "proceed");
    deleteRequest.setEntity(
        new NStringEntity(DELETE_QUERY.formatted(datasetId), ContentType.APPLICATION_JSON));
    updateDatasetIndexDate(datasetId, userId, null);
    // Taken after indexed_date is cleared: a later confirmation needed a newer index to set it
    // again.
    Optional<Instant> intentClearedAt = databaseTime();
    Response response = performRequest(deleteRequest);
    if (deletedCleanly(bulkBody(response))) {
      intentClearedAt.ifPresent(at -> clearIndexConfirmed(datasetId, at));
    } else {
      logWarn("Elasticsearch did not confirm deleting dataset %d".formatted(datasetId));
    }
    return response;
  }

  /**
   * Only a delete that removed a document, with no conflict, failure or timeout, shows the document
   * is gone: with conflicts=proceed a conflict is only counted, and a document indexed within the
   * refresh interval is not yet visible to the query, so it answers deleted 0.
   */
  private static boolean deletedCleanly(JsonObject body) {
    return count(body, "deleted") > 0
        && count(body, "version_conflicts") == 0
        && !(body.get("timed_out") instanceof JsonPrimitive timedOut
            && timedOut.isBoolean()
            && timedOut.getAsBoolean())
        && (!body.has("failures")
            || body.get("failures") instanceof JsonArray failures && failures.isEmpty());
  }

  private static long count(JsonObject body, String field) {
    return body.get(field) instanceof JsonPrimitive value && value.isNumber()
        ? value.getAsLong()
        : 0;
  }

  /**
   * A database error here only under-reports the datasets as listed until they are indexed again.
   */
  private void confirmIndexed(Set<Integer> ids) {
    if (ids.isEmpty()) {
      return;
    }
    try {
      datasetDAO.confirmDatasetsIndexed(ids);
    } catch (JdbiException e) {
      logWarn("Error recording datasets %s as indexed".formatted(ids), e);
    }
  }

  private Optional<Instant> databaseTime() {
    try {
      return Optional.ofNullable(datasetDAO.currentDatabaseTime());
    } catch (JdbiException e) {
      logWarn("Error reading the database time", e);
      return Optional.empty();
    }
  }

  private void clearIndexConfirmed(Integer datasetId, Instant intentClearedAt) {
    try {
      datasetDAO.clearDatasetIndexConfirmed(datasetId, intentClearedAt);
    } catch (JdbiException e) {
      logWarn("Error recording dataset %d as removed from the index".formatted(datasetId), e);
    }
  }

  public boolean invalidResultWindow(String query) {
    try {
      var queryJson = GsonUtil.getInstance().fromJson(query, Map.class);

      long size = (long) queryJson.getOrDefault("size", 10L);
      long from = (long) queryJson.getOrDefault("from", 0L);

      return from + size > MAX_RESULT_WINDOW;
    } catch (Exception e) {
      logWarn("Unable to parse query for result window validation: " + e.getMessage());
      return true;
    }
  }

  public boolean validateQuery(String query) throws IOException {
    if (invalidResultWindow(query)) {
      return false;
    }

    // Remove `sort`, `size` and `from` parameters from query, otherwise validation will fail
    var modifiedQuery =
        query
            .replaceAll("\"sort\": ?\\[(.*?)\\],?", "")
            .replaceAll("\"size\": ?\\d+,?", "")
            .replaceAll("\"from\": ?\\d+,?", "");

    Request validateRequest =
        new Request(HttpMethod.GET, "/" + esConfig.getDatasetIndexName() + "/_validate/query");
    validateRequest.setEntity(new NStringEntity(modifiedQuery, ContentType.APPLICATION_JSON));
    Response response = performRequest(validateRequest);

    var entity = response.getEntity().toString();
    var json = GsonUtil.getInstance().fromJson(entity, Map.class);

    return (boolean) json.get("valid");
  }

  public Response searchDatasets(String query) throws IOException {
    if (!validateQuery(query)) {
      throw new IOException("Invalid Elasticsearch query");
    }

    Request searchRequest =
        new Request(HttpMethod.GET, "/" + esConfig.getDatasetIndexName() + "/_search");
    searchRequest.setEntity(new NStringEntity(query, ContentType.APPLICATION_JSON));

    Response response = performRequest(searchRequest);

    var entity = response.getEntity().toString();
    var json = GsonUtil.getInstance().fromJson(entity, ElasticSearchHits.class);
    var hits = json.getHits();

    return Response.ok().entity(hits).build();
  }

  public InputStream searchDatasetsStream(String query) throws IOException {
    if (invalidResultWindow(query)) {
      throw new IOException("Invalid Elasticsearch query");
    }
    Request searchRequest =
        new Request(HttpMethod.GET, "/" + esConfig.getDatasetIndexName() + "/_search");
    searchRequest.setEntity(new NStringEntity(query, ContentType.APPLICATION_JSON));
    var response = esClient.performRequest(searchRequest);
    var status = response.getStatusLine().getStatusCode();
    if (status != 200) {
      throw new IOException("Invalid Elasticsearch query");
    }
    return response.getEntity().getContent();
  }

  public StudyTerm toStudyTerm(Study study) {
    if (Objects.isNull(study)) {
      return null;
    }

    StudyTerm term = new StudyTerm();

    term.setDescription(study.getDescription());
    term.setStudyName(study.getName());
    term.setStudyId(study.getStudyId());
    term.setDataTypes(study.getDataTypes());
    term.setPiName(study.getPiName());
    term.setPublicVisibility(study.getPublicVisibility());

    findStudyProperty(study.getProperties(), "throughBioId")
        .ifPresent(prop -> term.setThroughBioId(prop.getValue().toString()));

    findStudyProperty(study.getProperties(), "dbGaPPhsID")
        .ifPresent(prop -> term.setPhsId(prop.getValue().toString()));

    findStudyProperty(study.getProperties(), "phenotypeIndication")
        .ifPresent(prop -> term.setPhenotype(prop.getValue().toString()));

    findStudyProperty(study.getProperties(), "species")
        .ifPresent(prop -> term.setSpecies(prop.getValue().toString()));

    findStudyProperty(study.getProperties(), "dataCustodianEmail")
        .ifPresent(
            prop -> {
              JsonArray jsonArray = (JsonArray) prop.getValue();
              List<String> dataCustodianEmail = new ArrayList<>();
              jsonArray.forEach(email -> dataCustodianEmail.add(email.getAsString()));
              term.setDataCustodianEmail(dataCustodianEmail);
            });

    if (Objects.nonNull(study.getCreateUserId())) {
      term.setDataSubmitterId(study.getCreateUserId());
      User user = userDAO.findUserById(study.getCreateUserId());
      if (Objects.nonNull(user)) {
        study.setCreateUserEmail(user.getEmail());
      }
    }

    if (Objects.nonNull(study.getCreateUserEmail())) {
      term.setDataSubmitterEmail(study.getCreateUserEmail());
    }

    // The indexed assets object keeps its original shape — every promoted list plus whatever
    // unpromoted keys remain — so search consumers are unaffected by the promotion.
    Map<String, Object> studyAssetsMap = studyAssets.assemble(study.getProperties());
    if (!studyAssetsMap.isEmpty()) {
      term.setAssets(studyAssetsMap);
    }
    findStudyProperty(study.getProperties(), data)
        .ifPresent(prop -> term.setData(buildMapFromPropertyValue(prop.getValue())));
    findStudyProperty(study.getProperties(), EXTERNAL_IDENTIFIER)
        .ifPresent(prop -> term.setExternalIdentifier(prop.getValue().toString()));
    findStudyProperty(study.getProperties(), EXTERNAL_IDENTIFIER_TYPE)
        .ifPresent(prop -> term.setExternalIdentifierType(prop.getValue().toString()));
    return term;
  }

  public UserTerm toUserTerm(User user) {
    if (Objects.isNull(user)) {
      return null;
    }
    InstitutionTerm institution =
        (Objects.nonNull(user.getInstitutionId()))
            ? toInstitutionTerm(institutionDAO.findInstitutionById(user.getInstitutionId()))
            : null;
    return new UserTerm(user.getUserId(), user.getDisplayName(), institution);
  }

  public DacTerm toDacTerm(Dac dac) {
    if (Objects.isNull(dac)) {
      return null;
    }
    return new DacTerm(dac.getDacId(), dac.getName(), dac.getEmail());
  }

  public InstitutionTerm toInstitutionTerm(Institution institution) {
    if (Objects.isNull(institution)) {
      return null;
    }
    return new InstitutionTerm(institution.getId(), institution.getName());
  }

  public Reindex indexStudy(Integer studyId) {
    return indexStudy(studyId, Set.of());
  }

  /** Reindexes the study's datasets in the index, and adds {@code newDatasetIds} to it. */
  public Reindex indexStudy(Integer studyId, Collection<Integer> newDatasetIds) {
    return reindex(studyDAO.findDatasetIdsByStudyId(studyId), Set.copyOf(newDatasetIds));
  }

  /**
   * Synchronize the dataset in the ES index. This will only index the dataset if it has been
   * previously indexed, UNLESS the force argument is true which means it will index the dataset and
   * update the dataset's last indexed date value.
   *
   * @param dataset The Dataset
   * @param force Boolean to force the index update regardless of dataset's indexed date status.
   */
  public void synchronizeDatasetInESIndex(Dataset dataset, boolean force) {
    if (force || isIndexed(dataset)) {
      try (var response = indexDataset(dataset.getDatasetId())) {
        if (!HttpStatusCodes.isSuccess(response.getStatus())) {
          logWarn("Response error, unable to index dataset: %s".formatted(dataset.getDatasetId()));
        }
      } catch (IOException e) {
        logWarn("Exception, unable to index dataset: %s".formatted(dataset.getDatasetId()), e);
      }
    }
  }

  protected void synchronizeDatasetListInESIndex(List<Dataset> datasets) {
    try (var response = indexDatasetList(datasets)) {
      if (!HttpStatusCodes.isSuccess(response.getStatus())) {
        logWarn("Response error, unable to index datasets");
      }
    } catch (IOException e) {
      logWarn("Exception, unable to index datasets", e);
    }
  }

  public Response indexDataset(Integer datasetId) throws IOException {
    return indexDatasets(List.of(datasetId));
  }

  /** Sent to the index at least once and not removed by an admin, even if that request failed. */
  public static boolean isIndexed(Dataset dataset) {
    return dataset.getIndexedDate() != null;
  }

  /** Which datasets a reindex sent, and which of those Elasticsearch did not index. */
  public record Reindex(Set<Integer> sent, Set<Integer> failed) {}

  /** Reindexes, in one bulk request, only those of the datasets already in the index. */
  public Reindex reindexIndexedDatasets(Collection<Integer> datasetIds) {
    return reindex(datasetIds, Set.of());
  }

  private Reindex reindex(Collection<Integer> datasetIds, Set<Integer> newDatasetIds) {
    if (datasetIds.isEmpty()) {
      return new Reindex(Set.of(), Set.of());
    }
    // Until the lookup says which are indexed, a failure counts against every dataset.
    Set<Integer> sent = Set.copyOf(datasetIds);
    try {
      List<Dataset> datasets =
          datasetDAO.findDatasetsByIdList(List.copyOf(datasetIds)).stream()
              .filter(d -> isIndexed(d) || newDatasetIds.contains(d.getDatasetId()))
              .toList();
      sent = datasets.stream().map(Dataset::getDatasetId).collect(Collectors.toUnmodifiableSet());
      if (sent.isEmpty()) {
        return new Reindex(sent, Set.of());
      }
      attachStudies(datasets);
      return sendToIndex(datasets, sent);
    } catch (IOException | RuntimeException e) {
      logWarn("Unable to reindex datasets %s".formatted(sent), e);
      return new Reindex(sent, sent);
    }
  }

  private Reindex sendToIndex(List<Dataset> datasets, Set<Integer> sent) throws IOException {
    try (Response response = indexDatasetList(datasets)) {
      return new Reindex(
          sent,
          HttpStatusCodes.isSuccess(response.getStatus()) ? Set.of() : failed(response, sent));
    }
  }

  /** The datasets the bulk response names as rejected, or all of them when it names none. */
  private static Set<Integer> failed(Response response, Set<Integer> sent) {
    return rejectedIds(response).filter(ids -> !ids.isEmpty()).orElse(sent);
  }

  public Response indexDatasets(List<Integer> datasetIds) throws IOException {
    logInfo("Fetching %d datasets from database".formatted(datasetIds.size()));
    List<Dataset> datasets = datasetDAO.findDatasetsByIdList(datasetIds);
    logInfo("Fetched %d datasets from database".formatted(datasets.size()));
    attachStudies(datasets);
    return indexDatasetList(datasets);
  }

  /** Datasets in list context may not have their study populated, which indexing needs. */
  private void attachStudies(List<Dataset> datasets) {
    Map<Integer, Study> studyCache = new HashMap<>();
    datasets.forEach(
        dataset -> {
          if (dataset.getStudyId() != null) {
            dataset.setStudy(
                studyCache.computeIfAbsent(
                    dataset.getStudyId(),
                    k -> {
                      logInfo("Fetching study id %d from database".formatted(k));
                      Study study = studyDAO.findStudyById(k);
                      logInfo("Study id %d fetched.".formatted(k));
                      return study;
                    }));
          }
        });
  }

  public Response indexDatasetList(List<Dataset> datasets) throws IOException {
    // Resolved once for the whole batch rather than per dataset
    Map<Integer, Set<DACAutomationRuleType>> enabledRulesByDacId =
        resolveEnabledRulesByDacId().orElse(null);
    List<DatasetTerm> datasetTerms =
        datasets.parallelStream()
            .filter(Objects::nonNull)
            .map(dataset -> toDatasetTerm(dataset, enabledRulesByDacId))
            .toList();
    if (datasetTerms.isEmpty()) {
      return Response.status(Status.NOT_FOUND).build();
    }
    return indexDatasetTerms(datasetTerms);
  }

  /**
   * Sequentially index datasets to ElasticSearch by ID list. Note that this is intended for large
   * lists of dataset ids. For small sets of datasets (i.e. <~25), it is efficient to index them in
   * bulk using the {@link #indexDatasets(List)} method.
   *
   * @param datasetIds List of Dataset IDs to index
   * @return StreamingOutput of ElasticSearch responses from indexing datasets
   */
  public StreamingOutput indexDatasetIds(List<Integer> datasetIds) {
    return output -> {
      try (Response response = indexDatasets(datasetIds)) {
        output.write(response.getEntity().toString().getBytes());
        output.write("\n".getBytes());
      } catch (IOException e) {
        logWarn("Error indexing datasets", e);
        output.write("Error indexing datasets".getBytes());
        output.write("\n".getBytes());
      }
    };
  }

  /**
   * The automation rules each DAC currently has enabled, keyed by DAC id. {@code Optional.empty()}
   * means they could not be resolved at all, unlike an empty map, which means no DAC has any rule
   * enabled. Indexing continues either way, but unresolved rules are not reported as dataset state.
   */
  Optional<Map<Integer, Set<DACAutomationRuleType>>> resolveEnabledRulesByDacId() {
    try {
      List<DACRuleAssignment> assignments =
          Objects.requireNonNullElse(dacAutomationRuleDAO.findEnabledRuleAssignments(), List.of());
      return Optional.of(
          assignments.stream()
              .collect(
                  Collectors.groupingBy(
                      DACRuleAssignment::dacId,
                      Collectors.mapping(
                          DACRuleAssignment::ruleType, Collectors.toUnmodifiableSet()))));
    } catch (Exception e) {
      logWarn("Unable to resolve enabled DAC automation rules", e);
      return Optional.empty();
    }
  }

  /**
   * Whether the dataset's DAC has an enabled rule that would automatically approve a matching
   * request for it. Only the dataset half of each rule applies; the request half is unknowable at
   * indexing time. Mirrors {@code DACAutomationRuleService.applyRule}, shape gate first.
   */
  private boolean isInstantApprovalEligible(Dataset dataset, Set<DACAutomationRuleType> dacRules) {
    if (!DataUsePrimaryClassifier.hasCanonicalSinglePrimary(dataset.getDataUse())) {
      return false;
    }
    return Rules.implementationList.stream()
        .filter(rule -> dacRules.contains(rule.getRuleType()))
        .anyMatch(rule -> rule.datasetQualifies(dataset));
  }

  /**
   * @param enabledRulesByDacId resolved DAC rules, or {@code null} when they could not be resolved
   *     — the rule-derived fields are then left unset so clients render nothing rather than being
   *     told the wrong approval process
   */
  DatasetTerm toDatasetTerm(
      Dataset dataset, Map<Integer, Set<DACAutomationRuleType>> enabledRulesByDacId) {
    if (Objects.isNull(dataset)) {
      return null;
    }

    DatasetTerm term = new DatasetTerm();

    term.setDatasetId(dataset.getDatasetId());
    Optional.ofNullable(dataset.getCreateUserId())
        .ifPresent(
            userId -> {
              User user = userDAO.findUserById(dataset.getCreateUserId());
              term.setCreateUserId(dataset.getCreateUserId());
              term.setCreateUserDisplayName(user.getDisplayName());
              term.setSubmitter(toUserTerm(user));
            });
    Optional.ofNullable(dataset.getUpdateUserId())
        .map(userDAO::findUserById)
        .map(this::toUserTerm)
        .ifPresent(term::setUpdateUser);
    term.setDatasetIdentifier(dataset.getDatasetIdentifier());
    term.setDeletable(dataset.getDeletable());
    term.setDatasetName(dataset.getName());

    if (Objects.nonNull(dataset.getStudy())) {
      term.setStudy(toStudyTerm(dataset.getStudy()));
    }

    Optional.ofNullable(dataset.getDacId())
        .ifPresent(
            dacId -> {
              Dac dac = dacDAO.findById(dataset.getDacId());
              term.setDacId(dataset.getDacId());
              if (Objects.nonNull(dataset.getDacApproval())) {
                term.setDacApproval(dataset.getDacApproval());
              }
              term.setDac(toDacTerm(dac));
            });

    // A dataset with no DAC has no per-request approval step to satisfy and no DAC rule that could
    // auto-approve it, which holds whether or not the rules resolved; only datasets whose state
    // depends on the unresolved rules are left unset
    if (Objects.isNull(dataset.getDacId())) {
      term.setSoApprovalModel(SoApprovalModel.PRE_AUTHORIZED);
      term.setInstantApprovalEligible(false);
    } else if (Objects.nonNull(enabledRulesByDacId)) {
      Set<DACAutomationRuleType> dacRules =
          enabledRulesByDacId.getOrDefault(dataset.getDacId(), Set.of());
      term.setSoApprovalModel(
          dacRules.contains(DACAutomationRuleType.REQUIRE_SO_DAR_APPROVAL)
              ? SoApprovalModel.PER_REQUEST
              : SoApprovalModel.PRE_AUTHORIZED);
      term.setInstantApprovalEligible(isInstantApprovalEligible(dataset, dacRules));
    }

    if (Objects.nonNull(dataset.getDataUse())) {
      DataUseSummary summary = ontologyService.translateDataUseSummary(dataset.getDataUse());
      if (summary != null) {
        term.setDataUse(summary);
      } else {
        logWarn("No data use summary for dataset id: %d".formatted(dataset.getDatasetId()));
      }
    }

    Optional.ofNullable(dataset.getNihInstitutionalCertificationFile())
        .ifPresent(obj -> term.setHasInstitutionCertification(true));

    Optional.ofNullable(dataset.getAccessManagement())
        .ifPresent(accessManagement -> term.setAccessManagement(accessManagement.value()));

    findFirstDatasetPropertyByName(dataset.getProperties(), "# of participants")
        .ifPresent(
            datasetProperty -> {
              String value = datasetProperty.getPropertyValueAsString();
              try {
                term.setParticipantCount(Integer.valueOf(value));
              } catch (NumberFormatException e) {
                logWarn(
                    String.format(
                        "Unable to coerce participant count to integer: %s for dataset: %s",
                        value, dataset.getDatasetIdentifier()),
                    e);
              }
            });

    findDatasetProperty(dataset.getProperties(), "url")
        .ifPresent(datasetProperty -> term.setUrl(datasetProperty.getPropertyValueAsString()));

    findDatasetProperty(dataset.getProperties(), "requestLocation")
        .ifPresent(
            datasetProperty -> term.setRequestLocation(datasetProperty.getPropertyValueAsString()));

    findDatasetProperty(dataset.getProperties(), "dataLocation")
        .ifPresent(
            datasetProperty -> term.setDataLocation(datasetProperty.getPropertyValueAsString()));
    findDatasetProperty(dataset.getProperties(), "data")
        .ifPresent(
            datasetProperty ->
                term.setData(buildMapFromPropertyValue(datasetProperty.getPropertyValue())));
    return term;
  }

  protected void updateDatasetIndexDate(Integer datasetId, Integer userId, Instant indexDate) {
    // It is possible that a dataset has been deleted. If so, we don't want to try and update it.
    // Only the dataset's existence matters here, so avoid assembling the full dataset.
    if (datasetDAO.findDatasetIdById(datasetId) != null) {
      try {
        datasetServiceDAO.updateDatasetIndex(datasetId, userId, indexDate);
      } catch (JdbiException e) {
        // We don't want to send these to Sentry, but we do want to log them for follow up off cycle
        logWarn("Error updating dataset indexed date for dataset id: %d ".formatted(datasetId), e);
      }
    }
  }

  protected void updateDatasetIndexDateForDatasets(Set<Integer> ids) {
    if (ids != null && !ids.isEmpty()) {
      datasetDAO.updateDatasetsIndexedDate(ids);
    }
  }

  Optional<DatasetProperty> findDatasetProperty(
      Collection<DatasetProperty> props, String schemaProp) {
    return (props == null)
        ? Optional.empty()
        : props.stream()
            .filter(p -> Objects.nonNull(p.getSchemaProperty()))
            .filter(p -> p.getSchemaProperty().equals(schemaProp))
            .findFirst();
  }

  Optional<DatasetProperty> findFirstDatasetPropertyByName(
      Collection<DatasetProperty> props, String propertyName) {
    return (props == null)
        ? Optional.empty()
        : props.stream()
            .filter(p -> p.getPropertyName().equalsIgnoreCase(propertyName))
            .findFirst();
  }

  Optional<StudyProperty> findStudyProperty(Collection<StudyProperty> props, String key) {
    return (props == null)
        ? Optional.empty()
        : props.stream().filter(p -> p.getKey().equals(key)).findFirst();
  }

  public static Map<String, Object> buildMapFromPropertyValue(Object value) {
    Map<String, Object> objectMap;
    // When property is loaded from db it is deserialized as JsonObject
    if (value instanceof com.google.gson.JsonElement element) {
      objectMap =
          GsonUtil.getInstance()
              .fromJson(
                  element,
                  new com.google.gson.reflect.TypeToken<Map<String, Object>>() {}.getType());
      // Otherwise the value is a Map that Gson built from JSON
    } else if (value instanceof Map) {
      objectMap = (Map<String, Object>) value;
      // Fallback: try to parse as JSON string
    } else {
      objectMap =
          GsonUtil.getInstance()
              .fromJson(
                  value.toString(),
                  new com.google.gson.reflect.TypeToken<Map<String, Object>>() {}.getType());
    }
    return objectMap;
  }
}
