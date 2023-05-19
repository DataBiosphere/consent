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
import org.broadinstitute.consent.http.db.DacDAO;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.enumeration.PropertyType;
import org.broadinstitute.consent.http.models.Dac;
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
  ElasticsearchClient esClient;

  @Mock
  UseRestrictionConverter useRestrictionConverter;

  @Mock
  private DacDAO dacDAO;

  @Mock
  private UserDAO userDAO;

  @Mock
  private DatasetDAO datasetDAO;

  @Mock
  DataAccessRequestDAO dataAccessRequestDAO;

  @BeforeEach
  public void setUp() {
    openMocks(this);
  }

  private void initService() {
    service = new ElasticSearchService(esClient, useRestrictionConverter, datasetDAO,
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

    Dac dac = new Dac();
    dac.setName(RandomStringUtils.randomAlphabetic(10));

    User dataSubmitter = new User();
    dataSubmitter.setDisplayName(RandomStringUtils.randomAlphabetic(10));

    User approvedUser1 = new User();
    approvedUser1.setDisplayName(RandomStringUtils.randomAlphabetic(10));
    User approvedUser2 = new User();
    approvedUser2.setDisplayName(RandomStringUtils.randomAlphabetic(10));

    DataUseSummary dataUseSummary = new DataUseSummary();
    dataUseSummary.setPrimary(List.of(new DataUseTerm("DS", "Description")));
    dataUseSummary.setPrimary(List.of(new DataUseTerm("NMDS", "Description")));

    when(dacDAO.findById(any())).thenReturn(dac);
    when(userDAO.findUserById(study.getCreateUserId())).thenReturn(dataSubmitter);
    when(dataAccessRequestDAO.findAllUserIdsWithApprovedDARsByDatasetId(any())).thenReturn(
        List.of(1, 2));
    when(userDAO.findUsers(List.of(1, 2))).thenReturn(List.of(approvedUser1, approvedUser2));
    when(useRestrictionConverter.translateDataUseSummary(any())).thenReturn(dataUseSummary);

    initService();
    DatasetTerm term = service.toDatasetTerm(dataset);

    assertEquals(dataset.getDataSetId(), term.getDatasetId());
    assertEquals(dataset.getDatasetIdentifier(), term.getDatasetIdentifier());
    assertEquals(study.getDescription(), term.getDescription());
    assertEquals(study.getName(), term.getStudyName());
    assertEquals(study.getStudyId(), term.getStudyId());
    assertEquals(phenotypeProperty.getValue(), term.getPhenotype());
    assertEquals(speciesProperty.getValue(), term.getSpecies());
    assertEquals(study.getPiName(), term.getPiName());
    assertEquals(
        dataSubmitter.getDisplayName(),
        term.getDataSubmitter().getDisplayName());
    assertEquals(dataCustodianEmailProperty.getValue(), term.getDataCustodian());

    assertEquals(dataUseSummary, term.getDataUse());

    assertEquals(study.getDataTypes(), term.getDataTypes());
    assertEquals(dataLocationProp.getPropertyValue(), term.getDataLocation());
    assertEquals(dac.getName(), term.getDacName());
    assertEquals(openAccessProp.getPropertyValue(), term.getOpenAccess());
    assertEquals(study.getPublicVisibility(), term.getPublicVisibility());

    assertEquals(
        approvedUser1.getDisplayName(),
        term.getApprovedUsers().get(0).getDisplayName());
    assertEquals(
        approvedUser2.getDisplayName(),
        term.getApprovedUsers().get(1).getDisplayName());

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
    assertEquals(
        List.of("Example Role 1", "Example Role 2"),
        term.getRoles());
    assertEquals(user.getEmail(), term.getEmail());
    assertEquals(user.getDisplayName(), term.getDisplayName());
  }

  @Captor
  ArgumentCaptor<BulkRequest> bulkRequest;

  @Test
  public void testIndexDatasets() throws IOException {
    spy(esClient);

    DatasetTerm term1 = new DatasetTerm();
    term1.setDatasetId(1);
    term1.setStudyName("Some study");
    DatasetTerm term2 = new DatasetTerm();
    term2.setDatasetId(2);

    initService();
    service.indexDatasets(List.of(term1, term2));

    verify(esClient).bulk(bulkRequest.capture());

    BulkRequest.Builder expectedBr = new BulkRequest.Builder();

    expectedBr.operations(op -> op
        .index(idx -> idx
            .index("datasets")
            .id(term1.getDatasetId().toString())
            .document(term1))
    ).operations(op -> op
        .index(idx -> idx
            .index("datasets")
            .id(term2.getDatasetId().toString())
            .document(term2))
    );

    assertEquals(
        expectedBr.build().toString(),
        bulkRequest.getValue().toString());

  }
}
