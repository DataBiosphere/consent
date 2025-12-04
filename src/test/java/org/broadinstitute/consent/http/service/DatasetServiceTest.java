package org.broadinstitute.consent.http.service;

import static org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder.dataCustodianEmail;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.gson.Gson;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.broadinstitute.consent.http.AbstractTestHelper;
import org.broadinstitute.consent.http.db.DaaDAO;
import org.broadinstitute.consent.http.db.DacDAO;
import org.broadinstitute.consent.http.db.DatasetAuthorizationReaderDAO;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.StudyDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.enumeration.DataUseTranslationType;
import org.broadinstitute.consent.http.enumeration.PropertyType;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.ApprovedDataset;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetAuthorizationReader;
import org.broadinstitute.consent.http.models.DatasetStudySummary;
import org.broadinstitute.consent.http.models.Study;
import org.broadinstitute.consent.http.models.StudyPatch;
import org.broadinstitute.consent.http.models.StudyProperty;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.models.dataset_registration_v1.DatasetRegistrationSchemaV1.StudyType;
import org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder;
import org.broadinstitute.consent.http.service.dao.DatasetServiceDAO;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DatasetServiceTest extends AbstractTestHelper {

  private DatasetService datasetService;

  @Mock private DatasetAuthorizationReaderDAO datasetAuthorizationReaderDAO;
  @Mock private DatasetDAO datasetDAO;
  @Mock private DaaDAO daaDAO;
  @Mock private DacDAO dacDAO;
  @Mock private ElasticSearchService elasticSearchService;
  @Mock private EmailService emailService;
  @Mock private OntologyService ontologyService;
  @Mock private StudyDAO studyDAO;
  @Mock private DatasetServiceDAO datasetServiceDAO;
  @Mock private UserDAO userDAO;
  @Mock private User mockUser;

  @BeforeEach
  void initService() {
    datasetService =
        new DatasetService(
            datasetAuthorizationReaderDAO,
            datasetDAO,
            daaDAO,
            dacDAO,
            elasticSearchService,
            emailService,
            ontologyService,
            studyDAO,
            datasetServiceDAO,
            userDAO);
  }

  @Test
  void testFindDatasetListByDacIds() {
    when(datasetDAO.findDatasetListByDacIds(anyList())).thenReturn(List.of());

    assertDoesNotThrow(() -> datasetService.findDatasetListByDacIds(List.of(1, 2, 3)));
  }

  @Test
  void testFindDatasetListByDacIdsEmptyList() {
    List<Integer> emptyList = Collections.emptyList();
    assertThrows(
        BadRequestException.class, () -> datasetService.findDatasetListByDacIds(emptyList));
  }

  @Test
  void testFindDatasetListByDacIdsNullList() {
    assertThrows(BadRequestException.class, () -> datasetService.findDatasetListByDacIds(null));
  }

  @Test
  void testGetDatasetByName() {
    when(datasetDAO.getDatasetByName(getDatasets().get(0).getName().toLowerCase()))
        .thenReturn(getDatasets().get(0));

    Dataset dataset = datasetService.getDatasetByName("Test Dataset 1");

    assertNotNull(dataset);
    assertEquals(dataset.getDatasetId(), getDatasets().get(0).getDatasetId());
  }

  @Test
  void testFindStudyNames() {
    when(datasetDAO.findAllStudyNames()).thenReturn(Set.of("Hi", "Hello"));

    Set<String> returned = datasetService.findAllStudyNames();

    assertNotNull(returned);
    assertEquals(Set.of("Hi", "Hello"), returned);
  }

  @Test
  void testFindDatasetById() {
    when(datasetDAO.findDatasetById(getDatasets().get(0).getDatasetId()))
        .thenReturn(getDatasets().get(0));

    Dataset dataset = datasetService.findDatasetById(mockUser, 1);

    assertNotNull(dataset);
    assertEquals(dataset.getName(), getDatasets().get(0).getName());
  }

  @Test
  void testFindDatasetByIdentifier() {
    Dataset d = new Dataset();
    d.setCreateUserId(1);
    d.setAlias(3);
    d.setDatasetIdentifier();
    Study study = new Study();
    study.setPublicVisibility(Boolean.TRUE);
    d.setStudy(study);
    when(datasetDAO.findDatasetByAlias(3)).thenReturn(d);

    assertEquals(d, datasetService.findDatasetByIdentifier(mockUser, "DUOS-000003"));
  }

  @Test
  void testFindDatasetByIdentifier_WrongIdentifier() {
    Dataset d = new Dataset();
    d.setAlias(3);
    d.setDatasetIdentifier();
    when(datasetDAO.findDatasetByAlias(3)).thenReturn(d);

    assertNull(datasetService.findDatasetByIdentifier(mockUser, "DUOS-0003"));
  }

  @Test
  void testFindDatasetByIdentifier_NoDataset() {
    when(datasetDAO.findDatasetByAlias(3)).thenReturn(null);

    assertNull(datasetService.findDatasetByIdentifier(mockUser, "DUOS-00003"));
  }

  @Test
  void testUpdateDatasetDataUseAdmin() {
    doNothing().when(datasetDAO).updateDatasetDataUse(any(), any());
    when(datasetDAO.findDatasetById(any())).thenReturn(new Dataset());
    User u = new User();
    u.setAdminRole();
    DataUse dataUse = new DataUseBuilder().setGeneralUse(true).build();
    try {
      datasetService.updateDatasetDataUse(u, 1, dataUse);
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  void testUpdateDatasetDataUseNonAdmin() {
    when(datasetDAO.findDatasetById(any())).thenReturn(new Dataset());
    User u = new User();
    Stream.of(
            UserRoles.CHAIRPERSON,
            UserRoles.MEMBER,
            UserRoles.RESEARCHER,
            UserRoles.SIGNINGOFFICIAL,
            UserRoles.DATASUBMITTER,
            UserRoles.ITDIRECTOR,
            UserRoles.ALUMNI)
        .forEach(r -> u.addRole(new UserRole(r.getRoleId(), r.getRoleName())));
    DataUse dataUse = new DataUseBuilder().setGeneralUse(true).build();
    try {
      datasetService.updateDatasetDataUse(u, 1, dataUse);
      fail("Should have thrown an exception on datasetService.updateDatasetDataUse()");
    } catch (IllegalArgumentException e) {
      assertTrue(true);
    }
  }

  @Test
  void testApproveDataset_AlreadyApproved_TrueSubmission() throws Exception {
    Dataset dataset = new Dataset();
    User user = new User();
    user.setEmail("asdf@gmail.com");
    user.setDisplayName("John Doe");
    dataset.setDacApproval(true);
    dataset.setDatasetId(1);
    dataset.setUpdateDate(new Date());
    dataset.setUpdateUserId(4);
    dataset.setAlias(1);
    dataset.setDacId(3);
    Dac dac = new Dac();
    dac.setName("DAC NAME");

    Dataset datasetResult = datasetService.approveDataset(dataset, user, true);
    assertNotNull(datasetResult);
    assertEquals(dataset.getDatasetId(), datasetResult.getDatasetId());
    assertEquals(dataset.getUpdateUserId(), datasetResult.getUpdateUserId());
    assertEquals(dataset.getDacApproval(), datasetResult.getDacApproval());
    assertEquals(dataset.getUpdateDate(), datasetResult.getUpdateDate());
    verify(emailService, times(0)).sendDatasetApprovedMessage(any(), any(), any());
  }

  @Test
  void testApprovedDataset_AlreadyApproved_FalseSubmission() {
    Dataset dataset = new Dataset();
    User user = new User();
    dataset.setDacApproval(true);

    assertThrows(
        IllegalArgumentException.class, () -> datasetService.approveDataset(dataset, user, false));
  }

  @Test
  void testApprovedDataset_AlreadyApproved_NullSubmission() {
    Dataset dataset = new Dataset();
    User user = new User();
    dataset.setDacApproval(true);

    assertThrows(
        IllegalArgumentException.class, () -> datasetService.approveDataset(dataset, user, null));
  }

  @Test
  void testApproveDataset() throws Exception {
    Dataset dataset = new Dataset();
    dataset.setDatasetId(1);
    User user = new User();
    user.setUserId(1);
    user.setEmail("asdf@gmail.com");
    user.setDisplayName("John Doe");
    Boolean payloadBool = true;
    Dataset updatedDataset = new Dataset();
    updatedDataset.setDatasetId(1);
    updatedDataset.setDacApproval(payloadBool);

    when(datasetDAO.findDatasetWithoutFSOInformation(dataset.getDatasetId()))
        .thenReturn(updatedDataset);
    dataset.setAlias(1);
    dataset.setDacId(3);
    Dac dac = new Dac();
    dac.setName("DAC NAME");
    when(dacDAO.findById(3)).thenReturn(dac);

    Dataset returnedDataset = datasetService.approveDataset(dataset, user, payloadBool);
    assertEquals(dataset.getDatasetId(), returnedDataset.getDatasetId());
    assertTrue(returnedDataset.getDacApproval());

    // send approved email
    verify(emailService, times(1)).sendDatasetApprovedMessage(user, "DAC NAME", "DUOS-000001");
  }

  @Test
  void testApproveDataset_DenyDataset() throws Exception {
    Dataset dataset = new Dataset();
    dataset.setDatasetId(1);
    User user = new User();
    user.setUserId(1);
    user.setEmail("asdf@gmail.com");
    user.setDisplayName("John Doe");
    Boolean payloadBool = false;
    Dataset updatedDataset = new Dataset();
    updatedDataset.setDatasetId(1);
    updatedDataset.setDacApproval(payloadBool);

    when(datasetDAO.findDatasetWithoutFSOInformation(dataset.getDatasetId()))
        .thenReturn(updatedDataset);
    dataset.setAlias(1);
    dataset.setDacId(3);
    Dac dac = new Dac();
    dac.setName("DAC NAME");
    dac.setEmail("dacEmail@gmail.com");
    when(dacDAO.findById(3)).thenReturn(dac);

    Dataset returnedDataset = datasetService.approveDataset(dataset, user, payloadBool);
    assertEquals(dataset.getDatasetId(), returnedDataset.getDatasetId());
    assertFalse(returnedDataset.getDacApproval());

    // send denied email
    verify(emailService, times(1))
        .sendDatasetDeniedMessage(user, "DAC NAME", "DUOS-000001", "dacEmail@gmail.com");
  }

  @Test
  void testApproveDataset_DenyDataset_WithNoDACEmail() throws Exception {
    Dataset dataset = new Dataset();
    dataset.setDatasetId(1);
    User user = new User();
    user.setUserId(1);
    user.setEmail("asdf@gmail.com");
    user.setDisplayName("John Doe");
    Boolean payloadBool = false;
    Dataset updatedDataset = new Dataset();
    updatedDataset.setDatasetId(1);
    updatedDataset.setDacApproval(payloadBool);

    when(datasetDAO.findDatasetWithoutFSOInformation(dataset.getDatasetId()))
        .thenReturn(updatedDataset);
    dataset.setAlias(1);
    dataset.setDacId(3);
    Dac dac = new Dac();
    dac.setName("DAC NAME");
    when(dacDAO.findById(3)).thenReturn(dac);

    Dataset returnedDataset = datasetService.approveDataset(dataset, user, payloadBool);
    assertEquals(dataset.getDatasetId(), returnedDataset.getDatasetId());
    assertFalse(returnedDataset.getDacApproval());

    // do not send denied email
    verify(emailService, times(0)).sendDatasetDeniedMessage(user, "DAC NAME", "DUOS-000001", "");
  }

  @Test
  void testSyncDataUseTranslation() {
    Dataset ds = new Dataset();
    ds.setDataUse(new DataUseBuilder().setGeneralUse(true).build());

    when(datasetDAO.findDatasetById(1)).thenReturn(ds);
    String translation =
        """
        Samples are restricted for use under the following conditions:
        Data is limited for health/medical/biomedical research. [HMB]
        Data use is limited for studying: cancerophobia [DS]
        Commercial use is not prohibited.
        Data use for methods development research irrespective of the specified data use limitations is not prohibited.
        Restrictions for use as a control set for diseases other than those defined were not specified.
        """;
    when(ontologyService.translateDataUse(ds.getDataUse(), DataUseTranslationType.DATASET))
        .thenReturn(translation);

    datasetService.syncDatasetDataUseTranslation(1, mockUser);

    verify(datasetDAO, times(1)).updateDatasetTranslatedDataUse(1, translation);
  }

  @Test
  void testSyncDataUseTranslationNotFound() {
    when(datasetDAO.findDatasetById(1)).thenReturn(null);
    assertThrows(
        NotFoundException.class, () -> datasetService.syncDatasetDataUseTranslation(1, mockUser));
  }

  @Test
  void testGetStudyWithDatasetsById() {
    when(studyDAO.findStudyById(anyInt())).thenReturn(new Study());
    assertDoesNotThrow(() -> datasetService.getStudyWithDatasetsById(mockUser, 1));
  }

  @Test
  void testGetStudyWithDatasetsByIdNFE() {
    when(studyDAO.findStudyById(anyInt())).thenReturn(null);
    assertThrows(
        NotFoundException.class, () -> datasetService.getStudyWithDatasetsById(mockUser, 1));
  }

  @Test
  void testGetStudyWithDatasetsByIdGeneralException() {
    when(studyDAO.findStudyById(anyInt())).thenThrow(new RuntimeException("General Exception"));
    assertThrows(Exception.class, () -> datasetService.getStudyWithDatasetsById(mockUser, 1));
  }

  @Test
  void testGetApprovedDatasets() {
    User user =
        new User(1, "test@domain.com", "Test User", new Date(), List.of(UserRoles.Researcher()));
    ApprovedDataset example =
        new ApprovedDataset(
            1,
            "sampleDarId",
            "sampleName",
            "sampleDac",
            Timestamp.from(
                Instant.ofEpochMilli(
                    Instant.now().toEpochMilli() + DataAccessRequest.EXPIRATION_DURATION_MILLIS)));
    when(datasetDAO.getApprovedDatasets(anyInt())).thenReturn(List.of(example));
    assertEquals(1, datasetService.getApprovedDatasets(user).size());
    assertTrue(datasetService.getApprovedDatasets(user).get(0).isApprovedDatasetEqual(example));
  }

  @Test
  void testUpdateStudyCustodiansExisting() {
    User user = new User();
    user.setEmail("test@gmail.com");
    Study study = new Study();
    study.setStudyId(randomInt(100, 10000));
    StudyProperty prop = new StudyProperty();
    prop.setValue("[test@gmail.com]");
    prop.setStudyId(study.getStudyId());
    prop.setType(PropertyType.Json);
    prop.setKey(dataCustodianEmail);
    study.addProperties(prop);
    when(studyDAO.findStudyById(any())).thenReturn(study);

    datasetService.updateStudyCustodians(user, study.getStudyId(), "[new-user@test.com]");
    verify(studyDAO, times(1)).updateStudyProperty(any(), any(), any(), any());
    verify(studyDAO, never()).insertStudyProperty(any(), any(), any(), any());
  }

  @Test
  void testUpdateStudyCustodiansNew() {
    User user = new User();
    user.setEmail("test@gmail.com");
    Study study = new Study();
    study.setStudyId(randomInt(100, 10000));
    when(studyDAO.findStudyById(any())).thenReturn(study);

    datasetService.updateStudyCustodians(user, study.getStudyId(), "[new-user@test.com]");
    verify(studyDAO, never()).updateStudyProperty(any(), any(), any(), any());
    verify(studyDAO, times(1)).insertStudyProperty(any(), any(), any(), any());
  }

  @Test
  void testEnforceDAARestrictions() {
    final User user =
        new User(1, "test@domain.com", "Test User", new Date(), List.of(UserRoles.Researcher()));
    when(daaDAO.findDaaDatasetIdsByUserId(any())).thenReturn(List.of(1, 2, 3));

    assertDoesNotThrow(() -> datasetService.enforceDAARestrictions(user, List.of(1)));
    assertDoesNotThrow(() -> datasetService.enforceDAARestrictions(user, List.of(1, 2)));
    assertDoesNotThrow(() -> datasetService.enforceDAARestrictions(user, List.of(1, 2, 3)));
    List<Integer> firstExpectedList = List.of(1, 2, 3, 4);
    assertThrows(
        BadRequestException.class,
        () -> datasetService.enforceDAARestrictions(user, firstExpectedList));
    List<Integer> secondExpectedList = List.of(2, 3, 4, 5);
    assertThrows(
        BadRequestException.class,
        () -> datasetService.enforceDAARestrictions(user, secondExpectedList));
  }

  @Test
  void testFindAllDatasetStudySummaries() {
    User user = new User();
    user.setUserId(1);
    DatasetStudySummary summary =
        new DatasetStudySummary(
            1, user.getUserId() + 1, "Dataset Name", "DUOS-123", 1, "Study Name", 1000, true);
    when(datasetDAO.findAllDatasetStudySummaries()).thenReturn(List.of(summary));

    List<DatasetStudySummary> authorizedSummaries =
        datasetService.findAllDatasetStudySummaries(user);
    assertEquals(1, authorizedSummaries.size());
    assertEquals(summary, authorizedSummaries.get(0));
  }

  @Test
  void testVerifyPublicVisibilitySummaries_Admin_PV_False() {
    User admin = getAdmin();
    DatasetStudySummary summary =
        new DatasetStudySummary(
            1, admin.getUserId() + 1, "Dataset Name", "DUOS-123", 1, "Study Name", 1000, false);
    List<DatasetStudySummary> authorizedSummaries =
        datasetService.verifyPublicVisibilityAccess(List.of(summary), admin);
    assertEquals(1, authorizedSummaries.size());
    assertEquals(summary, authorizedSummaries.get(0));
  }

  @Test
  void testVerifyPublicVisibilitySummaries_Admin_PV_True() {
    User admin = getAdmin();
    DatasetStudySummary summary =
        new DatasetStudySummary(
            1, admin.getUserId() + 1, "Dataset Name", "DUOS-123", 1, "Study Name", 1000, true);
    List<DatasetStudySummary> authorizedSummaries =
        datasetService.verifyPublicVisibilityAccess(List.of(summary), admin);
    assertEquals(1, authorizedSummaries.size());
    assertEquals(summary, authorizedSummaries.get(0));
  }

  @Test
  void testVerifyPublicVisibilitySummaries_User_HiddenStudy() {
    User user = new User();
    user.setUserId(1);
    DatasetStudySummary summary =
        new DatasetStudySummary(
            1, user.getUserId() + 1, "Dataset Name", "DUOS-123", 1, "Study Name", 1000, false);
    List<DatasetStudySummary> authorizedSummaries =
        datasetService.verifyPublicVisibilityAccess(List.of(summary), user);
    assertEquals(0, authorizedSummaries.size());
  }

  @Test
  void testVerifyPublicVisibilitySummaries_User_PV_True() {
    User user = new User();
    user.setUserId(1);
    DatasetStudySummary summary =
        new DatasetStudySummary(
            1, user.getUserId() + 1, "Dataset Name", "DUOS-123", 1, "Study Name", 1000, true);
    List<DatasetStudySummary> authorizedSummaries =
        datasetService.verifyPublicVisibilityAccess(List.of(summary), user);
    assertEquals(1, authorizedSummaries.size());
    assertEquals(summary, authorizedSummaries.get(0));
  }

  @Test
  void testVerifyPublicVisibilitySummaries_User_Created_Dataset_PV_False() {
    User user = new User();
    user.setUserId(1);
    DatasetStudySummary summary =
        new DatasetStudySummary(
            1, user.getUserId(), "Dataset Name", "DUOS-123", 1, "Study Name", 1000, false);
    List<DatasetStudySummary> authorizedSummaries =
        datasetService.verifyPublicVisibilityAccess(List.of(summary), user);
    assertEquals(1, authorizedSummaries.size());
    assertEquals(summary, authorizedSummaries.get(0));
  }

  @Test
  void testVerifyPublicVisibilitySummaries_User_StudyCreator() {
    User user = new User();
    user.setUserId(1);
    DatasetStudySummary summary =
        new DatasetStudySummary(
            1,
            user.getUserId() + 1,
            "Dataset Name",
            "DUOS-123",
            1,
            "Study Name",
            user.getUserId(),
            false);
    List<DatasetStudySummary> authorizedSummaries =
        datasetService.verifyPublicVisibilityAccess(List.of(summary), user);
    assertEquals(1, authorizedSummaries.size());
    assertEquals(summary, authorizedSummaries.get(0));
  }

  @Test
  void testVerifyPublicVisibilitySummaries_User_IS_StudyCustodian() {
    Gson gson = GsonUtil.getInstance();
    User custodian = new User();
    custodian.setUserId(1);
    custodian.setEmail("alice@custodiansRus.org");
    Study study = new Study();
    study.setStudyId(1);
    study.setCreateUserId(custodian.getUserId() + 1);
    study.setPublicVisibility(false);
    StudyProperty prop = new StudyProperty();
    prop.setKey(DatasetRegistrationSchemaV1Builder.dataCustodianEmail);
    prop.setType(PropertyType.Json);
    prop.setValue(gson.toJson(List.of(custodian.getEmail())));
    study.addProperties(prop);
    DatasetStudySummary summary =
        new DatasetStudySummary(
            1,
            custodian.getUserId() + 1,
            "Dataset Name",
            "DUOS-123",
            study.getStudyId(),
            "Study Name",
            study.getCreateUserId(),
            false);
    when(studyDAO.findStudyById(summary.study_id())).thenReturn(study);

    List<DatasetStudySummary> authorizedSummaries =
        datasetService.verifyPublicVisibilityAccess(List.of(summary), custodian);
    assertEquals(1, authorizedSummaries.size());
    assertEquals(summary, authorizedSummaries.get(0));
  }

  @Test
  void testVerifyPublicVisibilitySummaries_User_NOT_StudyCustodian() {
    Gson gson = GsonUtil.getInstance();
    User custodian = new User();
    custodian.setUserId(1);
    custodian.setEmail("alice@custodiansRus.org");
    Study study = new Study();
    study.setStudyId(1);
    study.setCreateUserId(custodian.getUserId() + 1);
    study.setPublicVisibility(false);
    StudyProperty prop = new StudyProperty();
    prop.setKey(DatasetRegistrationSchemaV1Builder.dataCustodianEmail);
    prop.setType(PropertyType.Json);
    prop.setValue(gson.toJson(List.of("jane@custodiansRus.org")));
    study.addProperties(prop);
    DatasetStudySummary summary =
        new DatasetStudySummary(
            1,
            custodian.getUserId() + 1,
            "Dataset Name",
            "DUOS-123",
            study.getStudyId(),
            "Study Name",
            study.getCreateUserId(),
            false);
    when(studyDAO.findStudyById(summary.study_id())).thenReturn(study);

    List<DatasetStudySummary> authorizedSummaries =
        datasetService.verifyPublicVisibilityAccess(List.of(summary), custodian);
    assertEquals(0, authorizedSummaries.size());
  }

  @Test
  void testVerifyPublicVisibilitySummaries_Study_DoesntExist() {
    User user = new User();
    user.setUserId(1);
    DatasetStudySummary summary =
        new DatasetStudySummary(
            1, user.getUserId() + 1, "Dataset Name", "DUOS-123", null, null, null, null);
    when(datasetDAO.findAllDatasetStudySummaries()).thenReturn(List.of(summary));

    List<DatasetStudySummary> authorizedSummaries =
        datasetService.findAllDatasetStudySummaries(user);
    assertEquals(1, authorizedSummaries.size());
    assertEquals(summary, authorizedSummaries.get(0));
  }

  @Test
  void testVerifyPublicVisibilityAccess_Admin() {
    User admin = getAdmin();
    Dataset dataset = new Dataset();
    dataset.setCreateUserId(2);
    User studyCreator = new User();
    studyCreator.setUserId(3);
    studyCreator.setEmail("sCreator#email.com");
    Study study = new Study();
    study.setStudyId(studyCreator.getUserId());
    study.setCreateUserEmail(studyCreator.getEmail());
    study.setCreateUserId(studyCreator.getUserId());
    study.setPublicVisibility(Boolean.FALSE);
    dataset.setStudy(study);
    dataset.setStudyId(study.getStudyId());

    Dataset verfiedDataset = datasetService.verifyPublicVisibilityAccess(dataset, admin);
    assertEquals(dataset.getDatasetId(), verfiedDataset.getDatasetId());
  }

  private static User getAdmin() {
    User admin = new User();
    admin.setUserId(1);
    admin.setEmail("admin@email.com");
    // Without the admin role this test condition would fail.
    admin.setAdminRole();
    return admin;
  }

  @Test
  void testVerifyPublicVisibilityAccess_NoStudy() {
    User datasetCreator = new User();
    datasetCreator.setUserId(1);
    datasetCreator.setEmail("dsCreator@email.com");
    Dataset dataset = new Dataset();
    dataset.setCreateUserId(datasetCreator.getUserId());

    Dataset verfiedDataset = datasetService.verifyPublicVisibilityAccess(dataset, datasetCreator);
    assertEquals(dataset.getDatasetId(), verfiedDataset.getDatasetId());
  }

  @Test
  void testVerifyPublicVisibilityAccess_VisibleTrue() {
    User user = new User();
    user.setUserId(1);
    user.setEmail("user@email.com");

    User datasetCreator = new User();
    datasetCreator.setUserId(2);
    datasetCreator.setEmail("dsCreator@email.com");
    Dataset dataset = new Dataset();
    dataset.setCreateUserId(datasetCreator.getUserId());
    User studyCreator = new User();
    studyCreator.setUserId(3);
    studyCreator.setEmail("sCreator#email.com");
    Study study = new Study();
    study.setStudyId(studyCreator.getUserId());
    study.setCreateUserEmail(studyCreator.getEmail());
    study.setCreateUserId(studyCreator.getUserId());
    study.setPublicVisibility(Boolean.TRUE);
    dataset.setStudy(study);
    dataset.setStudyId(study.getStudyId());

    Dataset verfiedDataset = datasetService.verifyPublicVisibilityAccess(dataset, user);
    assertEquals(dataset.getDatasetId(), verfiedDataset.getDatasetId());
  }

  @Test
  void testVerifyPublicVisibilityAccess_VisibleNull() {
    User user = new User();
    user.setUserId(1);
    user.setEmail("user@email.com");

    User datasetCreator = new User();
    datasetCreator.setUserId(2);
    datasetCreator.setEmail("dsCreator@email.com");
    Dataset dataset = new Dataset();
    dataset.setCreateUserId(datasetCreator.getUserId());
    User studyCreator = new User();
    studyCreator.setUserId(3);
    studyCreator.setEmail("sCreator#email.com");
    Study study = new Study();
    study.setStudyId(studyCreator.getUserId());
    study.setCreateUserEmail(studyCreator.getEmail());
    study.setCreateUserId(studyCreator.getUserId());
    dataset.setStudy(study);
    dataset.setStudyId(study.getStudyId());

    Dataset verfiedDataset = datasetService.verifyPublicVisibilityAccess(dataset, user);
    assertEquals(dataset.getDatasetId(), verfiedDataset.getDatasetId());
  }

  @Test
  void testVerifyPublicVisibilityAccess_VisibleFalse() {
    User user = new User();
    user.setUserId(1);
    user.setEmail("user@email.com");

    User datasetCreator = new User();
    datasetCreator.setUserId(2);
    datasetCreator.setEmail("dsCreator@email.com");
    Dataset dataset = new Dataset();
    dataset.setCreateUserId(datasetCreator.getUserId());
    User studyCreator = new User();
    studyCreator.setUserId(3);
    studyCreator.setEmail("sCreator@email.com");
    Study study = new Study();
    study.setStudyId(studyCreator.getUserId());
    study.setCreateUserEmail(studyCreator.getEmail());
    study.setCreateUserId(studyCreator.getUserId());
    study.setPublicVisibility(Boolean.FALSE);
    dataset.setStudy(study);
    dataset.setStudyId(study.getStudyId());

    Dataset verfiedDataset = datasetService.verifyPublicVisibilityAccess(dataset, user);
    assertNull(verfiedDataset);
  }

  @Test
  void testIsCreatorOrCustodian_DatasetCreator() {
    User datasetCreator = new User();
    datasetCreator.setUserId(1);
    datasetCreator.setEmail("dsCreator@email.com");
    Dataset dataset = new Dataset();
    dataset.setCreateUserId(datasetCreator.getUserId());
    User studyCreator = new User();
    studyCreator.setUserId(2);
    studyCreator.setEmail("sCreator#email.com");
    Study study = new Study();
    study.setStudyId(studyCreator.getUserId());
    study.setCreateUserEmail(studyCreator.getEmail());
    study.setCreateUserId(studyCreator.getUserId());
    dataset.setStudy(study);
    dataset.setStudyId(study.getStudyId());

    boolean isCreateUser = datasetService.isCreatorOrCustodian(datasetCreator, dataset);
    assertTrue(isCreateUser);
  }

  @Test
  void testIsCreatorOrCustodian_StudyCreator() {
    User datasetCreator = new User();
    datasetCreator.setUserId(1);
    datasetCreator.setEmail("test@email.com");
    Dataset dataset = new Dataset();
    dataset.setCreateUserId(datasetCreator.getUserId());
    User studyCreator = new User();
    studyCreator.setUserId(2);
    studyCreator.setEmail("sCreator#email.com");
    Study study = new Study();
    study.setStudyId(studyCreator.getUserId());
    study.setCreateUserEmail(studyCreator.getEmail());
    study.setCreateUserId(studyCreator.getUserId());
    dataset.setStudy(study);
    dataset.setStudyId(study.getStudyId());

    boolean isCreateUser = datasetService.isCreatorOrCustodian(studyCreator, dataset);
    assertTrue(isCreateUser);
  }

  @Test
  void testIsCreatorOrCustodian_Custodian() {
    Gson gson = GsonUtil.getInstance();
    User studyCreator = new User();
    studyCreator.setUserId(1);
    studyCreator.setEmail("test@email.com");
    User custodian = new User();
    custodian.setUserId(2);
    custodian.setEmail("custodian@test.com");
    Dataset dataset = new Dataset();
    dataset.setCreateUserId(studyCreator.getUserId());
    Study study = new Study();
    study.setStudyId(1);
    study.setCreateUserEmail(studyCreator.getEmail());
    study.setCreateUserId(studyCreator.getUserId());
    StudyProperty prop = new StudyProperty();
    prop.setKey(DatasetRegistrationSchemaV1Builder.dataCustodianEmail);
    prop.setType(PropertyType.Json);
    prop.setValue(gson.toJson(List.of(custodian.getEmail())));
    study.addProperties(prop);
    dataset.setStudy(study);
    dataset.setStudyId(study.getStudyId());

    boolean isCustodian = datasetService.isCreatorOrCustodian(custodian, dataset);
    assertTrue(isCustodian);
  }

  @Test
  void testIsAuthorizedToListUsers() {
    when(datasetAuthorizationReaderDAO.findAuthorizedReadersByDatasetIdAndUserId(
            anyLong(), anyLong()))
        .thenReturn(new DatasetAuthorizationReader(1, 1, 1, 1, Timestamp.from(Instant.now())));
    assertTrue(datasetService.isAuthorizedToListUsers(1, 1));
  }

  @Test
  void testIsAuthorizedToListUsersNotFound() {
    when(datasetAuthorizationReaderDAO.findAuthorizedReadersByDatasetIdAndUserId(
            anyLong(), anyLong()))
        .thenReturn(null);
    assertFalse(datasetService.isAuthorizedToListUsers(1, 1));
  }

  @Test
  void testAddAuthorizedReader() {
    DatasetAuthorizationReader dauthr =
        new DatasetAuthorizationReader(1, 1, 1, 1, Timestamp.from(Instant.now()));
    when(datasetAuthorizationReaderDAO.addAuthorizedReaderToDataset(
            anyLong(), anyLong(), anyLong()))
        .thenReturn(dauthr.id());
    when(datasetAuthorizationReaderDAO.findAuthorizedReaderByRecordId(anyLong()))
        .thenReturn(dauthr);
    DatasetAuthorizationReader response = datasetService.addAuthorizedReader(1, 1, 1);
    assertNotNull(response);
    assertEquals(dauthr, response);
  }

  @Test
  void testRemoveAuthorizedAccessReader() {
    doNothing().when(datasetAuthorizationReaderDAO).deleteByDatasetAndUserId(anyLong(), anyLong());
    assertDoesNotThrow(() -> datasetAuthorizationReaderDAO.deleteByDatasetAndUserId(1, 1));
  }

  @Test
  void testPatchStudy() throws Exception {
    Study study = new Study();
    study.setStudyId(1);
    study.setName("Study Name");
    User user = new User();
    user.setUserId(1);
    StudyPatch patch =
        new StudyPatch(
            "New Study Name",
            StudyType.OBSERVATIONAL,
            "New Description",
            null,
            "New Phenotype",
            "New Species",
            "New PI",
            null,
            null,
            null,
            true);
    when(datasetServiceDAO.patchStudy(study, user, patch)).thenReturn(study);
    when(studyDAO.findStudyById(study.getStudyId())).thenReturn(study);
    assertDoesNotThrow(() -> datasetService.patchStudy(study.getStudyId(), user, patch));
  }

  /* Helper functions */

  private List<Dataset> getDatasets() {
    return IntStream.range(1, 3)
        .mapToObj(
            i -> {
              Dataset dataset = new Dataset();
              dataset.setDatasetId(i);
              dataset.setName("Test Dataset " + i);
              dataset.setProperties(Collections.emptySet());
              return dataset;
            })
        .toList();
  }
}
