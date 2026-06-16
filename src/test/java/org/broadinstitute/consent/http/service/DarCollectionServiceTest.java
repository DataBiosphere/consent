package org.broadinstitute.consent.http.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
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
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.broadinstitute.consent.http.AbstractTestHelper;
import org.broadinstitute.consent.http.db.DaaDAO;
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
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.exceptions.ConsentConflictException;
import org.broadinstitute.consent.http.mail.message.NewCaseMessage;
import org.broadinstitute.consent.http.mail.message.NewDARRequestMessage;
import org.broadinstitute.consent.http.mail.message.NewDARSigningOfficialRequestMessage;
import org.broadinstitute.consent.http.mail.message.NewProgressReportCaseMessage;
import org.broadinstitute.consent.http.mail.message.NewProgressReportRequestMessage;
import org.broadinstitute.consent.http.mail.message.SoDARSubmitted;
import org.broadinstitute.consent.http.mail.message.SoPRSubmitted;
import org.broadinstitute.consent.http.models.CloseoutSupplement;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DarCollection;
import org.broadinstitute.consent.http.models.DarCollectionSummary;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.DataManagementIncident;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.models.Vote;
import org.broadinstitute.consent.http.rules.DACAutomationRule;
import org.broadinstitute.consent.http.rules.DACAutomationRuleType;
import org.broadinstitute.consent.http.service.DarCollectionService.DacUserClassification;
import org.broadinstitute.consent.http.service.dao.DarCollectionServiceDAO;
import org.glassfish.jersey.server.ContainerRequest;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.transaction.TransactionalCallback;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
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
  @Mock private DaaDAO daaDAO;
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
    when(jdbi.onDemand(DaaDAO.class)).thenReturn(daaDAO);
    service =
        new DarCollectionService(
            jdbi, darCollectionServiceDAO, emailService, dacAutomationRuleService);
  }

  @Test
  void testUpdateCollectionToDraftStatusDeletesSnapshots() {
    String firstReferenceId = UUID.randomUUID().toString();
    String secondReferenceId = UUID.randomUUID().toString();
    Integer collectionId = 123;
    Integer userId = 456;
    Date now = new Date();

    DataAccessRequest firstDar = new DataAccessRequest();
    firstDar.setReferenceId(firstReferenceId);
    firstDar.setUserId(userId);
    firstDar.setCreateDate(new Timestamp(now.getTime() - 2_000));
    firstDar.setSubmissionDate(new Timestamp(now.getTime() - 1_000));
    DataAccessRequestData firstData = new DataAccessRequestData();
    firstData.setProjectTitle("project-1");
    firstDar.setData(firstData);

    DataAccessRequest secondDar = new DataAccessRequest();
    secondDar.setReferenceId(secondReferenceId);
    secondDar.setUserId(userId);
    secondDar.setCreateDate(new Timestamp(now.getTime()));
    secondDar.setSubmissionDate(new Timestamp(now.getTime()));
    DataAccessRequestData secondData = new DataAccessRequestData();
    secondData.setProjectTitle("project-2");
    secondDar.setData(secondData);

    DarCollection sourceCollection = new DarCollection();
    sourceCollection.setDarCollectionId(collectionId);
    sourceCollection.addDar(firstDar);
    sourceCollection.addDar(secondDar);

    DarCollection updatedCollection = new DarCollection();
    updatedCollection.setDarCollectionId(collectionId);
    updatedCollection.addDar(firstDar);
    when(darCollectionDAO.findDARCollectionByCollectionId(collectionId))
        .thenReturn(updatedCollection);

    service.updateCollectionToDraftStatus(sourceCollection);

    verify(daaDAO).deleteDarDatasetDaaSnapshotsByReferenceId(firstReferenceId);
    verify(daaDAO).deleteDarDatasetDaaSnapshotsByReferenceId(secondReferenceId);
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

    // For non-privileged roles, votes should only include type and vote, all identifying
    // information removed
    assertFalse(votes.isEmpty());
    for (Vote v : votes) {
      assertNull(v.getUserId(), "userId should be null for de-identified votes");
      assertNull(v.getVoteId(), "voteId should be null for de-identified votes");
      assertNull(v.getElectionId(), "electionId should be null for de-identified votes");
      assertNotNull(v.getType(), "type should be retained for de-identified votes");
      assertTrue(votes.contains(v), "vote should be present in de-identified votes");
      // Vote outcome can be null or Boolean
      assertTrue(
          v.getVote() == null || v.getVote() != null, "vote should be present (null or Boolean)");
    }
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
    user.setChairpersonRole();
    DataAccessRequest dar = new DataAccessRequest();
    dar.setReferenceId(UUID.randomUUID().toString());
    DarCollection collection = createMockCollections().getFirst();
    collection.addDar(dar);
    when(darCollectionServiceDAO.createElectionsForDarByUser(any(), any()))
        .thenReturn(List.of("electionId"));
    when(voteDAO.findVoteUsersByElectionReferenceIdList(any())).thenReturn(List.of(new User()));
    when(darCollectionDAO.findDARCollectionByCollectionId(collection.getDarCollectionId()))
        .thenReturn(collection);

    service.createElectionsForDarCollection(user, collection);
    verify(darCollectionServiceDAO).createElectionsForDarByUser(any(), eq(dar));
    verify(voteDAO).findVoteUsersByElectionReferenceIdList(any());
    verify(emailService).sendMessage(any(NewCaseMessage.class), any());
    verify(darCollectionDAO).findDARCollectionByCollectionId(any());
  }

  @Test
  void testApproveCollection_UserSO_ApprovalNotRequired() {
    User user = new User();
    user.setEmail("email");
    user.setSigningOfficialRole();
    DataAccessRequest dar = new DataAccessRequest();
    dar.setReferenceId(UUID.randomUUID().toString());
    DarCollection collection = createMockCollections().getFirst();
    collection.addDar(dar);

    assertThrows(
        ConsentConflictException.class,
        () -> service.approveDarCollection(user, collection, request));
  }

  @Test
  void testApproveCollectionWithSORequired_NotSigningOfficial() {
    User user = new User();
    user.setEmail("email");
    user.setUserId(1);
    User notASigningOfficial = new User();
    notASigningOfficial.setEmail("email2");
    notASigningOfficial.setUserId(2);
    DataAccessRequest dar = new DataAccessRequest();
    dar.setReferenceId(UUID.randomUUID().toString());
    dar.setUserId(user.getUserId());
    dar.setRequiresSOApproval(true);
    DataAccessRequestData darData = new DataAccessRequestData();
    darData.setSigningOfficialEmail("someone else");
    dar.setData(darData);
    DarCollection collection = createMockCollections().getFirst();
    collection.addDar(dar);

    assertThrows(
        ForbiddenException.class,
        () -> service.approveDarCollection(notASigningOfficial, collection, request));
  }

  @Test
  void testApproveCollectionWithSORequiredNoDARData() {
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
    dar.setRequiresSOApproval(true);
    DarCollection collection = createMockCollections().getFirst();
    collection.addDar(dar);

    assertThrows(
        ConsentConflictException.class,
        () -> service.approveDarCollection(signingOfficial, collection, request));
  }

  @Test
  void testApproveCollectionWithSORequiredNotSOInDARThrows() {
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
    dar.setRequiresSOApproval(true);
    DarCollection collection = createMockCollections().getFirst();
    collection.addDar(dar);

    assertThrows(
        ForbiddenException.class,
        () -> service.approveDarCollection(signingOfficial, collection, request));
  }

  @Test
  void testApproveCollectionWithSORequired_Approving() {
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
    dar.setRequiresSOApproval(true);
    DataAccessRequestData darData = new DataAccessRequestData();
    darData.setSigningOfficialEmail(signingOfficial.getEmail());
    dar.setData(darData);
    DarCollection collection = createMockCollections().getFirst();
    collection.addDar(dar);

    assertDoesNotThrow(() -> service.approveDarCollection(signingOfficial, collection, request));
  }

  @Test
  void testApproveDarCollectionDMI() {
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
    dar.setCloseoutSigningOfficialApprovedUserId(signingOfficial.getUserId());
    DataAccessRequestData darData = new DataAccessRequestData();
    darData.setSigningOfficialEmail(signingOfficial.getEmail());
    darData.setDmi(new DataManagementIncident(List.of("one"), "my incident"));
    dar.setData(darData);
    dar.setRequiresSOApproval(true);

    DarCollection collection = new DarCollection();
    collection.addDar(dar);

    doNothing().when(dataAccessRequestDAO).updateDarApprovalSO(anyInt(), anyString());
    when(userDAO.findUserById(user.getUserId())).thenReturn(user);
    service.approveDarCollection(signingOfficial, collection, request);
    verify(dacAutomationRuleService, never())
        .triggerDACRuleSettings(any(), anyList(), anyString(), any());
  }

  @Test
  void testApproveDarCollectionCloseout() {
    User user = new User();
    user.setEmail("email");
    user.setUserId(1);
    User signingOfficial = new User();
    signingOfficial.setRoles(List.of(UserRoles.SigningOfficial()));
    signingOfficial.setEmail("email2");
    signingOfficial.setUserId(2);
    DataAccessRequest dar = new DataAccessRequest();
    dar.setParentId(-1);
    dar.setReferenceId(UUID.randomUUID().toString());
    dar.setUserId(user.getUserId());
    dar.setCloseoutSigningOfficialApprovedUserId(signingOfficial.getUserId());
    DataAccessRequestData darData = new DataAccessRequestData();
    darData.setSigningOfficialEmail(signingOfficial.getEmail());
    darData.setCloseoutSupplement(
        new CloseoutSupplement(List.of("reasons"), "other text", signingOfficial.getUserId()));
    dar.setData(darData);
    dar.setRequiresSOApproval(true);

    DarCollection collection = new DarCollection();
    collection.addDar(dar);

    doNothing().when(dataAccessRequestDAO).updateDarApprovalSO(anyInt(), anyString());
    when(userDAO.findUserById(user.getUserId())).thenReturn(user);
    service.approveDarCollection(signingOfficial, collection, request);
    verify(dacAutomationRuleService, never())
        .triggerDACRuleSettings(any(), anyList(), anyString(), any());
  }

  @Test
  void testApproveCollectionWithSORequired_Approving_NoSO_Email() {
    User user = new User();
    user.setEmail("email");
    user.setUserId(1);
    User signingOfficial = new User();
    signingOfficial.setRoles(List.of(UserRoles.SigningOfficial()));
    signingOfficial.setUserId(2);
    DataAccessRequest dar = new DataAccessRequest();
    dar.setReferenceId(UUID.randomUUID().toString());
    dar.setUserId(user.getUserId());
    dar.setRequiresSOApproval(true);
    DataAccessRequestData darData = new DataAccessRequestData();
    darData.setSigningOfficialEmail(signingOfficial.getEmail());
    dar.setData(darData);
    DarCollection collection = createMockCollections().getFirst();
    collection.addDar(dar);

    assertThrows(
        ForbiddenException.class,
        () -> service.approveDarCollection(signingOfficial, collection, request));
  }

  @Test
  void testApproveCollectionWithSORequiredAlreadyApprovedDARThrows() {
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
    dar.setRequiresSOApproval(true);
    dar.setApprovingSigningOfficialUserId(signingOfficial.getUserId());
    dar.setApprovingSigningOfficialApprovedDate(Timestamp.from(Instant.now()));
    DataAccessRequestData darData = new DataAccessRequestData();
    darData.setSigningOfficialEmail("notTheSigningOfficialMakingRequest");
    dar.setData(darData);
    DarCollection collection = createMockCollections().getFirst();
    collection.addDar(dar);

    assertThrows(
        BadRequestException.class,
        () -> service.approveDarCollection(signingOfficial, collection, request));
  }

  @Test
  void testCreateElectionsForDarCollection_Chairperson_SO_Approval_Not_Needed()
      throws SQLException {
    User user = new User();
    user.setEmail("email");
    user.setUserId(1);
    User chairperson = new User();
    chairperson.setRoles(List.of(UserRoles.Chairperson()));
    chairperson.setEmail("email2");
    chairperson.setUserId(2);
    DataAccessRequest dar = new DataAccessRequest();
    dar.setReferenceId(UUID.randomUUID().toString());
    dar.setUserId(user.getUserId());
    dar.setRequiresSOApproval(false);
    DataAccessRequestData darData = new DataAccessRequestData();
    darData.setSigningOfficialEmail(chairperson.getEmail());
    dar.setData(darData);
    DarCollection collection = createMockCollections().getFirst();
    collection.addDar(dar);

    when(darCollectionServiceDAO.createElectionsForDarByUser(any(), any()))
        .thenReturn(List.of(UUID.randomUUID().toString()));
    when(darCollectionDAO.findDARCollectionByCollectionId(collection.getDarCollectionId()))
        .thenReturn(collection);
    when(voteDAO.findVoteUsersByElectionReferenceIdList(any())).thenReturn(List.of(chairperson));

    assertDoesNotThrow(() -> service.createElectionsForDarCollection(chairperson, collection));
  }

  @Test
  void testCreateElectionsForDarCollection_Chairperson_SO_Approval_Needed() {
    User user = new User();
    user.setEmail("email");
    user.setUserId(1);
    User chairperson = new User();
    chairperson.setRoles(List.of(UserRoles.Chairperson()));
    chairperson.setEmail("email2");
    chairperson.setUserId(2);
    DataAccessRequest dar = new DataAccessRequest();
    dar.setReferenceId(UUID.randomUUID().toString());
    dar.setUserId(user.getUserId());
    dar.setRequiresSOApproval(true);
    DataAccessRequestData darData = new DataAccessRequestData();
    darData.setSigningOfficialEmail(chairperson.getEmail());
    dar.setData(darData);
    DarCollection collection = createMockCollections().getFirst();
    collection.addDar(dar);

    assertThrows(
        ForbiddenException.class,
        () -> service.createElectionsForDarCollection(chairperson, collection));
  }

  @Test
  void testCreateElectionsForDarCollection_Chairperson_With_SO_Approval() throws SQLException {
    User user = new User();
    user.setEmail("email");
    user.setUserId(1);
    User chairperson = new User();
    chairperson.setRoles(List.of(UserRoles.Chairperson()));
    chairperson.setEmail("email2");
    chairperson.setUserId(2);
    DataAccessRequest dar = new DataAccessRequest();
    dar.setReferenceId(UUID.randomUUID().toString());
    dar.setUserId(user.getUserId());
    dar.setRequiresSOApproval(true);
    dar.setApprovingSigningOfficialUserId(5);
    DataAccessRequestData darData = new DataAccessRequestData();
    darData.setSigningOfficialEmail(chairperson.getEmail());
    dar.setData(darData);
    DarCollection collection = createMockCollections().getFirst();
    collection.addDar(dar);

    when(darCollectionServiceDAO.createElectionsForDarByUser(any(), any()))
        .thenReturn(List.of(UUID.randomUUID().toString()));
    when(darCollectionDAO.findDARCollectionByCollectionId(collection.getDarCollectionId()))
        .thenReturn(collection);
    when(voteDAO.findVoteUsersByElectionReferenceIdList(any())).thenReturn(List.of(chairperson));

    assertDoesNotThrow(() -> service.createElectionsForDarCollection(chairperson, collection));
  }

  @Test
  void testCreateElectionsForProgressReport() throws Exception {
    User user = new User();
    user.setEmail("email");
    user.setChairpersonRole();
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

    service.createElectionsForDarCollection(user, collection);

    verify(darCollectionDAO).findDARCollectionByCollectionId(collection.getDarCollectionId());
    verify(emailService).sendMessage(any(NewProgressReportCaseMessage.class), any());
  }

  @Test
  void testCreateElectionsForDarCollectionEmpty() {
    User user = new User();
    user.setEmail("email");
    user.setChairpersonRole();
    DataAccessRequest dar = new DataAccessRequest();
    dar.setReferenceId(UUID.randomUUID().toString());
    DarCollection collection = createMockCollections().getFirst();
    collection.addDar(dar);

    assertThrows(
        IllegalStateException.class,
        () -> service.createElectionsForDarCollection(user, collection));
  }

  @Test
  void testCreateElectionsForDarCollectionVoteUsersException() throws Exception {
    User user = new User();
    user.setEmail("email");
    user.setChairpersonRole();
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

    service.createElectionsForDarCollection(user, collection);
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
    user.setEmail("signingOfficial");
    DarCollectionSummary summary = new DarCollectionSummary();
    Dataset datasetOne = new Dataset();
    datasetOne.setDatasetId(1);
    Dataset datasetTwo = new Dataset();
    datasetTwo.setDatasetId(2);
    summary.addDatasetId(datasetOne.getDatasetId());
    summary.addDatasetId(datasetTwo.getDatasetId());
    summary.setRequiresSOApproval(true);
    summary.setSigningOfficialEmail(user.getEmail());
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
  void testProcessDarCollectionSummariesForDAC_SO_Unreviewed_NotTheSO() {
    User user = new User();
    user.setUserId(1);
    user.setEmail("signingOfficial");
    User notTheSigningOfficial = new User();
    notTheSigningOfficial.setUserId(1);
    notTheSigningOfficial.setEmail("notTheSigningOfficial");
    DarCollectionSummary summary = new DarCollectionSummary();
    Dataset datasetOne = new Dataset();
    datasetOne.setDatasetId(1);
    Dataset datasetTwo = new Dataset();
    datasetTwo.setDatasetId(2);
    summary.addDatasetId(datasetOne.getDatasetId());
    summary.addDatasetId(datasetTwo.getDatasetId());
    summary.setRequiresSOApproval(true);
    summary.setSigningOfficialEmail(user.getEmail());
    when(darCollectionSummaryDAO.getDarCollectionSummariesForSO(any()))
        .thenReturn(List.of(summary));

    List<DarCollectionSummary> summaries =
        service.getSummariesForRole(notTheSigningOfficial, UserRoles.SIGNINGOFFICIAL);
    assertNotNull(summaries);
    assertEquals(1, summaries.size());
    DarCollectionSummary s = summaries.getFirst();
    assertTrue(s.getStatus().equalsIgnoreCase(DarCollectionStatus.SUBMITTED.getValue()));
    assertTrue(s.requiresSOApproval());
    assertFalse(s.getActions().contains(DarCollectionActions.APPROVE.getValue()));
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
    assertFalse(s.getActions().contains(DarCollectionActions.APPROVE.getValue()));
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
  void testProcessDarCollectionSummariesForSOWithPendingCloseout_ForDifferentSO() {
    User user = new User();
    user.setUserId(1);
    user.addRole(UserRoles.SigningOfficial());
    user.setInstitutionId(1);

    User notThatSigningOfficial = new User();
    notThatSigningOfficial.setUserId(2);
    notThatSigningOfficial.addRole(UserRoles.SigningOfficial());
    notThatSigningOfficial.setInstitutionId(1);

    DarCollectionSummary summary = new DarCollectionSummary();
    summary.setLatestReferenceId(UUID.randomUUID().toString());

    CloseoutSupplement closeoutSupplement =
        new CloseoutSupplement(List.of("Closeout"), "Closeout", 1);
    summary.setCloseoutSupplement(closeoutSupplement);

    summary.setCloseoutSigningOfficialApprovalDate(null);

    when(darCollectionSummaryDAO.getDarCollectionSummariesForSO(user.getInstitutionId()))
        .thenReturn(List.of(summary));

    List<DarCollectionSummary> summaries =
        service.getSummariesForRole(notThatSigningOfficial, UserRoles.SIGNINGOFFICIAL);

    assertNotNull(summaries);
    assertEquals(1, summaries.size());
    assertFalse(
        summaries
            .getFirst()
            .getActions()
            .contains(DarCollectionActions.REVIEW_PROGRESS_REPORT.name()));
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
        Set.of(DarCollectionActions.VOTE.getValue(), DarCollectionActions.CANCEL.getValue());
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
    Set<String> expectedFiveActions = Set.of(DarCollectionActions.VOTE.getValue());
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
  void testProcessDarCollectionSummariesForChairWithSOApprovalRequired_And_Granted() {
    User user = new User();
    user.setUserId(1);
    user.addRole(UserRoles.Chairperson());
    DarCollectionSummary summary = new DarCollectionSummary();
    summary.setLatestReferenceId(UUID.randomUUID().toString());
    summary.setRequiresSOApproval(true);
    summary.setSOApprover(1);
    summary.addDatasetId(1);
    when(darCollectionSummaryDAO.getDarCollectionSummariesForDACRole(
            user.getUserId(), UserRoles.CHAIRPERSON.getRoleId()))
        .thenReturn(List.of(summary));

    List<DarCollectionSummary> summaries = service.getSummariesForRole(user, UserRoles.CHAIRPERSON);

    assertNotNull(summaries);
    assertEquals(1, summaries.size());
    // Chair summary have the open action after it is approved by the SO.
    assertTrue(summaries.getFirst().getActions().contains(DarCollectionActions.OPEN.getValue()));
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
        Set.of(DarCollectionActions.VOTE.getValue(), DarCollectionActions.CANCEL.getValue());
    assertTrue(
        summaryResult.getStatus().equalsIgnoreCase(DarCollectionStatus.IN_PROCESS.getValue()));
    assertEquals(expectedActions, summaryResult.getActions());
  }

  @Test
  void testGetSummaryForRoleByCollectionId_ChairDoesNotOpenWhenRpElectionIsOpen() {
    Dac dac = new Dac();
    dac.setDacId(1);
    User user = new User();
    user.setUserId(1);
    user.setChairpersonRoleWithDAC(dac.getDacId());

    DarCollectionSummary summary = new DarCollectionSummary();
    Integer collectionId = randomInt(1, 100);
    summary.setDarCollectionId(collectionId);
    summary.addDatasetId(1);

    Election dataAccessElection = new Election();
    dataAccessElection.setElectionId(1);
    dataAccessElection.setElectionType(ElectionType.DATA_ACCESS.getValue());
    dataAccessElection.setStatus(ElectionStatus.CLOSED.getValue());
    summary.addElection(dataAccessElection);

    Election rpElection = new Election();
    rpElection.setElectionId(2);
    rpElection.setElectionType(ElectionType.RP.getValue());
    rpElection.setStatus(ElectionStatus.OPEN.getValue());
    summary.addElection(rpElection);

    when(darCollectionSummaryDAO.getDarCollectionSummaryForDACByCollectionId(
            user.getUserId(), List.of(), collectionId))
        .thenReturn(summary);
    when(datasetDAO.findDatasetIdsByDacIds(any())).thenReturn(List.of());

    DarCollectionSummary summaryResult =
        service.getSummaryForRoleByCollectionId(user, UserRoles.CHAIRPERSON, collectionId);

    assertNotNull(summaryResult);
    assertEquals(DarCollectionStatus.IN_PROCESS.getValue(), summaryResult.getStatus());
    assertEquals(Set.of(DarCollectionActions.VOTE.getValue()), summaryResult.getActions());
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
    when(userDAO.findUsersForDatasetsByRole(anyList(), anyList())).thenReturn(Set.of(chair));
    service.createElectionsForNewDarCollection(1);
    service.sendNewDARCollectionMessage(1);
    verify(emailService).sendMessage(any(NewDARRequestMessage.class), any());
    verify(emailService, never()).sendMessage(any(NewCaseMessage.class), any());
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
    when(userDAO.findUsersForDatasetsByRole(anyList(), anyList())).thenReturn(Set.of(member));
    when(userDAO.findUserById(any())).thenReturn(new User());

    service.createElectionsForNewDarCollection(1);
    service.sendNewDARCollectionMessage(1);

    verify(emailService).sendMessage(any(NewCaseMessage.class), any());
    verify(emailService, never()).sendMessage(any(NewDARRequestMessage.class), any());
  }

  @Test
  void testSendNewDARCollectionMessage_AutoOpenProgressReportDACsOnly() throws Exception {
    DarCollection collection = new DarCollection();
    collection.setDarCollectionId(1);

    DataAccessRequest parentDar = new DataAccessRequest();
    parentDar.setId(1);
    parentDar.setReferenceId(UUID.randomUUID().toString());
    parentDar.setSubmissionDate(Timestamp.from(Instant.now()));
    collection.addDar(parentDar);

    DataAccessRequest progressReport = new DataAccessRequest();
    progressReport.setReferenceId(UUID.randomUUID().toString());
    progressReport.setParentId(parentDar.getId());
    progressReport.setSubmissionDate(Timestamp.from(Instant.now()));

    Dataset dataset = new Dataset();
    dataset.setDatasetId(1);
    dataset.setDacId(1);

    progressReport.setDatasetIds(List.of(dataset.getDatasetId()));
    collection.addDar(progressReport);

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
    when(userDAO.findUsersForDatasetsByRole(anyList(), anyList())).thenReturn(Set.of(member));
    when(userDAO.findUserById(any())).thenReturn(new User());

    service.createElectionsForNewDarCollection(1);
    service.sendNewDARCollectionMessage(1);

    verify(emailService).sendMessage(any(NewProgressReportCaseMessage.class), any());
    verify(emailService, never()).sendMessage(any(NewCaseMessage.class), any());
    verify(emailService, never()).sendMessage(any(NewDARRequestMessage.class), any());
    verify(emailService, never()).sendMessage(any(NewProgressReportRequestMessage.class), any());
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
    dar.setRequiresSOApproval(true);

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
    when(darCollectionDAO.findDARCollectionByCollectionId(1)).thenReturn(collection);
    when(datasetDAO.findDatasetsByIdList(anyList())).thenReturn(List.of(dataset));
    when(dacDAO.findDacsForDatasetIds(anyList())).thenReturn(Set.of(dac));
    when(dacAutomationRuleService.findAllByDacId(anyInt())).thenReturn(List.of(rule));
    when(userDAO.findUsersForDatasetsByRole(anyList(), anyList())).thenReturn(Set.of(member));
    when(userDAO.findUserById(any())).thenReturn(new User());
    when(userDAO.findUserByEmail(signingOfficial.getEmail())).thenReturn(signingOfficial);

    service.createElectionsForNewDarCollection(1);
    service.sendNewDARCollectionMessage(1);

    verify(emailService, never()).sendMessage(any(NewCaseMessage.class), any());
    verify(emailService, never()).sendMessage(any(NewDARRequestMessage.class), any());
    verify(emailService).sendMessage(any(NewDARSigningOfficialRequestMessage.class), any());
  }

  @Test
  void testSendNewDARCollectionMessage_SOApproval() throws Exception {
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
    dar.setRequiresSOApproval(true);
    dar.setApprovingSigningOfficialUserId(signingOfficial.getUserId());

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
        new UserRole(UserRoles.CHAIRPERSON.getRoleId(), UserRoles.CHAIRPERSON.getRoleName());
    memberRole.setDacId(dac.getDacId());
    member.setRoles(List.of(memberRole));

    DACAutomationRule rule = mock(DACAutomationRule.class);

    when(rule.ruleType()).thenReturn(DACAutomationRuleType.REQUIRE_SO_DAR_APPROVAL);
    when(darCollectionDAO.findDARCollectionByCollectionId(1)).thenReturn(collection);
    when(datasetDAO.findDatasetsByIdList(anyList())).thenReturn(List.of(dataset));
    when(dacDAO.findDacsForDatasetIds(anyList())).thenReturn(Set.of(dac));
    when(dacAutomationRuleService.findAllByDacId(anyInt())).thenReturn(List.of(rule));
    when(userDAO.findUsersForDatasetsByRole(anyList(), anyList())).thenReturn(Set.of(member));
    when(userDAO.findUserById(any())).thenReturn(new User());

    service.createElectionsForNewDarCollection(1);
    service.sendNewDARCollectionMessage(1);

    verify(emailService, never()).sendMessage(any(NewCaseMessage.class), any());
    verify(emailService).sendMessage(any(NewDARRequestMessage.class), any());
    verify(emailService, never())
        .sendMessage(any(NewDARSigningOfficialRequestMessage.class), any());
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
    when(darCollectionDAO.findDARCollectionByCollectionId(1)).thenReturn(collection);
    when(datasetDAO.findDatasetsByIdList(anyList())).thenReturn(List.of(dataset));
    when(dacDAO.findDacsForDatasetIds(anyList())).thenReturn(Set.of(dac));
    when(dacAutomationRuleService.findAllByDacId(anyInt())).thenReturn(List.of(rule));
    when(userDAO.findUsersForDatasetsByRole(anyList(), anyList())).thenReturn(Set.of(chairperson));
    when(userDAO.findUserById(any())).thenReturn(new User());

    service.createElectionsForNewDarCollection(1);
    service.sendNewDARCollectionMessage(1);

    verify(emailService, never()).sendMessage(any(NewCaseMessage.class), any());
    verify(emailService).sendMessage(any(NewProgressReportRequestMessage.class), any());
    verify(emailService, never())
        .sendMessage(any(NewDARSigningOfficialRequestMessage.class), any());
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
    when(userDAO.findUsersForDatasetsByRole(anyList(), anyList()))
        .thenReturn(Set.of(member, chair));
    when(userDAO.findUserById(any())).thenReturn(new User());

    service.createElectionsForNewDarCollection(1);
    service.sendNewDARCollectionMessage(1);

    verify(emailService).sendMessage(any(NewCaseMessage.class), any());
    verify(emailService).sendMessage(any(NewDARRequestMessage.class), any());
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
    dar.setRequiresSOApproval(true);
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

    DACAutomationRule enabledRequireSORule = mock(DACAutomationRule.class);
    when(enabledRequireSORule.ruleType()).thenReturn(DACAutomationRuleType.REQUIRE_SO_DAR_APPROVAL);

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
    when(userDAO.findUsersForDatasetsByRole(anyList(), anyList()))
        .thenReturn(Set.of(member, chair));
    when(userDAO.findUserById(any())).thenReturn(new User());
    when(userDAO.findUserByEmail(signingOfficial.getEmail())).thenReturn(signingOfficial);

    service.createElectionsForNewDarCollection(1);
    service.sendNewDARCollectionMessage(1);

    verify(emailService, never()).sendMessage(any(NewCaseMessage.class), any());
    verify(emailService, never()).sendMessage(any(NewDARRequestMessage.class), any());
    verify(emailService).sendMessage(any(NewDARSigningOfficialRequestMessage.class), any());
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
    verify(emailService, never()).sendMessage(any(SoPRSubmitted.class), any());
    verify(emailService, times(1)).sendMessage(any(SoDARSubmitted.class), any());
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
    verify(emailService, times(1)).sendMessage(any(SoPRSubmitted.class), any());
    verify(emailService, never()).sendMessage(any(SoDARSubmitted.class), any());
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
    verify(emailService, never()).sendMessage(any(SoPRSubmitted.class), any());
    verify(emailService, never()).sendMessage(any(SoDARSubmitted.class), any());
  }

  @Test
  void testNotifySigningOfficialsOfDARSubmission_NullResearcher()
      throws TemplateException, IOException {
    DarCollection collection = new DarCollection();
    collection.setDarCode("DAR-000123");
    collection.setDarCollectionId(1);
    DataAccessRequest dar = new DataAccessRequest();
    dar.setReferenceId(UUID.randomUUID().toString());
    dar.setUserId(42);
    dar.setDatasetIds(List.of(1));
    collection.addDar(dar);

    service.notifySigningOfficialsOfDARSubmission(
        collection.getMostRecentDar(), null, collection.getDarCode());
    verify(emailService, never()).sendMessage(any(SoPRSubmitted.class), any());
    verify(emailService, never()).sendMessage(any(SoDARSubmitted.class), any());
  }

  // Helper to make inTransaction actually execute its callback
  private void stubInTransactionToExecute() {
    doAnswer(
            invocation -> {
              TransactionalCallback<Object, ElectionDAO, Exception> cb = invocation.getArgument(0);
              cb.inTransaction(electionDAO);
              return null;
            })
        .when(electionDAO)
        .inTransaction(any());
  }

  @Test
  void testCreateElectionsAndVotesForAutoOpenDacs_RequiresSOApprovalNoSigningOfficial() {
    DataAccessRequest dar = new DataAccessRequest();
    dar.setRequiresSOApproval(true);
    // approvingSigningOfficialUserId is null by default

    DacUserClassification classification = new DacUserClassification();
    Dataset dataset = new Dataset();
    dataset.setDatasetId(1);
    dataset.setDacId(10);
    classification.autoOpenDatasets.add(dataset);

    service.createElectionsAndVotesForAutoOpenDacs(classification, dar);

    verifyNoInteractions(electionDAO);
    verifyNoInteractions(dacAutomationRuleService);
  }

  @Test
  void testCreateElectionsAndVotesForAutoOpenDacs_RequiresSOApprovalWithApprovedSigningOfficial() {
    DataAccessRequest dar = new DataAccessRequest();
    dar.setRequiresSOApproval(true);
    dar.setApprovingSigningOfficialUserId(42);
    dar.setReferenceId(UUID.randomUUID().toString());
    dar.setData(new DataAccessRequestData());

    Dataset dataset = new Dataset();
    dataset.setDatasetId(1);
    dataset.setDacId(10);

    DacUserClassification classification = new DacUserClassification();
    classification.autoOpenDatasets.add(dataset);

    when(electionDAO.findLastElectionByReferenceIdDatasetIdAndType(any(), anyInt(), any()))
        .thenReturn(null);
    when(electionDAO.findElectionsByReferenceIdAndDatasetId(any(), anyInt())).thenReturn(List.of());
    when(dacAutomationRuleService.createOpenElectionForDAR(
            any(), any(), eq(ElectionType.DATA_ACCESS)))
        .thenReturn(100);
    when(dacAutomationRuleService.createOpenElectionForDAR(any(), any(), eq(ElectionType.RP)))
        .thenReturn(200);
    stubInTransactionToExecute();

    service.createElectionsAndVotesForAutoOpenDacs(classification, dar);

    verify(dacAutomationRuleService)
        .createOpenElectionForDAR(dar, dataset, ElectionType.DATA_ACCESS);
    verify(dacAutomationRuleService).createOpenElectionForDAR(dar, dataset, ElectionType.RP);
  }

  @Test
  void testCreateElectionsAndVotesForAutoOpenDacs_EmptyDatasets() {
    DataAccessRequest dar = new DataAccessRequest();
    dar.setData(new DataAccessRequestData());

    DacUserClassification classification = new DacUserClassification();
    // autoOpenDatasets is empty

    service.createElectionsAndVotesForAutoOpenDacs(classification, dar);

    verifyNoInteractions(electionDAO);
    verifyNoInteractions(dacAutomationRuleService);
  }

  @Test
  void testCreateElectionsAndVotesForAutoOpenDacs_DatasetWithExistingOpenElection() {
    DataAccessRequest dar = new DataAccessRequest();
    dar.setReferenceId(UUID.randomUUID().toString());
    dar.setData(new DataAccessRequestData());

    Dataset dataset = new Dataset();
    dataset.setDatasetId(1);
    dataset.setDacId(10);

    DacUserClassification classification = new DacUserClassification();
    classification.autoOpenDatasets.add(dataset);

    Election openElection = new Election();
    openElection.setStatus(ElectionStatus.OPEN.getValue());
    when(electionDAO.findLastElectionByReferenceIdDatasetIdAndType(
            dar.getReferenceId(), dataset.getDatasetId(), ElectionType.DATA_ACCESS.getValue()))
        .thenReturn(openElection);

    service.createElectionsAndVotesForAutoOpenDacs(classification, dar);

    verify(electionDAO)
        .findLastElectionByReferenceIdDatasetIdAndType(
            dar.getReferenceId(), dataset.getDatasetId(), ElectionType.DATA_ACCESS.getValue());
    verify(dacAutomationRuleService, never()).createOpenElectionForDAR(any(), any(), any());
    verify(electionDAO, never()).inTransaction(any());
  }

  @Test
  void testCreateElectionsAndVotesForAutoOpenDacs_ArchivesOldElectionsBeforeCreating() {
    DataAccessRequest dar = new DataAccessRequest();
    dar.setReferenceId(UUID.randomUUID().toString());
    dar.setData(new DataAccessRequestData());

    Dataset dataset = new Dataset();
    dataset.setDatasetId(1);
    dataset.setDacId(10);

    DacUserClassification classification = new DacUserClassification();
    classification.autoOpenDatasets.add(dataset);

    Election oldElection = new Election();
    oldElection.setElectionId(55);
    when(electionDAO.findLastElectionByReferenceIdDatasetIdAndType(any(), anyInt(), any()))
        .thenReturn(null);
    when(electionDAO.findElectionsByReferenceIdAndDatasetId(
            dar.getReferenceId(), dataset.getDatasetId()))
        .thenReturn(List.of(oldElection));
    when(dacAutomationRuleService.createOpenElectionForDAR(
            any(), any(), eq(ElectionType.DATA_ACCESS)))
        .thenReturn(100);
    when(dacAutomationRuleService.createOpenElectionForDAR(any(), any(), eq(ElectionType.RP)))
        .thenReturn(200);
    stubInTransactionToExecute();

    service.createElectionsAndVotesForAutoOpenDacs(classification, dar);

    verify(electionDAO).archiveElectionByIds(eq(List.of(55)), any());
  }

  @Test
  void testCreateElectionsAndVotesForAutoOpenDacs_CreatesElectionsAndVotesForUsers() {
    DataAccessRequest dar = new DataAccessRequest();
    dar.setReferenceId(UUID.randomUUID().toString());
    dar.setData(new DataAccessRequestData());

    Dataset dataset = new Dataset();
    dataset.setDatasetId(1);
    dataset.setDacId(10);

    User member = createUserWithRoleAndDacId(UserRoles.MEMBER, 10);

    DacUserClassification classification = new DacUserClassification();
    classification.autoOpenDatasets.add(dataset);
    classification.autoOpenUsers.add(member);

    when(electionDAO.findLastElectionByReferenceIdDatasetIdAndType(any(), anyInt(), any()))
        .thenReturn(null);
    when(electionDAO.findElectionsByReferenceIdAndDatasetId(any(), anyInt())).thenReturn(List.of());
    when(dacAutomationRuleService.createOpenElectionForDAR(
            any(), any(), eq(ElectionType.DATA_ACCESS)))
        .thenReturn(100);
    when(dacAutomationRuleService.createOpenElectionForDAR(any(), any(), eq(ElectionType.RP)))
        .thenReturn(200);
    stubInTransactionToExecute();

    service.createElectionsAndVotesForAutoOpenDacs(classification, dar);

    verify(dacAutomationRuleService)
        .createOpenElectionForDAR(dar, dataset, ElectionType.DATA_ACCESS);
    verify(dacAutomationRuleService).createOpenElectionForDAR(dar, dataset, ElectionType.RP);
    verify(dacAutomationRuleService).createVoteForElection(100, member.getUserId(), VoteType.DAC);
    verify(dacAutomationRuleService).createVoteForElection(200, member.getUserId(), VoteType.DAC);
  }

  @Test
  void testCreateElectionsAndVotesForAutoOpenDacs_MultipleDatasets_OneAlreadyOpen() {
    DataAccessRequest dar = new DataAccessRequest();
    dar.setReferenceId(UUID.randomUUID().toString());
    dar.setData(new DataAccessRequestData());

    Dataset openDataset = new Dataset();
    openDataset.setDatasetId(1);
    openDataset.setDacId(10);

    Dataset closedDataset = new Dataset();
    closedDataset.setDatasetId(2);
    closedDataset.setDacId(20);

    DacUserClassification classification = new DacUserClassification();
    classification.autoOpenDatasets.add(openDataset);
    classification.autoOpenDatasets.add(closedDataset);

    Election openElection = new Election();
    openElection.setStatus(ElectionStatus.OPEN.getValue());

    when(electionDAO.findLastElectionByReferenceIdDatasetIdAndType(
            dar.getReferenceId(), openDataset.getDatasetId(), ElectionType.DATA_ACCESS.getValue()))
        .thenReturn(openElection);
    when(electionDAO.findLastElectionByReferenceIdDatasetIdAndType(
            dar.getReferenceId(),
            closedDataset.getDatasetId(),
            ElectionType.DATA_ACCESS.getValue()))
        .thenReturn(null);
    when(electionDAO.findElectionsByReferenceIdAndDatasetId(
            dar.getReferenceId(), closedDataset.getDatasetId()))
        .thenReturn(List.of());
    when(dacAutomationRuleService.createOpenElectionForDAR(
            any(), eq(closedDataset), eq(ElectionType.DATA_ACCESS)))
        .thenReturn(100);
    when(dacAutomationRuleService.createOpenElectionForDAR(
            any(), eq(closedDataset), eq(ElectionType.RP)))
        .thenReturn(200);
    stubInTransactionToExecute();

    service.createElectionsAndVotesForAutoOpenDacs(classification, dar);

    // Only the closed dataset should have elections created
    verify(dacAutomationRuleService, never())
        .createOpenElectionForDAR(any(), eq(openDataset), any());
    verify(dacAutomationRuleService)
        .createOpenElectionForDAR(dar, closedDataset, ElectionType.DATA_ACCESS);
    verify(dacAutomationRuleService).createOpenElectionForDAR(dar, closedDataset, ElectionType.RP);
  }

  @Test
  void testCreateVotesForAllUsers_EmptyUsers() {
    DataAccessRequest dar = new DataAccessRequest();
    dar.setData(new DataAccessRequestData());

    service.createVotesForAllUsers(Set.of(), 1, 2, dar, 1);

    verifyNoInteractions(dacAutomationRuleService);
  }

  @Test
  void testCreateVotesForAllUsers_NonChairUser() {
    User member = createUserWithRole(UserRoles.MEMBER);
    DataAccessRequest dar = new DataAccessRequest();
    dar.setData(new DataAccessRequestData());

    service.createVotesForAllUsers(Set.of(member), 10, 20, dar, 1);

    verify(dacAutomationRuleService).createVoteForElection(10, member.getUserId(), VoteType.DAC);
    verify(dacAutomationRuleService).createVoteForElection(20, member.getUserId(), VoteType.DAC);
    verifyNoMoreInteractions(dacAutomationRuleService);
  }

  @Test
  void testCreateVotesForAllUsers_ChairpersonNoManualReview() {
    int dacId = 1;
    User chair = createUserWithRoleAndDacId(UserRoles.CHAIRPERSON, dacId);
    DataAccessRequest dar = new DataAccessRequest();
    dar.setData(new DataAccessRequestData()); // all flags null → requiresManualReview() == false

    service.createVotesForAllUsers(Set.of(chair), 10, 20, dar, dacId);

    verify(dacAutomationRuleService).createVoteForElection(10, chair.getUserId(), VoteType.DAC);
    verify(dacAutomationRuleService).createVoteForElection(20, chair.getUserId(), VoteType.DAC);
    verify(dacAutomationRuleService)
        .createVoteForElection(10, chair.getUserId(), VoteType.CHAIRPERSON);
    verify(dacAutomationRuleService)
        .createVoteForElection(20, chair.getUserId(), VoteType.CHAIRPERSON);
    verify(dacAutomationRuleService).createVoteForElection(10, chair.getUserId(), VoteType.FINAL);
    verify(dacAutomationRuleService)
        .createVoteForElection(10, chair.getUserId(), VoteType.AGREEMENT);
    verifyNoMoreInteractions(dacAutomationRuleService);
  }

  @Test
  void testCreateVotesForAllUsers_ChairpersonRequiresManualReview() {
    int dacId = 1;
    User chair = createUserWithRoleAndDacId(UserRoles.CHAIRPERSON, dacId);
    DataAccessRequest dar = new DataAccessRequest();
    DataAccessRequestData data = new DataAccessRequestData();
    data.setPoa(true); // triggers requiresManualReview() == true
    dar.setData(data);

    service.createVotesForAllUsers(Set.of(chair), 10, 20, dar, dacId);

    verify(dacAutomationRuleService).createVoteForElection(10, chair.getUserId(), VoteType.DAC);
    verify(dacAutomationRuleService).createVoteForElection(20, chair.getUserId(), VoteType.DAC);
    verify(dacAutomationRuleService)
        .createVoteForElection(10, chair.getUserId(), VoteType.CHAIRPERSON);
    verify(dacAutomationRuleService)
        .createVoteForElection(20, chair.getUserId(), VoteType.CHAIRPERSON);
    verify(dacAutomationRuleService).createVoteForElection(10, chair.getUserId(), VoteType.FINAL);
    // Exactly 5 vote-creation calls: AGREEMENT is not created when requiresManualReview is true.
    verify(dacAutomationRuleService, times(5)).createVoteForElection(anyInt(), anyInt(), any());
  }

  @Test
  void testCreateVotesForAllUsers_MixedChairAndMember() {
    int dacId = 1;
    User chair = createUserWithRoleAndDacId(UserRoles.CHAIRPERSON, dacId);
    User member = createUserWithRole(UserRoles.MEMBER);
    DataAccessRequest dar = new DataAccessRequest();
    dar.setData(new DataAccessRequestData());

    service.createVotesForAllUsers(Set.of(chair, member), 10, 20, dar, dacId);

    // chair gets DAC + CHAIRPERSON + FINAL + AGREEMENT votes
    verify(dacAutomationRuleService).createVoteForElection(10, chair.getUserId(), VoteType.DAC);
    verify(dacAutomationRuleService).createVoteForElection(20, chair.getUserId(), VoteType.DAC);
    verify(dacAutomationRuleService)
        .createVoteForElection(10, chair.getUserId(), VoteType.CHAIRPERSON);
    verify(dacAutomationRuleService)
        .createVoteForElection(20, chair.getUserId(), VoteType.CHAIRPERSON);
    verify(dacAutomationRuleService).createVoteForElection(10, chair.getUserId(), VoteType.FINAL);
    verify(dacAutomationRuleService)
        .createVoteForElection(10, chair.getUserId(), VoteType.AGREEMENT);
    // member gets only DAC votes
    verify(dacAutomationRuleService).createVoteForElection(10, member.getUserId(), VoteType.DAC);
    verify(dacAutomationRuleService).createVoteForElection(20, member.getUserId(), VoteType.DAC);
    verifyNoMoreInteractions(dacAutomationRuleService);
  }

  @Test
  void testCreateVotesForAllUsers_ChairOfDifferentDacGetsNochairpersonVotes() {
    // A chair from DAC 2 should not receive chairperson votes when creating votes for DAC 1's
    // election
    int electionDacId = 1;
    User chairOfOtherDac = createUserWithRoleAndDacId(UserRoles.CHAIRPERSON, 2);
    DataAccessRequest dar = new DataAccessRequest();
    dar.setData(new DataAccessRequestData());

    service.createVotesForAllUsers(Set.of(chairOfOtherDac), 10, 20, dar, electionDacId);

    verify(dacAutomationRuleService)
        .createVoteForElection(10, chairOfOtherDac.getUserId(), VoteType.DAC);
    verify(dacAutomationRuleService)
        .createVoteForElection(20, chairOfOtherDac.getUserId(), VoteType.DAC);
    verifyNoMoreInteractions(dacAutomationRuleService);
  }

  @Test
  void testCreateElectionsAndVotesForAutoOpenDacs_MultipleDatasetsSameDac() {
    // Both datasets from the same DAC must each get their own elections
    DataAccessRequest dar = new DataAccessRequest();
    dar.setReferenceId(UUID.randomUUID().toString());
    dar.setData(new DataAccessRequestData());

    Dataset dataset1 = new Dataset();
    dataset1.setDatasetId(1);
    dataset1.setDacId(10);

    Dataset dataset2 = new Dataset();
    dataset2.setDatasetId(2);
    dataset2.setDacId(10);

    DacUserClassification classification = new DacUserClassification();
    classification.autoOpenDatasets.add(dataset1);
    classification.autoOpenDatasets.add(dataset2);

    when(electionDAO.findLastElectionByReferenceIdDatasetIdAndType(any(), anyInt(), any()))
        .thenReturn(null);
    when(electionDAO.findElectionsByReferenceIdAndDatasetId(any(), anyInt())).thenReturn(List.of());
    when(dacAutomationRuleService.createOpenElectionForDAR(
            any(), any(), eq(ElectionType.DATA_ACCESS)))
        .thenReturn(100);
    when(dacAutomationRuleService.createOpenElectionForDAR(any(), any(), eq(ElectionType.RP)))
        .thenReturn(200);
    stubInTransactionToExecute();

    service.createElectionsAndVotesForAutoOpenDacs(classification, dar);

    verify(dacAutomationRuleService)
        .createOpenElectionForDAR(dar, dataset1, ElectionType.DATA_ACCESS);
    verify(dacAutomationRuleService).createOpenElectionForDAR(dar, dataset1, ElectionType.RP);
    verify(dacAutomationRuleService)
        .createOpenElectionForDAR(dar, dataset2, ElectionType.DATA_ACCESS);
    verify(dacAutomationRuleService).createOpenElectionForDAR(dar, dataset2, ElectionType.RP);
  }

  // ─── Full-flow integration: userDAO → classifyDacsAndUsers → vote creation ──

  /**
   * Full flow — two auto-open DACs, distinct chairs and members.
   *
   * <p>Exercises the complete path from userDAO sourcing through classifyDacsAndUsers into vote
   * creation. Verifies that cross-DAC isolation holds end-to-end: each user receives votes only for
   * the election belonging to their own DAC.
   */
  @Test
  void testFullFlow_TwoAutoOpenDacs_DistinctChairsAndMembers_VotesCreatedPerDac() {
    User chairDac10 = createUserWithRoleAndDacId(UserRoles.CHAIRPERSON, 10);
    User memberDac10 = createUserWithRoleAndDacId(UserRoles.MEMBER, 10);
    User chairDac20 = createUserWithRoleAndDacId(UserRoles.CHAIRPERSON, 20);
    User memberDac20 = createUserWithRoleAndDacId(UserRoles.MEMBER, 20);

    DataAccessRequest dar = new DataAccessRequest();
    dar.setReferenceId(UUID.randomUUID().toString());
    dar.setSubmissionDate(Timestamp.from(Instant.now()));
    dar.setData(new DataAccessRequestData());
    dar.setDatasetIds(List.of(1, 2));

    DarCollection collection = new DarCollection();
    collection.setDarCollectionId(1);
    collection.setCreateUserId(99);
    collection.addDar(dar);

    Dataset dataset1 = new Dataset();
    dataset1.setDatasetId(1);
    dataset1.setDacId(10);

    Dataset dataset2 = new Dataset();
    dataset2.setDatasetId(2);
    dataset2.setDacId(20);

    Dac dac10 = new Dac();
    dac10.setDacId(10);
    Dac dac20 = new Dac();
    dac20.setDacId(20);

    DACAutomationRule autoOpenRule = mock(DACAutomationRule.class);
    when(autoOpenRule.ruleType()).thenReturn(DACAutomationRuleType.AUTO_OPEN_DAR_FOR_ALL_MEMBERS);
    when(autoOpenRule.enabledByUserId()).thenReturn(chairDac10.getUserId());

    when(darCollectionDAO.findDARCollectionByCollectionId(1)).thenReturn(collection);
    when(userDAO.findUserById(99)).thenReturn(new User());
    when(datasetDAO.findDatasetsByIdList(anyList())).thenReturn(List.of(dataset1, dataset2));
    when(dacDAO.findDacsForDatasetIds(anyList())).thenReturn(Set.of(dac10, dac20));
    when(userDAO.findUsersForDatasetsByRole(anyList(), anyList()))
        .thenReturn(Set.of(chairDac10, memberDac10, chairDac20, memberDac20));
    when(dacAutomationRuleService.findAllByDacId(anyInt())).thenReturn(List.of(autoOpenRule));
    when(electionDAO.findLastElectionByReferenceIdDatasetIdAndType(any(), anyInt(), any()))
        .thenReturn(null);
    when(electionDAO.findElectionsByReferenceIdAndDatasetId(any(), anyInt())).thenReturn(List.of());
    when(dacAutomationRuleService.createOpenElectionForDAR(
            any(), eq(dataset1), eq(ElectionType.DATA_ACCESS)))
        .thenReturn(100);
    when(dacAutomationRuleService.createOpenElectionForDAR(
            any(), eq(dataset1), eq(ElectionType.RP)))
        .thenReturn(200);
    when(dacAutomationRuleService.createOpenElectionForDAR(
            any(), eq(dataset2), eq(ElectionType.DATA_ACCESS)))
        .thenReturn(300);
    when(dacAutomationRuleService.createOpenElectionForDAR(
            any(), eq(dataset2), eq(ElectionType.RP)))
        .thenReturn(400);
    stubInTransactionToExecute();

    service.createElectionsForNewDarCollection(1);

    // DAC 10 election — only chairDac10 and memberDac10
    verify(dacAutomationRuleService)
        .createVoteForElection(100, chairDac10.getUserId(), VoteType.DAC);
    verify(dacAutomationRuleService)
        .createVoteForElection(200, chairDac10.getUserId(), VoteType.DAC);
    verify(dacAutomationRuleService)
        .createVoteForElection(100, chairDac10.getUserId(), VoteType.CHAIRPERSON);
    verify(dacAutomationRuleService)
        .createVoteForElection(100, chairDac10.getUserId(), VoteType.FINAL);
    verify(dacAutomationRuleService)
        .createVoteForElection(100, chairDac10.getUserId(), VoteType.AGREEMENT);
    verify(dacAutomationRuleService)
        .createVoteForElection(100, memberDac10.getUserId(), VoteType.DAC);
    verify(dacAutomationRuleService)
        .createVoteForElection(200, memberDac10.getUserId(), VoteType.DAC);
    // DAC 20 election — only chairDac20 and memberDac20
    verify(dacAutomationRuleService)
        .createVoteForElection(300, chairDac20.getUserId(), VoteType.DAC);
    verify(dacAutomationRuleService)
        .createVoteForElection(400, chairDac20.getUserId(), VoteType.DAC);
    verify(dacAutomationRuleService)
        .createVoteForElection(300, chairDac20.getUserId(), VoteType.CHAIRPERSON);
    verify(dacAutomationRuleService)
        .createVoteForElection(300, chairDac20.getUserId(), VoteType.FINAL);
    verify(dacAutomationRuleService)
        .createVoteForElection(300, chairDac20.getUserId(), VoteType.AGREEMENT);
    verify(dacAutomationRuleService)
        .createVoteForElection(300, memberDac20.getUserId(), VoteType.DAC);
    verify(dacAutomationRuleService)
        .createVoteForElection(400, memberDac20.getUserId(), VoteType.DAC);

    // Exactly 16 vote-creation calls: cross-DAC users received no votes in the wrong elections.
    verify(dacAutomationRuleService, times(16)).createVoteForElection(anyInt(), anyInt(), any());
  }

  /**
   * Full flow — one auto-open DAC, one manual DAC.
   *
   * <p>Verifies that classification correctly routes users from the two DACs into separate sets,
   * and that only the auto-open DAC's datasets receive elections and votes.
   */
  @Test
  void testFullFlow_MixedDacs_OnlyAutoOpenDacReceivesElectionsAndVotes() {
    User chairDac10 = createUserWithRoleAndDacId(UserRoles.CHAIRPERSON, 10);
    User memberDac10 = createUserWithRoleAndDacId(UserRoles.MEMBER, 10);
    User chairDac20 = createUserWithRoleAndDacId(UserRoles.CHAIRPERSON, 20);

    DataAccessRequest dar = new DataAccessRequest();
    dar.setReferenceId(UUID.randomUUID().toString());
    dar.setSubmissionDate(Timestamp.from(Instant.now()));
    dar.setData(new DataAccessRequestData());
    dar.setDatasetIds(List.of(1, 2));

    DarCollection collection = new DarCollection();
    collection.setDarCollectionId(1);
    collection.setCreateUserId(99);
    collection.addDar(dar);

    Dataset dataset1 = new Dataset();
    dataset1.setDatasetId(1);
    dataset1.setDacId(10);

    Dataset dataset2 = new Dataset();
    dataset2.setDatasetId(2);
    dataset2.setDacId(20);

    Dac dac10 = new Dac();
    dac10.setDacId(10);
    Dac dac20 = new Dac();
    dac20.setDacId(20);

    DACAutomationRule autoOpenRule = mock(DACAutomationRule.class);
    when(autoOpenRule.ruleType()).thenReturn(DACAutomationRuleType.AUTO_OPEN_DAR_FOR_ALL_MEMBERS);
    when(autoOpenRule.enabledByUserId()).thenReturn(chairDac10.getUserId());

    when(darCollectionDAO.findDARCollectionByCollectionId(1)).thenReturn(collection);
    when(userDAO.findUserById(99)).thenReturn(new User());
    when(datasetDAO.findDatasetsByIdList(anyList())).thenReturn(List.of(dataset1, dataset2));
    when(dacDAO.findDacsForDatasetIds(anyList())).thenReturn(Set.of(dac10, dac20));
    when(userDAO.findUsersForDatasetsByRole(anyList(), anyList()))
        .thenReturn(Set.of(chairDac10, memberDac10, chairDac20));
    when(dacAutomationRuleService.findAllByDacId(10)).thenReturn(List.of(autoOpenRule));
    when(dacAutomationRuleService.findAllByDacId(20)).thenReturn(List.of());
    when(electionDAO.findLastElectionByReferenceIdDatasetIdAndType(any(), anyInt(), any()))
        .thenReturn(null);
    when(electionDAO.findElectionsByReferenceIdAndDatasetId(any(), anyInt())).thenReturn(List.of());
    when(dacAutomationRuleService.createOpenElectionForDAR(
            any(), eq(dataset1), eq(ElectionType.DATA_ACCESS)))
        .thenReturn(100);
    when(dacAutomationRuleService.createOpenElectionForDAR(
            any(), eq(dataset1), eq(ElectionType.RP)))
        .thenReturn(200);
    stubInTransactionToExecute();

    service.createElectionsForNewDarCollection(1);

    // DAC 10 auto-open → elections and votes created
    verify(dacAutomationRuleService)
        .createOpenElectionForDAR(dar, dataset1, ElectionType.DATA_ACCESS);
    verify(dacAutomationRuleService)
        .createVoteForElection(100, chairDac10.getUserId(), VoteType.DAC);
    verify(dacAutomationRuleService)
        .createVoteForElection(100, chairDac10.getUserId(), VoteType.CHAIRPERSON);
    verify(dacAutomationRuleService)
        .createVoteForElection(100, memberDac10.getUserId(), VoteType.DAC);

    // DAC 20 manual → no election created, no votes for chairDac20
    verify(dacAutomationRuleService, never()).createOpenElectionForDAR(any(), eq(dataset2), any());
    verify(dacAutomationRuleService, never())
        .createVoteForElection(anyInt(), eq(chairDac20.getUserId()), any());
  }

  /**
   * Full flow — user chairs both auto-open DACs.
   *
   * <p>Verifies that a user deduped in the flat autoOpenUsers set (Set semantics via addUsers)
   * still receives full chairperson votes for both elections because the per-DAC filter in
   * createElectionsAndVotesForAutoOpenDacs matches them for each DAC independently.
   */
  @Test
  void testFullFlow_UserChairsBothAutoOpenDacs_GetsFullVotesInBothElections() {
    // sharedChair has CHAIRPERSON role for both DAC 10 and DAC 20
    User sharedChair = new User();
    sharedChair.setUserId(randomInt(1, 100000));
    sharedChair.setEmailPreference(true);
    UserRole roleForDac10 =
        new UserRole(UserRoles.CHAIRPERSON.getRoleId(), UserRoles.CHAIRPERSON.getRoleName());
    roleForDac10.setDacId(10);
    UserRole roleForDac20 =
        new UserRole(UserRoles.CHAIRPERSON.getRoleId(), UserRoles.CHAIRPERSON.getRoleName());
    roleForDac20.setDacId(20);
    sharedChair.setRoles(List.of(roleForDac10, roleForDac20));

    DataAccessRequest dar = new DataAccessRequest();
    dar.setReferenceId(UUID.randomUUID().toString());
    dar.setSubmissionDate(Timestamp.from(Instant.now()));
    dar.setData(new DataAccessRequestData());
    dar.setDatasetIds(List.of(1, 2));

    DarCollection collection = new DarCollection();
    collection.setDarCollectionId(1);
    collection.setCreateUserId(99);
    collection.addDar(dar);

    Dataset dataset1 = new Dataset();
    dataset1.setDatasetId(1);
    dataset1.setDacId(10);

    Dataset dataset2 = new Dataset();
    dataset2.setDatasetId(2);
    dataset2.setDacId(20);

    Dac dac10 = new Dac();
    dac10.setDacId(10);
    Dac dac20 = new Dac();
    dac20.setDacId(20);

    DACAutomationRule autoOpenRule = mock(DACAutomationRule.class);
    when(autoOpenRule.ruleType()).thenReturn(DACAutomationRuleType.AUTO_OPEN_DAR_FOR_ALL_MEMBERS);
    when(autoOpenRule.enabledByUserId()).thenReturn(sharedChair.getUserId());

    when(darCollectionDAO.findDARCollectionByCollectionId(1)).thenReturn(collection);
    when(userDAO.findUserById(99)).thenReturn(new User());
    when(datasetDAO.findDatasetsByIdList(anyList())).thenReturn(List.of(dataset1, dataset2));
    when(dacDAO.findDacsForDatasetIds(anyList())).thenReturn(Set.of(dac10, dac20));
    when(userDAO.findUsersForDatasetsByRole(anyList(), anyList())).thenReturn(Set.of(sharedChair));
    when(dacAutomationRuleService.findAllByDacId(anyInt())).thenReturn(List.of(autoOpenRule));
    when(electionDAO.findLastElectionByReferenceIdDatasetIdAndType(any(), anyInt(), any()))
        .thenReturn(null);
    when(electionDAO.findElectionsByReferenceIdAndDatasetId(any(), anyInt())).thenReturn(List.of());
    when(dacAutomationRuleService.createOpenElectionForDAR(
            any(), eq(dataset1), eq(ElectionType.DATA_ACCESS)))
        .thenReturn(100);
    when(dacAutomationRuleService.createOpenElectionForDAR(
            any(), eq(dataset1), eq(ElectionType.RP)))
        .thenReturn(200);
    when(dacAutomationRuleService.createOpenElectionForDAR(
            any(), eq(dataset2), eq(ElectionType.DATA_ACCESS)))
        .thenReturn(300);
    when(dacAutomationRuleService.createOpenElectionForDAR(
            any(), eq(dataset2), eq(ElectionType.RP)))
        .thenReturn(400);
    stubInTransactionToExecute();

    service.createElectionsForNewDarCollection(1);

    // Full chair votes in both DAC 10's and DAC 20's elections
    verify(dacAutomationRuleService)
        .createVoteForElection(100, sharedChair.getUserId(), VoteType.DAC);
    verify(dacAutomationRuleService)
        .createVoteForElection(100, sharedChair.getUserId(), VoteType.CHAIRPERSON);
    verify(dacAutomationRuleService)
        .createVoteForElection(100, sharedChair.getUserId(), VoteType.FINAL);
    verify(dacAutomationRuleService)
        .createVoteForElection(100, sharedChair.getUserId(), VoteType.AGREEMENT);
    verify(dacAutomationRuleService)
        .createVoteForElection(300, sharedChair.getUserId(), VoteType.DAC);
    verify(dacAutomationRuleService)
        .createVoteForElection(300, sharedChair.getUserId(), VoteType.CHAIRPERSON);
    verify(dacAutomationRuleService)
        .createVoteForElection(300, sharedChair.getUserId(), VoteType.FINAL);
    verify(dacAutomationRuleService)
        .createVoteForElection(300, sharedChair.getUserId(), VoteType.AGREEMENT);
  }

  // ─── Multi-DAC / autoOpen on+off comprehensive vote coverage ──────────────

  /**
   * autoOpen ON, two datasets in different DACs, chairs and members are distinct per DAC.
   *
   * <p>Verifies that for each dataset's elections:
   *
   * <ul>
   *   <li>The chair of that specific DAC gets DAC + CHAIRPERSON + FINAL + AGREEMENT votes
   *   <li>The member of that specific DAC gets only DAC votes
   *   <li>Users from the OTHER DAC get only standard DAC votes (no CHAIRPERSON/FINAL/AGREEMENT)
   * </ul>
   */
  @Test
  void testElectionVotes_AutoOpenOn_TwoDacs_DistinctChairsAndMembers() {
    DataAccessRequest dar = new DataAccessRequest();
    dar.setReferenceId(UUID.randomUUID().toString());
    dar.setData(new DataAccessRequestData()); // requiresManualReview() == false

    Dataset dataset1 = new Dataset();
    dataset1.setDatasetId(1);
    dataset1.setDacId(10);

    Dataset dataset2 = new Dataset();
    dataset2.setDatasetId(2);
    dataset2.setDacId(20);

    User chairDac10 = createUserWithRoleAndDacId(UserRoles.CHAIRPERSON, 10);
    User memberDac10 = createUserWithRoleAndDacId(UserRoles.MEMBER, 10);
    User chairDac20 = createUserWithRoleAndDacId(UserRoles.CHAIRPERSON, 20);
    User memberDac20 = createUserWithRoleAndDacId(UserRoles.MEMBER, 20);

    DacUserClassification classification = new DacUserClassification();
    classification.autoOpenDatasets.add(dataset1);
    classification.autoOpenDatasets.add(dataset2);
    classification.autoOpenUsers.addAll(Set.of(chairDac10, memberDac10, chairDac20, memberDac20));

    when(electionDAO.findLastElectionByReferenceIdDatasetIdAndType(any(), anyInt(), any()))
        .thenReturn(null);
    when(electionDAO.findElectionsByReferenceIdAndDatasetId(any(), anyInt())).thenReturn(List.of());
    // dataset1: DATA_ACCESS=100, RP=200; dataset2: DATA_ACCESS=300, RP=400
    when(dacAutomationRuleService.createOpenElectionForDAR(
            any(), eq(dataset1), eq(ElectionType.DATA_ACCESS)))
        .thenReturn(100);
    when(dacAutomationRuleService.createOpenElectionForDAR(
            any(), eq(dataset1), eq(ElectionType.RP)))
        .thenReturn(200);
    when(dacAutomationRuleService.createOpenElectionForDAR(
            any(), eq(dataset2), eq(ElectionType.DATA_ACCESS)))
        .thenReturn(300);
    when(dacAutomationRuleService.createOpenElectionForDAR(
            any(), eq(dataset2), eq(ElectionType.RP)))
        .thenReturn(400);
    stubInTransactionToExecute();

    service.createElectionsAndVotesForAutoOpenDacs(classification, dar);

    // ── dataset1 (DAC 10): chairDac10 gets full chair votes; memberDac10 gets DAC votes only ──
    verify(dacAutomationRuleService)
        .createVoteForElection(100, chairDac10.getUserId(), VoteType.DAC);
    verify(dacAutomationRuleService)
        .createVoteForElection(200, chairDac10.getUserId(), VoteType.DAC);
    verify(dacAutomationRuleService)
        .createVoteForElection(100, chairDac10.getUserId(), VoteType.CHAIRPERSON);
    verify(dacAutomationRuleService)
        .createVoteForElection(200, chairDac10.getUserId(), VoteType.CHAIRPERSON);
    verify(dacAutomationRuleService)
        .createVoteForElection(100, chairDac10.getUserId(), VoteType.FINAL);
    verify(dacAutomationRuleService)
        .createVoteForElection(100, chairDac10.getUserId(), VoteType.AGREEMENT);
    verify(dacAutomationRuleService)
        .createVoteForElection(100, memberDac10.getUserId(), VoteType.DAC);
    verify(dacAutomationRuleService)
        .createVoteForElection(200, memberDac10.getUserId(), VoteType.DAC);

    // ── dataset2 (DAC 20): chairDac20 gets full chair votes; memberDac20 gets DAC votes only ──
    verify(dacAutomationRuleService)
        .createVoteForElection(300, chairDac20.getUserId(), VoteType.DAC);
    verify(dacAutomationRuleService)
        .createVoteForElection(400, chairDac20.getUserId(), VoteType.DAC);
    verify(dacAutomationRuleService)
        .createVoteForElection(300, chairDac20.getUserId(), VoteType.CHAIRPERSON);
    verify(dacAutomationRuleService)
        .createVoteForElection(400, chairDac20.getUserId(), VoteType.CHAIRPERSON);
    verify(dacAutomationRuleService)
        .createVoteForElection(300, chairDac20.getUserId(), VoteType.FINAL);
    verify(dacAutomationRuleService)
        .createVoteForElection(300, chairDac20.getUserId(), VoteType.AGREEMENT);
    verify(dacAutomationRuleService)
        .createVoteForElection(300, memberDac20.getUserId(), VoteType.DAC);
    verify(dacAutomationRuleService)
        .createVoteForElection(400, memberDac20.getUserId(), VoteType.DAC);

    // Exactly 16 vote-creation calls: cross-DAC users and member CHAIRPERSON votes are excluded.
    // Any cross-DAC contamination or spurious role promotion would push this count above 16.
    verify(dacAutomationRuleService, times(16)).createVoteForElection(anyInt(), anyInt(), any());
  }

  /**
   * autoOpen ON, two datasets in different DACs, one user chairs both DACs.
   *
   * <p>Verifies that the shared chair receives full CHAIRPERSON + FINAL + AGREEMENT votes for both
   * DAC 10's and DAC 20's elections (because they have CHAIRPERSON role for each).
   */
  @Test
  void testElectionVotes_AutoOpenOn_TwoDacs_SameUserChairsBothDacs() {
    DataAccessRequest dar = new DataAccessRequest();
    dar.setReferenceId(UUID.randomUUID().toString());
    dar.setData(new DataAccessRequestData());

    Dataset dataset1 = new Dataset();
    dataset1.setDatasetId(1);
    dataset1.setDacId(10);

    Dataset dataset2 = new Dataset();
    dataset2.setDatasetId(2);
    dataset2.setDacId(20);

    // sharedChair has CHAIRPERSON role for both DAC 10 and DAC 20
    User sharedChair = new User();
    sharedChair.setUserId(randomInt(1, 100000));
    sharedChair.setEmailPreference(Boolean.TRUE);
    UserRole roleForDac10 =
        new UserRole(UserRoles.CHAIRPERSON.getRoleId(), UserRoles.CHAIRPERSON.getRoleName());
    roleForDac10.setDacId(10);
    UserRole roleForDac20 =
        new UserRole(UserRoles.CHAIRPERSON.getRoleId(), UserRoles.CHAIRPERSON.getRoleName());
    roleForDac20.setDacId(20);
    sharedChair.setRoles(List.of(roleForDac10, roleForDac20));

    DacUserClassification classification = new DacUserClassification();
    classification.autoOpenDatasets.add(dataset1);
    classification.autoOpenDatasets.add(dataset2);
    classification.autoOpenUsers.add(sharedChair);

    when(electionDAO.findLastElectionByReferenceIdDatasetIdAndType(any(), anyInt(), any()))
        .thenReturn(null);
    when(electionDAO.findElectionsByReferenceIdAndDatasetId(any(), anyInt())).thenReturn(List.of());
    when(dacAutomationRuleService.createOpenElectionForDAR(
            any(), eq(dataset1), eq(ElectionType.DATA_ACCESS)))
        .thenReturn(100);
    when(dacAutomationRuleService.createOpenElectionForDAR(
            any(), eq(dataset1), eq(ElectionType.RP)))
        .thenReturn(200);
    when(dacAutomationRuleService.createOpenElectionForDAR(
            any(), eq(dataset2), eq(ElectionType.DATA_ACCESS)))
        .thenReturn(300);
    when(dacAutomationRuleService.createOpenElectionForDAR(
            any(), eq(dataset2), eq(ElectionType.RP)))
        .thenReturn(400);
    stubInTransactionToExecute();

    service.createElectionsAndVotesForAutoOpenDacs(classification, dar);

    // sharedChair gets full chair votes for dataset1 (DAC 10)
    verify(dacAutomationRuleService)
        .createVoteForElection(100, sharedChair.getUserId(), VoteType.DAC);
    verify(dacAutomationRuleService)
        .createVoteForElection(200, sharedChair.getUserId(), VoteType.DAC);
    verify(dacAutomationRuleService)
        .createVoteForElection(100, sharedChair.getUserId(), VoteType.CHAIRPERSON);
    verify(dacAutomationRuleService)
        .createVoteForElection(200, sharedChair.getUserId(), VoteType.CHAIRPERSON);
    verify(dacAutomationRuleService)
        .createVoteForElection(100, sharedChair.getUserId(), VoteType.FINAL);
    verify(dacAutomationRuleService)
        .createVoteForElection(100, sharedChair.getUserId(), VoteType.AGREEMENT);
    // sharedChair also gets full chair votes for dataset2 (DAC 20)
    verify(dacAutomationRuleService)
        .createVoteForElection(300, sharedChair.getUserId(), VoteType.DAC);
    verify(dacAutomationRuleService)
        .createVoteForElection(400, sharedChair.getUserId(), VoteType.DAC);
    verify(dacAutomationRuleService)
        .createVoteForElection(300, sharedChair.getUserId(), VoteType.CHAIRPERSON);
    verify(dacAutomationRuleService)
        .createVoteForElection(400, sharedChair.getUserId(), VoteType.CHAIRPERSON);
    verify(dacAutomationRuleService)
        .createVoteForElection(300, sharedChair.getUserId(), VoteType.FINAL);
    verify(dacAutomationRuleService)
        .createVoteForElection(300, sharedChair.getUserId(), VoteType.AGREEMENT);
  }

  /**
   * autoOpen ON, two datasets in different DACs, same user is chair of DAC 10 and member of DAC 20.
   *
   * <p>Verifies that the shared user gets:
   *
   * <ul>
   *   <li>Full CHAIRPERSON + FINAL + AGREEMENT votes for DAC 10's elections
   *   <li>Only standard DAC votes for DAC 20's elections (member role, not chair)
   * </ul>
   */
  @Test
  void testElectionVotes_AutoOpenOn_TwoDacs_SameUserChairOfOneAndMemberOfOther() {
    DataAccessRequest dar = new DataAccessRequest();
    dar.setReferenceId(UUID.randomUUID().toString());
    dar.setData(new DataAccessRequestData());

    Dataset dataset1 = new Dataset();
    dataset1.setDatasetId(1);
    dataset1.setDacId(10);

    Dataset dataset2 = new Dataset();
    dataset2.setDatasetId(2);
    dataset2.setDacId(20);

    // sharedUser: CHAIRPERSON of DAC 10, MEMBER of DAC 20
    User sharedUser = new User();
    sharedUser.setUserId(randomInt(1, 100000));
    sharedUser.setEmailPreference(Boolean.TRUE);
    UserRole chairRoleDac10 =
        new UserRole(UserRoles.CHAIRPERSON.getRoleId(), UserRoles.CHAIRPERSON.getRoleName());
    chairRoleDac10.setDacId(10);
    UserRole memberRoleDac20 =
        new UserRole(UserRoles.MEMBER.getRoleId(), UserRoles.MEMBER.getRoleName());
    memberRoleDac20.setDacId(20);
    sharedUser.setRoles(List.of(chairRoleDac10, memberRoleDac20));

    // separateChair is the chair of DAC 20
    User separateChairDac20 = createUserWithRoleAndDacId(UserRoles.CHAIRPERSON, 20);

    DacUserClassification classification = new DacUserClassification();
    classification.autoOpenDatasets.add(dataset1);
    classification.autoOpenDatasets.add(dataset2);
    classification.autoOpenUsers.add(sharedUser);
    classification.autoOpenUsers.add(separateChairDac20);

    when(electionDAO.findLastElectionByReferenceIdDatasetIdAndType(any(), anyInt(), any()))
        .thenReturn(null);
    when(electionDAO.findElectionsByReferenceIdAndDatasetId(any(), anyInt())).thenReturn(List.of());
    when(dacAutomationRuleService.createOpenElectionForDAR(
            any(), eq(dataset1), eq(ElectionType.DATA_ACCESS)))
        .thenReturn(100);
    when(dacAutomationRuleService.createOpenElectionForDAR(
            any(), eq(dataset1), eq(ElectionType.RP)))
        .thenReturn(200);
    when(dacAutomationRuleService.createOpenElectionForDAR(
            any(), eq(dataset2), eq(ElectionType.DATA_ACCESS)))
        .thenReturn(300);
    when(dacAutomationRuleService.createOpenElectionForDAR(
            any(), eq(dataset2), eq(ElectionType.RP)))
        .thenReturn(400);
    stubInTransactionToExecute();

    service.createElectionsAndVotesForAutoOpenDacs(classification, dar);

    // ── dataset1 (DAC 10) ─────────────────────────────────────────────────────
    // sharedUser is chair of DAC 10 → full chair votes
    verify(dacAutomationRuleService)
        .createVoteForElection(100, sharedUser.getUserId(), VoteType.DAC);
    verify(dacAutomationRuleService)
        .createVoteForElection(200, sharedUser.getUserId(), VoteType.DAC);
    verify(dacAutomationRuleService)
        .createVoteForElection(100, sharedUser.getUserId(), VoteType.CHAIRPERSON);
    verify(dacAutomationRuleService)
        .createVoteForElection(200, sharedUser.getUserId(), VoteType.CHAIRPERSON);
    verify(dacAutomationRuleService)
        .createVoteForElection(100, sharedUser.getUserId(), VoteType.FINAL);
    verify(dacAutomationRuleService)
        .createVoteForElection(100, sharedUser.getUserId(), VoteType.AGREEMENT);
    // ── dataset2 (DAC 20) ─────────────────────────────────────────────────────
    // sharedUser is member of DAC 20, not chair → only DAC votes
    verify(dacAutomationRuleService)
        .createVoteForElection(300, sharedUser.getUserId(), VoteType.DAC);
    verify(dacAutomationRuleService)
        .createVoteForElection(400, sharedUser.getUserId(), VoteType.DAC);
    // separateChairDac20 is chair of DAC 20 → full chair votes
    verify(dacAutomationRuleService)
        .createVoteForElection(300, separateChairDac20.getUserId(), VoteType.DAC);
    verify(dacAutomationRuleService)
        .createVoteForElection(400, separateChairDac20.getUserId(), VoteType.DAC);
    verify(dacAutomationRuleService)
        .createVoteForElection(300, separateChairDac20.getUserId(), VoteType.CHAIRPERSON);
    verify(dacAutomationRuleService)
        .createVoteForElection(400, separateChairDac20.getUserId(), VoteType.CHAIRPERSON);
    verify(dacAutomationRuleService)
        .createVoteForElection(300, separateChairDac20.getUserId(), VoteType.FINAL);
    verify(dacAutomationRuleService)
        .createVoteForElection(300, separateChairDac20.getUserId(), VoteType.AGREEMENT);

    // Exactly 14 vote-creation calls: cross-DAC isolation and the sharedUser's member (not chair)
    // role in DAC 20 are both enforced — any leakage or spurious promotion pushes the count above
    // 14.
    verify(dacAutomationRuleService, times(14)).createVoteForElection(anyInt(), anyInt(), any());
  }

  /**
   * autoOpen OFF, two datasets in different DACs with distinct chairs.
   *
   * <p>Verifies that when no DAC has an auto-open rule, no elections and no votes are created via
   * the automation path.
   */
  @Test
  void testElectionVotes_AutoOpenOff_TwoDacs_NoElectionsOrVotesCreated() {
    DarCollection collection = new DarCollection();
    collection.setDarCollectionId(1);

    Dataset dataset1 = new Dataset();
    dataset1.setDatasetId(1);
    dataset1.setDacId(10);

    Dataset dataset2 = new Dataset();
    dataset2.setDatasetId(2);
    dataset2.setDacId(20);

    DataAccessRequest dar = new DataAccessRequest();
    dar.setReferenceId(UUID.randomUUID().toString());
    dar.setDatasetIds(List.of(dataset1.getDatasetId(), dataset2.getDatasetId()));
    collection.addDar(dar);

    Dac dac10 = new Dac();
    dac10.setDacId(10);
    dac10.setName("DAC-10");

    Dac dac20 = new Dac();
    dac20.setDacId(20);
    dac20.setName("DAC-20");

    User chairDac10 = new User();
    chairDac10.setUserId(randomInt(1, 100000));
    UserRole chair10Role =
        new UserRole(UserRoles.CHAIRPERSON.getRoleId(), UserRoles.CHAIRPERSON.getRoleName());
    chair10Role.setDacId(10);
    chairDac10.setRoles(List.of(chair10Role));

    User chairDac20 = new User();
    chairDac20.setUserId(randomInt(1, 100000));
    UserRole chair20Role =
        new UserRole(UserRoles.CHAIRPERSON.getRoleId(), UserRoles.CHAIRPERSON.getRoleName());
    chair20Role.setDacId(20);
    chairDac20.setRoles(List.of(chair20Role));

    when(darCollectionDAO.findDARCollectionByCollectionId(1)).thenReturn(collection);
    when(datasetDAO.findDatasetsByIdList(anyList())).thenReturn(List.of(dataset1, dataset2));
    when(dacDAO.findDacsForDatasetIds(anyList())).thenReturn(Set.of(dac10, dac20));
    // No auto-open rule for either DAC
    when(dacAutomationRuleService.findAllByDacId(anyInt())).thenReturn(List.of());
    when(userDAO.findUsersForDatasetsByRole(anyList(), anyList()))
        .thenReturn(Set.of(chairDac10, chairDac20));
    when(userDAO.findUserById(any())).thenReturn(new User());

    service.createElectionsForNewDarCollection(1);

    // No elections or votes created when auto-open is off
    verify(dacAutomationRuleService, never()).createOpenElectionForDAR(any(), any(), any());
    verify(dacAutomationRuleService, never()).createVoteForElection(anyInt(), anyInt(), any());
  }

  @Test
  void testFindDatasetIdsByDACUser() {
    User user = new User();
    user.setUserId(42);
    List<Integer> expectedIds = List.of(1, 2, 3);
    when(datasetDAO.findDatasetIdsByDACUserId(user.getUserId())).thenReturn(expectedIds);

    List<Integer> result = service.findDatasetIdsByDACUser(user);

    assertEquals(expectedIds, result);
    verify(datasetDAO).findDatasetIdsByDACUserId(user.getUserId());
  }

  @Test
  void testFindDatasetIdsByDACUser_Empty() {
    User user = new User();
    user.setUserId(99);
    when(datasetDAO.findDatasetIdsByDACUserId(user.getUserId())).thenReturn(List.of());

    List<Integer> result = service.findDatasetIdsByDACUser(user);

    assertTrue(result.isEmpty());
  }

  @Test
  void testGetByReferenceId() {
    User user = mock(User.class);
    String referenceId = UUID.randomUUID().toString();
    DarCollection collection = new DarCollection();
    collection.setDarCollectionId(10);
    when(darCollectionDAO.findDARCollectionByReferenceId(referenceId)).thenReturn(collection);

    DarCollection result = service.getByReferenceId(user, referenceId);

    assertNotNull(result);
    assertEquals(10, result.getDarCollectionId());
  }

  @Test
  void testGetByReferenceId_NotFound() {
    User user = mock(User.class);
    String referenceId = UUID.randomUUID().toString();
    when(darCollectionDAO.findDARCollectionByReferenceId(referenceId)).thenReturn(null);

    assertThrows(NotFoundException.class, () -> service.getByReferenceId(user, referenceId));
  }

  @Test
  void testGetByReferenceId_ServiceException() {
    User user = mock(User.class);
    String referenceId = UUID.randomUUID().toString();
    RuntimeException expected = new RuntimeException("DB error");
    when(darCollectionDAO.findDARCollectionByReferenceId(referenceId)).thenThrow(expected);

    RuntimeException thrown =
        assertThrows(RuntimeException.class, () -> service.getByReferenceId(user, referenceId));
    assertEquals(expected, thrown);
  }

  @Test
  void testCancelDarCollectionAsResearcher_NotCollectionOwner() {
    DataAccessRequest dar = new DataAccessRequest();
    dar.setReferenceId(UUID.randomUUID().toString());
    dar.setData(new DataAccessRequestData());
    DarCollection collection = createMockCollections().getFirst();
    collection.addDar(dar);
    collection.setCreateUserId(999);

    User user = new User();
    user.setUserId(1);

    assertThrows(
        NotFoundException.class,
        () -> service.cancelDarCollectionByRole(user, collection, UserRoles.RESEARCHER));
    verifyNoInteractions(electionDAO);
    verifyNoInteractions(dataAccessRequestDAO);
  }

  @Test
  void testAddDatasetsToCollection_NoDars() {
    DarCollection collection = new DarCollection();

    DarCollection result = service.addDatasetsToCollection(collection);

    assertNotNull(result);
    verifyNoInteractions(dataAccessRequestDAO);
    verifyNoInteractions(datasetDAO);
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

  /**
   * Regression test: dacToDatasetsMap must be scoped per-user in the manual notification path.
   *
   * <p>With two chairs from different DACs, each user's NewDARRequestMessage must contain only the
   * DAC/dataset entries for their own DAC — not a union of all prior users' entries.
   */
  @Test
  void testNotifyUsersForDacs_ManualPath_EachUserReceivesOnlyTheirOwnDacDatasets()
      throws Exception {
    DataAccessRequest dar = new DataAccessRequest();
    dar.setReferenceId(UUID.randomUUID().toString());
    DataAccessRequestData darData = new DataAccessRequestData();
    dar.setData(darData);

    DarCollection collection = new DarCollection();
    collection.setDarCollectionId(1);
    collection.setDarCode("DAR-TEST");

    Dac dac10 = new Dac();
    dac10.setDacId(10);
    dac10.setName("DAC-10");

    Dac dac20 = new Dac();
    dac20.setDacId(20);
    dac20.setName("DAC-20");

    Dataset dataset1 = new Dataset();
    dataset1.setDatasetId(1);
    dataset1.setDacId(10);
    dataset1.setAlias(1);
    dataset1.setDatasetIdentifier();

    Dataset dataset2 = new Dataset();
    dataset2.setDatasetId(2);
    dataset2.setDacId(20);
    dataset2.setAlias(2);
    dataset2.setDatasetIdentifier();

    User chairDac10 = createUserWithRoleAndDacId(UserRoles.CHAIRPERSON, 10);
    User chairDac20 = createUserWithRoleAndDacId(UserRoles.CHAIRPERSON, 20);

    service.notifyUsersForDacs(
        Set.of(chairDac10, chairDac20),
        Set.of(dac10, dac20),
        Set.of(dataset1, dataset2),
        dar,
        collection,
        "Researcher Name",
        false);

    ArgumentCaptor<NewDARRequestMessage> captor =
        ArgumentCaptor.forClass(NewDARRequestMessage.class);
    verify(emailService, times(2)).sendMessage(captor.capture(), any());

    List<NewDARRequestMessage> messages = captor.getAllValues();
    NewDARRequestMessage msgForChair10 =
        messages.stream()
            .filter(m -> m.toUser.getUserId().equals(chairDac10.getUserId()))
            .findFirst()
            .orElseThrow();
    NewDARRequestMessage msgForChair20 =
        messages.stream()
            .filter(m -> m.toUser.getUserId().equals(chairDac20.getUserId()))
            .findFirst()
            .orElseThrow();

    @SuppressWarnings("unchecked")
    Map<String, List<String>> chair10Map =
        (Map<String, List<String>>) msgForChair10.createModel().get("dacDatasetGroups");
    @SuppressWarnings("unchecked")
    Map<String, List<String>> chair20Map =
        (Map<String, List<String>>) msgForChair20.createModel().get("dacDatasetGroups");

    String ds1Identifier = Dataset.parseAliasToIdentifier(1);
    String ds2Identifier = Dataset.parseAliasToIdentifier(2);

    // chairDac10 must see only DAC-10's dataset
    assertTrue(chair10Map.containsKey("DAC-10"), "chair10 map should contain DAC-10");
    assertFalse(chair10Map.containsKey("DAC-20"), "chair10 map must not contain DAC-20");
    assertEquals(List.of(ds1Identifier), chair10Map.get("DAC-10"));

    // chairDac20 must see only DAC-20's dataset
    assertTrue(chair20Map.containsKey("DAC-20"), "chair20 map should contain DAC-20");
    assertFalse(chair20Map.containsKey("DAC-10"), "chair20 map must not contain DAC-10");
    assertEquals(List.of(ds2Identifier), chair20Map.get("DAC-20"));
  }

  /** Manual progress-report path — map isolation holds for NewProgressReportRequestMessage. */
  @Test
  void testNotifyUsersForDacs_ManualProgressReport_EachUserReceivesOnlyTheirOwnDacDatasets()
      throws Exception {
    DataAccessRequest dar = new DataAccessRequest();
    dar.setReferenceId(UUID.randomUUID().toString());
    dar.progressReport = true;
    dar.setData(new DataAccessRequestData());

    DarCollection collection = new DarCollection();
    collection.setDarCode("DAR-PR");

    Dac dac10 = new Dac();
    dac10.setDacId(10);
    dac10.setName("DAC-10");

    Dac dac20 = new Dac();
    dac20.setDacId(20);
    dac20.setName("DAC-20");

    Dataset dataset1 = new Dataset();
    dataset1.setDatasetId(1);
    dataset1.setDacId(10);
    dataset1.setAlias(10);
    dataset1.setDatasetIdentifier();

    Dataset dataset2 = new Dataset();
    dataset2.setDatasetId(2);
    dataset2.setDacId(20);
    dataset2.setAlias(20);
    dataset2.setDatasetIdentifier();

    User chairDac10 = createUserWithRoleAndDacId(UserRoles.CHAIRPERSON, 10);
    User chairDac20 = createUserWithRoleAndDacId(UserRoles.CHAIRPERSON, 20);

    service.notifyUsersForDacs(
        Set.of(chairDac10, chairDac20),
        Set.of(dac10, dac20),
        Set.of(dataset1, dataset2),
        dar,
        collection,
        "Researcher Name",
        false);

    ArgumentCaptor<NewProgressReportRequestMessage> captor =
        ArgumentCaptor.forClass(NewProgressReportRequestMessage.class);
    verify(emailService, times(2)).sendMessage(captor.capture(), any());

    List<NewProgressReportRequestMessage> messages = captor.getAllValues();
    NewProgressReportRequestMessage msgForChair10 =
        messages.stream()
            .filter(m -> m.toUser.getUserId().equals(chairDac10.getUserId()))
            .findFirst()
            .orElseThrow();
    NewProgressReportRequestMessage msgForChair20 =
        messages.stream()
            .filter(m -> m.toUser.getUserId().equals(chairDac20.getUserId()))
            .findFirst()
            .orElseThrow();

    @SuppressWarnings("unchecked")
    Map<String, List<String>> chair10Map =
        (Map<String, List<String>>) msgForChair10.createModel().get("dacDatasetGroups");
    @SuppressWarnings("unchecked")
    Map<String, List<String>> chair20Map =
        (Map<String, List<String>>) msgForChair20.createModel().get("dacDatasetGroups");

    assertTrue(chair10Map.containsKey("DAC-10"));
    assertFalse(chair10Map.containsKey("DAC-20"), "chair10 must not see DAC-20's datasets");
    assertTrue(chair20Map.containsKey("DAC-20"));
    assertFalse(chair20Map.containsKey("DAC-10"), "chair20 must not see DAC-10's datasets");
  }

  /** Auto-open, non-progress-report: each user receives a NewCaseMessage, not a DAR request. */
  @Test
  void testNotifyUsersForDacs_AutoOpen_NonProgressReport_SendsNewCaseMessagePerUser()
      throws Exception {
    DataAccessRequest dar = new DataAccessRequest();
    dar.setReferenceId(UUID.randomUUID().toString());
    dar.setData(new DataAccessRequestData());

    DarCollection collection = new DarCollection();
    collection.setDarCode("DAR-AUTO");

    User chair1 = createUserWithRoleAndDacId(UserRoles.CHAIRPERSON, 10);
    User member1 = createUserWithRoleAndDacId(UserRoles.MEMBER, 10);

    service.notifyUsersForDacs(
        Set.of(chair1, member1), Set.of(), Set.of(), dar, collection, "Researcher Name", true);

    verify(emailService, times(2)).sendMessage(any(NewCaseMessage.class), any());
    verify(emailService, never()).sendMessage(any(NewDARRequestMessage.class), any());
  }

  /** Auto-open, progress-report: each user receives a NewProgressReportCaseMessage. */
  @Test
  void testNotifyUsersForDacs_AutoOpen_ProgressReport_SendsProgressReportCaseMessagePerUser()
      throws Exception {
    DataAccessRequest dar = new DataAccessRequest();
    dar.setReferenceId(UUID.randomUUID().toString());
    dar.progressReport = true;
    dar.setData(new DataAccessRequestData());

    DarCollection collection = new DarCollection();
    collection.setDarCode("DAR-PR-AUTO");

    User chair1 = createUserWithRoleAndDacId(UserRoles.CHAIRPERSON, 10);
    User member1 = createUserWithRoleAndDacId(UserRoles.MEMBER, 10);

    service.notifyUsersForDacs(
        Set.of(chair1, member1), Set.of(), Set.of(), dar, collection, "Researcher Name", true);

    verify(emailService, times(2)).sendMessage(any(NewProgressReportCaseMessage.class), any());
    verify(emailService, never()).sendMessage(any(NewDARRequestMessage.class), any());
  }

  /** Empty user set — no email interactions at all. */
  @Test
  void testNotifyUsersForDacs_EmptyUsers_NoEmailsSent() throws Exception {
    DataAccessRequest dar = new DataAccessRequest();
    dar.setReferenceId(UUID.randomUUID().toString());
    dar.setData(new DataAccessRequestData());

    DarCollection collection = new DarCollection();
    collection.setDarCode("DAR-EMPTY");

    service.notifyUsersForDacs(
        Set.of(), Set.of(), Set.of(), dar, collection, "Researcher Name", false);

    verifyNoInteractions(emailService);
  }

  /**
   * User whose DAC role does not match any DAC in the notification set receives an email with an
   * empty dacDatasetGroups map — no data from other users leaks in.
   */
  @Test
  void testNotifyUsersForDacs_ManualPath_UserWithNoMatchingDac_ReceivesEmptyMap() throws Exception {
    DataAccessRequest dar = new DataAccessRequest();
    dar.setReferenceId(UUID.randomUUID().toString());
    dar.setData(new DataAccessRequestData());

    DarCollection collection = new DarCollection();
    collection.setDarCode("DAR-NOMATCH");

    Dac dac10 = new Dac();
    dac10.setDacId(10);
    dac10.setName("DAC-10");

    Dataset dataset1 = new Dataset();
    dataset1.setDatasetId(1);
    dataset1.setDacId(10);
    dataset1.setAlias(1);
    dataset1.setDatasetIdentifier();

    // User has CHAIRPERSON role for DAC 99, which is not in the notification DAC set
    User chairDac99 = createUserWithRoleAndDacId(UserRoles.CHAIRPERSON, 99);

    service.notifyUsersForDacs(
        Set.of(chairDac99),
        Set.of(dac10),
        Set.of(dataset1),
        dar,
        collection,
        "Researcher Name",
        false);

    ArgumentCaptor<NewDARRequestMessage> captor =
        ArgumentCaptor.forClass(NewDARRequestMessage.class);
    verify(emailService).sendMessage(captor.capture(), eq(chairDac99.getUserId()));

    @SuppressWarnings("unchecked")
    Map<String, List<String>> recipientMap =
        (Map<String, List<String>>) captor.getValue().createModel().get("dacDatasetGroups");

    assertTrue(recipientMap.isEmpty(), "user with no matching DAC should receive empty map");
  }

  // ─── filterUsersForDac ──────────────────────────────────────────────────────

  @Test
  void testFilterUsersForDac_ReturnsChairAndMemberForMatchingDac() {
    User chair10 = createUserWithRoleAndDacId(UserRoles.CHAIRPERSON, 10);
    User member10 = createUserWithRoleAndDacId(UserRoles.MEMBER, 10);
    User chair20 = createUserWithRoleAndDacId(UserRoles.CHAIRPERSON, 20);

    Set<User> result = service.filterUsersForDac(Set.of(chair10, member10, chair20), 10);

    assertEquals(2, result.size());
    assertTrue(result.contains(chair10));
    assertTrue(result.contains(member10));
    assertFalse(result.contains(chair20));
  }

  @Test
  void testFilterUsersForDac_EmptyInput_ReturnsEmpty() {
    assertTrue(service.filterUsersForDac(Set.of(), 10).isEmpty());
  }

  // ─── addUsers ───────────────────────────────────────────────────────────────

  @Test
  void testAddUsers_AutoOpen_IncludesBothChairAndMember() {
    Set<User> target = new HashSet<>();
    User chair = createUserWithRoleAndDacId(UserRoles.CHAIRPERSON, 10);
    User member = createUserWithRoleAndDacId(UserRoles.MEMBER, 10);

    service.addUsers(target, 10, List.of(chair, member), true);

    assertEquals(Set.of(chair, member), target);
  }

  @Test
  void testAddUsers_AutoOpen_ExcludesUserFromDifferentDac() {
    Set<User> target = new HashSet<>();
    User chairOtherDac = createUserWithRoleAndDacId(UserRoles.CHAIRPERSON, 20);

    service.addUsers(target, 10, List.of(chairOtherDac), true);

    assertTrue(target.isEmpty());
  }

  @Test
  void testAddUsers_Manual_IncludesChair_ExcludesMember() {
    Set<User> target = new HashSet<>();
    User chair = createUserWithRoleAndDacId(UserRoles.CHAIRPERSON, 10);
    User member = createUserWithRoleAndDacId(UserRoles.MEMBER, 10);

    service.addUsers(target, 10, List.of(chair, member), false);

    assertTrue(target.contains(chair));
    assertFalse(target.contains(member));
  }

  @Test
  void testAddUsers_Manual_ExcludesChairFromDifferentDac() {
    Set<User> target = new HashSet<>();
    User chairOtherDac = createUserWithRoleAndDacId(UserRoles.CHAIRPERSON, 20);

    service.addUsers(target, 10, List.of(chairOtherDac), false);

    assertTrue(target.isEmpty());
  }

  // ─── classifyDacsAndUsers ───────────────────────────────────────────────────

  @Test
  void testClassifyDacsAndUsers_AutoOpen_PopulatesAllAutoOpenFields() {
    Dac dac = new Dac();
    dac.setDacId(10);

    Dataset dataset = new Dataset();
    dataset.setDatasetId(1);
    dataset.setDacId(10);

    User chair = createUserWithRoleAndDacId(UserRoles.CHAIRPERSON, 10);
    User member = createUserWithRoleAndDacId(UserRoles.MEMBER, 10);

    DACAutomationRule autoOpenRule = mock(DACAutomationRule.class);
    when(autoOpenRule.ruleType()).thenReturn(DACAutomationRuleType.AUTO_OPEN_DAR_FOR_ALL_MEMBERS);
    when(autoOpenRule.enabledByUserId()).thenReturn(chair.getUserId());
    when(dacAutomationRuleService.findAllByDacId(10)).thenReturn(List.of(autoOpenRule));

    DacUserClassification result =
        service.classifyDacsAndUsers(List.of(dac), List.of(dataset), List.of(chair, member));

    assertTrue(result.autoOpenDacs.contains(dac));
    assertTrue(result.autoOpenDatasets.contains(dataset));
    assertTrue(result.autoOpenUsers.contains(chair));
    assertTrue(result.autoOpenUsers.contains(member));
    assertEquals(chair.getUserId(), result.autoOpenUserIds.get(10));
    assertTrue(result.manualOpenDacs.isEmpty());
    assertTrue(result.manualOpenDatasets.isEmpty());
    assertTrue(result.manualOpenUsers.isEmpty());
  }

  @Test
  void testClassifyDacsAndUsers_ManualOpen_MemberExcluded() {
    Dac dac = new Dac();
    dac.setDacId(10);

    Dataset dataset = new Dataset();
    dataset.setDatasetId(1);
    dataset.setDacId(10);

    User chair = createUserWithRoleAndDacId(UserRoles.CHAIRPERSON, 10);
    User member = createUserWithRoleAndDacId(UserRoles.MEMBER, 10);

    when(dacAutomationRuleService.findAllByDacId(10)).thenReturn(List.of());

    DacUserClassification result =
        service.classifyDacsAndUsers(List.of(dac), List.of(dataset), List.of(chair, member));

    assertTrue(result.manualOpenDacs.contains(dac));
    assertTrue(result.manualOpenDatasets.contains(dataset));
    assertTrue(result.manualOpenUsers.contains(chair));
    assertFalse(result.manualOpenUsers.contains(member));
    assertTrue(result.autoOpenDacs.isEmpty());
    assertTrue(result.autoOpenDatasets.isEmpty());
    assertTrue(result.autoOpenUsers.isEmpty());
    assertTrue(result.autoOpenUserIds.isEmpty());
  }

  @Test
  void testClassifyDacsAndUsers_MixedDacs_PartitionsCorrectly() {
    Dac dac10 = new Dac();
    dac10.setDacId(10);
    Dac dac20 = new Dac();
    dac20.setDacId(20);

    Dataset dataset1 = new Dataset();
    dataset1.setDatasetId(1);
    dataset1.setDacId(10);
    Dataset dataset2 = new Dataset();
    dataset2.setDatasetId(2);
    dataset2.setDacId(20);

    User chair10 = createUserWithRoleAndDacId(UserRoles.CHAIRPERSON, 10);
    User chair20 = createUserWithRoleAndDacId(UserRoles.CHAIRPERSON, 20);

    DACAutomationRule autoOpenRule = mock(DACAutomationRule.class);
    when(autoOpenRule.ruleType()).thenReturn(DACAutomationRuleType.AUTO_OPEN_DAR_FOR_ALL_MEMBERS);
    when(autoOpenRule.enabledByUserId()).thenReturn(chair10.getUserId());
    when(dacAutomationRuleService.findAllByDacId(10)).thenReturn(List.of(autoOpenRule));
    when(dacAutomationRuleService.findAllByDacId(20)).thenReturn(List.of());

    DacUserClassification result =
        service.classifyDacsAndUsers(
            List.of(dac10, dac20), List.of(dataset1, dataset2), List.of(chair10, chair20));

    assertTrue(result.autoOpenDacs.contains(dac10));
    assertFalse(result.autoOpenDacs.contains(dac20));
    assertTrue(result.autoOpenDatasets.contains(dataset1));
    assertFalse(result.autoOpenDatasets.contains(dataset2));
    assertTrue(result.autoOpenUsers.contains(chair10));
    assertFalse(result.autoOpenUsers.contains(chair20));
    assertEquals(chair10.getUserId(), result.autoOpenUserIds.get(10));
    assertFalse(result.autoOpenUserIds.containsKey(20));

    assertTrue(result.manualOpenDacs.contains(dac20));
    assertFalse(result.manualOpenDacs.contains(dac10));
    assertTrue(result.manualOpenDatasets.contains(dataset2));
    assertFalse(result.manualOpenDatasets.contains(dataset1));
    assertTrue(result.manualOpenUsers.contains(chair20));
    assertFalse(result.manualOpenUsers.contains(chair10));
  }

  @Test
  void testClassifyDacsAndUsers_DatasetWithNoMatchingDac_DacNotAddedToResult() {
    Dataset dataset = new Dataset();
    dataset.setDatasetId(1);
    dataset.setDacId(10);

    User chair = createUserWithRoleAndDacId(UserRoles.CHAIRPERSON, 10);

    DACAutomationRule autoOpenRule = mock(DACAutomationRule.class);
    when(autoOpenRule.ruleType()).thenReturn(DACAutomationRuleType.AUTO_OPEN_DAR_FOR_ALL_MEMBERS);
    when(autoOpenRule.enabledByUserId()).thenReturn(chair.getUserId());
    when(dacAutomationRuleService.findAllByDacId(10)).thenReturn(List.of(autoOpenRule));

    DacUserClassification result =
        service.classifyDacsAndUsers(List.of(), List.of(dataset), List.of(chair));

    assertTrue(result.autoOpenDacs.isEmpty());
    assertTrue(result.autoOpenDatasets.contains(dataset));
    assertTrue(result.autoOpenUsers.contains(chair));
    assertEquals(chair.getUserId(), result.autoOpenUserIds.get(10));
  }

  // ─── SO-approval gate in createElectionsAndVotesForAutoOpenDacs ─────────────

  @Test
  void testCreateElectionsAndVotesForAutoOpenDacs_SoApprovalPending_NoElectionsCreated() {
    DataAccessRequest dar = new DataAccessRequest();
    dar.setReferenceId(UUID.randomUUID().toString());
    dar.setRequiresSOApproval(true);
    // approvingSigningOfficialUserId defaults to null — SO approval is still pending.
    // The guard returns before consulting the classification, so its contents are irrelevant.
    DacUserClassification classification = new DacUserClassification();

    service.createElectionsAndVotesForAutoOpenDacs(classification, dar);

    verify(dacAutomationRuleService, never()).createOpenElectionForDAR(any(), any(), any());
    verify(dacAutomationRuleService, never()).createVoteForElection(anyInt(), anyInt(), any());
  }

  // ─── DAO role-list assertion ─────────────────────────────────────────────────

  @Test
  @SuppressWarnings("unchecked")
  void testCreateElectionsForNewDarCollection_FetchesOnlyChairAndMemberRoles() {
    DarCollection collection = new DarCollection();
    collection.setDarCollectionId(1);
    DataAccessRequest dar = new DataAccessRequest();
    dar.setReferenceId(UUID.randomUUID().toString());
    dar.setDatasetIds(List.of(1));
    collection.addDar(dar);

    when(darCollectionDAO.findDARCollectionByCollectionId(1)).thenReturn(collection);
    when(datasetDAO.findDatasetsByIdList(anyList())).thenReturn(List.of());
    when(dacDAO.findDacsForDatasetIds(anyList())).thenReturn(Set.of());
    ArgumentCaptor<List<Integer>> roleCaptor = ArgumentCaptor.forClass(List.class);
    when(userDAO.findUsersForDatasetsByRole(anyList(), roleCaptor.capture())).thenReturn(Set.of());

    service.createElectionsForNewDarCollection(1);

    List<Integer> capturedRoleIds = roleCaptor.getValue();
    assertTrue(capturedRoleIds.contains(UserRoles.CHAIRPERSON.getRoleId()));
    assertTrue(capturedRoleIds.contains(UserRoles.MEMBER.getRoleId()));
    assertFalse(
        capturedRoleIds.contains(UserRoles.ADMIN.getRoleId()), "ADMIN role must not be fetched");
  }

  // ─── Member excluded from manual notification (integration) ─────────────────

  @Test
  void testSendNewDARCollectionMessage_ManualDac_MemberExcluded_OnlyChairNotified()
      throws Exception {
    DarCollection collection = new DarCollection();
    collection.setDarCollectionId(1);

    DataAccessRequest dar = new DataAccessRequest();
    dar.setReferenceId(UUID.randomUUID().toString());
    dar.setData(new DataAccessRequestData());

    Dataset dataset = new Dataset();
    dataset.setDatasetId(1);
    dataset.setDacId(10);
    dataset.setAlias(1);
    dataset.setDatasetIdentifier();

    dar.setDatasetIds(List.of(dataset.getDatasetId()));
    collection.addDar(dar);

    Dac dac = new Dac();
    dac.setDacId(10);
    dac.setName("DAC-10");

    User chair = createUserWithRoleAndDacId(UserRoles.CHAIRPERSON, 10);
    User member = createUserWithRoleAndDacId(UserRoles.MEMBER, 10);

    when(darCollectionDAO.findDARCollectionByCollectionId(1)).thenReturn(collection);
    when(userDAO.findUserById(any())).thenReturn(new User());
    when(datasetDAO.findDatasetsByIdList(anyList())).thenReturn(List.of(dataset));
    when(dacDAO.findDacsForDatasetIds(anyList())).thenReturn(Set.of(dac));
    when(dacAutomationRuleService.findAllByDacId(anyInt())).thenReturn(List.of());
    // anyList() for roles is intentional here — role-list sourcing is verified separately in
    // testCreateElectionsForNewDarCollection_FetchesOnlyChairAndMemberRoles.
    // This test focuses on classification and notification: the DAO returns both chair and
    // member; only the chair (classified into manualOpenUsers) should receive an email.
    when(userDAO.findUsersForDatasetsByRole(anyList(), anyList()))
        .thenReturn(Set.of(chair, member));

    service.sendNewDARCollectionMessage(1);

    ArgumentCaptor<NewDARRequestMessage> captor =
        ArgumentCaptor.forClass(NewDARRequestMessage.class);
    verify(emailService, times(1)).sendMessage(captor.capture(), any());
    assertEquals(
        chair.getUserId(),
        captor.getValue().toUser.getUserId(),
        "Only the chair should receive a manual-open notification, not the member");
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

  private User createUserWithRoleAndDacId(UserRoles userRoles, Integer dacId) {
    User user = new User();
    user.setUserId(randomInt(1, 100000));
    user.setDisplayName(String.format("%s - %s", userRoles.getRoleName(), user.getUserId()));
    user.setEmail(String.format("%s@test.com", userRoles.getRoleName()));
    UserRole role = new UserRole(userRoles.getRoleId(), userRoles.getRoleName());
    role.setDacId(dacId);
    user.setRoles(List.of(role));
    user.setEmailPreference(Boolean.TRUE);
    return user;
  }
}
