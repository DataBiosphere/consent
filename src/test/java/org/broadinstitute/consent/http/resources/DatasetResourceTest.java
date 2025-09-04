package org.broadinstitute.consent.http.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.client.http.HttpStatusCodes;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.broadinstitute.consent.http.AbstractTestHelper;
import org.broadinstitute.consent.http.enumeration.PropertyType;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetAuthorizationReader;
import org.broadinstitute.consent.http.models.DatasetPatch;
import org.broadinstitute.consent.http.models.DatasetProperty;
import org.broadinstitute.consent.http.models.DatasetSummary;
import org.broadinstitute.consent.http.models.DuosUser;
import org.broadinstitute.consent.http.models.Error;
import org.broadinstitute.consent.http.models.Study;
import org.broadinstitute.consent.http.models.StudyProperty;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.models.dataset_registration_v1.ConsentGroup.AccessManagement;
import org.broadinstitute.consent.http.models.dataset_registration_v1.ConsentGroup.DataLocation;
import org.broadinstitute.consent.http.models.dataset_registration_v1.DatasetRegistrationSchemaV1;
import org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder;
import org.broadinstitute.consent.http.service.DatasetRegistrationService;
import org.broadinstitute.consent.http.service.DatasetService;
import org.broadinstitute.consent.http.service.ElasticSearchService;
import org.broadinstitute.consent.http.service.TDRService;
import org.broadinstitute.consent.http.service.UserService;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DatasetResourceTest extends AbstractTestHelper {

  @Mock
  private DatasetService datasetService;
  @Mock
  private DatasetRegistrationService datasetRegistrationService;

  @Mock
  private ElasticSearchService elasticSearchService;

  @Mock
  private TDRService tdrService;

  @Mock
  private UserService userService;

  @Mock
  private Response mockResponse;

  private final AuthUser authUser = new AuthUser().setEmail("test@test.com");
  private final User user = new User();
  private final DuosUser duosUser = new DuosUser(authUser, user);
  private DatasetResource resource;

  @BeforeEach
  void initResource() {
    resource = new DatasetResource(datasetService, userService,
        datasetRegistrationService, elasticSearchService, tdrService);
  }

  @Test
  void testPatchByDatasetUpdate_emptyInput() {
    Dataset dataset = new Dataset();
    dataset.setDatasetId(randomInt(1, 100));
    dataset.setName(randomAlphabetic(10));
    dataset.setCreateUserId(user.getUserId());
    when(datasetService.findDatasetById(any(), any())).thenReturn(dataset);

    try (Response response = resource.patchByDatasetUpdate(duosUser, 1, "")) {
      assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    }
  }

  @Test
  void testPatchByDatasetUpdate_malformedInput() {
    Dataset dataset = new Dataset();
    dataset.setDatasetId(randomInt(1, 100));
    dataset.setName(randomAlphabetic(10));
    dataset.setCreateUserId(user.getUserId());
    when(datasetService.findDatasetById(any(), any())).thenReturn(dataset);

    try (Response response = resource.patchByDatasetUpdate(duosUser, 1, "}{")) {
      assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    }
  }

  @Test
  void testPatchByDatasetUpdate_datasetNotFound() {
    try (Response response = resource.patchByDatasetUpdate(duosUser, 1, "{}")) {
      assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
    }
  }

  @Test
  void testPatchByDatasetUpdate_userNotFound() {
    try (Response response = resource.patchByDatasetUpdate(duosUser, 1, "{}")) {
      assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
    }
  }

  @Test
  void testPatchByDatasetUpdate_userForbiddenException() {
    Dataset dataset = new Dataset();
    dataset.setDatasetId(randomInt(1, 10));

    when(datasetService.findDatasetById(any(), any())).thenReturn(dataset);
    user.setUserId(randomInt(1, 10));
    // This ensures the dataset create user is NOT the current duosUser
    dataset.setCreateUserId(randomInt(100, 200));

    try (Response response = resource.patchByDatasetUpdate(duosUser, 1, "{}")) {
      assertEquals(HttpStatusCodes.STATUS_CODE_FORBIDDEN, response.getStatus());
    }
  }

  @Test
  void testPatchByDatasetUpdate_notModified() {
    Gson gson = GsonUtil.buildGson();

    Dataset dataset = new Dataset();
    dataset.setDatasetId(randomInt(1, 100));
    dataset.setName(randomAlphabetic(10));

    DatasetProperty dataLocationProp = new DatasetProperty();
    dataLocationProp.setPropertyName("data location");
    dataLocationProp.setSchemaProperty(DatasetRegistrationSchemaV1Builder.dataLocation);
    dataLocationProp.setPropertyType(PropertyType.String);
    dataLocationProp.setPropertyValue(DataLocation.NOT_DETERMINED.value());
    dataset.setProperties(Set.of(dataLocationProp));

    when(datasetService.findDatasetById(any(), any())).thenReturn(dataset);
    DatasetPatch patch = new DatasetPatch(dataset.getDatasetName(),
        dataset.getProperties().stream().toList());

    user.setUserId(randomInt(1, 100));
    dataset.setCreateUserId(user.getUserId());

    try (Response response = resource.patchByDatasetUpdate(duosUser, dataset.getDatasetId(),
        gson.toJson(patch))) {
      assertEquals(HttpStatusCodes.STATUS_CODE_NOT_MODIFIED, response.getStatus());
    }
  }

  @Test
  void testPatchByDatasetUpdate_nonUniqueName() {
    Gson gson = GsonUtil.buildGson();

    Dataset dataset = new Dataset();
    dataset.setDatasetId(randomInt(1, 100));
    dataset.setName(randomAlphabetic(10));

    DatasetProperty dataLocationProp = new DatasetProperty();
    dataLocationProp.setPropertyName("data location");
    dataLocationProp.setSchemaProperty(DatasetRegistrationSchemaV1Builder.dataLocation);
    dataLocationProp.setPropertyType(PropertyType.String);
    dataLocationProp.setPropertyValue(DataLocation.NOT_DETERMINED.value());
    dataset.setProperties(Set.of(dataLocationProp));

    when(datasetService.findDatasetById(any(), any())).thenReturn(dataset);

    DatasetProperty patchProp = new DatasetProperty();
    patchProp.setPropertyName("data location");
    patchProp.setSchemaProperty(DatasetRegistrationSchemaV1Builder.dataLocation);
    patchProp.setPropertyType(PropertyType.String);
    patchProp.setPropertyValue(DataLocation.TDR_LOCATION.value());
    DatasetPatch patch = new DatasetPatch(randomAlphabetic(20), List.of(patchProp));
    dataset.setCreateUserId(user.getUserId());
    when(datasetService.findAllDatasetNames()).thenReturn(List.of(dataset.getName(), patch.name()));

    try (Response response = resource.patchByDatasetUpdate(duosUser, dataset.getDatasetId(),
        gson.toJson(patch))) {
      assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    }
  }

  @Test
  void testPatchByDatasetUpdate_patchable() throws Exception {
    Gson gson = GsonUtil.buildGson();

    Dataset dataset = new Dataset();
    dataset.setDatasetId(randomInt(1, 100));
    dataset.setName(randomAlphabetic(10));

    DatasetProperty dataLocationProp = new DatasetProperty();
    dataLocationProp.setPropertyName("data location");
    dataLocationProp.setSchemaProperty(DatasetRegistrationSchemaV1Builder.dataLocation);
    dataLocationProp.setPropertyType(PropertyType.String);
    dataLocationProp.setPropertyValue(DataLocation.NOT_DETERMINED.value());
    dataset.setProperties(Set.of(dataLocationProp));

    when(datasetService.findDatasetById(any(), any())).thenReturn(dataset);

    DatasetProperty patchProp = new DatasetProperty();
    patchProp.setPropertyName("data location");
    patchProp.setSchemaProperty(DatasetRegistrationSchemaV1Builder.dataLocation);
    patchProp.setPropertyType(PropertyType.String);
    patchProp.setPropertyValue(DataLocation.TDR_LOCATION.value());

    DatasetPatch patch = new DatasetPatch(randomAlphabetic(20), List.of(patchProp));

    when(datasetRegistrationService.patchDataset(any(), any(), any())).thenReturn(dataset);
    user.setUserId(randomInt(1, 100));
    dataset.setCreateUserId(user.getUserId());

    try (Response response = resource.patchByDatasetUpdate(duosUser, dataset.getDatasetId(),
        gson.toJson(patch))) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
      verify(elasticSearchService, never()).indexDataset(dataset.getDatasetId(), user);
    }
  }

  @Test
  void testPatchByDatasetUpdate_patchableNoName() {
    Gson gson = GsonUtil.buildGson();

    Dataset dataset = new Dataset();
    dataset.setDatasetId(randomInt(1, 100));
    dataset.setName(randomAlphabetic(10));

    DatasetProperty dataLocationProp = new DatasetProperty();
    dataLocationProp.setPropertyName("data location");
    dataLocationProp.setSchemaProperty(DatasetRegistrationSchemaV1Builder.dataLocation);
    dataLocationProp.setPropertyType(PropertyType.String);
    dataLocationProp.setPropertyValue(DataLocation.NOT_DETERMINED.value());
    dataset.setProperties(Set.of(dataLocationProp));

    when(datasetService.findDatasetById(any(), any())).thenReturn(dataset);

    DatasetProperty patchProp = new DatasetProperty();
    patchProp.setPropertyName("data location");
    patchProp.setSchemaProperty(DatasetRegistrationSchemaV1Builder.dataLocation);
    patchProp.setPropertyType(PropertyType.String);
    patchProp.setPropertyValue(DataLocation.TDR_LOCATION.value());

    // Name is nullable and will not be updated if not provided
    DatasetPatch patch = new DatasetPatch(null, List.of(patchProp));

    when(datasetRegistrationService.patchDataset(any(), any(), any())).thenReturn(dataset);
    dataset.setCreateUserId(user.getUserId());

    try (Response response = resource.patchByDatasetUpdate(duosUser, dataset.getDatasetId(),
        gson.toJson(patch))) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }
  }

  @Test
  void testPatchByDatasetUpdate_invokeIndexUpdate() {
    Gson gson = GsonUtil.buildGson();

    Dataset dataset = new Dataset();
    dataset.setDatasetId(randomInt(1, 100));
    dataset.setName(randomAlphabetic(10));
    dataset.setIndexedDate(new Date());
    when(datasetService.findDatasetById(any(), any())).thenReturn(dataset);
    String newName = randomAlphabetic(20);
    DatasetPatch patch = new DatasetPatch(newName, List.of());

    when(datasetRegistrationService.patchDataset(any(), any(), any())).thenReturn(dataset);
    dataset.setCreateUserId(user.getUserId());

    try (Response response = resource.patchByDatasetUpdate(duosUser, dataset.getDatasetId(),
        gson.toJson(patch))) {
      verify(elasticSearchService).synchronizeDatasetInESIndex(dataset, user, false);
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }
  }

  @Test
  void testPatchByDatasetUpdate_invalidPatchProperties() {
    Gson gson = GsonUtil.buildGson();

    Dataset dataset = new Dataset();
    dataset.setDatasetId(randomInt(1, 100));
    dataset.setName(randomAlphabetic(10));

    DatasetProperty invalidProp = new DatasetProperty();
    invalidProp.setPropertyName("invalid");
    invalidProp.setSchemaProperty(DatasetRegistrationSchemaV1Builder.accessManagement);
    invalidProp.setPropertyType(PropertyType.String);
    invalidProp.setPropertyValue(AccessManagement.OPEN.value());
    dataset.setProperties(Set.of(invalidProp));

    when(datasetService.findDatasetById(any(), any())).thenReturn(dataset);

    DatasetProperty patchProp = new DatasetProperty();
    patchProp.setPropertyName("invalid");
    patchProp.setSchemaProperty(DatasetRegistrationSchemaV1Builder.accessManagement);
    patchProp.setPropertyType(PropertyType.String);
    patchProp.setPropertyValue(AccessManagement.CONTROLLED.value());

    DatasetPatch patch = new DatasetPatch(randomAlphabetic(20), List.of(patchProp));
    dataset.setCreateUserId(user.getUserId());

    try (Response response = resource.patchByDatasetUpdate(duosUser, dataset.getDatasetId(),
        gson.toJson(patch))) {
      assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    }
  }

  @Test
  void testValidateDatasetNameSuccess() {
    Dataset testDataset = new Dataset();
    when(datasetService.getDatasetByName("test")).thenReturn(testDataset);

    try (var response = resource.validateDatasetName("test")) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }
  }

  @Test
  void testValidateDatasetNameNotFound() {
    assertThrows(NotFoundException.class, () -> {
      try (var response = resource.validateDatasetName("test")) {
        fail("Should not get to this point");
      }
    });
  }

  @Test
  void testFindAllStudyNamesSuccess() {
    when(datasetService.findAllStudyNames()).thenReturn(Set.of("Hi", "Hello"));
    try (var response = resource.findAllStudyNames()) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }
  }

  @Test
  void testFindAllStudyNamesFail() {
    when(datasetService.findAllStudyNames()).thenThrow();
    try (var response = resource.findAllStudyNames()) {
      assertEquals(HttpStatusCodes.STATUS_CODE_SERVER_ERROR, response.getStatus());
    }
  }

  @Test
  void testDeleteSuccessAdmin() throws Exception {
    Dataset dataSet = new Dataset();

    user.addRole(UserRoles.Admin());
    when(datasetService.findDatasetById(any(), any())).thenReturn(dataSet);
    when(elasticSearchService.deleteIndex(any(), any())).thenReturn(mockResponse);

    try (var response = resource.delete(duosUser, 1)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }
  }

  @Test
  void testDeleteSuccessChairperson() throws Exception {
    Dataset dataSet = new Dataset();
    dataSet.setDatasetId(1);
    dataSet.setDacId(1);

    UserRole role = UserRoles.Chairperson();
    role.setDacId(1);
    user.addRole(role);

    when(datasetService.findDatasetById(any(), any())).thenReturn(dataSet);
    when(elasticSearchService.deleteIndex(any(), any())).thenReturn(mockResponse);

    try (var response = resource.delete(duosUser, 1)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }
  }

  @Test
  void testDeleteErrorNoDacIds() {
    Dataset dataSet = new Dataset();

    UserRole role = UserRoles.Chairperson();
    user.addRole(role);

    when(datasetService.findDatasetById(any(), any())).thenReturn(dataSet);

    try (var response = resource.delete(duosUser, 1)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
    }
  }

  @Test
  void testDeleteErrorNullConsent() {
    Dataset dataset = new Dataset();
    dataset.setDatasetId(1);

    UserRole role = UserRoles.Chairperson();
    role.setDacId(1);
    user.addRole(role);

    when(datasetService.findDatasetById(duosUser.getUser(), dataset.getDatasetId())).thenReturn(dataset);

    try (var response = resource.delete(duosUser, 1)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
    }
  }

  @Test
  void testDeleteErrorMismatch() {
    Dataset dataSet = new Dataset();
    dataSet.setDatasetId(1);
    dataSet.setDacId(2);

    UserRole role = UserRoles.Chairperson();
    role.setDacId(1);
    user.addRole(role);

    when(datasetService.findDatasetById(any(), any())).thenReturn(dataSet);

    try (var response = resource.delete(duosUser, 1)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
    }
  }

  @Test
  void testIndexAllDatasets() throws Exception {
    Dataset dataset = new Dataset();
    dataset.setDatasetId(randomInt(10, 100));
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
        esResponseArray.formatted(dataset.getDatasetId()).getBytes());
    when(datasetService.findAllDatasetIds()).thenReturn(List.of(dataset.getDatasetId()));
    when(elasticSearchService.indexDatasetIds(List.of(dataset.getDatasetId()), user)).thenReturn(
        output);
    when(userService.findUserByEmail(duosUser.getEmail())).thenReturn(user);

    try (Response response = resource.indexDatasets(duosUser)) {
      var entity = (StreamingOutput) response.getEntity();
      var baos = new ByteArrayOutputStream();
      entity.write(baos);
      var entityString = baos.toString();
      List<JsonObject> responseList = gson.fromJson(entityString, new TypeToken<>() {});
      assertEquals(1, responseList.size());
      JsonArray items = responseList.get(0).getAsJsonArray("items");
      assertEquals(1, items.size());
      assertEquals(
          dataset.getDatasetId(),
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
    when(mockResponse.getStatus()).thenReturn(HttpStatusCodes.STATUS_CODE_OK);
    when(elasticSearchService.indexDataset(dataset.getDatasetId(), user)).thenReturn(mockResponse);
    when(userService.findUserByEmail(authUser.getEmail())).thenReturn(user);

    try (var response = resource.indexDataset(authUser, dataset.getDatasetId())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }
  }

  @Test
  void testIndexDelete() throws IOException {
    when(mockResponse.getStatus()).thenReturn(HttpStatusCodes.STATUS_CODE_OK);
    when(elasticSearchService.deleteIndex(any(), any())).thenReturn(mockResponse);
    when(userService.findUserByEmail(any())).thenReturn(user);

    try (var response = resource.deleteDatasetIndex(authUser, 0)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }
  }

  @Test
  void testAutocompleteDatasets() {
    when(datasetService.searchDatasetSummaries(any())).thenReturn(
        List.of(new DatasetSummary(1, "ID", "Name")));

    try (var response = resource.autocompleteDatasets(authUser, "test")) {
      assertTrue(HttpStatusCodes.isSuccess(response.getStatus()));
    }
  }

  @Test
  void testSearchDatasetIndex() throws IOException {
    String query = "{ \"dataUse\": [\"HMB\"] }";

    when(mockResponse.getStatus()).thenReturn(HttpStatusCodes.STATUS_CODE_OK);
    when(mockResponse.getEntity()).thenReturn(query);
    when(elasticSearchService.searchDatasets(any())).thenReturn(mockResponse);

    try (var response = resource.searchDatasetIndex(authUser, query)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
      assertTrue(response.getEntity().toString().length() > 2);
    }
  }

  @Test
  void testGetDataset() {
    Dataset ds = new Dataset();
    ds.setDatasetId(1);
    ds.setName("asdfasdfasdfasdfasdfasdf");
    when(datasetService.findDatasetById(duosUser.getUser(), 1)).thenReturn(ds);

    Response response = resource.getDataset(duosUser, 1);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    assertEquals(ds, response.getEntity());
  }

  @Test
  void testGetDatasetNotFound() {
    when(datasetService.findDatasetById(duosUser.getUser(), 1)).thenReturn(null);

    Response response = resource.getDataset(duosUser, 1);
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  @Test
  void testGetDatasets() {
    Dataset ds1 = new Dataset();
    ds1.setDatasetId(1);
    Dataset ds2 = new Dataset();
    ds2.setDatasetId(2);
    Dataset ds3 = new Dataset();
    ds3.setDatasetId(3);
    List<Dataset> datasets = List.of(ds1, ds2, ds3);

    when(datasetService.findDatasetsByIds(duosUser.getUser(), List.of(1, 2, 3))).thenReturn(datasets);

    Response response = resource.getDatasets(duosUser, List.of(1, 2, 3));
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    assertEquals(datasets, response.getEntity());
  }

  @Test
  void testGetDatasetsDuplicates() {
    Dataset ds1 = new Dataset();
    ds1.setDatasetId(1);
    Dataset ds2 = new Dataset();
    ds2.setDatasetId(2);
    Dataset ds3 = new Dataset();
    ds3.setDatasetId(3);
    List<Dataset> datasets = List.of(ds1, ds2, ds3);

    when(datasetService.findDatasetsByIds(duosUser.getUser(), List.of(1, 1, 2, 2, 3, 3))).thenReturn(datasets);

    Response response = resource.getDatasets(duosUser, List.of(1, 1, 2, 2, 3, 3));
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    assertEquals(datasets, response.getEntity());
  }

  @Test
  void testGetDatasetsDuplicatesNotFound() {
    Dataset ds1 = new Dataset();
    ds1.setDatasetId(1);
    Dataset ds2 = new Dataset();
    ds2.setDatasetId(2);

    when(datasetService.findDatasetsByIds(duosUser.getUser(), List.of(1, 1, 2, 2, 3, 3))).thenReturn(List.of(
        ds1,
        ds2
    ));

    Response response = resource.getDatasets(duosUser, List.of(1, 1, 2, 2, 3, 3));
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
    assertTrue(((Error) response.getEntity()).message().contains("3"));
    assertFalse(((Error) response.getEntity()).message().contains("2"));
    assertFalse(((Error) response.getEntity()).message().contains("1"));

  }

  @Test
  void testGetDatasetsNotFound() {
    Dataset ds1 = new Dataset();
    ds1.setDatasetId(1);
    Dataset ds3 = new Dataset();
    ds3.setDatasetId(3);

    when(datasetService.findDatasetsByIds(duosUser.getUser(), List.of(1, 2, 3, 4))).thenReturn(List.of(
        ds1,
        ds3
    ));

    Response response = resource.getDatasets(duosUser, List.of(1, 2, 3, 4));
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
    assertTrue(((Error) response.getEntity()).message().contains("4"));
    assertFalse(((Error) response.getEntity()).message().contains("3"));
    assertTrue(((Error) response.getEntity()).message().contains("2"));
    assertFalse(((Error) response.getEntity()).message().contains("1"));
  }

  @Test
  void testGetDatasetsNotFoundNullValues() {
    Dataset ds1 = new Dataset();
    ds1.setDatasetId(1);
    Dataset ds3 = new Dataset();
    ds3.setDatasetId(3);

    List<Integer> input = new ArrayList<>(List.of(1, 2, 3, 4));
    input.add(null);
    when(datasetService.findDatasetsByIds(duosUser.getUser(), input)).thenReturn(List.of(
        ds1,
        ds3
    ));

    Response response = resource.getDatasets(duosUser, input);
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
    assertTrue(((Error) response.getEntity()).message().contains("4"));
    assertFalse(((Error) response.getEntity()).message().contains("3"));
    assertTrue(((Error) response.getEntity()).message().contains("2"));
    assertFalse(((Error) response.getEntity()).message().contains("1"));
  }


  @Test
  void testUpdateDatasetDataUse_OK() {
    Dataset d = new Dataset();
    when(datasetService.findDatasetById(any(), any())).thenReturn(d);
    when(datasetService.updateDatasetDataUse(any(), any(), any())).thenReturn(d);

    String duString = new DataUseBuilder().setGeneralUse(true).build().toString();
    try (var response = resource.updateDatasetDataUse(new AuthUser(), 1, duString)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }
  }

  @Test
  void testUpdateDatasetDataUse_BadRequestJson() {
    try (var response = resource.updateDatasetDataUse(new AuthUser(), 1, "invalid json")) {
      assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    }
  }

  @Test
  void testUpdateDatasetDataUse_BadRequestService() {
    Dataset d = new Dataset();
    when(datasetService.findDatasetById(any(), any())).thenReturn(d);
    when(datasetService.updateDatasetDataUse(any(), any(), any())).thenThrow(
        new IllegalArgumentException());

    String duString = new DataUseBuilder().setGeneralUse(true).build().toString();
    try (var response = resource.updateDatasetDataUse(new AuthUser(), 1, duString)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    }
  }

  @Test
  void testUpdateDatasetDataUse_NotFound() {
    when(datasetService.findDatasetById(any(), any())).thenThrow(new NotFoundException());

    String duString = new DataUseBuilder().setGeneralUse(true).build().toString();
    try (var response = resource.updateDatasetDataUse(new AuthUser(), 1, duString)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
    }
  }

  @Test
  void testUpdateDatasetDataUse_NotModified() {
    Dataset d = new Dataset();
    DataUse du = new DataUseBuilder().setGeneralUse(true).build();
    d.setDataUse(du);
    when(datasetService.findDatasetById(any(), any())).thenReturn(d);

    try (var response = resource.updateDatasetDataUse(new AuthUser(), 1, du.toString())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_NOT_MODIFIED, response.getStatus());
    }
  }

  @Test
  void testFindAllDatasetStudySummaries() {
    when(datasetService.findAllDatasetStudySummaries()).thenReturn(List.of());

    try (var response = resource.findAllDatasetStudySummaries(authUser)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }
  }

  @Test
  void testCreateDatasetRegistration_invalidSchema_case1() {
    try (var response = resource.createDatasetRegistration(authUser, null, "")) {
      assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    }
  }

  @Test
  void testCreateDatasetRegistration_invalidSchema_case2() {
    try (var response = resource.createDatasetRegistration(authUser, null, "{}")) {
      assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    }
  }

  @Test
  void testCreateDatasetRegistration_invalidSchema_case3() {
    DatasetRegistrationSchemaV1 schemaV1 = new DatasetRegistrationSchemaV1();
    String schemaString = new Gson().toJson(schemaV1);

    try (var response = resource.createDatasetRegistration(authUser, null, schemaString)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    }
  }

  @Test
  void testCreateDatasetRegistration_validSchema() throws SQLException, IOException {
    when(userService.findUserByEmail(any())).thenReturn(user);
    user.setUserId(1);
    Dataset dataset = new Dataset();
    Study study = new Study();
    study.setStudyId(1);
    dataset.setStudy(study);
    when(datasetRegistrationService.createDatasetsFromRegistration(any(), any(), any())).thenReturn(
        List.of(dataset));
    String schemaV1 = createDatasetRegistrationMock(user);

    try (var response = resource.createDatasetRegistration(authUser, null, schemaV1)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_CREATED, response.getStatus());
    }
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
    user.setUserId(1);
    Dataset dataset = new Dataset();
    Study study = new Study();
    study.setStudyId(1);
    dataset.setStudy(study);
    when(datasetRegistrationService.createDatasetsFromRegistration(any(), any(), any())).thenReturn(
        List.of(dataset));
    String schemaV1 = createDatasetRegistrationMock(user);

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
    user.setUserId(1);
    Dataset dataset = new Dataset();
    Study study = new Study();
    study.setStudyId(1);
    dataset.setStudy(study);
    when(datasetRegistrationService.createDatasetsFromRegistration(any(), any(), any())).thenReturn(
        List.of(dataset));
    String schemaV1 = createDatasetRegistrationMock(user);

    Response response = resource.createDatasetRegistration(authUser, formDataMultiPart, schemaV1);

    assertEquals(HttpStatusCodes.STATUS_CODE_CREATED, response.getStatus());
    verify(datasetRegistrationService, times(1)).createDatasetsFromRegistration(
        any(),
        eq(user),
        eq(Map.of("file", formDataBodyPartFile, "other", formDataBodyPartOther)));

  }

  @Test
  void testCreateDatasetRegistration_invalidFileName() {
    FormDataMultiPart formDataMultiPart = mock(FormDataMultiPart.class);
    String schemaV1 = createDatasetRegistrationMock(user);

    Response response = resource.createDatasetRegistration(authUser, formDataMultiPart, schemaV1);
    assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
  }

  @Test
  void testGetRegistrationFromDatasetIdentifier() {
    Study study = createMockStudy();
    Dataset dataset = study.getDatasets().stream().findFirst().orElse(null);
    assertNotNull(dataset);
    when(datasetService.findDatasetByIdentifier(any(), any())).thenReturn(dataset);

    Response response = resource.getRegistrationFromDatasetIdentifier(authUser,
        dataset.getDatasetIdentifier());
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  void testGetRegistrationFromDatasetIdentifierStudyNotFound() {
    Dataset dataset = createMockDataset();
    assertNotNull(dataset);
    when(datasetService.findDatasetByIdentifier(any(), any())).thenReturn(dataset);

    Response response = resource.getRegistrationFromDatasetIdentifier(authUser,
        dataset.getDatasetIdentifier());
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  @Test
  void testGetRegistrationFromDatasetIdentifierDatasetNotFound() {
    Study study = createMockStudy();
    Dataset dataset = study.getDatasets().stream().findFirst().orElse(null);
    assertNotNull(dataset);
    when(datasetService.findDatasetByIdentifier(any(), any())).thenReturn(null);

    Response response = resource.getRegistrationFromDatasetIdentifier(authUser,
        dataset.getDatasetIdentifier());
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  @Test
  void testUpdateDatasetByDatasetIntakeSuccess() throws SQLException, IOException {
    Dataset preexistingDataset = new Dataset();
    when(datasetService.findDatasetById(any(), anyInt())).thenReturn(preexistingDataset);
    when(datasetRegistrationService.updateDataset(any(), any(), any(), any())).thenReturn(
        preexistingDataset);
    String json = createDataset(user);

    FormDataContentDisposition content = FormDataContentDisposition
        .name("file")
        .fileName("validFile.txt")
        .build();

    FormDataBodyPart formDataBodyPart = mock(FormDataBodyPart.class);
    when(formDataBodyPart.getContentDisposition()).thenReturn(content);

    FormDataMultiPart formDataMultiPart = mock(FormDataMultiPart.class);
    when(formDataMultiPart.getFields()).thenReturn(Map.of("file", List.of(formDataBodyPart)));

    try (var response = resource.updateByDatasetUpdate(duosUser, 1, formDataMultiPart, json)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
      assertEquals(Optional.of(preexistingDataset).get(), response.getEntity());
    }
  }

  @Test
  void testUpdateDatasetWithNoJson() {
    FormDataMultiPart formDataMultiPart = mock(FormDataMultiPart.class);

    try (var response = resource.updateByDatasetUpdate(duosUser, 1, formDataMultiPart, "")) {
      assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    }
  }

  @Test
  void testUpdateDatasetWithInvalidJson() {
    String json = createInvalidDataset(user);
    FormDataMultiPart formDataMultiPart = mock(FormDataMultiPart.class);

    try (var response = resource.updateByDatasetUpdate(duosUser, 1, formDataMultiPart, json)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    }
  }

  /**
   * tests the case that there are no updates to the dataset properties, should result in success
   */
  @Test
  void testUpdateDatasetWithNoProperties() throws Exception {
    Dataset dataset = new Dataset();
    FormDataContentDisposition content = FormDataContentDisposition
        .name("file")
        .fileName("validFile.txt")
        .build();

    FormDataBodyPart formDataBodyPart = mock(FormDataBodyPart.class);
    when(formDataBodyPart.getContentDisposition()).thenReturn(content);

    FormDataMultiPart formDataMultiPart = mock(FormDataMultiPart.class);
    when(formDataMultiPart.getFields()).thenReturn(Map.of("file", List.of(formDataBodyPart)));
    when(datasetService.findDatasetById(any(), any())).thenReturn(dataset);
    when(datasetRegistrationService.updateDataset(any(), any(), any(), any())).thenReturn(dataset);

    try (var response = resource.updateByDatasetUpdate(duosUser, 1, formDataMultiPart,
        "{\"properties\":[]}")) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }
  }

  @Test
  void testUpdateDatasetWIthDatasetIdNotFound() {
    FormDataMultiPart formDataMultiPart = mock(FormDataMultiPart.class);
    String json = createDatasetRegistrationMock(user);
    when(datasetService.findDatasetById(any(), anyInt())).thenReturn(null);

    try (var response = resource.updateByDatasetUpdate(duosUser, 1, formDataMultiPart, json)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
    }
  }

  @Test
  void testUpdateDatasetInvalidFileName() {
    Dataset preexistingDataset = new Dataset();
    when(datasetService.findDatasetById(any(), anyInt())).thenReturn(preexistingDataset);
    String json = createDatasetRegistrationMock(user);

    FormDataContentDisposition content = FormDataContentDisposition
        .name("file")
        .fileName("\"file/with&$invalid*^chars\\\\.txt\"")
        .build();

    FormDataBodyPart formDataBodyPart = mock(FormDataBodyPart.class);
    when(formDataBodyPart.getContentDisposition()).thenReturn(content);

    FormDataMultiPart formDataMultiPart = mock(FormDataMultiPart.class);
    when(formDataMultiPart.getFields()).thenReturn(Map.of("file", List.of(formDataBodyPart)));

    try (var response = resource.updateByDatasetUpdate(duosUser, 1, formDataMultiPart, json)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    }
  }

  @Test
  void testSyncDataUseTranslation() {
    when(datasetService.syncDatasetDataUseTranslation(any(), any())).thenReturn(new Dataset());

    try (var response = resource.syncDataUseTranslation(authUser, 1)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }
  }

  @Test
  void testSyncDataUseTranslationNotFound() {
    when(datasetService.syncDatasetDataUseTranslation(any(), any())).thenThrow(
        new NotFoundException());

    try (var response = resource.syncDataUseTranslation(authUser, 1)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
    }
  }

  @Test
  void testGetAuthorizedReadersOK() {
    when(datasetService.getAuthorizationReaders(anyLong())).thenReturn(new ArrayList<>());
    Response response = resource.getAuthorizedReaders(duosUser,1L);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }


  @Test
  void testGetAuthorizedReadersError() {
    doThrow(new RuntimeException("Some Exception"))
        .when(datasetService)
        .getAuthorizationReaders(anyLong());
    Response response = resource.getAuthorizedReaders(duosUser,1L);
    assertEquals(HttpStatusCodes.STATUS_CODE_SERVER_ERROR, response.getStatus());
  }

  @Test
  void testAddAuthorizedReadersNotServiceAccount(){
    User readerUser = new User();
    readerUser.setUserId(1);
    readerUser.addRole(
        new UserRole(UserRoles.CHAIRPERSON.getRoleId(), UserRoles.CHAIRPERSON.getRoleName()));
    when(userService.findUserById(any())).thenReturn(readerUser);
    Response response = resource.addAuthorizedReaders(duosUser, 1, 1);
    assertEquals(HttpStatusCodes.STATUS_CODE_CONFLICT, response.getStatus());
  }

  @Test
  void testAddAuthorizedReaders(){
    User readerUser = new User();
    readerUser.setUserId(1);
    readerUser.addRole(
        new UserRole(UserRoles.SERVICE_ACCOUNT.getRoleId(), UserRoles.SERVICE_ACCOUNT.getRoleName()));
    user.setUserId(1);
    DatasetAuthorizationReader reader = new DatasetAuthorizationReader(1,1,1, 1, Timestamp.from(
        Instant.now()));
    when(userService.findUserById(any())).thenReturn(readerUser);
    when(datasetService.addAuthorizedReader(anyLong(), anyLong(), anyLong())).thenReturn(reader);
    Response response = resource.addAuthorizedReaders(duosUser, 1, 1);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  void testAddAuthorizedReadersThrows(){
    User readerUser = new User();
    readerUser.setUserId(1);
    readerUser.addRole(
        new UserRole(UserRoles.SERVICE_ACCOUNT.getRoleId(), UserRoles.SERVICE_ACCOUNT.getRoleName()));
    user.setUserId(1);
    doThrow(new RuntimeException("Some exception")).when(userService).findUserById(any());
    Response response = resource.addAuthorizedReaders(duosUser, 1, 1);
    assertEquals(HttpStatusCodes.STATUS_CODE_SERVER_ERROR, response.getStatus());
  }

  @Test
  void testRemoveAuthorizedReaders(){
    doNothing().when(datasetService).removeAuthorizedAccessReader(anyLong(), anyLong());
    Response response = resource.removeAuthorizedReaders(duosUser, 1, 1);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  void testRemoveAuthorizedReadersThrowns(){
    doThrow(new RuntimeException("Some Exception")).when(datasetService).removeAuthorizedAccessReader(anyLong(), anyLong());
    Response response = resource.removeAuthorizedReaders(duosUser, 1, 1);
    assertEquals(HttpStatusCodes.STATUS_CODE_SERVER_ERROR, response.getStatus());
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
          "datasetId": 2,
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
              "datasetId": 2,
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
          "datasetId": 2,
        }
        """;

    return String.format(format, user.getUserId());
  }

  /*
   * Study mock
   */
  private Study createMockStudy() {
    Dataset dataset = createMockDataset();

    Study study = new Study();
    study.setName(randomAlphabetic(10));
    study.setDescription(randomAlphabetic(20));
    study.setStudyId(12345);
    study.setPiName(randomAlphabetic(10));
    study.setDataTypes(List.of(randomAlphabetic(10)));
    study.setCreateUserId(9);
    study.setCreateUserEmail(randomAlphabetic(10));
    study.setPublicVisibility(true);
    study.addDatasetIds(Set.of(dataset.getDatasetId()));

    StudyProperty phenotypeProperty = new StudyProperty();
    phenotypeProperty.setKey("phenotypeIndication");
    phenotypeProperty.setType(PropertyType.String);
    phenotypeProperty.setValue(randomAlphabetic(10));

    StudyProperty speciesProperty = new StudyProperty();
    speciesProperty.setKey("species");
    speciesProperty.setType(PropertyType.String);
    speciesProperty.setValue(randomAlphabetic(10));

    StudyProperty dataCustodianEmailProperty = new StudyProperty();
    dataCustodianEmailProperty.setKey("dataCustodianEmail");
    dataCustodianEmailProperty.setType(PropertyType.Json);
    dataCustodianEmailProperty.setValue(List.of(randomAlphabetic(10)));

    study.addProperties(phenotypeProperty, speciesProperty, dataCustodianEmailProperty);
    dataset.setStudy(study);
    study.addDatasets(List.of(dataset));
    return study;
  }

  private Dataset createMockDataset() {
    Dataset dataset = new Dataset();
    dataset.setDatasetId(100);
    dataset.setAlias(10);
    dataset.setDatasetIdentifier();
    dataset.setDacId(1);
    dataset.setDataUse(new DataUse());
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
    return dataset;
  }
}
