package org.broadinstitute.consent.http.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.RandomStringUtils;
import org.broadinstitute.consent.http.configurations.ElasticSearchConfiguration;
import org.broadinstitute.consent.http.db.DacDAO;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.enumeration.PropertyType;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetProperty;
import org.broadinstitute.consent.http.models.Study;
import org.broadinstitute.consent.http.models.StudyProperty;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.models.elastic_search.DatasetTerm;
import org.broadinstitute.consent.http.models.elastic_search.UserTerm;
import org.broadinstitute.consent.http.models.ontology.DataUseSummary;
import org.broadinstitute.consent.http.models.ontology.DataUseTerm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

public class ElasticSearchServiceTest {

  private ElasticSearchService service;

  @Mock
  private ElasticsearchClient esClient;

  @Mock
  private UseRestrictionConverter useRestrictionConverter;

  @Mock
  private DacDAO dacDAO;

  @Mock
  private UserDAO userDAO;

  @Mock
  private DatasetDAO datasetDAO;

  @Mock
  private ElasticSearchConfiguration esConfig;

  @Mock
  private DataAccessRequestDAO dataAccessRequestDAO;

  @BeforeEach
  public void setUp() {
    openMocks(this);
  }

  private void initService() {
    service = new ElasticSearchService(esClient, esConfig, useRestrictionConverter, datasetDAO,
        dataAccessRequestDAO,
        dacDAO, userDAO);
  }

  @Test
  public void testToDatasetTermComplete() {
    Dataset dataset = new Dataset();
    dataset.setDataSetId(100);
    dataset.setAlias(10);
    dataset.setDatasetIdentifier();
    dataset.setDacId(1);

    Study study = new Study();
    study.setName(RandomStringUtils.randomAlphabetic(10));
    study.setDescription(RandomStringUtils.randomAlphabetic(20));
    study.setStudyId(12345);
    study.setPiName(RandomStringUtils.randomAlphabetic(10));
    study.setDataTypes(List.of(RandomStringUtils.randomAlphabetic(10)));
    study.setCreateUserId(15);
    study.setPublicVisibility(true);

    StudyProperty phenotypeProperty = new StudyProperty();
    phenotypeProperty.setKey("phenotypeIndication");
    phenotypeProperty.setType(PropertyType.String);
    phenotypeProperty.setValue(RandomStringUtils.randomAlphabetic(10));

    StudyProperty speciesProperty = new StudyProperty();
    speciesProperty.setKey("species");
    speciesProperty.setType(PropertyType.String);
    speciesProperty.setValue(RandomStringUtils.randomAlphabetic(10));

    StudyProperty dataCustodianEmailProperty = new StudyProperty();
    dataCustodianEmailProperty.setKey("dataCustodianEmail");
    dataCustodianEmailProperty.setType(PropertyType.String);
    dataCustodianEmailProperty.setValue(RandomStringUtils.randomAlphabetic(10));

    study.setProperties(Set.of(phenotypeProperty, speciesProperty, dataCustodianEmailProperty));

    dataset.setStudy(study);

    DatasetProperty openAccessProp = new DatasetProperty();
    openAccessProp.setSchemaProperty("openAccess");
    openAccessProp.setPropertyType(PropertyType.Boolean);
    openAccessProp.setPropertyValue(true);

    DatasetProperty dataLocationProp = new DatasetProperty();
    dataLocationProp.setSchemaProperty("dataLocation");
    dataLocationProp.setPropertyType(PropertyType.String);
    dataLocationProp.setPropertyValue("some location");

    dataset.setProperties(Set.of(openAccessProp, dataLocationProp));

    User dataSubmitter = new User();
    dataSubmitter.setUserId(9);

    User approvedUser1 = new User();
    approvedUser1.setUserId(10);
    User approvedUser2 = new User();
    approvedUser2.setUserId(11);

    DataUseSummary dataUseSummary = new DataUseSummary();
    dataUseSummary.setPrimary(List.of(new DataUseTerm("DS", "Description")));
    dataUseSummary.setPrimary(List.of(new DataUseTerm("NMDS", "Description")));

    when(userDAO.findUserById(study.getCreateUserId())).thenReturn(dataSubmitter);
    when(dataAccessRequestDAO.findAllUserIdsWithApprovedDARsByDatasetId(any())).thenReturn(
        List.of(1, 2));
    when(userDAO.findUsers(List.of(1, 2))).thenReturn(List.of(approvedUser1, approvedUser2));
    when(useRestrictionConverter.translateDataUseSummary(any())).thenReturn(dataUseSummary);

    initService();
    DatasetTerm term = service.toDatasetTerm(dataset);

    assertEquals(dataset.getDataSetId(), term.getDatasetId());
    assertEquals(dataset.getDatasetIdentifier(), term.getDatasetIdentifier());
    assertEquals(study.getDescription(), term.getStudy().getDescription());
    assertEquals(study.getName(), term.getStudy().getStudyName());
    assertEquals(study.getStudyId(), term.getStudy().getStudyId());
    assertEquals(phenotypeProperty.getValue(), term.getStudy().getPhenotype());
    assertEquals(speciesProperty.getValue(), term.getStudy().getSpecies());
    assertEquals(study.getPiName(), term.getStudy().getPiName());
    assertEquals(
        dataSubmitter.getUserId(),
        term.getStudy().getDataSubmitter().getUserId());
    assertEquals(dataCustodianEmailProperty.getValue(), term.getStudy().getDataCustodian());

    assertEquals(dataUseSummary, term.getDataUse());

    assertEquals(study.getDataTypes(), term.getStudy().getDataTypes());
    assertEquals(dataLocationProp.getPropertyValue(), term.getDataLocation());
    assertEquals(dataset.getDacId(), term.getDacId());
    assertEquals(openAccessProp.getPropertyValue(), term.getOpenAccess());
    assertEquals(study.getPublicVisibility(), term.getStudy().getPublicVisibility());

    assertEquals(
        approvedUser1.getUserId(),
        term.getApprovedUsers().get(0).getUserId());
    assertEquals(
        approvedUser2.getUserId(),
        term.getApprovedUsers().get(1).getUserId());

  }

  @Test
  public void testToDatasetTermIncomplete() {
    Dataset dataset = new Dataset();
    dataset.setDataSetId(100);
    dataset.setAlias(10);
    dataset.setDatasetIdentifier();
    dataset.setProperties(Set.of());

    when(dacDAO.findById(any())).thenReturn(null);
    when(userDAO.findUserById(any())).thenReturn(null);
    when(dataAccessRequestDAO.findAllUserIdsWithApprovedDARsByDatasetId(any())).thenReturn(
        List.of());
    when(userDAO.findUsers(any())).thenReturn(List.of());
    when(useRestrictionConverter.translateDataUseSummary(any())).thenReturn(new DataUseSummary());

    initService();
    DatasetTerm term = service.toDatasetTerm(dataset);

    assertEquals(dataset.getDataSetId(), term.getDatasetId());
    assertEquals(dataset.getDatasetIdentifier(), term.getDatasetIdentifier());
    assertNotNull(term.getDataUse());
  }

  @Test
  public void toUserTerm() {
    User user = new User();

    user.setUserId(100);
    user.setEmail(RandomStringUtils.randomAlphabetic(10) + "@gmail.com");
    user.setDisplayName(RandomStringUtils.randomAlphabetic(10));

    user.setRoles(
        List.of(
            new UserRole(1, "Example Role 1"),
            new UserRole(2, "Example Role 2")
        )
    );

    initService();
    UserTerm term = service.toUserTerm(user);

    assertEquals(user.getUserId(), term.getUserId());
  }

  @Captor
  ArgumentCaptor<BulkRequest> bulkRequest;

  @Test
  public void testIndexDatasets() throws IOException {
    spy(esClient);

    DatasetTerm term1 = new DatasetTerm();
    term1.setDatasetId(1);
    DatasetTerm term2 = new DatasetTerm();
    term2.setDatasetId(2);

    String datasetIndexName = RandomStringUtils.randomAlphabetic(10);

    when(esConfig.getDatasetIndexName()).thenReturn(datasetIndexName);

    initService();
    service.indexDatasets(List.of(term1, term2));

    verify(esClient).bulk(bulkRequest.capture());

    BulkRequest.Builder expectedBr = new BulkRequest.Builder();

    expectedBr.operations(op -> op
        .index(idx -> idx
            .index(datasetIndexName)
            .id(term1.getDatasetId().toString())
            .document(term1))
    ).operations(op -> op
        .index(idx -> idx
            .index(datasetIndexName)
            .id(term2.getDatasetId().toString())
            .document(term2))
    );

    assertEquals(
        expectedBr.build().toString(),
        bulkRequest.getValue().toString());

  }
}
