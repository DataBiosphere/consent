package org.broadinstitute.consent.http.service;

import jakarta.ws.rs.HttpMethod;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.broadinstitute.consent.http.configurations.ElasticSearchConfiguration;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetProperty;
import org.broadinstitute.consent.http.models.Study;
import org.broadinstitute.consent.http.models.StudyProperty;
import org.broadinstitute.consent.http.models.elastic_search.DatasetTerm;
import org.broadinstitute.consent.http.models.elastic_search.StudyTerm;
import org.broadinstitute.consent.http.util.ConsentLogger;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;

public class ElasticSearchService implements ConsentLogger {

  private final RestClient esClient;
  private final ElasticSearchConfiguration esConfig;
  private final DataAccessRequestDAO dataAccessRequestDAO;

  private final OntologyService ontologyService;

  public ElasticSearchService(
      RestClient esClient,
      ElasticSearchConfiguration esConfig,
      DataAccessRequestDAO dataAccessRequestDAO,
      OntologyService ontologyService) {
    this.esClient = esClient;
    this.esConfig = esConfig;
    this.dataAccessRequestDAO = dataAccessRequestDAO;
    this.ontologyService = ontologyService;
  }


  private static final String bulkHeader = """
      { "index": {"_type": "dataset", "_id": "%d"} }
      """;

  public Response indexDatasetTerms(List<DatasetTerm> datasets) throws IOException {
    List<String> bulkApiCall = new ArrayList<>();

    datasets.forEach((dsTerm) -> {
      bulkApiCall.add(bulkHeader.formatted(dsTerm.getDatasetId()));
      bulkApiCall.add(GsonUtil.getInstance().toJson(dsTerm) + "\n");
    });

    Request bulkRequest = new Request(
        HttpMethod.PUT,
        "/" + esConfig.getDatasetIndexName() + "/_bulk");

    bulkRequest.setEntity(new NStringEntity(
        String.join("", bulkApiCall) + "\n",
        ContentType.DEFAULT_BINARY));

    return esClient.performRequest(bulkRequest);
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
        prop -> term.setDataCustodian(prop.getValue().toString())
    );

    if (Objects.nonNull(study.getCreateUserId())) {
      term.setDataSubmitterId(study.getCreateUserId());
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
    term.setDatasetIdentifier(dataset.getDatasetIdentifier());

    if (Objects.nonNull(dataset.getStudy())) {
      term.setStudy(toStudyTerm(dataset.getStudy()));
    }

    term.setDacId(dataset.getDacId());

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
            .filter((p) -> Objects.nonNull(p.getSchemaProperty()))
            .filter((p) -> p.getSchemaProperty().equals(schemaProp))
            .findFirst();
  }

  Optional<StudyProperty> findStudyProperty(Collection<StudyProperty> props, String key) {
    if (Objects.isNull(props)) {
      return Optional.empty();
    }

    return
        props
            .stream()
            .filter((p) -> p.getKey().equals(key))
            .findFirst();
  }


}
