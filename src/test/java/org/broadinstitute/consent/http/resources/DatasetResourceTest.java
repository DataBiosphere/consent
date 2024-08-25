package org.broadinstitute.consent.http.resources;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.client.http.HttpStatusCodes;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.enumeration.PropertyType;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetProperty;
import org.broadinstitute.consent.http.models.DatasetSummary;
import org.broadinstitute.consent.http.models.Error;
import org.broadinstitute.consent.http.models.Study;
import org.broadinstitute.consent.http.models.StudyProperty;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.models.dataset_registration_v1.ConsentGroup.AccessManagement;
import org.broadinstitute.consent.http.models.dataset_registration_v1.ConsentGroup.DataLocation;
import org.broadinstitute.consent.http.models.dataset_registration_v1.DatasetRegistrationSchemaV1;
import org.broadinstitute.consent.http.models.dto.DatasetDTO;
import org.broadinstitute.consent.http.models.dto.DatasetPropertyDTO;
import org.broadinstitute.consent.http.service.DatasetRegistrationService;
import org.broadinstitute.consent.http.service.DatasetService;
import org.broadinstitute.consent.http.service.ElasticSearchService;
import org.broadinstitute.consent.http.service.UserService;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DatasetResourceTest {

  @Mock
  private DatasetService datasetService;
  @Mock
  private DatasetRegistrationService datasetRegistrationService;

  @Mock
  private ElasticSearchService elasticSearchService;

  @Mock
  private UserService userService;

  @Mock
  private AuthUser authUser;

  @Mock
  private User user;

  @Mock
  private UriInfo uriInfo;

  @Mock
  private UriBuilder uriBuilder;

  private DatasetResource resource;

  private void initResource() {
    resource = new DatasetResource(datasetService, userService,
        datasetRegistrationService, elasticSearchService);
  }

  private String createPropertiesJson(List<DatasetPropertyDTO> properties) {
    DatasetDTO json = new DatasetDTO();
    json.setProperties(properties);
    return new Gson().toJson(json);
  }

  private String createPropertiesJson(String propertyName, String propertyValue) {
    List<DatasetPropertyDTO> jsonProperties = new ArrayList<>();
    jsonProperties.add(new DatasetPropertyDTO(propertyName, propertyValue));
    return createPropertiesJson(jsonProperties);
  }

  private DatasetDTO createMockDatasetDTO() {
    DatasetDTO mockDTO = new DatasetDTO();
    mockDTO.setDataSetId(RandomUtils.nextInt(100, 1000));
    mockDTO.setDatasetName("test");
    mockDTO.addProperty(new DatasetPropertyDTO("Property", "test"));

    return mockDTO;
  }

  @Test
  void testCreateDatasetSuccess() throws Exception {
    DatasetDTO result = createMockDatasetDTO();
    String json = createPropertiesJson("Dataset Name", "test");

    when(datasetService.getDatasetByName("test")).thenReturn(null);
    when(datasetService.createDatasetFromDatasetDTO(any(), any(), anyInt())).thenReturn(result);
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(user.getUserId()).thenReturn(1);
    when(uriInfo.getRequestUriBuilder()).thenReturn(uriBuilder);
    when(uriBuilder.replacePath(anyString())).thenReturn(uriBuilder);
    when(uriBuilder.build(any())).thenReturn(new URI("/api/dataset/1"));
    initResource();
    Response response = resource.createDataset(authUser, uriInfo, json);

    assertEquals(HttpStatusCodes.STATUS_CODE_CREATED, response.getStatus());
    assertEquals(result, response.getEntity());
  }

  @Test
  void testCreateDatasetNoJson() {
    initResource();
    assertThrows(BadRequestException.class, () -> {
      resource.createDataset(authUser, uriInfo, "");
    });
  }

  @Test
  void testCreateDatasetNoProperties() {
    initResource();
    assertThrows(BadRequestException.class, () -> {
      resource.createDataset(authUser, uriInfo, "{\"properties\":[]}");
    });
  }

  @Test
  void testCreateDatasetNullName() {
    String json = createPropertiesJson("Dataset Name", null);

    initResource();
    assertThrows(BadRequestException.class, () -> {
      resource.createDataset(authUser, uriInfo, json);
    });
  }

  @Test
  void testCreateDatasetEmptyName() {
    String json = createPropertiesJson("Dataset Name", "");

    initResource();

    assertThrows(BadRequestException.class, () -> {
      resource.createDataset(authUser, uriInfo, json);
    });
  }

  @Test
  void testCreateDatasetMissingName() {
    String json = createPropertiesJson("Property", "test");

    initResource();
    assertThrows(BadRequestException.class, () -> {
      resource.createDataset(authUser, uriInfo, json);
    });
  }

  @Test
  void testCreateDatasetInvalidProperty() {
    List<DatasetPropertyDTO> invalidProperties = new ArrayList<>();
    invalidProperties.add(new DatasetPropertyDTO("Invalid Property", "test"));
    when(datasetService.findInvalidProperties(any())).thenReturn(invalidProperties);

    String json = createPropertiesJson(invalidProperties);

    initResource();

    assertThrows(BadRequestException.class, () -> {
      resource.createDataset(authUser, uriInfo, json);
    });
  }

  @Test
  void testCreateDatasetDuplicateProperties() {
    List<DatasetPropertyDTO> duplicateProperties = new ArrayList<>();
    duplicateProperties.add(new DatasetPropertyDTO("Dataset Name", "test"));
    duplicateProperties.add(new DatasetPropertyDTO("Dataset Name", "test"));
    when(datasetService.findDuplicateProperties(any())).thenReturn(duplicateProperties);

    String json = createPropertiesJson(duplicateProperties);

    initResource();
    assertThrows(BadRequestException.class, () -> {
      resource.createDataset(authUser, uriInfo, json);
    });
  }

  @Test
  void testCreateDatasetNameInUse() {
    Dataset inUse = new Dataset();
    when(datasetService.getDatasetByName("test")).thenReturn(inUse);

    String json = createPropertiesJson("Dataset Name", "test");

    initResource();

    assertThrows(ClientErrorException.class, () -> {
      resource.createDataset(authUser, uriInfo, json);
    });
  }

  @Test
  void testCreateDatasetError() {
    String json = createPropertiesJson("Dataset Name", "test");

    when(datasetService.getDatasetByName("test")).thenReturn(null);
    doThrow(new RuntimeException()).when(datasetService)
        .createDatasetFromDatasetDTO(any(), any(), anyInt());
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(user.getUserId()).thenReturn(1);
    initResource();
    Response response = resource.createDataset(authUser, uriInfo, json);

    assertEquals(HttpStatusCodes.STATUS_CODE_SERVER_ERROR, response.getStatus());
  }

  @Test
  void testUpdateDatasetSuccess() {
    Dataset preexistingDataset = new Dataset();
    String json = createPropertiesJson("Dataset Name", "test");
    when(datasetService.findDatasetById(anyInt())).thenReturn(preexistingDataset);
    when(datasetService.updateDataset(any(), any(), any())).thenReturn(
        Optional.of(preexistingDataset));
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(user.getUserId()).thenReturn(1);
    when(user.hasUserRole(any())).thenReturn(true);
    when(uriInfo.getRequestUriBuilder()).thenReturn(uriBuilder);
    when(uriBuilder.replacePath(anyString())).thenReturn(uriBuilder);
    initResource();
    Response response = resource.updateDataset(authUser, uriInfo, 1, json);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    assertEquals(Optional.of(preexistingDataset).get(), response.getEntity());
  }

  @Test
  void testUpdateDatasetNoJson() {
    initResource();
    Response response = resource.updateDataset(authUser, uriInfo, 1, "");
    assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
  }

  @Test
  void testUpdateDatasetNoProperties() {
    initResource();
    Response response = resource.updateDataset(authUser, uriInfo, 1, "{\"properties\":[]}");
    assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
  }

  @Test
  void testUpdateDatasetIdNotFound() {
    String json = createPropertiesJson("Dataset Name", "test");
    when(datasetService.findDatasetById(anyInt())).thenReturn(null);

    initResource();
    Response response = resource.updateDataset(authUser, uriInfo, 1, json);
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  @Test
  void testUpdateDatasetInvalidProperty() {
    List<DatasetPropertyDTO> invalidProperties = new ArrayList<>();
    invalidProperties.add(new DatasetPropertyDTO("Invalid Property", "test"));
    when(datasetService.findInvalidProperties(any())).thenReturn(invalidProperties);

    Dataset preexistingDataset = new Dataset();
    when(datasetService.findDatasetById(anyInt())).thenReturn(preexistingDataset);
    String json = createPropertiesJson(invalidProperties);

    initResource();
    Response response = resource.updateDataset(authUser, uriInfo, 1, json);
    assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
  }

  @Test
  void testUpdateDatasetDuplicateProperties() {
    List<DatasetPropertyDTO> duplicateProperties = new ArrayList<>();
    duplicateProperties.add(new DatasetPropertyDTO("Dataset Name", "test"));
    duplicateProperties.add(new DatasetPropertyDTO("Dataset Name", "test"));
    when(datasetService.findDuplicateProperties(any())).thenReturn(duplicateProperties);

    Dataset preexistingDataset = new Dataset();
    when(datasetService.findDatasetById(anyInt())).thenReturn(preexistingDataset);
    String json = createPropertiesJson(duplicateProperties);

    initResource();
    Response response = resource.updateDataset(authUser, uriInfo, 1, json);
    assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
  }

  @Test
  void testUpdateDatasetNoContent() {
    Dataset preexistingDataset = new Dataset();
    String json = createPropertiesJson("Dataset Name", "test");
    when(datasetService.findDatasetById(anyInt())).thenReturn(preexistingDataset);
    when(datasetService.updateDataset(any(), any(), any())).thenReturn(Optional.empty());
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(user.getUserId()).thenReturn(1);
    when(user.hasUserRole(any())).thenReturn(true);
    initResource();
    Response responseNoContent = resource.updateDataset(authUser, uriInfo, 1, json);
    assertEquals(204, responseNoContent.getStatus());
  }

  @Test
  void testValidateDatasetNameSuccess() {
    Dataset testDataset = new Dataset();
    when(datasetService.getDatasetByName("test")).thenReturn(testDataset);
    initResource();
    Response response = resource.validateDatasetName("test");
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  void testValidateDatasetNameNotFound() {
    initResource();

    assertThrows(NotFoundException.class, () -> {
      resource.validateDatasetName("test");
    });
  }

  @Test
  void testFindAllStudyNamesSuccess() {
    when(datasetService.findAllStudyNames()).thenReturn(Set.of("Hi", "Hello"));
    initResource();
    Response response = resource.findAllStudyNames();
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  void testFindAllStudyNamesFail() {
    when(datasetService.findAllStudyNames()).thenThrow();
    initResource();
    Response response = resource.findAllStudyNames();
    assertEquals(HttpStatusCodes.STATUS_CODE_SERVER_ERROR, response.getStatus());
  }

  @Test
  void testDownloadDatasetsSuccess() {
    List<DatasetDTO> dtoList = new ArrayList<>();
    DatasetDTO testDTO = createMockDatasetDTO();
    dtoList.add(testDTO);

    when(datasetService.describeDataSetsByReceiveOrder(any())).thenReturn(dtoList);
    initResource();

    Response response = resource.downloadDataSets(List.of(1));
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  void testDownloadDatasetsHeaderError() {
    doThrow(new RuntimeException()).when(datasetService).describeDictionaryByReceiveOrder();
    initResource();
    Response response = resource.downloadDataSets(List.of(1));
    assertEquals(HttpStatusCodes.STATUS_CODE_SERVER_ERROR, response.getStatus());
  }

  @Test
  void testDownloadDatasetsEmptyList() {
    initResource();
    Response response = resource.downloadDataSets(List.of());
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  void testDownloadDatasetsServiceError() {
    doThrow(new RuntimeException()).when(datasetService).describeDataSetsByReceiveOrder(any());
    initResource();
    Response response = resource.downloadDataSets(List.of(1));
    assertEquals(HttpStatusCodes.STATUS_CODE_SERVER_ERROR, response.getStatus());
  }

  @Test
  void testDeleteSuccessAdmin() {
    Dataset dataSet = new Dataset();

    when(user.hasUserRole(UserRoles.ADMIN)).thenReturn(true);
    when(userService.findUserByEmail(authUser.getEmail())).thenReturn(user);
    when(datasetService.findDatasetById(any())).thenReturn(dataSet);

    initResource();
    Response response = resource.delete(authUser, 1, null);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  void testDeleteSuccessChairperson() {
    Dataset dataSet = new Dataset();
    dataSet.setDataSetId(1);
    dataSet.setDacId(1);

    when(user.hasUserRole(UserRoles.ADMIN)).thenReturn(false);
    UserRole role = UserRoles.Chairperson();
    role.setDacId(1);
    when(user.getRoles()).thenReturn(List.of(role));

    when(userService.findUserByEmail(authUser.getEmail())).thenReturn(user);
    when(datasetService.findDatasetById(any())).thenReturn(dataSet);

    initResource();
    Response response = resource.delete(authUser, 1, null);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  void testDeleteErrorNoDacIds() {
    Dataset dataSet = new Dataset();

    when(user.hasUserRole(UserRoles.ADMIN)).thenReturn(false);
    UserRole role = UserRoles.Chairperson();
    when(user.getRoles()).thenReturn(List.of(role));

    when(userService.findUserByEmail(authUser.getEmail())).thenReturn(user);
    when(datasetService.findDatasetById(any())).thenReturn(dataSet);

    initResource();
    Response response = resource.delete(authUser, 1, null);
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  @Test
  void testDeleteErrorNullConsent() {
    Dataset dataSet = new Dataset();
    dataSet.setDataSetId(1);

    when(user.hasUserRole(UserRoles.ADMIN)).thenReturn(false);
    UserRole role = UserRoles.Chairperson();
    role.setDacId(1);
    when(user.getRoles()).thenReturn(List.of(role));

    when(userService.findUserByEmail(authUser.getEmail())).thenReturn(user);
    when(datasetService.findDatasetById(any())).thenReturn(dataSet);

    initResource();
    Response response = resource.delete(authUser, 1, null);
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  @Test
  void testDeleteErrorMismatch() {
    Dataset dataSet = new Dataset();
    dataSet.setDataSetId(1);
    dataSet.setDacId(2);

    when(user.hasUserRole(UserRoles.ADMIN)).thenReturn(false);
    UserRole role = UserRoles.Chairperson();
    role.setDacId(1);
    when(user.getRoles()).thenReturn(List.of(role));

    when(userService.findUserByEmail(authUser.getEmail())).thenReturn(user);
    when(datasetService.findDatasetById(any())).thenReturn(dataSet);

    initResource();
    Response response = resource.delete(authUser, 1, null);
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  @Test
  void testIndexAllDatasets() throws Exception {
    Dataset dataset = new Dataset();
    dataset.setDataSetId(RandomUtils.nextInt(10, 100));
    Gson gson = GsonUtil.buildGson();
    String esResponseArray = """
        [
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
        ]
        """;
    StreamingOutput output = out -> out.write(
        esResponseArray.formatted(dataset.getDataSetId()).getBytes());
    when(datasetService.findAllDatasetIds()).thenReturn(List.of(dataset.getDataSetId()));
    when(elasticSearchService.indexDatasetIds(List.of(dataset.getDataSetId()))).thenReturn(output);

    initResource();
    try (Response response = resource.indexDatasets()) {
      var entity = (StreamingOutput) response.getEntity();
      var baos = new ByteArrayOutputStream();
      entity.write(baos);
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
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }
  }

  @Test
  void testIndexDataset() throws IOException {
    Dataset dataset = new Dataset();

    Response mockResponse = Response.ok().entity(dataset).build();
    when(datasetService.findDatasetById(any())).thenReturn(dataset);
    when(elasticSearchService.indexDataset(dataset)).thenReturn(mockResponse);

    initResource();
    Response response = resource.indexDataset(1);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  void testIndexDelete() throws IOException {
    Response mockResponse = Response.status(HttpStatusCodes.STATUS_CODE_OK).entity("deleted")
        .build();
    when(elasticSearchService.deleteIndex(any())).thenReturn(mockResponse);

    initResource();
    Response response = resource.deleteDatasetIndex(0);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  void testSearchDatasetsControlledAccess() {
    Dataset ds = new Dataset();
    ds.setDataSetId(1);
    AccessManagement accessManagement = AccessManagement.CONTROLLED;
    when(authUser.getEmail()).thenReturn("testauthuser@test.com");
    when(userService.findUserByEmail("testauthuser@test.com")).thenReturn(user);
    when(datasetService.searchDatasets("search query", accessManagement, user)).thenReturn(
        List.of(ds));

    initResource();
    Response response = resource.searchDatasets(authUser, "search query",
        accessManagement.toString());

    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    assertEquals(GsonUtil.buildGson().toJson(List.of(ds)), response.getEntity());
  }

  @Test
  void testSearchDatasetsOpenAccess() {
    Dataset ds = new Dataset();
    ds.setDataSetId(1);
    AccessManagement accessManagement = AccessManagement.OPEN;
    when(authUser.getEmail()).thenReturn("testauthuser@test.com");
    when(userService.findUserByEmail("testauthuser@test.com")).thenReturn(user);
    when(datasetService.searchDatasets("search query", accessManagement, user)).thenReturn(
        List.of(ds));

    initResource();
    Response response = resource.searchDatasets(authUser, "search query",
        accessManagement.toString());

    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    assertEquals(GsonUtil.buildGson().toJson(List.of(ds)), response.getEntity());
  }

  @Test
  void testAutocompleteDatasets() {
    when(authUser.getEmail()).thenReturn("testauthuser@test.com");
    when(userService.findUserByEmail("testauthuser@test.com")).thenReturn(user);
    when(datasetService.searchDatasetSummaries(any())).thenReturn(List.of(new DatasetSummary(1, "ID", "Name")));

    initResource();
    try (Response response = resource.autocompleteDatasets(authUser, "test")) {
      assertTrue(HttpStatusCodes.isSuccess(response.getStatus()));
    }
  }

  @Test
  void testSearchDatasetIndex() throws IOException {
    String query = "{ \"dataUse\": [\"HMB\"] }";

    Response mockResponse = Response.ok().entity(query).build();
    when(authUser.getEmail()).thenReturn("testauthuser@test.com");
    when(userService.findUserByEmail("testauthuser@test.com")).thenReturn(user);
    when(elasticSearchService.searchDatasets(any())).thenReturn(mockResponse);

    initResource();
    Response response = resource.searchDatasetIndex(authUser, query);

    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    assertTrue(response.getEntity().toString().length() > 2);
  }

  @Test
  void testGetDataset() {
    Dataset ds = new Dataset();
    ds.setDataSetId(1);
    ds.setName("asdfasdfasdfasdfasdfasdf");
    when(datasetService.findDatasetById(1)).thenReturn(ds);
    initResource();
    Response response = resource.getDataset(1);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    assertEquals(ds, response.getEntity());
  }

  @Test
  void testGetDatasetNotFound() {
    when(datasetService.findDatasetById(1)).thenReturn(null);

    initResource();
    Response response = resource.getDataset(1);
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  @Test
  void testGetDatasets() {
    Dataset ds1 = new Dataset();
    ds1.setDataSetId(1);
    Dataset ds2 = new Dataset();
    ds2.setDataSetId(2);
    Dataset ds3 = new Dataset();
    ds3.setDataSetId(3);
    List<Dataset> datasets = List.of(ds1, ds2, ds3);

    when(datasetService.findDatasetsByIds(List.of(1, 2, 3))).thenReturn(datasets);

    initResource();
    Response response = resource.getDatasets(List.of(1, 2, 3));
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    assertEquals(datasets, response.getEntity());
  }

  @Test
  void testGetDatasetsDuplicates() {
    Dataset ds1 = new Dataset();
    ds1.setDataSetId(1);
    Dataset ds2 = new Dataset();
    ds2.setDataSetId(2);
    Dataset ds3 = new Dataset();
    ds3.setDataSetId(3);
    List<Dataset> datasets = List.of(ds1, ds2, ds3);

    when(datasetService.findDatasetsByIds(List.of(1, 1, 2, 2, 3, 3))).thenReturn(datasets);

    initResource();
    Response response = resource.getDatasets(List.of(1, 1, 2, 2, 3, 3));
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    assertEquals(datasets, response.getEntity());
  }

  @Test
  void testGetDatasetsDuplicatesNotFound() {
    Dataset ds1 = new Dataset();
    ds1.setDataSetId(1);
    Dataset ds2 = new Dataset();
    ds2.setDataSetId(2);

    when(datasetService.findDatasetsByIds(List.of(1, 1, 2, 2, 3, 3))).thenReturn(List.of(
        ds1,
        ds2
    ));

    initResource();
    Response response = resource.getDatasets(List.of(1, 1, 2, 2, 3, 3));
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
    assertTrue(((Error) response.getEntity()).message().contains("3"));
    assertFalse(((Error) response.getEntity()).message().contains("2"));
    assertFalse(((Error) response.getEntity()).message().contains("1"));

  }

  @Test
  void testGetDatasetsNotFound() {
    Dataset ds1 = new Dataset();
    ds1.setDataSetId(1);
    Dataset ds3 = new Dataset();
    ds3.setDataSetId(3);

    when(datasetService.findDatasetsByIds(List.of(1, 2, 3, 4))).thenReturn(List.of(
        ds1,
        ds3
    ));

    initResource();
    Response response = resource.getDatasets(List.of(1, 2, 3, 4));
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
    assertTrue(((Error) response.getEntity()).message().contains("4"));
    assertFalse(((Error) response.getEntity()).message().contains("3"));
    assertTrue(((Error) response.getEntity()).message().contains("2"));
    assertFalse(((Error) response.getEntity()).message().contains("1"));
  }

  @Test
  void testGetDatasetsNotFoundNullValues() {
    Dataset ds1 = new Dataset();
    ds1.setDataSetId(1);
    Dataset ds3 = new Dataset();
    ds3.setDataSetId(3);

    when(datasetService.findDatasetsByIds(any())).thenReturn(List.of(
        ds1,
        ds3
    ));

    initResource();
    List<Integer> input = new ArrayList<>(List.of(1, 2, 3, 4));
    input.add(null);
    Response response = resource.getDatasets(input);
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
    assertTrue(((Error) response.getEntity()).message().contains("4"));
    assertFalse(((Error) response.getEntity()).message().contains("3"));
    assertTrue(((Error) response.getEntity()).message().contains("2"));
    assertFalse(((Error) response.getEntity()).message().contains("1"));
  }


  @Test
  void testUpdateDatasetDataUse_OK() {
    when(userService.findUserByEmail(any())).thenReturn(new User());
    Dataset d = new Dataset();
    when(datasetService.findDatasetById(any())).thenReturn(d);
    when(datasetService.updateDatasetDataUse(any(), any(), any())).thenReturn(d);

    initResource();
    String duString = new DataUseBuilder().setGeneralUse(true).build().toString();
    Response response = resource.updateDatasetDataUse(new AuthUser(), 1, duString);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  void testUpdateDatasetDataUse_BadRequestJson() {
    when(userService.findUserByEmail(any())).thenReturn(new User());

    initResource();
    Response response = resource.updateDatasetDataUse(new AuthUser(), 1, "invalid json");
    assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
  }

  @Test
  void testUpdateDatasetDataUse_BadRequestService() {
    when(userService.findUserByEmail(any())).thenReturn(new User());
    Dataset d = new Dataset();
    when(datasetService.findDatasetById(any())).thenReturn(d);
    when(datasetService.updateDatasetDataUse(any(), any(), any())).thenThrow(
        new IllegalArgumentException());

    initResource();
    String duString = new DataUseBuilder().setGeneralUse(true).build().toString();
    Response response = resource.updateDatasetDataUse(new AuthUser(), 1, duString);
    assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
  }

  @Test
  void testUpdateDatasetDataUse_NotFound() {
    when(userService.findUserByEmail(any())).thenReturn(new User());
    when(datasetService.findDatasetById(any())).thenThrow(new NotFoundException());

    initResource();
    String duString = new DataUseBuilder().setGeneralUse(true).build().toString();
    Response response = resource.updateDatasetDataUse(new AuthUser(), 1, duString);
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  @Test
  void testUpdateDatasetDataUse_NotModified() {
    when(userService.findUserByEmail(any())).thenReturn(new User());
    Dataset d = new Dataset();
    DataUse du = new DataUseBuilder().setGeneralUse(true).build();
    d.setDataUse(du);
    when(datasetService.findDatasetById(any())).thenReturn(d);

    initResource();
    Response response = resource.updateDatasetDataUse(new AuthUser(), 1, du.toString());
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_MODIFIED, response.getStatus());
  }

  @Test
  void testFindAllDatasetsStreaming() throws Exception {
    var dataset = new Dataset();
    dataset.setDataSetId(RandomUtils.nextInt(100, 1000));
    when(userService.findUserByEmail(any())).thenReturn(user);
    final Gson gson = GsonUtil.buildGson();
    StreamingOutput output = out -> out.write(gson.toJson(List.of(dataset)).getBytes());
    when(datasetService.findAllDatasetsAsStreamingOutput()).thenReturn(output);
    initResource();

    Response response = resource.findAllDatasetsStreaming(authUser);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    var entity = (StreamingOutput) response.getEntity();
    var baos = new ByteArrayOutputStream();
    entity.write(baos);
    var entityString = baos.toString();
    Type listOfDatasetsType = new TypeToken<List<Dataset>>() {}.getType();
    List<Dataset> returnedDatasets = gson.fromJson(entityString, listOfDatasetsType);
    assertThat(returnedDatasets, hasSize(1));
    assertEquals(dataset.getDataSetId(), returnedDatasets.get(0).getDataSetId());
  }

  @Test
  void testFindAllDatasetStudySummaries() {
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(datasetService.findAllDatasetStudySummaries()).thenReturn(List.of());
    initResource();
    Response response = resource.findAllDatasetStudySummaries(authUser);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  void testCreateDatasetRegistration_invalidSchema_case1() {
    initResource();
    Response response = resource.createDatasetRegistration(authUser, null, "");
    assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
  }

  @Test
  void testCreateDatasetRegistration_invalidSchema_case2() {
    initResource();
    Response response = resource.createDatasetRegistration(authUser, null, "{}");
    assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
  }

  @Test
  void testCreateDatasetRegistration_invalidSchema_case3() {
    DatasetRegistrationSchemaV1 schemaV1 = new DatasetRegistrationSchemaV1();
    String schemaString = new Gson().toJson(schemaV1);
    initResource();
    Response response = resource.createDatasetRegistration(authUser, null, schemaString);
    assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
  }

  @Test
  void testCreateDatasetRegistration_validSchema() throws SQLException, IOException {
    when(userService.findUserByEmail(any())).thenReturn(user);
    Dataset dataset = new Dataset();
    Study study = new Study();
    study.setStudyId(1);
    dataset.setStudy(study);
    when(datasetRegistrationService.createDatasetsFromRegistration(any(), any(), any())).thenReturn(
        List.of(dataset));
    String schemaV1 = createDatasetRegistrationMock(user);
    initResource();

    Response response = resource.createDatasetRegistration(authUser, null, schemaV1);
    System.out.println(response.getEntity());
    assertEquals(HttpStatusCodes.STATUS_CODE_CREATED, response.getStatus());
  }

  @Test
  void testCreateDatasetRegistration_withFile() throws SQLException, IOException {
    FormDataContentDisposition content = FormDataContentDisposition
        .name("file")
        .fileName("sharing_plan.txt")
        .build();
    FormDataBodyPart formDataBodyPart = mock(FormDataBodyPart.class);
    when(formDataBodyPart.getContentDisposition()).thenReturn(content);

    FormDataMultiPart formDataMultiPart = mock(FormDataMultiPart.class);
    when(formDataMultiPart.getFields()).thenReturn(Map.of("file", List.of(formDataBodyPart)));

    when(userService.findUserByEmail(any())).thenReturn(user);
    Dataset dataset = new Dataset();
    Study study = new Study();
    study.setStudyId(1);
    dataset.setStudy(study);
    when(datasetRegistrationService.createDatasetsFromRegistration(any(), any(), any())).thenReturn(
        List.of(dataset));
    String schemaV1 = createDatasetRegistrationMock(user);
    initResource();

    Response response = resource.createDatasetRegistration(authUser, formDataMultiPart, schemaV1);
    assertEquals(HttpStatusCodes.STATUS_CODE_CREATED, response.getStatus());
  }

  @Test
  void testCreateDatasetRegistration_multipleFiles() throws SQLException, IOException {
    FormDataContentDisposition contentFile = FormDataContentDisposition
        .name("file")
        .fileName("sharing_plan.txt")
        .build();
    FormDataBodyPart formDataBodyPartFile = mock(FormDataBodyPart.class);
    when(formDataBodyPartFile.getName()).thenReturn("file");
    when(formDataBodyPartFile.getContentDisposition()).thenReturn(contentFile);

    FormDataContentDisposition contentOther = FormDataContentDisposition
        .name("other")
        .fileName("other.txt")
        .build();
    FormDataBodyPart formDataBodyPartOther = mock(FormDataBodyPart.class);
    when(formDataBodyPartOther.getName()).thenReturn("other");
    when(formDataBodyPartOther.getContentDisposition()).thenReturn(contentOther);

    FormDataContentDisposition contentNotFile = FormDataContentDisposition
        .name("notFile")
        .build();
    FormDataBodyPart formDataBodyPartNotFile = mock(FormDataBodyPart.class);
    when(formDataBodyPartNotFile.getContentDisposition()).thenReturn(contentNotFile);

    FormDataMultiPart formDataMultiPart = mock(FormDataMultiPart.class);
    when(formDataMultiPart.getFields()).thenReturn(
        Map.of(
            "file", List.of(formDataBodyPartFile),
            "other", List.of(formDataBodyPartOther),
            "notFile", List.of(formDataBodyPartNotFile)));

    when(userService.findUserByEmail(any())).thenReturn(user);
    Dataset dataset = new Dataset();
    Study study = new Study();
    study.setStudyId(1);
    dataset.setStudy(study);
    when(datasetRegistrationService.createDatasetsFromRegistration(any(), any(), any())).thenReturn(
        List.of(dataset));
    String schemaV1 = createDatasetRegistrationMock(user);
    initResource();

    Response response = resource.createDatasetRegistration(authUser, formDataMultiPart, schemaV1);

    assertEquals(HttpStatusCodes.STATUS_CODE_CREATED, response.getStatus());
    verify(datasetRegistrationService, times(1)).createDatasetsFromRegistration(
        any(),
        eq(user),
        eq(Map.of("file", formDataBodyPartFile, "other", formDataBodyPartOther)));

  }

  @Test
  void testCreateDatasetRegistration_invalidFileName() {
    FormDataContentDisposition content = FormDataContentDisposition
        .name("file")
        .fileName("file/with&$invalid*^chars\\.txt")
        .build();
    FormDataBodyPart formDataBodyPart = mock(FormDataBodyPart.class);
    when(formDataBodyPart.getContentDisposition()).thenReturn(content);

    FormDataMultiPart formDataMultiPart = mock(FormDataMultiPart.class);
    when(formDataMultiPart.getFields()).thenReturn(Map.of("file", List.of(formDataBodyPart)));

    when(userService.findUserByEmail(any())).thenReturn(user);
    String schemaV1 = createDatasetRegistrationMock(user);
    initResource();

    Response response = resource.createDatasetRegistration(authUser, formDataMultiPart, schemaV1);
    assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
  }

  @Test
  void testGetRegistrationFromDatasetIdentifier() {
    Study study = createMockStudy();
    Dataset dataset = study.getDatasets().stream().findFirst().orElse(null);
    assertNotNull(dataset);
    when(datasetService.findDatasetByIdentifier(any())).thenReturn(dataset);
    when(datasetService.findStudyById(any())).thenReturn(study);

    initResource();
    Response response = resource.getRegistrationFromDatasetIdentifier(authUser,
        dataset.getDatasetIdentifier());
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  void testGetRegistrationFromDatasetIdentifierStudyNotFound() {
    Study study = createMockStudy();
    Dataset dataset = study.getDatasets().stream().findFirst().orElse(null);
    assertNotNull(dataset);
    when(datasetService.findDatasetByIdentifier(any())).thenReturn(dataset);
    when(datasetService.findStudyById(any())).thenThrow(new NotFoundException());

    initResource();
    Response response = resource.getRegistrationFromDatasetIdentifier(authUser,
        dataset.getDatasetIdentifier());
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  @Test
  void testGetRegistrationFromDatasetIdentifierDatasetNotFound() {
    Study study = createMockStudy();
    Dataset dataset = study.getDatasets().stream().findFirst().orElse(null);
    assertNotNull(dataset);
    when(datasetService.findDatasetByIdentifier(any())).thenReturn(null);

    initResource();
    Response response = resource.getRegistrationFromDatasetIdentifier(authUser,
        dataset.getDatasetIdentifier());
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  @Test
  void testupdateDatasetByDatasetIntakeSuccess() throws SQLException, IOException {
    Dataset preexistingDataset = new Dataset();
    when(datasetService.findDatasetById(anyInt())).thenReturn(preexistingDataset);
    when(datasetRegistrationService.updateDataset(any(), any(), any(), any())).thenReturn(
        preexistingDataset);
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(user.getUserId()).thenReturn(1);
    String json = createDataset(user);

    FormDataContentDisposition content = FormDataContentDisposition
        .name("file")
        .fileName("validFile.txt")
        .build();

    FormDataBodyPart formDataBodyPart = mock(FormDataBodyPart.class);
    when(formDataBodyPart.getContentDisposition()).thenReturn(content);

    FormDataMultiPart formDataMultiPart = mock(FormDataMultiPart.class);
    when(formDataMultiPart.getFields()).thenReturn(Map.of("file", List.of(formDataBodyPart)));
    initResource();

    Response response = resource.updateByDatasetUpdate(authUser, 1, formDataMultiPart, json);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    assertEquals(Optional.of(preexistingDataset).get(), response.getEntity());
  }

  @Test
  void testUpdateDatasetWithNoJson() {
    FormDataContentDisposition content = FormDataContentDisposition
        .name("file")
        .fileName("validFile.txt")
        .build();
    FormDataMultiPart formDataMultiPart = mock(FormDataMultiPart.class);
    initResource();
    Response response = resource.updateByDatasetUpdate(authUser, 1, formDataMultiPart, "");
    assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
  }

  @Test
  void testUpdateDatasetWithInvalidJson() {
    FormDataContentDisposition content = FormDataContentDisposition
        .name("file")
        .fileName("validFile.txt")
        .build();
    String json = createInvalidDataset(user);
    FormDataMultiPart formDataMultiPart = mock(FormDataMultiPart.class);
    initResource();
    Response response = resource.updateByDatasetUpdate(authUser, 1, formDataMultiPart, json);
    assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
  }

  /**
   * tests the case that there are no updates to the dataset properties, should result in success
   */
  @Test
  void testUpdateDatasetWithNoProperties() {
    Dataset dataset = new Dataset();
    FormDataContentDisposition content = FormDataContentDisposition
        .name("file")
        .fileName("validFile.txt")
        .build();

    FormDataBodyPart formDataBodyPart = mock(FormDataBodyPart.class);
    when(formDataBodyPart.getContentDisposition()).thenReturn(content);

    FormDataMultiPart formDataMultiPart = mock(FormDataMultiPart.class);
    when(formDataMultiPart.getFields()).thenReturn(Map.of("file", List.of(formDataBodyPart)));
    when(datasetService.findDatasetById(any())).thenReturn(dataset);
    initResource();
    Response response = resource.updateByDatasetUpdate(authUser, 1, formDataMultiPart,
        "{\"properties\":[]}");
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  void testUpdateDatasetWIthDatasetIdNotFound() {
    FormDataContentDisposition content = FormDataContentDisposition
        .name("file")
        .fileName("validFile.txt")
        .build();
    FormDataMultiPart formDataMultiPart = mock(FormDataMultiPart.class);
    String json = createDatasetRegistrationMock(user);
    when(datasetService.findDatasetById(anyInt())).thenReturn(null);

    initResource();
    Response response = resource.updateByDatasetUpdate(authUser, 1, formDataMultiPart, json);
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  @Test
  void testUpdateDatasetInvalidFileName() {
    Dataset preexistingDataset = new Dataset();
    when(datasetService.findDatasetById(anyInt())).thenReturn(preexistingDataset);
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(user.getUserId()).thenReturn(1);
    String json = createDatasetRegistrationMock(user);

    FormDataContentDisposition content = FormDataContentDisposition
        .name("file")
        .fileName("\"file/with&$invalid*^chars\\\\.txt\"")
        .build();

    FormDataBodyPart formDataBodyPart = mock(FormDataBodyPart.class);
    when(formDataBodyPart.getContentDisposition()).thenReturn(content);

    FormDataMultiPart formDataMultiPart = mock(FormDataMultiPart.class);
    when(formDataMultiPart.getFields()).thenReturn(Map.of("file", List.of(formDataBodyPart)));
    initResource();

    Response response = resource.updateByDatasetUpdate(authUser, 1, formDataMultiPart, json);
    assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
  }

  @Test
  void testSyncDataUseTranslation() {
    when(datasetService.syncDatasetDataUseTranslation(any())).thenReturn(new Dataset());
    initResource();

    Response response = resource.syncDataUseTranslation(authUser, 1);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  void testSyncDataUseTranslationNotFound() {
    when(datasetService.syncDatasetDataUseTranslation(any())).thenThrow(new NotFoundException());
    initResource();

    Response response = resource.syncDataUseTranslation(authUser, 1);
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  /**
   * Helper method to create a minimally valid instance of a dataset registration schema
   *
   * @param user The User
   * @return The DatasetRegistrationSchemaV1.yaml instance
   */
  private String createDatasetRegistrationMock(User user) {
    String format = """
        {
          "studyType": "Observational",
          "studyName": "name",
          "studyDescription": "description",
          "dataTypes": ["types"],
          "phenotypeIndication": "",
          "species": "species",
          "piName": "PI Name",
          "nihAnvilUse": "I am not NHGRI funded and do not plan to store data in AnVIL",
          "dataSubmitterUserId": %s,
          "dataCustodianEmail": ["email@abc.com"],
          "publicVisibility": true,
          "consentGroups": [{
            "fileTypes": [{
              "fileType": "Arrays",
              "functionalEquivalence": "equivalence"
            }],
            "numberOfParticipants": 2,
            "consentGroupName": "name",
            "generalResearchUse": true,
            "dataAccessCommitteeId": 1,
            "url": "https://asdf.com"
          }],
          "embargoReleaseDate": "1234-10-10"
        }
        """;

    return String.format(format, user.getUserId());
  }

  /**
   * Helper method to create a minimally valid instance of a dataset for updating dataset
   *
   * @param user The User
   * @return The Dataset instance
   */
  private String createDataset(User user) {
    String format = """
        {
          "dataSetId": 2,
          "objectId": "SC-10985",
          "name": "Herman Taylor (U. Miss Med Center) - Jackson Heart Study",
          "createDate": "Mar 21, 2019",
          "active": true,
          "alias": 3,
          "datasetIdentifier": "DUOS-000003",
          "dataUse": {
            "diseaseRestrictions": [
              "http://purl.obolibrary.org/obo/DOID_602",
              "http://purl.obolibrary.org/obo/DOID_9351"
            ],
            "populationOriginsAncestry": true,
            "commercialUse": false,
            "controlSetOption": "No",
            "gender": "Female",
            "pediatric": true
          },
          "dacId": 5,
          "consentId": "eac1d4f9-78c9-4c88-9b10-9d692e171b5b",
          "deletable": false,
          "properties": [
            {
              "dataSetId": 2,
              "propertyName": "test",
              "propertyValue": "John Doe",
              "propertyType": "String"
            },
          ],
          "dacApproval": true,
          "createUser": {},
          "study": {
            "datasetIds": [
              null
            ]
          }
        }
        """;

    return String.format(format, user.getUserId());
  }

  /**
   * Helper method to create a minimally invalid instance of a dataset for updating dataset
   *
   * @param user The User
   * @return The Dataset instance
   */
  private String createInvalidDataset(User user) {
    String format = """
        {
          "dataSetId": 2,
        }
        """;

    return String.format(format, user.getUserId());
  }

  /*
   * Study mock
   */
  private Study createMockStudy() {
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
    study.setCreateUserEmail(RandomStringUtils.randomAlphabetic(10));
    study.setPublicVisibility(true);
    study.setDatasetIds(Set.of(dataset.getDataSetId()));

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
    dataCustodianEmailProperty.setType(PropertyType.Json);
    dataCustodianEmailProperty.setValue(List.of(RandomStringUtils.randomAlphabetic(10)));

    study.setProperties(Set.of(phenotypeProperty, speciesProperty, dataCustodianEmailProperty));

    dataset.setStudy(study);

    DatasetProperty accessManagementProp = new DatasetProperty();
    accessManagementProp.setSchemaProperty("accessManagement");
    accessManagementProp.setPropertyType(PropertyType.String);
    accessManagementProp.setPropertyValue(AccessManagement.OPEN.value());

    DatasetProperty dataLocationProp = new DatasetProperty();
    dataLocationProp.setSchemaProperty("dataLocation");
    dataLocationProp.setPropertyType(PropertyType.String);
    dataLocationProp.setPropertyValue(DataLocation.NOT_DETERMINED.value());

    DatasetProperty numParticipantsProp = new DatasetProperty();
    numParticipantsProp.setSchemaProperty("numberOfParticipants");
    numParticipantsProp.setPropertyType(PropertyType.Number);
    numParticipantsProp.setPropertyValue(20);

    dataset.setProperties(Set.of(accessManagementProp, dataLocationProp, numParticipantsProp));
    study.addDatasets(List.of(dataset));

    return study;
  }
}
