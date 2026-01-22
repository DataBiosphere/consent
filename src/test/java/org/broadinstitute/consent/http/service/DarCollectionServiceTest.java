package org.broadinstitute.consent.http.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import freemarker.template.TemplateException;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.broadinstitute.consent.http.AbstractTestHelper;
import org.broadinstitute.consent.http.db.DacDAO;
import org.broadinstitute.consent.http.db.DarCollectionDAO;
import org.broadinstitute.consent.http.db.DarCollectionSummaryDAO;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.enumeration.DarCollectionActions;
import org.broadinstitute.consent.http.enumeration.DarCollectionStatus;
import org.broadinstitute.consent.http.enumeration.DarStatus;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.exceptions.ConsentConflictException;
import org.broadinstitute.consent.http.models.CloseoutSupplement;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DarCollection;
import org.broadinstitute.consent.http.models.DarCollectionSummary;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.models.Vote;
import org.broadinstitute.consent.http.rules.DACAutomationRule;
import org.broadinstitute.consent.http.rules.DACAutomationRuleType;
import org.broadinstitute.consent.http.service.dao.DarCollectionServiceDAO;
import org.glassfish.jersey.server.ContainerRequest;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DarCollectionServiceTest extends AbstractTestHelper {

  private DarCollectionService service;

  @Mock private DarCollectionDAO darCollectionDAO;
  @Mock private DarCollectionSummaryDAO darCollectionSummaryDAO;
  @Mock private DarCollectionServiceDAO darCollectionServiceDAO;
  @Mock private DatasetDAO datasetDAO;
  @Mock private ElectionDAO electionDAO;
  @Mock private DataAccessRequestDAO dataAccessRequestDAO;
  @Mock private EmailService emailService;
  @Mock private VoteDAO voteDAO;
  @Mock private UserDAO userDAO;
  @Mock private DacDAO dacDAO;
  @Mock private Jdbi jdbi;
  @Mock private DACAutomationRuleService dacAutomationRuleService;
  @Mock private ContainerRequest request;

  @BeforeEach
  void setUp() {
    when(jdbi.onDemand(DarCollectionDAO.class)).thenReturn(darCollectionDAO);
    when(jdbi.onDemand(DarCollectionSummaryDAO.class)).thenReturn(darCollectionSummaryDAO);
    when(jdbi.onDemand(DatasetDAO.class)).thenReturn(datasetDAO);
    when(jdbi.onDemand(ElectionDAO.class)).thenReturn(electionDAO);
    when(jdbi.onDemand(DataAccessRequestDAO.class)).thenReturn(dataAccessRequestDAO);
    when(jdbi.onDemand(VoteDAO.class)).thenReturn(voteDAO);
    when(jdbi.onDemand(UserDAO.class)).thenReturn(userDAO);
    when(jdbi.onDemand(DacDAO.class)).thenReturn(dacDAO);
    service =
        new DarCollectionService(
            jdbi, darCollectionServiceDAO, emailService, dacAutomationRuleService);
  }

  @Test
  void testAddDatasetsToCollection() {
    Set<Dataset> datasets = new HashSet<>();
    DarCollection collection = generateMockDarCollection(datasets);
    List<Integer> datasetIds = datasets.stream().map(Dataset::getDatasetId).sorted().toList();

    when(datasetDAO.findDatasetsByIdList(anyList())).thenReturn(List.copyOf(datasets));
    when(dataAccessRequestDAO.findAllDARDatasetRelations(any())).thenReturn(datasetIds);

    collection = service.addDatasetsToCollection(collection);
    assertNotNull(collection);

    Set<Dataset> datasetsFromCollection = collection.getDatasets();
    assertEquals(datasetIds.size(), datasetsFromCollection.size());

    List<Integer> collectionDatasetIds =
        datasetsFromCollection.stream().map(Dataset::getDatasetId).sorted().toList();
    assertEquals(datasetIds, collectionDatasetIds);
  }

  @Test
  void testGetByCollectionId() {
    User user = mock(User.class);
    Integer collectionId = 1;
    DarCollection collection = new DarCollection();
    collection.setDarCollectionId(collectionId);
    when(darCollectionDAO.findDARCollectionByCollectionId(collectionId)).thenReturn(collection);
    DarCollection result = service.getByCollectionId(user, collectionId);
    assertNotNull(result);
    assertEquals(collectionId, result.getDarCollectionId());
  }

  @Test
  void testGetByCollectionId_NotFound() {
    User user = mock(User.class);
    Integer collectionId = 1;
    when(darCollectionDAO.findDARCollectionByCollectionId(collectionId)).thenReturn(null);

    assertThrows(NotFoundException.class, () -> service.getByCollectionId(user, collectionId));
  }

  @Test
  void testGetCollectionById_ServiceException() {
    User user = mock(User.class);
    Integer collectionId = 1;
    RuntimeException expectedException = new RuntimeException("Test exception");
    when(darCollectionDAO.findDARCollectionByCollectionId(collectionId))
        .thenThrow(expectedException);

    RuntimeException exception =
        assertThrows(RuntimeException.class, () -> service.getByCollectionId(user, collectionId));
    assertEquals(expectedException, exception);
  }

  @ParameterizedTest
  @EnumSource(
      value = UserRoles.class,
      names = {"ADMIN", "MEMBER", "CHAIRPERSON"})
  void testGetByCollectionIdWithVotes(UserRoles role) {
    User user = new User();
    user.addRole(new UserRole(role.getRoleId(), role.getRoleName()));
    Integer collectionId = 1;
    DarCollection collection = mockDarCollectionWithElectionAndVotes(user);
    when(darCollectionDAO.findDARCollectionByCollectionId(collectionId)).thenReturn(collection);

    DarCollection result = service.getByCollectionId(user, collectionId);
    assertNotNull(result);
    List<Vote> votes =
        result.getDars().values().stream()
            .flatMap(d -> d.getElections().values().stream())
            .map(Election::getVotes)
            .flatMap(v -> v.values().stream())
            .toList();
    assertFalse(votes.isEmpty());
  }

  @ParameterizedTest
  @EnumSource(
      value = UserRoles.class,
      names = {
        "RESEARCHER",
        "ALUMNI",
        "SIGNINGOFFICIAL",
        "DATASUBMITTER",
        "ITDIRECTOR",
        "SERVICE_ACCOUNT"
      })
  void testGetByCollectionIdWithoutVotes(UserRoles role) {
    User user = new User();
    user.addRole(new UserRole(role.getRoleId(), role.getRoleName()));
    Integer collectionId = 1;
    DarCollection collection = mockDarCollectionWithElectionAndVotes(user);
    when(darCollectionDAO.findDARCollectionByCollectionId(collectionId)).thenReturn(collection);

    DarCollection result = service.getByCollectionId(user, collectionId);
    assertNotNull(result);
    List<Vote> votes =
        result.getDars().values().stream()
            .flatMap(d -> d.getElections().values().stream())
            .map(Election::getVotes)
            .flatMap(v -> v.values().stream())
            .toList();
    assertTrue(votes.isEmpty());
  }

  @Test
  void testGetCollectionWithAllElectionsByCollectionId() {
    User user = new User();
    user.setAdminRole();
    Integer collectionId = 1;
    DarCollection collection = new DarCollection();
    collection.setDarCollectionId(collectionId);
    when(darCollectionDAO.findCollectionWithAllElectionsByCollectionId(collectionId))
        .thenReturn(collection);

    DarCollection result = service.getCollectionWithAllElectionsByCollectionId(user, collectionId);
    assertNotNull(result);
    assertEquals(collectionId, result.getDarCollectionId());
  }

  @Test
  void testGetCollectionWithAllElectionsByCollectionId_NotFound() {
    User user = new User();
    user.setAdminRole();
    Integer collectionId = 1;
    when(darCollectionDAO.findCollectionWithAllElectionsByCollectionId(collectionId))
        .thenReturn(null);

    assertThrows(
        NotFoundException.class,
        () -> service.getCollectionWithAllElectionsByCollectionId(user, collectionId));
  }

  @Test
  void testGetCollectionWithAllElectionsByCollectionId_ServiceException() {
    User user = new User();
    user.setAdminRole();
    Integer collectionId = 1;
    RuntimeException expectedException = new RuntimeException("Test exception");
    when(darCollectionDAO.findCollectionWithAllElectionsByCollectionId(collectionId))
        .thenThrow(expectedException);

    RuntimeException exception =
        assertThrows(
            RuntimeException.class,
            () -> service.getCollectionWithAllElectionsByCollectionId(user, collectionId));
    assertEquals(expectedException, exception);
  }

  @Test
  void testGetCollectionWithElectionsByCollectionIdAndDatasetIds() {
    User user = mock(User.class);
    Integer collectionId = 1;
    DarCollection collection = new DarCollection();
    collection.setDarCollectionId(collectionId);
    List<Integer> datasetIds = List.of(1, 2, 3);
    when(darCollectionDAO.findCollectionWithElectionsByCollectionIdAndDatasetIds(
            datasetIds, collectionId))
        .thenReturn(collection);

    DarCollection result =
        service.getCollectionWithElectionsByCollectionIdAndDatasetIds(
            user, datasetIds, collectionId);
    assertNotNull(result);
    assertEquals(collectionId, result.getDarCollectionId());
  }

  @Test
  void testGetCollectionWithElectionsByCollectionIdAndDatasetIds_NotFound() {
    User user = mock(User.class);
    Integer collectionId = 1;
    List<Integer> datasetIds = List.of(1, 2, 3);
    when(darCollectionDAO.findCollectionWithElectionsByCollectionIdAndDatasetIds(
            datasetIds, collectionId))
        .thenReturn(null);

    assertThrows(
        NotFoundException.class,
        () ->
            service.getCollectionWithElectionsByCollectionIdAndDatasetIds(
                user, datasetIds, collectionId));
  }

  @Test
  void testGetCollectionWithElectionsByCollectionIdAndDatasetIds_ServiceException() {
    User user = mock(User.class);
    Integer collectionId = 1;
    List<Integer> datasetIds = List.of(1, 2, 3);
    RuntimeException expectedException = new RuntimeException("Test exception");
    when(darCollectionDAO.findCollectionWithElectionsByCollectionIdAndDatasetIds(
            datasetIds, collectionId))
        .thenThrow(expectedException);

    RuntimeException exception =
        assertThrows(
            RuntimeException.class,
            () ->
                service.getCollectionWithElectionsByCollectionIdAndDatasetIds(
                    user, datasetIds, collectionId));
    assertEquals(expectedException, exception);
  }

  @Test
  void testCancelDarCollection_noElections() {
    Set<Dataset> datasets = new HashSet<>();
    DarCollection collection = generateMockDarCollection(datasets);
    collection.getDars().values().forEach(d -> d.getData().setStatus("Canceled"));
    when(darCollectionDAO.findDARCollectionByCollectionId(any())).thenReturn(collection);
    when(darCollectionSummaryDAO.getDarCollectionSummaryByCollectionId(
            collection.getDarCollectionId()))
        .thenReturn(new DarCollectionSummary());

    User user = new User();
    user.setUserId(1);
    collection.setCreateUserId(user.getUserId());
    when(electionDAO.findLastElectionsByReferenceIds(anyList())).thenReturn(List.of());
    DarCollection canceledCollection =
        service.cancelDarCollectionByRole(user, collection, UserRoles.RESEARCHER);
    for (DataAccessRequest collectionDar : canceledCollection.getDars().values()) {
      assertEquals("canceled", collectionDar.getData().getStatus().toLowerCase());
    }
  }

  @Test
  void testCancelDarCollection_electionPresent() {
    Set<Dataset> datasets = new HashSet<>();
    DarCollection collection = generateMockDarCollection(datasets);

    when(electionDAO.findLastElectionsByReferenceIds(anyList()))
        .thenReturn(List.of(new Election()));
    when(darCollectionSummaryDAO.getDarCollectionSummaryByCollectionId(
            collection.getDarCollectionId()))
        .thenReturn(new DarCollectionSummary());

    User user = new User();
    user.setUserId(1);
    collection.setCreateUserId(user.getUserId());
    assertThrows(
        BadRequestException.class,
        () -> service.cancelDarCollectionByRole(user, collection, UserRoles.RESEARCHER));
  }

  @Test
  void testCancelDarCollectionAsResearcher_NoElections() {
    DataAccessRequest dar = new DataAccessRequest();
    dar.setReferenceId(UUID.randomUUID().toString());
    DataAccessRequestData data = new DataAccessRequestData();
    dar.setData(data);
    DarCollection collection = createMockCollections().getFirst();
    collection.addDar(dar);
    when(electionDAO.findLastElectionsByReferenceIds(anyList())).thenReturn(List.of());
    when(darCollectionDAO.findDARCollectionByCollectionId(any())).thenReturn(collection);
    when(darCollectionSummaryDAO.getDarCollectionSummaryByCollectionId(
            collection.getDarCollectionId()))
        .thenReturn(new DarCollectionSummary());

    User user = new User();
    user.setUserId(1);
    collection.setCreateUserId(user.getUserId());
    service.cancelDarCollectionByRole(user, collection, UserRoles.RESEARCHER);
    verify(electionDAO).findLastElectionsByReferenceIds(anyList());
    verify(electionDAO, times(0)).updateElectionById(anyInt(), anyString(), any());
    verify(dataAccessRequestDAO).cancelByReferenceIds(anyList());
    verify(darCollectionDAO, atLeastOnce())
        .findDARCollectionByCollectionId(collection.getDarCollectionId());
  }

  @Test
  void testCancelDarCollectionAsResearcher_WithElections() {
    DataAccessRequest dar = new DataAccessRequest();
    dar.setReferenceId(UUID.randomUUID().toString());
    DataAccessRequestData data = new DataAccessRequestData();
    dar.setData(data);
    DarCollection collection = createMockCollections().getFirst();
    collection.addDar(dar);
    Election election = createMockElection();
    election.setReferenceId(dar.getReferenceId());
    election.setStatus(ElectionStatus.OPEN.getValue());
    election.setElectionId(1);
    User user = new User();
    user.setUserId(1);
    collection.setCreateUserId(user.getUserId());
    when(electionDAO.findLastElectionsByReferenceIds(anyList())).thenReturn(List.of(election));
    when(darCollectionSummaryDAO.getDarCollectionSummaryByCollectionId(
            collection.getDarCollectionId()))
        .thenReturn(new DarCollectionSummary());

    assertThrows(
        BadRequestException.class,
        () -> service.cancelDarCollectionByRole(user, collection, UserRoles.RESEARCHER));
  }

  @Test
  void testCancelDarCollectionAsAdmin() {
    DataAccessRequest dar = new DataAccessRequest();
    dar.setReferenceId(UUID.randomUUID().toString());
    DataAccessRequestData data = new DataAccessRequestData();
    dar.setData(data);
    DarCollection collection = createMockCollections().getFirst();
    collection.addDar(dar);
    Election election = createMockElection();
    election.setReferenceId(dar.getReferenceId());
    election.setStatus(ElectionStatus.OPEN.getValue());
    election.setElectionId(1);
    when(electionDAO.findOpenElectionsByReferenceIds(anyList())).thenReturn(List.of(election));
    when(darCollectionDAO.findDARCollectionByCollectionId(collection.getDarCollectionId()))
        .thenReturn(collection);

    service.cancelDarCollectionByRole(new User(), collection, UserRoles.ADMIN);
    verify(electionDAO).findOpenElectionsByReferenceIds(anyList());
    verify(electionDAO).updateElectionById(anyInt(), anyString(), any());
    verify(dataAccessRequestDAO, times(0)).cancelByReferenceIds(anyList());
    verify(darCollectionDAO, atLeastOnce())
        .findDARCollectionByCollectionId(collection.getDarCollectionId());
  }

  @Test
  void testCancelDarCollectionAsChair_ChairHasDatasets() {
    User user = new User();
    user.setUserId(randomInt(1, 10));
    Dataset dataset = new Dataset();
    dataset.setDatasetId(1);
    DataAccessRequest dar = new DataAccessRequest();
    dar.setReferenceId(UUID.randomUUID().toString());
    DataAccessRequestData data = new DataAccessRequestData();
    dar.addDatasetId(dataset.getDatasetId());
    dar.setData(data);
    DarCollection collection = createMockCollections().getFirst();
    collection.addDar(dar);
    Election election = createMockElection();
    election.setReferenceId(dar.getReferenceId());
    election.setStatus(ElectionStatus.OPEN.getValue());
    election.setElectionId(1);
    when(datasetDAO.findDatasetIdsByDACUserId(anyInt()))
        .thenReturn(List.of(dataset.getDatasetId()));
    when(electionDAO.findOpenElectionsByReferenceIds(anyList())).thenReturn(List.of(election));
    when(darCollectionDAO.findDARCollectionByCollectionId(collection.getDarCollectionId()))
        .thenReturn(collection);

    service.cancelDarCollectionByRole(user, collection, UserRoles.CHAIRPERSON);
    verify(datasetDAO).findDatasetIdsByDACUserId(anyInt());
    verify(electionDAO).findOpenElectionsByReferenceIds(anyList());
    verify(electionDAO).updateElectionById(anyInt(), anyString(), any());
    verify(dataAccessRequestDAO, times(0)).cancelByReferenceIds(anyList());
    verify(darCollectionDAO, atLeastOnce())
        .findDARCollectionByCollectionId(collection.getDarCollectionId());
  }

  @Test
  void testCancelDarCollectionAsChair_ChairHasNoDatasets() {
    User user = new User();
    user.setUserId(randomInt(1, 10));
    Dataset dataset = new Dataset();
    dataset.setDatasetId(1);
    DataAccessRequest dar = new DataAccessRequest();
    dar.setReferenceId(UUID.randomUUID().toString());
    DataAccessRequestData data = new DataAccessRequestData();
    dar.addDatasetId(dataset.getDatasetId());
    dar.setData(data);
    DarCollection collection = createMockCollections().getFirst();
    collection.addDar(dar);
    Election election = createMockElection();
    election.setReferenceId(dar.getReferenceId());
    election.setStatus(ElectionStatus.OPEN.getValue());
    election.setElectionId(1);
    when(datasetDAO.findDatasetIdsByDACUserId(anyInt())).thenReturn(List.of());
    when(darCollectionDAO.findDARCollectionByCollectionId(collection.getDarCollectionId()))
        .thenReturn(collection);

    service.cancelDarCollectionByRole(user, collection, UserRoles.CHAIRPERSON);
    verify(datasetDAO).findDatasetIdsByDACUserId(anyInt());
    verifyNoMoreInteractions(electionDAO);
  }

  @Test
  void cancelDarCollectionByRole_ProgressReport() {
    DarCollection collection = createMockCollections().getFirst();
    collection.addDar(new DataAccessRequest());
    DarCollectionSummary summary = new DarCollectionSummary();
    summary.addParentChildRelationship(123, "456");
    summary.setSubmissionDate(new Timestamp(0));
    when(darCollectionSummaryDAO.getDarCollectionSummaryByCollectionId(
            collection.getDarCollectionId()))
        .thenReturn(summary);
    User user = new User();
    user.setUserId(1);
    collection.setCreateUserId(user.getUserId());
    assertThrows(
        BadRequestException.class,
        () -> service.cancelDarCollectionByRole(user, collection, UserRoles.RESEARCHER));
  }

  @Test
  void cancelDarCollectionByRole_NoDars() {
    DarCollection collection = new DarCollection();
    var newCollection = service.cancelDarCollectionByRole(null, collection, UserRoles.RESEARCHER);
    assertEquals(collection, newCollection);
    verifyNoInteractions(electionDAO);
  }

  @Test
  void testCreateElectionsForDarCollection() throws Exception {
    User user = new User();
    user.setEmail("email");
    DataAccessRequest dar = new DataAccessRequest();
    dar.setReferenceId(UUID.randomUUID().toString());
    DarCollection collection = createMockCollections().getFirst();
    collection.addDar(dar);
    when(darCollectionServiceDAO.createElectionsForDarByUser(any(), any()))
        .thenReturn(List.of("electionId"));
    when(voteDAO.findVoteUsersByElectionReferenceIdList(any())).thenReturn(List.of(new User()));
    when(darCollectionDAO.findDARCollectionByCollectionId(collection.getDarCollectionId()))
        .thenReturn(collection);

    service.createElectionsForDarCollection(user, collection, request);
    verify(darCollectionServiceDAO).createElectionsForDarByUser(any(), eq(dar));
    verify(voteDAO).findVoteUsersByElectionReferenceIdList(any());
    verify(emailService).sendDarNewCollectionElectionMessage(any(), any());
    verify(darCollectionDAO).findDARCollectionByCollectionId(any());
  }

  @Test
  void testCreateElectionsForDarCollectionWithSORequired() throws Exception {
    User user = new User();
    user.setEmail("email");
    user.setUserId(1);
    User signingOfficial = new User();
    signingOfficial.setRoles(List.of(UserRoles.SigningOfficial()));
    signingOfficial.setEmail("email2");
    signingOfficial.setUserId(2);
    DataAccessRequest dar = new DataAccessRequest();
    dar.setReferenceId(UUID.randomUUID().toString());
    dar.setUserId(user.getUserId());
    DataAccessRequestData darData = new DataAccessRequestData();
    darData.setSigningOfficialEmail(signingOfficial.getEmail());
    dar.setData(darData);
    DarCollection collection = createMockCollections().getFirst();
    collection.addDar(dar);
    when(darCollectionServiceDAO.createElectionsForDarByUser(any(), any()))
        .thenReturn(List.of("electionId"));
    when(voteDAO.findVoteUsersByElectionReferenceIdList(any())).thenReturn(List.of(new User()));
    when(darCollectionDAO.findDARCollectionByCollectionId(collection.getDarCollectionId()))
        .thenReturn(collection);
    when(userDAO.findUserById(user.getUserId())).thenReturn(user);

    service.createElectionsForDarCollection(signingOfficial, collection, request);
    verify(dataAccessRequestDAO)
        .updateDarApprovalSO(signingOfficial.getUserId(), dar.getReferenceId());
    verify(darCollectionServiceDAO).createElectionsForDarByUser(any(), eq(dar));
    verify(voteDAO).findVoteUsersByElectionReferenceIdList(any());
    verify(emailService).sendDarNewCollectionElectionMessage(any(), any());
    verify(darCollectionDAO).findDARCollectionByCollectionId(any());
  }

  @Test
  void testCreateElectionsForDarCollectionWithSORequiredNoDARData() {
    User user = new User();
    user.setEmail("email");
    user.setUserId(1);
    User signingOfficial = new User();
    signingOfficial.setRoles(List.of(UserRoles.SigningOfficial()));
    signingOfficial.setEmail("email2");
    signingOfficial.setUserId(2);
    DataAccessRequest dar = new DataAccessRequest();
    dar.setReferenceId(UUID.randomUUID().toString());
    dar.setUserId(user.getUserId());
    DarCollection collection = createMockCollections().getFirst();
    collection.addDar(dar);

    assertThrows(
        ConsentConflictException.class,
        () -> service.createElectionsForDarCollection(signingOfficial, collection, request));
  }

  @Test
  void testCreateElectionsForDarCollectionWithSORequiredNotSOInDARThrows() {
    User user = new User();
    user.setEmail("email");
    user.setUserId(1);
    User signingOfficial = new User();
    signingOfficial.setRoles(List.of(UserRoles.SigningOfficial()));
    signingOfficial.setEmail("email2");
    signingOfficial.setUserId(2);
    DataAccessRequest dar = new DataAccessRequest();
    dar.setReferenceId(UUID.randomUUID().toString());
    dar.setUserId(user.getUserId());
    DataAccessRequestData darData = new DataAccessRequestData();
    darData.setSigningOfficialEmail("notTheSigningOfficialMakingRequest");
    dar.setData(darData);
    DarCollection collection = createMockCollections().getFirst();
    collection.addDar(dar);

    assertThrows(
        ForbiddenException.class,
        () -> service.createElectionsForDarCollection(signingOfficial, collection, request));
  }

  @Test
  void testCreateElectionsForDarCollectionWithSORequiredAlreadyApprovedDARThrows() {
    User user = new User();
    user.setEmail("email");
    user.setUserId(1);
    User signingOfficial = new User();
    signingOfficial.setRoles(List.of(UserRoles.SigningOfficial()));
    signingOfficial.setEmail("email2");
    signingOfficial.setUserId(2);
    DataAccessRequest dar = new DataAccessRequest();
    dar.setReferenceId(UUID.randomUUID().toString());
    dar.setUserId(user.getUserId());
    dar.setApprovingSigningOfficialUserId(signingOfficial.getUserId());
    dar.setApprovingSigningOfficialApprovedDate(Timestamp.from(Instant.now()));
    DataAccessRequestData darData = new DataAccessRequestData();
    darData.setSigningOfficialEmail("notTheSigningOfficialMakingRequest");
    dar.setData(darData);
    DarCollection collection = createMockCollections().getFirst();
    collection.addDar(dar);

    assertThrows(
        BadRequestException.class,
        () -> service.createElectionsForDarCollection(signingOfficial, collection, request));
  }

  @Test
  void testCreateElectionsForProgressReport() throws Exception {
    User user = new User();
    user.setEmail("email");
    DataAccessRequest dar = new DataAccessRequest();
    dar.setReferenceId(UUID.randomUUID().toString());
    dar.setId(randomInt(1, 10));
    dar.setSubmissionDate(Timestamp.from(Instant.now()));
    DataAccessRequest progressReport = new DataAccessRequest();
    progressReport.setReferenceId(UUID.randomUUID().toString());
    progressReport.setParentId(dar.getId());
    progressReport.setSubmissionDate(Timestamp.from(Instant.now()));
    DarCollection collection = createMockCollections().getFirst();
    collection.addDar(dar);
    collection.addDar(progressReport);

    User voteUser = new User();
    String electionId = "electionId";
    when(darCollectionServiceDAO.createElectionsForDarByUser(user, progressReport))
        .thenReturn(List.of(electionId));
    when(voteDAO.findVoteUsersByElectionReferenceIdList(List.of(electionId)))
        .thenReturn(List.of(voteUser));
    when(darCollectionDAO.findDARCollectionByCollectionId(collection.getDarCollectionId()))
        .thenReturn(collection);

    service.createElectionsForDarCollection(user, collection, request);

    verify(darCollectionDAO).findDARCollectionByCollectionId(collection.getDarCollectionId());
    verify(emailService)
        .sendProgressReportNewCollectionElectionMessage(List.of(voteUser), collection.getDarCode());
  }

  @Test
  void testCreateElectionsForDarCollectionEmpty() {
    User user = new User();
    user.setEmail("email");
    DataAccessRequest dar = new DataAccessRequest();
    dar.setReferenceId(UUID.randomUUID().toString());
    DarCollection collection = createMockCollections().getFirst();
    collection.addDar(dar);

    assertThrows(
        IllegalStateException.class,
        () -> service.createElectionsForDarCollection(user, collection, request));
  }

  @Test
  void testCreateElectionsForDarCollectionVoteUsersException() throws Exception {
    User user = new User();
    user.setEmail("email");
    DataAccessRequest dar = new DataAccessRequest();
    dar.setReferenceId(UUID.randomUUID().toString());
    DarCollection collection = createMockCollections().getFirst();
    collection.addDar(dar);
    List<String> electionIds = List.of("electionId");
    when(darCollectionServiceDAO.createElectionsForDarByUser(user, dar)).thenReturn(electionIds);
    when(voteDAO.findVoteUsersByElectionReferenceIdList(electionIds))
        .thenThrow(IllegalArgumentException.class);
    when(darCollectionDAO.findDARCollectionByCollectionId(collection.getDarCollectionId()))
        .thenReturn(collection);

    service.createElectionsForDarCollection(user, collection, request);
    verify(darCollectionServiceDAO).createElectionsForDarByUser(user, dar);
    verify(voteDAO).findVoteUsersByElectionReferenceIdList(electionIds);
    verifyNoInteractions(emailService);
    verify(darCollectionDAO).findDARCollectionByCollectionId(collection.getDarCollectionId());
  }

  @Test
  void testProcessDarCollectionSummariesForDAC_SO_InProcess() {
    User user = new User();
    user.setUserId(1);
    DarCollectionSummary summary = new DarCollectionSummary();
    Election electionOne = new Election();
    electionOne.setElectionId(1);
    electionOne.setStatus(ElectionStatus.OPEN.getValue());
    Dataset datasetOne = new Dataset();
    datasetOne.setDatasetId(1);
    summary.addElection(electionOne);
    summary.addDatasetId(datasetOne.getDatasetId());
    when(darCollectionSummaryDAO.getDarCollectionSummariesForSO(any()))
        .thenReturn(List.of(summary));

    List<DarCollectionSummary> summaries =
        service.getSummariesForRole(user, UserRoles.SIGNINGOFFICIAL);
    assertNotNull(summaries);
    assertEquals(1, summaries.size());
    DarCollectionSummary s = summaries.getFirst();
    assertTrue(s.getStatus().equalsIgnoreCase(DarCollectionStatus.IN_PROCESS.getValue()));
  }

  @Test
  void testProcessDarCollectionSummariesForDAC_SO_Complete() {
    User user = new User();
    user.setUserId(1);
    DarCollectionSummary summary = new DarCollectionSummary();
    Election electionOne = new Election();
    electionOne.setElectionId(1);
    electionOne.setStatus(ElectionStatus.CLOSED.getValue());
    Election electionTwo = new Election();
    electionTwo.setElectionId(2);
    electionTwo.setStatus((ElectionStatus.CANCELED.getValue()));
    Dataset datasetOne = new Dataset();
    datasetOne.setDatasetId(1);
    Dataset datasetTwo = new Dataset();
    datasetTwo.setDatasetId(2);
    summary.addElection(electionOne);
    summary.addElection(electionTwo);
    summary.addDatasetId(datasetOne.getDatasetId());
    summary.addDatasetId(datasetTwo.getDatasetId());
    when(darCollectionSummaryDAO.getDarCollectionSummariesForSO(any()))
        .thenReturn(List.of(summary));

    List<DarCollectionSummary> summaries =
        service.getSummariesForRole(user, UserRoles.SIGNINGOFFICIAL);
    assertNotNull(summaries);
    assertEquals(1, summaries.size());
    DarCollectionSummary s = summaries.getFirst();
    assertTrue(s.getStatus().equalsIgnoreCase(DarCollectionStatus.COMPLETE.getValue()));
  }

  @Test
  void testProcessDarCollectionSummariesForDAC_SO_Unreviewed() {
    User user = new User();
    user.setUserId(1);
    DarCollectionSummary summary = new DarCollectionSummary();
    Dataset datasetOne = new Dataset();
    datasetOne.setDatasetId(1);
    Dataset datasetTwo = new Dataset();
    datasetTwo.setDatasetId(2);
    summary.addDatasetId(datasetOne.getDatasetId());
    summary.addDatasetId(datasetTwo.getDatasetId());
    summary.setRequiresSOApproval(true);
    when(darCollectionSummaryDAO.getDarCollectionSummariesForSO(any()))
        .thenReturn(List.of(summary));

    List<DarCollectionSummary> summaries =
        service.getSummariesForRole(user, UserRoles.SIGNINGOFFICIAL);
    assertNotNull(summaries);
    assertEquals(1, summaries.size());
    DarCollectionSummary s = summaries.getFirst();
    assertTrue(s.getStatus().equalsIgnoreCase(DarCollectionStatus.SUBMITTED.getValue()));
    assertTrue(s.requiresSOApproval());
    assertTrue(s.getActions().contains(DarCollectionActions.APPROVE.getValue()));
  }

  @Test
  void testProcessDarCollectionSummariesForDAC_SO_Reviewed() {
    User user = new User();
    user.setUserId(1);
    DarCollectionSummary summary = new DarCollectionSummary();
    Dataset datasetOne = new Dataset();
    datasetOne.setDatasetId(1);
    Dataset datasetTwo = new Dataset();
    datasetTwo.setDatasetId(2);
    summary.addDatasetId(datasetOne.getDatasetId());
    summary.addDatasetId(datasetTwo.getDatasetId());
    summary.setRequiresSOApproval(true);
    summary.setSOApprover(1);
    when(darCollectionSummaryDAO.getDarCollectionSummariesForSO(any()))
        .thenReturn(List.of(summary));

    List<DarCollectionSummary> summaries =
        service.getSummariesForRole(user, UserRoles.SIGNINGOFFICIAL);
    assertNotNull(summaries);
    assertEquals(1, summaries.size());
    DarCollectionSummary s = summaries.getFirst();
    assertTrue(s.getStatus().equalsIgnoreCase(DarCollectionStatus.SUBMITTED.getValue()));
    assertTrue(s.requiresSOApproval());
    assertTrue(!s.getActions().contains(DarCollectionActions.APPROVE.getValue()));
  }

  @Test
  void testProcessDarCollectionSummariesForAdminWithCloseout() {
    User user = new User();
    user.setUserId(1);
    DarCollectionSummary summary = new DarCollectionSummary();
    summary.setLatestReferenceId(UUID.randomUUID().toString());
    summary.setCloseoutSupplement(new CloseoutSupplement(List.of("Closeout"), "Closeout", 1));
    when(darCollectionSummaryDAO.getDarCollectionSummariesForAdmin()).thenReturn(List.of(summary));

    List<DarCollectionSummary> summaries = service.getSummariesForRole(user, UserRoles.ADMIN);

    assertNotNull(summaries);
    assertEquals(1, summaries.size());
    // Admin summary should not have any actions
    assertTrue(summaries.getFirst().getActions().isEmpty());
  }

  @Test
  void testProcessDarCollectionSummariesForAdminWithoutCloseout() {
    User user = new User();
    user.setUserId(1);
    DarCollectionSummary summary = new DarCollectionSummary();
    summary.setLatestReferenceId(UUID.randomUUID().toString());
    when(darCollectionSummaryDAO.getDarCollectionSummariesForAdmin()).thenReturn(List.of(summary));

    List<DarCollectionSummary> summaries = service.getSummariesForRole(user, UserRoles.ADMIN);

    assertNotNull(summaries);
    assertEquals(1, summaries.size());
    // With no elections, there should be an Open action
    assertFalse(summaries.getFirst().getActions().contains(DarCollectionActions.OPEN.getValue()));
  }

  @Test
  void testProcessDarCollectionSummariesForResearcher() {

    // summaryOne -> in review (elections present)
    // summaryTwo -> no elections
    // summaryThree -> no elections, canceled
    // summaryFour -> closed election, approved datasets
    // summaryFive -> dar collection is a progress report
    // summarySix -> draft

    User user = new User();
    user.setUserId(1);

    DarCollectionSummary summaryOne = new DarCollectionSummary();
    Dataset datasetOne = new Dataset();
    datasetOne.setDatasetId(1);
    Dataset datasetTwo = new Dataset();
    datasetTwo.setDatasetId(2);
    Election electionOne = new Election();
    electionOne.setElectionId(1);
    electionOne.setStatus(ElectionStatus.OPEN.getValue());
    summaryOne.addElection(electionOne);
    summaryOne.addDatasetId(datasetOne.getDatasetId());
    summaryOne.addDatasetId(datasetTwo.getDatasetId());
    summaryOne.setLatestReferenceId("ref1");

    DarCollectionSummary summaryTwo = new DarCollectionSummary();
    Dataset datasetThree = new Dataset();
    datasetThree.setDatasetId(3);
    Dataset datasetFour = new Dataset();
    datasetFour.setDatasetId(4);
    summaryTwo.addDatasetId(datasetThree.getDatasetId());
    summaryTwo.addDatasetId(datasetFour.getDatasetId());
    summaryTwo.setLatestReferenceId("ref1");

    DarCollectionSummary summaryThree = new DarCollectionSummary();
    Dataset datasetFive = new Dataset();
    datasetFive.setDatasetId(5);
    summaryThree.addDatasetId(datasetFive.getDatasetId());
    summaryThree.addStatus(DarStatus.CANCELED.getValue(), randomAlphabetic(3));
    summaryThree.setLatestReferenceId("ref1");

    DarCollectionSummary summaryFour = new DarCollectionSummary();
    Dataset datasetSix = new Dataset();
    datasetSix.setDatasetId(6);
    summaryFour.addDatasetId(datasetSix.getDatasetId());
    Election electionTwo = new Election();
    electionOne.setElectionId(2);
    electionOne.setStatus(ElectionStatus.CLOSED.getValue());
    summaryFour.addElection(electionTwo);
    summaryFour.setLatestReferenceId("ref4");

    DarCollectionSummary summaryFive = new DarCollectionSummary();
    {
      summaryFive.setSubmissionDate(new Timestamp(0));
      summaryFive.addParentChildRelationship(123, "456");
      Election election = new Election();
      election.setElectionId(234);
      election.setStatus(ElectionStatus.OPEN.getValue());
      summaryFive.addElection(election);
      summaryFive.setLatestReferenceId("ref1");
    }

    DataAccessRequest draft = new DataAccessRequest();
    draft.setCreateDate(new Timestamp(new Date().getTime()));
    DataAccessRequestData data = new DataAccessRequestData();
    data.setProjectTitle(randomAlphabetic(10));
    draft.setData(data);
    when(dataAccessRequestDAO.findAllDraftsByUserId(any())).thenReturn(List.of(draft));
    when(darCollectionSummaryDAO.getDarCollectionSummariesForResearcher(any()))
        .thenReturn(List.of(summaryOne, summaryTwo, summaryThree, summaryFour, summaryFive));
    when(dataAccessRequestDAO.findDatasetApprovalsByDar("ref1")).thenReturn(Set.of());
    when(dataAccessRequestDAO.findDatasetApprovalsByDar("ref4"))
        .thenReturn(Set.of(datasetSix.getDatasetId()));

    List<DarCollectionSummary> summaries = service.getSummariesForRole(user, UserRoles.RESEARCHER);
    assertNotNull(summaries);
    assertEquals(6, summaries.size());

    DarCollectionSummary testOne = summaries.getFirst();
    Set<String> expectedOneActions = Set.of(DarCollectionActions.REVIEW.getValue());
    assertTrue(testOne.getStatus().equalsIgnoreCase(DarCollectionStatus.IN_PROCESS.getValue()));
    assertEquals(expectedOneActions, testOne.getActions());

    DarCollectionSummary testTwo = summaries.get(1);
    Set<String> expectedTwoActions =
        Set.of(DarCollectionActions.REVIEW.getValue(), DarCollectionActions.CANCEL.getValue());
    assertTrue(testTwo.getStatus().equalsIgnoreCase(DarCollectionStatus.SUBMITTED.getValue()));
    assertEquals(expectedTwoActions, testTwo.getActions());

    DarCollectionSummary testThree = summaries.get(2);
    Set<String> expectedThreeActions =
        Set.of(DarCollectionActions.REVIEW.getValue(), DarCollectionActions.REVISE.getValue());
    assertTrue(testThree.getStatus().equalsIgnoreCase(DarCollectionStatus.CANCELED.getValue()));
    assertEquals(expectedThreeActions, testThree.getActions());

    DarCollectionSummary testFour = summaries.get(3);
    Set<String> expectedFourActions =
        Set.of(
            DarCollectionActions.REVIEW.getValue(),
            DarCollectionActions.CREATE_PROGRESS_REPORT.getValue());
    assertTrue(testFour.getStatus().equalsIgnoreCase(DarCollectionStatus.COMPLETE.getValue()));
    assertEquals(testFour.getActions(), expectedFourActions);

    DarCollectionSummary testFive = summaries.get(4);
    assertEquals(DarCollectionStatus.IN_PROCESS.getValue(), testFive.getStatus());
    assertEquals(Set.of(DarCollectionActions.REVIEW.getValue()), testFive.getActions());

    DarCollectionSummary testDraft = summaries.get(5);
    Set<String> expectedDraftActions =
        Set.of(DarCollectionActions.RESUME.getValue(), DarCollectionActions.DELETE.getValue());
    assertEquals(DarCollectionStatus.DRAFT.getValue(), testDraft.getStatus());
    assertEquals(expectedDraftActions, testDraft.getActions());
  }

  @Test
  void testProcessDarCollectionSummariesForResearcherWithCloseout() {
    User user = new User();
    user.setUserId(1);
    DarCollectionSummary summary = new DarCollectionSummary();
    summary.setLatestReferenceId(UUID.randomUUID().toString());
    summary.setCloseoutSupplement(new CloseoutSupplement(List.of("Closeout"), "Closeout", 1));
    when(darCollectionSummaryDAO.getDarCollectionSummariesForResearcher(user.getUserId()))
        .thenReturn(List.of(summary));
    when(dataAccessRequestDAO.findDatasetApprovalsByDar(summary.getLatestReferenceId()))
        .thenReturn(Set.of(1));

    List<DarCollectionSummary> summaries = service.getSummariesForRole(user, UserRoles.RESEARCHER);

    assertNotNull(summaries);
    assertEquals(1, summaries.size());
    // All summaries should have the REVIEW action
    assertTrue(summaries.getFirst().getActions().contains(DarCollectionActions.REVIEW.getValue()));
    // Summaries with closeout should not have the CREATE_PROGRESS_REPORT action
    assertFalse(
        summaries
            .getFirst()
            .getActions()
            .contains(DarCollectionActions.CREATE_PROGRESS_REPORT.getValue()));
  }

  @Test
  void testProcessDarCollectionSummariesForResearcherWithoutCloseout() {
    User user = new User();
    user.setUserId(1);
    DarCollectionSummary summary = new DarCollectionSummary();
    summary.setLatestReferenceId(UUID.randomUUID().toString());
    when(darCollectionSummaryDAO.getDarCollectionSummariesForResearcher(user.getUserId()))
        .thenReturn(List.of(summary));
    when(dataAccessRequestDAO.findDatasetApprovalsByDar(summary.getLatestReferenceId()))
        .thenReturn(Set.of(1));

    List<DarCollectionSummary> summaries = service.getSummariesForRole(user, UserRoles.RESEARCHER);

    assertNotNull(summaries);
    assertEquals(1, summaries.size());
    // All summaries should have the REVIEW action
    assertTrue(summaries.getFirst().getActions().contains(DarCollectionActions.REVIEW.getValue()));
    // Summaries without a closeout should have the CREATE_PROGRESS_REPORT action
    assertTrue(
        summaries
            .getFirst()
            .getActions()
            .contains(DarCollectionActions.CREATE_PROGRESS_REPORT.getValue()));
  }

  @Test
  void testProcessDarCollectionSummariesForSOWithPendingCloseout() {
    User user = new User();
    user.setUserId(1);
    user.addRole(UserRoles.SigningOfficial());
    user.setInstitutionId(1);

    DarCollectionSummary summary = new DarCollectionSummary();
    summary.setLatestReferenceId(UUID.randomUUID().toString());

    CloseoutSupplement closeoutSupplement =
        new CloseoutSupplement(List.of("Closeout"), "Closeout", 1);
    summary.setCloseoutSupplement(closeoutSupplement);

    summary.setCloseoutSigningOfficialApprovalDate(null);

    when(darCollectionSummaryDAO.getDarCollectionSummariesForSO(user.getInstitutionId()))
        .thenReturn(List.of(summary));

    List<DarCollectionSummary> summaries =
        service.getSummariesForRole(user, UserRoles.SIGNINGOFFICIAL);

    assertNotNull(summaries);
    assertEquals(1, summaries.size());
    Set<String> expectedActions = Set.of(DarCollectionActions.REVIEW_PROGRESS_REPORT.getValue());
    assertEquals(expectedActions, summaries.getFirst().getActions());
  }

  @Test
  void testProcessDarCollectionSummariesForSOWithApprovedCloseout() {
    User user = new User();
    user.setUserId(1);
    user.addRole(UserRoles.SigningOfficial());
    user.setInstitutionId(1);

    DarCollectionSummary summary = new DarCollectionSummary();
    summary.setLatestReferenceId(UUID.randomUUID().toString());

    CloseoutSupplement closeoutSupplement =
        new CloseoutSupplement(List.of("Closeout"), "Closeout", 1);
    summary.setCloseoutSupplement(closeoutSupplement);

    summary.setCloseoutSigningOfficialApprovalDate(new Timestamp(System.currentTimeMillis()));

    when(darCollectionSummaryDAO.getDarCollectionSummariesForSO(user.getInstitutionId()))
        .thenReturn(List.of(summary));

    List<DarCollectionSummary> summaries =
        service.getSummariesForRole(user, UserRoles.SIGNINGOFFICIAL);

    assertNotNull(summaries);
    assertEquals(1, summaries.size());
    assertEquals(Set.of(), summaries.getFirst().getActions());
  }

  @Test
  void testProcessDarCollectionSummariesForChairWithApprovedCloseout() {
    User user = new User();
    user.setUserId(1);
    user.addRole(UserRoles.Chairperson());

    DarCollectionSummary summary = new DarCollectionSummary();
    summary.setLatestReferenceId(UUID.randomUUID().toString());

    CloseoutSupplement closeoutSupplement =
        new CloseoutSupplement(List.of("Closeout"), "Closeout", 1);
    summary.setCloseoutSupplement(closeoutSupplement);

    summary.setCloseoutSigningOfficialApprovalDate(new Timestamp(System.currentTimeMillis()));

    when(darCollectionSummaryDAO.getDarCollectionSummariesForDACRole(
            user.getUserId(), UserRoles.CHAIRPERSON.getRoleId()))
        .thenReturn(List.of(summary));

    List<DarCollectionSummary> summaries = service.getSummariesForRole(user, UserRoles.CHAIRPERSON);

    assertNotNull(summaries);
    assertEquals(1, summaries.size());
    Set<String> expectedActions = Set.of(DarCollectionActions.REVIEW_PROGRESS_REPORT.getValue());
    assertEquals(expectedActions, summaries.getFirst().getActions());
  }

  @Test
  void testProcessDarCollectionSummariesForAdmin() {
    // summaryOne -> all elections present and open
    // summaryTwo -> mix of open elections : absent/non-open elections (in process)
    // summaryThree -> all canceled elections (Complete)
    // summaryFour -> no elections (unreviewed)

    User user = new User();
    user.setUserId(1);

    DarCollectionSummary summaryOne = new DarCollectionSummary();
    Dataset datasetOne = new Dataset();
    datasetOne.setDatasetId(1);
    Dataset datasetTwo = new Dataset();
    datasetTwo.setDatasetId(2);
    Election electionOne = new Election();
    electionOne.setElectionId(1);
    electionOne.setStatus(ElectionStatus.OPEN.getValue());
    Election electionTwo = new Election();
    electionTwo.setElectionId(2);
    electionTwo.setStatus(ElectionStatus.OPEN.getValue());
    summaryOne.addElection(electionOne);
    summaryOne.addElection(electionTwo);
    summaryOne.addDatasetId(datasetOne.getDatasetId());
    summaryOne.addDatasetId(datasetTwo.getDatasetId());

    DarCollectionSummary summaryTwo = new DarCollectionSummary();
    Dataset datasetThree = new Dataset();
    datasetThree.setDatasetId(3);
    Dataset datasetFour = new Dataset();
    datasetFour.setDatasetId(4);
    Dataset datasetFive = new Dataset();
    datasetFive.setDatasetId(5);
    Election electionThree = new Election();
    electionThree.setElectionId(3);
    electionThree.setStatus(ElectionStatus.OPEN.getValue());
    Election electionFour = new Election();
    electionFour.setElectionId(4);
    electionFour.setStatus(ElectionStatus.CANCELED.getValue());
    summaryTwo.addElection(electionThree);
    summaryTwo.addElection(electionFour);
    summaryTwo.addDatasetId(datasetThree.getDatasetId());
    summaryTwo.addDatasetId(datasetFour.getDatasetId());
    summaryTwo.addDatasetId(datasetFive.getDatasetId());

    DarCollectionSummary summaryThree = new DarCollectionSummary();
    Dataset datasetSix = new Dataset();
    datasetSix.setDatasetId(6);
    Election electionFive = new Election();
    electionFive.setElectionId(5);
    electionFive.setStatus(ElectionStatus.CANCELED.getValue());
    summaryThree.addElection(electionFive);
    summaryThree.addDatasetId(datasetSix.getDatasetId());

    DarCollectionSummary summaryFour = new DarCollectionSummary();
    Dataset datasetSeven = new Dataset();
    datasetSeven.setDatasetId(7);
    summaryFour.addDatasetId(datasetSeven.getDatasetId());

    when(darCollectionSummaryDAO.getDarCollectionSummariesForAdmin())
        .thenReturn(List.of(summaryOne, summaryTwo, summaryThree, summaryFour));

    List<DarCollectionSummary> summaries = service.getSummariesForRole(user, UserRoles.ADMIN);

    DarCollectionSummary testOne = summaries.getFirst();
    Set<String> expectedOneActions = Set.of(DarCollectionActions.CANCEL.getValue());
    assertTrue(testOne.getStatus().equalsIgnoreCase(DarCollectionStatus.IN_PROCESS.getValue()));
    assertEquals(expectedOneActions, testOne.getActions());

    DarCollectionSummary testTwo = summaries.get(1);
    Set<String> expectedTwoActions = Set.of(DarCollectionActions.CANCEL.getValue());
    assertTrue(testTwo.getStatus().equalsIgnoreCase(DarCollectionStatus.IN_PROCESS.getValue()));
    assertEquals(expectedTwoActions, testTwo.getActions());

    DarCollectionSummary testThree = summaries.get(2);
    Set<String> expectedThreeActions = Set.of();
    assertTrue(testThree.getStatus().equalsIgnoreCase(DarCollectionStatus.COMPLETE.getValue()));
    assertEquals(expectedThreeActions, testThree.getActions());

    DarCollectionSummary testFour = summaries.get(3);
    Set<String> expectedFourActions = Set.of();
    assertTrue(testFour.getStatus().equalsIgnoreCase(DarCollectionStatus.SUBMITTED.getValue()));
    assertEquals(expectedFourActions, testFour.getActions());
  }

  @Test
  void testProcessDarCollectionSummariesForDACMemberNoDatasets() {
    Dac dac = new Dac();
    dac.setDacId(randomInt(1, 10));
    User user = new User();
    user.setUserId(randomInt(1, 10));
    user.setMemberRole();
    List<DarCollectionSummary> summaries = service.getSummariesForRole(user, UserRoles.MEMBER);
    assertTrue(summaries.isEmpty());
  }

  @Test
  void testProcessDarCollectionSummariesForDACChairNoDatasets() {
    Dac dac = new Dac();
    dac.setDacId(randomInt(1, 10));
    User user = new User();
    user.setUserId(randomInt(1, 10));
    user.setChairpersonRole();
    List<DarCollectionSummary> summaries = service.getSummariesForRole(user, UserRoles.CHAIRPERSON);
    assertTrue(summaries.isEmpty());
  }

  @Test
  void testProcessDarCollectionSummariesForDACMember() {
    Dac dac = new Dac();
    dac.setDacId(1);
    User user = new User();
    user.setUserId(1);
    user.setMemberRoleWithDAC(dac.getDacId());

    // summaryOne -> no open elections (no action)
    // summaryTwo -> at least one open election, member has submitted all votes (Update button)
    // summaryThree -> unreviewed scenario (no elections),
    // summaryFour -> at least one open election, member has not submitted all votes (Vote button)

    DarCollectionSummary summary = new DarCollectionSummary();
    summary.addDatasetId(1);
    summary.addDatasetId(2);
    Election election = new Election();
    election.setStatus(ElectionStatus.CLOSED.getValue());
    election.setElectionId(1);
    Election electionTwo = new Election();
    electionTwo.setStatus(ElectionStatus.CANCELED.getValue());
    electionTwo.setElectionId(2);
    summary.addElection(election);
    summary.addElection(electionTwo);

    DarCollectionSummary summaryTwo = new DarCollectionSummary();
    summaryTwo.addDatasetId(3);
    Election electionThree = new Election();
    Vote vote =
        new Vote(
            1,
            true,
            user.getUserId(),
            null,
            null,
            electionThree.getElectionId(),
            null,
            VoteType.DAC.getValue(),
            null,
            null);
    electionThree.setElectionId(3);
    electionThree.setStatus(ElectionStatus.OPEN.getValue());
    summaryTwo.addElection(electionThree);
    summaryTwo.addVote(vote);

    DarCollectionSummary summaryThree = new DarCollectionSummary();
    summaryThree.addDatasetId(4);

    DarCollectionSummary summaryFour = new DarCollectionSummary();
    summaryFour.addDatasetId(5);
    Election electionFour = new Election();
    Vote voteTwo =
        new Vote(
            2,
            true,
            user.getUserId(),
            null,
            null,
            electionThree.getElectionId(),
            null,
            VoteType.DAC.getValue(),
            null,
            null);
    Vote voteThree =
        new Vote(
            4,
            null,
            user.getUserId(),
            null,
            null,
            electionThree.getElectionId(),
            null,
            VoteType.DAC.getValue(),
            null,
            null);
    electionFour.setElectionId(4);
    electionFour.setStatus(ElectionStatus.OPEN.getValue());
    summaryFour.addElection(electionFour);
    summaryFour.setVotes(List.of(voteTwo, voteThree));

    when(darCollectionSummaryDAO.getDarCollectionSummariesForDACRole(any(), any()))
        .thenReturn(List.of(summary, summaryTwo, summaryThree, summaryFour));

    List<DarCollectionSummary> summaries = service.getSummariesForRole(user, UserRoles.MEMBER);

    assertNotNull(summaries);
    assertEquals(4, summaries.size());

    DarCollectionSummary testOne = summaries.getFirst();
    Set<String> expectedOneActions = Set.of();
    assertEquals(testOne.getActions(), expectedOneActions);
    assertEquals(DarCollectionStatus.COMPLETE.getValue(), testOne.getStatus());

    DarCollectionSummary testTwo = summaries.get(1);
    Set<String> expectedTwoActions = Set.of(DarCollectionActions.UPDATE.getValue());
    assertEquals(testTwo.getActions(), expectedTwoActions);
    assertEquals(DarCollectionStatus.IN_PROCESS.getValue(), testTwo.getStatus());

    DarCollectionSummary testThree = summaries.get(2);
    Set<String> expectedThreeActions = Set.of();
    assertEquals(testThree.getActions(), expectedThreeActions);
    assertEquals(DarCollectionStatus.SUBMITTED.getValue(), testThree.getStatus());

    DarCollectionSummary testFour = summaries.get(3);
    Set<String> expectedFourActions = Set.of(DarCollectionActions.VOTE.getValue());
    assertEquals(testFour.getActions(), expectedFourActions);
    assertEquals(DarCollectionStatus.IN_PROCESS.getValue(), testFour.getStatus());
  }

  @Test
  void testProcessDarCollectionSummariesForChair() {
    // summaryOne -> all elections present and open
    // summaryTwo -> mix of open elections : absent/canceled elections (in process)
    // summaryThree -> all canceled elections (Complete)
    // summaryFour -> no elections (unreviewed)
    // summaryFive -> mix of open : absent/closed elections (in process, but cancel action does not
    // appear)
    // summarySix -> all closed elections (complete, only open available)

    Dac dac = new Dac();
    dac.setDacId(1);
    User user = new User();
    user.setUserId(1);
    user.setChairpersonRoleWithDAC(dac.getDacId());

    DarCollectionSummary summaryOne = new DarCollectionSummary();
    Dataset datasetOne = new Dataset();
    datasetOne.setDatasetId(1);
    Dataset datasetTwo = new Dataset();
    datasetTwo.setDatasetId(2);
    Election electionOne = new Election();
    electionOne.setElectionId(1);
    electionOne.setStatus(ElectionStatus.OPEN.getValue());
    Election electionTwo = new Election();
    electionTwo.setElectionId(2);
    electionTwo.setStatus(ElectionStatus.OPEN.getValue());
    summaryOne.addElection(electionOne);
    summaryOne.addElection(electionTwo);
    summaryOne.addDatasetId(datasetOne.getDatasetId());
    summaryOne.addDatasetId(datasetTwo.getDatasetId());

    DarCollectionSummary summaryTwo = new DarCollectionSummary();
    Dataset datasetThree = new Dataset();
    datasetThree.setDatasetId(3);
    Dataset datasetFour = new Dataset();
    datasetFour.setDatasetId(4);
    Dataset datasetFive = new Dataset();
    datasetFive.setDatasetId(5);
    Election electionThree = new Election();
    electionThree.setElectionId(3);
    electionThree.setStatus(ElectionStatus.OPEN.getValue());
    Election electionFour = new Election();
    electionFour.setElectionId(4);
    electionFour.setStatus(ElectionStatus.CANCELED.getValue());
    summaryTwo.addElection(electionThree);
    summaryTwo.addElection(electionFour);
    summaryTwo.addDatasetId(datasetThree.getDatasetId());
    summaryTwo.addDatasetId(datasetFour.getDatasetId());
    summaryTwo.addDatasetId(datasetFive.getDatasetId());

    DarCollectionSummary summaryThree = new DarCollectionSummary();
    Dataset datasetSix = new Dataset();
    datasetSix.setDatasetId(6);
    Election electionFive = new Election();
    electionFive.setElectionId(5);
    electionFive.setStatus(ElectionStatus.CANCELED.getValue());
    summaryThree.addElection(electionFive);
    summaryThree.addDatasetId(datasetSix.getDatasetId());

    DarCollectionSummary summaryFour = new DarCollectionSummary();
    Dataset datasetSeven = new Dataset();
    datasetSeven.setDatasetId(7);
    summaryFour.addDatasetId(datasetSeven.getDatasetId());

    DarCollectionSummary summaryFive = new DarCollectionSummary();
    Election electionSix = new Election();
    electionSix.setElectionId(6);
    electionSix.setStatus(ElectionStatus.OPEN.getValue());
    Election electionSeven = new Election();
    electionSeven.setElectionId(7);
    electionSeven.setStatus(ElectionStatus.CLOSED.getValue());
    summaryFive.addElection(electionSix);
    summaryFive.addElection(electionSeven);
    summaryFive.addDatasetId(7);
    summaryFive.addDatasetId(8);
    summaryFive.addDatasetId(9);

    DarCollectionSummary summarySix = new DarCollectionSummary();
    Election electionEight = new Election();
    electionEight.setElectionId(8);
    electionEight.setStatus(ElectionStatus.CLOSED.getValue());
    Election electionNine = new Election();
    electionNine.setElectionId(9);
    electionNine.setStatus(ElectionStatus.CLOSED.getValue());
    summarySix.addElection(electionEight);
    summarySix.addElection(electionNine);
    summarySix.addDatasetId(10);
    summarySix.addDatasetId(11);

    when(darCollectionSummaryDAO.getDarCollectionSummariesForDACRole(any(), any()))
        .thenReturn(
            List.of(summaryOne, summaryTwo, summaryThree, summaryFour, summaryFive, summarySix));

    List<DarCollectionSummary> summaries = service.getSummariesForRole(user, UserRoles.CHAIRPERSON);
    assertEquals(6, summaries.size());

    DarCollectionSummary testOne = summaries.getFirst();
    Set<String> expectedOneActions =
        Set.of(DarCollectionActions.VOTE.getValue(), DarCollectionActions.CANCEL.getValue());
    assertTrue(testOne.getStatus().equalsIgnoreCase(DarCollectionStatus.IN_PROCESS.getValue()));
    assertEquals(testOne.getActions(), expectedOneActions);

    DarCollectionSummary testTwo = summaries.get(1);
    Set<String> expectedTwoActions =
        Set.of(
            DarCollectionActions.VOTE.getValue(),
            DarCollectionActions.CANCEL.getValue(),
            DarCollectionActions.OPEN.getValue());
    assertTrue(testTwo.getStatus().equalsIgnoreCase(DarCollectionStatus.IN_PROCESS.getValue()));
    assertEquals(testTwo.getActions(), expectedTwoActions);

    DarCollectionSummary testThree = summaries.get(2);
    Set<String> expectedThreeActions = Set.of(DarCollectionActions.OPEN.getValue());
    assertTrue(testThree.getStatus().equalsIgnoreCase(DarCollectionStatus.COMPLETE.getValue()));
    assertEquals(testThree.getActions(), expectedThreeActions);

    DarCollectionSummary testFour = summaries.get(3);
    Set<String> expectedFourActions = Set.of(DarCollectionActions.OPEN.getValue());
    assertTrue(testFour.getStatus().equalsIgnoreCase(DarCollectionStatus.SUBMITTED.getValue()));
    assertEquals(testFour.getActions(), expectedFourActions);

    DarCollectionSummary testFive = summaries.get(4);
    Set<String> expectedFiveActions =
        Set.of(DarCollectionActions.OPEN.getValue(), DarCollectionActions.VOTE.getValue());
    assertTrue(testFive.getStatus().equalsIgnoreCase(DarCollectionStatus.IN_PROCESS.getValue()));
    assertEquals(testFive.getActions(), expectedFiveActions);

    DarCollectionSummary testSix = summaries.get(5);
    Set<String> expectedSixActions = Set.of(DarCollectionActions.OPEN.getValue());
    assertTrue(testSix.getStatus().equalsIgnoreCase(DarCollectionStatus.COMPLETE.getValue()));
    assertEquals(testSix.getActions(), expectedSixActions);
  }

  @Test
  void testProcessDarCollectionSummariesForChairWithCloseout() {
    User user = new User();
    user.setUserId(1);
    user.addRole(UserRoles.Chairperson());
    DarCollectionSummary summary = new DarCollectionSummary();
    summary.setLatestReferenceId(UUID.randomUUID().toString());
    summary.setCloseoutSupplement(new CloseoutSupplement(List.of("Closeout"), "Closeout", 1));
    when(darCollectionSummaryDAO.getDarCollectionSummariesForDACRole(
            user.getUserId(), UserRoles.CHAIRPERSON.getRoleId()))
        .thenReturn(List.of(summary));

    List<DarCollectionSummary> summaries = service.getSummariesForRole(user, UserRoles.CHAIRPERSON);

    assertNotNull(summaries);
    assertEquals(1, summaries.size());
    // Chair summary should not have any actions
    assertTrue(summaries.getFirst().getActions().isEmpty());
  }

  @Test
  void testProcessDarCollectionSummariesForChairWithoutCloseout() {
    User user = new User();
    user.setUserId(1);
    user.addRole(UserRoles.Chairperson());
    DarCollectionSummary summary = new DarCollectionSummary();
    summary.setLatestReferenceId(UUID.randomUUID().toString());
    when(darCollectionSummaryDAO.getDarCollectionSummariesForDACRole(
            user.getUserId(), UserRoles.CHAIRPERSON.getRoleId()))
        .thenReturn(List.of(summary));

    List<DarCollectionSummary> summaries = service.getSummariesForRole(user, UserRoles.CHAIRPERSON);

    assertNotNull(summaries);
    assertEquals(1, summaries.size());
    // With no elections, there should be an Open action
    assertTrue(summaries.getFirst().getActions().contains(DarCollectionActions.OPEN.getValue()));
  }

  @Test
  void testProcessDarCollectionSummariesForChairWithSOApprovalRequired() {
    User user = new User();
    user.setUserId(1);
    user.addRole(UserRoles.Chairperson());
    DarCollectionSummary summary = new DarCollectionSummary();
    summary.setLatestReferenceId(UUID.randomUUID().toString());
    summary.setRequiresSOApproval(true);
    when(darCollectionSummaryDAO.getDarCollectionSummariesForDACRole(
        user.getUserId(), UserRoles.CHAIRPERSON.getRoleId()))
        .thenReturn(List.of(summary));

    List<DarCollectionSummary> summaries = service.getSummariesForRole(user, UserRoles.CHAIRPERSON);

    assertNotNull(summaries);
    assertEquals(1, summaries.size());
    // Chair summary should not have any actions
    assertTrue(summaries.getFirst().getActions().isEmpty());
  }

  @Test
  void testGetSummaryForRoleByCollectionId_SO() {
    User user = new User();
    user.setUserId(1);

    DarCollectionSummary summary = createDarCollectionSummaryWithElections();
    Integer collectionId = summary.getDarCollectionId();

    when(darCollectionSummaryDAO.getDarCollectionSummaryByCollectionId(collectionId))
        .thenReturn(summary);

    DarCollectionSummary summaryResult =
        service.getSummaryForRoleByCollectionId(user, UserRoles.SIGNINGOFFICIAL, collectionId);
    assertNotNull(summaryResult);

    assertTrue(
        summaryResult.getStatus().equalsIgnoreCase(DarCollectionStatus.IN_PROCESS.getValue()));
    assertEquals(Set.of(), summaryResult.getActions());
  }

  @Test
  void testGetSummaryForRoleByCollectionId_Researcher() {
    User user = new User();
    user.setUserId(1);

    DarCollectionSummary summary = createDarCollectionSummaryWithElections();
    summary.setLatestReferenceId("ref1");
    Integer collectionId = summary.getDarCollectionId();

    when(darCollectionSummaryDAO.getDarCollectionSummaryByCollectionId(collectionId))
        .thenReturn(summary);
    when(dataAccessRequestDAO.findDatasetApprovalsByDar("ref1")).thenReturn(Set.of());

    DarCollectionSummary summaryResult =
        service.getSummaryForRoleByCollectionId(user, UserRoles.RESEARCHER, collectionId);
    assertNotNull(summaryResult);

    Set<String> expectedActions = Set.of(DarCollectionActions.REVIEW.getValue());
    assertTrue(
        summaryResult.getStatus().equalsIgnoreCase(DarCollectionStatus.IN_PROCESS.getValue()));
    assertEquals(expectedActions, summaryResult.getActions());
  }

  @Test
  void testGetSummaryForRoleByCollectionId_Admin() {
    User user = new User();
    user.setUserId(1);

    DarCollectionSummary summary = createDarCollectionSummaryWithElections();
    Integer collectionId = summary.getDarCollectionId();

    when(darCollectionSummaryDAO.getDarCollectionSummaryByCollectionId(collectionId))
        .thenReturn(summary);

    DarCollectionSummary summaryResult =
        service.getSummaryForRoleByCollectionId(user, UserRoles.ADMIN, collectionId);
    assertNotNull(summaryResult);

    Set<String> expectedActions = Set.of(DarCollectionActions.CANCEL.getValue());
    assertTrue(
        summaryResult.getStatus().equalsIgnoreCase(DarCollectionStatus.IN_PROCESS.getValue()));
    assertEquals(expectedActions, summaryResult.getActions());
  }

  @Test
  void testGetSummaryForRoleNameByCollectionIdForResearcher_PRWithClosedElections() {
    User user = new User();
    user.setUserId(1);

    DarCollectionSummary summary = createDarCollectionSummaryWithElections();
    summary
        .getElections()
        .values()
        .forEach(election -> election.setStatus(ElectionStatus.CLOSED.getValue()));
    summary.setLatestReferenceId("ref1");
    Integer collectionId = summary.getDarCollectionId();

    when(darCollectionSummaryDAO.getDarCollectionSummaryByCollectionId(collectionId))
        .thenReturn(summary);

    when(dataAccessRequestDAO.findDatasetApprovalsByDar("ref1")).thenReturn(Set.of(1));

    DarCollectionSummary summaryResult =
        service.getSummaryForRoleByCollectionId(user, UserRoles.RESEARCHER, collectionId);

    assertNotNull(summaryResult);

    assertTrue(summaryResult.getStatus().equalsIgnoreCase(DarCollectionStatus.COMPLETE.getValue()));

    // Verify that the create_progress_report action is included
    Set<String> expectedActions =
        Set.of(
            DarCollectionActions.REVIEW.getValue(),
            DarCollectionActions.CREATE_PROGRESS_REPORT.getValue());
    assertEquals(expectedActions, summaryResult.getActions());
  }

  @Test
  void testGetSummaryForRoleNameByCollectionIdForResearcher_PRWithOpenElections() {
    User user = new User();
    user.setUserId(1);

    DarCollectionSummary summary = createDarCollectionSummaryWithElections();
    summary
        .getElections()
        .values()
        .forEach(election -> election.setStatus(ElectionStatus.OPEN.getValue()));
    summary.setLatestReferenceId("ref1");
    Integer collectionId = summary.getDarCollectionId();

    when(darCollectionSummaryDAO.getDarCollectionSummaryByCollectionId(collectionId))
        .thenReturn(summary);

    when(dataAccessRequestDAO.findDatasetApprovalsByDar("ref1")).thenReturn(Set.of(1));

    DarCollectionSummary summaryResult =
        service.getSummaryForRoleByCollectionId(user, UserRoles.RESEARCHER, collectionId);

    assertNotNull(summaryResult);

    assertTrue(
        summaryResult.getStatus().equalsIgnoreCase(DarCollectionStatus.IN_PROCESS.getValue()));

    // Verify that the create_progress_report action is NOT included
    Set<String> expectedActions = Set.of(DarCollectionActions.REVIEW.getValue());
    assertEquals(expectedActions, summaryResult.getActions());
  }

  @Test
  void testGetSummaryForRoleByCollectionId_Chair() {
    Dac dac = new Dac();
    dac.setDacId(1);
    User user = new User();
    user.setUserId(1);
    user.setChairpersonRoleWithDAC(dac.getDacId());

    DarCollectionSummary summary = createDarCollectionSummaryWithElections();
    Integer collectionId = summary.getDarCollectionId();

    when(darCollectionSummaryDAO.getDarCollectionSummaryForDACByCollectionId(
            user.getUserId(), List.of(), collectionId))
        .thenReturn(summary);
    when(datasetDAO.findDatasetIdsByDacIds(any())).thenReturn(List.of());

    DarCollectionSummary summaryResult =
        service.getSummaryForRoleByCollectionId(user, UserRoles.CHAIRPERSON, collectionId);
    assertNotNull(summaryResult);

    Set<String> expectedActions =
        Set.of(
            DarCollectionActions.VOTE.getValue(),
            DarCollectionActions.CANCEL.getValue(),
            DarCollectionActions.OPEN.getValue());
    assertTrue(
        summaryResult.getStatus().equalsIgnoreCase(DarCollectionStatus.IN_PROCESS.getValue()));
    assertEquals(expectedActions, summaryResult.getActions());
  }

  @Test
  void testGetSummaryForRoleByCollectionId_DACMember() {
    Dac dac = new Dac();
    dac.setDacId(1);
    User user = new User();
    user.setUserId(1);
    user.setMemberRole();

    DarCollectionSummary summary = new DarCollectionSummary();
    Integer collectionId = randomInt(1, 100);
    summary.setDarCollectionId(collectionId);
    Dataset datasetOne = new Dataset();
    datasetOne.setDatasetId(1);
    Dataset datasetTwo = new Dataset();
    datasetTwo.setDatasetId(2);
    Election electionOne = new Election();
    electionOne.setElectionId(1);
    electionOne.setStatus(ElectionStatus.OPEN.getValue());
    Election electionTwo = new Election();
    electionTwo.setElectionId(2);
    electionTwo.setStatus(ElectionStatus.CLOSED.getValue());
    Vote vote =
        new Vote(
            1, null, user.getUserId(), null, null, 1, null, VoteType.DAC.getValue(), null, null);
    summary.addElection(electionOne);
    summary.addElection(electionTwo);
    summary.addDatasetId(datasetOne.getDatasetId());
    summary.addDatasetId(datasetTwo.getDatasetId());
    summary.setVotes(List.of(vote));

    when(darCollectionSummaryDAO.getDarCollectionSummaryForDACByCollectionId(
            user.getUserId(), List.of(), collectionId))
        .thenReturn(summary);

    DarCollectionSummary summaryResult =
        service.getSummaryForRoleByCollectionId(user, UserRoles.MEMBER, collectionId);
    assertNotNull(summaryResult);

    Set<String> expectedActions = Set.of(DarCollectionActions.VOTE.getValue());
    assertTrue(
        summaryResult.getStatus().equalsIgnoreCase(DarCollectionStatus.IN_PROCESS.getValue()));
    assertEquals(expectedActions, summaryResult.getActions());
  }

  @Test
  void testGetSummaryForRoleNameByCollectionId_NoSummaryFound() {
    User user = new User();
    user.setUserId(1);
    DarCollectionSummary summary = new DarCollectionSummary();
    Integer collectionId = randomInt(1, 100);
    summary.setDarCollectionId(collectionId);

    when(darCollectionSummaryDAO.getDarCollectionSummaryByCollectionId(collectionId))
        .thenReturn(null);

    assertThrows(
        NotFoundException.class,
        () -> service.getSummaryForRoleByCollectionId(user, UserRoles.RESEARCHER, collectionId));
  }

  private DarCollection mockDarCollectionWithElectionAndVotes(User user) {
    Integer collectionId = 1;
    DarCollection collection = new DarCollection();
    collection.setDarCollectionId(collectionId);
    DataAccessRequest dar = new DataAccessRequest();
    dar.setReferenceId(UUID.randomUUID().toString());
    Election election = new Election();
    election.setReferenceId(dar.getReferenceId());
    election.setElectionId(1);
    Vote vote = new Vote();
    vote.setVoteId(1);
    vote.setType(VoteType.FINAL.getValue());
    vote.setUserId(user.getUserId());
    vote.setElectionId(election.getElectionId());
    vote.setVote(true);
    election.setVotes(Map.of(vote.getVoteId(), vote));
    dar.addElection(election);
    collection.addDar(dar);
    return collection;
  }

  private DarCollectionSummary createDarCollectionSummaryWithElections() {
    DarCollectionSummary summary = new DarCollectionSummary();
    Integer collectionId = randomInt(1, 100);
    summary.setDarCollectionId(collectionId);
    Dataset datasetOne = new Dataset();
    datasetOne.setDatasetId(1);
    Dataset datasetTwo = new Dataset();
    datasetTwo.setDatasetId(2);
    Election electionOne = new Election();
    electionOne.setElectionId(1);
    electionOne.setStatus(ElectionStatus.OPEN.getValue());
    Election electionTwo = new Election();
    electionTwo.setElectionId(2);
    electionTwo.setStatus(ElectionStatus.CANCELED.getValue());
    summary.addElection(electionOne);
    summary.addElection(electionTwo);
    summary.addDatasetId(datasetOne.getDatasetId());
    summary.addDatasetId(datasetTwo.getDatasetId());
    return summary;
  }

  // DAR Collection not found
  @Test
  void testSendNewDARCollectionMessage_NoCollection() throws Exception {
    when(darCollectionDAO.findDARCollectionByCollectionId(anyInt())).thenReturn(null);
    service.createElectionsForNewDarCollection(999);
    service.sendNewDARCollectionMessage(999);
    verifyNoInteractions(emailService);
  }

  // DARs with no datasets
  @Test
  void testSendNewDARCollectionMessage_NoDatasets() throws Exception {
    DarCollection collection = new DarCollection();
    collection.setDarCollectionId(1);
    DataAccessRequest dar = new DataAccessRequest();
    dar.setReferenceId(UUID.randomUUID().toString());
    dar.setDatasetIds(List.of());
    collection.addDar(dar);
    when(darCollectionDAO.findDARCollectionByCollectionId(1)).thenReturn(collection);
    when(userDAO.findUserById(any())).thenReturn(new User());
    service.createElectionsForNewDarCollection(1);
    service.sendNewDARCollectionMessage(1);
    verifyNoInteractions(emailService);
  }

  // Only manual DACs, no automation rules
  @Test
  void testSendNewDARCollectionMessage_ManualDACsOnly() throws Exception {
    DarCollection collection = new DarCollection();
    collection.setDarCollectionId(1);
    DataAccessRequest dar = new DataAccessRequest();
    dar.setReferenceId(UUID.randomUUID().toString());
    Dataset dataset = new Dataset();
    dataset.setDatasetId(1);
    dataset.setDacId(1);
    dar.setDatasetIds(List.of(dataset.getDatasetId()));
    collection.addDar(dar);
    Dac dac = new Dac();
    dac.setDacId(1);
    dac.setName("DAC-1");
    User chair = new User();
    chair.setUserId(2);
    UserRole chairRole =
        new UserRole(UserRoles.CHAIRPERSON.getRoleId(), UserRoles.CHAIRPERSON.getRoleName());
    chairRole.setDacId(dac.getDacId());
    chair.setRoles(List.of(chairRole));
    when(darCollectionDAO.findDARCollectionByCollectionId(1)).thenReturn(collection);
    when(userDAO.findUserById(any())).thenReturn(new User());
    when(datasetDAO.findDatasetsByIdList(anyList())).thenReturn(List.of(dataset));
    when(dacDAO.findDacsForDatasetIds(anyList())).thenReturn(Set.of(dac));
    when(dacAutomationRuleService.findAllByDacId(anyInt())).thenReturn(List.of());
    when(userDAO.findUsersByRoleId(UserRoles.ADMIN.getRoleId())).thenReturn(List.of());
    when(userDAO.findUsersForDatasetsByRole(anyList(), anyList())).thenReturn(Set.of(chair));
    service.createElectionsForNewDarCollection(1);
    service.sendNewDARCollectionMessage(1);
    verify(emailService).sendNewDARRequestEmail(any(), any(), any(), any());
    verify(emailService, never()).sendDarNewCollectionElectionMessage(any(), any());
  }

  // Only auto-open DACs
  @Test
  void testSendNewDARCollectionMessage_AutoOpenDACsOnly() throws Exception {
    DarCollection collection = new DarCollection();
    collection.setDarCollectionId(1);

    DataAccessRequest dar = new DataAccessRequest();
    dar.setReferenceId(UUID.randomUUID().toString());

    Dataset dataset = new Dataset();
    dataset.setDatasetId(1);
    dataset.setDacId(1);

    dar.setDatasetIds(List.of(dataset.getDatasetId()));
    collection.addDar(dar);

    Dac dac = new Dac();
    dac.setDacId(1);
    dac.setName("DAC-1");

    User member = new User();
    member.setUserId(3);
    member.setInstitutionId(1);

    UserRole memberRole =
        new UserRole(UserRoles.MEMBER.getRoleId(), UserRoles.MEMBER.getRoleName());
    memberRole.setDacId(dac.getDacId());
    member.setRoles(List.of(memberRole));

    DACAutomationRule rule = mock(DACAutomationRule.class);

    when(rule.ruleType()).thenReturn(DACAutomationRuleType.AUTO_OPEN_DAR_FOR_ALL_MEMBERS);
    when(rule.enabledByUserId()).thenReturn(member.getUserId());
    when(darCollectionDAO.findDARCollectionByCollectionId(1)).thenReturn(collection);
    when(datasetDAO.findDatasetsByIdList(anyList())).thenReturn(List.of(dataset));
    when(dacDAO.findDacsForDatasetIds(anyList())).thenReturn(Set.of(dac));
    when(dacAutomationRuleService.findAllByDacId(anyInt())).thenReturn(List.of(rule));
    when(userDAO.findUsersByRoleId(UserRoles.ADMIN.getRoleId())).thenReturn(List.of());
    when(userDAO.findUsersForDatasetsByRole(anyList(), anyList())).thenReturn(Set.of(member));
    when(userDAO.findUserById(any())).thenReturn(new User());

    service.createElectionsForNewDarCollection(1);
    service.sendNewDARCollectionMessage(1);

    verify(emailService).sendDarNewCollectionElectionMessage(any(), any());
    verify(emailService, never()).sendNewDARRequestEmail(any(), any(), any(), any());
  }

  @Test
  void testSendNewDARCollectionMessage_RequiresSOApproval() throws Exception {
    DarCollection collection = new DarCollection();
    collection.setDarCollectionId(1);

    User signingOfficial = new User();
    signingOfficial.setUserId(3);
    signingOfficial.setInstitutionId(1);
    signingOfficial.setEmail("so@example.org");

    DataAccessRequest dar = new DataAccessRequest();
    dar.setReferenceId(UUID.randomUUID().toString());
    DataAccessRequestData darData = new DataAccessRequestData();
    darData.setSigningOfficialEmail(signingOfficial.getEmail());
    dar.setData(darData);

    Dataset dataset = new Dataset();
    dataset.setDatasetId(1);
    dataset.setDacId(1);

    dar.setDatasetIds(List.of(dataset.getDatasetId()));
    collection.addDar(dar);

    Dac dac = new Dac();
    dac.setDacId(1);
    dac.setName("DAC-1");

    User member = new User();
    member.setUserId(3);
    member.setInstitutionId(1);

    UserRole memberRole =
        new UserRole(UserRoles.MEMBER.getRoleId(), UserRoles.MEMBER.getRoleName());
    memberRole.setDacId(dac.getDacId());
    member.setRoles(List.of(memberRole));

    DACAutomationRule rule = mock(DACAutomationRule.class);

    when(rule.ruleType()).thenReturn(DACAutomationRuleType.REQUIRE_SO_DAR_APPROVAL);
    when(rule.enabledByUserId()).thenReturn(member.getUserId());
    when(darCollectionDAO.findDARCollectionByCollectionId(1)).thenReturn(collection);
    when(datasetDAO.findDatasetsByIdList(anyList())).thenReturn(List.of(dataset));
    when(dacDAO.findDacsForDatasetIds(anyList())).thenReturn(Set.of(dac));
    when(dacAutomationRuleService.findAllByDacId(anyInt())).thenReturn(List.of(rule));
    when(userDAO.findUsersByRoleId(UserRoles.ADMIN.getRoleId())).thenReturn(List.of());
    when(userDAO.findUsersForDatasetsByRole(anyList(), anyList())).thenReturn(Set.of(member));
    when(userDAO.findUserById(any())).thenReturn(new User());

    service.createElectionsForNewDarCollection(1);
    service.sendNewDARCollectionMessage(1);

    verify(emailService, never()).sendDarNewCollectionElectionMessage(any(), any());
    verify(emailService, never()).sendNewDARRequestEmail(any(), any(), any(), any());
    verify(emailService).sendNewDARSigningOfficialRequestEmail(any(), any(), any());
  }

  @Test
  void testSendNewDARCollectionMessage_RequiresSOApprovalShouldDoDefaultBehaviorWithProgressReport()
      throws Exception {
    DarCollection collection = new DarCollection();
    collection.setDarCollectionId(1);

    User signingOfficial = new User();
    signingOfficial.setUserId(3);
    signingOfficial.setInstitutionId(1);
    signingOfficial.setEmail("so@example.org");

    DataAccessRequest dar = new DataAccessRequest();
    dar.setReferenceId(UUID.randomUUID().toString());
    dar.setParentId(2);
    dar.setSubmissionDate(Timestamp.from(Instant.now()));
    assertTrue(dar.getProgressReport());
    DataAccessRequestData darData = new DataAccessRequestData();
    darData.setSigningOfficialEmail(signingOfficial.getEmail());
    dar.setData(darData);

    Dataset dataset = new Dataset();
    dataset.setDatasetId(1);
    dataset.setDacId(1);

    dar.setDatasetIds(List.of(dataset.getDatasetId()));
    collection.addDar(dar);

    Dac dac = new Dac();
    dac.setDacId(1);
    dac.setName("DAC-1");

    User chairperson = new User();
    chairperson.setUserId(3);
    chairperson.setInstitutionId(1);

    UserRole chairRole =
        new UserRole(UserRoles.CHAIRPERSON.getRoleId(), UserRoles.CHAIRPERSON.getRoleName());
    chairRole.setDacId(dac.getDacId());
    chairperson.setRoles(List.of(chairRole));

    DACAutomationRule rule = mock(DACAutomationRule.class);

    when(rule.ruleType()).thenReturn(DACAutomationRuleType.REQUIRE_SO_DAR_APPROVAL);
    when(rule.enabledByUserId()).thenReturn(chairperson.getUserId());
    when(darCollectionDAO.findDARCollectionByCollectionId(1)).thenReturn(collection);
    when(datasetDAO.findDatasetsByIdList(anyList())).thenReturn(List.of(dataset));
    when(dacDAO.findDacsForDatasetIds(anyList())).thenReturn(Set.of(dac));
    when(dacAutomationRuleService.findAllByDacId(anyInt())).thenReturn(List.of(rule));
    when(userDAO.findUsersByRoleId(UserRoles.ADMIN.getRoleId())).thenReturn(List.of());
    when(userDAO.findUsersForDatasetsByRole(anyList(), anyList())).thenReturn(Set.of(chairperson));
    when(userDAO.findUserById(any())).thenReturn(new User());

    service.createElectionsForNewDarCollection(1);
    service.sendNewDARCollectionMessage(1);

    verify(emailService, never()).sendDarNewCollectionElectionMessage(any(), any());
    verify(emailService).sendNewProgressReportRequestEmail(any(), any(), any(), any(), any());
    verify(emailService, never()).sendNewDARSigningOfficialRequestEmail(any(), any(), any());
  }

  // Mixed auto-open and manual DACs
  @Test
  void testSendNewDARCollectionMessage_MixedAutoOpenAndManualDACs() throws Exception {
    DarCollection collection = new DarCollection();
    collection.setDarCollectionId(1);

    // Dataset 1: auto-open DAC
    Dataset autoOpenDataset = new Dataset();
    autoOpenDataset.setDatasetId(1);
    autoOpenDataset.setDacId(1);

    // Dataset 2: manual DAC
    Dataset manualDataset = new Dataset();
    manualDataset.setDatasetId(2);
    manualDataset.setDacId(2);

    DataAccessRequest dar = new DataAccessRequest();
    dar.setReferenceId(UUID.randomUUID().toString());
    dar.setDatasetIds(List.of(autoOpenDataset.getDatasetId(), manualDataset.getDatasetId()));
    collection.addDar(dar);

    Dac autoOpenDac = new Dac();
    autoOpenDac.setDacId(1);
    autoOpenDac.setName("AutoOpenDAC");

    Dac manualDac = new Dac();
    manualDac.setDacId(2);
    manualDac.setName("ManualDAC");

    User member = new User();
    member.setUserId(3);
    UserRole memberRole =
        new UserRole(UserRoles.MEMBER.getRoleId(), UserRoles.MEMBER.getRoleName());
    memberRole.setDacId(autoOpenDac.getDacId());
    member.setRoles(List.of(memberRole));

    User chair = new User();
    chair.setUserId(4);
    UserRole chairRole =
        new UserRole(UserRoles.CHAIRPERSON.getRoleId(), UserRoles.CHAIRPERSON.getRoleName());
    chairRole.setDacId(manualDac.getDacId());
    chair.setRoles(List.of(chairRole));

    DACAutomationRule rule = mock(DACAutomationRule.class);
    when(rule.ruleType()).thenReturn(DACAutomationRuleType.AUTO_OPEN_DAR_FOR_ALL_MEMBERS);
    when(rule.enabledByUserId()).thenReturn(member.getUserId());

    when(darCollectionDAO.findDARCollectionByCollectionId(1)).thenReturn(collection);
    when(datasetDAO.findDatasetsByIdList(anyList()))
        .thenReturn(List.of(autoOpenDataset, manualDataset));
    when(dacDAO.findDacsForDatasetIds(anyList())).thenReturn(Set.of(autoOpenDac, manualDac));
    when(dacAutomationRuleService.findAllByDacId(autoOpenDac.getDacId())).thenReturn(List.of(rule));
    when(dacAutomationRuleService.findAllByDacId(manualDac.getDacId())).thenReturn(List.of());
    when(userDAO.findUsersByRoleId(UserRoles.ADMIN.getRoleId())).thenReturn(List.of());
    when(userDAO.findUsersForDatasetsByRole(anyList(), anyList()))
        .thenReturn(Set.of(member, chair));
    when(userDAO.findUserById(any())).thenReturn(new User());

    service.createElectionsForNewDarCollection(1);
    service.sendNewDARCollectionMessage(1);

    verify(emailService).sendDarNewCollectionElectionMessage(any(), any());
    verify(emailService).sendNewDARRequestEmail(any(), any(), any(), any());
  }

  @Test
  void testSendNewDARCollectionMessage_MixedAutoOpenAndManualDACsWithSORequiredOnOne()
      throws Exception {
    DarCollection collection = new DarCollection();
    collection.setDarCollectionId(1);

    // Dataset 1: auto-open DAC
    Dataset autoOpenDataset = new Dataset();
    autoOpenDataset.setDatasetId(1);
    autoOpenDataset.setDacId(1);

    // Dataset 2: manual DAC
    Dataset manualDataset = new Dataset();
    manualDataset.setDatasetId(2);
    manualDataset.setDacId(2);

    // Dataset 3: SO required DAC
    Dataset soRequiredDataset = new Dataset();
    soRequiredDataset.setDatasetId(3);
    soRequiredDataset.setDacId(3);

    // Signing Official User
    User signingOfficial = new User();
    signingOfficial.setUserId(1);
    signingOfficial.setEmail("so@example.org");

    DataAccessRequest dar = new DataAccessRequest();
    dar.setReferenceId(UUID.randomUUID().toString());
    dar.setSubmissionDate(Timestamp.from(Instant.now()));
    dar.setDatasetIds(
        List.of(
            autoOpenDataset.getDatasetId(),
            manualDataset.getDatasetId(),
            soRequiredDataset.getDatasetId()));
    DataAccessRequestData darData = new DataAccessRequestData();
    darData.setSigningOfficialEmail(signingOfficial.getEmail());
    dar.setData(darData);
    collection.addDar(dar);

    Dac autoOpenDac = new Dac();
    autoOpenDac.setDacId(1);
    autoOpenDac.setName("AutoOpenDAC");

    Dac manualDac = new Dac();
    manualDac.setDacId(2);
    manualDac.setName("ManualDAC");

    Dac soRequiredDac = new Dac();
    soRequiredDac.setDacId(3);
    soRequiredDac.setName("SORequiredDac");

    User member = new User();
    member.setUserId(3);
    UserRole memberRole =
        new UserRole(UserRoles.MEMBER.getRoleId(), UserRoles.MEMBER.getRoleName());
    memberRole.setDacId(autoOpenDac.getDacId());
    member.setRoles(List.of(memberRole));

    User chair = new User();
    chair.setUserId(4);
    UserRole chairRole =
        new UserRole(UserRoles.CHAIRPERSON.getRoleId(), UserRoles.CHAIRPERSON.getRoleName());
    chairRole.setDacId(manualDac.getDacId());
    chair.setRoles(List.of(chairRole));

    DACAutomationRule disabledAutoOpenRule = mock(DACAutomationRule.class);
    when(disabledAutoOpenRule.ruleType())
        .thenReturn(DACAutomationRuleType.AUTO_OPEN_DAR_FOR_ALL_MEMBERS);
    when(disabledAutoOpenRule.enabledByUserId()).thenReturn(null);

    DACAutomationRule enabledAutoOpenRule = mock(DACAutomationRule.class);
    when(enabledAutoOpenRule.ruleType())
        .thenReturn(DACAutomationRuleType.AUTO_OPEN_DAR_FOR_ALL_MEMBERS);
    when(enabledAutoOpenRule.enabledByUserId()).thenReturn(member.getUserId());

    DACAutomationRule disabledRequireSORule = mock(DACAutomationRule.class);
    when(disabledRequireSORule.ruleType())
        .thenReturn(DACAutomationRuleType.REQUIRE_SO_DAR_APPROVAL);
    when(disabledRequireSORule.enabledByUserId()).thenReturn(null);

    DACAutomationRule enabledRequireSORule = mock(DACAutomationRule.class);
    when(enabledRequireSORule.ruleType()).thenReturn(DACAutomationRuleType.REQUIRE_SO_DAR_APPROVAL);
    when(enabledRequireSORule.enabledByUserId()).thenReturn(member.getUserId());

    when(darCollectionDAO.findDARCollectionByCollectionId(1)).thenReturn(collection);
    when(datasetDAO.findDatasetsByIdList(anyList()))
        .thenReturn(List.of(autoOpenDataset, manualDataset, soRequiredDataset));
    when(dacDAO.findDacsForDatasetIds(anyList()))
        .thenReturn(Set.of(autoOpenDac, manualDac, soRequiredDac));
    when(dacAutomationRuleService.findAllByDacId(autoOpenDac.getDacId()))
        .thenReturn(List.of(enabledAutoOpenRule));
    when(dacAutomationRuleService.findAllByDacId(manualDac.getDacId()))
        .thenReturn(List.of(disabledAutoOpenRule, disabledRequireSORule));
    when(dacAutomationRuleService.findAllByDacId(soRequiredDac.getDacId()))
        .thenReturn(List.of(enabledRequireSORule));
    when(userDAO.findUsersByRoleId(UserRoles.ADMIN.getRoleId())).thenReturn(List.of());
    when(userDAO.findUsersForDatasetsByRole(anyList(), anyList()))
        .thenReturn(Set.of(member, chair));
    when(userDAO.findUserById(any())).thenReturn(new User());

    service.createElectionsForNewDarCollection(1);
    service.sendNewDARCollectionMessage(1);

    verify(emailService).sendDarNewCollectionElectionMessage(any(), any());
    verify(emailService).sendNewDARRequestEmail(any(), any(), any(), any());
    verify(emailService).sendNewDARSigningOfficialRequestEmail(any(), any(), any());
  }

  @Test
  void testNotifySigningOfficialsOfDARSubmission_DAR() throws TemplateException, IOException {
    Dataset dataset = new Dataset();
    dataset.setDatasetId(1);
    dataset.setDataUse(new DataUseBuilder().setGeneralUse(true).build());
    when(datasetDAO.findDatasetsByIdList(List.of(dataset.getDatasetId())))
        .thenReturn(List.of(dataset));

    DarCollection collection = new DarCollection();
    collection.setDarCode("DAR-000123");
    collection.setDarCollectionId(1);
    DataAccessRequest dar = new DataAccessRequest();
    dar.setReferenceId(UUID.randomUUID().toString());
    dar.setDatasetIds(List.of(dataset.getDatasetId()));
    collection.addDar(dar);

    User researcher = createUserWithRole(UserRoles.RESEARCHER);
    collection.setCreateUserId(researcher.getUserId());
    researcher.setInstitutionId(1);
    User signingOfficial = createUserWithRole(UserRoles.SIGNINGOFFICIAL);
    signingOfficial.setEmailPreference(true);
    when(userDAO.getSOsByInstitution(researcher.getInstitutionId()))
        .thenReturn(List.of(signingOfficial));

    service.notifySigningOfficialsOfDARSubmission(
        collection.getMostRecentDar(), researcher, collection.getDarCode());
    verify(emailService, never())
        .sendNewSoProgressReportSubmittedEmail(any(), any(), any(), any(), any());
    verify(emailService, times(1)).sendNewSoDARSubmittedEmail(any(), any(), any(), any(), any());
  }

  @Test
  void testNotifySigningOfficialsOfDARSubmission_PR() throws TemplateException, IOException {
    Dataset dataset = new Dataset();
    dataset.setDatasetId(1);
    dataset.setDataUse(new DataUseBuilder().setGeneralUse(true).build());
    when(datasetDAO.findDatasetsByIdList(List.of(dataset.getDatasetId())))
        .thenReturn(List.of(dataset));

    DarCollection collection = new DarCollection();
    collection.setDarCode("DAR-000123");
    collection.setDarCollectionId(1);
    DataAccessRequest parent = new DataAccessRequest();
    parent.setId(1);
    parent.setCollectionId(collection.getDarCollectionId());
    parent.setReferenceId(UUID.randomUUID().toString());
    parent.setDatasetIds(List.of(dataset.getDatasetId()));
    parent.setSubmissionDate(Timestamp.from(Instant.now()));
    collection.addDar(parent);

    DataAccessRequest child = new DataAccessRequest();
    child.setReferenceId(UUID.randomUUID().toString());
    child.setParentId(parent.getId());
    child.setSubmissionDate(Timestamp.from(Instant.now()));
    child.setCollectionId(collection.getDarCollectionId());
    child.setDatasetIds(List.of(dataset.getDatasetId()));
    collection.addDar(child);

    User researcher = createUserWithRole(UserRoles.RESEARCHER);
    collection.setCreateUserId(researcher.getUserId());
    researcher.setInstitutionId(1);
    User signingOfficial = createUserWithRole(UserRoles.SIGNINGOFFICIAL);
    signingOfficial.setEmailPreference(true);
    when(userDAO.getSOsByInstitution(researcher.getInstitutionId()))
        .thenReturn(List.of(signingOfficial));

    service.notifySigningOfficialsOfDARSubmission(
        collection.getMostRecentDar(), researcher, collection.getDarCode());
    verify(emailService, times(1))
        .sendNewSoProgressReportSubmittedEmail(any(), any(), any(), any(), any());
    verify(emailService, never()).sendNewSoDARSubmittedEmail(any(), any(), any(), any(), any());
  }

  @Test
  void testNotifySigningOfficialsOfDARSubmission_NoInstitution()
      throws TemplateException, IOException {
    Dataset dataset = new Dataset();
    dataset.setDatasetId(1);
    dataset.setDataUse(new DataUseBuilder().setGeneralUse(true).build());

    DarCollection collection = new DarCollection();
    collection.setDarCode("DAR-000123");
    collection.setDarCollectionId(1);
    DataAccessRequest dar = new DataAccessRequest();
    dar.setReferenceId(UUID.randomUUID().toString());
    dar.setDatasetIds(List.of(dataset.getDatasetId()));
    collection.addDar(dar);

    User researcher = createUserWithRole(UserRoles.RESEARCHER);
    collection.setCreateUserId(researcher.getUserId());

    service.notifySigningOfficialsOfDARSubmission(
        collection.getMostRecentDar(), researcher, collection.getDarCode());
    verify(emailService, never())
        .sendNewSoProgressReportSubmittedEmail(any(), any(), any(), any(), any());
    verify(emailService, never()).sendNewSoDARSubmittedEmail(any(), any(), any(), any(), any());
  }

  private DarCollection generateMockDarCollection(Set<Dataset> datasets) {
    DarCollection collection = new DarCollection();
    collection.addDar(generateMockDarWithDatasetId(datasets));
    collection.addDar(generateMockDarWithDatasetId(datasets));
    return collection;
  }

  private DataAccessRequest generateMockDarWithDatasetId(Set<Dataset> datasets) {
    DataAccessRequest dar = new DataAccessRequest();
    DataAccessRequestData data = new DataAccessRequestData();

    Integer datasetId = randomInt(1, 100);
    datasets.add(generateMockDatasetWithDataUse(datasetId));
    dar.addDatasetId(datasetId);
    dar.setData(data);
    dar.setReferenceId(UUID.randomUUID().toString());
    return dar;
  }

  private Dataset generateMockDatasetWithDataUse(Integer datasetId) {
    Dataset dataset = new Dataset();
    dataset.setDatasetId(datasetId);
    return dataset;
  }

  private List<DarCollection> createMockCollections() {
    DarCollection collection = new DarCollection();
    collection.setDarCollectionId(1);
    collection.setDarCode(randomAlphanumeric(5));
    collection.setCreateUserId(1);
    return List.of(collection);
  }

  private Election createMockElection() {
    Election election = new Election();
    election.setElectionId(1);
    election.setReferenceId(UUID.randomUUID().toString());
    return election;
  }

  private User createUserWithRole(UserRoles userRoles) {
    User user = new User();
    user.setUserId(randomInt(1, 100000));
    user.setDisplayName(String.format("%s - %s", userRoles.getRoleName(), user.getUserId()));
    user.setEmail(String.format("%s@test.com", userRoles.getRoleName()));
    UserRole role = new UserRole(userRoles.getRoleId(), userRoles.getRoleName());
    user.setRoles(List.of(role));
    user.setEmailPreference(Boolean.TRUE);
    return user;
  }
}
