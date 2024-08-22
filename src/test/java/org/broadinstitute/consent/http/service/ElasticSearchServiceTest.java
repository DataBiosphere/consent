package org.broadinstitute.consent.http.service;

import static jakarta.ws.rs.core.Response.Status.fromStatusCode;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import jakarta.ws.rs.core.StreamingOutput;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpVersion;
import org.apache.http.StatusLine;
import org.apache.http.entity.ContentType;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.nio.entity.NStringEntity;
import org.broadinstitute.consent.http.configurations.ElasticSearchConfiguration;
import org.broadinstitute.consent.http.db.DacDAO;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.InstitutionDAO;
import org.broadinstitute.consent.http.db.StudyDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.enumeration.PropertyType;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetProperty;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.models.Study;
import org.broadinstitute.consent.http.models.StudyProperty;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.elastic_search.DatasetTerm;
import org.broadinstitute.consent.http.models.ontology.DataUseSummary;
import org.broadinstitute.consent.http.models.ontology.DataUseTerm;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ElasticSearchServiceTest {

  private ElasticSearchService service;

  @Mock
  private RestClient esClient;

  @Mock
  private OntologyService ontologyService;

  @Mock
  private ElasticSearchConfiguration esConfig;

  @Mock
  private DacDAO dacDAO;

  @Mock
  private DataAccessRequestDAO dataAccessRequestDAO;

  @Mock
  private UserDAO userDao;

  @Mock
  private InstitutionDAO institutionDAO;

  @Mock
  private DatasetDAO datasetDAO;
  @Mock
  private StudyDAO studyDAO;

  private void initService() {
    service = new ElasticSearchService(
        esClient,
        esConfig,
        dacDAO,
        dataAccessRequestDAO,
        userDao,
        ontologyService,
        institutionDAO,
        datasetDAO,
        studyDAO);
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

  private Institution createInstitution() {
    Institution institution = new Institution();
    institution.setId(RandomUtils.nextInt(1, 1000));
    return institution;
  }

  private User createUser(int start, int max) {
    User user = new User();
    user.setUserId(RandomUtils.nextInt(start, max));
    user.setDisplayName(RandomStringUtils.randomAlphabetic(10));
    user.setEmail(RandomStringUtils.randomAlphabetic(10));
    Institution i = createInstitution();
    user.setInstitution(i);
    user.setInstitutionId(i.getId());
    return user;
  }

  private Dataset createDataset(User user, User updateUser, DataUse dataUse, Dac dac) {
    Dataset dataset = new Dataset();
    dataset.setDataSetId(RandomUtils.nextInt(1, 100));
    dataset.setAlias(dataset.getDataSetId());
    dataset.setDatasetIdentifier();
    dataset.setDeletable(true);
    dataset.setName(RandomStringUtils.randomAlphabetic(10));
    dataset.setDatasetName(dataset.getName());
    dataset.setDacId(dac.getDacId());
    dataset.setDacApproval(true);
    dataset.setDataUse(dataUse);
    dataset.setCreateUser(user);
    dataset.setUpdateUserId(updateUser.getUserId());
    dataset.setCreateUserId(user.getUserId());
    return dataset;
  }

  private Dac createDac() {
    Dac dac = new Dac();
    dac.setDacId(RandomUtils.nextInt(1, 100));
    dac.setName(RandomStringUtils.randomAlphabetic(10));
    return dac;
  }

  private Study createStudy(User user) {
    Study study = new Study();
    study.setName(RandomStringUtils.randomAlphabetic(10));
    study.setDescription(RandomStringUtils.randomAlphabetic(20));
    study.setStudyId(RandomUtils.nextInt(1, 100));
    study.setPiName(RandomStringUtils.randomAlphabetic(10));
    study.setDataTypes(List.of(RandomStringUtils.randomAlphabetic(10)));
    study.setPublicVisibility(true);
    study.setCreateUserEmail(user.getEmail());
    study.setCreateUserId(user.getUserId());
    study.setCreateUserEmail(user.getEmail());
    return study;
  }

  private StudyProperty createStudyProperty(String key, PropertyType type) {
    StudyProperty prop = new StudyProperty();
    prop.setKey(key);
    prop.setType(type);
    switch (type) {
      case Boolean -> prop.setValue(true);
      case Json -> {
        var val = new JsonArray();
        val.add(RandomStringUtils.randomAlphabetic(10));
        prop.setValue(val);
      }
      case Number -> prop.setValue(RandomUtils.nextInt(1, 100));
      default -> prop.setValue(RandomStringUtils.randomAlphabetic(10));
    }
    return prop;
  }

  private DatasetProperty createDatasetProperty(String schemaProp, PropertyType type,
      String propertyName) {
    DatasetProperty prop = new DatasetProperty();
    prop.setSchemaProperty(schemaProp);
    prop.setPropertyType(type);
    prop.setPropertyName(propertyName);
    switch (type) {
      case Boolean -> prop.setPropertyValue(true);
      case Number -> prop.setPropertyValue(RandomUtils.nextInt(1, 100));
      default -> prop.setPropertyValue(RandomStringUtils.randomAlphabetic(10));
    }
    return prop;
  }

  private DataUseSummary createDataUseSummary() {
    DataUseSummary dataUseSummary = new DataUseSummary();
    dataUseSummary.setPrimary(List.of(new DataUseTerm("DS", "Description")));
    dataUseSummary.setPrimary(List.of(new DataUseTerm("NMDS", "Description")));
    return dataUseSummary;
  }

  /**
   * Private container record to consolidate dataset and associated object creation
   */
  private record DatasetRecord(User createUser, User updateUser, Dac dac, Dataset dataset,
                               Study study) {

  }

  private DatasetRecord createDatasetRecord() {
    User user = createUser(1, 100);
    User updateUser = createUser(101, 200);
    Dac dac = createDac();
    Study study = createStudy(user);
    study.setProperties(Set.of(
        createStudyProperty("dbGaPPhsID", PropertyType.String),
        createStudyProperty("phenotypeIndication", PropertyType.String),
        createStudyProperty("species", PropertyType.String),
        createStudyProperty("dataCustodianEmail", PropertyType.Json)
    ));
    Dataset dataset = createDataset(user, updateUser, new DataUse(), dac);
    dataset.setProperties(Set.of(
        createDatasetProperty("accessManagement", PropertyType.Boolean, "accessManagement"),
        createDatasetProperty("numberOfParticipants", PropertyType.Number, "# of participants"),
        createDatasetProperty("url", PropertyType.String, "url"),
        createDatasetProperty("dataLocation", PropertyType.String, "dataLocation")
    ));
    dataset.setStudy(study);
    return new DatasetRecord(user, updateUser, dac, dataset, study);
  }

  @Test
  void testToDatasetTerm_UserInfo() {
    DatasetRecord datasetRecord = createDatasetRecord();
    when(userDao.findUserById(datasetRecord.createUser.getUserId())).thenReturn(
        datasetRecord.createUser);
    when(userDao.findUserById(datasetRecord.updateUser.getUserId())).thenReturn(
        datasetRecord.updateUser);
    when(
        institutionDAO.findInstitutionById(datasetRecord.createUser.getInstitutionId())).thenReturn(
        datasetRecord.createUser.getInstitution());
    when(
        institutionDAO.findInstitutionById(datasetRecord.updateUser.getInstitutionId())).thenReturn(
        datasetRecord.updateUser.getInstitution());
    when(dacDAO.findById(any())).thenReturn(datasetRecord.dac);

    initService();
    DatasetTerm term = service.toDatasetTerm(datasetRecord.dataset);
    assertEquals(datasetRecord.createUser.getUserId(), term.getCreateUserId());
    assertEquals(datasetRecord.createUser.getDisplayName(), term.getCreateUserDisplayName());
    assertEquals(datasetRecord.createUser.getUserId(), term.getSubmitter().userId());
    assertEquals(datasetRecord.createUser.getDisplayName(), term.getSubmitter().displayName());
    assertEquals(datasetRecord.createUser.getInstitutionId(),
        term.getSubmitter().institution().id());
    assertEquals(datasetRecord.createUser.getInstitution().getName(),
        term.getSubmitter().institution().name());
    assertEquals(datasetRecord.updateUser.getUserId(), term.getUpdateUser().userId());
    assertEquals(datasetRecord.updateUser.getDisplayName(), term.getUpdateUser().displayName());
    assertEquals(datasetRecord.updateUser.getInstitutionId(),
        term.getUpdateUser().institution().id());
    assertEquals(datasetRecord.updateUser.getInstitution().getName(),
        term.getUpdateUser().institution().name());
  }

  @Test
  void testToDatasetTerm_StudyInfo() {
    DatasetRecord datasetRecord = createDatasetRecord();
    when(userDao.findUserById(datasetRecord.createUser.getUserId())).thenReturn(
        datasetRecord.createUser);
    when(userDao.findUserById(datasetRecord.updateUser.getUserId())).thenReturn(
        datasetRecord.updateUser);
    when(dacDAO.findById(any())).thenReturn(datasetRecord.dac);

    initService();
    DatasetTerm term = service.toDatasetTerm(datasetRecord.dataset);
    assertEquals(datasetRecord.study.getDescription(), term.getStudy().getDescription());
    assertEquals(datasetRecord.study.getName(), term.getStudy().getStudyName());
    assertEquals(datasetRecord.study.getStudyId(), term.getStudy().getStudyId());
    Optional<StudyProperty> phsIdProp = datasetRecord.study.getProperties().stream()
        .filter(p -> p.getKey().equals("dbGaPPhsID")).findFirst();
    assertTrue(phsIdProp.isPresent());
    assertEquals(phsIdProp.get().getValue().toString(), term.getStudy().getPhsId());
    Optional<StudyProperty> phenoProp = datasetRecord.study.getProperties().stream()
        .filter(p -> p.getKey().equals("phenotypeIndication")).findFirst();
    assertTrue(phenoProp.isPresent());
    assertEquals(phenoProp.get().getValue().toString(), term.getStudy().getPhenotype());
    Optional<StudyProperty> speciesProp = datasetRecord.study.getProperties().stream()
        .filter(p -> p.getKey().equals("species")).findFirst();
    assertTrue(speciesProp.isPresent());
    assertEquals(speciesProp.get().getValue().toString(), term.getStudy().getSpecies());
    assertEquals(datasetRecord.study.getPiName(), term.getStudy().getPiName());
    assertEquals(datasetRecord.study.getCreateUserEmail(), term.getStudy().getDataSubmitterEmail());
    assertEquals(datasetRecord.study.getCreateUserId(), term.getStudy().getDataSubmitterId());
    Optional<StudyProperty> custodianProp = datasetRecord.study.getProperties().stream()
        .filter(p -> p.getKey().equals("dataCustodianEmail")).findFirst();
    assertTrue(custodianProp.isPresent());
    String termCustodians = GsonUtil.getInstance()
        .toJson(term.getStudy().getDataCustodianEmail(), ArrayList.class);
    assertEquals(custodianProp.get().getValue().toString(), termCustodians);
    assertEquals(datasetRecord.study.getPublicVisibility(), term.getStudy().getPublicVisibility());
    assertEquals(datasetRecord.study.getDataTypes(), term.getStudy().getDataTypes());
  }

  @Test
  void testToDatasetTerm_DatasetInfo() {
    DataAccessRequest dar1 = new DataAccessRequest();
    dar1.setUserId(1);
    DataAccessRequest dar2 = new DataAccessRequest();
    dar2.setUserId(2);
    List<Integer> approvedUserIds = List.of(dar1.getUserId(), dar2.getUserId());
    DataUseSummary dataUseSummary = createDataUseSummary();
    DatasetRecord datasetRecord = createDatasetRecord();
    when(userDao.findUserById(datasetRecord.createUser.getUserId())).thenReturn(
        datasetRecord.createUser);
    when(userDao.findUserById(datasetRecord.updateUser.getUserId())).thenReturn(
        datasetRecord.updateUser);
    when(dacDAO.findById(any())).thenReturn(datasetRecord.dac);
    when(ontologyService.translateDataUseSummary(any())).thenReturn(dataUseSummary);
    when(dataAccessRequestDAO.findApprovedDARsByDatasetId(any())).thenReturn(List.of(dar1, dar2));
    initService();
    DatasetTerm term = service.toDatasetTerm(datasetRecord.dataset);

    assertEquals(datasetRecord.dataset.getDataSetId(), term.getDatasetId());
    assertEquals(datasetRecord.dataset.getDatasetIdentifier(), term.getDatasetIdentifier());
    assertEquals(datasetRecord.dataset.getDeletable(), term.getDeletable());
    assertEquals(datasetRecord.dataset.getName(), term.getDatasetName());
    assertEquals(datasetRecord.dataset.getDatasetName(), term.getDatasetName());

    Optional<DatasetProperty> countProp = datasetRecord.dataset.getProperties().stream()
        .filter(p -> p.getSchemaProperty().equals("numberOfParticipants")).findFirst();
    assertTrue(countProp.isPresent());
    assertEquals(Integer.valueOf(countProp.get().getPropertyValue().toString()),
        term.getParticipantCount());
    assertEquals(dataUseSummary, term.getDataUse());
    Optional<DatasetProperty> locationProp = datasetRecord.dataset.getProperties().stream()
        .filter(p -> p.getSchemaProperty().equals("dataLocation")).findFirst();
    assertTrue(locationProp.isPresent());
    assertEquals(locationProp.get().getPropertyValue().toString(), term.getDataLocation());
    Optional<DatasetProperty> urlProp = datasetRecord.dataset.getProperties().stream()
        .filter(p -> p.getSchemaProperty().equals("url")).findFirst();
    assertTrue(urlProp.isPresent());
    assertEquals(urlProp.get().getPropertyValue().toString(), term.getUrl());
    assertEquals(datasetRecord.dataset.getDacApproval(), term.getDacApproval());
    Optional<DatasetProperty> accessManagementProp = datasetRecord.dataset.getProperties().stream()
        .filter(p -> p.getSchemaProperty().equals("accessManagement")).findFirst();
    assertTrue(accessManagementProp.isPresent());
    assertEquals(accessManagementProp.get().getPropertyValue().toString(),
        term.getAccessManagement());
    assertEquals(approvedUserIds, term.getApprovedUserIds());
  }

  @Test
  void testToDatasetTerm_DacInfo() {
    DatasetRecord datasetRecord = createDatasetRecord();
    when(dacDAO.findById(any())).thenReturn(datasetRecord.dac);
    when(userDao.findUserById(datasetRecord.createUser.getUserId())).thenReturn(
        datasetRecord.createUser);
    initService();
    DatasetTerm term = service.toDatasetTerm(datasetRecord.dataset);

    assertEquals(datasetRecord.dataset.getDacApproval(), term.getDacApproval());
    assertEquals(datasetRecord.dac.getDacId(), term.getDacId());
    assertEquals(datasetRecord.dac.getDacId(), term.getDac().dacId());
    assertEquals(datasetRecord.dac.getName(), term.getDac().dacName());
  }

  @Test
  void testToDatasetTerm_StringNumberOfParticipants() {
    User user = createUser(1, 100);
    User updateUser = createUser(101, 200);
    Dac dac = createDac();
    Study study = createStudy(user);
    study.setProperties(Set.of(
        createStudyProperty("phenotypeIndication", PropertyType.String),
        createStudyProperty("species", PropertyType.String),
        createStudyProperty("dataCustodianEmail", PropertyType.Json)
    ));
    Dataset dataset = createDataset(user, updateUser, new DataUse(), dac);
    dataset.setProperties(Set.of(
        createDatasetProperty("numberOfParticipants", PropertyType.String, "# of participants"),
        createDatasetProperty("url", PropertyType.String, "url")
    ));
    dataset.setStudy(study);
    DatasetRecord record = new DatasetRecord(user, updateUser, dac, dataset, study);
    when(dacDAO.findById(any())).thenReturn(dac);
    when(userDao.findUserById(user.getUserId())).thenReturn(user);
    when(dacDAO.findById(any())).thenReturn(record.dac);
    when(userDao.findUserById(record.createUser.getUserId())).thenReturn(record.createUser);
    initService();
    assertDoesNotThrow(() -> service.toDatasetTerm(dataset));
  }

  @Test
  void testToDatasetTermIncomplete() {
    Dataset dataset = new Dataset();
    dataset.setDataSetId(100);
    dataset.setAlias(10);
    dataset.setDatasetIdentifier();
    dataset.setProperties(Set.of());

    when(dataAccessRequestDAO.findApprovedDARsByDatasetId(any())).thenReturn(
        List.of());

    initService();
    DatasetTerm term = service.toDatasetTerm(dataset);

    assertEquals(dataset.getDataSetId(), term.getDatasetId());
    assertEquals(dataset.getDatasetIdentifier(), term.getDatasetIdentifier());
  }

  @Test
  void testToDatasetTermNullDatasetProps() {
    Dataset dataset = new Dataset();
    initService();
    assertDoesNotThrow(() -> service.toDatasetTerm(dataset));
  }

  @Test
  void testToDatasetTermNullStudyProps() {
    Dataset dataset = new Dataset();
    Study study = new Study();
    study.setName(RandomStringUtils.randomAlphabetic(10));
    study.setDescription(RandomStringUtils.randomAlphabetic(20));
    study.setStudyId(RandomUtils.nextInt(1, 100));
    dataset.setStudy(study);
    initService();
    assertDoesNotThrow(() -> service.toDatasetTerm(dataset));
  }

  @Captor
  ArgumentCaptor<Request> request;

  @Test
  void testIndexDatasets() throws IOException {
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
  void testSearchDatasets() throws IOException {
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
  void testValidateQuery() throws IOException {
    String query = "{ \"query\": { \"query_string\": { \"query\": \"(GRU) AND (HMB)\" } } }";

    mockElasticSearchResponse(200, "{\"valid\":true}");

    initService();
    assertTrue(service.validateQuery(query));
  }

  @Test
  void testValidateQueryWithFromAndSize() throws IOException {
    String query = "{ \"from\": 0, \"size\": 100, \"query\": { \"query_string\": { \"query\": \"(GRU) AND (HMB)\" } } }";

    mockElasticSearchResponse(200, "{\"valid\":true}");

    initService();
    assertTrue(service.validateQuery(query));
  }

  @Test
  void testValidateQueryEmpty() throws IOException {
    String query = "{}";

    Response response = mock(Response.class);
    String reasonPhrase = fromStatusCode(400).getReasonPhrase();
    BasicStatusLine status = new BasicStatusLine(HttpVersion.HTTP_1_1, 400, reasonPhrase);
    when(esClient.performRequest(any())).thenReturn(response);
    when(response.getStatusLine()).thenReturn(status);

    initService();
    assertThrows(IOException.class, () -> service.validateQuery(query));
  }

  @Test
  void testValidateQueryInvalid() throws IOException {
    String query = "{ \"bad\": [\"and\", \"invalid\"] }";

    mockElasticSearchResponse(200, "{\"valid\":false}");

    initService();
    assertFalse(service.validateQuery(query));
  }

  @Test
  void testIndexDatasetIds() throws Exception {
    Gson gson = GsonUtil.buildGson();
    Dataset dataset = new Dataset();
    dataset.setDataSetId(RandomUtils.nextInt(10, 100));
    String esResponseBody = """
          {
            "took": 2,
            "errors": false,
            "items": [
              {
                "index": {
                  "_index": "dataset",
                  "_type": "dataset",
                  "_id": "%d",
                  "_version": 3,
                  "result": "updated",
                  "_shards": {
                    "total": 2,
                    "successful": 1,
                    "failed": 0
                  },
                  "created": false,
                  "status": 200
                }
              }
            ]
          }
        """;

    initService();

    when(datasetDAO.findDatasetById(dataset.getDataSetId())).thenReturn(dataset);
    mockESClientResponse(200, esResponseBody.formatted(dataset.getDataSetId()));
    StreamingOutput output = service.indexDatasetIds(List.of(dataset.getDataSetId()));
    var baos = new ByteArrayOutputStream();
    output.write(baos);
    var entityString = baos.toString();
    Type listOfEsResponses = new TypeToken<List<JsonObject>>() {
    }.getType();
    List<JsonObject> responseList = gson.fromJson(entityString, listOfEsResponses);
    assertEquals(1, responseList.size());
    JsonArray items = responseList.get(0).getAsJsonArray("items");
    assertEquals(1, items.size());
    assertEquals(
        dataset.getDataSetId(),
        items.get(0)
            .getAsJsonObject()
            .getAsJsonObject("index")
            .get("_id")
            .getAsInt());
  }

  @Test
  void testIndexDatasetIdsErrors() throws Exception {
    Gson gson = GsonUtil.buildGson();
    Dataset dataset = new Dataset();
    dataset.setDataSetId(RandomUtils.nextInt(10, 100));
    when(datasetDAO.findDatasetById(dataset.getDataSetId())).thenReturn(dataset);
    mockESClientResponse(500, "error condition");
    initService();

    StreamingOutput output = service.indexDatasetIds(List.of(dataset.getDataSetId()));
    var baos = new ByteArrayOutputStream();
    output.write(baos);
    JsonArray jsonArray = gson.fromJson(baos.toString(), JsonArray.class);
    assertEquals(0, jsonArray.size());
  }

  // Helper method to mock an ElasticSearch Client response
  private void mockESClientResponse(int status, String body) throws Exception {
    var esClientResponse = mock(org.elasticsearch.client.Response.class);
    var statusLine = mock(StatusLine.class);
    when(esClientResponse.getStatusLine()).thenReturn(statusLine);
    when(statusLine.getStatusCode()).thenReturn(status);
    var httpEntity = mock(HttpEntity.class);
    if (status == 200) {
      when(httpEntity.getContent())
          .thenReturn(new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));
      when(esClientResponse.getEntity()).thenReturn(httpEntity);
    }
    when(esClient.performRequest(any())).thenReturn(esClientResponse);
  }


  @Test
  void testIndexStudyWithDatasets() {
    Study study = new Study();
    study.setStudyId(1);
    Dataset d = new Dataset();
    d.setDataSetId(1);
    study.addDatasetId(d.getDataSetId());
    when(studyDAO.findStudyById(any())).thenReturn(study);
    when(datasetDAO.findDatasetsByIdList(any())).thenReturn(List.of(d));

    initService();
    assertDoesNotThrow(() -> service.indexStudy(1));
  }

  @Test
  void testIndexStudyWithNoDatasets() {
    Study study = new Study();
    study.setStudyId(1);
    when(studyDAO.findStudyById(any())).thenReturn(study);

    initService();
    assertDoesNotThrow(() -> service.indexStudy(1));
  }
}
