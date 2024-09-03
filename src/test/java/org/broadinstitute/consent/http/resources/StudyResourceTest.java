package org.broadinstitute.consent.http.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.google.api.client.http.HttpStatusCodes;
import com.google.gson.Gson;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.RandomStringUtils;
import org.broadinstitute.consent.http.enumeration.PropertyType;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetProperty;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StudyResourceTest {

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

  private StudyResource resource;

  private void initResource() {
    resource = new StudyResource(datasetService, userService, datasetRegistrationService, elasticSearchService);
  }

  @Test
  void testUpdateCustodiansSuccess() {
    initResource();
    Response response = resource.updateCustodians(authUser, 1, "[\"user_1@test.com\", \"user_2@test.com\"]");
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  void testUpdateCustodiansInvalidEmails() {
    initResource();
    Response response = resource.updateCustodians(authUser, 1, "[\"user_1\", \"@test.com\"]");
    assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
  }

  @Test
  void testUpdateCustodiansNotFound() {
    when(datasetService.updateStudyCustodians(any(), any(), any())).thenThrow(new NotFoundException("Study not found"));
    initResource();
    Response response = resource.updateCustodians(authUser, 1, "[\"user_1@test.com\", \"user_2@test.com\"]");
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  @Test
  void testGetStudyByIdNoDatasets() {
    Study study = new Study();
    study.setStudyId(1);
    study.setName("asdfasdfasdfasdfasdfasdf");
    when(datasetService.getStudyWithDatasetsById(1)).thenReturn(study);
    initResource();
    Response response = resource.getStudyById(1);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  void testGetStudyByIdWithDatasets() {
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

    when(datasetService.getStudyWithDatasetsById(12345)).thenReturn(study);

    initResource();
    Response response = resource.getStudyById(12345);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
    assertEquals(study.getDatasetIds().size(), datasets.size());
  }

  @Test
  void testGetStudyByIdNotFound() {
    when(datasetService.getStudyWithDatasetsById(1)).thenThrow(new NotFoundException());

    initResource();
    Response response = resource.getStudyById(1);
    assertEquals(HttpStatusCodes.STATUS_CODE_NOT_FOUND, response.getStatus());
  }

  @Test
  void testGetRegistrationFromStudy() {
    Study study = createMockStudy();
    when(datasetService.getStudyWithDatasetsById(any())).thenReturn(study);

    initResource();
    Response response = resource.getRegistrationFromStudy(authUser, 1);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  void testGetRegistrationFromStudyNoDatasets() {
    Study study = createMockStudy();
    study.getDatasets().clear();
    when(datasetService.getStudyWithDatasetsById(any())).thenReturn(study);

    initResource();
    Response response = resource.getRegistrationFromStudy(authUser, 1);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
  }

  @Test
  void testGetRegistrationFromStudyNotFound() {
    Study study = createMockStudy();
    when(datasetService.getStudyWithDatasetsById(any())).thenThrow(new NotFoundException());

    initResource();
    Response response = resource.getRegistrationFromStudy(authUser, study.getStudyId());
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
      study.setDatasetIds(Set.of(datasetIds.get(0) + 1));
    }
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(datasetRegistrationService.findStudyById(any())).thenReturn(study);
    initResource();

    Response response = resource.updateStudyByRegistration(authUser, null, 1, input);
    assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
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
    study.setDatasetIds(datasetIds);
    when(userService.findUserByEmail(any())).thenReturn(user);
    when(datasetRegistrationService.findStudyById(any())).thenReturn(study);
    initResource();

    Response response = resource.updateStudyByRegistration(authUser, null, 1, input);
    assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
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
