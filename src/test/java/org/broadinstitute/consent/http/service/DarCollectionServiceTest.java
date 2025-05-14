package org.broadinstitute.consent.http.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotAcceptableException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
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
import org.broadinstitute.consent.http.models.Dac;
import org.broadinstitute.consent.http.models.DarCollection;
import org.broadinstitute.consent.http.models.DarCollectionSummary;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
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
    initService();
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

    when(datasetDAO.findDatasetsByIdList(anyList())).thenReturn(new ArrayList<>(datasets));
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
        .collect(Collectors.toList());

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

    DarCollection canceledCollection = service.cancelDarCollectionAsResearcher(collection);
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

    assertThrows(BadRequestException.class, () -> service.cancelDarCollectionAsResearcher(collection));
  }

  @Test
  void testCancelDarCollectionAsResearcher_NoElections() {
    DataAccessRequest dar = new DataAccessRequest();
    dar.setReferenceId(UUID.randomUUID().toString());
    DataAccessRequestData data = new DataAccessRequestData();
    dar.setData(data);
    DarCollection collection = createMockCollections().get(0);
    collection.setDars(Map.of(dar.getReferenceId(), dar));
    when(electionDAO.findLastElectionsByReferenceIds(anyList())).thenReturn(List.of());
    when(darCollectionDAO.findDARCollectionByCollectionId(any())).thenReturn(collection);
    initService();

    service.cancelDarCollectionAsResearcher(collection);
    verify(electionDAO, times(1)).findLastElectionsByReferenceIds(anyList());
    verify(electionDAO, times(0)).updateElectionById(anyInt(), anyString(), any());
    verify(dataAccessRequestDAO, times(1)).cancelByReferenceIds(anyList());
    verify(darCollectionDAO, times(1)).findDARCollectionByCollectionId(anyInt());
  }

  @Test
  void testCancelDarCollectionAsResearcher_WithElections() {
    DataAccessRequest dar = new DataAccessRequest();
    dar.setReferenceId(UUID.randomUUID().toString());
    DataAccessRequestData data = new DataAccessRequestData();
    dar.setData(data);
    DarCollection collection = createMockCollections().get(0);
    collection.setDars(Map.of(dar.getReferenceId(), dar));
    Election election = createMockElection();
    election.setReferenceId(dar.getReferenceId());
    election.setStatus(ElectionStatus.OPEN.getValue());
    election.setElectionId(1);
    when(electionDAO.findLastElectionsByReferenceIds(anyList())).thenReturn(List.of(election));

    assertThrows(BadRequestException.class, () -> service.cancelDarCollectionAsResearcher(collection));
  }

  @Test
  void testCancelDarCollectionAsAdmin() {
    DataAccessRequest dar = new DataAccessRequest();
    dar.setReferenceId(UUID.randomUUID().toString());
    DataAccessRequestData data = new DataAccessRequestData();
    dar.setData(data);
    DarCollection collection = createMockCollections().get(0);
    collection.setDars(Map.of(dar.getReferenceId(), dar));
    Election election = createMockElection();
    election.setReferenceId(dar.getReferenceId());
    election.setStatus(ElectionStatus.OPEN.getValue());
    election.setElectionId(1);
    when(electionDAO.findOpenElectionsByReferenceIds(anyList())).thenReturn(List.of(election));
    when(darCollectionDAO.findDARCollectionByCollectionId(collection.getDarCollectionId())).thenReturn(collection);

    service.cancelDarCollectionElectionsAsAdmin(collection);
    verify(electionDAO, times(1)).findOpenElectionsByReferenceIds(anyList());
    verify(electionDAO, times(1)).updateElectionById(anyInt(), anyString(), any());
    verify(dataAccessRequestDAO, times(0)).cancelByReferenceIds(anyList());
    verify(darCollectionDAO, times(1)).findDARCollectionByCollectionId(anyInt());
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
    collection.setDars(Map.of(dar.getReferenceId(), dar));
    Election election = createMockElection();
    election.setReferenceId(dar.getReferenceId());
    election.setStatus(ElectionStatus.OPEN.getValue());
    election.setElectionId(1);
    when(datasetDAO.findDatasetIdsByDACUserId(anyInt())).thenReturn(
        List.of(dataset.getDatasetId()));
    when(electionDAO.findOpenElectionsByReferenceIds(anyList())).thenReturn(List.of(election));
    when(darCollectionDAO.findDARCollectionByCollectionId(collection.getDarCollectionId())).thenReturn(collection);

    service.cancelDarCollectionElectionsAsChair(collection, user);
    verify(datasetDAO, times(1)).findDatasetIdsByDACUserId(anyInt());
    verify(electionDAO, times(1)).findOpenElectionsByReferenceIds(anyList());
    verify(electionDAO, times(1)).updateElectionById(anyInt(), anyString(), any());
    verify(dataAccessRequestDAO, times(0)).cancelByReferenceIds(anyList());
    verify(darCollectionDAO, times(1)).findDARCollectionByCollectionId(anyInt());
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
    collection.setDars(Map.of(dar.getReferenceId(), dar));
    Election election = createMockElection();
    election.setReferenceId(dar.getReferenceId());
    election.setStatus(ElectionStatus.OPEN.getValue());
    election.setElectionId(1);
    when(datasetDAO.findDatasetIdsByDACUserId(anyInt())).thenReturn(List.of());

    service.cancelDarCollectionElectionsAsChair(collection, user);
    verify(datasetDAO, times(1)).findDatasetIdsByDACUserId(anyInt());
    verify(electionDAO, times(0)).findLastElectionsByReferenceIds(anyList());
    verify(electionDAO, times(0)).updateElectionById(anyInt(), anyString(), any());
    verify(dataAccessRequestDAO, times(0)).cancelByReferenceIds(anyList());
    verify(darCollectionDAO, times(0)).findDARCollectionByCollectionId(anyInt());
  }

  @Test
  void testCreateElectionsForDarCollection() throws Exception {
    User user = new User();
    user.setEmail("email");
    DataAccessRequest dar = new DataAccessRequest();
    dar.setReferenceId(UUID.randomUUID().toString());
    DarCollection collection = createMockCollections().get(0);
    collection.setDars(Map.of(dar.getReferenceId(), dar));
    when(darCollectionServiceDAO.createElectionsForDarByUser(any(), any())).thenReturn(
        List.of("electionId"));
    when(voteDAO.findVoteUsersByElectionReferenceIdList(any())).thenReturn(List.of(new User()));

    service.createElectionsForDarCollection(user, collection);
    verify(darCollectionServiceDAO, times(1)).createElectionsForDarByUser(any(), eq(dar));
    verify(voteDAO, times(1)).findVoteUsersByElectionReferenceIdList(any());
    verify(darCollectionDAO, times(1)).findDARCollectionByCollectionId(any());
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
    collection.setDars(Map.of(dar.getReferenceId(), dar, progressReport.getReferenceId(), progressReport));

    when(darCollectionServiceDAO.createElectionsForDarByUser(any(), any())).thenReturn(
        List.of("electionId"));
    when(voteDAO.findVoteUsersByElectionReferenceIdList(any())).thenReturn(List.of(new User()));

    service.createElectionsForDarCollection(user, collection);
    verify(darCollectionServiceDAO, times(1)).createElectionsForDarByUser(any(), eq(progressReport));
    verify(voteDAO, times(1)).findVoteUsersByElectionReferenceIdList(any());
    verify(darCollectionDAO, times(1)).findDARCollectionByCollectionId(any());
    verify(emailService, times(1)).sendProgressReportNewCollectionElectionMessage(any(), any());
  }

  @Test
  void testCreateElectionsForDarCollectionEmpty() {
    User user = new User();
    user.setEmail("email");
    DataAccessRequest dar = new DataAccessRequest();
    dar.setReferenceId(UUID.randomUUID().toString());
    DarCollection collection = createMockCollections().get(0);
    collection.setDars(Map.of(dar.getReferenceId(), dar));

    assertThrows(IllegalStateException.class, () -> service.createElectionsForDarCollection(user, collection));
  }

  @Test
  void testCreateElectionsForDarCollectionVoteUsersException() throws Exception {
    User user = new User();
    user.setEmail("email");
    DataAccessRequest dar = new DataAccessRequest();
    dar.setReferenceId(UUID.randomUUID().toString());
    DarCollection collection = createMockCollections().get(0);
    collection.setDars(Map.of(dar.getReferenceId(), dar));
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

    when(electionDAO.findElectionsByReferenceIds(any())).thenReturn(new ArrayList<>());
    when(darCollectionDAO.findDARCollectionByCollectionId(any())).thenReturn(collection);

    Integer collectionId = collection.getDarCollectionId();

    System.out.println(collectionId);
    service.deleteByCollectionId(user, collectionId);

    // verify each DAR was deleted
    verify(dataAccessRequestDAO, times(1)).deleteByReferenceIds(any());
    verify(dataAccessRequestDAO, times(1)).deleteDARDatasetRelationByReferenceIds(any());
    verify(matchDAO, times(1)).deleteMatchesByPurposeIds(any());
    // verify overarching collection was deleted
    verify(darCollectionDAO, times(1)).deleteByCollectionId(collectionId);
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

    Election e = createMockElection();
    when(electionDAO.findElectionsByReferenceIds(any())).thenReturn(new ArrayList<>() {{
      add(e);
    }});
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

    Election e = createMockElection();
    when(electionDAO.findElectionsByReferenceIds(any())).thenReturn(new ArrayList<>() {{
      add(e);
    }});
    when(darCollectionDAO.findDARCollectionByCollectionId(any())).thenReturn(collection);

    Integer collectionId = collection.getDarCollectionId();

    service.deleteByCollectionId(user, collectionId);

    verify(dataAccessRequestDAO, times(1)).deleteByReferenceIds(any());
    verify(dataAccessRequestDAO, times(1)).deleteDARDatasetRelationByReferenceIds(any());
    verify(matchDAO, times(1)).deleteMatchesByPurposeIds(any());
    verify(darCollectionDAO, times(1)).deleteByCollectionId(collectionId);
    verify(electionDAO, times(1)).deleteElectionsByIds(any());
    verify(voteDAO, times(1)).deleteVotesByReferenceIds(any());
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

    //summaryOne -> in review (elections present)
    //summaryTwo -> no elections
    //summaryThree -> no elections, canceled
    //summaryThree -> draft
    //summaryFour -> closed election, approved datasets

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
    Dataset datasetSix = new Dataset();
    datasetSix.setDatasetId(6);
    summaryFour.addDatasetId(datasetSix.getDatasetId());
    Election electionTwo = new Election();
    electionOne.setElectionId(2);
    electionOne.setStatus(ElectionStatus.CLOSED.getValue());
    summaryFour.addElection(electionTwo);
    summaryFour.setReferenceIds(Set.of("ref1"));

    DataAccessRequest draft = new DataAccessRequest();
    draft.setCreateDate(new Timestamp(new Date().getTime()));
    DataAccessRequestData data = new DataAccessRequestData();
    data.setProjectTitle(randomAlphabetic(10));
    data.setCreateDate(draft.getCreateDate().getTime());
    draft.setData(data);
    List<DarCollectionSummary> mockSummaries = new ArrayList<>();
    mockSummaries.add(summaryOne);
    mockSummaries.add(summaryTwo);
    mockSummaries.add(summaryThree);
    mockSummaries.add(summaryFour);
    when(dataAccessRequestDAO.findAllDraftsByUserId(any())).thenReturn(List.of(draft));
    when(darCollectionSummaryDAO.getDarCollectionSummariesForResearcher(any())).thenReturn(
        mockSummaries);
    when(dataAccessRequestDAO.findDatasetApprovalsByDars(List.of())).thenReturn(Set.of());
    when(dataAccessRequestDAO.findDatasetApprovalsByDars(List.of("ref1")))
        .thenReturn(Set.of(datasetSix.getDatasetId()));

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
    assertEquals(testOne.getActions(), expectedOneActions);

    DarCollectionSummary testTwo = summaries.get(1);
    Set<String> expectedTwoActions = Set.of(
        DarCollectionActions.REVIEW.getValue(),
        DarCollectionActions.CANCEL.getValue()
    );
    assertTrue(
        testTwo.getStatus().equalsIgnoreCase(DarCollectionStatus.SUBMITTED.getValue()));
    assertEquals(testTwo.getActions(), expectedTwoActions);

    DarCollectionSummary testThree = summaries.get(2);
    Set<String> expectedThreeActions = Set.of(
        DarCollectionActions.REVIEW.getValue(),
        DarCollectionActions.REVISE.getValue());
    assertTrue(
        testThree.getStatus().equalsIgnoreCase(DarCollectionStatus.CANCELED.getValue()));
    assertEquals(testThree.getActions(), expectedThreeActions);

    DarCollectionSummary testFour = summaries.get(3);
    Set<String> expectedFourActions = Set.of(
        DarCollectionActions.REVIEW.getValue(),
        DarCollectionActions.CREATE_PROGRESS_REPORT.getValue());
    assertTrue(
        testFour.getStatus().equalsIgnoreCase(DarCollectionStatus.COMPLETE.getValue()));
    assertEquals(testFour.getActions(), expectedFourActions);

    DarCollectionSummary testDraft = summaries.get(4);
    Set<String> expectedDraftActions = Set.of(
        DarCollectionActions.RESUME.getValue(),
        DarCollectionActions.DELETE.getValue());
    assertTrue(
        testDraft.getStatus().equalsIgnoreCase(DarCollectionStatus.DRAFT.getValue()));
    assertEquals(testDraft.getActions(), expectedDraftActions);
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
    Integer collectionId = summary.getDarCollectionId();

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
    summary.setReferenceIds(Set.of("ref1"));
    Integer collectionId = summary.getDarCollectionId();

    when(darCollectionSummaryDAO.getDarCollectionSummaryByCollectionId(collectionId))
        .thenReturn(summary);

    when(dataAccessRequestDAO.findDatasetApprovalsByDars(List.of("ref1")))
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
  void testSendNewDARCollectionMessage() {
    Dac dac = new Dac();
    dac.setDacId(1);
    User chairperson = createUserWithRole(UserRoles.CHAIRPERSON, dac.getDacId());
    dac.setChairpersons(List.of(chairperson));
    dac.setName("DAC-01");

    Dataset d1 = createDataset(dac.getDacId());
    Dataset d2 = createDataset(dac.getDacId());

    DarCollection collection = new DarCollection();
    collection.setDarCode("01");
    collection.setDarCollectionId(1);
    collection.setDatasets(Set.of(d1, d2));
    DataAccessRequest dar = new DataAccessRequest();
    dar.setReferenceId(UUID.randomUUID().toString());
    dar.setSubmissionDate(Timestamp.from(Instant.now()));
    dar.setDatasetIds(List.of(d1.getDatasetId(), d2.getDatasetId()));
    collection.setDars(Map.of(dar.getReferenceId(), dar));

    assertDoesNotThrow(() -> service.sendNewDARCollectionMessage(collection.getDarCollectionId()));
  }

  private DarCollection generateMockDarCollection(Set<Dataset> datasets) {
    DarCollection collection = new DarCollection();
    Map<String, DataAccessRequest> dars = new HashMap<>();
    DataAccessRequest darOne = generateMockDarWithDatasetId(datasets);
    DataAccessRequest darTwo = generateMockDarWithDatasetId(datasets);
    dars.put(darOne.getReferenceId(), darOne);
    dars.put(darTwo.getReferenceId(), darTwo);
    collection.setDars(dars);
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

  private void initService() {
    service = new DarCollectionService(darCollectionDAO, darCollectionServiceDAO, datasetDAO,
        electionDAO, dataAccessRequestDAO, emailService, voteDAO, matchDAO,
        darCollectionSummaryDAO, userDAO, dacDAO);
  }

  private List<DarCollection> createMockCollections() {
    return IntStream.rangeClosed(1, 1)
        .mapToObj(
            i -> {
              DarCollection collection = new DarCollection();
              collection.setDarCollectionId(i);
              collection.setDarCode(randomAlphanumeric(5));
              collection.setCreateUserId(1);
              return collection;
            })
        .toList();
  }

  private Election createMockElection() {
    Election election = new Election();
    election.setElectionId(1);
    election.setReferenceId(UUID.randomUUID().toString());
    return election;
  }

  private Dataset createDataset(Integer dacId) {
    Dataset dataset = new Dataset();
    dataset.setDatasetId(RandomUtils.nextInt(1, 100000));
    dataset.setAlias(dataset.getDatasetId());
    dataset.setDatasetIdentifier();
    dataset.setDacId(dacId);
    dataset.setName(String.format("Dataset %s-%s", RandomStringUtils.randomAlphabetic(10),
        dataset.getDatasetId()));
    return dataset;
  }

  private User createUserWithRole(UserRoles userRoles, Integer dacId) {
    User user = new User();
    user.setUserId(RandomUtils.nextInt(1, 100000));
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
