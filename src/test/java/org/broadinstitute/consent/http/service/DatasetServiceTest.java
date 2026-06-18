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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.gson.Gson;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
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
import org.broadinstitute.consent.http.enumeration.PropertyType;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.mail.message.DatasetApprovedMessage;
import org.broadinstitute.consent.http.mail.message.DatasetDeniedMessage;
import org.broadinstitute.consent.http.models.ApprovedDataset;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.DatasetAuthorizationReader;
import org.broadinstitute.consent.http.models.DatasetProperty;
import org.broadinstitute.consent.http.models.DatasetStudySummary;
import org.broadinstitute.consent.http.models.Dictionary;
import org.broadinstitute.consent.http.models.Study;
import org.broadinstitute.consent.http.models.StudyConversion;
import org.broadinstitute.consent.http.models.StudyPatch;
import org.broadinstitute.consent.http.models.StudyProperty;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.models.dataset_registration_v1.DatasetRegistrationSchemaV1.StudyType;
import org.broadinstitute.consent.http.models.dataset_registration_v1.builder.DatasetRegistrationSchemaV1Builder;
import org.broadinstitute.consent.http.service.dao.DatasetServiceDAO;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DatasetServiceTest extends AbstractTestHelper {

  private DatasetService datasetService;

  @Mock private Jdbi jdbi;
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
    when(jdbi.onDemand(DatasetAuthorizationReaderDAO.class))
        .thenReturn(datasetAuthorizationReaderDAO);
    when(jdbi.onDemand(DatasetDAO.class)).thenReturn(datasetDAO);
    when(jdbi.onDemand(DaaDAO.class)).thenReturn(daaDAO);
    when(jdbi.onDemand(DacDAO.class)).thenReturn(dacDAO);
    when(jdbi.onDemand(StudyDAO.class)).thenReturn(studyDAO);
    when(jdbi.onDemand(UserDAO.class)).thenReturn(userDAO);
    datasetService =
        new DatasetService(
            jdbi, datasetServiceDAO, elasticSearchService, emailService, ontologyService);
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
    when(datasetDAO.getDatasetByName(getDatasets().getFirst().getName().toLowerCase()))
        .thenReturn(getDatasets().getFirst());

    Dataset dataset = datasetService.getDatasetByName("Test Dataset 1");

    assertNotNull(dataset);
    assertEquals(dataset.getDatasetId(), getDatasets().getFirst().getDatasetId());
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
    when(datasetDAO.findDatasetById(getDatasets().getFirst().getDatasetId()))
        .thenReturn(getDatasets().getFirst());

    Dataset dataset = datasetService.findDatasetById(mockUser, 1);

    assertNotNull(dataset);
    assertEquals(dataset.getName(), getDatasets().getFirst().getName());
  }

  @Test
  void testFindDatasetByIdForRead() {
    Dataset dataset = getDatasets().getFirst();
    dataset.setStudyId(null);
    when(datasetDAO.findDatasetById(dataset.getDatasetId())).thenReturn(dataset);

    Dataset result = datasetService.findDatasetByIdForRead(mockUser, dataset.getDatasetId());

    assertNotNull(result);
    assertEquals(dataset.getDatasetId(), result.getDatasetId());
  }

  @Test
  void testFindDatasetByIdForReadNotFound() {
    when(datasetDAO.findDatasetById(99)).thenReturn(null);

    assertThrows(
        NotFoundException.class, () -> datasetService.findDatasetByIdForRead(mockUser, 99));
  }

  @Test
  void testFindDatasetByIdForReadForbidden() {
    User user = new User();
    user.setUserId(1);
    user.setEmail("user@email.com");
    Dataset dataset = new Dataset();
    dataset.setDatasetId(5);
    dataset.setCreateUserId(3);
    Study study = new Study();
    study.setStudyId(7);
    study.setCreateUserId(4);
    study.setPublicVisibility(false);
    StudyProperty property = new StudyProperty();
    property.setKey("other");
    property.setValue("[]");
    study.addProperties(property);
    dataset.setStudyId(study.getStudyId());
    dataset.setStudy(study);

    int datasetId = dataset.getDatasetId();
    when(datasetDAO.findDatasetById(datasetId)).thenReturn(dataset);

    assertThrows(
        ForbiddenException.class, () -> datasetService.findDatasetByIdForRead(user, datasetId));
  }

  @Test
  void testFindStudyByIdForRead() {
    User user = new User();
    user.setUserId(1);
    Study study = new Study();
    study.setStudyId(7);
    study.setPublicVisibility(true);

    when(studyDAO.findStudyById(study.getStudyId())).thenReturn(study);

    Study result = datasetService.findStudyByIdForRead(user, study.getStudyId());
    assertEquals(study.getStudyId(), result.getStudyId());
  }

  @Test
  void testFindStudyByIdForReadNotFound() {
    when(studyDAO.findStudyById(99)).thenReturn(null);

    assertThrows(NotFoundException.class, () -> datasetService.findStudyByIdForRead(mockUser, 99));
  }

  @Test
  void testFindStudyByIdForReadForbidden() {
    User user = new User();
    user.setUserId(1);
    user.setEmail("user@email.com");
    Study study = new Study();
    study.setStudyId(7);
    study.setCreateUserId(4);
    study.setPublicVisibility(false);
    StudyProperty property = new StudyProperty();
    property.setKey("other");
    property.setValue("[]");
    study.addProperties(property);

    int studyId = study.getStudyId();
    when(studyDAO.findStudyById(study.getStudyId())).thenReturn(study);

    assertThrows(
        ForbiddenException.class, () -> datasetService.findStudyByIdForRead(user, studyId));
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
  void testFindMinimalDatasetByIdentifierVisible() {
    Dataset dataset = new Dataset();
    dataset.setAlias(1);
    dataset.setDatasetIdentifier();
    Study study = new Study();
    study.setStudyId(1);
    study.setPublicVisibility(Boolean.TRUE);
    dataset.setStudy(study);
    when(datasetDAO.findMinimalDatasetByAlias(dataset.getAlias())).thenReturn(dataset);
    Dataset found =
        datasetService.findMinimalDatasetByIdentifier(
            mockUser, dataset.getDatasetIdentifier(), false);
    assertNotNull(found);
  }

  @Test
  void testFindMinimalDatasetByIdentifierVisible2() {
    Dataset dataset = new Dataset();
    dataset.setAlias(1);
    dataset.setDatasetIdentifier();
    Study study = new Study();
    study.setStudyId(1);
    study.setPublicVisibility(Boolean.TRUE);
    dataset.setStudy(study);
    dataset.setStudyId(study.getStudyId());
    when(datasetDAO.findMinimalDatasetByAlias(dataset.getAlias())).thenReturn(dataset);
    when(studyDAO.findStudyById(study.getStudyId())).thenReturn(study);
    Dataset found =
        datasetService.findMinimalDatasetByIdentifier(
            mockUser, dataset.getDatasetIdentifier(), true);
    assertNotNull(found);
  }

  @Test
  @SuppressWarnings({"java:S5778"})
  void testFindMinimalDatasetByIdentifierNotFound() {
    Dataset dataset = new Dataset();
    dataset.setAlias(1);
    dataset.setDatasetIdentifier();
    when(datasetDAO.findMinimalDatasetByAlias(dataset.getAlias())).thenReturn(null);
    assertThrows(
        NotFoundException.class,
        () ->
            datasetService.findMinimalDatasetByIdentifier(
                mockUser, dataset.getDatasetIdentifier(), false));
  }

  @Test
  @SuppressWarnings({"java:S5778"})
  void testFindMinimalDatasetByIdentifierIncorrectIdentifier() {
    Dataset dataset = new Dataset();
    dataset.setAlias(1);
    dataset.setDatasetIdentifier();
    when(datasetDAO.findMinimalDatasetByAlias(2)).thenReturn(dataset);
    Dataset found = datasetService.findMinimalDatasetByIdentifier(mockUser, "DUOS-000002", false);
    assertNull(found);
  }

  @Test
  void testUpdateDatasetDataUseAdmin() {
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
    } catch (IllegalArgumentException _) {
      assertTrue(true);
    }
  }

  @Test
  @SuppressWarnings({"java:S5778"})
  void testUpdateDatasetDataUseNullDataset() {
    when(datasetDAO.findDatasetById(any())).thenReturn(null);
    assertThrows(
        NotFoundException.class,
        () -> datasetService.updateDatasetDataUse(mockUser, 1, new DataUse()));
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
    verify(emailService, times(0)).sendMessage(any(DatasetApprovedMessage.class), any());
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
    User creatorUser = new User();
    creatorUser.setUserId(2);
    creatorUser.setEmail("creator@gmail.com");
    creatorUser.setDisplayName("Jane Doe");
    Dataset dataset = new Dataset();
    dataset.setDatasetId(1);
    dataset.setName("Test Dataset");
    dataset.setCreateUser(creatorUser);
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
    verify(emailService, times(1)).sendMessage(any(DatasetApprovedMessage.class), any());
  }

  @Test
  void testApproveDataset_DenyDataset() throws Exception {
    User creatorUser = new User();
    creatorUser.setUserId(2);
    creatorUser.setEmail("creator@gmail.com");
    creatorUser.setDisplayName("Jane Doe");
    Dataset dataset = new Dataset();
    dataset.setDatasetId(1);
    dataset.setName("Test Dataset");
    dataset.setCreateUser(creatorUser);
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
    verify(emailService, times(1)).sendMessage(any(DatasetDeniedMessage.class), any());
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
    verify(emailService, never()).sendMessage(any(DatasetDeniedMessage.class), any());
  }

  @Test
  void testSyncDataUseTranslation() {
    Dataset dataset = new Dataset();
    dataset.setDatasetId(1);
    when(datasetDAO.findDatasetById(1)).thenReturn(dataset);

    Dataset updated = datasetService.syncDatasetDataUseTranslation(1, mockUser);
    assertNotNull(updated);
    assertEquals(dataset.getDatasetId(), updated.getDatasetId());
  }

  @Test
  void testSyncDataUseTranslationNotFound() {
    when(datasetDAO.findDatasetById(1)).thenReturn(null);
    assertThrows(
        NotFoundException.class, () -> datasetService.syncDatasetDataUseTranslation(1, mockUser));
  }

  @ParameterizedTest
  @ValueSource(ints = {200, 500})
  void testDeleteDataset(int status) throws Exception {
    Dataset dataset = new Dataset();
    dataset.setDatasetId(1);
    when(datasetDAO.findDatasetById(1)).thenReturn(dataset);
    Response response = mock(Response.class);
    when(response.getStatus()).thenReturn(status);
    when(elasticSearchService.deleteIndex(any(), any())).thenReturn(response);

    datasetService.deleteDataset(dataset.getDatasetId(), mockUser.getUserId());
    verify(datasetServiceDAO).deleteDataset(dataset, mockUser.getUserId());
  }

  @ParameterizedTest
  @ValueSource(ints = {200, 500})
  void testDeleteStudy(int status) throws Exception {
    Study study = new Study();
    study.setStudyId(1);
    study.addDatasetIds(Set.of(1));
    Response response = mock(Response.class);
    when(response.getStatus()).thenReturn(status);
    when(elasticSearchService.deleteIndex(any(), any())).thenReturn(response);

    datasetService.deleteStudy(study, mockUser);
    verify(datasetServiceDAO).deleteStudy(study, mockUser);
  }

  @Test
  void testDeleteStudyDeletesEachDatasetFromIndex() throws Exception {
    Study study = new Study();
    study.setStudyId(1);
    study.addDatasetIds(Set.of(1, 2));
    User user = new User();
    user.setUserId(10);
    Response response = mock(Response.class);
    when(response.getStatus()).thenReturn(200);
    when(elasticSearchService.deleteIndex(any(), any())).thenReturn(response);

    datasetService.deleteStudy(study, user);

    verify(elasticSearchService).deleteIndex(1, user.getUserId());
    verify(elasticSearchService).deleteIndex(2, user.getUserId());
    verify(datasetServiceDAO).deleteStudy(study, user);
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
    assertTrue(datasetService.getApprovedDatasets(user).getFirst().isApprovedDatasetEqual(example));
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
    assertEquals(summary, authorizedSummaries.getFirst());
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
    assertEquals(summary, authorizedSummaries.getFirst());
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
    assertEquals(summary, authorizedSummaries.getFirst());
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
    assertEquals(summary, authorizedSummaries.getFirst());
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
    assertEquals(summary, authorizedSummaries.getFirst());
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
    assertEquals(summary, authorizedSummaries.getFirst());
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
    assertEquals(summary, authorizedSummaries.getFirst());
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
    assertEquals(summary, authorizedSummaries.getFirst());
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
            null,
            true);
    when(datasetServiceDAO.patchStudy(study, user, patch)).thenReturn(study);
    when(studyDAO.findStudyById(study.getStudyId())).thenReturn(study);
    assertDoesNotThrow(() -> datasetService.patchStudy(study.getStudyId(), user, patch));
  }

  // ==================== convertDatasetToStudy ====================

  @Test
  void testConvertDatasetToStudy_NonAdminThrows() {
    User nonAdmin = new User();
    nonAdmin.setUserId(2);
    Dataset dataset = new Dataset();
    StudyConversion conversion = new StudyConversion();
    assertThrows(
        NotAuthorizedException.class,
        () -> datasetService.convertDatasetToStudy(nonAdmin, dataset, conversion));
  }

  @Test
  void testConvertDatasetToStudy_AdminCreatesNewStudy() {
    User admin = getAdmin();
    Dataset dataset = buildDataset(10, 5);
    StudyConversion conversion = new StudyConversion();
    conversion.setName("New Study");

    int newStudyId = 42;
    Study createdStudy = new Study();
    createdStudy.setStudyId(newStudyId);

    when(studyDAO.findStudyByName("New Study")).thenReturn(null);
    when(studyDAO.insertStudy(any(), any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(newStudyId);
    when(studyDAO.findStudyById(newStudyId)).thenReturn(createdStudy);
    when(datasetDAO.getDictionaryTerms()).thenReturn(List.of());

    Study result = datasetService.convertDatasetToStudy(admin, dataset, conversion);

    assertNotNull(result);
    verify(studyDAO).insertStudy(any(), any(), any(), any(), any(), any(), any(), any(), any());
    verify(datasetDAO).updateStudyId(10, newStudyId);
  }

  @Test
  void testConvertDatasetToStudy_DacIdUpdated() {
    User admin = getAdmin();
    Dataset dataset = buildDataset(10, 5);
    StudyConversion conversion = new StudyConversion();
    conversion.setName("Study");
    conversion.setDacId(99);

    int newStudyId = 42;
    Study createdStudy = new Study();
    createdStudy.setStudyId(newStudyId);

    when(studyDAO.findStudyByName("Study")).thenReturn(null);
    when(studyDAO.insertStudy(any(), any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(newStudyId);
    when(studyDAO.findStudyById(newStudyId)).thenReturn(createdStudy);
    when(datasetDAO.getDictionaryTerms()).thenReturn(List.of());

    datasetService.convertDatasetToStudy(admin, dataset, conversion);

    verify(datasetDAO).updateDatasetDacId(10, 99);
  }

  @Test
  void testConvertDatasetToStudy_DataUseUpdated() {
    User admin = getAdmin();
    Dataset dataset = buildDataset(10, 5);
    DataUse dataUse = new DataUseBuilder().setGeneralUse(true).build();
    StudyConversion conversion = new StudyConversion();
    conversion.setName("Study");
    conversion.setDataUse(dataUse);

    int newStudyId = 42;
    Study createdStudy = new Study();
    createdStudy.setStudyId(newStudyId);

    when(studyDAO.findStudyByName("Study")).thenReturn(null);
    when(studyDAO.insertStudy(any(), any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(newStudyId);
    when(studyDAO.findStudyById(newStudyId)).thenReturn(createdStudy);
    when(datasetDAO.getDictionaryTerms()).thenReturn(List.of());
    when(ontologyService.translateDataUse(any(), any())).thenReturn("GRU");

    datasetService.convertDatasetToStudy(admin, dataset, conversion);

    verify(datasetServiceDAO).updateDatasetDataUse(eq(admin), eq(dataset), eq(dataUse), any());
  }

  @Test
  void testConvertDatasetToStudy_DatasetNameUpdated() {
    User admin = getAdmin();
    Dataset dataset = buildDataset(10, 5);
    StudyConversion conversion = new StudyConversion();
    conversion.setName("Study");
    conversion.setDatasetName("New Dataset Name");

    int newStudyId = 42;
    Study createdStudy = new Study();
    createdStudy.setStudyId(newStudyId);

    when(studyDAO.findStudyByName("Study")).thenReturn(null);
    when(studyDAO.insertStudy(any(), any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(newStudyId);
    when(studyDAO.findStudyById(newStudyId)).thenReturn(createdStudy);
    when(datasetDAO.getDictionaryTerms()).thenReturn(List.of());

    datasetService.convertDatasetToStudy(admin, dataset, conversion);

    verify(datasetDAO).updateDatasetName(10, "New Dataset Name");
  }

  @Test
  void testConvertDatasetToStudy_LegacyPropsInserted() {
    User admin = getAdmin();
    Dataset dataset = buildDataset(10, 5);
    StudyConversion conversion = new StudyConversion();
    conversion.setName("Study");
    conversion.setPhenotype("Cancer");
    conversion.setSpecies("Human");
    conversion.setNumberOfParticipants(200);

    int newStudyId = 42;
    Study createdStudy = new Study();
    createdStudy.setStudyId(newStudyId);

    Dictionary phenotypeDict = new Dictionary(1, "Phenotype/Indication", false, 1, 1);
    Dictionary speciesDict = new Dictionary(2, "Species", false, 2, 2);
    Dictionary participantsDict = new Dictionary(3, "# of participants", false, 3, 3);

    when(studyDAO.findStudyByName("Study")).thenReturn(null);
    when(studyDAO.insertStudy(any(), any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(newStudyId);
    when(studyDAO.findStudyById(newStudyId)).thenReturn(createdStudy);
    when(datasetDAO.getDictionaryTerms())
        .thenReturn(List.of(phenotypeDict, speciesDict, participantsDict));

    datasetService.convertDatasetToStudy(admin, dataset, conversion);

    verify(datasetDAO, times(3)).insertDatasetProperties(any());
  }

  @Test
  void testConvertDatasetToStudy_LegacyPropUpdated() {
    User admin = getAdmin();
    DatasetProperty existingPhenotype = new DatasetProperty();
    existingPhenotype.setPropertyName("Phenotype/Indication");
    existingPhenotype.setPropertyKey(1);
    Dataset dataset = buildDataset(10, 5);
    dataset.setProperties(Set.of(existingPhenotype));

    StudyConversion conversion = new StudyConversion();
    conversion.setName("Study");
    conversion.setPhenotype("UpdatedPhenotype");

    int newStudyId = 42;
    Study createdStudy = new Study();
    createdStudy.setStudyId(newStudyId);
    Dictionary phenotypeDict = new Dictionary(1, "Phenotype/Indication", false, 1, 1);

    when(studyDAO.findStudyByName("Study")).thenReturn(null);
    when(studyDAO.insertStudy(any(), any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(newStudyId);
    when(studyDAO.findStudyById(newStudyId)).thenReturn(createdStudy);
    when(datasetDAO.getDictionaryTerms()).thenReturn(List.of(phenotypeDict));

    datasetService.convertDatasetToStudy(admin, dataset, conversion);

    verify(datasetDAO).updateDatasetProperty(10, 1, "UpdatedPhenotype");
    verify(datasetDAO, never()).insertDatasetProperties(any());
  }

  @Test
  void testConvertDatasetToStudy_NewPropsInserted() {
    User admin = getAdmin();
    Dataset dataset = buildDataset(10, 5);
    StudyConversion conversion = new StudyConversion();
    conversion.setName("Study");
    conversion.setDataLocation("gs://bucket/data");
    conversion.setUrl("https://example.com/study");

    int newStudyId = 42;
    Study createdStudy = new Study();
    createdStudy.setStudyId(newStudyId);
    Dictionary dataLocationDict = new Dictionary(4, "Data Location", false, 4, 4);
    Dictionary urlDict = new Dictionary(5, "URL", false, 5, 5);

    when(studyDAO.findStudyByName("Study")).thenReturn(null);
    when(studyDAO.insertStudy(any(), any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(newStudyId);
    when(studyDAO.findStudyById(newStudyId)).thenReturn(createdStudy);
    when(datasetDAO.getDictionaryTerms()).thenReturn(List.of(dataLocationDict, urlDict));

    datasetService.convertDatasetToStudy(admin, dataset, conversion);

    verify(datasetDAO, times(2)).insertDatasetProperties(any());
  }

  @Test
  void testConvertDatasetToStudy_NewPropUpdated() {
    User admin = getAdmin();
    DatasetProperty existingDataLocation = new DatasetProperty();
    existingDataLocation.setSchemaProperty("dataLocation");
    existingDataLocation.setPropertyKey(4);
    Dataset dataset = buildDataset(10, 5);
    dataset.setProperties(Set.of(existingDataLocation));

    StudyConversion conversion = new StudyConversion();
    conversion.setName("Study");
    conversion.setDataLocation("gs://new-bucket/data");

    int newStudyId = 42;
    Study createdStudy = new Study();
    createdStudy.setStudyId(newStudyId);

    when(studyDAO.findStudyByName("Study")).thenReturn(null);
    when(studyDAO.insertStudy(any(), any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(newStudyId);
    when(studyDAO.findStudyById(newStudyId)).thenReturn(createdStudy);
    when(datasetDAO.getDictionaryTerms()).thenReturn(List.of());

    datasetService.convertDatasetToStudy(admin, dataset, conversion);

    verify(datasetDAO).updateDatasetProperty(10, 4, "gs://new-bucket/data");
    verify(datasetDAO, never()).insertDatasetProperties(any());
  }

  @Test
  void testConvertDatasetToStudy_DataSubmitterEmailFound() {
    User admin = getAdmin();
    Dataset dataset = buildDataset(10, 5);
    StudyConversion conversion = new StudyConversion();
    conversion.setName("Study");
    conversion.setDataSubmitterEmail("submitter@test.com");

    int newStudyId = 42;
    Study createdStudy = new Study();
    createdStudy.setStudyId(newStudyId);
    User submitter = new User();
    submitter.setUserId(99);

    when(studyDAO.findStudyByName("Study")).thenReturn(null);
    when(studyDAO.insertStudy(any(), any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(newStudyId);
    when(studyDAO.findStudyById(newStudyId)).thenReturn(createdStudy);
    when(datasetDAO.getDictionaryTerms()).thenReturn(List.of());
    when(userDAO.findUserByEmail("submitter@test.com")).thenReturn(submitter);

    datasetService.convertDatasetToStudy(admin, dataset, conversion);

    verify(datasetDAO).updateDatasetCreateUserId(anyInt(), anyInt());
  }

  @Test
  void testConvertDatasetToStudy_DataSubmitterEmailNotFound() {
    User admin = getAdmin();
    Dataset dataset = buildDataset(10, 5);
    StudyConversion conversion = new StudyConversion();
    conversion.setName("Study");
    conversion.setDataSubmitterEmail("unknown@test.com");

    int newStudyId = 42;
    Study createdStudy = new Study();
    createdStudy.setStudyId(newStudyId);

    when(studyDAO.findStudyByName("Study")).thenReturn(null);
    when(studyDAO.insertStudy(any(), any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(newStudyId);
    when(studyDAO.findStudyById(newStudyId)).thenReturn(createdStudy);
    when(datasetDAO.getDictionaryTerms()).thenReturn(List.of());
    when(userDAO.findUserByEmail("unknown@test.com")).thenReturn(null);

    datasetService.convertDatasetToStudy(admin, dataset, conversion);

    verify(datasetDAO, never()).updateDatasetCreateUserId(any(), any());
  }

  @Test
  void testConvertDatasetToStudy_AdminUpdatesExistingStudy() {
    User admin = getAdmin();
    Dataset dataset = buildDataset(10, 5);
    StudyConversion conversion = new StudyConversion();
    conversion.setName("Existing Study");

    Study existingStudy = new Study();
    existingStudy.setStudyId(77);

    when(studyDAO.findStudyByName("Existing Study")).thenReturn(existingStudy);
    when(studyDAO.findStudyById(77)).thenReturn(existingStudy);
    when(datasetDAO.getDictionaryTerms()).thenReturn(List.of());

    Study result = datasetService.convertDatasetToStudy(admin, dataset, conversion);

    assertNotNull(result);
    verify(studyDAO).updateStudy(any(), any(), any(), any(), any(), any(), any(), any(), any());
    verify(studyDAO, never())
        .insertStudy(any(), any(), any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  void testConvertDatasetToStudy_StudyPropsInsertedWhenKeyMissing() {
    User admin = getAdmin();
    Dataset dataset = buildDataset(10, 5);
    StudyConversion conversion = new StudyConversion();
    conversion.setName("Study");
    conversion.setNihAnvilUse("Yes");

    int newStudyId = 42;
    Study createdStudy = new Study();
    createdStudy.setStudyId(newStudyId);
    // Existing prop has a different key — nihAnvilUse is absent
    createdStudy.addProperties(
        new StudyProperty("phenotypeIndication", "Cancer", PropertyType.String));

    when(studyDAO.findStudyByName("Study")).thenReturn(null);
    when(studyDAO.insertStudy(any(), any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(newStudyId);
    when(studyDAO.findStudyById(newStudyId)).thenReturn(createdStudy);
    when(datasetDAO.getDictionaryTerms()).thenReturn(List.of());

    datasetService.convertDatasetToStudy(admin, dataset, conversion);

    verify(studyDAO).insertStudyProperty(eq(newStudyId), eq("nihAnvilUse"), any(), any());
  }

  @Test
  void testConvertDatasetToStudy_StudyPropsUpdatedWhenMatching() {
    User admin = getAdmin();
    Dataset dataset = buildDataset(10, 5);
    StudyConversion conversion = new StudyConversion();
    conversion.setName("Study");
    conversion.setNihAnvilUse("Yes");

    int newStudyId = 42;
    Study createdStudy = new Study();
    createdStudy.setStudyId(newStudyId);
    // Existing prop matches exactly — same key, value, and type as the conversion prop
    createdStudy.addProperties(new StudyProperty("nihAnvilUse", "Yes", PropertyType.String));

    when(studyDAO.findStudyByName("Study")).thenReturn(null);
    when(studyDAO.insertStudy(any(), any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(newStudyId);
    when(studyDAO.findStudyById(newStudyId)).thenReturn(createdStudy);
    when(datasetDAO.getDictionaryTerms()).thenReturn(List.of());

    datasetService.convertDatasetToStudy(admin, dataset, conversion);

    verify(studyDAO).updateStudyProperty(eq(newStudyId), eq("nihAnvilUse"), any(), any());
  }

  @Test
  void testConvertDatasetToStudy_NullDatasetCreateUserIdFallsBackToAdminId() {
    User admin = getAdmin();
    Dataset dataset = buildDataset(10, null); // null createUserId → use admin's userId
    StudyConversion conversion = new StudyConversion();
    conversion.setName("Study");

    int newStudyId = 42;
    Study createdStudy = new Study();
    createdStudy.setStudyId(newStudyId);

    when(studyDAO.findStudyByName("Study")).thenReturn(null);
    when(studyDAO.insertStudy(
            any(), any(), any(), any(), any(), any(), eq(admin.getUserId()), any(), any()))
        .thenReturn(newStudyId);
    when(studyDAO.findStudyById(newStudyId)).thenReturn(createdStudy);
    when(datasetDAO.getDictionaryTerms()).thenReturn(List.of());

    Study result = datasetService.convertDatasetToStudy(admin, dataset, conversion);

    assertNotNull(result);
    verify(studyDAO)
        .insertStudy(any(), any(), any(), any(), any(), any(), eq(admin.getUserId()), any(), any());
  }

  // ==================== updateStudyCustodians ====================

  @Test
  void testUpdateStudyCustodians_StudyNotFound() {
    when(studyDAO.findStudyById(anyInt())).thenReturn(null);
    assertThrows(
        NotFoundException.class, () -> datasetService.updateStudyCustodians(mockUser, 99, "[]"));
  }

  // ==================== deleteDataset ====================

  @Test
  void testDeleteDataset_NullDataset() throws Exception {
    when(datasetDAO.findDatasetById(99)).thenReturn(null);
    assertDoesNotThrow(() -> datasetService.deleteDataset(99, 1));
    verify(datasetServiceDAO, never()).deleteDataset(any(), any());
  }

  // ==================== isCreatorCustodianOrAdmin ====================

  @Test
  void testIsCreatorCustodianOrAdmin_Admin() {
    User admin = getAdmin();
    Study study = new Study();
    study.setStudyId(1);
    study.setCreateUserId(2);
    assertTrue(datasetService.isCreatorCustodianOrAdmin(admin, study));
  }

  @Test
  void testIsCreatorCustodianOrAdmin_Creator() {
    User creator = new User();
    creator.setUserId(5);
    creator.setEmail("creator@test.com");
    Study study = new Study();
    study.setStudyId(1);
    study.setCreateUserId(creator.getUserId());
    assertTrue(datasetService.isCreatorCustodianOrAdmin(creator, study));
  }

  @Test
  void testIsCreatorCustodianOrAdmin_NeitherAdminNorCreatorNorCustodian() {
    User user = new User();
    user.setUserId(5);
    user.setEmail("user@test.com");
    Study study = new Study();
    study.setStudyId(1);
    study.setCreateUserId(99);
    assertFalse(datasetService.isCreatorCustodianOrAdmin(user, study));
  }

  // ==================== patchStudy – exception path ====================

  @Test
  void testPatchStudy_ExceptionThrowsInternalServerError() throws Exception {
    Study study = new Study();
    study.setStudyId(1);
    User user = new User();
    user.setUserId(1);
    StudyPatch patch =
        new StudyPatch(null, null, null, null, null, null, null, null, null, null, null, null);

    when(studyDAO.findStudyById(1)).thenReturn(study);
    when(datasetServiceDAO.patchStudy(any(), any(), any()))
        .thenThrow(new RuntimeException("DB error"));

    assertThrows(
        InternalServerErrorException.class, () -> datasetService.patchStudy(1, user, patch));
  }

  // ==================== findDatasetsByIds ====================

  @Test
  void testFindDatasetsByIds_AdminSeesAll() {
    User admin = getAdmin();
    Dataset d1 = new Dataset();
    d1.setDatasetId(1);
    d1.setCreateUserId(99);

    when(datasetDAO.findDatasetsByIdList(anyList())).thenReturn(List.of(d1));

    List<Dataset> result = datasetService.findDatasetsByIds(admin, List.of(1));

    assertEquals(1, result.size());
  }

  @Test
  void testFindDatasetsByIds_FilteredByVisibility() {
    User user = new User();
    user.setUserId(1);
    user.setEmail("user@test.com");

    Study privateStudy = new Study();
    privateStudy.setStudyId(10);
    privateStudy.setCreateUserId(99);
    privateStudy.setPublicVisibility(false);

    Dataset d1 = new Dataset();
    d1.setDatasetId(1);
    d1.setCreateUserId(99);
    d1.setStudyId(10);
    d1.setStudy(privateStudy);

    when(datasetDAO.findDatasetsByIdList(anyList())).thenReturn(List.of(d1));

    List<Dataset> result = datasetService.findDatasetsByIds(user, List.of(1));

    assertEquals(0, result.size());
  }

  // ==================== canReadStudy ====================

  @Test
  void testCanReadStudy_NullStudy() {
    User user = new User();
    user.setUserId(1);
    assertFalse(datasetService.canReadStudy(user, null));
  }

  @Test
  void testCanReadStudy_Admin() {
    User admin = getAdmin();
    Study study = new Study();
    study.setPublicVisibility(false);
    assertTrue(datasetService.canReadStudy(admin, study));
  }

  @Test
  void testCanReadStudy_PublicVisibilityNull() {
    User user = new User();
    user.setUserId(1);
    Study study = new Study();
    // publicVisibility null → !Boolean.FALSE.equals(null) is true → readable
    assertTrue(datasetService.canReadStudy(user, study));
  }

  @Test
  void testCanReadStudy_PrivateFalseNotCreatorNorCustodian() {
    User user = new User();
    user.setUserId(1);
    user.setEmail("user@test.com");
    Study study = new Study();
    study.setCreateUserId(99);
    study.setPublicVisibility(false);
    assertFalse(datasetService.canReadStudy(user, study));
  }

  // ==================== isCreatorOrCustodian(User, Study) – null study ====================

  @Test
  void testIsCreatorOrCustodian_NullStudy() {
    User user = new User();
    user.setUserId(1);
    assertFalse(datasetService.isCreatorOrCustodian(user, (Study) null));
  }

  // ==================== Helper ==================== //

  private Dataset buildDataset(int datasetId, Integer createUserId) {
    Dataset dataset = new Dataset();
    dataset.setDatasetId(datasetId);
    dataset.setCreateUserId(createUserId);
    dataset.setProperties(Collections.emptySet());
    return dataset;
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
