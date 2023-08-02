package org.broadinstitute.consent.http.service;

import static jakarta.ws.rs.core.Response.Status.fromStatusCode;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpVersion;
import org.apache.http.entity.ContentType;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.nio.entity.NStringEntity;
import org.broadinstitute.consent.http.configurations.ElasticSearchConfiguration;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.enumeration.PropertyType;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetProperty;
import org.broadinstitute.consent.http.models.Study;
import org.broadinstitute.consent.http.models.StudyProperty;
import org.broadinstitute.consent.http.models.elastic_search.DatasetTerm;
import org.broadinstitute.consent.http.models.ontology.DataUseSummary;
import org.broadinstitute.consent.http.models.ontology.DataUseTerm;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

public class ElasticSearchServiceTest {

  private ElasticSearchService service;

  @Mock
  private RestClient esClient;

  @Mock
  private OntologyService ontologyService;

  @Mock
  private ElasticSearchConfiguration esConfig;

  @Mock
  private DataAccessRequestDAO dataAccessRequestDAO;

  @BeforeEach
  public void setUp() {
    openMocks(this);
  }

  private void initService() {
    service = new ElasticSearchService(
        esClient,
        esConfig,
        dataAccessRequestDAO,
        ontologyService);
  }

  private void mockElasticSearchResponse(int statusCode, String body) throws IOException {
    Response response = mock(Response.class);
    String reasonPhrase = fromStatusCode(statusCode).getReasonPhrase();
    BasicStatusLine status = new BasicStatusLine(HttpVersion.HTTP_1_1, statusCode, reasonPhrase);
    HttpEntity entity = new NStringEntity(body, ContentType.APPLICATION_JSON);

    when(esClient.performRequest(any())).thenReturn(response);
    when(response.getStatusLine()).thenReturn(status);
    when(response.getEntity()).thenReturn(entity);
  }

  @Test
  public void testToDatasetTermComplete() {
    Dataset dataset = new Dataset();
    dataset.setDataSetId(100);
    dataset.setAlias(10);
    dataset.setDatasetIdentifier();
    dataset.setDacId(1);
    dataset.setDataUse(new DataUse());

    Study study = new Study();
    study.setName(RandomStringUtils.randomAlphabetic(10));
    study.setDescription(RandomStringUtils.randomAlphabetic(20));
    study.setStudyId(12345);
    study.setPiName(RandomStringUtils.randomAlphabetic(10));
    study.setDataTypes(List.of(RandomStringUtils.randomAlphabetic(10)));
    study.setCreateUserId(9);
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

    DatasetProperty numParticipantsProp = new DatasetProperty();
    numParticipantsProp.setSchemaProperty("numberOfParticipants");
    numParticipantsProp.setPropertyType(PropertyType.Number);
    numParticipantsProp.setPropertyValue(20);

    dataset.setProperties(Set.of(openAccessProp, dataLocationProp, numParticipantsProp));

    DataUseSummary dataUseSummary = new DataUseSummary();
    dataUseSummary.setPrimary(List.of(new DataUseTerm("DS", "Description")));
    dataUseSummary.setPrimary(List.of(new DataUseTerm("NMDS", "Description")));

    when(dataAccessRequestDAO.findAllUserIdsWithApprovedDARsByDatasetId(any())).thenReturn(
        List.of(10, 11));
    when(ontologyService.translateDataUseSummary(any())).thenReturn(dataUseSummary);

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
        study.getCreateUserId(),
        term.getStudy().getDataSubmitterId());
    assertEquals(dataCustodianEmailProperty.getValue(), term.getStudy().getDataCustodian());

    assertEquals(dataUseSummary, term.getDataUse());

    assertEquals(study.getDataTypes(), term.getStudy().getDataTypes());
    assertEquals(dataLocationProp.getPropertyValue(), term.getDataLocation());
    assertEquals(dataset.getDacId(), term.getDacId());
    assertEquals(openAccessProp.getPropertyValue(), term.getOpenAccess());
    assertEquals(study.getPublicVisibility(), term.getStudy().getPublicVisibility());

    assertEquals(numParticipantsProp.getPropertyValue(), term.getParticipantCount());

    assertEquals(List.of(10, 11),
        term.getApprovedUserIds());

  }

  @Test
  public void testToDatasetTermIncomplete() {
    Dataset dataset = new Dataset();
    dataset.setDataSetId(100);
    dataset.setAlias(10);
    dataset.setDatasetIdentifier();
    dataset.setProperties(Set.of());

    when(dataAccessRequestDAO.findAllUserIdsWithApprovedDARsByDatasetId(any())).thenReturn(
        List.of());

    initService();
    DatasetTerm term = service.toDatasetTerm(dataset);

    assertEquals(dataset.getDataSetId(), term.getDatasetId());
    assertEquals(dataset.getDatasetIdentifier(), term.getDatasetIdentifier());
  }

  @Captor
  ArgumentCaptor<Request> request;

  @Test
  public void testIndexDatasets() throws IOException {
    DatasetTerm term1 = new DatasetTerm();
    term1.setDatasetId(1);
    DatasetTerm term2 = new DatasetTerm();
    term2.setDatasetId(2);

    String datasetIndexName = RandomStringUtils.randomAlphabetic(10);

    when(esConfig.getDatasetIndexName()).thenReturn(datasetIndexName);
    mockElasticSearchResponse(200, "");

    initService();
    service.indexDatasetTerms(List.of(term1, term2));

    verify(esClient).performRequest(request.capture());

    Request capturedRequest = request.getValue();

    assertEquals("PUT", capturedRequest.getMethod());
    assertEquals("""
            { "index": {"_type": "dataset", "_id": "1"} }
            {"datasetId":1}
            { "index": {"_type": "dataset", "_id": "2"} }
            {"datasetId":2}
                        
            """,
        new String(capturedRequest.getEntity().getContent().readAllBytes(),
            StandardCharsets.UTF_8));
  }

  @Test
  public void testSearchDatasets() throws IOException {
    String query = "{ \"query\": { \"query_string\": { \"query\": \"(GRU) AND (HMB)\" } } }";

    /*
     * FIXME: this approach is kind of hacky, we stick both the validation response and the search
     *  response in the same body, and then rely on Gson to parse these into separate objects.
     *  Ideally each request and response should be mocked separately, but this would involve many
     *  more classes and methods. Alternately, it is possible to just mock the Gson parsing, but
     *  this seems to affect the results of the other tests.
     */
    mockElasticSearchResponse(200, "{\"valid\":true,\"hits\":{\"hits\":[]}}");

    initService();
    var response = service.searchDatasets(query);
    assertEquals(200, response.getStatus());
  }

  @Test
  public void testValidateQuery() throws IOException {
    String query = "{ \"query\": { \"query_string\": { \"query\": \"(GRU) AND (HMB)\" } } }";

    mockElasticSearchResponse(200, "{\"valid\":true}");

    initService();
    assertTrue(service.validateQuery(query));
  }

  @Test
  public void testValidateQueryWithFromAndSize() throws IOException {
    String query = "{ \"from\": 0, \"size\": 100, \"query\": { \"query_string\": { \"query\": \"(GRU) AND (HMB)\" } } }";

    mockElasticSearchResponse(200, "{\"valid\":true}");

    initService();
    assertTrue(service.validateQuery(query));
  }

  @Test
  public void testValidateQueryEmpty() throws IOException {
    String query = "{}";

    mockElasticSearchResponse(400, "Bad Request");

    initService();
    assertThrows(IOException.class, () -> service.validateQuery(query));
  }

  @Test
  public void testValidateQueryInvalid() throws IOException {
    String query = "{ \"bad\": [\"and\", \"invalid\"] }";

    mockElasticSearchResponse(200, "{\"valid\":false}");

    initService();
    assertFalse(service.validateQuery(query));
  }
}
