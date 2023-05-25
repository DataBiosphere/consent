package org.broadinstitute.consent.http.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.broadinstitute.consent.http.configurations.ElasticSearchConfiguration;
import org.broadinstitute.consent.http.db.DacDAO;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetProperty;
import org.broadinstitute.consent.http.models.Study;
import org.broadinstitute.consent.http.models.StudyProperty;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.elastic_search.DatasetTerm;
import org.broadinstitute.consent.http.models.elastic_search.StudyTerm;
import org.broadinstitute.consent.http.models.elastic_search.UserTerm;
import org.broadinstitute.consent.http.util.ConsentLogger;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;

public class ElasticSearchService implements ConsentLogger {

  private final RestClient esClient;
  private final ElasticSearchConfiguration esConfig;
  private final DatasetDAO datasetDAO;
  private final DataAccessRequestDAO dataAccessRequestDAO;
  private final DacDAO dacDAO;
  private final UserDAO userDAO;
  private final UseRestrictionConverter useRestrictionConverter;

  public ElasticSearchService(
      RestClient esClient,
      ElasticSearchConfiguration esConfig,
      UseRestrictionConverter useRestrictionConverter,
      DatasetDAO datasetDAO,
      DataAccessRequestDAO dataAccessRequestDAO,
      DacDAO dacDAO,
      UserDAO userDAO) {
    this.esClient = esClient;
    this.esConfig = esConfig;
    this.useRestrictionConverter = useRestrictionConverter;
    this.datasetDAO = datasetDAO;
    this.dataAccessRequestDAO = dataAccessRequestDAO;
    this.dacDAO = dacDAO;
    this.userDAO = userDAO;
  }


  private static final String bulkHeader = """
      { "index": {"_type": "dataset", "_id": "%d"} }
      """;

  public Response indexDatasets(List<DatasetTerm> datasets) throws IOException {
    List<String> bulkApiCall = new ArrayList<>();

    datasets.forEach((dsTerm) -> {
      bulkApiCall.add(bulkHeader.formatted(dsTerm.getDatasetId()));
      bulkApiCall.add(GsonUtil.getInstance().toJson(dsTerm));
    });

    Request bulkRequest = new Request(
        "PUT",
        "/" + esConfig.getDatasetIndexName());

    bulkRequest.setEntity(new NStringEntity(
        String.join("\n", bulkApiCall),
        ContentType.APPLICATION_JSON));

    return esClient.performRequest(bulkRequest);
  }

  public UserTerm toUserTerm(User user) {
    if (Objects.isNull(user)) {
      return null;
    }

    UserTerm term = new UserTerm();

    term.setUserId(user.getUserId());

    return term;
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
        prop -> term.setPhenotype((String) prop.getValue())
    );

    findStudyProperty(
        study.getProperties(), "species"
    ).ifPresent(
        prop -> term.setSpecies((String) prop.getValue())
    );

    findStudyProperty(
        study.getProperties(), "dataCustodianEmail"
    ).ifPresent(
        prop -> term.setDataCustodian((String) prop.getValue())
    );

    if (Objects.nonNull(study.getCreateUserId())) {
      User dataSubmitter = userDAO.findUserById(study.getCreateUserId());
      term.setDataSubmitter(toUserTerm(dataSubmitter));
    }

    return term;
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

    Collection<Integer> approvedUserIds =
        dataAccessRequestDAO.findAllUserIdsWithApprovedDARsByDatasetId(
            dataset.getDataSetId());

    if (!approvedUserIds.isEmpty()) {
      Collection<User> approvedUsers = userDAO.findUsers(approvedUserIds);

      List<UserTerm> approvedUserTerms = approvedUsers.stream().map(this::toUserTerm).toList();
      term.setApprovedUsers(approvedUserTerms);
    }

    if (Objects.nonNull(dataset.getDataUse())) {
      term.setDataUse(useRestrictionConverter.translateDataUseSummary(dataset.getDataUse()));
    }

    findDatasetProperty(
        dataset.getProperties(), "openAccess"
    ).ifPresent(
        datasetProperty -> term.setOpenAccess((Boolean) datasetProperty.getPropertyValue())
    );

    // TODO: unsure how to get number of participants; there could
    // be multiple if there are multiple file types

    findDatasetProperty(
        dataset.getProperties(), "url"
    ).ifPresent(
        datasetProperty -> term.setUrl((String) datasetProperty.getPropertyValue())
    );

    findDatasetProperty(
        dataset.getProperties(), "dataLocation"
    ).ifPresent(
        datasetProperty -> term.setDataLocation((String) datasetProperty.getPropertyValue())
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