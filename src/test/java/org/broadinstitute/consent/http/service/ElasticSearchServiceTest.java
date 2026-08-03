package org.broadinstitute.consent.http.service;

import static jakarta.ws.rs.core.Response.Status.fromStatusCode;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.assets;
import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.data;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.google.api.client.http.HttpStatusCodes;
import com.google.gson.JsonArray;
import jakarta.ws.rs.core.StreamingOutput;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpVersion;
import org.apache.http.StatusLine;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.nio.entity.NStringEntity;
import org.broadinstitute.consent.http.AbstractTestHelper;
import org.broadinstitute.consent.http.configurations.ElasticSearchConfiguration;
import org.broadinstitute.consent.http.db.DacDAO;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.InstitutionDAO;
import org.broadinstitute.consent.http.db.StudyDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.enumeration.PropertyType;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetProperty;
import org.broadinstitute.consent.http.models.FileStorageObject;
import org.broadinstitute.consent.http.models.Institution;
import org.broadinstitute.consent.http.models.LibraryCard;
import org.broadinstitute.consent.http.models.Study;
import org.broadinstitute.consent.http.models.StudyProperty;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.elastic_search.DatasetTerm;
import org.broadinstitute.consent.http.models.ontology.DataUseSummary;
import org.broadinstitute.consent.http.models.ontology.DataUseTerm;
import org.broadinstitute.consent.http.service.dao.DatasetServiceDAO;
import org.broadinstitute.consent.http.util.TestAppender;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
class ElasticSearchServiceTest extends AbstractTestHelper {

  private ElasticSearchService service;

  @Mock private RestClient esClient;

  @Mock private OntologyService ontologyService;

  @Mock private ElasticSearchConfiguration esConfig;

  @Mock private DacDAO dacDAO;

  @Mock private UserDAO userDao;

  @Mock private InstitutionDAO institutionDAO;

  @Mock private DatasetDAO datasetDAO;

  @Mock private DatasetServiceDAO datasetServiceDAO;

  @Mock private StudyDAO studyDAO;

  @Mock private Jdbi jdbi;

  @BeforeEach
  void initService() {
    when(jdbi.onDemand(DacDAO.class)).thenReturn(dacDAO);
    when(jdbi.onDemand(UserDAO.class)).thenReturn(userDao);
    when(jdbi.onDemand(InstitutionDAO.class)).thenReturn(institutionDAO);
    when(jdbi.onDemand(DatasetDAO.class)).thenReturn(datasetDAO);
    when(jdbi.onDemand(StudyDAO.class)).thenReturn(studyDAO);
    service =
        new ElasticSearchService(jdbi, datasetServiceDAO, esClient, esConfig, ontologyService);
    service.setIndexKey("_index");
  }

  private void mockElasticSearchResponse(String body) throws IOException {
    Response response = mock(Response.class);
    String reasonPhrase = fromStatusCode(200).getReasonPhrase();
    BasicStatusLine status = new BasicStatusLine(HttpVersion.HTTP_1_1, 200, reasonPhrase);
    HttpEntity entity = new NStringEntity(body, ContentType.APPLICATION_JSON);

    when(esClient.performRequest(any())).thenReturn(response);
    when(response.getStatusLine()).thenReturn(status);
    when(response.getEntity()).thenReturn(entity);
  }

  private Institution createInstitution() {
    Institution institution = new Institution();
    institution.setId(randomInt(1, 1000));
    return institution;
  }

  private User createUser(int start, int max) {
    User user = new User();
    user.setUserId(randomInt(start, max));
    user.setDisplayName(randomAlphabetic(10));
    user.setEmail(randomAlphabetic(10));
    Institution i = createInstitution();
    user.setInstitution(i);
    user.setInstitutionId(i.getId());
    return user;
  }

  private Dataset createDataset(User user, User updateUser, DataUse dataUse, Dac dac) {
    Dataset dataset = new Dataset();
    dataset.setDatasetId(randomInt(1, 100));
    dataset.setAlias(dataset.getDatasetId());
    dataset.setDatasetIdentifier();
    dataset.setDeletable(true);
    dataset.setName(randomAlphabetic(10));
    dataset.setDatasetName(dataset.getName());
    dataset.setDacId(dac.getDacId());
    dataset.setDacApproval(true);
    dataset.setDataUse(dataUse);
    dataset.setCreateUser(user);
    dataset.setUpdateUserId(updateUser.getUserId());
    dataset.setCreateUserId(user.getUserId());
    dataset.setNihInstitutionalCertificationFile(new FileStorageObject());
    return dataset;
  }

  private Dac createDac() {
    Dac dac = new Dac();
    dac.setDacId(randomInt(1, 100));
    dac.setName(randomAlphabetic(10));
    return dac;
  }

  private Study createStudy(User user) {
    Study study = new Study();
    study.setName(randomAlphabetic(10));
    study.setDescription(randomAlphabetic(20));
    study.setStudyId(randomInt(1, 100));
    study.setPiName(randomAlphabetic(10));
    study.setDataTypes(List.of(randomAlphabetic(10)));
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
        val.add(randomAlphabetic(10));
        prop.setValue(val);
      }
      case Number -> prop.setValue(randomInt(1, 100));
      default -> prop.setValue(randomAlphabetic(10));
    }
    return prop;
  }

  private DatasetProperty createDatasetProperty(
      String schemaProp, PropertyType type, String propertyName) {
    DatasetProperty prop = new DatasetProperty();
    prop.setSchemaProperty(schemaProp);
    prop.setPropertyType(type);
    prop.setPropertyName(propertyName);
    switch (type) {
      case Boolean -> prop.setPropertyValue(true);
      case Number -> prop.setPropertyValue(randomInt(1, 100));
      default -> prop.setPropertyValue(randomAlphabetic(10));
    }
    return prop;
  }

  private DataUseSummary createDataUseSummary() {
    DataUseSummary dataUseSummary = new DataUseSummary();
    dataUseSummary.setPrimary(List.of(new DataUseTerm("DS", "Description")));
    dataUseSummary.setPrimary(List.of(new DataUseTerm("NMDS", "Description")));
    return dataUseSummary;
  }

  /** Private container record to consolidate dataset and associated object creation */
  private record DatasetRecord(
      User createUser, User updateUser, Dac dac, Dataset dataset, Study study) {}

  private DatasetRecord createDatasetRecord() {
    User user = createUser(1, 100);
    User updateUser = createUser(101, 200);
    Dac dac = createDac();
    Study study = createStudy(user);
    study.addProperties(
        createStudyProperty("dbGaPPhsID", PropertyType.String),
        createStudyProperty("phenotypeIndication", PropertyType.String),
        createStudyProperty("species", PropertyType.String),
        createStudyProperty("dataCustodianEmail", PropertyType.Json),
        createStudyProperty("throughBioId", PropertyType.String),
        createStudyProperty("externalIdentifier", PropertyType.String),
        createStudyProperty("externalIdentifierType", PropertyType.String));
    Dataset dataset = createDataset(user, updateUser, new DataUse(), dac);
    DatasetProperty accessManagement =
        createDatasetProperty("accessManagement", PropertyType.String, "accessManagement");
    accessManagement.setPropertyValue("open");
    dataset.setProperties(
        Set.of(
            accessManagement,
            createDatasetProperty("numberOfParticipants", PropertyType.Number, "# of participants"),
            createDatasetProperty("url", PropertyType.String, "url"),
            createDatasetProperty("dataLocation", PropertyType.String, "dataLocation")));
    dataset.setStudy(study);
    dataset.setStudyId(study.getStudyId());
    study.addDatasetId(dataset.getDatasetId());
    return new DatasetRecord(user, updateUser, dac, dataset, study);
  }

  @Test
  void testIndexStudy() throws IOException {
    DatasetRecord datasetRecord = createDatasetRecord();
    when(userDao.findUserById(datasetRecord.createUser.getUserId()))
        .thenReturn(datasetRecord.createUser);
    when(datasetDAO.findDatasetsByIdList(datasetRecord.study.getDatasetIds()))
        .thenReturn(List.of(datasetRecord.dataset));
    when(studyDAO.findStudyById(datasetRecord.study.getStudyId())).thenReturn(datasetRecord.study);
    org.elasticsearch.client.Response mockResponse = mock();
    when(esClient.performRequest(any())).thenReturn(mockResponse);
    when(mockResponse.getEntity()).thenReturn(new StringEntity("response body"));
    StatusLine statusLine = mock();
    when(mockResponse.getStatusLine()).thenReturn(statusLine);
    when(statusLine.getStatusCode()).thenReturn(200);

    ElasticSearchService elasticSearchSpy = spy(service);

    Logger logger = (Logger) LoggerFactory.getLogger(ElasticSearchService.class);
    Level previousLevel = logger.getLevel();
    logger.setLevel(Level.INFO);
    TestAppender appender = new TestAppender();
    appender.start();
    logger.addAppender(appender);
    try (jakarta.ws.rs.core.Response response =
        elasticSearchSpy.indexStudy(datasetRecord.study.getStudyId())) {
      // Ensure that the synchronous method was called with the expected parameters
      verify(elasticSearchSpy, timeout(1000))
          .synchronizeDatasetListInESIndex(List.of(datasetRecord.dataset));
      assertEquals(200, response.getStatus());
      List<ILoggingEvent> events = appender.getLoggedEvents();
      assertTrue(
          events.stream()
              .anyMatch(
                  event ->
                      event.getLevel() == Level.INFO
                          && event
                              .getFormattedMessage()
                              .equals(
                                  "Loading datasets for study: %d"
                                      .formatted(datasetRecord.study.getStudyId()))));
      assertTrue(
          events.stream()
              .anyMatch(
                  event ->
                      event.getLevel() == Level.INFO
                          && event
                              .getFormattedMessage()
                              .equals(
                                  "Loaded 1 datasets for study: %d"
                                      .formatted(datasetRecord.study.getStudyId()))));
    } finally {
      logger.detachAppender(appender);
      appender.stop();
      logger.setLevel(previousLevel);
    }
  }

  @Test
  void testIndexStudyLogsWarningOnFailure() {
    Study study = new Study();
    study.setStudyId(1);
    study.addDatasetId(1);
    Dataset dataset = new Dataset();
    dataset.setDatasetId(1);
    when(studyDAO.findStudyById(1)).thenReturn(study);
    when(datasetDAO.findDatasetsByIdList(study.getDatasetIds())).thenReturn(List.of(dataset));

    ElasticSearchService elasticSearchSpy = spy(service);
    RuntimeException failure = new RuntimeException("synthetic indexing failure");
    doThrow(failure).when(elasticSearchSpy).synchronizeDatasetListInESIndex(List.of(dataset));

    Logger logger = (Logger) LoggerFactory.getLogger(ElasticSearchService.class);
    TestAppender appender = new TestAppender();
    appender.start();
    logger.addAppender(appender);
    try (jakarta.ws.rs.core.Response response = elasticSearchSpy.indexStudy(study.getStudyId())) {
      assertEquals(500, response.getStatus());
      assertTrue(
          appender.getLoggedEvents().stream()
              .anyMatch(
                  event ->
                      event.getLevel() == Level.WARN
                          && event
                              .getFormattedMessage()
                              .equals("Exception, unable to index study: 1")
                          && event.getThrowableProxy() != null));
    } finally {
      logger.detachAppender(appender);
      appender.stop();
    }
  }

  @Test
  void testToDatasetTerm_UserInfo() {
    DatasetRecord datasetRecord = createDatasetRecord();
    when(userDao.findUserById(datasetRecord.createUser.getUserId()))
        .thenReturn(datasetRecord.createUser);
    when(userDao.findUserById(datasetRecord.updateUser.getUserId()))
        .thenReturn(datasetRecord.updateUser);
    when(institutionDAO.findInstitutionById(datasetRecord.createUser.getInstitutionId()))
        .thenReturn(datasetRecord.createUser.getInstitution());
    when(institutionDAO.findInstitutionById(datasetRecord.updateUser.getInstitutionId()))
        .thenReturn(datasetRecord.updateUser.getInstitution());
    when(dacDAO.findById(any())).thenReturn(datasetRecord.dac);

    DatasetTerm term = service.toDatasetTerm(datasetRecord.dataset);
    assertEquals(datasetRecord.createUser.getUserId(), term.getCreateUserId());
    assertEquals(datasetRecord.createUser.getDisplayName(), term.getCreateUserDisplayName());
    assertEquals(datasetRecord.createUser.getUserId(), term.getSubmitter().userId());
    assertEquals(datasetRecord.createUser.getDisplayName(), term.getSubmitter().displayName());
    assertEquals(
        datasetRecord.createUser.getInstitutionId(), term.getSubmitter().institution().id());
    assertEquals(
        datasetRecord.createUser.getInstitution().getName(),
        term.getSubmitter().institution().name());
    assertEquals(datasetRecord.updateUser.getUserId(), term.getUpdateUser().userId());
    assertEquals(datasetRecord.updateUser.getDisplayName(), term.getUpdateUser().displayName());
    assertEquals(
        datasetRecord.updateUser.getInstitutionId(), term.getUpdateUser().institution().id());
    assertEquals(
        datasetRecord.updateUser.getInstitution().getName(),
        term.getUpdateUser().institution().name());
  }

  @Test
  void testToDatasetTerm_StudyInfo() {
    DatasetRecord datasetRecord = createDatasetRecord();
    when(userDao.findUserById(datasetRecord.createUser.getUserId()))
        .thenReturn(datasetRecord.createUser);
    when(userDao.findUserById(datasetRecord.updateUser.getUserId()))
        .thenReturn(datasetRecord.updateUser);
    when(dacDAO.findById(any())).thenReturn(datasetRecord.dac);

    DatasetTerm term = service.toDatasetTerm(datasetRecord.dataset);
    assertEquals(datasetRecord.study.getDescription(), term.getStudy().getDescription());
    assertEquals(datasetRecord.study.getName(), term.getStudy().getStudyName());
    assertEquals(datasetRecord.study.getStudyId(), term.getStudy().getStudyId());
    Optional<StudyProperty> phsIdProp =
        datasetRecord.study.getProperties().stream()
            .filter(p -> p.getKey().equals("dbGaPPhsID"))
            .findFirst();
    assertTrue(phsIdProp.isPresent());
    assertEquals(phsIdProp.get().getValue().toString(), term.getStudy().getPhsId());
    Optional<StudyProperty> phenoProp =
        datasetRecord.study.getProperties().stream()
            .filter(p -> p.getKey().equals("phenotypeIndication"))
            .findFirst();
    assertTrue(phenoProp.isPresent());
    assertEquals(phenoProp.get().getValue().toString(), term.getStudy().getPhenotype());
    Optional<StudyProperty> speciesProp =
        datasetRecord.study.getProperties().stream()
            .filter(p -> p.getKey().equals("species"))
            .findFirst();
    assertTrue(speciesProp.isPresent());
    assertEquals(speciesProp.get().getValue().toString(), term.getStudy().getSpecies());
    assertEquals(datasetRecord.study.getPiName(), term.getStudy().getPiName());
    assertEquals(datasetRecord.study.getCreateUserEmail(), term.getStudy().getDataSubmitterEmail());
    assertEquals(datasetRecord.study.getCreateUserId(), term.getStudy().getDataSubmitterId());
    Optional<StudyProperty> custodianProp =
        datasetRecord.study.getProperties().stream()
            .filter(p -> p.getKey().equals("dataCustodianEmail"))
            .findFirst();
    assertTrue(custodianProp.isPresent());
    String termCustodians =
        GsonUtil.getInstance().toJson(term.getStudy().getDataCustodianEmail(), ArrayList.class);
    assertEquals(custodianProp.get().getValue().toString(), termCustodians);
    assertEquals(datasetRecord.study.getPublicVisibility(), term.getStudy().getPublicVisibility());
    assertEquals(datasetRecord.study.getDataTypes(), term.getStudy().getDataTypes());
    Optional<StudyProperty> throughBioIdProp =
        datasetRecord.study.getProperties().stream()
            .filter(p -> p.getKey().equals("throughBioId"))
            .findFirst();
    assertTrue(throughBioIdProp.isPresent());
    Optional<StudyProperty> externalIdentifierProp =
        datasetRecord.study.getProperties().stream()
            .filter(p -> p.getKey().equals("externalIdentifier"))
            .findFirst();
    assertTrue(externalIdentifierProp.isPresent());
    assertEquals(
        externalIdentifierProp.get().getValue().toString(),
        term.getStudy().getExternalIdentifier());
    Optional<StudyProperty> externalIdentifierTypeProp =
        datasetRecord.study.getProperties().stream()
            .filter(p -> p.getKey().equals("externalIdentifierType"))
            .findFirst();
    assertTrue(externalIdentifierTypeProp.isPresent());
    assertEquals(
        externalIdentifierTypeProp.get().getValue().toString(),
        term.getStudy().getExternalIdentifierType());
  }

  @ParameterizedTest
  @ValueSource(strings = {assets, data})
  void testToDatasetTerm_JsonBlobs(String propKey) {
    DatasetRecord datasetRecord = createDatasetRecord();
    Map<String, Object> refMap = Map.of("key", List.of("value1", "value2"));
    String refJson = GsonUtil.getInstance().toJson(refMap);
    StudyProperty jsonBlob = new StudyProperty();
    jsonBlob.setStudyId(datasetRecord.study.getStudyId());
    jsonBlob.setKey(propKey);
    jsonBlob.setType(PropertyType.Json);
    jsonBlob.setValue(GsonUtil.getInstance().fromJson(refJson, Object.class));
    datasetRecord.study.addProperty(jsonBlob);

    when(userDao.findUserById(datasetRecord.createUser.getUserId()))
        .thenReturn(datasetRecord.createUser);
    when(userDao.findUserById(datasetRecord.updateUser.getUserId()))
        .thenReturn(datasetRecord.updateUser);
    when(dacDAO.findById(any())).thenReturn(datasetRecord.dac);

    DatasetTerm term = service.toDatasetTerm(datasetRecord.dataset);
    switch (propKey) {
      case assets:
        assertEquals(refMap, term.getStudy().getAssets());
        return;
      case data:
        assertEquals(refMap, term.getStudy().getData());
        return;
      default:
    }
  }

  @Test
  void testToDatasetTerm_DatasetInfo() {
    DataAccessRequest dar1 = new DataAccessRequest();
    dar1.setUserId(1);
    DataAccessRequest dar2 = new DataAccessRequest();
    dar2.setUserId(2);
    DataUseSummary dataUseSummary = createDataUseSummary();
    DatasetRecord datasetRecord = createDatasetRecord();
    when(userDao.findUserById(datasetRecord.createUser.getUserId()))
        .thenReturn(datasetRecord.createUser);
    when(userDao.findUserById(datasetRecord.updateUser.getUserId()))
        .thenReturn(datasetRecord.updateUser);
    LibraryCard card1 = new LibraryCard();
    card1.setUserId(dar1.getUserId());
    LibraryCard card2 = new LibraryCard();
    card2.setUserId(dar2.getUserId());
    when(dacDAO.findById(any())).thenReturn(datasetRecord.dac);
    when(ontologyService.translateDataUseSummary(any())).thenReturn(dataUseSummary);
    DatasetTerm term = service.toDatasetTerm(datasetRecord.dataset);

    assertEquals(datasetRecord.dataset.getDatasetId(), term.getDatasetId());
    assertEquals(datasetRecord.dataset.getDatasetIdentifier(), term.getDatasetIdentifier());
    assertEquals(datasetRecord.dataset.getDeletable(), term.getDeletable());
    assertEquals(datasetRecord.dataset.getName(), term.getDatasetName());
    assertEquals(datasetRecord.dataset.getDatasetName(), term.getDatasetName());

    Optional<DatasetProperty> countProp =
        datasetRecord.dataset.getProperties().stream()
            .filter(p -> p.getSchemaProperty().equals("numberOfParticipants"))
            .findFirst();
    assertTrue(countProp.isPresent());
    assertEquals(
        Integer.valueOf(countProp.get().getPropertyValue().toString()), term.getParticipantCount());
    assertEquals(dataUseSummary, term.getDataUse());
    Optional<DatasetProperty> locationProp =
        datasetRecord.dataset.getProperties().stream()
            .filter(p -> p.getSchemaProperty().equals("dataLocation"))
            .findFirst();
    assertTrue(locationProp.isPresent());
    assertEquals(locationProp.get().getPropertyValue().toString(), term.getDataLocation());
    Optional<DatasetProperty> urlProp =
        datasetRecord.dataset.getProperties().stream()
            .filter(p -> p.getSchemaProperty().equals("url"))
            .findFirst();
    assertTrue(urlProp.isPresent());
    assertEquals(urlProp.get().getPropertyValue().toString(), term.getUrl());
    assertEquals(datasetRecord.dataset.getDacApproval(), term.getDacApproval());
    Optional<DatasetProperty> accessManagementProp =
        datasetRecord.dataset.getProperties().stream()
            .filter(p -> p.getSchemaProperty().equals("accessManagement"))
            .findFirst();
    assertTrue(accessManagementProp.isPresent());
    assertEquals(
        accessManagementProp.get().getPropertyValue().toString(), term.getAccessManagement());
  }

  @Test
  void testToDatasetTerm_Data() {
    Dataset dataset = new Dataset();
    dataset.setDatasetId(1);
    Set<DatasetProperty> datasetProperties = new HashSet<>();
    Map<String, Object> refMap = Map.of("key", List.of("value1", "value2"));
    DatasetProperty newProperty =
        new DatasetProperty(
            99,
            dataset.getDatasetId(),
            99,
            data,
            GsonUtil.getInstance().toJson(refMap),
            PropertyType.Json,
            new Date());
    newProperty.setPropertyName(data);
    newProperty.setSchemaProperty(data);
    datasetProperties.add(newProperty);
    dataset.setProperties(datasetProperties);

    DatasetTerm term = service.toDatasetTerm(dataset);
    Optional<DatasetProperty> dataProp =
        dataset.getProperties().stream()
            .filter(p -> p.getSchemaProperty().equals(data))
            .findFirst();
    assertTrue(dataProp.isPresent());
    assertEquals(refMap, term.getData());
  }

  @Test
  void testToDatasetTermUsesLegacyAccessManagementProperty() {
    Dataset dataset = new Dataset();
    dataset.setDatasetId(1);
    DatasetProperty property = new DatasetProperty();
    property.setSchemaProperty(Dataset.LEGACY_ACCESS_MANAGEMENT_SCHEMA_PROPERTY);
    property.setPropertyName("Access Management");
    property.setPropertyValue("open");
    dataset.setProperties(Set.of(property));

    DatasetTerm term = service.toDatasetTerm(dataset);

    assertEquals("open", term.getAccessManagement());
  }

  @Test
  void testToDatasetTerm_DacInfo() {
    DatasetRecord datasetRecord = createDatasetRecord();
    when(dacDAO.findById(any())).thenReturn(datasetRecord.dac);
    when(userDao.findUserById(datasetRecord.createUser.getUserId()))
        .thenReturn(datasetRecord.createUser);
    DatasetTerm term = service.toDatasetTerm(datasetRecord.dataset);

    assertEquals(datasetRecord.dataset.getDacApproval(), term.getDacApproval());
    assertEquals(datasetRecord.dac.getDacId(), term.getDacId());
    assertEquals(datasetRecord.dac.getDacId(), term.getDac().dacId());
    assertEquals(datasetRecord.dac.getName(), term.getDac().dacName());
  }

  @Test
  void testToDatasetTerm_NIHInstitutionalCertification() {
    DatasetRecord datasetRecord = createDatasetRecord();
    when(dacDAO.findById(any())).thenReturn(datasetRecord.dac);
    when(userDao.findUserById(datasetRecord.createUser.getUserId()))
        .thenReturn(datasetRecord.createUser);
    DatasetTerm term = service.toDatasetTerm(datasetRecord.dataset);
    assertEquals(
        datasetRecord.dataset.getNihInstitutionalCertificationFile() != null,
        term.getHasInstitutionCertification());
  }

  @Test
  void testToDatasetTerm_Missing_NIHInstitutionalCertification() {
    DatasetRecord datasetRecord = createDatasetRecord();
    when(dacDAO.findById(any())).thenReturn(datasetRecord.dac);
    when(userDao.findUserById(datasetRecord.createUser.getUserId()))
        .thenReturn(datasetRecord.createUser);
    datasetRecord.dataset.setNihInstitutionalCertificationFile(null);
    DatasetTerm term = service.toDatasetTerm(datasetRecord.dataset);
    assertNull(term.getHasInstitutionCertification());
  }

  @Test
  void testToDatasetTerm_StringNumberOfParticipants() {
    User user = createUser(1, 100);
    User updateUser = createUser(101, 200);
    Dac dac = createDac();
    Study study = createStudy(user);
    study.addProperties(
        createStudyProperty("phenotypeIndication", PropertyType.String),
        createStudyProperty("species", PropertyType.String),
        createStudyProperty("dataCustodianEmail", PropertyType.Json));
    Dataset dataset = createDataset(user, updateUser, new DataUse(), dac);
    dataset.setProperties(
        Set.of(
            createDatasetProperty("numberOfParticipants", PropertyType.String, "# of participants"),
            createDatasetProperty("url", PropertyType.String, "url")));
    dataset.setStudy(study);
    DatasetRecord datasetRecord = new DatasetRecord(user, updateUser, dac, dataset, study);
    when(dacDAO.findById(any())).thenReturn(dac);
    when(userDao.findUserById(user.getUserId())).thenReturn(user);
    when(dacDAO.findById(any())).thenReturn(datasetRecord.dac);
    when(userDao.findUserById(datasetRecord.createUser.getUserId()))
        .thenReturn(datasetRecord.createUser);
    assertDoesNotThrow(() -> service.toDatasetTerm(dataset));
  }

  @Test
  void testToDatasetTermIncomplete() {
    Dataset dataset = new Dataset();
    dataset.setDatasetId(100);
    dataset.setAlias(10);
    dataset.setDatasetIdentifier();
    dataset.setProperties(Set.of());

    DatasetTerm term = service.toDatasetTerm(dataset);

    assertEquals(dataset.getDatasetId(), term.getDatasetId());
    assertEquals(dataset.getDatasetIdentifier(), term.getDatasetIdentifier());
  }

  @Test
  void testToDatasetTerm_RequestLocation() {
    User user = createUser(1, 100);
    User updateUser = createUser(101, 200);
    Dac dac = createDac();
    Study study = createStudy(user);
    Dataset dataset = createDataset(user, updateUser, new DataUse(), dac);
    dataset.setProperties(
        Set.of(createDatasetProperty("requestLocation", PropertyType.String, "Request Location")));
    dataset.setStudy(study);
    when(userDao.findUserById(user.getUserId())).thenReturn(user);

    DatasetTerm term = service.toDatasetTerm(dataset);

    Optional<DatasetProperty> requestLocationProp =
        dataset.getProperties().stream()
            .filter(p -> p.getSchemaProperty().equals("requestLocation"))
            .findFirst();
    assertTrue(requestLocationProp.isPresent());
    assertEquals(
        requestLocationProp.get().getPropertyValue().toString(), term.getRequestLocation());
  }

  @Test
  void testToDatasetTerm_RequestLocation_Missing() {
    User user = createUser(1, 100);
    User updateUser = createUser(101, 200);
    Dac dac = createDac();
    Dataset dataset = createDataset(user, updateUser, new DataUse(), dac);
    // No requestLocation property
    dataset.setProperties(Set.of(createDatasetProperty("url", PropertyType.String, "url")));
    when(userDao.findUserById(user.getUserId())).thenReturn(user);

    DatasetTerm term = service.toDatasetTerm(dataset);

    assertNull(term.getRequestLocation());
  }

  @Test
  void testToDatasetTermNullDatasetProps() {
    Dataset dataset = new Dataset();
    assertDoesNotThrow(() -> service.toDatasetTerm(dataset));
  }

  @Test
  void testToDatasetTermNullStudyProps() {
    Dataset dataset = new Dataset();
    Study study = new Study();
    study.setName(randomAlphabetic(10));
    study.setDescription(randomAlphabetic(20));
    study.setStudyId(randomInt(1, 100));
    dataset.setStudy(study);
    assertDoesNotThrow(() -> service.toDatasetTerm(dataset));
  }

  @Captor ArgumentCaptor<Request> request;

  @Test
  void testIndexDatasetTerms() throws IOException {
    DatasetTerm term1 = new DatasetTerm();
    term1.setDatasetId(1);
    DatasetTerm term2 = new DatasetTerm();
    term2.setDatasetId(2);
    String datasetIndexName = randomAlphabetic(10);

    when(esConfig.getDatasetIndexName()).thenReturn(datasetIndexName);
    mockElasticSearchResponse("");

    try (var _ = service.indexDatasetTerms(List.of(term1, term2))) {
      verify(esClient).performRequest(request.capture());
      Request capturedRequest = request.getValue();
      assertEquals("PUT", capturedRequest.getMethod());
      assertEquals(
          """
              { "index": {"_index": "dataset", "_id": "1"} }
              {"datasetId":1}
              { "index": {"_index": "dataset", "_id": "2"} }
              {"datasetId":2}

              """,
          new String(
              capturedRequest.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8));
    }
  }

  @Test
  void testDeleteIndexProceedsOnVersionConflicts() throws IOException {
    String datasetIndexName = randomAlphabetic(10);
    int datasetId = randomInt(1, 100);
    int userId = randomInt(1, 100);

    when(esConfig.getDatasetIndexName()).thenReturn(datasetIndexName);
    mockElasticSearchResponse("{\"deleted\":1}");

    try (var _ = service.deleteIndex(datasetId, userId)) {
      verify(esClient).performRequest(request.capture());
      Request capturedRequest = request.getValue();
      assertEquals("POST", capturedRequest.getMethod());
      assertEquals("/" + datasetIndexName + "/_delete_by_query", capturedRequest.getEndpoint());
      assertEquals("proceed", capturedRequest.getParameters().get("conflicts"));
      assertEquals(
          """
              { "query": { "bool": { "must": [ { "match": { "_index": "dataset" } }, { "match": { "_id": "%d" } } ] } } }
              """
              .formatted(datasetId),
          new String(
              capturedRequest.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8));
    }
  }

  @Test
  void testIndexDataset() throws Exception {
    DatasetRecord datasetRecord = createDatasetRecord();
    Dataset dataset1 = datasetRecord.dataset;
    when(datasetDAO.findDatasetsByIdList(List.of(dataset1.getDatasetId())))
        .thenReturn(List.of(dataset1));
    when(studyDAO.findStudyById(datasetRecord.study.getStudyId())).thenReturn(datasetRecord.study);
    when(userDao.findUserById(dataset1.getCreateUserId())).thenReturn(datasetRecord.createUser);
    org.elasticsearch.client.Response mockResponse = mock();
    when(esClient.performRequest(any())).thenReturn(mockResponse);
    when(mockResponse.getEntity()).thenReturn(new StringEntity("response body"));
    StatusLine statusLine = mock();
    when(mockResponse.getStatusLine()).thenReturn(statusLine);
    when(statusLine.getStatusCode()).thenReturn(200);
    try (var _ = service.indexDataset(dataset1.getDatasetId())) {
      verify(datasetDAO, times(1)).findDatasetsByIdList(List.of(dataset1.getDatasetId()));
      verify(studyDAO, times(1)).findStudyById(dataset1.getStudyId());
    }
  }

  @Test
  void testIndexDatasets() throws Exception {
    DatasetRecord datasetRecord = createDatasetRecord();
    Dataset dataset1 = datasetRecord.dataset;
    Dataset dataset2 =
        createDataset(
            datasetRecord.createUser,
            datasetRecord.updateUser,
            new DataUseBuilder().setGeneralUse(true).build(),
            datasetRecord.dac);
    dataset2.setStudy(datasetRecord.study);
    dataset2.setStudyId(dataset1.getStudyId());
    when(datasetDAO.findDatasetsByIdList(List.of(dataset1.getDatasetId(), dataset2.getDatasetId())))
        .thenReturn(List.of(dataset1, dataset2));
    when(studyDAO.findStudyById(datasetRecord.study.getStudyId())).thenReturn(datasetRecord.study);
    when(userDao.findUserById(dataset1.getCreateUserId())).thenReturn(datasetRecord.createUser);
    when(userDao.findUserById(dataset2.getCreateUserId())).thenReturn(datasetRecord.createUser);
    org.elasticsearch.client.Response mockResponse = mock();
    when(esClient.performRequest(any())).thenReturn(mockResponse);
    when(mockResponse.getEntity()).thenReturn(new StringEntity("response body"));
    StatusLine statusLine = mock();
    when(mockResponse.getStatusLine()).thenReturn(statusLine);
    when(statusLine.getStatusCode()).thenReturn(200);

    try (var _ = service.indexDatasets(List.of(dataset1.getDatasetId(), dataset2.getDatasetId()))) {
      verify(datasetDAO, times(1))
          .findDatasetsByIdList(List.of(dataset1.getDatasetId(), dataset2.getDatasetId()));
      assertEquals(dataset1.getStudyId(), dataset2.getStudyId());
      verify(studyDAO, times(1)).findStudyById(dataset1.getStudyId());
    }
  }

  @Test
  void testIndexDatasetsHandleSingleNullDataset() throws Exception {
    when(datasetDAO.findDatasetsByIdList(List.of(1))).thenReturn(List.of());
    try (var response = service.indexDatasets(List.of(1))) {
      verify(datasetDAO).findDatasetsByIdList(List.of(1));
      verify(esClient, never()).performRequest(any());
      assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
    }
  }

  @Test
  void testIndexDatasetsHandleNullDatasetInBatch() throws Exception {
    User user = createUser(1, 100);
    Dataset d1 = createDataset(user, user, new DataUse(), createDac());
    Dataset d2 = createDataset(user, user, new DataUse(), createDac());
    // List of IDs includes two valid datasets and a third ID that does not correspond to an
    // existing dataset
    List<Integer> datasetIds =
        List.of(d1.getDatasetId(), d2.getDatasetId(), d1.getDatasetId() + d2.getDatasetId());
    when(datasetDAO.findDatasetsByIdList(
            List.of(d1.getDatasetId(), d2.getDatasetId(), d1.getDatasetId() + d2.getDatasetId())))
        .thenReturn(List.of(d1, d2));
    when(userDao.findUserById(user.getUserId())).thenReturn(user);
    org.elasticsearch.client.Response mockResponse = mock();
    when(esClient.performRequest(any())).thenReturn(mockResponse);
    when(mockResponse.getEntity()).thenReturn(new StringEntity("response body"));
    StatusLine statusLine = mock();
    when(mockResponse.getStatusLine()).thenReturn(statusLine);
    when(statusLine.getStatusCode()).thenReturn(200);
    try (var response = service.indexDatasets(datasetIds)) {
      verify(datasetDAO, atLeastOnce())
          .findDatasetsByIdList(
              List.of(d1.getDatasetId(), d2.getDatasetId(), d1.getDatasetId() + d2.getDatasetId()));
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }
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
    mockElasticSearchResponse("{\"valid\":true,\"hits\":{\"hits\":[]}}");

    try (var response = service.searchDatasets(query)) {
      assertEquals(200, response.getStatus());
    }
  }

  @Test
  void testSearchDatasetsStream() throws IOException {
    String query = "{ \"query\": { \"query_string\": { \"query\": \"(GRU) AND (HMB)\" } } }";
    String mockResponse = "{\"valid\":true,\"hits\":{\"hits\":[]}}";
    mockElasticSearchResponse(mockResponse);
    try (var inputStream = service.searchDatasetsStream(query)) {
      String received = IOUtils.toString(inputStream, Charset.defaultCharset());
      assertEquals(mockResponse, received);
    }
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "{ \"query\": { \"query_string\": { \"query\": \"(GRU) AND (HMB)\" } } }",
        "{ \"from\": 0, \"size\": 100, \"query\": { \"query_string\": { \"query\": \"(GRU) AND (HMB)\" } } }",
        "{ \"sort\": [\"datasetIdentifier\"], \"query\": { \"query_string\": { \"query\": \"(GRU) AND (HMB)\" } } }",
        "{ \"from\": 0, \"size\": 100, \"sort\": [\"datasetIdentifier\"], \"query\": { \"query_string\": { \"query\": \"(GRU) AND (HMB)\" } } }",
      })
  void testValidateQuerySuccess(String query) throws IOException {
    mockElasticSearchResponse("{\"valid\":true}");
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

    assertThrows(IOException.class, () -> service.validateQuery(query));
  }

  @Test
  void testValidateQueryInvalid() throws IOException {
    String query = "{ \"bad\": [\"and\", \"invalid\"] }";

    mockElasticSearchResponse("{\"valid\":false}");

    assertFalse(service.validateQuery(query));
  }

  @Test
  void testIndexDatasetIdsErrors() throws Exception {
    String mockErrorMessage = "error condition";
    Dataset dataset = new Dataset();
    dataset.setDatasetId(randomInt(10, 100));
    when(datasetDAO.findDatasetsByIdList(List.of(dataset.getDatasetId())))
        .thenReturn(List.of(dataset));
    mockESClientResponse(200, mockErrorMessage);

    StreamingOutput output = service.indexDatasetIds(List.of(dataset.getDatasetId()));
    var baos = new ByteArrayOutputStream();
    output.write(baos);
    assertTrue(baos.toString().contains(mockErrorMessage));
  }

  @Test
  void testIndexDatasetIdsErrors_Not_200_response() throws Exception {
    String mockErrorMessage = "error condition";
    Dataset dataset = new Dataset();
    dataset.setDatasetId(randomInt(10, 100));
    when(datasetDAO.findDatasetsByIdList(List.of(dataset.getDatasetId())))
        .thenReturn(List.of(dataset));
    mockESClientResponse(500, mockErrorMessage);

    StreamingOutput output = service.indexDatasetIds(List.of(dataset.getDatasetId()));
    var baos = new ByteArrayOutputStream();
    output.write(baos);
    assertTrue(baos.toString().contains("Error indexing datasets"));
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
    d.setDatasetId(1);
    study.addDatasetId(d.getDatasetId());
    when(studyDAO.findStudyById(any())).thenReturn(study);

    assertDoesNotThrow(() -> service.indexStudy(1));
  }

  @Test
  void testIndexStudyWithNoDatasets() {
    Study study = new Study();
    study.setStudyId(1);
    when(studyDAO.findStudyById(any())).thenReturn(study);

    assertDoesNotThrow(
        () -> {
          try (var response = service.indexStudy(1)) {
            assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
          }
        });
  }

  @Test
  void testUpdateDatasetIndexDateWithValue() {
    assertDoesNotThrow(() -> service.updateDatasetIndexDate(1, 1, Instant.now()));
  }

  @Test
  void testDeleteDatasetIndexWhenDatasetExists() throws Exception {
    Dataset dataset = new Dataset();
    when(datasetDAO.findDatasetById(any())).thenReturn(dataset);
    assertDoesNotThrow(() -> service.updateDatasetIndexDate(1, 1, null));
    verify(datasetServiceDAO).updateDatasetIndex(any(), any(), any());
  }

  @Test
  void testDeleteDatasetIndexWhenDatasetIsNull() throws Exception {
    when(datasetDAO.findDatasetById(any())).thenReturn(null);
    assertDoesNotThrow(() -> service.updateDatasetIndexDate(1, 1, null));
    verify(datasetServiceDAO, never()).updateDatasetIndex(any(), any(), any());
  }

  @Test
  void testInvalidResultWindow_ValidQueryWithinLimits() {
    String query = "{\"size\": 100, \"from\": 0}";
    assertFalse(service.invalidResultWindow(query));
  }

  @Test
  void testInvalidResultWindow_DoubleValuesThrowClassCastException() {
    String query = "{\"size\": 100.0, \"from\": 0.0}";
    assertTrue(service.invalidResultWindow(query));
  }

  @Test
  void testInvalidResultWindow_StringValuesThrowClassCastException() {
    String query = "{\"size\": \"100\", \"from\": \"0\"}";
    assertTrue(service.invalidResultWindow(query));
  }

  @Test
  void testInvalidResultWindow_ValidQueryAtLimit() {
    String query = "{\"size\": 5000, \"from\": 5000}";
    assertFalse(service.invalidResultWindow(query));
  }

  @Test
  void testInvalidResultWindow_ExceedsMaxResultWindow() {
    String query = "{\"size\": 5000, \"from\": 5001}";
    assertTrue(service.invalidResultWindow(query));
  }

  @Test
  void testInvalidResultWindow_ExceedsMaxResultWindowLargeValues() {
    String query = "{\"size\": 10000, \"from\": 1}";
    assertTrue(service.invalidResultWindow(query));
  }

  @Test
  void testInvalidResultWindow_DefaultValues() {
    String query = "{}";
    assertFalse(service.invalidResultWindow(query));
  }

  @Test
  void testInvalidResultWindow_OnlySizeProvided() {
    String query = "{\"size\": 50}";
    assertFalse(service.invalidResultWindow(query));
  }

  @Test
  void testInvalidResultWindow_OnlyFromProvided() {
    String query = "{\"from\": 100}";
    assertFalse(service.invalidResultWindow(query));
  }

  @Test
  void testInvalidResultWindow_InvalidJsonFormat() {
    String query = "{invalid json}";
    assertTrue(service.invalidResultWindow(query));
  }

  @Test
  void testInvalidResultWindow_NullQuery() {
    assertTrue(service.invalidResultWindow(null));
  }

  @Test
  void testInvalidResultWindow_EmptyString() {
    String query = "";
    assertTrue(service.invalidResultWindow(query));
  }

  @Test
  void testInvalidResultWindow_NonJsonString() {
    String query = "not a json string";
    assertTrue(service.invalidResultWindow(query));
  }

  @Test
  void testInvalidResultWindow_ExtraFieldsInQuery() {
    String query = "{\"size\": 100, \"from\": 50, \"query\": {\"match_all\": {}}}";
    assertFalse(service.invalidResultWindow(query));
  }

  @Test
  void testGetIndexKey_SetKey() {
    service.setIndexKey("custom-key");
    assertEquals("custom-key", service.getIndexKey());
  }

  @Test
  void testGetIndexKey_DefaultIndex() throws IOException {
    service.setIndexKey(null);
    String body = "{ \"version\": { \"number\": \"7.10.2\" } }";
    mockElasticSearchResponse(body);
    assertEquals("_index", service.getIndexKey());
  }

  @Test
  void testGetIndexKey_LegacyType() throws IOException {
    service.setIndexKey(null);
    String body = "{ \"version\": { \"number\": \"6.8.0\" } }";
    mockElasticSearchResponse(body);
    assertEquals("_type", service.getIndexKey());
  }

  @Test
  void testGetIndexKey_OpenSearch() throws IOException {
    service.setIndexKey(null);
    String body = "{ \"version\": { \"number\": \"3.3.0\", \"distribution\": \"opensearch\" } }";
    mockElasticSearchResponse(body);
    assertEquals("_index", service.getIndexKey());
  }

  @Test
  void testGetIndexKey_ExceptionDefaultsToIndex() throws IOException {
    service.setIndexKey(null);
    when(esClient.performRequest(any())).thenThrow(new IOException("Connection failed"));
    assertEquals("_index", service.getIndexKey());
  }

  @Test
  void testGetIndexKey_NullInfoDefaultsToIndex() throws IOException {
    service.setIndexKey(null);
    mockElasticSearchResponse("{}");
    assertEquals("_index", service.getIndexKey());
  }
}
