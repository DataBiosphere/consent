package org.broadinstitute.consent.http.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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

public class ElasticSearchService implements ConsentLogger {

  private final ElasticsearchClient esClient;
  private final ElasticSearchConfiguration esConfig;
  private final DatasetDAO datasetDAO;
  private final DataAccessRequestDAO dataAccessRequestDAO;
  private final DacDAO dacDAO;
  private final UserDAO userDAO;
  private final UseRestrictionConverter useRestrictionConverter;

  public ElasticSearchService(
      ElasticsearchClient esClient,
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

  public BulkResponse indexDatasets(List<DatasetTerm> datasets) throws IOException {
    BulkRequest.Builder br = new BulkRequest.Builder();

    for (DatasetTerm dataset : datasets) {
      br.operations(op -> op
          .index(idx -> idx
              .index(esConfig.getDatasetIndexName())
              .id(dataset.getDatasetId().toString())
              .document(dataset)
          )
      );
    }

    return esClient.bulk(br.build());
  }

  public UserTerm toUserTerm(User user) {

    UserTerm term = new UserTerm();

    term.setUserId(user.getUserId());

    return term;
  }

  public StudyTerm toStudyTerm(Study study) {
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

    User dataSubmitter = userDAO.findUserById(study.getCreateUserId());
    term.setDataSubmitter(toUserTerm(dataSubmitter));

    return term;
  }

  public DatasetTerm toDatasetTerm(Dataset dataset) {
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

    term.setDataUse(useRestrictionConverter.translateDataUseSummary(dataset.getDataUse()));

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
            .filter((p) -> p.getSchemaProperty().equals(schemaProp))
            .findFirst();
  }

  Optional<StudyProperty> findStudyProperty(Collection<StudyProperty> props, String key) {
    return
        props
            .stream()
            .filter((p) -> p.getKey().equals(key))
            .findFirst();
  }


}
