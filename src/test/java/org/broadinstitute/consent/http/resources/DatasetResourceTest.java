package org.broadinstitute.consent.http.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import static org.mockito.MockitoAnnotations.openMocks;

import com.google.api.client.http.HttpStatusCodes;
import com.google.gson.Gson;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import java.io.IOException;
import java.net.URI;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.authentication.GenericUser;
import org.broadinstitute.consent.http.db.StudyDAO;
import org.broadinstitute.consent.http.enumeration.PropertyType;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetProperty;
import org.broadinstitute.consent.http.models.Dictionary;
import org.broadinstitute.consent.http.models.Error;
import org.broadinstitute.consent.http.models.Study;
import org.broadinstitute.consent.http.models.StudyProperty;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.models.dataset_registration_v1.ConsentGroup;
import org.broadinstitute.consent.http.models.dataset_registration_v1.DatasetRegistrationSchemaV1;
import org.broadinstitute.consent.http.models.dto.DatasetDTO;
import org.broadinstitute.consent.http.models.dto.DatasetPropertyDTO;
import org.broadinstitute.consent.http.service.DataAccessRequestService;
import org.broadinstitute.consent.http.service.DatasetRegistrationService;
import org.broadinstitute.consent.http.service.DatasetService;
import org.broadinstitute.consent.http.service.ElasticSearchService;
import org.broadinstitute.consent.http.service.UserService;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;

public class DatasetResourceTest {

  @Mock
  private DataAccessRequestService darService;

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
  private GenericUser genericUser;

  @Mock
  private User user;

  @Mock
  private UriInfo uriInfo;

  @Mock
  private UriBuilder uriBuilder;

  @Mock
  private Collection<Dictionary> dictionaries;

  private DatasetResource resource;

  private StudyDAO studyDAO;

  @BeforeEach
  public void setUp() {
    openMocks(this);
  }

  private void initResource() {
    resource = new DatasetResource(datasetService, userService, darService,
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
  public void testCreateDatasetSuccess() throws Exception {
    DatasetDTO result = createMockDatasetDTO();
    String json = createPropertiesJson("Dataset Name", "test");

    when(datasetService.getDatasetByName("test")).thenReturn(null);
    when(datasetService.createDatasetFromDatasetDTO(any(), any(), anyInt())).thenReturn(result);
    when(datasetService.getDatasetDTO(any())).thenReturn(result);
    when(authUser.getGenericUser()).thenReturn(genericUser);
    when(genericUser.getEmail()).thenReturn("email@email.com");
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(user.getUserId()).thenReturn(1);
    when(uriInfo.getRequestUriBuilder()).thenReturn(uriBuilder);
    when(uriBuilder.replacePath(anyString())).thenReturn(uriBuilder);
    when(uriBuilder.build(anyString())).thenReturn(new URI("/api/dataset/1"));
    initResource();
    Response response = resource.createDataset(authUser, uriInfo, json);

    assertEquals(201, response.getStatus());
    assertEquals(result, response.getEntity());
  }

  @Test
  public void testCreateDatasetNoJson() {
    initResource();
    assertThrows(BadRequestException.class, () -> {
      resource.createDataset(authUser, uriInfo, "");
    });
  }

  @Test
  public void testCreateDatasetNoProperties() {
    initResource();
    assertThrows(BadRequestException.class, () -> {
      resource.createDataset(authUser, uriInfo, "{\"properties\":[]}");
    });
  }

  @Test
  public void testCreateDatasetNullName() {
    String json = createPropertiesJson("Dataset Name", null);

    initResource();
    assertThrows(BadRequestException.class, () -> {
      resource.createDataset(authUser, uriInfo, json);
    });
  }

  @Test
  public void testCreateDatasetEmptyName() {
    String json = createPropertiesJson("Dataset Name", "");

    initResource();

    assertThrows(BadRequestException.class, () -> {
      resource.createDataset(authUser, uriInfo, json);
    });
  }

  @Test
  public void testCreateDatasetMissingName() {
    String json = createPropertiesJson("Property", "test");

    initResource();
    assertThrows(BadRequestException.class, () -> {
      resource.createDataset(authUser, uriInfo, json);
    });
  }

  @Test
  public void testCreateDatasetInvalidProperty() {
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
  public void testCreateDatasetDuplicateProperties() {
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
  public void testCreateDatasetNameInUse() {
    Dataset inUse = new Dataset();
    when(datasetService.getDatasetByName("test")).thenReturn(inUse);

    String json = createPropertiesJson("Dataset Name", "test");

    initResource();

    assertThrows(ClientErrorException.class, () -> {
      resource.createDataset(authUser, uriInfo, json);
    });
  }

  @Test
  public void testCreateDatasetError() throws Exception {
    String json = createPropertiesJson("Dataset Name", "test");

    when(datasetService.getDatasetByName("test")).thenReturn(null);
    doThrow(new RuntimeException()).when(datasetService)
        .createDatasetFromDatasetDTO(any(), any(), anyInt());
    when(authUser.getGenericUser()).thenReturn(genericUser);
    when(genericUser.getEmail()).thenReturn("email@email.com");
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(user.getUserId()).thenReturn(1);
    initResource();
    Response response = resource.createDataset(authUser, uriInfo, json);

    assertEquals(500, response.getStatus());
  }

  @Test
  public void testUpdateDatasetSuccess() {
    Dataset preexistingDataset = new Dataset();
    String json = createPropertiesJson("Dataset Name", "test");
    when(datasetService.findDatasetById(anyInt())).thenReturn(preexistingDataset);
    when(datasetService.updateDataset(any(), any(), any())).thenReturn(
        Optional.of(preexistingDataset));
    when(authUser.getGenericUser()).thenReturn(genericUser);
    when(genericUser.getEmail()).thenReturn("email@email.com");
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(user.getUserId()).thenReturn(1);
    when(user.hasUserRole(any())).thenReturn(true);
    when(uriInfo.getRequestUriBuilder()).thenReturn(uriBuilder);
    when(uriBuilder.replacePath(anyString())).thenReturn(uriBuilder);
    initResource();
    Response response = resource.updateDataset(authUser, uriInfo, 1, json);
    assertEquals(200, response.getStatus());
    assertEquals(Optional.of(preexistingDataset).get(), response.getEntity());
  }

  @Test
  public void testUpdateDatasetNoJson() {
    initResource();
    Response response = resource.updateDataset(authUser, uriInfo, 1, "");
    assertEquals(400, response.getStatus());
  }

  @Test
  public void testUpdateDatasetNoProperties() {
    initResource();
    Response response = resource.updateDataset(authUser, uriInfo, 1, "{\"properties\":[]}");
    assertEquals(400, response.getStatus());
  }

  @Test
  public void testUpdateDatasetIdNotFound() {
    String json = createPropertiesJson("Dataset Name", "test");
    when(datasetService.findDatasetById(anyInt())).thenReturn(null);

    initResource();
    Response response = resource.updateDataset(authUser, uriInfo, 1, json);
    assertEquals(404, response.getStatus());
  }

  @Test
  public void testUpdateDatasetInvalidProperty() {
    List<DatasetPropertyDTO> invalidProperties = new ArrayList<>();
    invalidProperties.add(new DatasetPropertyDTO("Invalid Property", "test"));
    when(datasetService.findInvalidProperties(any())).thenReturn(invalidProperties);

    Dataset preexistingDataset = new Dataset();
    when(datasetService.findDatasetById(anyInt())).thenReturn(preexistingDataset);
    String json = createPropertiesJson(invalidProperties);

    initResource();
    Response response = resource.updateDataset(authUser, uriInfo, 1, json);
    assertEquals(400, response.getStatus());
  }

  @Test
  public void testUpdateDatasetDuplicateProperties() {
    List<DatasetPropertyDTO> duplicateProperties = new ArrayList<>();
    duplicateProperties.add(new DatasetPropertyDTO("Dataset Name", "test"));
    duplicateProperties.add(new DatasetPropertyDTO("Dataset Name", "test"));
    when(datasetService.findDuplicateProperties(any())).thenReturn(duplicateProperties);

    Dataset preexistingDataset = new Dataset();
    when(datasetService.findDatasetById(anyInt())).thenReturn(preexistingDataset);
    String json = createPropertiesJson(duplicateProperties);

    initResource();
    Response response = resource.updateDataset(authUser, uriInfo, 1, json);
    assertEquals(400, response.getStatus());
  }

  @Test
  public void testUpdateDatasetNoContent() {
    Dataset preexistingDataset = new Dataset();
    String json = createPropertiesJson("Dataset Name", "test");
    when(datasetService.findDatasetById(anyInt())).thenReturn(preexistingDataset);
    when(datasetService.updateDataset(any(), any(), any())).thenReturn(Optional.empty());
    when(authUser.getGenericUser()).thenReturn(genericUser);
    when(genericUser.getEmail()).thenReturn("email@email.com");
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(user.getUserId()).thenReturn(1);
    when(user.hasUserRole(any())).thenReturn(true);
    when(uriInfo.getRequestUriBuilder()).thenReturn(uriBuilder);
    when(uriBuilder.replacePath(anyString())).thenReturn(uriBuilder);
    initResource();
    Response responseNoContent = resource.updateDataset(authUser, uriInfo, 1, json);
    assertEquals(204, responseNoContent.getStatus());
  }

  @Test
  public void testDescribeDatasetsSuccess() {
    when(authUser.getEmail()).thenReturn("authUserEmail");
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(datasetService.describeDatasets(anyInt())).thenReturn(Collections.emptySet());
    initResource();
    Response response = resource.describeDataSets(authUser);
    assertEquals(200, response.getStatus());
  }

  @Test
  public void testDescribeDatasetsError() {
    when(authUser.getEmail()).thenReturn("authUserEmail");
    when(userService.findUserByEmail(any())).thenReturn(user);
    doThrow(new RuntimeException()).when(datasetService).describeDatasets(anyInt());
    initResource();
    Response response = resource.describeDataSets(authUser);
    assertEquals(500, response.getStatus());
  }

  @Test
  public void testValidateDatasetNameSuccess() {
    Dataset testDataset = new Dataset();
    when(datasetService.getDatasetByName("test")).thenReturn(testDataset);
    initResource();
    Response response = resource.validateDatasetName("test");
    assertEquals(200, response.getStatus());
  }

  @Test
  public void testValidateDatasetNameNotFound() {
    initResource();

    assertThrows(NotFoundException.class, () -> {
      resource.validateDatasetName("test");
    });
  }

  @Test
  public void testFindAllStudyNamesSuccess() {
    when(datasetService.findAllStudyNames()).thenReturn(Set.of("Hi", "Hello"));
    initResource();
    Response response = resource.findAllStudyNames();
    assertEquals(200, response.getStatus());
  }

  @Test
  public void testFindAllStudyNamesFail() {
    when(datasetService.findAllStudyNames()).thenThrow();
    initResource();
    Response response = resource.findAllStudyNames();
    assertEquals(500, response.getStatus());
  }

  @Test
  public void testGetDataSetSample() {
    List<String> header = List.of("attachment; filename=DataSetSample.tsv");
    initResource();
    Response response = resource.getDataSetSample();
    assertEquals(200, response.getStatus());
    assertEquals(header, response.getHeaders().get("Content-Disposition"));
  }

  @Test
  public void testDownloadDatasetsSuccess() {
    List<DatasetDTO> dtoList = new ArrayList<>();
    DatasetDTO testDTO = createMockDatasetDTO();
    dtoList.add(testDTO);

    when(datasetService.describeDataSetsByReceiveOrder(any())).thenReturn(dtoList);
    initResource();

    Response response = resource.downloadDataSets(List.of(1));
    assertEquals(200, response.getStatus());
  }

  @Test
  public void testDownloadDatasetsHeaderError() {
    doThrow(new RuntimeException()).when(datasetService).describeDictionaryByReceiveOrder();
    initResource();
    Response response = resource.downloadDataSets(List.of(1));
    assertEquals(500, response.getStatus());
  }

  @Test
  public void testDownloadDatasetsEmptyList() {
    initResource();
    Response response = resource.downloadDataSets(List.of());
    assertEquals(200, response.getStatus());
  }

  @Test
  public void testDownloadDatasetsServiceError() {
    doThrow(new RuntimeException()).when(datasetService).describeDataSetsByReceiveOrder(any());
    initResource();
    Response response = resource.downloadDataSets(List.of(1));
    assertEquals(500, response.getStatus());
  }

  @Test
  public void testDeleteSuccessAdmin() {
    Dataset dataSet = new Dataset();

    when(user.hasUserRole(UserRoles.ADMIN)).thenReturn(true);
    when(userService.findUserByEmail(authUser.getEmail())).thenReturn(user);
    when(datasetService.findDatasetById(any())).thenReturn(dataSet);

    initResource();
    Response response = resource.delete(authUser, 1, null);
    assertEquals(200, response.getStatus());
  }

  @Test
  public void testDeleteSuccessChairperson() {
    Dataset dataSet = new Dataset();
    dataSet.setDataSetId(1);
    dataSet.setDacId(1);

    when(user.hasUserRole(UserRoles.ADMIN)).thenReturn(false);
    UserRole role = new UserRole(UserRoles.CHAIRPERSON.getRoleId(),
        UserRoles.CHAIRPERSON.getRoleName());
    role.setDacId(1);
    when(user.getRoles()).thenReturn(List.of(role));

    when(userService.findUserByEmail(authUser.getEmail())).thenReturn(user);
    when(datasetService.findDatasetById(any())).thenReturn(dataSet);

    initResource();
    Response response = resource.delete(authUser, 1, null);
    assertEquals(200, response.getStatus());
  }

  @Test
  public void testDeleteErrorNoDacIds() {
    Dataset dataSet = new Dataset();

    when(user.hasUserRole(UserRoles.ADMIN)).thenReturn(false);
    UserRole role = new UserRole(UserRoles.CHAIRPERSON.getRoleId(),
        UserRoles.CHAIRPERSON.getRoleName());
    when(user.getRoles()).thenReturn(List.of(role));

    when(userService.findUserByEmail(authUser.getEmail())).thenReturn(user);
    when(datasetService.findDatasetById(any())).thenReturn(dataSet);

    initResource();
    Response response = resource.delete(authUser, 1, null);
    assertEquals(404, response.getStatus());
  }

  @Test
  public void testDeleteErrorNullConsent() {
    Dataset dataSet = new Dataset();
    dataSet.setDataSetId(1);

    when(user.hasUserRole(UserRoles.ADMIN)).thenReturn(false);
    UserRole role = new UserRole(UserRoles.CHAIRPERSON.getRoleId(),
        UserRoles.CHAIRPERSON.getRoleName());
    role.setDacId(1);
    when(user.getRoles()).thenReturn(List.of(role));

    when(userService.findUserByEmail(authUser.getEmail())).thenReturn(user);
    when(datasetService.findDatasetById(any())).thenReturn(dataSet);

    initResource();
    Response response = resource.delete(authUser, 1, null);
    assertEquals(404, response.getStatus());
  }

  @Test
  public void testDeleteErrorMismatch() {
    Dataset dataSet = new Dataset();
    dataSet.setDataSetId(1);
    dataSet.setDacId(2);

    when(user.hasUserRole(UserRoles.ADMIN)).thenReturn(false);
    UserRole role = new UserRole(UserRoles.CHAIRPERSON.getRoleId(),
        UserRoles.CHAIRPERSON.getRoleName());
    role.setDacId(1);
    when(user.getRoles()).thenReturn(List.of(role));

    when(userService.findUserByEmail(authUser.getEmail())).thenReturn(user);
    when(datasetService.findDatasetById(any())).thenReturn(dataSet);

    initResource();
    Response response = resource.delete(authUser, 1, null);
    assertEquals(404, response.getStatus());
  }

  @Test
  public void testDisableDataSetSuccessAdmin() {
    Dataset dataSet = new Dataset();

    when(user.hasUserRole(UserRoles.ADMIN)).thenReturn(true);
    when(userService.findUserByEmail(authUser.getEmail())).thenReturn(user);
    when(datasetService.findDatasetById(any())).thenReturn(dataSet);

    initResource();
    Response response = resource.disableDataSet(authUser, 1, true, null);
    assertEquals(200, response.getStatus());
  }

  @Test
  public void testDisableDataSetSuccessChairperson() {
    Dataset dataSet = new Dataset();
    dataSet.setDataSetId(1);
    dataSet.setDacId(1);

    when(user.hasUserRole(UserRoles.ADMIN)).thenReturn(false);
    UserRole role = new UserRole(UserRoles.CHAIRPERSON.getRoleId(),
        UserRoles.CHAIRPERSON.getRoleName());
    role.setDacId(1);
    when(user.getRoles()).thenReturn(List.of(role));

    when(userService.findUserByEmail(authUser.getEmail())).thenReturn(user);
    when(datasetService.findDatasetById(any())).thenReturn(dataSet);

    initResource();
    Response response = resource.disableDataSet(authUser, 1, true, null);
    assertEquals(200, response.getStatus());
  }

  @Test
  public void testDisableDataSetErrorNoDacIds() {
    Dataset dataSet = new Dataset();

    when(user.hasUserRole(UserRoles.ADMIN)).thenReturn(false);
    UserRole role = new UserRole(UserRoles.CHAIRPERSON.getRoleId(),
        UserRoles.CHAIRPERSON.getRoleName());
    when(user.getRoles()).thenReturn(List.of(role));

    when(userService.findUserByEmail(authUser.getEmail())).thenReturn(user);
    when(datasetService.findDatasetById(any())).thenReturn(dataSet);

    initResource();
    Response response = resource.disableDataSet(authUser, 1, true, null);
    assertEquals(404, response.getStatus());
  }

  @Test
  public void testDisableDataSetErrorNullConsent() {
    Dataset dataSet = new Dataset();
    dataSet.setDataSetId(1);

    when(user.hasUserRole(UserRoles.ADMIN)).thenReturn(false);
    UserRole role = new UserRole(UserRoles.CHAIRPERSON.getRoleId(),
        UserRoles.CHAIRPERSON.getRoleName());
    role.setDacId(1);
    when(user.getRoles()).thenReturn(List.of(role));

    when(userService.findUserByEmail(authUser.getEmail())).thenReturn(user);
    when(datasetService.findDatasetById(any())).thenReturn(dataSet);

    initResource();
    Response response = resource.disableDataSet(authUser, 1, true, null);
    assertEquals(404, response.getStatus());
  }

  @Test
  public void testDisableDataSetErrorMismatch() {
    Dataset dataSet = new Dataset();
    dataSet.setDataSetId(1);
    dataSet.setDacId(2);

    when(user.hasUserRole(UserRoles.ADMIN)).thenReturn(false);
    UserRole role = new UserRole(UserRoles.CHAIRPERSON.getRoleId(),
        UserRoles.CHAIRPERSON.getRoleName());
    role.setDacId(1);
    when(user.getRoles()).thenReturn(List.of(role));

    when(userService.findUserByEmail(authUser.getEmail())).thenReturn(user);
    when(datasetService.findDatasetById(any())).thenReturn(dataSet);

    initResource();
    Response response = resource.disableDataSet(authUser, 1, true, null);
    assertEquals(404, response.getStatus());
  }

  @Test
  public void testDescribeDictionarySuccess() {
    when(datasetService.describeDictionaryByDisplayOrder()).thenReturn(dictionaries);
    initResource();
    Response response = resource.describeDictionary();
    assertEquals(200, response.getStatus());
  }

  @Test
  public void testDescribeDictionaryError() {
    doThrow(new RuntimeException()).when(datasetService).describeDictionaryByDisplayOrder();
    initResource();
    Response response = resource.describeDictionary();
    assertEquals(500, response.getStatus());
  }

  @Test
  public void testDatasetAutocompleteSuccess() {
    List<Map<String, String>> autocompleteMap = List.of(Collections.EMPTY_MAP);
    when(authUser.getEmail()).thenReturn("testauthuser@test.com");
    when(userService.findUserByEmail(anyString())).thenReturn(user);
    when(user.getUserId()).thenReturn(0);
    when(datasetService.autoCompleteDatasets(anyString(), anyInt())).thenReturn(autocompleteMap);

    initResource();
    Response response = resource.datasetAutocomplete(authUser, "test");
    assertEquals(200, response.getStatus());
  }


  @Test
  public void testDatasetAutocompleteError() {
    when(authUser.getEmail()).thenReturn("testauthuser@test.com");
    when(userService.findUserByEmail(anyString())).thenReturn(user);
    when(user.getUserId()).thenReturn(0);
    doThrow(new RuntimeException()).when(datasetService)
        .autoCompleteDatasets(anyString(), anyInt());

    initResource();
    Response response = resource.datasetAutocomplete(authUser, "test");
    assertEquals(500, response.getStatus());
  }

  @Test
  public void testIndexAllDatasets() throws IOException {
    List<Dataset> datasets = List.of(new Dataset());

    Response mockResponse = Response.ok().entity(datasets).build();
    when(datasetService.findAllDatasets()).thenReturn(datasets);
    when(elasticSearchService.indexDatasets(datasets)).thenReturn(mockResponse);

    initResource();
    Response response = resource.indexDatasets();
    assertEquals(200, response.getStatus());
  }

  @Test
  public void testIndexDataset() throws IOException {
    Dataset dataset = new Dataset();

    Response mockResponse = Response.ok().entity(dataset).build();
    when(datasetService.findDatasetById(any())).thenReturn(dataset);
    when(elasticSearchService.indexDataset(dataset)).thenReturn(mockResponse);

    initResource();
    Response response = resource.indexDataset(1);
    assertEquals(200, response.getStatus());
  }

  @Test
  public void testIndexDelete() throws IOException {
    Response mockResponse = Response.status(200).entity("deleted").build();
    when(elasticSearchService.deleteIndex(any())).thenReturn(mockResponse);

    initResource();
    Response response = resource.deleteDatasetIndex(0);
    assertEquals(200, response.getStatus());
  }

  @Test
  public void testSearchDatasetsOpenAccessFalse() {
    Dataset ds = new Dataset();
    ds.setDataSetId(1);
    Boolean openAccess = false;
    when(authUser.getEmail()).thenReturn("testauthuser@test.com");
    when(userService.findUserByEmail("testauthuser@test.com")).thenReturn(user);
    when(user.getUserId()).thenReturn(0);
    when(datasetService.searchDatasets("search query", openAccess, user)).thenReturn(List.of(ds));

    initResource();
    Response response = resource.searchDatasets(authUser, "search query", openAccess);

    assertEquals(200, response.getStatus());
    assertEquals(GsonUtil.buildGson().toJson(List.of(ds)), response.getEntity());
  }

  @Test
  public void testSearchDatasetsOpenAccessTrue() {
    Dataset ds = new Dataset();
    ds.setDataSetId(1);
    Boolean openAccess = true;
    when(authUser.getEmail()).thenReturn("testauthuser@test.com");
    when(userService.findUserByEmail("testauthuser@test.com")).thenReturn(user);
    when(user.getUserId()).thenReturn(0);
    when(datasetService.searchDatasets("search query", openAccess, user)).thenReturn(List.of(ds));

    initResource();
    Response response = resource.searchDatasets(authUser, "search query", openAccess);

    assertEquals(200, response.getStatus());
    assertEquals(GsonUtil.buildGson().toJson(List.of(ds)), response.getEntity());
  }

  @Test
  public void testSearchDatasetIndex() throws IOException {
    String query = "{ \"dataUse\": [\"HMB\"] }";

    Response mockResponse = Response.ok().entity(query).build();
    when(authUser.getEmail()).thenReturn("testauthuser@test.com");
    when(userService.findUserByEmail("testauthuser@test.com")).thenReturn(user);
    when(user.getUserId()).thenReturn(0);
    when(elasticSearchService.searchDatasets(any())).thenReturn(mockResponse);

    initResource();
    Response response = resource.searchDatasetIndex(authUser, query);

    assertEquals(200, response.getStatus());
    assertTrue(response.getEntity().toString().length() > 2);
  }

  @Test
  public void testUpdateNeedsReviewDataSetsSuccess() {
    Dataset dataSet = new Dataset();
    when(datasetService.updateNeedsReviewDatasets(any(), any())).thenReturn(dataSet);

    initResource();
    Response response = resource.updateNeedsReviewDataSets(1, true);
    assertEquals(200, response.getStatus());
  }

  @Test
  public void testGetDataset() {
    Dataset ds = new Dataset();
    ds.setDataSetId(1);
    ds.setName("asdfasdfasdfasdfasdfasdf");
    when(datasetService.findDatasetById(1)).thenReturn(ds);
    initResource();
    Response response = resource.getDataset(1);
    assertEquals(200, response.getStatus());
    assertEquals(ds, response.getEntity());
  }

  @Test
  public void testGetDatasetNotFound() {
    when(datasetService.findDatasetById(1)).thenReturn(null);

    initResource();
    Response response = resource.getDataset(1);
    assertEquals(404, response.getStatus());
  }

  @Test
  public void testGetDatasets() {
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
    assertEquals(200, response.getStatus());
    assertEquals(datasets, response.getEntity());
  }

  @Test
  public void testGetDatasetsDuplicates() {
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
    assertEquals(200, response.getStatus());
    assertEquals(datasets, response.getEntity());
  }

  @Test
  public void testGetDatasetsDuplicatesNotFound() {
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
    assertEquals(404, response.getStatus());
    assertTrue(((Error) response.getEntity()).message().contains("3"));
    assertFalse(((Error) response.getEntity()).message().contains("2"));
    assertFalse(((Error) response.getEntity()).message().contains("1"));

  }

  @Test
  public void testGetDatasetsNotFound() {
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
    assertEquals(404, response.getStatus());
    assertTrue(((Error) response.getEntity()).message().contains("4"));
    assertFalse(((Error) response.getEntity()).message().contains("3"));
    assertTrue(((Error) response.getEntity()).message().contains("2"));
    assertFalse(((Error) response.getEntity()).message().contains("1"));
  }


  @Test
  public void testUpdateNeedsReviewDataSetsError() {
    doThrow(new RuntimeException()).when(datasetService).updateNeedsReviewDatasets(any(), any());

    initResource();
    Response response = resource.updateNeedsReviewDataSets(1, true);
    assertEquals(500, response.getStatus());
  }

  @Test
  public void testUpdateDatasetDataUse_OK() {
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
  public void testUpdateDatasetDataUse_BadRequestJson() {
    when(userService.findUserByEmail(any())).thenReturn(new User());
    when(datasetService.updateDatasetDataUse(any(), any(), any())).thenReturn(new Dataset());

    initResource();
    Response response = resource.updateDatasetDataUse(new AuthUser(), 1, "invalid json");
    assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
  }

  @Test
  public void testUpdateDatasetDataUse_BadRequestService() {
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
  public void testUpdateDatasetDataUse_NotFound() {
    when(userService.findUserByEmail(any())).thenReturn(new User());
    when(datasetService.findDatasetById(any())).thenThrow(new NotFoundException());
    when(datasetService.updateDatasetDataUse(any(), any(), any())).thenThrow(
        new NotFoundException());

    initResource();
    String duString = new DataUseBuilder().setGeneralUse(true).build().toString();
    Response response = resource.updateDatasetDataUse(new AuthUser(), 1, duString);
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  @Test
  public void testUpdateDatasetDataUse_NotModified() {
    when(userService.findUserByEmail(any())).thenReturn(new User());
    Dataset d = new Dataset();
    DataUse du = new DataUseBuilder().setGeneralUse(true).build();
    d.setDataUse(du);
    when(datasetService.findDatasetById(any())).thenReturn(d);
    when(datasetService.updateDatasetDataUse(any(), any(), any())).thenReturn(d);

    initResource();
    Response response = resource.updateDatasetDataUse(new AuthUser(), 1, du.toString());
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_MODIFIED, response.getStatus());
  }

  @Test
  public void testDownloadDatasetApprovedUsersSuccess() {
    List<String> header = List.of("attachment; filename=DatasetApprovedUsers.tsv");
    initResource();
    Response response = resource.downloadDatasetApprovedUsers(new AuthUser(), 1);
    assertEquals(200, response.getStatus());
    assertEquals(header, response.getHeaders().get("Content-Disposition"));
  }

  @Test
  public void testDownloadDatasetApprovedUsersError() {
    doThrow(new RuntimeException()).when(darService).getDatasetApprovedUsersContent(any(), any());
    initResource();
    Response response = resource.downloadDatasetApprovedUsers(new AuthUser(), 1);
    assertEquals(500, response.getStatus());
  }

  @Test
  public void testFindAllDatasetsAvailableToUser() {
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(datasetService.findAllDatasetsByUser(any())).thenReturn(List.of(new Dataset()));
    initResource();
    Response response = resource.findAllDatasetsAvailableToUser(authUser, null);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  void testFindAllDatasetsAvailableToUserAsCustodian() {
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(datasetService.findDatasetsByCustodian(any())).thenReturn(List.of(new Dataset()));
    initResource();
    Response response = resource.findAllDatasetsAvailableToUser(authUser, true);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  public void testCreateDatasetRegistration_invalidSchema_case1() {
    initResource();
    Response response = resource.createDatasetRegistration(authUser, null, "");
    assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
  }

  @Test
  public void testCreateDatasetRegistration_invalidSchema_case2() {
    initResource();
    Response response = resource.createDatasetRegistration(authUser, null, "{}");
    assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
  }

  @Test
  public void testCreateDatasetRegistration_invalidSchema_case3() {
    DatasetRegistrationSchemaV1 schemaV1 = new DatasetRegistrationSchemaV1();
    String schemaString = new Gson().toJson(schemaV1);
    initResource();
    Response response = resource.createDatasetRegistration(authUser, null, schemaString);
    assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
  }

  @Test
  public void testCreateDatasetRegistration_validSchema() throws SQLException, IOException {
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(datasetRegistrationService.createDatasetsFromRegistration(any(), any(), any())).thenReturn(
        List.of());
    String schemaV1 = createDatasetRegistrationMock(user);
    initResource();

    Response response = resource.createDatasetRegistration(authUser, null, schemaV1);
    System.out.println(response.getEntity());
    assertEquals(HttpStatusCodes.STATUS_CODE_CREATED, response.getStatus());
  }

  @Test
  public void testCreateDatasetRegistration_withFile() throws SQLException, IOException {
    FormDataContentDisposition content = FormDataContentDisposition
        .name("file")
        .fileName("sharing_plan.txt")
        .build();
    FormDataBodyPart formDataBodyPart = mock(FormDataBodyPart.class);
    when(formDataBodyPart.getContentDisposition()).thenReturn(content);

    FormDataMultiPart formDataMultiPart = mock(FormDataMultiPart.class);
    when(formDataMultiPart.getFields()).thenReturn(Map.of("file", List.of(formDataBodyPart)));

    when(userService.findUserByEmail(any())).thenReturn(user);
    when(datasetRegistrationService.createDatasetsFromRegistration(any(), any(), any())).thenReturn(
        List.of());
    String schemaV1 = createDatasetRegistrationMock(user);
    initResource();

    Response response = resource.createDatasetRegistration(authUser, formDataMultiPart, schemaV1);
    assertEquals(HttpStatusCodes.STATUS_CODE_CREATED, response.getStatus());
  }

  @Test
  public void testCreateDatasetRegistration_multipleFiles() throws SQLException, IOException {
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
    when(formDataBodyPartNotFile.getName()).thenReturn("notFile");
    when(formDataBodyPartNotFile.getContentDisposition()).thenReturn(contentNotFile);

    FormDataMultiPart formDataMultiPart = mock(FormDataMultiPart.class);
    when(formDataMultiPart.getFields()).thenReturn(
        Map.of(
            "file", List.of(formDataBodyPartFile),
            "other", List.of(formDataBodyPartOther),
            "notFile", List.of(formDataBodyPartNotFile)));

    when(userService.findUserByEmail(any())).thenReturn(user);
    when(datasetRegistrationService.createDatasetsFromRegistration(any(), any(), any())).thenReturn(
        List.of());
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
  public void testCreateDatasetRegistration_invalidFileName() {
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
  public void testGetStudyByIdNoDatasets() {
    Study study = new Study();
    study.setStudyId(1);
    study.setName("asdfasdfasdfasdfasdfasdf");
    when(datasetService.getStudyWithDatasetsById(1)).thenReturn(study);
    initResource();
    Response response = resource.getStudyById(1);
    assertEquals(200, response.getStatus());
  }

  @Test
  public void testGetStudyByIdWithDatasets() {
    Dataset ds1 = new Dataset();
    ds1.setDataSetId(1);
    Dataset ds2 = new Dataset();
    ds2.setDataSetId(2);
    Dataset ds3 = new Dataset();
    ds3.setDataSetId(3);
    List<Dataset> datasets = List.of(ds1, ds2, ds3);

    Study study = new Study();
    study.setName(RandomStringUtils.randomAlphabetic(10));
    study.setStudyId(12345);
    study.setDatasetIds(Set.of(1, 2, 3));

    List<Integer> datasetIds = new ArrayList<>(study.getDatasetIds());

    when(datasetService.getStudyWithDatasetsById(12345)).thenReturn(study);
    when(datasetService.findDatasetsByIds(datasetIds)).thenReturn(datasets);

    initResource();
    Response response = resource.getStudyById(12345);
    assertEquals(200, response.getStatus());
    assertEquals(study.getDatasetIds().size(), datasets.size());
  }

  @Test
  public void testGetStudyByIdNotFound() {
    when(datasetService.getStudyWithDatasetsById(1)).thenThrow(new NotFoundException());

    initResource();
    Response response = resource.getStudyById(1);
    assertEquals(404, response.getStatus());
  }

  @Test
  public void testupdateDatasetByDatasetIntakeSuccess() throws SQLException, IOException {
    Dataset preexistingDataset = new Dataset();
    when(datasetService.findDatasetById(anyInt())).thenReturn(preexistingDataset);
    when(datasetRegistrationService.updateDataset(any(), any(), any(), any())).thenReturn(
        preexistingDataset);
    when(authUser.getGenericUser()).thenReturn(genericUser);
    when(genericUser.getEmail()).thenReturn("email@email.com");
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(user.getUserId()).thenReturn(1);
    when(user.hasUserRole(any())).thenReturn(true);
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
    assertEquals(200, response.getStatus());
    assertEquals(Optional.of(preexistingDataset).get(), response.getEntity());
  }

  @Test
  public void testUpdateDatasetWithNoJson() {
    FormDataContentDisposition content = FormDataContentDisposition
        .name("file")
        .fileName("validFile.txt")
        .build();

    FormDataBodyPart formDataBodyPart = mock(FormDataBodyPart.class);
    when(formDataBodyPart.getContentDisposition()).thenReturn(content);

    FormDataMultiPart formDataMultiPart = mock(FormDataMultiPart.class);
    when(formDataMultiPart.getFields()).thenReturn(Map.of("file", List.of(formDataBodyPart)));
    initResource();
    Response response = resource.updateByDatasetUpdate(authUser, 1, formDataMultiPart, "");
    assertEquals(400, response.getStatus());
  }

  @Test
  public void testUpdateDatasetWithInvalidJson() {
    FormDataContentDisposition content = FormDataContentDisposition
        .name("file")
        .fileName("validFile.txt")
        .build();

    String json = createInvalidDataset(user);

    FormDataBodyPart formDataBodyPart = mock(FormDataBodyPart.class);
    when(formDataBodyPart.getContentDisposition()).thenReturn(content);

    FormDataMultiPart formDataMultiPart = mock(FormDataMultiPart.class);
    when(formDataMultiPart.getFields()).thenReturn(Map.of("file", List.of(formDataBodyPart)));
    initResource();
    Response response = resource.updateByDatasetUpdate(authUser, 1, formDataMultiPart, json);
    assertEquals(400, response.getStatus());
  }

  /**
   * tests the case that there are no updates to the dataset properties, should result in success
   */
  @Test
  public void testUpdateDatasetWithNoProperties() {
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
    assertEquals(200, response.getStatus());
  }

  @Test
  public void testUpdateDatasetWIthDatasetIdNotFound() {
    FormDataContentDisposition content = FormDataContentDisposition
        .name("file")
        .fileName("validFile.txt")
        .build();

    FormDataBodyPart formDataBodyPart = mock(FormDataBodyPart.class);
    when(formDataBodyPart.getContentDisposition()).thenReturn(content);

    FormDataMultiPart formDataMultiPart = mock(FormDataMultiPart.class);
    when(formDataMultiPart.getFields()).thenReturn(Map.of("file", List.of(formDataBodyPart)));
    String json = createDatasetRegistrationMock(user);
    when(datasetService.findDatasetById(anyInt())).thenReturn(null);

    initResource();
    Response response = resource.updateByDatasetUpdate(authUser, 1, formDataMultiPart, json);
    assertEquals(404, response.getStatus());
  }

  @Test
  public void testUpdateDatasetInvalidFileName() throws SQLException, IOException {
    Dataset preexistingDataset = new Dataset();
    when(datasetService.findDatasetById(anyInt())).thenReturn(preexistingDataset);
    when(datasetRegistrationService.updateDataset(any(), any(), any(), any())).thenReturn(
        preexistingDataset);
    when(authUser.getGenericUser()).thenReturn(genericUser);
    when(genericUser.getEmail()).thenReturn("email@email.com");
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(user.getUserId()).thenReturn(1);
    when(user.hasUserRole(any())).thenReturn(true);
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
  public void testSyncDataUseTranslation() {
    when(datasetService.syncDatasetDataUseTranslation(any())).thenReturn(new Dataset());
    initResource();

    Response response = resource.syncDataUseTranslation(authUser, 1);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  public void testSyncDataUseTranslationNotFound() {
    when(datasetService.syncDatasetDataUseTranslation(any())).thenThrow(new NotFoundException());
    initResource();

    Response response = resource.syncDataUseTranslation(authUser, 1);
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  @ParameterizedTest
  @ValueSource(strings = {
      DataResourceTestData.registrationWithMalformedJson,
      DataResourceTestData.registrationWithStudyName,
      DataResourceTestData.registrationWithDataSubmitterUserId,
      DataResourceTestData.registrationWithExistingCGDataUse,
      DataResourceTestData.registrationWithExistingCG
  })
  public void testUpdateStudyByRegistrationInvalid(String input) {
    Study study = createMockStudy();
    // for DataResourceTestData.registrationWithExistingCG, manipulate the dataset ids to simulate
    // a dataset deletion
    if (input.equals(DataResourceTestData.registrationWithExistingCG)) {
      Gson gson = GsonUtil.gsonBuilderWithAdapters().create();
      DatasetRegistrationSchemaV1 schemaV1 = gson.fromJson(input,
          DatasetRegistrationSchemaV1.class);
      List<Integer> datasetIds = schemaV1.getConsentGroups().stream()
          .map(ConsentGroup::getDatasetId).toList();
      study.setDatasetIds(Set.of(datasetIds.get(0) + 1));
    }
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(datasetRegistrationService.findStudyById(any())).thenReturn(study);
    initResource();

    Response response = resource.updateStudyByRegistration(authUser, null, 1, input);
    assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
  }

  @Test
  public void testUpdateStudyByRegistration() {
    String input = DataResourceTestData.validRegistration;
    Study study = createMockStudy();
    Gson gson = GsonUtil.gsonBuilderWithAdapters().create();
    DatasetRegistrationSchemaV1 schemaV1 = gson.fromJson(input, DatasetRegistrationSchemaV1.class);
    Set<Integer> datasetIds = schemaV1
        .getConsentGroups()
        .stream()
        .map(ConsentGroup::getDatasetId)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
    study.setDatasetIds(datasetIds);
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(datasetRegistrationService.findStudyById(any())).thenReturn(study);
    initResource();

    Response response = resource.updateStudyByRegistration(authUser, null, 1, input);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
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

    return study;
  }
}
