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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import freemarker.template.TemplateException;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotAcceptableException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.broadinstitute.consent.http.AbstractTestHelper;
import org.broadinstitute.consent.http.db.DacDAO;
import org.broadinstitute.consent.http.db.DarCollectionDAO;
import org.broadinstitute.consent.http.db.DarCollectionSummaryDAO;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.MatchDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.enumeration.DarCollectionActions;
import org.broadinstitute.consent.http.enumeration.DarCollectionStatus;
import org.broadinstitute.consent.http.enumeration.DarStatus;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.enumeration.VoteType;
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
import org.broadinstitute.consent.http.service.dao.DarCollectionServiceDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DarCollectionServiceTest extends AbstractTestHelper {

  private DarCollectionService service;

  @Mock
  private DarCollectionDAO darCollectionDAO;
  @Mock
  private DarCollectionSummaryDAO darCollectionSummaryDAO;
  @Mock
  private DarCollectionServiceDAO darCollectionServiceDAO;
  @Mock
  private DatasetDAO datasetDAO;
  @Mock
  private ElectionDAO electionDAO;
  @Mock
  private DataAccessRequestDAO dataAccessRequestDAO;
  @Mock
  private EmailService emailService;
  @Mock
  private VoteDAO voteDAO;
  @Mock
  private MatchDAO matchDAO;
  @Mock
  private UserDAO userDAO;
  @Mock
  private DacDAO dacDAO;

  @BeforeEach
  void setUp() {
    service = new DarCollectionService(darCollectionDAO, darCollectionServiceDAO, datasetDAO,
        electionDAO, dataAccessRequestDAO, emailService, voteDAO, matchDAO,
        darCollectionSummaryDAO, userDAO, dacDAO);
  }

  @Test
  void testAddDatasetsToCollection() {
    Set<Dataset> datasets = new HashSet<>();
    DarCollection collection = generateMockDarCollection(datasets);
    List<Integer> datasetIds = datasets.stream()
        .map(Dataset::getDatasetId)
        .sorted()
        .toList();

    when(datasetDAO.findDatasetsByIdList(anyList())).thenReturn(List.copyOf(datasets));
    when(dataAccessRequestDAO.findAllDARDatasetRelations(any())).thenReturn(datasetIds);

    collection = service.addDatasetsToCollection(collection);
    assertNotNull(collection);

    Set<Dataset> datasetsFromCollection = collection.getDatasets();
    assertEquals(datasetIds.size(), datasetsFromCollection.size());

    List<Integer> collectionDatasetIds = datasetsFromCollection.stream()
        .map(Dataset::getDatasetId)
        .sorted()
        .toList();
    assertEquals(datasetIds, collectionDatasetIds);
  }

  @Test
  void testCancelDarCollection_noElections() {
    Set<Dataset> datasets = new HashSet<>();
    DarCollection collection = generateMockDarCollection(datasets);
    collection.getDars().values().forEach(d -> d.getData().setStatus("Canceled"));
    when(darCollectionDAO.findDARCollectionByCollectionId(any())).thenReturn(collection);
    when(darCollectionSummaryDAO.getDarCollectionSummaryByCollectionId(collection.getDarCollectionId()))
        .thenReturn(new DarCollectionSummary());

    User user = new User();
    user.setUserId(1);
    collection.setCreateUserId(user.getUserId());
    when(electionDAO.findLastElectionsByReferenceIds(anyList())).thenReturn(List.of());
    DarCollection canceledCollection = service.cancelDarCollectionByRole(user, collection, UserRoles.RESEARCHER);
    for (DataAccessRequest collectionDar : canceledCollection.getDars().values()) {
      assertEquals("canceled", collectionDar.getData().getStatus().toLowerCase());
    }
  }

  @Test
  void testCancelDarCollection_electionPresent() {
    Set<Dataset> datasets = new HashSet<>();
    DarCollection collection = generateMockDarCollection(datasets);

    when(electionDAO.findLastElectionsByReferenceIds(anyList())).thenReturn(
        List.of(new Election()));
    when(darCollectionSummaryDAO.getDarCollectionSummaryByCollectionId(collection.getDarCollectionId()))
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
    DarCollection collection = createMockCollections().get(0);
    collection.addDar(dar);
    when(electionDAO.findLastElectionsByReferenceIds(anyList())).thenReturn(List.of());
    when(darCollectionDAO.findDARCollectionByCollectionId(any())).thenReturn(collection);
    when(darCollectionSummaryDAO.getDarCollectionSummaryByCollectionId(collection.getDarCollectionId()))
        .thenReturn(new DarCollectionSummary());

    User user = new User();
    user.setUserId(1);
    collection.setCreateUserId(user.getUserId());
    service.cancelDarCollectionByRole(user, collection, UserRoles.RESEARCHER);
    verify(electionDAO).findLastElectionsByReferenceIds(anyList());
    verify(electionDAO, times(0)).updateElectionById(anyInt(), anyString(), any());
    verify(dataAccessRequestDAO).cancelByReferenceIds(anyList());
    verify(darCollectionDAO).findDARCollectionByCollectionId(anyInt());
  }

  @Test
  void testCancelDarCollectionAsResearcher_WithElections() {
    DataAccessRequest dar = new DataAccessRequest();
    dar.setReferenceId(UUID.randomUUID().toString());
    DataAccessRequestData data = new DataAccessRequestData();
    dar.setData(data);
    DarCollection collection = createMockCollections().get(0);
    collection.addDar(dar);
    Election election = createMockElection();
    election.setReferenceId(dar.getReferenceId());
    election.setStatus(ElectionStatus.OPEN.getValue());
    election.setElectionId(1);
    User user = new User();
    user.setUserId(1);
    collection.setCreateUserId(user.getUserId());
    when(electionDAO.findLastElectionsByReferenceIds(anyList())).thenReturn(List.of(election));
    when(darCollectionSummaryDAO.getDarCollectionSummaryByCollectionId(collection.getDarCollectionId()))
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
    DarCollection collection = createMockCollections().get(0);
    collection.addDar(dar);
    Election election = createMockElection();
    election.setReferenceId(dar.getReferenceId());
    election.setStatus(ElectionStatus.OPEN.getValue());
    election.setElectionId(1);
    when(electionDAO.findOpenElectionsByReferenceIds(anyList())).thenReturn(List.of(election));
    when(darCollectionDAO.findDARCollectionByCollectionId(collection.getDarCollectionId())).thenReturn(collection);

    service.cancelDarCollectionByRole(new User(), collection, UserRoles.ADMIN);
    verify(electionDAO).findOpenElectionsByReferenceIds(anyList());
    verify(electionDAO).updateElectionById(anyInt(), anyString(), any());
    verify(dataAccessRequestDAO, times(0)).cancelByReferenceIds(anyList());
    verify(darCollectionDAO).findDARCollectionByCollectionId(anyInt());
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
    DarCollection collection = createMockCollections().get(0);
    collection.addDar(dar);
    Election election = createMockElection();
    election.setReferenceId(dar.getReferenceId());
    election.setStatus(ElectionStatus.OPEN.getValue());
    election.setElectionId(1);
    when(datasetDAO.findDatasetIdsByDACUserId(anyInt())).thenReturn(
        List.of(dataset.getDatasetId()));
    when(electionDAO.findOpenElectionsByReferenceIds(anyList())).thenReturn(List.of(election));
    when(darCollectionDAO.findDARCollectionByCollectionId(collection.getDarCollectionId())).thenReturn(collection);

    service.cancelDarCollectionByRole(user, collection, UserRoles.CHAIRPERSON);
    verify(datasetDAO).findDatasetIdsByDACUserId(anyInt());
    verify(electionDAO).findOpenElectionsByReferenceIds(anyList());
    verify(electionDAO).updateElectionById(anyInt(), anyString(), any());
    verify(dataAccessRequestDAO, times(0)).cancelByReferenceIds(anyList());
    verify(darCollectionDAO).findDARCollectionByCollectionId(anyInt());
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
    DarCollection collection = createMockCollections().get(0);
    collection.addDar(dar);
    Election election = createMockElection();
    election.setReferenceId(dar.getReferenceId());
    election.setStatus(ElectionStatus.OPEN.getValue());
    election.setElectionId(1);
    when(datasetDAO.findDatasetIdsByDACUserId(anyInt())).thenReturn(List.of());

    service.cancelDarCollectionByRole(user, collection, UserRoles.CHAIRPERSON);
    verify(datasetDAO).findDatasetIdsByDACUserId(anyInt());
    verifyNoMoreInteractions(electionDAO);
    verifyNoInteractions(dataAccessRequestDAO, darCollectionDAO);
  }

  @Test
  void cancelDarCollectionByRole_ProgressReport() {
    DarCollection collection = createMockCollections().get(0);
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
    DarCollection collection = createMockCollections().get(0);
    collection.addDar(dar);
    when(darCollectionServiceDAO.createElectionsForDarByUser(any(), any())).thenReturn(
        List.of("electionId"));
    when(voteDAO.findVoteUsersByElectionReferenceIdList(any())).thenReturn(List.of(new User()));

    service.createElectionsForDarCollection(user, collection);
    verify(darCollectionServiceDAO).createElectionsForDarByUser(any(), eq(dar));
    verify(voteDAO).findVoteUsersByElectionReferenceIdList(any());
    verify(emailService).sendDarNewCollectionElectionMessage(any(), any());
    verify(darCollectionDAO).findDARCollectionByCollectionId(any());
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
    DarCollection collection = createMockCollections().get(0);
    collection.addDar(dar);
    collection.addDar(progressReport);

    User voteUser = new User();
    String electionId = "electionId";
    when(darCollectionServiceDAO.createElectionsForDarByUser(user, progressReport))
        .thenReturn(List.of(electionId));
    when(voteDAO.findVoteUsersByElectionReferenceIdList(List.of(electionId)))
        .thenReturn(List.of(voteUser));

    service.createElectionsForDarCollection(user, collection);

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
    DarCollection collection = createMockCollections().get(0);
    collection.addDar(dar);

    assertThrows(IllegalStateException.class, () -> service.createElectionsForDarCollection(user, collection));
  }

  @Test
  void testCreateElectionsForDarCollectionVoteUsersException() throws Exception {
    User user = new User();
    user.setEmail("email");
    DataAccessRequest dar = new DataAccessRequest();
    dar.setReferenceId(UUID.randomUUID().toString());
    DarCollection collection = createMockCollections().get(0);
    collection.addDar(dar);
    List<String> electionIds = List.of("electionId");
    when(darCollectionServiceDAO.createElectionsForDarByUser(user, dar)).thenReturn(
        electionIds);
    when(voteDAO.findVoteUsersByElectionReferenceIdList(electionIds)).thenThrow(
        IllegalArgumentException.class);

    service.createElectionsForDarCollection(user, collection);
    verify(darCollectionServiceDAO).createElectionsForDarByUser(user, dar);
    verify(voteDAO).findVoteUsersByElectionReferenceIdList(electionIds);
    verifyNoInteractions(emailService);
    verify(darCollectionDAO).findDARCollectionByCollectionId(collection.getDarCollectionId());
  }

  @Test
  void testDeleteAsResearcherNoElections() {
    User user = new User();
    user.setUserId(1);
    user.setResearcherRole();

    Set<Dataset> datasets = new HashSet<>();
    DarCollection collection = generateMockDarCollection(datasets);
    collection.setDarCollectionId(10);
    collection.setCreateUserId(user.getUserId());

    when(electionDAO.findElectionsByReferenceIds(any())).thenReturn(List.of());
    when(darCollectionDAO.findDARCollectionByCollectionId(any())).thenReturn(collection);

    Integer collectionId = collection.getDarCollectionId();

    System.out.println(collectionId);
    service.deleteByCollectionId(user, collectionId);

    // verify each DAR was deleted
    verify(dataAccessRequestDAO).deleteByReferenceIds(any());
    verify(dataAccessRequestDAO).deleteDARDatasetRelationByReferenceIds(any());
    verify(matchDAO).deleteMatchesByPurposeIds(any());
    // verify overarching collection was deleted
    verify(darCollectionDAO).deleteByCollectionId(collectionId);
    verify(electionDAO, times(0)).deleteElectionsByIds(any());
    verify(voteDAO, times(0)).deleteVotesByReferenceIds(any());

  }

  @Test
  void testDeleteAsResearcherWithElections() {
    User user = new User();
    user.setUserId(1);
    user.setResearcherRole();

    Set<Dataset> datasets = new HashSet<>();
    DarCollection collection = generateMockDarCollection(datasets);
    collection.setDarCollectionId(10);
    collection.setCreateUserId(user.getUserId());

    when(electionDAO.findElectionsByReferenceIds(any())).thenReturn(List.of(createMockElection()));
    when(darCollectionDAO.findDARCollectionByCollectionId(any())).thenReturn(collection);

    Integer collectionId = collection.getDarCollectionId();

    assertThrows(NotAcceptableException.class, () -> service.deleteByCollectionId(user, collectionId));
  }

  @Test
  void testDeleteAsAdminWithElections() {
    User user = new User();
    user.setUserId(1);
    user.setAdminRole();

    Set<Dataset> datasets = new HashSet<>();
    DarCollection collection = generateMockDarCollection(datasets);
    collection.setDarCollectionId(10);

    when(electionDAO.findElectionsByReferenceIds(any())).thenReturn(List.of(createMockElection()));
    when(darCollectionDAO.findDARCollectionByCollectionId(any())).thenReturn(collection);

    Integer collectionId = collection.getDarCollectionId();

    service.deleteByCollectionId(user, collectionId);

    verify(dataAccessRequestDAO).deleteByReferenceIds(any());
    verify(dataAccessRequestDAO).deleteDARDatasetRelationByReferenceIds(any());
    verify(matchDAO).deleteMatchesByPurposeIds(any());
    verify(darCollectionDAO).deleteByCollectionId(collectionId);
    verify(electionDAO).deleteElectionsByIds(any());
    verify(voteDAO).deleteVotesByReferenceIds(any());
  }


  @Test
  void testDeleteAsUser() {
    User user = new User();
    user.setUserId(1);

    Set<Dataset> datasets = new HashSet<>();
    DarCollection collection = generateMockDarCollection(datasets);
    collection.setDarCollectionId(10);
    collection.setCreateUserId(2); // not same as user id

    Integer collectionId = collection.getDarCollectionId();

    when(darCollectionDAO.findDARCollectionByCollectionId(any())).thenReturn(collection);

    assertThrows(NotAuthorizedException.class, () -> service.deleteByCollectionId(user, collectionId));
  }

  @Test
  void testDeleteButNoCollection() {
    User user = new User();
    user.setUserId(1);

    Set<Dataset> datasets = new HashSet<>();
    DarCollection collection = generateMockDarCollection(datasets);
    collection.setDarCollectionId(10);
    collection.setCreateUserId(1);

    Integer collectionId = collection.getDarCollectionId();

    when(darCollectionDAO.findDARCollectionByCollectionId(any())).thenReturn(null);

    assertThrows(NotFoundException.class, () -> service.deleteByCollectionId(user, collectionId));
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
    when(darCollectionSummaryDAO.getDarCollectionSummariesForSO(any())).thenReturn(
        List.of(summary));

    List<DarCollectionSummary> summaries = service.getSummariesForRole(user,
        UserRoles.SIGNINGOFFICIAL);
    assertNotNull(summaries);
    assertEquals(1, summaries.size());
    DarCollectionSummary s = summaries.get(0);
    assertTrue(
        s.getStatus().equalsIgnoreCase(DarCollectionStatus.IN_PROCESS.getValue()));
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
    when(darCollectionSummaryDAO.getDarCollectionSummariesForSO(any())).thenReturn(
        List.of(summary));

    List<DarCollectionSummary> summaries = service.getSummariesForRole(user,
        UserRoles.SIGNINGOFFICIAL);
    assertNotNull(summaries);
    assertEquals(1, summaries.size());
    DarCollectionSummary s = summaries.get(0);
    assertTrue(
        s.getStatus().equalsIgnoreCase(DarCollectionStatus.COMPLETE.getValue()));
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
    when(darCollectionSummaryDAO.getDarCollectionSummariesForSO(any())).thenReturn(
        List.of(summary));

    List<DarCollectionSummary> summaries = service.getSummariesForRole(user,
        UserRoles.SIGNINGOFFICIAL);
    assertNotNull(summaries);
    assertEquals(1, summaries.size());
    DarCollectionSummary s = summaries.get(0);
    assertTrue(
        s.getStatus().equalsIgnoreCase(DarCollectionStatus.SUBMITTED.getValue()));
  }

  @Test
  void testProcessDarCollectionSummariesForAdminWithCloseout() {
    User user = new User();
    user.setUserId(1);
    DarCollectionSummary summary = new DarCollectionSummary();
    summary.setLatestReferenceId(UUID.randomUUID().toString());
    summary.setCloseoutSupplement(new CloseoutSupplement(List.of("Closeout"), "Closeout", 1));
    when(darCollectionSummaryDAO.getDarCollectionSummariesForAdmin())
        .thenReturn(List.of(summary));

    List<DarCollectionSummary> summaries = service.getSummariesForRole(user, UserRoles.ADMIN);

    assertNotNull(summaries);
    assertEquals(1, summaries.size());
    // Admin summary should not have any actions
    assertTrue(summaries.get(0).getActions().isEmpty());
  }

  @Test
  void testProcessDarCollectionSummariesForAdminWithoutCloseout() {
    User user = new User();
    user.setUserId(1);
    DarCollectionSummary summary = new DarCollectionSummary();
    summary.setLatestReferenceId(UUID.randomUUID().toString());
    when(darCollectionSummaryDAO.getDarCollectionSummariesForAdmin())
        .thenReturn(List.of(summary));

    List<DarCollectionSummary> summaries = service.getSummariesForRole(user, UserRoles.ADMIN);

    assertNotNull(summaries);
    assertEquals(1, summaries.size());
    // With no elections, there should be an Open action
    assertTrue(summaries.get(0).getActions().contains(DarCollectionActions.OPEN.getValue()));
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
    data.setCreateDate(draft.getCreateDate().getTime());
    draft.setData(data);
    when(dataAccessRequestDAO.findAllDraftsByUserId(any())).thenReturn(List.of(draft));
    when(darCollectionSummaryDAO.getDarCollectionSummariesForResearcher(any())).thenReturn(
        List.of(summaryOne, summaryTwo, summaryThree, summaryFour, summaryFive));
    when(dataAccessRequestDAO.findDatasetApprovalsByDar("ref1")).thenReturn(Set.of());
    when(dataAccessRequestDAO.findDatasetApprovalsByDar("ref4"))
        .thenReturn(Set.of(datasetSix.getDatasetId()));

    List<DarCollectionSummary> summaries = service.getSummariesForRole(user,
        UserRoles.RESEARCHER);
    assertNotNull(summaries);
    assertEquals(6, summaries.size());

    DarCollectionSummary testOne = summaries.get(0);
    Set<String> expectedOneActions = Set.of(
        DarCollectionActions.REVIEW.getValue()
    );
    assertTrue(
        testOne.getStatus().equalsIgnoreCase(DarCollectionStatus.IN_PROCESS.getValue()));
    assertEquals(expectedOneActions, testOne.getActions());

    DarCollectionSummary testTwo = summaries.get(1);
    Set<String> expectedTwoActions = Set.of(
        DarCollectionActions.REVIEW.getValue(),
        DarCollectionActions.CANCEL.getValue()
    );
    assertTrue(
        testTwo.getStatus().equalsIgnoreCase(DarCollectionStatus.SUBMITTED.getValue()));
    assertEquals(expectedTwoActions, testTwo.getActions());

    DarCollectionSummary testThree = summaries.get(2);
    Set<String> expectedThreeActions = Set.of(
        DarCollectionActions.REVIEW.getValue(),
        DarCollectionActions.REVISE.getValue());
    assertTrue(
        testThree.getStatus().equalsIgnoreCase(DarCollectionStatus.CANCELED.getValue()));
    assertEquals(expectedThreeActions, testThree.getActions());

    DarCollectionSummary testFour = summaries.get(3);
    Set<String> expectedFourActions = Set.of(
        DarCollectionActions.REVIEW.getValue(),
        DarCollectionActions.CREATE_PROGRESS_REPORT.getValue());
    assertTrue(
        testFour.getStatus().equalsIgnoreCase(DarCollectionStatus.COMPLETE.getValue()));
    assertEquals(testFour.getActions(), expectedFourActions);

    DarCollectionSummary testFive = summaries.get(4);
    assertEquals(DarCollectionStatus.IN_PROCESS.getValue(), testFive.getStatus());
    assertEquals(Set.of(DarCollectionActions.REVIEW.getValue()), testFive.getActions());

    DarCollectionSummary testDraft = summaries.get(5);
    Set<String> expectedDraftActions = Set.of(
        DarCollectionActions.RESUME.getValue(),
        DarCollectionActions.DELETE.getValue());
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
    assertTrue(summaries.get(0).getActions().contains(DarCollectionActions.REVIEW.getValue()));
    // Summaries with closeout should not have the CREATE_PROGRESS_REPORT action
    assertFalse(summaries.get(0).getActions()
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
    assertTrue(summaries.get(0).getActions().contains(DarCollectionActions.REVIEW.getValue()));
    // Summaries without a closeout should have the CREATE_PROGRESS_REPORT action
    assertTrue(summaries.get(0).getActions()
        .contains(DarCollectionActions.CREATE_PROGRESS_REPORT.getValue()));
  }

  @Test
  void testProcessDarCollectionSummariesForAdmin() {
    //summaryOne -> all elections present and open
    //summaryTwo -> mix of open elections : absent/non-open elections (in process)
    //summaryThree -> all canceled elections (Complete)
    //summaryFour -> no elections (unreviewed)

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

    List<DarCollectionSummary> summaries = service.getSummariesForRole(user,
        UserRoles.ADMIN);

    DarCollectionSummary testOne = summaries.get(0);
    Set<String> expectedOneActions = Set.of(
        DarCollectionActions.CANCEL.getValue());
    assertTrue(
        testOne.getStatus().equalsIgnoreCase(DarCollectionStatus.IN_PROCESS.getValue()));
    assertEquals(testOne.getActions(), expectedOneActions);

    DarCollectionSummary testTwo = summaries.get(1);
    Set<String> expectedTwoActions = Set.of(
        DarCollectionActions.CANCEL.getValue(),
        DarCollectionActions.OPEN.getValue());
    assertTrue(
        testTwo.getStatus().equalsIgnoreCase(DarCollectionStatus.IN_PROCESS.getValue()));
    assertEquals(testTwo.getActions(), expectedTwoActions);

    DarCollectionSummary testThree = summaries.get(2);
    Set<String> expectedThreeActions = Set.of(
        DarCollectionActions.OPEN.getValue());
    assertTrue(
        testThree.getStatus().equalsIgnoreCase(DarCollectionStatus.COMPLETE.getValue()));
    assertEquals(testThree.getActions(), expectedThreeActions);

    DarCollectionSummary testFour = summaries.get(3);
    Set<String> expectedFourActions = Set.of(
        DarCollectionActions.OPEN.getValue());
    assertTrue(
        testFour.getStatus().equalsIgnoreCase(DarCollectionStatus.SUBMITTED.getValue()));
    assertEquals(testFour.getActions(), expectedFourActions);
  }

  @Test
  void testProcessDarCollectionSummariesForDACMemberNoDatasets() {
    Dac dac = new Dac();
    dac.setDacId(randomInt(1, 10));
    User user = new User();
    user.setUserId(randomInt(1, 10));
    user.setMemberRole();
    List<DarCollectionSummary> summaries = service.getSummariesForRole(user,
        UserRoles.MEMBER);
    assertTrue(summaries.isEmpty());
  }

  @Test
  void testProcessDarCollectionSummariesForDACChairNoDatasets() {
    Dac dac = new Dac();
    dac.setDacId(randomInt(1, 10));
    User user = new User();
    user.setUserId(randomInt(1, 10));
    user.setChairpersonRole();
    List<DarCollectionSummary> summaries = service.getSummariesForRole(user,
        UserRoles.CHAIRPERSON);
    assertTrue(summaries.isEmpty());
  }

  @Test
  void testProcessDarCollectionSummariesForDACMember() {
    Dac dac = new Dac();
    dac.setDacId(1);
    User user = new User();
    user.setUserId(1);
    user.setMemberRoleWithDAC(dac.getDacId());

    //summaryOne -> no open elections (no action)
    //summaryTwo -> at least one open election, member has submitted all votes (Update button)
    //summaryThree -> unreviewed scenario (no elections),
    //summaryFour -> at least one open election, member has not submitted all votes (Vote button)

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
    Vote vote = new Vote(1, true, user.getUserId(), null, null, electionThree.getElectionId(), null,
        VoteType.DAC.getValue(), null, null);
    electionThree.setElectionId(3);
    electionThree.setStatus(ElectionStatus.OPEN.getValue());
    summaryTwo.addElection(electionThree);
    summaryTwo.addVote(vote);

    DarCollectionSummary summaryThree = new DarCollectionSummary();
    summaryThree.addDatasetId(4);

    DarCollectionSummary summaryFour = new DarCollectionSummary();
    summaryFour.addDatasetId(5);
    Election electionFour = new Election();
    Vote voteTwo = new Vote(2, true, user.getUserId(), null, null, electionThree.getElectionId(),
        null,
        VoteType.DAC.getValue(), null, null);
    Vote voteThree = new Vote(4, null, user.getUserId(), null, null, electionThree.getElectionId(),
        null,
        VoteType.DAC.getValue(), null, null);
    electionFour.setElectionId(4);
    electionFour.setStatus(ElectionStatus.OPEN.getValue());
    summaryFour.addElection(electionFour);
    summaryFour.setVotes(List.of(voteTwo, voteThree));

    List<Dataset> datasets = Stream.of(
            summary.getDatasetIds(),
            summaryTwo.getDatasetIds(),
            summaryThree.getDatasetIds(),
            summaryFour.getDatasetIds()
        ).flatMap(Set::stream)
        .distinct()
        .map(id -> {
          Dataset d = new Dataset();
          d.setDatasetId(id);
          return d;
        })
        .toList();
    when(datasetDAO.findDatasetListByDacIds(any())).thenReturn(datasets);
    when(darCollectionSummaryDAO.getDarCollectionSummariesForDAC(any(), any()))
        .thenReturn(List.of(summary, summaryTwo, summaryThree, summaryFour));

    List<DarCollectionSummary> summaries = service.getSummariesForRole(user,
        UserRoles.MEMBER);

    assertNotNull(summaries);
    assertEquals(4, summaries.size());

    DarCollectionSummary testOne = summaries.get(0);
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
    //summaryOne -> all elections present and open
    //summaryTwo -> mix of open elections : absent/canceled elections (in process)
    //summaryThree -> all canceled elections (Complete)
    //summaryFour -> no elections (unreviewed)
    //summaryFive -> mix of open : absent/closed elections (in process, but cancel action does not appear)
    //summarySix -> all closed elections (complete, only open available)

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

    when(darCollectionSummaryDAO.getDarCollectionSummariesForDAC(any(), any()))
        .thenReturn(
            List.of(summaryOne, summaryTwo, summaryThree, summaryFour, summaryFive, summarySix));
    when(datasetDAO.findDatasetListByDacIds(any())).thenReturn(List.of(datasetOne,
        datasetTwo,
        datasetThree,
        datasetFour,
        datasetFive,
        datasetFive,
        datasetSix));

    List<DarCollectionSummary> summaries = service.getSummariesForRole(user,
        UserRoles.CHAIRPERSON);
    assertEquals(6, summaries.size());

    DarCollectionSummary testOne = summaries.get(0);
    Set<String> expectedOneActions = Set.of(
        DarCollectionActions.VOTE.getValue(),
        DarCollectionActions.CANCEL.getValue());
    assertTrue(
        testOne.getStatus().equalsIgnoreCase(DarCollectionStatus.IN_PROCESS.getValue()));
    assertEquals(testOne.getActions(), expectedOneActions);

    DarCollectionSummary testTwo = summaries.get(1);
    Set<String> expectedTwoActions = Set.of(
        DarCollectionActions.VOTE.getValue(),
        DarCollectionActions.CANCEL.getValue(),
        DarCollectionActions.OPEN.getValue());
    assertTrue(
        testTwo.getStatus().equalsIgnoreCase(DarCollectionStatus.IN_PROCESS.getValue()));
    assertEquals(testTwo.getActions(), expectedTwoActions);

    DarCollectionSummary testThree = summaries.get(2);
    Set<String> expectedThreeActions = Set.of(
        DarCollectionActions.OPEN.getValue());
    assertTrue(
        testThree.getStatus().equalsIgnoreCase(DarCollectionStatus.COMPLETE.getValue()));
    assertEquals(testThree.getActions(), expectedThreeActions);

    DarCollectionSummary testFour = summaries.get(3);
    Set<String> expectedFourActions = Set.of(
        DarCollectionActions.OPEN.getValue());
    assertTrue(
        testFour.getStatus().equalsIgnoreCase(DarCollectionStatus.SUBMITTED.getValue()));
    assertEquals(testFour.getActions(), expectedFourActions);

    DarCollectionSummary testFive = summaries.get(4);
    Set<String> expectedFiveActions = Set.of(
        DarCollectionActions.OPEN.getValue(),
        DarCollectionActions.VOTE.getValue()
    );
    assertTrue(
        testFive.getStatus().equalsIgnoreCase(DarCollectionStatus.IN_PROCESS.getValue()));
    assertEquals(testFive.getActions(), expectedFiveActions);

    DarCollectionSummary testSix = summaries.get(5);
    Set<String> expectedSixActions = Set.of(
        DarCollectionActions.OPEN.getValue()
    );
    assertTrue(
        testSix.getStatus().equalsIgnoreCase(DarCollectionStatus.COMPLETE.getValue()));
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
    when(darCollectionSummaryDAO.getDarCollectionSummariesForDAC(user.getUserId(), List.of()))
        .thenReturn(List.of(summary));

    List<DarCollectionSummary> summaries = service.getSummariesForRole(user, UserRoles.CHAIRPERSON);

    assertNotNull(summaries);
    assertEquals(1, summaries.size());
    // Chair summary should not have any actions
    assertTrue(summaries.get(0).getActions().isEmpty());
  }

  @Test
  void testProcessDarCollectionSummariesForChairWithoutCloseout() {
    User user = new User();
    user.setUserId(1);
    user.addRole(UserRoles.Chairperson());
    DarCollectionSummary summary = new DarCollectionSummary();
    summary.setLatestReferenceId(UUID.randomUUID().toString());
    when(darCollectionSummaryDAO.getDarCollectionSummariesForDAC(user.getUserId(), List.of()))
        .thenReturn(List.of(summary));

    List<DarCollectionSummary> summaries = service.getSummariesForRole(user, UserRoles.CHAIRPERSON);

    assertNotNull(summaries);
    assertEquals(1, summaries.size());
    // With no elections, there should be an Open action
    assertTrue(summaries.get(0).getActions().contains(DarCollectionActions.OPEN.getValue()));
  }

  @Test
  void testGetSummaryForRoleByCollectionId_SO() {
    User user = new User();
    user.setUserId(1);

    DarCollectionSummary summary = createDarCollectionSummaryWithElections();
    Integer collectionId = summary.getDarCollectionId();

    when(darCollectionSummaryDAO.getDarCollectionSummaryByCollectionId(collectionId))
        .thenReturn(summary);

    DarCollectionSummary summaryResult = service.getSummaryForRoleByCollectionId(user,
        UserRoles.SIGNINGOFFICIAL, collectionId);
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

    DarCollectionSummary summaryResult = service.getSummaryForRoleByCollectionId(user,
        UserRoles.RESEARCHER, collectionId);
    assertNotNull(summaryResult);

    Set<String> expectedActions = Set.of(
        DarCollectionActions.REVIEW.getValue());
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

    DarCollectionSummary summaryResult = service.getSummaryForRoleByCollectionId(user,
        UserRoles.ADMIN, collectionId);
    assertNotNull(summaryResult);

    Set<String> expectedActions = Set.of(
        DarCollectionActions.CANCEL.getValue(),
        DarCollectionActions.OPEN.getValue());
    assertTrue(
        summaryResult.getStatus().equalsIgnoreCase(DarCollectionStatus.IN_PROCESS.getValue()));
    assertEquals(expectedActions, summaryResult.getActions());
  }

  @Test
  void testGetSummaryForRoleNameByCollectionId_Researcher_PR() {
    User user = new User();
    user.setUserId(1);

    DarCollectionSummary summary = createDarCollectionSummaryWithElections();
    summary.setLatestReferenceId("ref1");
    Integer collectionId = summary.getDarCollectionId();

    when(darCollectionSummaryDAO.getDarCollectionSummaryByCollectionId(collectionId))
        .thenReturn(summary);

    when(dataAccessRequestDAO.findDatasetApprovalsByDar("ref1"))
        .thenReturn(Set.of(1));

    DarCollectionSummary summaryResult = service.getSummaryForRoleByCollectionId(user,
        UserRoles.RESEARCHER, collectionId);

    assertNotNull(summaryResult);

    // Verify that the create_progress_report action is included
    Set<String> expectedActions = Set.of(
        DarCollectionActions.REVIEW.getValue(),
        DarCollectionActions.CREATE_PROGRESS_REPORT.getValue());
    assertTrue(
        summaryResult.getStatus().equalsIgnoreCase(DarCollectionStatus.IN_PROCESS.getValue()));
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

    when(darCollectionSummaryDAO.getDarCollectionSummaryForDACByCollectionId(user.getUserId(),
        List.of(), collectionId))
        .thenReturn(summary);
    when(datasetDAO.findDatasetListByDacIds(any())).thenReturn(List.of());

    DarCollectionSummary summaryResult = service.getSummaryForRoleByCollectionId(user,
        UserRoles.CHAIRPERSON, collectionId);
    assertNotNull(summaryResult);

    Set<String> expectedActions = Set.of(
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
    Vote vote = new Vote(1, null, user.getUserId(), null, null, 1, null, VoteType.DAC.getValue(),
        null, null);
    summary.addElection(electionOne);
    summary.addElection(electionTwo);
    summary.addDatasetId(datasetOne.getDatasetId());
    summary.addDatasetId(datasetTwo.getDatasetId());
    summary.setVotes(List.of(vote));

    when(darCollectionSummaryDAO.getDarCollectionSummaryForDACByCollectionId(user.getUserId(),
        List.of(), collectionId))
        .thenReturn(summary);

    DarCollectionSummary summaryResult = service.getSummaryForRoleByCollectionId(user,
        UserRoles.MEMBER, collectionId);
    assertNotNull(summaryResult);

    Set<String> expectedActions = Set.of(
        DarCollectionActions.VOTE.getValue());
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

    assertThrows(NotFoundException.class, () -> service.getSummaryForRoleByCollectionId(user, UserRoles.RESEARCHER, collectionId));
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

  @Test
  void testSendNewDARCollectionMessage() throws Exception {
    Dac dac = new Dac();
    dac.setDacId(1);
    User chairperson = createUserWithRole(UserRoles.CHAIRPERSON, dac.getDacId());
    dac.setChairpersons(List.of(chairperson));
    dac.setName("DAC-01");
    User researcher = createUserWithRole(UserRoles.RESEARCHER, null);

    Dataset d1 = createDataset(dac.getDacId());
    Dataset d2 = createDataset(dac.getDacId());

    DarCollection collection = new DarCollection();
    collection.setDarCode("01");
    collection.setDarCollectionId(1);
    collection.setDatasets(Set.of(d1, d2));
    collection.setCreateUserId(researcher.getUserId());
    DataAccessRequest dar = new DataAccessRequest();
    dar.setReferenceId(UUID.randomUUID().toString());
    dar.setSubmissionDate(Timestamp.from(Instant.now()));
    dar.setDatasetIds(List.of(d1.getDatasetId(), d2.getDatasetId()));
    collection.addDar(dar);

    when(darCollectionDAO.findDARCollectionByCollectionId(collection.getDarCollectionId()))
        .thenReturn(collection);
    when(userDAO.findUserById(researcher.getUserId())).thenReturn(researcher);
    when(userDAO.findUsersForDatasetsByRole(dar.getDatasetIds(),
        Collections.singletonList(UserRoles.CHAIRPERSON.getRoleName()))).thenReturn(Set.of(chairperson));
    when(dacDAO.findDacsForDatasetIds(dar.getDatasetIds())).thenReturn(Set.of(dac));
    when(datasetDAO.findDatasetsByIdList(dar.getDatasetIds())).thenReturn(List.of(d1, d2));
    service.sendNewDARCollectionMessage(collection.getDarCollectionId());
    verify(emailService)
        .sendNewDARRequestEmail(
            chairperson,
            Map.of(dac.getName(), List.of(d1.getDatasetIdentifier(), d2.getDatasetIdentifier())),
            researcher.getDisplayName(),
            collection.getDarCode());
  }

  @Test
  void testNotifySigningOfficialsOfDARSubmission_DAR() throws TemplateException, IOException {
    Dataset dataset = new Dataset();
    dataset.setDatasetId(1);
    dataset.setDataUse(new DataUseBuilder().setGeneralUse(true).build());
    when(datasetDAO.findDatasetsByIdList(List.of(dataset.getDatasetId()))).thenReturn(List.of(dataset));

    DarCollection collection = new DarCollection();
    collection.setDarCode("DAR-000123");
    collection.setDarCollectionId(1);
    DataAccessRequest dar = new DataAccessRequest();
    dar.setReferenceId(UUID.randomUUID().toString());
    dar.setDatasetIds(List.of(dataset.getDatasetId()));
    collection.addDar(dar);

    User researcher = createUserWithRole(UserRoles.RESEARCHER, null);
    collection.setCreateUserId(researcher.getUserId());
    researcher.setInstitutionId(1);
    User signingOfficial = createUserWithRole(UserRoles.SIGNINGOFFICIAL, null);
    signingOfficial.setEmailPreference(true);
    when(userDAO.getSOsByInstitution(researcher.getInstitutionId())).thenReturn(List.of(signingOfficial));

    service.notifySigningOfficialsOfDARSubmission(collection.getMostRecentDar(), researcher, collection.getDarCode());
    verify(emailService, never()).sendNewSoProgressReportSubmittedEmail(any(), any(), any(), any(), any());
    verify(emailService, times(1)).sendNewSoDARSubmittedEmail(any(), any(), any(), any(), any());
  }

  @Test
  void testNotifySigningOfficialsOfDARSubmission_PR() throws TemplateException, IOException {
    Dataset dataset = new Dataset();
    dataset.setDatasetId(1);
    dataset.setDataUse(new DataUseBuilder().setGeneralUse(true).build());
    when(datasetDAO.findDatasetsByIdList(List.of(dataset.getDatasetId()))).thenReturn(List.of(dataset));

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

    User researcher = createUserWithRole(UserRoles.RESEARCHER, null);
    collection.setCreateUserId(researcher.getUserId());
    researcher.setInstitutionId(1);
    User signingOfficial = createUserWithRole(UserRoles.SIGNINGOFFICIAL, null);
    signingOfficial.setEmailPreference(true);
    when(userDAO.getSOsByInstitution(researcher.getInstitutionId())).thenReturn(List.of(signingOfficial));

    service.notifySigningOfficialsOfDARSubmission(collection.getMostRecentDar(), researcher, collection.getDarCode());
    verify(emailService, times(1)).sendNewSoProgressReportSubmittedEmail(any(), any(), any(), any(), any());
    verify(emailService, never()).sendNewSoDARSubmittedEmail(any(), any(), any(), any(), any());
  }

  @Test
  void testNotifySigningOfficialsOfDARSubmission_NoInstitution() throws TemplateException, IOException {
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

    User researcher = createUserWithRole(UserRoles.RESEARCHER, null);
    collection.setCreateUserId(researcher.getUserId());

    service.notifySigningOfficialsOfDARSubmission(collection.getMostRecentDar(), researcher, collection.getDarCode());
    verify(emailService, never()).sendNewSoProgressReportSubmittedEmail(any(), any(), any(), any(), any());
    verify(emailService, never()).sendNewSoDARSubmittedEmail(any(), any(), any(), any(), any());
  }

  @Test
  void testNotifySigningOfficialsOfDARSubmission_SO_Disabled() throws TemplateException, IOException {
    Dataset dataset = new Dataset();
    dataset.setDatasetId(1);
    dataset.setDataUse(new DataUseBuilder().setGeneralUse(true).build());
    when(datasetDAO.findDatasetsByIdList(List.of(dataset.getDatasetId()))).thenReturn(List.of(dataset));

    DarCollection collection = new DarCollection();
    collection.setDarCode("DAR-000123");
    collection.setDarCollectionId(1);
    DataAccessRequest dar = new DataAccessRequest();
    dar.setReferenceId(UUID.randomUUID().toString());
    dar.setDatasetIds(List.of(dataset.getDatasetId()));
    collection.addDar(dar);

    User researcher = createUserWithRole(UserRoles.RESEARCHER, null);
    collection.setCreateUserId(researcher.getUserId());
    researcher.setInstitutionId(1);
    User signingOfficial = createUserWithRole(UserRoles.SIGNINGOFFICIAL, null);
    signingOfficial.setEmailPreference(false);
    when(userDAO.getSOsByInstitution(researcher.getInstitutionId())).thenReturn(List.of(signingOfficial));

    service.notifySigningOfficialsOfDARSubmission(collection.getMostRecentDar(), researcher, collection.getDarCode());
    verify(emailService, never()).sendNewSoProgressReportSubmittedEmail(any(), any(), any(), any(), any());
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

  private Dataset createDataset(Integer dacId) {
    Dataset dataset = new Dataset();
    dataset.setDatasetId(randomInt(1, 100000));
    dataset.setAlias(dataset.getDatasetId());
    dataset.setDatasetIdentifier();
    dataset.setDacId(dacId);
    dataset.setName(String.format("Dataset %s-%s", randomAlphabetic(10),
        dataset.getDatasetId()));
    return dataset;
  }

  private User createUserWithRole(UserRoles userRoles, Integer dacId) {
    User user = new User();
    user.setUserId(randomInt(1, 100000));
    user.setDisplayName(String.format("%s - %s", userRoles.getRoleName(), user.getUserId()));
    user.setEmail(String.format("%s@test.com", userRoles.getRoleName()));
    UserRole role = new UserRole(
        userRoles.getRoleId(),
        userRoles.getRoleName()
    );
    if (dacId != null) {
      role.setDacId(dacId);
    }
    user.setRoles(List.of(role));
    user.setEmailPreference(Boolean.TRUE);
    return user;
  }

}
