package org.broadinstitute.consent.http.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.client.http.HttpStatusCodes;
import com.google.gson.Gson;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.broadinstitute.consent.http.AbstractTestHelper;
import org.broadinstitute.consent.http.enumeration.PropertyType;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetProperty;
import org.broadinstitute.consent.http.models.DuosUser;
import org.broadinstitute.consent.http.models.Study;
import org.broadinstitute.consent.http.models.StudyProperty;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.dataset_registration_v1.ConsentGroup;
import org.broadinstitute.consent.http.models.dataset_registration_v1.ConsentGroup.AccessManagement;
import org.broadinstitute.consent.http.models.dataset_registration_v1.ConsentGroup.DataLocation;
import org.broadinstitute.consent.http.models.dataset_registration_v1.DatasetRegistrationSchemaV1;
import org.broadinstitute.consent.http.service.DatasetRegistrationService;
import org.broadinstitute.consent.http.service.DatasetService;
import org.broadinstitute.consent.http.service.ElasticSearchService;
import org.broadinstitute.consent.http.service.UserService;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StudyResourceTest extends AbstractTestHelper {

  @Mock
  private DatasetService datasetService;

  @Mock
  private DatasetRegistrationService datasetRegistrationService;

  @Mock
  private UserService userService;

  @Mock
  private ElasticSearchService elasticSearchService;

  @Mock
  private AuthUser authUser;

  @Mock
  private User user;

  @Mock
  private DuosUser duosUser;

  private StudyResource resource;

  @BeforeEach
  void setUp() {
    resource = new StudyResource(datasetService, userService, datasetRegistrationService,
        elasticSearchService);
  }

  @Test
  void testUpdateCustodiansSuccess() {
    try (var response = resource.updateCustodians(authUser, 1,
        "[\"user_1@test.com\", \"user_2@test.com\"]")) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }
  }

  @Test
  void testUpdateCustodiansInvalidEmails() {
    try (var response = resource.updateCustodians(authUser, 1, "[\"user_1\", \"@test.com\"]")) {
      assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    }
  }

  @Test
  void testUpdateCustodiansNotFound() {
    when(datasetService.updateStudyCustodians(any(), any(), any())).thenThrow(
        new NotFoundException("Study not found"));

    try (var response = resource.updateCustodians(authUser, 1,
        "[\"user_1@test.com\", \"user_2@test.com\"]")) {
      assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
    }
  }

  @Test
  void testGetStudyByIdNoDatasets() {
    Study study = new Study();
    study.setStudyId(1);
    study.setPublicVisibility(true);
    study.setCreateUserId(1);
    study.setName("asdfasdfasdfasdfasdfasdf");
    when(datasetService.getStudyWithDatasetsById(1)).thenReturn(study);
    when(duosUser.getUser()).thenReturn(user);
    when(user.getUserId()).thenReturn(study.getCreateUserId());

    try (var response = resource.getStudyById(duosUser, 1)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }
  }

  @Test
  void testGetStudyByIdWithDatasets() {
    Dataset ds1 = new Dataset();
    ds1.setDatasetId(1);
    Dataset ds2 = new Dataset();
    ds2.setDatasetId(2);
    Dataset ds3 = new Dataset();
    ds3.setDatasetId(3);
    List<Dataset> datasets = List.of(ds1, ds2, ds3);

    Study study = new Study();
    study.setName(randomAlphabetic(10));
    study.setStudyId(12345);
    study.addDatasetIds(Set.of(1, 2, 3));
    study.setPublicVisibility(true);
    study.setCreateUserId(9);

    when(datasetService.getStudyWithDatasetsById(12345)).thenReturn(study);
    when(duosUser.getUser()).thenReturn(user);
    when(user.getUserId()).thenReturn(study.getCreateUserId());

    try (var response = resource.getStudyById(duosUser, 12345)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
      assertEquals(study.getDatasetIds().size(), datasets.size());
    }
  }

  @Test
  void testGetStudyByIdNotFound() {
    when(datasetService.getStudyWithDatasetsById(1)).thenThrow(new NotFoundException());

    try (var response = resource.getStudyById(duosUser, 1)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
    }
  }

  @Test
  void testGetStudyByIdNotPublicGeneralUser() {
    Study study = createMockStudy();
    study.setPublicVisibility(false);
    User generalUser = new User();
    generalUser.setUserId(randomInt(1000, 1100));
    when(duosUser.getUser()).thenReturn(generalUser);
    when(datasetService.getStudyWithDatasetsById(1)).thenReturn(study);

    try (var response = resource.getStudyById(duosUser, 1)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
    }
  }

  @Test
  void testGetStudyByIdNotPublicCreateUser() {
    Study study = createMockStudy();
    study.setPublicVisibility(false);
    User createUser = new User();
    createUser.setUserId(study.getCreateUserId());
    when(duosUser.getUser()).thenReturn(createUser);
    when(datasetService.getStudyWithDatasetsById(1)).thenReturn(study);

    try (var response = resource.getStudyById(duosUser, 1)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }
  }

  @Test
  void testGetRegistrationFromStudy() {
    Study study = createMockStudy();
    when(datasetService.getStudyWithDatasetsById(any())).thenReturn(study);
    when(duosUser.getUser()).thenReturn(user);
    when(user.getUserId()).thenReturn(study.getCreateUserId());

    try (var response = resource.getRegistrationFromStudy(duosUser, 1)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }
  }

  @Test
  void testGetRegistrationFromStudyNoDatasets() {
    Study study = createMockStudy();
    study.getDatasets().clear();
    when(datasetService.getStudyWithDatasetsById(any())).thenReturn(study);
    when(duosUser.getUser()).thenReturn(user);
    when(user.getUserId()).thenReturn(study.getCreateUserId());

    try (var response = resource.getRegistrationFromStudy(duosUser, 1)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }
  }

  @Test
  void testGetRegistrationFromStudyNotFound() {
    Study study = createMockStudy();
    when(datasetService.getStudyWithDatasetsById(any())).thenThrow(new NotFoundException());

    try (var response = resource.getRegistrationFromStudy(duosUser, study.getStudyId())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
    }
  }

  @Test
  void testGetRegistrationFromStudyNotPublicGeneralUser() {
    Study study = createMockStudy();
    study.setPublicVisibility(false);
    User generalUser = new User();
    generalUser.setUserId(randomInt(1000, 1100));
    when(duosUser.getUser()).thenReturn(generalUser);
    when(datasetService.getStudyWithDatasetsById(any())).thenReturn(study);

    try (var response = resource.getRegistrationFromStudy(duosUser, 1)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
    }
  }

  @Test
  void testGetRegistrationFromStudyNotPublicCreateUser() {
    Study study = createMockStudy();
    study.setPublicVisibility(false);
    User createUser = new User();
    createUser.setUserId(study.getCreateUserId());
    when(duosUser.getUser()).thenReturn(createUser);
    when(datasetService.getStudyWithDatasetsById(any())).thenReturn(study);

    try (var response = resource.getRegistrationFromStudy(duosUser, 1)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {
      DataResourceTestData.registrationWithMalformedJson,
      DataResourceTestData.registrationWithStudyName,
      DataResourceTestData.registrationWithDataSubmitterUserId,
      DataResourceTestData.registrationWithExistingCGDataUse,
      DataResourceTestData.registrationWithExistingCG
  })
  void testUpdateStudyByRegistrationInvalid(String input) {
    Study study = createMockStudy();
    // for DataResourceTestData.registrationWithExistingCG, manipulate the dataset ids to simulate
    // a dataset deletion
    if (input.equals(DataResourceTestData.registrationWithExistingCG)) {
      Gson gson = GsonUtil.gsonBuilderWithAdapters().create();
      DatasetRegistrationSchemaV1 schemaV1 = gson.fromJson(input,
          DatasetRegistrationSchemaV1.class);
      List<Integer> datasetIds = schemaV1.getConsentGroups().stream()
          .map(ConsentGroup::getDatasetId).toList();
      study.addDatasetIds(Set.of(datasetIds.get(0) + 1));
    }
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(datasetRegistrationService.findStudyById(any())).thenReturn(study);

    try (var response = resource.updateStudyByRegistration(authUser, null, 1, input)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    }
  }

  @Test
  void testUpdateStudyByRegistration() {
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
    study.getDatasetIds().clear();
    study.addDatasetIds(datasetIds);
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(datasetRegistrationService.findStudyById(any())).thenReturn(study);

    try (var response = resource.updateStudyByRegistration(authUser, null, 1, input)) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    }
  }

  @Test
  void testDeleteStudyById() throws Exception {
    Study study = createMockStudy();
    study.getDatasets().forEach(d -> d.setDeletable(true));
    when(datasetService.getStudyWithDatasetsById(any())).thenReturn(study);
    User admin = new User();
    admin.setAdminRole();
    admin.setUserId(study.getCreateUserId());
    when(userService.findUserByEmail(any())).thenReturn(admin);
    Response esResponse = Mockito.mock(Response.class);
    when(elasticSearchService.deleteIndex(any(), any())).thenReturn(esResponse);

    try (var response = resource.deleteStudyById(authUser, study.getStudyId())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
      verify(elasticSearchService, times(1)).deleteIndex(any(), any());
    }
  }

  @Test
  void testDeleteStudyByIdNotFound() throws Exception {
    Study study = createMockStudy();

    try (var response = resource.deleteStudyById(authUser, study.getStudyId())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
      verify(elasticSearchService, never()).deleteIndex(any(), any());
    }
  }

  @Test
  void testDeleteStudyByIdNonCreatorNonAdmin() throws Exception {
    Study study = createMockStudy();
    study.getDatasets().forEach(d -> d.setDeletable(true));
    when(datasetService.getStudyWithDatasetsById(any())).thenReturn(study);
    User chair = new User();
    chair.setChairpersonRole();
    chair.setUserId(study.getCreateUserId() + 1);
    when(userService.findUserByEmail(any())).thenReturn(chair);

    try (var response = resource.deleteStudyById(authUser, study.getStudyId())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
      verify(elasticSearchService, never()).deleteIndex(any(), any());
    }
  }

  @Test
  void testDeleteStudyByIdNotDeletable() throws Exception {
    Study study = createMockStudy();
    study.getDatasets().forEach(d -> d.setDeletable(false));
    when(datasetService.getStudyWithDatasetsById(any())).thenReturn(study);
    User admin = new User();
    admin.setAdminRole();
    admin.setUserId(study.getCreateUserId());
    when(userService.findUserByEmail(any())).thenReturn(admin);

    try (var response = resource.deleteStudyById(authUser, study.getStudyId())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
      verify(elasticSearchService, never()).deleteIndex(any(), any());
    }
  }

  @Test
  void testDeleteStudyByIdNullDatasets() throws Exception {
    Study study = new Study();
    study.setStudyId(1);
    when(datasetService.getStudyWithDatasetsById(any())).thenReturn(study);
    User admin = new User();
    admin.setAdminRole();
    admin.setUserId(study.getCreateUserId());
    when(userService.findUserByEmail(any())).thenReturn(admin);

    try (var response = resource.deleteStudyById(authUser, study.getStudyId())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
      verify(elasticSearchService, never()).deleteIndex(any(), any());
    }
  }


  @Test
  void testDeleteStudyByIdNoDatasets() throws Exception {
    Study study = createMockStudy();
    study.getDatasetIds().clear();
    study.getDatasets().clear();
    when(datasetService.getStudyWithDatasetsById(any())).thenReturn(study);
    User admin = new User();
    admin.setAdminRole();
    admin.setUserId(study.getCreateUserId());
    when(userService.findUserByEmail(any())).thenReturn(admin);

    try (var response = resource.deleteStudyById(authUser, study.getStudyId())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
      verify(elasticSearchService, never()).deleteIndex(any(), any());
    }
  }

  @Test
  void testDeleteStudyByIdElasticSearchFailure() throws Exception {
    Study study = createMockStudy();
    study.getDatasets().forEach(d -> d.setDeletable(true));
    when(datasetService.getStudyWithDatasetsById(any())).thenReturn(study);
    User admin = new User();
    admin.setAdminRole();
    admin.setUserId(study.getCreateUserId());
    when(userService.findUserByEmail(any())).thenReturn(admin);
    when(elasticSearchService.deleteIndex(any(), any())).thenThrow(new IOException());

    try (var response = resource.deleteStudyById(authUser, study.getStudyId())) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
      verify(elasticSearchService, atLeastOnce()).deleteIndex(any(), any());
    }
  }


  /*
   * Study mock
   */
  private Study createMockStudy() {
    Dataset dataset = new Dataset();
    dataset.setDatasetId(100);
    dataset.setAlias(10);
    dataset.setDatasetIdentifier();
    dataset.setDacId(1);
    dataset.setDataUse(new DataUse());

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
