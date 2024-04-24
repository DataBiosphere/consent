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
import static org.mockito.Mockito.when;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotAcceptableException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;
import java.sql.Timestamp;
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
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
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
import org.broadinstitute.consent.http.enumeration.ElectionType;
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
class DarCollectionServiceTest {

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
  public void setUp() {
    initService();
  }

  @Test
  void testAddDatasetsToCollection() {
    List<DarCollection> collections = new ArrayList<>();
    Set<Dataset> datasets = new HashSet<>();
    collections.add(generateMockDarCollection(datasets));
    List<Integer> datasetIds = datasets.stream()
        .map(Dataset::getDataSetId)
        .sorted()
        .collect(Collectors.toList());

    when(datasetDAO.findDatasetWithDataUseByIdList(anyList())).thenReturn(datasets);
    when(dataAccessRequestDAO.findAllDARDatasetRelations(any())).thenReturn(datasetIds);

    collections = service.addDatasetsToCollections(collections, List.of());
    assertEquals(1, collections.size());

    DarCollection collection = collections.get(0);
    Set<Dataset> datasetsFromCollection = collection.getDatasets();
    assertEquals(datasetIds.size(), datasetsFromCollection.size());

    List<Integer> collectionDatasetIds = datasetsFromCollection.stream()
        .map(Dataset::getDataSetId)
        .sorted()
        .collect(Collectors.toList());
    assertEquals(datasetIds, collectionDatasetIds);
  }

  @Test
  void testAddDatasetsToCollectionsWithFilterDatasetIds() {
    List<DarCollection> collections = new ArrayList<>();
    Set<Dataset> datasets = new HashSet<>();
    // need a minimal version of a collection with an array of datasetIds
    collections.add(generateMockDarCollection(datasets));
    List<Integer> datasetIds = datasets.stream()
        .map(Dataset::getDataSetId)
        .sorted()
        .collect(Collectors.toList());

    Dataset dataset = new Dataset();
    dataset.setDataSetId(datasetIds.get(0));

    // mocking out findDatasetWithDataUseByIdList to only return one of the datasets
    when(datasetDAO.findDatasetWithDataUseByIdList(List.of(dataset.getDataSetId()))).thenReturn(
        new HashSet<>(List.of(dataset)));
    when(dataAccessRequestDAO.findAllDARDatasetRelations(any())).thenReturn(datasetIds);


    collections = service.addDatasetsToCollections(collections, List.of(dataset.getDataSetId()));
    assertEquals(1, collections.size());

    DarCollection collection = collections.get(0);
    Set<Dataset> datasetsFromCollection = collection.getDatasets();
    assertEquals(1, datasetsFromCollection.size());

    List<Integer> collectionDatasetIds = datasetsFromCollection.stream()
        .map(Dataset::getDataSetId)
        .sorted()
        .toList();
    assertEquals(dataset.getDataSetId(), collectionDatasetIds.get(0));
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

    assertThrows(BadRequestException.class, () -> {
      service.cancelDarCollectionAsResearcher(collection);
    });
  }

  @Test
  void testCancelDarCollectionAsResearcher_NoElections() {
    DataAccessRequest dar = new DataAccessRequest();
    dar.setReferenceId(UUID.randomUUID().toString());
    DataAccessRequestData data = new DataAccessRequestData();
    dar.setData(data);
    DarCollection collection = createMockCollections(1).get(0);
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
    DarCollection collection = createMockCollections(1).get(0);
    collection.setDars(Map.of(dar.getReferenceId(), dar));
    Election election = createMockElection();
    election.setReferenceId(dar.getReferenceId());
    election.setStatus(ElectionStatus.OPEN.getValue());
    election.setElectionId(1);
    when(electionDAO.findLastElectionsByReferenceIds(anyList())).thenReturn(List.of(election));

    assertThrows(BadRequestException.class, () -> {
      service.cancelDarCollectionAsResearcher(collection);
    });
  }

  @Test
  void testCancelDarCollectionAsAdmin() {
    DataAccessRequest dar = new DataAccessRequest();
    dar.setReferenceId(UUID.randomUUID().toString());
    DataAccessRequestData data = new DataAccessRequestData();
    dar.setData(data);
    DarCollection collection = createMockCollections(1).get(0);
    collection.setDars(Map.of(dar.getReferenceId(), dar));
    Election election = createMockElection();
    election.setReferenceId(dar.getReferenceId());
    election.setStatus(ElectionStatus.OPEN.getValue());
    election.setElectionId(1);
    when(electionDAO.findOpenElectionsByReferenceIds(anyList())).thenReturn(List.of(election));

    service.cancelDarCollectionElectionsAsAdmin(collection);
    verify(electionDAO, times(1)).findOpenElectionsByReferenceIds(anyList());
    verify(electionDAO, times(1)).updateElectionById(anyInt(), anyString(), any());
    verify(dataAccessRequestDAO, times(0)).cancelByReferenceIds(anyList());
    verify(darCollectionDAO, times(1)).findDARCollectionByCollectionId(anyInt());
  }

  @Test
  void testCancelDarCollectionAsChair_ChairHasDatasets() {
    User user = new User();
    user.setEmail("email");
    Dataset dataset = new Dataset();
    dataset.setDataSetId(1);
    DataAccessRequest dar = new DataAccessRequest();
    dar.setReferenceId(UUID.randomUUID().toString());
    DataAccessRequestData data = new DataAccessRequestData();
    dar.addDatasetId(dataset.getDataSetId());
    dar.setData(data);
    DarCollection collection = createMockCollections(1).get(0);
    collection.setDars(Map.of(dar.getReferenceId(), dar));
    Election election = createMockElection();
    election.setReferenceId(dar.getReferenceId());
    election.setStatus(ElectionStatus.OPEN.getValue());
    election.setElectionId(1);
    when(datasetDAO.findDatasetsByAuthUserEmail(anyString())).thenReturn(List.of(dataset));
    when(electionDAO.findOpenElectionsByReferenceIds(anyList())).thenReturn(List.of(election));

    service.cancelDarCollectionElectionsAsChair(collection, user);
    verify(datasetDAO, times(1)).findDatasetsByAuthUserEmail(anyString());
    verify(electionDAO, times(1)).findOpenElectionsByReferenceIds(anyList());
    verify(electionDAO, times(1)).updateElectionById(anyInt(), anyString(), any());
    verify(dataAccessRequestDAO, times(0)).cancelByReferenceIds(anyList());
    verify(darCollectionDAO, times(1)).findDARCollectionByCollectionId(anyInt());
  }

  @Test
  void testCancelDarCollectionAsChair_ChairHasNoDatasets() {
    User user = new User();
    user.setEmail("email");
    Dataset dataset = new Dataset();
    dataset.setDataSetId(1);
    DataAccessRequest dar = new DataAccessRequest();
    dar.setReferenceId(UUID.randomUUID().toString());
    DataAccessRequestData data = new DataAccessRequestData();
    dar.addDatasetId(dataset.getDataSetId());
    dar.setData(data);
    DarCollection collection = createMockCollections(1).get(0);
    collection.setDars(Map.of(dar.getReferenceId(), dar));
    Election election = createMockElection();
    election.setReferenceId(dar.getReferenceId());
    election.setStatus(ElectionStatus.OPEN.getValue());
    election.setElectionId(1);
    when(datasetDAO.findDatasetsByAuthUserEmail(anyString())).thenReturn(List.of());

    service.cancelDarCollectionElectionsAsChair(collection, user);
    verify(datasetDAO, times(1)).findDatasetsByAuthUserEmail(anyString());
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
    DarCollection collection = createMockCollections(1).get(0);
    collection.setDars(Map.of(dar.getReferenceId(), dar));
    Election election = createMockElection();
    election.setReferenceId(dar.getReferenceId());
    election.setStatus(ElectionStatus.CANCELED.getValue());
    election.setElectionType(ElectionType.DATA_ACCESS.getValue());
    election.setElectionId(1);
    when(voteDAO.findVoteUsersByElectionReferenceIdList(any())).thenReturn(List.of(new User()));

    service.createElectionsForDarCollection(user, collection);
    verify(darCollectionServiceDAO, times(1)).createElectionsForDarCollection(any(), any());
    verify(voteDAO, times(1)).findVoteUsersByElectionReferenceIdList(any());
    verify(emailService, times(1)).sendDarNewCollectionElectionMessage(any(), any());
    verify(darCollectionDAO, times(1)).findDARCollectionByCollectionId(any());
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
    verify(electionDAO, times(0)).deleteElectionsFromAccessRPs(any());
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

    assertThrows(NotAcceptableException.class, () -> {
      service.deleteByCollectionId(user, collectionId);
    });
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
    verify(electionDAO, times(1)).deleteElectionsFromAccessRPs(any());
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

    assertThrows(NotAuthorizedException.class, () -> {
      service.deleteByCollectionId(user, collectionId);
    });
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

    assertThrows(NotFoundException.class, () -> {
      service.deleteByCollectionId(user, collectionId);
    });
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
    datasetOne.setDataSetId(1);
    summary.addElection(electionOne);
    summary.addDatasetId(datasetOne.getDataSetId());
    when(darCollectionSummaryDAO.getDarCollectionSummariesForSO(any())).thenReturn(
        List.of(summary));

    List<DarCollectionSummary> summaries = service.getSummariesForRoleName(user,
        UserRoles.SIGNINGOFFICIAL.getRoleName());
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
    datasetOne.setDataSetId(1);
    Dataset datasetTwo = new Dataset();
    datasetTwo.setDataSetId(2);
    summary.addElection(electionOne);
    summary.addElection(electionTwo);
    summary.addDatasetId(datasetOne.getDataSetId());
    summary.addDatasetId(datasetTwo.getDataSetId());
    when(darCollectionSummaryDAO.getDarCollectionSummariesForSO(any())).thenReturn(
        List.of(summary));

    List<DarCollectionSummary> summaries = service.getSummariesForRoleName(user,
        UserRoles.SIGNINGOFFICIAL.getRoleName());
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
    datasetOne.setDataSetId(1);
    Dataset datasetTwo = new Dataset();
    datasetTwo.setDataSetId(2);
    summary.addDatasetId(datasetOne.getDataSetId());
    summary.addDatasetId(datasetTwo.getDataSetId());
    when(darCollectionSummaryDAO.getDarCollectionSummariesForSO(any())).thenReturn(
        List.of(summary));

    List<DarCollectionSummary> summaries = service.getSummariesForRoleName(user,
        UserRoles.SIGNINGOFFICIAL.getRoleName());
    assertNotNull(summaries);
    assertEquals(1, summaries.size());
    DarCollectionSummary s = summaries.get(0);
    assertTrue(
        s.getStatus().equalsIgnoreCase(DarCollectionStatus.UNREVIEWED.getValue()));
  }

  @Test
  void testProcessDarCollectionSummariesForResearcher() {

    //summaryOne -> in review (elections present)
    //summarytwo -> no elections
    //summaryThree -> no elections, canceled
    //summaryThree -> draft

    User user = new User();
    user.setUserId(1);

    DarCollectionSummary summaryOne = new DarCollectionSummary();
    Dataset datasetOne = new Dataset();
    datasetOne.setDataSetId(1);
    Dataset datasetTwo = new Dataset();
    datasetTwo.setDataSetId(2);
    Election electionOne = new Election();
    electionOne.setElectionId(1);
    electionOne.setStatus(ElectionStatus.OPEN.getValue());
    summaryOne.addElection(electionOne);
    summaryOne.addDatasetId(datasetOne.getDataSetId());
    summaryOne.addDatasetId(datasetTwo.getDataSetId());

    DarCollectionSummary summaryTwo = new DarCollectionSummary();
    Dataset datasetThree = new Dataset();
    datasetThree.setDataSetId(3);
    Dataset datasetFour = new Dataset();
    datasetFour.setDataSetId(4);
    summaryTwo.addDatasetId(datasetThree.getDataSetId());
    summaryTwo.addDatasetId(datasetFour.getDataSetId());

    DarCollectionSummary summaryThree = new DarCollectionSummary();
    Dataset datasetFive = new Dataset();
    datasetFive.setDataSetId(5);
    summaryThree.addDatasetId(datasetFive.getDataSetId());
    summaryThree.addStatus(DarStatus.CANCELED.getValue(), RandomStringUtils.randomAlphabetic(3));

    DataAccessRequest draft = new DataAccessRequest();
    draft.setCreateDate(new Timestamp(new Date().getTime()));
    DataAccessRequestData data = new DataAccessRequestData();
    data.setProjectTitle(RandomStringUtils.randomAlphabetic(10));
    data.setCreateDate(draft.getCreateDate().getTime());
    draft.setDraft(true);
    draft.setData(data);
    List<DarCollectionSummary> mockSummaries = new ArrayList<>();
    mockSummaries.add(summaryOne);
    mockSummaries.add(summaryTwo);
    mockSummaries.add(summaryThree);
    when(dataAccessRequestDAO.findAllDraftsByUserId(any())).thenReturn(List.of(draft));
    when(darCollectionSummaryDAO.getDarCollectionSummariesForResearcher(any())).thenReturn(
        mockSummaries);


    List<DarCollectionSummary> summaries = service.getSummariesForRoleName(user,
        UserRoles.RESEARCHER.getRoleName());
    assertNotNull(summaries);
    assertEquals(4, summaries.size());

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
        testTwo.getStatus().equalsIgnoreCase(DarCollectionStatus.UNREVIEWED.getValue()));
    assertEquals(testTwo.getActions(), expectedTwoActions);

    DarCollectionSummary testThree = summaries.get(2);
    Set<String> expectedThreeActions = Set.of(
        DarCollectionActions.REVIEW.getValue(),
        DarCollectionActions.REVISE.getValue());
    assertTrue(
        testThree.getStatus().equalsIgnoreCase(DarCollectionStatus.CANCELED.getValue()));
    assertEquals(testThree.getActions(), expectedThreeActions);

    DarCollectionSummary testDraft = summaries.get(3);
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
    datasetOne.setDataSetId(1);
    Dataset datasetTwo = new Dataset();
    datasetTwo.setDataSetId(2);
    Election electionOne = new Election();
    electionOne.setElectionId(1);
    electionOne.setStatus(ElectionStatus.OPEN.getValue());
    Election electionTwo = new Election();
    electionTwo.setElectionId(2);
    electionTwo.setStatus(ElectionStatus.OPEN.getValue());
    summaryOne.addElection(electionOne);
    summaryOne.addElection(electionTwo);
    summaryOne.addDatasetId(datasetOne.getDataSetId());
    summaryOne.addDatasetId(datasetTwo.getDataSetId());

    DarCollectionSummary summaryTwo = new DarCollectionSummary();
    Dataset datasetThree = new Dataset();
    datasetThree.setDataSetId(3);
    Dataset datasetFour = new Dataset();
    datasetFour.setDataSetId(4);
    Dataset datasetFive = new Dataset();
    datasetFive.setDataSetId(5);
    Election electionThree = new Election();
    electionThree.setElectionId(3);
    electionThree.setStatus(ElectionStatus.OPEN.getValue());
    Election electionFour = new Election();
    electionFour.setElectionId(4);
    electionFour.setStatus(ElectionStatus.CANCELED.getValue());
    summaryTwo.addElection(electionThree);
    summaryTwo.addElection(electionFour);
    summaryTwo.addDatasetId(datasetThree.getDataSetId());
    summaryTwo.addDatasetId(datasetFour.getDataSetId());
    summaryTwo.addDatasetId(datasetFive.getDataSetId());

    DarCollectionSummary summaryThree = new DarCollectionSummary();
    Dataset datasetSix = new Dataset();
    datasetSix.setDataSetId(6);
    Election electionFive = new Election();
    electionFive.setElectionId(5);
    electionFive.setStatus(ElectionStatus.CANCELED.getValue());
    summaryThree.addElection(electionFive);
    summaryThree.addDatasetId(datasetSix.getDataSetId());

    DarCollectionSummary summaryFour = new DarCollectionSummary();
    Dataset datasetSeven = new Dataset();
    datasetSeven.setDataSetId(7);
    summaryFour.addDatasetId(datasetSeven.getDataSetId());

    when(darCollectionSummaryDAO.getDarCollectionSummariesForAdmin())
        .thenReturn(List.of(summaryOne, summaryTwo, summaryThree, summaryFour));


    List<DarCollectionSummary> summaries = service.getSummariesForRoleName(user,
        UserRoles.ADMIN.getRoleName());

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
        testFour.getStatus().equalsIgnoreCase(DarCollectionStatus.UNREVIEWED.getValue()));
    assertEquals(testFour.getActions(), expectedFourActions);
  }

  @Test
  void testProcessDarCollectionSummariesForDACMember() {
    Dac dac = new Dac();
    dac.setDacId(1);
    User user = new User();
    user.setUserId(1);
    user.setMemberRole();

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

    when(darCollectionSummaryDAO.getDarCollectionSummariesForDAC(any(), any()))
        .thenReturn(List.of(summary, summaryTwo, summaryThree, summaryFour));
    when(datasetDAO.findDatasetListByDacIds(any())).thenReturn(List.of());


    List<DarCollectionSummary> summaries = service.getSummariesForRoleName(user,
        UserRoles.MEMBER.getRoleName());

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
    assertEquals(DarCollectionStatus.UNREVIEWED.getValue(), testThree.getStatus());

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
    datasetOne.setDataSetId(1);
    Dataset datasetTwo = new Dataset();
    datasetTwo.setDataSetId(2);
    Election electionOne = new Election();
    electionOne.setElectionId(1);
    electionOne.setStatus(ElectionStatus.OPEN.getValue());
    Election electionTwo = new Election();
    electionTwo.setElectionId(2);
    electionTwo.setStatus(ElectionStatus.OPEN.getValue());
    summaryOne.addElection(electionOne);
    summaryOne.addElection(electionTwo);
    summaryOne.addDatasetId(datasetOne.getDataSetId());
    summaryOne.addDatasetId(datasetTwo.getDataSetId());

    DarCollectionSummary summaryTwo = new DarCollectionSummary();
    Dataset datasetThree = new Dataset();
    datasetThree.setDataSetId(3);
    Dataset datasetFour = new Dataset();
    datasetFour.setDataSetId(4);
    Dataset datasetFive = new Dataset();
    datasetFive.setDataSetId(5);
    Election electionThree = new Election();
    electionThree.setElectionId(3);
    electionThree.setStatus(ElectionStatus.OPEN.getValue());
    Election electionFour = new Election();
    electionFour.setElectionId(4);
    electionFour.setStatus(ElectionStatus.CANCELED.getValue());
    summaryTwo.addElection(electionThree);
    summaryTwo.addElection(electionFour);
    summaryTwo.addDatasetId(datasetThree.getDataSetId());
    summaryTwo.addDatasetId(datasetFour.getDataSetId());
    summaryTwo.addDatasetId(datasetFive.getDataSetId());

    DarCollectionSummary summaryThree = new DarCollectionSummary();
    Dataset datasetSix = new Dataset();
    datasetSix.setDataSetId(6);
    Election electionFive = new Election();
    electionFive.setElectionId(5);
    electionFive.setStatus(ElectionStatus.CANCELED.getValue());
    summaryThree.addElection(electionFive);
    summaryThree.addDatasetId(datasetSix.getDataSetId());

    DarCollectionSummary summaryFour = new DarCollectionSummary();
    Dataset datasetSeven = new Dataset();
    datasetSeven.setDataSetId(7);
    summaryFour.addDatasetId(datasetSeven.getDataSetId());

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
    when(datasetDAO.findDatasetListByDacIds(any())).thenReturn(List.of());


    List<DarCollectionSummary> summaries = service.getSummariesForRoleName(user,
        UserRoles.CHAIRPERSON.getRoleName());
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
        testFour.getStatus().equalsIgnoreCase(DarCollectionStatus.UNREVIEWED.getValue()));
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
  void testGetSummaryForRoleNameByCollectionId_SO() {
    User user = new User();
    user.setUserId(1);

    DarCollectionSummary summary = new DarCollectionSummary();
    Integer collectionId = RandomUtils.nextInt(1, 100);
    summary.setDarCollectionId(collectionId);
    Dataset datasetOne = new Dataset();
    datasetOne.setDataSetId(1);
    Dataset datasetTwo = new Dataset();
    datasetTwo.setDataSetId(2);
    Election electionOne = new Election();
    electionOne.setElectionId(1);
    electionOne.setStatus(ElectionStatus.OPEN.getValue());
    Election electionTwo = new Election();
    electionTwo.setElectionId(2);
    electionTwo.setStatus(ElectionStatus.CLOSED.getValue());
    summary.addElection(electionOne);
    summary.addElection(electionTwo);
    summary.addDatasetId(datasetOne.getDataSetId());
    summary.addDatasetId(datasetTwo.getDataSetId());

    when(darCollectionSummaryDAO.getDarCollectionSummaryByCollectionId(collectionId))
        .thenReturn(summary);

    DarCollectionSummary summaryResult = service.getSummaryForRoleNameByCollectionId(user,
        UserRoles.SIGNINGOFFICIAL.getRoleName(), collectionId);
    assertNotNull(summaryResult);

    assertTrue(
        summaryResult.getStatus().equalsIgnoreCase(DarCollectionStatus.IN_PROCESS.getValue()));
    assertEquals(Set.of(), summaryResult.getActions());
  }

  @Test
  void testGetSummaryForRoleNameByCollectionId_Researcher() {
    User user = new User();
    user.setUserId(1);

    DarCollectionSummary summary = new DarCollectionSummary();
    Integer collectionId = RandomUtils.nextInt(1, 100);
    summary.setDarCollectionId(collectionId);
    Dataset datasetOne = new Dataset();
    datasetOne.setDataSetId(1);
    Dataset datasetTwo = new Dataset();
    datasetTwo.setDataSetId(2);
    Election electionOne = new Election();
    electionOne.setElectionId(1);
    electionOne.setStatus(ElectionStatus.OPEN.getValue());
    Election electionTwo = new Election();
    electionTwo.setElectionId(2);
    electionTwo.setStatus(ElectionStatus.CLOSED.getValue());
    summary.addElection(electionOne);
    summary.addElection(electionTwo);
    summary.addDatasetId(datasetOne.getDataSetId());
    summary.addDatasetId(datasetTwo.getDataSetId());

    when(darCollectionSummaryDAO.getDarCollectionSummaryByCollectionId(collectionId))
        .thenReturn(summary);

    DarCollectionSummary summaryResult = service.getSummaryForRoleNameByCollectionId(user,
        UserRoles.RESEARCHER.getRoleName(), collectionId);
    assertNotNull(summaryResult);

    Set<String> expectedActions = Set.of(
        DarCollectionActions.REVIEW.getValue());
    assertTrue(
        summaryResult.getStatus().equalsIgnoreCase(DarCollectionStatus.IN_PROCESS.getValue()));
    assertEquals(expectedActions, summaryResult.getActions());
  }

  @Test
  void testGetSummaryForRoleNameByCollectionId_Admin() {
    User user = new User();
    user.setUserId(1);

    DarCollectionSummary summary = new DarCollectionSummary();
    Integer collectionId = RandomUtils.nextInt(1, 100);
    summary.setDarCollectionId(collectionId);
    Dataset datasetOne = new Dataset();
    datasetOne.setDataSetId(1);
    Dataset datasetTwo = new Dataset();
    datasetTwo.setDataSetId(2);
    Election electionOne = new Election();
    electionOne.setElectionId(1);
    electionOne.setStatus(ElectionStatus.OPEN.getValue());
    Election electionTwo = new Election();
    electionTwo.setElectionId(2);
    electionTwo.setStatus(ElectionStatus.CLOSED.getValue());
    summary.addElection(electionOne);
    summary.addElection(electionTwo);
    summary.addDatasetId(datasetOne.getDataSetId());
    summary.addDatasetId(datasetTwo.getDataSetId());

    when(darCollectionSummaryDAO.getDarCollectionSummaryByCollectionId(collectionId))
        .thenReturn(summary);

    DarCollectionSummary summaryResult = service.getSummaryForRoleNameByCollectionId(user,
        UserRoles.ADMIN.getRoleName(), collectionId);
    assertNotNull(summaryResult);

    Set<String> expectedActions = Set.of(
        DarCollectionActions.CANCEL.getValue(),
        DarCollectionActions.OPEN.getValue());
    assertTrue(
        summaryResult.getStatus().equalsIgnoreCase(DarCollectionStatus.IN_PROCESS.getValue()));
    assertEquals(expectedActions, summaryResult.getActions());
  }

  @Test
  void testGetSummaryForRoleNameByCollectionId_Chair() {
    Dac dac = new Dac();
    dac.setDacId(1);
    User user = new User();
    user.setUserId(1);
    user.setChairpersonRoleWithDAC(dac.getDacId());
    DarCollectionSummary summary = new DarCollectionSummary();
    Integer collectionId = RandomUtils.nextInt(1, 100);
    summary.setDarCollectionId(collectionId);
    Dataset datasetOne = new Dataset();
    datasetOne.setDataSetId(1);
    Dataset datasetTwo = new Dataset();
    datasetTwo.setDataSetId(2);
    Election electionOne = new Election();
    electionOne.setElectionId(1);
    electionOne.setStatus(ElectionStatus.OPEN.getValue());
    Election electionTwo = new Election();
    electionTwo.setElectionId(2);
    electionTwo.setStatus(ElectionStatus.CANCELED.getValue());
    summary.addElection(electionOne);
    summary.addElection(electionTwo);
    summary.addDatasetId(datasetOne.getDataSetId());
    summary.addDatasetId(datasetTwo.getDataSetId());

    when(darCollectionSummaryDAO.getDarCollectionSummaryForDACByCollectionId(user.getUserId(),
        List.of(), collectionId))
        .thenReturn(summary);
    when(datasetDAO.findDatasetListByDacIds(any())).thenReturn(List.of());


    DarCollectionSummary summaryResult = service.getSummaryForRoleNameByCollectionId(user,
        UserRoles.CHAIRPERSON.getRoleName(), collectionId);
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
  void testGetSummaryForRoleNameByCollectionId_DACMember() {
    Dac dac = new Dac();
    dac.setDacId(1);
    User user = new User();
    user.setUserId(1);
    user.setMemberRole();

    DarCollectionSummary summary = new DarCollectionSummary();
    Integer collectionId = RandomUtils.nextInt(1, 100);
    summary.setDarCollectionId(collectionId);
    Dataset datasetOne = new Dataset();
    datasetOne.setDataSetId(1);
    Dataset datasetTwo = new Dataset();
    datasetTwo.setDataSetId(2);
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
    summary.addDatasetId(datasetOne.getDataSetId());
    summary.addDatasetId(datasetTwo.getDataSetId());
    summary.setVotes(List.of(vote));

    when(darCollectionSummaryDAO.getDarCollectionSummaryForDACByCollectionId(user.getUserId(),
        List.of(), collectionId))
        .thenReturn(summary);
    when(datasetDAO.findDatasetListByDacIds(any())).thenReturn(List.of());


    DarCollectionSummary summaryResult = service.getSummaryForRoleNameByCollectionId(user,
        UserRoles.MEMBER.getRoleName(), collectionId);
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
    Integer collectionId = RandomUtils.nextInt(1, 100);
    summary.setDarCollectionId(collectionId);

    when(darCollectionSummaryDAO.getDarCollectionSummaryByCollectionId(collectionId))
        .thenReturn(null);

    String reasearcherRoleName = UserRoles.RESEARCHER.getRoleName();
    assertThrows(NotFoundException.class, () -> {
      service.getSummaryForRoleNameByCollectionId(user, reasearcherRoleName, collectionId);
    });
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

    Integer datasetId = RandomUtils.nextInt(1, 100);
    datasets.add(generateMockDatasetWithDataUse(datasetId));
    dar.addDatasetId(datasetId);
    dar.setData(data);
    dar.setReferenceId(UUID.randomUUID().toString());
    return dar;
  }

  private Dataset generateMockDatasetWithDataUse(Integer datasetId) {
    Dataset dataset = new Dataset();
    dataset.setDataSetId(datasetId);
    return dataset;
  }

  private void initService() {
    service = new DarCollectionService(darCollectionDAO, darCollectionServiceDAO, datasetDAO,
        electionDAO, dataAccessRequestDAO, emailService, voteDAO, matchDAO,
        darCollectionSummaryDAO);
  }

  private List<DarCollection> createMockCollections(int count) {
    return IntStream.rangeClosed(1, count)
        .mapToObj(
            i -> {
              DarCollection collection = new DarCollection();
              collection.setDarCollectionId(i);
              collection.setDarCode(RandomStringUtils.randomAlphanumeric(5));
              collection.setCreateUserId(1);
              return collection;
            })
        .collect(Collectors.toList());
  }

  private Election createMockElection() {
    Election election = new Election();
    election.setElectionId(1);
    election.setReferenceId(UUID.randomUUID().toString());
    return election;
  }

}
