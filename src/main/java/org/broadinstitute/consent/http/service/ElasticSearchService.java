package org.broadinstitute.consent.http.service;

import com.google.api.client.http.HttpStatusCodes;
import com.google.gson.JsonArray;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.Instant;
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
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.InstitutionDAO;
import org.broadinstitute.consent.http.db.StudyDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetProperty;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.models.Study;
import org.broadinstitute.consent.http.models.StudyProperty;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.elastic_search.DacTerm;
import org.broadinstitute.consent.http.models.elastic_search.DatasetTerm;
import org.broadinstitute.consent.http.models.elastic_search.ElasticSearchHits;
import org.broadinstitute.consent.http.models.elastic_search.InstitutionTerm;
import org.broadinstitute.consent.http.models.elastic_search.StudyTerm;
import org.broadinstitute.consent.http.models.elastic_search.UserTerm;
import org.broadinstitute.consent.http.models.ontology.DataUseSummary;
import org.broadinstitute.consent.http.service.dao.DatasetServiceDAO;
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
  private final InstitutionDAO institutionDAO;
  private final DatasetDAO datasetDAO;
  private final DatasetServiceDAO datasetServiceDAO;
  private final StudyDAO studyDAO;

  public ElasticSearchService(
      RestClient esClient,
      ElasticSearchConfiguration esConfig,
      DacDAO dacDAO,
      DataAccessRequestDAO dataAccessRequestDAO,
      UserDAO userDao,
      OntologyService ontologyService,
      InstitutionDAO institutionDAO,
      DatasetDAO datasetDAO,
      DatasetServiceDAO datasetServiceDAO,
      StudyDAO studyDAO) {
    this.esClient = esClient;
    this.esConfig = esConfig;
    this.dacDAO = dacDAO;
    this.dataAccessRequestDAO = dataAccessRequestDAO;
    this.userDAO = userDao;
    this.ontologyService = ontologyService;
    this.institutionDAO = institutionDAO;
    this.datasetDAO = datasetDAO;
    this.datasetServiceDAO = datasetServiceDAO;
    this.studyDAO = studyDAO;
  }


  private static final String BULK_HEADER = """
      { "index": {"_type": "dataset", "_id": "%d"} }
      """;

  private static final String DELETE_QUERY = """
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

  public Response indexDatasetTerms(List<DatasetTerm> datasets, User user) throws IOException {
    List<String> bulkApiCall = new ArrayList<>();

    datasets.forEach(dsTerm -> {
      bulkApiCall.add(BULK_HEADER.formatted(dsTerm.getDatasetId()));
      bulkApiCall.add(GsonUtil.getInstance().toJson(dsTerm) + "\n");
      try {
        updateDatasetIndexDate(dsTerm.getDatasetId(), user.getUserId(), Instant.now());
      } catch (SQLException e) {
        // We don't want to send these to Sentry, but we do want to log them for follow up off cycle
        logWarn("Error updating dataset indexed date for dataset id: %d ".formatted(dsTerm.getDatasetId()), e);
      }
    });

    Request bulkRequest = new Request(
        HttpMethod.PUT,
        "/" + esConfig.getDatasetIndexName() + "/_bulk");

    bulkRequest.setEntity(new NStringEntity(
        String.join("", bulkApiCall) + "\n",
        ContentType.APPLICATION_JSON));

    return performRequest(bulkRequest);
  }

  public Response deleteIndex(Integer datasetId, Integer userId) throws IOException, SQLException {
    Request deleteRequest = new Request(
        HttpMethod.POST,
        "/" + esConfig.getDatasetIndexName() + "/_delete_by_query");
    deleteRequest.setEntity(new NStringEntity(
        DELETE_QUERY.formatted(datasetId),
        ContentType.APPLICATION_JSON));
    updateDatasetIndexDate(datasetId, userId, null);
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
        study.getProperties(), "dbGaPPhsID"
    ).ifPresent(
        prop -> term.setPhsId(prop.getValue().toString())
    );

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

  public UserTerm toUserTerm(User user) {
    if (Objects.isNull(user)) {
      return null;
    }
    InstitutionTerm institution = (Objects.nonNull(user.getInstitutionId())) ?
        toInstitutionTerm(institutionDAO.findInstitutionById(user.getInstitutionId())) :
        null;
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

  /**
   * Synchronize the dataset in the ES index. This will only index the dataset if it has been
   * previously indexed, UNLESS the force argument is true which means it will index the dataset
   * and update the dataset's last indexed date value.
   *
   * @param dataset The Dataset
   * @param user    The User
   * @param force   Boolean to force the index update regardless of dataset's indexed date status.
   */
  public void synchronizeDatasetInESIndex(Dataset dataset, User user, boolean force) {
    if (force || dataset.getIndexedDate() != null) {
      try (var response = indexDataset(dataset, user)) {
        if (!HttpStatusCodes.isSuccess(response.getStatus())) {
          logWarn("Response error, unable to index dataset: %s".formatted(dataset.getDatasetId()));
        }
      } catch (IOException e) {
        logWarn("Exception, unable to index dataset: %s".formatted(dataset.getDatasetId()));
      }
    }
  }

  public Response indexDataset(Dataset dataset, User user) throws IOException {
    return indexDatasets(List.of(dataset), user);
  }

  public Response indexDatasets(List<Dataset> datasets, User user) throws IOException {
    List<DatasetTerm> datasetTerms = datasets.stream().map(this::toDatasetTerm).toList();
    return indexDatasetTerms(datasetTerms, user);
  }

  /**
   * Sequentially index datasets to ElasticSearch by ID list. Note that this is intended for large
   * lists of dataset ids. For small sets of datasets (i.e. <~25), it is efficient to index them in
   * bulk using the {@link #indexDatasets(List, User)} method.
   *
   * @param datasetIds List of Dataset IDs to index
   * @return StreamingOutput of ElasticSearch responses from indexing datasets
   */
  public StreamingOutput indexDatasetIds(List<Integer> datasetIds, User user) {
    Integer lastDatasetId = datasetIds.get(datasetIds.size() - 1);
    return output -> {
      output.write("[".getBytes());
      datasetIds.forEach(id -> {
        Dataset dataset = datasetDAO.findDatasetById(id);
        try (Response response = indexDataset(dataset, user)) {
          output.write(response.getEntity().toString().getBytes());
          if (!id.equals(lastDatasetId)) {
            output.write(",".getBytes());
          }
          output.write("\n".getBytes());
        } catch (IOException e) {
          logException("Error indexing dataset term for dataset id: %d ".formatted(dataset.getDatasetId()), e);
        }
      });
      output.write("]".getBytes());
    };
  }

  public Response indexStudy(Integer studyId, User user) {
    Study study = studyDAO.findStudyById(studyId);
    // The dao call above does not populate its datasets so we need to check for datasetIds
    if (study != null && study.getDatasetIds() != null && !study.getDatasetIds().isEmpty()) {
      List<Dataset> datasets = datasetDAO.findDatasetsByIdList(study.getDatasetIds().stream().toList());
      try (Response response = indexDatasets(datasets, user)) {
        return response;
      } catch (Exception e) {
        logException(String.format("Failed to index datasets for study id: %d", studyId), e);
        return Response.status(Status.INTERNAL_SERVER_ERROR).build();
      }
    }
    return Response.status(Status.NOT_FOUND).build();
  }

  public DatasetTerm toDatasetTerm(Dataset dataset) {
    if (Objects.isNull(dataset)) {
      return null;
    }

    DatasetTerm term = new DatasetTerm();

    term.setDatasetId(dataset.getDatasetId());
    Optional.ofNullable(dataset.getCreateUserId()).ifPresent(userId -> {
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

    Optional.ofNullable(dataset.getDacId()).ifPresent(dacId -> {
      Dac dac = dacDAO.findById(dataset.getDacId());
      term.setDacId(dataset.getDacId());
      if (Objects.nonNull(dataset.getDacApproval())) {
        term.setDacApproval(dataset.getDacApproval());
      }
      term.setDac(toDacTerm(dac));
    });

    List<Integer> approvedUserIds = dataAccessRequestDAO
        .findApprovedDARsByDatasetId(dataset.getDatasetId())
        .stream()
        .map(DataAccessRequest::getUserId)
        .toList();

    if (!approvedUserIds.isEmpty()) {
      term.setApprovedUserIds(approvedUserIds);
    }

    if (Objects.nonNull(dataset.getDataUse())) {
      DataUseSummary summary = ontologyService.translateDataUseSummary(dataset.getDataUse());
      if (summary != null) {
        term.setDataUse(summary);
      } else {
        logWarn("No data use summary for dataset id: %d".formatted(dataset.getDatasetId()));
      }
    }

    findDatasetProperty(
        dataset.getProperties(), "accessManagement"
    ).ifPresent(
        datasetProperty -> term.setAccessManagement(datasetProperty.getPropertyValueAsString())
    );

    findFirstDatasetPropertyByName(
        dataset.getProperties(), "# of participants"
    ).ifPresent(
        datasetProperty -> {
          String value = datasetProperty.getPropertyValueAsString();
          try {
            term.setParticipantCount(Integer.valueOf(value));
          } catch (NumberFormatException e) {
            logWarn(String.format("Unable to coerce participant count to integer: %s for dataset: %s", value, dataset.getDatasetIdentifier()));
          }
        }
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

  protected void updateDatasetIndexDate(Integer datasetId, Integer userId, Instant indexDate)
      throws SQLException {
    // It is possible that a dataset has been deleted. If so, we don't want to try and update it.
    Dataset dataset = datasetDAO.findDatasetById(datasetId);
    if (dataset != null) {
      datasetServiceDAO.updateDatasetIndex(datasetId, userId, indexDate);
    }
  }

  Optional<DatasetProperty> findDatasetProperty(Collection<DatasetProperty> props,
      String schemaProp) {
    return
        (props == null) ? Optional.empty() : props
            .stream()
            .filter(p -> Objects.nonNull(p.getSchemaProperty()))
            .filter(p -> p.getSchemaProperty().equals(schemaProp))
            .findFirst();
  }

  Optional<DatasetProperty> findFirstDatasetPropertyByName(Collection<DatasetProperty> props,
      String propertyName) {
    return
        (props == null) ? Optional.empty(): props
            .stream()
            .filter(p -> p.getPropertyName().equalsIgnoreCase(propertyName))
            .findFirst();
  }

  Optional<StudyProperty> findStudyProperty(Collection<StudyProperty> props, String key) {
    return
        (props == null) ? Optional.empty() : props
            .stream()
            .filter(p -> p.getKey().equals(key))
            .findFirst();
  }

}
