package org.broadinstitute.consent.http.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotAcceptableException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.broadinstitute.consent.http.AbstractTestHelper;
import org.broadinstitute.consent.http.db.DarCollectionDAO;
import org.broadinstitute.consent.http.db.DarCollectionSummaryDAO;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.MatchDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.enumeration.DarCollectionActions;
import org.broadinstitute.consent.http.enumeration.DarCollectionStatus;
import org.broadinstitute.consent.http.enumeration.DarStatus;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DarCollection;
import org.broadinstitute.consent.http.models.DarCollectionSummary;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.User;
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

  @BeforeEach
  void setUp() {
    service = new DarCollectionService(darCollectionDAO, darCollectionServiceDAO, datasetDAO,
        electionDAO, dataAccessRequestDAO, emailService, voteDAO, matchDAO,
        darCollectionSummaryDAO);
  }

  @Test
  void testAddDatasetsToCollection() {
    List<DarCollection> collections = new ArrayList<>();
    Set<Dataset> datasets = new HashSet<>();
    collections.add(generateMockDarCollection(datasets));
    List<Integer> datasetIds = datasets.stream()
        .map(Dataset::getDatasetId)
        .sorted()
        .toList();

    when(datasetDAO.findDatasetsByIdList(anyList())).thenReturn(List.copyOf(datasets));
    when(dataAccessRequestDAO.findAllDARDatasetRelations(any())).thenReturn(datasetIds);

    collections = service.addDatasetsToCollections(collections, List.of());
    assertEquals(1, collections.size());

    DarCollection collection = collections.get(0);
    Set<Dataset> datasetsFromCollection = collection.getDatasets();
    assertEquals(datasetIds.size(), datasetsFromCollection.size());

    List<Integer> collectionDatasetIds = datasetsFromCollection.stream()
        .map(Dataset::getDatasetId)
        .sorted()
        .toList();
    assertEquals(datasetIds, collectionDatasetIds);
  }

  @Test
  void testAddDatasetsToCollectionsWithFilterDatasetIds() {
    List<DarCollection> collections = new ArrayList<>();
    Set<Dataset> datasets = new HashSet<>();
    // need a minimal version of a collection with an array of datasetIds
    collections.add(generateMockDarCollection(datasets));
    List<Integer> datasetIds = datasets.stream()
        .map(Dataset::getDatasetId)
        .sorted()
        .toList();

    Dataset dataset = new Dataset();
    dataset.setDatasetId(datasetIds.get(0));

    // mocking out findDatasetsByIdList to only return one of the datasets
    when(datasetDAO.findDatasetsByIdList(List.of(dataset.getDatasetId()))).thenReturn(
        List.of(dataset));
    when(dataAccessRequestDAO.findAllDARDatasetRelations(any())).thenReturn(datasetIds);

    collections = service.addDatasetsToCollections(collections, List.of(dataset.getDatasetId()));
    assertEquals(1, collections.size());

    DarCollection collection = collections.get(0);
    Set<Dataset> datasetsFromCollection = collection.getDatasets();
    assertEquals(1, datasetsFromCollection.size());

    List<Integer> collectionDatasetIds = datasetsFromCollection.stream()
        .map(Dataset::getDatasetId)
        .sorted()
        .toList();
    assertEquals(dataset.getDatasetId(), collectionDatasetIds.get(0));
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
    DarCollection canceledCollection = service.cancelDarCollectionElections(user, collection, UserRoles.RESEARCHER);
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
        () -> service.cancelDarCollectionElections(user, collection, UserRoles.RESEARCHER));
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
    service.cancelDarCollectionElections(user, collection, UserRoles.RESEARCHER);
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
        () -> service.cancelDarCollectionElections(user, collection, UserRoles.RESEARCHER));
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
    when(darCollectionSummaryDAO.getDarCollectionSummaryByCollectionId(collection.getDarCollectionId()))
        .thenReturn(new DarCollectionSummary());

    service.cancelDarCollectionElections(new User(), collection, UserRoles.ADMIN);
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
    when(darCollectionSummaryDAO.getDarCollectionSummaryByCollectionId(collection.getDarCollectionId()))
        .thenReturn(new DarCollectionSummary());

    service.cancelDarCollectionElections(user, collection, UserRoles.CHAIRPERSON);
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
    when(darCollectionSummaryDAO.getDarCollectionSummaryByCollectionId(collection.getDarCollectionId()))
        .thenReturn(new DarCollectionSummary());

    service.cancelDarCollectionElections(user, collection, UserRoles.CHAIRPERSON);
    verify(datasetDAO).findDatasetIdsByDACUserId(anyInt());
    verifyNoMoreInteractions(electionDAO);
    verifyNoInteractions(dataAccessRequestDAO, darCollectionDAO);
  }

  // Tests: getSummariesForRole, getSummaryForRoleByCollectionId: admin, signing official, chairperson, with progress report

  @Test
  void cancelDarCollectionElections_ProgressReport() {
    DarCollection collection = createMockCollections().get(0);
    collection.addDar(new DataAccessRequest());
    DarCollectionSummary summary = new DarCollectionSummary();
    summary.addParentChildRelationship(123, "456");
    summary.setSubmissionDate(new Timestamp(0));
    when(darCollectionSummaryDAO.getDarCollectionSummaryByCollectionId(
            collection.getDarCollectionId()))
        .thenReturn(summary);
    assertThrows(
        BadRequestException.class,
        () -> service.cancelDarCollectionElections(null, collection, UserRoles.RESEARCHER));
  }

  @Test
  void cancelDarCollectionElections_NoDars() {
    DarCollection collection = new DarCollection();
    collection.setDarCollectionId(123);
    when(darCollectionSummaryDAO.getDarCollectionSummaryByCollectionId(
            collection.getDarCollectionId()))
        .thenReturn(new DarCollectionSummary());
    var newCollection = service.cancelDarCollectionElections(null, collection, UserRoles.RESEARCHER);
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
    when(darCollectionServiceDAO.createElectionsForDarCollection(any(), any())).thenReturn(
        List.of("electionId"));
    when(voteDAO.findVoteUsersByElectionReferenceIdList(any())).thenReturn(List.of(new User()));

    service.createElectionsForDarCollection(user, collection);
    verify(darCollectionServiceDAO).createElectionsForDarCollection(any(), any());
    verify(voteDAO).findVoteUsersByElectionReferenceIdList(any());
    verify(emailService).sendDarNewCollectionElectionMessage(any(), any());
    verify(darCollectionDAO).findDARCollectionByCollectionId(any());
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
    when(darCollectionServiceDAO.createElectionsForDarCollection(user, collection)).thenReturn(
        electionIds);
    when(voteDAO.findVoteUsersByElectionReferenceIdList(electionIds)).thenThrow(
        IllegalArgumentException.class);

    service.createElectionsForDarCollection(user, collection);
    verify(darCollectionServiceDAO).createElectionsForDarCollection(user, collection);
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
  void testProcessDarCollectionSummariesForResearcher() {

    // summaryOne -> in review (elections present)
    // summaryTwo -> no elections
    // summaryThree -> no elections, canceled
    // summaryFour -> dar collection is a progress report
    // summaryFive -> draft

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

    DarCollectionSummary summaryTwo = new DarCollectionSummary();
    Dataset datasetThree = new Dataset();
    datasetThree.setDatasetId(3);
    Dataset datasetFour = new Dataset();
    datasetFour.setDatasetId(4);
    summaryTwo.addDatasetId(datasetThree.getDatasetId());
    summaryTwo.addDatasetId(datasetFour.getDatasetId());

    DarCollectionSummary summaryThree = new DarCollectionSummary();
    Dataset datasetFive = new Dataset();
    datasetFive.setDatasetId(5);
    summaryThree.addDatasetId(datasetFive.getDatasetId());
    summaryThree.addStatus(DarStatus.CANCELED.getValue(), randomAlphabetic(3));

    DarCollectionSummary summaryFour = new DarCollectionSummary();
    {
      summaryFour.setSubmissionDate(new Timestamp(0));
      summaryFour.addParentChildRelationship(123, "456");
      Election election = new Election();
      election.setElectionId(234);
      election.setStatus(ElectionStatus.OPEN.getValue());
      summaryFour.addElection(election);
    }

    DataAccessRequest draft = new DataAccessRequest();
    draft.setCreateDate(new Timestamp(new Date().getTime()));
    DataAccessRequestData data = new DataAccessRequestData();
    data.setProjectTitle(randomAlphabetic(10));
    data.setCreateDate(draft.getCreateDate().getTime());
    draft.setData(data);
    when(dataAccessRequestDAO.findAllDraftsByUserId(any())).thenReturn(List.of(draft));
    when(darCollectionSummaryDAO.getDarCollectionSummariesForResearcher(any())).thenReturn(
        List.of(summaryOne, summaryTwo, summaryThree, summaryFour));

    List<DarCollectionSummary> summaries = service.getSummariesForRole(user,
        UserRoles.RESEARCHER);
    assertNotNull(summaries);
    assertEquals(5, summaries.size());

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
    assertEquals(DarCollectionStatus.IN_PROCESS.getValue(), testFour.getStatus());
    assertEquals(Set.of(DarCollectionActions.REVIEW.getValue()), testFour.getActions());

    DarCollectionSummary testDraft = summaries.get(4);
    Set<String> expectedDraftActions = Set.of(
        DarCollectionActions.RESUME.getValue(),
        DarCollectionActions.DELETE.getValue());
    assertEquals(DarCollectionStatus.DRAFT.getValue(), testDraft.getStatus());
    assertEquals(expectedDraftActions, testDraft.getActions());
  }

  @Test
  void testProcessDarCollectionSummariesForAdmin() {
    // summaryOne -> all elections present and open
    // summaryTwo -> mix of open elections : absent/non-open elections (in process)
    // summaryThree -> all canceled elections (Complete)
    // summaryFour -> no elections (unreviewed)
    // summaryFive -> dar collection is a progress report

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

    DarCollectionSummary summaryFive = new DarCollectionSummary();
    {
      summaryFive.setSubmissionDate(new Timestamp(0));
      summaryFive.addParentChildRelationship(123, "456");
      Election election = new Election();
      election.setElectionId(234);
      election.setStatus(ElectionStatus.OPEN.getValue());
      summaryFive.addElection(election);
    }

    when(darCollectionSummaryDAO.getDarCollectionSummariesForAdmin())
        .thenReturn(List.of(summaryOne, summaryTwo, summaryThree, summaryFour, summaryFive));

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

    DarCollectionSummary testFive = summaries.get(4);
    assertEquals(DarCollectionStatus.IN_PROCESS.getValue(), testFive.getStatus());
    assertTrue(testFive.getActions().isEmpty());
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
    // summaryOne -> all elections present and open
    // summaryTwo -> mix of open elections : absent/canceled elections (in process)
    // summaryThree -> all canceled elections (Complete)
    // summaryFour -> no elections (unreviewed)
    // summaryFive -> mix of open : absent/closed elections (in process, but cancel action does not appear)
    // summarySix -> all closed elections (complete, only open available)
    // summarySeven -> dar collection is a progress report

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

    DarCollectionSummary summarySeven = new DarCollectionSummary();
    {
      summarySeven.setSubmissionDate(new Timestamp(0));
      summarySeven.addParentChildRelationship(123, "456");
      Election election = new Election();
      election.setElectionId(234);
      election.setStatus(ElectionStatus.OPEN.getValue());
      summarySeven.addElection(election);
    }

    when(darCollectionSummaryDAO.getDarCollectionSummariesForDAC(any(), any()))
        .thenReturn(
            List.of(summaryOne, summaryTwo, summaryThree, summaryFour, summaryFive, summarySix, summarySeven));
    when(datasetDAO.findDatasetListByDacIds(any())).thenReturn(List.of(datasetOne,
        datasetTwo,
        datasetThree,
        datasetFour,
        datasetFive,
        datasetFive,
        datasetSix));

    List<DarCollectionSummary> summaries = service.getSummariesForRole(user,
        UserRoles.CHAIRPERSON);
    assertEquals(7, summaries.size());

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

    DarCollectionSummary testSeven = summaries.get(6);
    assertEquals(DarCollectionStatus.IN_PROCESS.getValue(), testSeven.getStatus());
    assertEquals(Set.of(DarCollectionActions.VOTE.getValue()), testSeven.getActions());
  }

  @Test
  void testGetSummaryForRoleByCollectionId_SO() {
    User user = new User();
    user.setUserId(1);

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
    summary.addElection(electionOne);
    summary.addElection(electionTwo);
    summary.addDatasetId(datasetOne.getDatasetId());
    summary.addDatasetId(datasetTwo.getDatasetId());

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
    summary.addElection(electionOne);
    summary.addElection(electionTwo);
    summary.addDatasetId(datasetOne.getDatasetId());
    summary.addDatasetId(datasetTwo.getDatasetId());

    when(darCollectionSummaryDAO.getDarCollectionSummaryByCollectionId(collectionId))
        .thenReturn(summary);

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
    summary.addElection(electionOne);
    summary.addElection(electionTwo);
    summary.addDatasetId(datasetOne.getDatasetId());
    summary.addDatasetId(datasetTwo.getDatasetId());

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
  void testGetSummaryForRoleByCollectionId_Chair() {
    Dac dac = new Dac();
    dac.setDacId(1);
    User user = new User();
    user.setUserId(1);
    user.setChairpersonRoleWithDAC(dac.getDacId());
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

}
