package org.broadinstitute.consent.http.service;

import static org.broadinstitute.consent.http.models.StudyPatch.EXTERNAL_IDENTIFIER;
import static org.broadinstitute.consent.http.models.StudyPatch.EXTERNAL_IDENTIFIER_TYPE;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.data;

import com.google.api.client.http.HttpStatusCodes;
import com.google.gson.JsonArray;
import com.google.inject.Inject;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
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
    updateDatasetIndexDateForDatasets(
        datasets.stream().map(DatasetTerm::getDatasetId).collect(Collectors.toSet()));
    return performRequest(bulkRequest);
  }

  public Response deleteIndex(Integer datasetId, Integer userId) throws IOException {
    Request deleteRequest =
        new Request(HttpMethod.POST, "/" + esConfig.getDatasetIndexName() + "/_delete_by_query");
    deleteRequest.addParameter("conflicts", "proceed");
    deleteRequest.setEntity(
        new NStringEntity(DELETE_QUERY.formatted(datasetId), ContentType.APPLICATION_JSON));
    updateDatasetIndexDate(datasetId, userId, null);
    return performRequest(deleteRequest);
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

  public Response indexStudy(Integer studyId) {
    Study study = studyDAO.findStudyById(studyId);
    logInfo("Loading datasets for study: %d".formatted(studyId));
    List<Dataset> datasets = datasetDAO.findDatasetsByIdList(study.getDatasetIds());
    logInfo("Loaded %d datasets for study: %d".formatted(datasets.size(), studyId));
    datasets.forEach(d -> d.setStudy(study));
    if (datasets.isEmpty()) {
      return Response.status(Status.NOT_FOUND).build();
    }
    try {
      synchronizeDatasetListInESIndex(datasets);
      return Response.ok().build();
    } catch (Exception e) {
      logWarn("Exception, unable to index study: %d".formatted(studyId), e);
      return Response.status(500, "Indexing failed.").build();
    }
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
    if (force || dataset.getIndexedDate() != null) {
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

  public Response indexDatasets(List<Integer> datasetIds) throws IOException {
    // Datasets in list context may not have their study populated, so we need to ensure that is
    // true before trying to index them in ES.
    logInfo("Fetching %d datasets from database".formatted(datasetIds.size()));
    List<Dataset> datasets = datasetDAO.findDatasetsByIdList(datasetIds);
    logInfo("Fetched %d datasets from database".formatted(datasets.size()));
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
    return indexDatasetList(datasets);
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
      } catch (SQLException e) {
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
      // Otherwise Gson deserializes JSON and creates a LinkedTreeMap
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
