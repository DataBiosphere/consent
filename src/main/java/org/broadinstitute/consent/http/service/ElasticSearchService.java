package org.broadinstitute.consent.http.service;

import com.google.gson.JsonArray;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.broadinstitute.consent.http.configurations.ElasticSearchConfiguration;
import org.broadinstitute.consent.http.db.DacDAO;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetProperty;
import org.broadinstitute.consent.http.models.Study;
import org.broadinstitute.consent.http.models.StudyProperty;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.elastic_search.DatasetTerm;
import org.broadinstitute.consent.http.models.elastic_search.ElasticSearchHits;
import org.broadinstitute.consent.http.models.elastic_search.StudyTerm;
import org.broadinstitute.consent.http.util.ConsentLogger;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RestClient;

public class ElasticSearchService implements ConsentLogger {

  private final RestClient esClient;
  private final ElasticSearchConfiguration esConfig;
  private final DacDAO dacDAO;
  private final DataAccessRequestDAO dataAccessRequestDAO;
  private final UserDAO userDAO;
  private final OntologyService ontologyService;

  public ElasticSearchService(
      RestClient esClient,
      ElasticSearchConfiguration esConfig,
      DacDAO dacDAO,
      DataAccessRequestDAO dataAccessRequestDAO,
      UserDAO userDao,
      OntologyService ontologyService) {
    this.esClient = esClient;
    this.esConfig = esConfig;
    this.dacDAO = dacDAO;
    this.dataAccessRequestDAO = dataAccessRequestDAO;
    this.userDAO = userDao;
    this.ontologyService = ontologyService;
  }


  private static final String bulkHeader = """
      { "index": {"_type": "dataset", "_id": "%d"} }
      """;

  private static final String deleteQuery = """
      { "query": { "bool": { "must": [ { "match": { "_type": "dataset" } }, { "match": { "_id": "%d" } } ] } } }
      """;

  private Response performRequest(Request request) throws IOException {
    var response = esClient.performRequest(request);
    var status = response.getStatusLine().getStatusCode();
    if (status != 200) {
      throw new IOException("Invalid Elasticsearch query");
    }
    var body = new String(response.getEntity().getContent().readAllBytes(),
        StandardCharsets.UTF_8);
    return Response.status(status).entity(body).build();
  }

  public Response indexDatasetTerms(List<DatasetTerm> datasets) throws IOException {
    List<String> bulkApiCall = new ArrayList<>();

    datasets.forEach(dsTerm -> {
      bulkApiCall.add(bulkHeader.formatted(dsTerm.getDatasetId()));
      bulkApiCall.add(GsonUtil.getInstance().toJson(dsTerm) + "\n");
    });

    Request bulkRequest = new Request(
        HttpMethod.PUT,
        "/" + esConfig.getDatasetIndexName() + "/_bulk");

    bulkRequest.setEntity(new NStringEntity(
        String.join("", bulkApiCall) + "\n",
        ContentType.APPLICATION_JSON));

    return performRequest(bulkRequest);
  }

  public Response deleteIndex(Integer datasetId) throws IOException {
    Request deleteRequest = new Request(
        HttpMethod.POST,
        "/" + esConfig.getDatasetIndexName() + "/_delete_by_query");
    deleteRequest.setEntity(new NStringEntity(
        deleteQuery.formatted(datasetId),
        ContentType.APPLICATION_JSON));
    return performRequest(deleteRequest);
  }

  public boolean validateQuery(String query) throws IOException {
    // Remove `size` and `from` parameters from query, otherwise validation will fail
    var modifiedQuery = query
        .replaceAll("\"size\": ?\\d+,?", "")
        .replaceAll("\"from\": ?\\d+,?", "");

    Request validateRequest = new Request(
        HttpMethod.GET,
        "/" + esConfig.getDatasetIndexName() + "/_validate/query");
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

    Request searchRequest = new Request(
        HttpMethod.GET,
        "/" + esConfig.getDatasetIndexName() + "/_search");
    searchRequest.setEntity(new NStringEntity(query, ContentType.APPLICATION_JSON));

    Response response = performRequest(searchRequest);

    var entity = response.getEntity().toString();
    var json = GsonUtil.getInstance().fromJson(entity, ElasticSearchHits.class);
    var hits = json.getHits();

    return Response.ok().entity(hits).build();
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

    findStudyProperty(
        study.getProperties(), "phenotypeIndication"
    ).ifPresent(
        prop -> term.setPhenotype(prop.getValue().toString())
    );

    findStudyProperty(
        study.getProperties(), "species"
    ).ifPresent(
        prop -> term.setSpecies(prop.getValue().toString())
    );

    findStudyProperty(
        study.getProperties(), "dataCustodianEmail"
    ).ifPresent(
        prop -> {
          JsonArray jsonArray = (JsonArray) prop.getValue();
          List<String> dataCustodianEmail = new ArrayList<>();
          jsonArray.forEach(email -> dataCustodianEmail.add(email.getAsString()));
          term.setDataCustodianEmail(dataCustodianEmail);
        }
    );

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

    return term;
  }

  public Response indexDataset(Dataset dataset) throws IOException {
    return indexDatasetTerms(List.of(toDatasetTerm(dataset)));
  }

  public Response indexDatasets(List<Dataset> datasets) throws IOException {
    List<DatasetTerm> datasetTerms = datasets.stream().map(this::toDatasetTerm).toList();
    return indexDatasetTerms(datasetTerms);
  }

  public DatasetTerm toDatasetTerm(Dataset dataset) {
    if (Objects.isNull(dataset)) {
      return null;
    }

    DatasetTerm term = new DatasetTerm();

    term.setDatasetId(dataset.getDataSetId());
    term.setCreateUserId(dataset.getCreateUserId());
    term.setDatasetIdentifier(dataset.getDatasetIdentifier());

    if (Objects.nonNull(dataset.getStudy())) {
      term.setStudy(toStudyTerm(dataset.getStudy()));
    }

    if (Objects.nonNull(dataset.getDacId())) {
      Dac dac = dacDAO.findById(dataset.getDacId());
      term.setDacId(dataset.getDacId());
      term.setDacName(dac.getName());
      if (Objects.nonNull(dataset.getDacApproval())) {
        term.setDacApproval(dataset.getDacApproval());
      }
    }

    List<Integer> approvedUserIds =
        dataAccessRequestDAO.findAllUserIdsWithApprovedDARsByDatasetId(
            dataset.getDataSetId());

    if (!approvedUserIds.isEmpty()) {
      term.setApprovedUserIds(approvedUserIds);
    }

    if (Objects.nonNull(dataset.getDataUse())) {
      term.setDataUse(ontologyService.translateDataUseSummary(dataset.getDataUse()));
    }

    findDatasetProperty(
        dataset.getProperties(), "openAccess"
    ).ifPresent(
        datasetProperty -> term.setOpenAccess((Boolean) datasetProperty.getPropertyValue())
    );

    findDatasetProperty(
        dataset.getProperties(), "numberOfParticipants"
    ).ifPresent(
        datasetProperty -> term.setParticipantCount((Integer) datasetProperty.getPropertyValue())
    );

    findDatasetProperty(
        dataset.getProperties(), "url"
    ).ifPresent(
        datasetProperty -> term.setUrl(datasetProperty.getPropertyValueAsString())
    );

    findDatasetProperty(
        dataset.getProperties(), "dataLocation"
    ).ifPresent(
        datasetProperty -> term.setDataLocation(datasetProperty.getPropertyValueAsString())
    );

    return term;
  }

  Optional<DatasetProperty> findDatasetProperty(Collection<DatasetProperty> props,
      String schemaProp) {
    return
        props
            .stream()
            .filter(p -> Objects.nonNull(p.getSchemaProperty()))
            .filter(p -> p.getSchemaProperty().equals(schemaProp))
            .findFirst();
  }

  Optional<StudyProperty> findStudyProperty(Collection<StudyProperty> props, String key) {
    if (Objects.isNull(props)) {
      return Optional.empty();
    }

    return
        props
            .stream()
            .filter(p -> p.getKey().equals(key))
            .findFirst();
  }


}
