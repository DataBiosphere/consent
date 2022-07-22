package org.broadinstitute.consent.http.service;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.db.DarCollectionDAO;
import org.broadinstitute.consent.http.db.DarCollectionSummaryDAO;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.db.MatchDAO;
import org.broadinstitute.consent.http.enumeration.DarCollectionActions;
import org.broadinstitute.consent.http.enumeration.DarCollectionStatus;
import org.broadinstitute.consent.http.enumeration.DarStatus;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.DarCollection;
import org.broadinstitute.consent.http.models.DarCollectionSummary;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.PaginationResponse;
import org.broadinstitute.consent.http.models.PaginationToken;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.service.dao.DarCollectionServiceDAO;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

public class DarCollectionServiceTest {

  private DarCollectionService service;

  @Mock private DarCollectionDAO darCollectionDAO;
  @Mock private DarCollectionSummaryDAO darCollectionSummaryDAO;
  @Mock private DarCollectionServiceDAO darCollectionServiceDAO;
  @Mock private DatasetDAO datasetDAO;
  @Mock private ElectionDAO electionDAO;
  @Mock private DataAccessRequestDAO dataAccessRequestDAO;
  @Mock private EmailNotifierService emailNotifierService;
  @Mock private VoteDAO voteDAO;
  @Mock private MatchDAO matchDAO;
  @Mock private User user;

  @Before
  public void setUp() {
    openMocks(this);
  }

  @Test
  public void testGetCollectionsForUserByRoleName_ADMIN() {
    User user = new User();
    user.addRole(new UserRole(UserRoles.ADMIN.getRoleId(), UserRoles.ADMIN.getRoleName()));
    DarCollection collection = new DarCollection();
    when(darCollectionDAO.findAllDARCollections()).thenReturn(List.of(collection));
    initService();

    List<DarCollection> collections = service.getCollectionsForUserByRoleName(user, UserRoles.ADMIN.getRoleName());
    assertEquals(1, collections.size());
  }

  @Test
  public void testGetCollectionsForUserByRoleName_CHAIR_MEMBER() {
    User user = new User();
    UserRole chair = new UserRole(1, user.getUserId(), UserRoles.CHAIRPERSON.getRoleId(), UserRoles.CHAIRPERSON.getRoleName(), 2);
    UserRole member = new UserRole(2, user.getUserId(), UserRoles.MEMBER.getRoleId(), UserRoles.MEMBER.getRoleName(), 3);
    user.addRole(chair);
    user.addRole(member);
    user.getRoles().get(0).setDacId(1);
    user.getRoles().get(1).setDacId(1);
    when(darCollectionDAO.findDARCollectionIdsByDacIds(List.of(1))).thenReturn(List.of(1));
    DarCollection collection = new DarCollection();
    when(darCollectionDAO.findDARCollectionByCollectionIds(List.of(1))).thenReturn(List.of(collection));
    initService();

    List<DarCollection> collections = service.getCollectionsForUserByRoleName(user, UserRoles.CHAIRPERSON.getRoleName());
    assertEquals(1, collections.size());
  }

  @Test
  public void testGetCollectionsForUserByRoleName_SIGNING_OFFICIAL() {
    User user = new User();
    user.addRole(new UserRole(UserRoles.SIGNINGOFFICIAL.getRoleId(), UserRoles.SIGNINGOFFICIAL.getRoleName()));
    user.setInstitutionId(1);
    when(darCollectionDAO.findDARCollectionIdsByInstitutionId(1)).thenReturn(List.of(1));
    DarCollection collection = new DarCollection();
    when(darCollectionDAO.findDARCollectionByCollectionIds(List.of(1))).thenReturn(List.of(collection));
    initService();

    List<DarCollection> collections = service.getCollectionsForUserByRoleName(user, UserRoles.SIGNINGOFFICIAL.getRoleName());
    assertEquals(1, collections.size());
  }

  @Test
  public void testGetCollectionsForUserByRoleName_DEFAULT() {
    User user = new User();
    user.setUserId(1);
    user.addRole(new UserRole(UserRoles.RESEARCHER.getRoleId(), UserRoles.RESEARCHER.getRoleName()));
    DarCollection collection = new DarCollection();
    when(darCollectionDAO.findDARCollectionsCreatedByUserId(user.getUserId())).thenReturn(List.of(collection));
    initService();

    List<DarCollection> collections = service.getCollectionsForUserByRoleName(user, UserRoles.RESEARCHER.getRoleName());
    assertEquals(1, collections.size());
  }

  @Test
  public void testGetCollectionsForUserByRoleName_NULL() {
    User user = new User();
    user.setUserId(1);
    when(darCollectionDAO.findDARCollectionsCreatedByUserId(user.getUserId())).thenReturn(List.of(new DarCollection()));
    initService();

    List<DarCollection> collections = service.getCollectionsForUserByRoleName(user, null);
    assertEquals(1, collections.size());
  }

  @Test
  public void testGetCollectionsForUserByRoleName_NoCanceledCollections() {
    User user = new User();
    user.addRole(new UserRole(UserRoles.ADMIN.getRoleId(), UserRoles.ADMIN.getRoleName()));
    DataAccessRequestData canceledDarData = new DataAccessRequestData();
    canceledDarData.setStatus(DarStatus.CANCELED.getValue());
    DataAccessRequest canceledDar = new DataAccessRequest();
    canceledDar.setData(canceledDarData);
    DarCollection canceledCollection = new DarCollection();
    canceledCollection.addDar(canceledDar);
    when(darCollectionDAO.findAllDARCollections()).thenReturn(List.of(canceledCollection));
    initService();

    List<DarCollection> collections = service.getCollectionsForUserByRoleName(user, UserRoles.ADMIN.getRoleName());
    assertEquals(0, collections.size());
  }

  @Test
  public void testGetCollectionsByUserDacs() {
    User user = new User();
    user.setUserId(1);
    UserRole chair = new UserRole(UserRoles.CHAIRPERSON.getRoleId(), UserRoles.CHAIRPERSON.getRoleName());
    chair.setDacId(1);
    user.setRoles(List.of(chair));
    when(darCollectionDAO.findDARCollectionIdsByDacIds(List.of(chair.getDacId()))).thenReturn(List.of(1));
    when(darCollectionDAO.findDARCollectionByCollectionIds(List.of(1))).thenReturn(List.of(new DarCollection()));
    initService();

    List<DarCollection> collections = service.getCollectionsByUserDacs(user, false);
    assertEquals(1, collections.size());
  }

  @Test
  public void testGetCollectionsByUserInstitution() {
    User user = new User();
    user.setInstitutionId(1);
    when(darCollectionDAO.findDARCollectionIdsByInstitutionId(user.getInstitutionId())).thenReturn(List.of(1));
    when(darCollectionDAO.findDARCollectionByCollectionIds(List.of(1))).thenReturn(List.of(new DarCollection()));
    initService();

    List<DarCollection> collections = service.getCollectionsByUserInstitution(user);
    assertEquals(1, collections.size());
  }

  @Test
  public void testGetCollectionsWithFiltersByPage() {
    User user = createMockAdminUser();
    IntStream.rangeClosed(1, 8)
        .forEach(
            page -> {
              int filteredCount = 75;
              int unfilteredCount = 100;
              PaginationToken token = new PaginationToken(page, 10, "darCode", "DESC", null, DarCollection.acceptableSortFields, DarCollection.defaultTokenSortField);
              List<DarCollection> unfilteredList = createMockCollections(unfilteredCount);
              List<DarCollection> filteredList = unfilteredList.subList(0, filteredCount);
              initService();
              when(darCollectionDAO.returnUnfilteredCollectionCount()).thenReturn(unfilteredCount);
              when(darCollectionDAO.getFilteredCollectionsForAdmin(anyString(), anyString(), anyString())).thenReturn(filteredList);
              PaginationResponse<DarCollection> response = service.queryCollectionsByFiltersAndUserRoles(user, token, UserRoles.ADMIN.getRoleName());
              /*
               page 1: ids 01-10
               page 2: ids 11-20
               page 3: ids 21-30
               ...
               page 8: ids 71-75
              */
              // Assert that the results sizes are correct
              if (page == 8) {
                int lastPageSize = token.getFilteredCount() % token.getPageSize();
                assertEquals(lastPageSize, response.getResults().size());
              } else {
                assertEquals((int) token.getPageSize(), response.getResults().size());
              }

              // Assert that the returned results are what we expect them to be, based on ID
              int expectedCollectionId = (page * token.getPageSize()) - token.getPageSize() + 1;
              assertEquals(Integer.valueOf(expectedCollectionId), response.getResults().get(0).getDarCollectionId());
              assertEquals(filteredCount, response.getFilteredCount().intValue());
            });
  }

  @Test
  public void testGetCollectionsWithFilters_EmptyUnfiltered() {
    User user = createMockAdminUser();
    PaginationToken token = new PaginationToken(1, 10, "darCode", "DESC", null, DarCollection.acceptableSortFields, DarCollection.defaultTokenSortField);
    when(darCollectionDAO.returnUnfilteredCollectionCount()).thenReturn(0);
    initService();

    PaginationResponse<DarCollection> response = service.queryCollectionsByFiltersAndUserRoles(user, token, UserRoles.ADMIN.getRoleName());

    assertEquals(1, response.getFilteredPageCount().intValue());
    assertEquals(0, response.getUnfilteredCount().intValue());
    assertEquals(0, response.getFilteredCount().intValue());
  }

  @Test
  public void testGetCollectionsWithFiltersByPageLessThanPageSize() {
      int filteredCount = 3;
      int unfilteredCount = 5;
      User user = createMockAdminUser();
      PaginationToken token = new PaginationToken(1, 10, "darCode", "DESC", null, DarCollection.acceptableSortFields, DarCollection.defaultTokenSortField);
      List<DarCollection> mockCollections = createMockCollections(3);
      when(darCollectionDAO.returnUnfilteredCollectionCount()).thenReturn(unfilteredCount);
      when(darCollectionDAO.getFilteredCollectionsForAdmin(anyString(), anyString(), anyString())).thenReturn(mockCollections);
      initService();
      PaginationResponse<DarCollection> response = service.queryCollectionsByFiltersAndUserRoles(user, token, UserRoles.ADMIN.getRoleName());
      assertEquals(1, response.getFilteredPageCount().intValue());
      assertEquals(filteredCount, response.getResults().size());
      assertEquals(filteredCount, response.getFilteredCount().intValue());
  }

  @Test
  public void testInitWithInvalidTokenValues() {
      int filteredCount = 5;
      int unfilteredCount = 20;
      PaginationToken token = new PaginationToken(2, 10, "darCode", "DESC", null, DarCollection.acceptableSortFields, DarCollection.defaultTokenSortField);
      initWithPaginationToken(token, unfilteredCount, filteredCount);

      // Start index will be > end index in this case since we're trying to get results 11-20 when
      // there are only 5 items in the results array, so there should be 0 results returned
      PaginationResponse<DarCollection> response = service.queryCollectionsByFiltersAndUserRoles(user, token, UserRoles.ADMIN.getRoleName());
      assertTrue(response.getResults().isEmpty());
  }

  @Test
  public void testAddDatasetsToCollection() {
    List<DarCollection> collections = new ArrayList<>();
    Set<Dataset> datasets = new HashSet<>();
    collections.add(generateMockDarCollection(datasets));
    List<Integer> datasetIds = datasets.stream()
      .map(Dataset::getDataSetId)
      .sorted()
      .collect(Collectors.toList());

    when(datasetDAO.findDatasetWithDataUseByIdList(anyList())).thenReturn(datasets);
    when(dataAccessRequestDAO.findAllDARDatasetRelations(any())).thenReturn(datasetIds);
    initService();

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
  public void testAddDatasetsToCollectionsWithFilterDatasetIds() {
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
    when(datasetDAO.findDatasetWithDataUseByIdList(List.of(dataset.getDataSetId()))).thenReturn(new HashSet<>(List.of(dataset)));
    when(dataAccessRequestDAO.findAllDARDatasetRelations(any())).thenReturn(datasetIds);

    initService();

    collections = service.addDatasetsToCollections(collections, List.of(dataset.getDataSetId()));
    assertEquals(1, collections.size());

    DarCollection collection = collections.get(0);
    Set<Dataset> datasetsFromCollection = collection.getDatasets();
    assertEquals(1, datasetsFromCollection.size());

    List<Integer> collectionDatasetIds = datasetsFromCollection.stream()
            .map(Dataset::getDataSetId)
            .sorted()
            .collect(Collectors.toList());
    assertEquals(dataset.getDataSetId(), collectionDatasetIds.get(0));
  }

  @Test
  public void testCancelDarCollection_noElections() {
    Set<Dataset> datasets = new HashSet<>();
    DarCollection collection = generateMockDarCollection(datasets);
    collection.getDars().values().forEach(d -> d.getData().setStatus("Canceled"));
    List<Election> elections = new ArrayList<>();
    when(electionDAO.findLastElectionsByReferenceIdsAndType(anyList(), anyString())).thenReturn(elections);
    doNothing().when(dataAccessRequestDAO).cancelByReferenceIds(anyList());
    when(darCollectionDAO.findDARCollectionByCollectionId(any())).thenReturn(collection);
    initService();

    DarCollection canceledCollection = service.cancelDarCollectionAsResearcher(collection);
    for (DataAccessRequest collectionDar : canceledCollection.getDars().values()) {
      assertEquals("canceled", collectionDar.getData().getStatus().toLowerCase());
    }
  }

  @Test(expected = BadRequestException.class)
  public void testCancelDarCollection_electionPresent() {
    Set<Dataset> datasets = new HashSet<>();
    DarCollection collection = generateMockDarCollection(datasets);

    when(electionDAO.findLastElectionsByReferenceIds(anyList())).thenReturn(List.of(new Election()));
    doNothing().when(dataAccessRequestDAO).cancelByReferenceIds(anyList());
    when(darCollectionDAO.findDARCollectionByCollectionId(any())).thenReturn(collection);
    initService();

    service.cancelDarCollectionAsResearcher(collection);
  }

  @Test
  public void testCancelDarCollectionAsResearcher_NoElections() {
    DataAccessRequest dar = new DataAccessRequest();
    dar.setReferenceId(UUID.randomUUID().toString());
    DataAccessRequestData data = new DataAccessRequestData();
    dar.setData(data);
    DarCollection collection = createMockCollections(1).get(0);
    collection.setDars(Map.of(dar.getReferenceId(), dar));
    when(electionDAO.findLastElectionsByReferenceIds(anyList())).thenReturn(List.of());
    when(darCollectionDAO.findDARCollectionByCollectionId(any())).thenReturn(collection);
    spy(electionDAO);
    spy(dataAccessRequestDAO);
    spy(darCollectionDAO);
    initService();

    service.cancelDarCollectionAsResearcher(collection);
    verify(electionDAO, times(1)).findLastElectionsByReferenceIds(anyList());
    verify(electionDAO, times(0)).updateElectionById(anyInt(), anyString(), any());
    verify(dataAccessRequestDAO, times(1)).cancelByReferenceIds(anyList());
    verify(darCollectionDAO, times(1)).findDARCollectionByCollectionId(anyInt());
  }

  @Test(expected = BadRequestException.class)
  public void testCancelDarCollectionAsResearcher_WithElections() {
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
    initService();

    service.cancelDarCollectionAsResearcher(collection);
  }

  @Test
  public void testCancelDarCollectionAsAdmin() {
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
    spy(electionDAO);
    spy(dataAccessRequestDAO);
    spy(darCollectionDAO);
    initService();

    service.cancelDarCollectionElectionsAsAdmin(collection);
    verify(electionDAO, times(1)).findOpenElectionsByReferenceIds(anyList());
    verify(electionDAO, times(1)).updateElectionById(anyInt(), anyString(), any());
    verify(dataAccessRequestDAO, times(0)).cancelByReferenceIds(anyList());
    verify(darCollectionDAO, times(1)).findDARCollectionByCollectionId(anyInt());
  }

  @Test
  public void testCancelDarCollectionAsChair_ChairHasDatasets() {
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
    spy(datasetDAO);
    spy(electionDAO);
    spy(dataAccessRequestDAO);
    spy(darCollectionDAO);
    initService();

    service.cancelDarCollectionElectionsAsChair(collection, user);
    verify(datasetDAO, times(1)).findDatasetsByAuthUserEmail(anyString());
    verify(electionDAO, times(1)).findOpenElectionsByReferenceIds(anyList());
    verify(electionDAO, times(1)).updateElectionById(anyInt(), anyString(), any());
    verify(dataAccessRequestDAO, times(0)).cancelByReferenceIds(anyList());
    verify(darCollectionDAO, times(1)).findDARCollectionByCollectionId(anyInt());
  }

  @Test
  public void testCancelDarCollectionAsChair_ChairHasNoDatasets() {
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
    spy(datasetDAO);
    spy(electionDAO);
    spy(dataAccessRequestDAO);
    spy(darCollectionDAO);
    initService();

    service.cancelDarCollectionElectionsAsChair(collection, user);
    verify(datasetDAO, times(1)).findDatasetsByAuthUserEmail(anyString());
    verify(electionDAO, times(0)).findLastElectionsByReferenceIds(anyList());
    verify(electionDAO, times(0)).updateElectionById(anyInt(), anyString(), any());
    verify(dataAccessRequestDAO, times(0)).cancelByReferenceIds(anyList());
    verify(darCollectionDAO, times(0)).findDARCollectionByCollectionId(anyInt());
  }

  @Test
  public void testCreateElectionsForDarCollection() throws Exception {
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
    when(electionDAO.findLastElectionsByReferenceIds(anyList())).thenReturn(List.of(election));
    when(electionDAO.findLastElectionByReferenceIdAndType(any(), any())).thenReturn(election);
    when(voteDAO.findVoteUsersByElectionReferenceIdList(any())).thenReturn(List.of(new User()));
    spy(darCollectionServiceDAO);
    spy(electionDAO);
    spy(voteDAO);
    spy(emailNotifierService);
    spy(darCollectionDAO);
    initService();

    service.createElectionsForDarCollection(user, collection);
    verify(darCollectionServiceDAO, times(1)).createElectionsForDarCollection(any(), any());
    verify(voteDAO, times(1)).findVoteUsersByElectionReferenceIdList(any());
    verify(emailNotifierService, times(1)).sendDarNewCollectionElectionMessage(any(), any());
    verify(darCollectionDAO, times(1)).findDARCollectionByCollectionId(any());
  }

  @Test
  public void testQueryCollectionsByFiltersAndUserRolesForAdmin() {
    User user = createMockAdminUser();
    String adminName = UserRoles.ADMIN.getRoleName();
    int unfilteredCount = 20;
    int pageSize = 10;
    int mockCollectionSize = 3;
    List<DarCollection> mockCollection = createMockCollections(mockCollectionSize);
    PaginationToken token = initPaginationToken("darCode", "DESC", "", pageSize);
    when(darCollectionDAO.returnUnfilteredCollectionCount()).thenReturn(unfilteredCount);
    when(darCollectionDAO.getFilteredCollectionsForAdmin(anyString(), anyString(), anyString())).thenReturn(mockCollection);
    initService();

    PaginationResponse<DarCollection> collectionResponse = service.queryCollectionsByFiltersAndUserRoles(user, token, adminName);
    assertNotNull(collectionResponse);
    int responseUnfilteredCount = collectionResponse.getUnfilteredCount();
    assertEquals(unfilteredCount, responseUnfilteredCount);
    assertEquals(mockCollectionSize, (int)collectionResponse.getFilteredCount());
    assertEquals(1, (int)collectionResponse.getFilteredPageCount());
  }

  @Test
  public void testQueryCollectionsByFiltersAndUserRolesForSO() {
    User user = new User();
    String soName = UserRoles.SIGNINGOFFICIAL.getRoleName();
    Integer soId = UserRoles.SIGNINGOFFICIAL.getRoleId();
    UserRole soRole = new UserRole(soId, soName);
    user.addRole(soRole);
    user.setInstitutionId(2);
    int unfilteredCount = 20;
    int pageSize = 10;
    int mockCollectionSize = 3;
    List<DarCollection> mockCollection = createMockCollections(mockCollectionSize);
    PaginationToken token = initPaginationToken("darCode", "DESC", "", pageSize);
    when(darCollectionDAO.returnUnfilteredCountForInstitution(anyInt())).thenReturn(unfilteredCount);
    when(darCollectionDAO.getFilteredCollectionsForSigningOfficial(anyString(), anyString(), anyInt(), anyString())).thenReturn(mockCollection);
    initService();
    PaginationResponse<DarCollection> collectionResponse = service.queryCollectionsByFiltersAndUserRoles(user, token, soName);
    assertNotNull(collectionResponse);
    queryCollectionsAssertions(collectionResponse, unfilteredCount, mockCollectionSize);
  }

  @Test
  public void testQueryCollectionsByFiltersAndUserRolesForDACMember() {
    User user = new User();
    String dacRoleName = UserRoles.MEMBER.getRoleName();
    Integer dacRoleId = UserRoles.MEMBER.getRoleId();
    UserRole memberRole = new UserRole(dacRoleId, dacRoleName);
    user.addRole(memberRole);
    int pageSize = 10;
    int mockCollectionSize = 3;
    List<Integer> dacIds = List.of(1,2,3,4,5);
    List<DarCollection> mockCollection = createMockCollections(mockCollectionSize);
    PaginationToken token = initPaginationToken("darCode", "DESC", "", pageSize);
    when(darCollectionDAO.findDARCollectionIdsByDacIds(anyList())).thenReturn(dacIds);
    when(darCollectionDAO.getFilteredCollectionsForDACByCollectionIds(anyString(), anyString(), anyList(), anyString())).thenReturn(mockCollection);
    initService();
    PaginationResponse<DarCollection> response = service.queryCollectionsByFiltersAndUserRoles(user, token, dacRoleName);
    assertNotNull(response);
    assertEquals(dacIds.size(), (int)response.getUnfilteredCount());
    assertEquals(3, (int)response.getFilteredCount());
    assertEquals(1, (int)response.getFilteredPageCount());
    List<DarCollection> results = response.getResults();
    assertEquals(mockCollectionSize, results.size());
  }

  @Test
  public void testQueryCollectionsByFiltersAndUserRolesForDACChair() {
    User user = new User();
    String dacRoleName = UserRoles.CHAIRPERSON.getRoleName();
    Integer dacRoleId = UserRoles.CHAIRPERSON.getRoleId();
    UserRole memberRole = new UserRole(dacRoleId, dacRoleName);
    user.addRole(memberRole);
    int pageSize = 10;
    int mockCollectionSize = 3;
    List<Integer> dacIds = List.of(1,2,3,4,5);
    List<DarCollection> mockCollection = createMockCollections(mockCollectionSize);
    PaginationToken token = initPaginationToken("darCode", "DESC", "", pageSize);
    when(darCollectionDAO.findDARCollectionIdsByDacIds(anyList())).thenReturn(dacIds);
    when(darCollectionDAO.getFilteredCollectionsForDACByCollectionIds(anyString(), anyString(), anyList(), anyString())).thenReturn(mockCollection);
    initService();
    PaginationResponse<DarCollection> response = service.queryCollectionsByFiltersAndUserRoles(user, token, dacRoleName);
    assertNotNull(response);
    assertEquals(dacIds.size(), (int)response.getUnfilteredCount());
    assertEquals(3, (int)response.getFilteredCount());
    assertEquals(1, (int)response.getFilteredPageCount());
  }

  @Test
  public void testQueryCollectionsByFiltersAndUserRolesForResearcher() {
    User user = new User();
    user.setUserId(1);
    String dacRoleName = UserRoles.RESEARCHER.getRoleName();
    Integer dacRoleId = UserRoles.RESEARCHER.getRoleId();
    UserRole memberRole = new UserRole(dacRoleId, dacRoleName);
    user.addRole(memberRole);
    int pageSize = 10;
    int mockCollectionSize = 3;
    List<DarCollection> mockCollection = createMockCollections(mockCollectionSize);
    PaginationToken token = initPaginationToken("darCode", "DESC", "", pageSize);
    when(darCollectionDAO.returnUnfilteredResearcherCollectionCount(anyInt())).thenReturn(mockCollectionSize);
    when(darCollectionDAO.getFilteredListForResearcher(anyString(), anyString(), anyInt(), anyString()))
        .thenReturn(mockCollection);
    initService();
    PaginationResponse<DarCollection> response = service.queryCollectionsByFiltersAndUserRoles(user, token,
        dacRoleName);
    assertNotNull(response);
    assertEquals(mockCollectionSize, (int) response.getUnfilteredCount());
    assertEquals(3, (int) response.getFilteredCount());
    assertEquals(1, (int) response.getFilteredPageCount());
  }

  @Test
  public void testDeleteAsResearcherNoElections() {
    User user = new User();
    user.setUserId(1);
    String dacRoleName = UserRoles.RESEARCHER.getRoleName();
    Integer dacRoleId = UserRoles.RESEARCHER.getRoleId();
    UserRole memberRole = new UserRole(dacRoleId, dacRoleName);
    user.addRole(memberRole);

    Set<Dataset> datasets = new HashSet<>();
    DarCollection collection = generateMockDarCollection(datasets);
    collection.setDarCollectionId(10);
    collection.setCreateUserId(user.getUserId());

    when(electionDAO.findElectionsByReferenceIds(any())).thenReturn(new ArrayList<>());
    when(darCollectionDAO.findDARCollectionByCollectionId(any())).thenReturn(collection);
    List<String> referenceIds = collection.getDars().values().stream().map(DataAccessRequest::getReferenceId).collect(Collectors.toList());

    Integer collectionId = collection.getDarCollectionId();

    spy(electionDAO);
    spy(dataAccessRequestDAO);
    spy(darCollectionDAO);

    initService();
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

  @Test(expected = NotAcceptableException.class)
  public void testDeleteAsResearcherWithElections() {
    User user = new User();
    user.setUserId(1);
    String dacRoleName = UserRoles.RESEARCHER.getRoleName();
    Integer dacRoleId = UserRoles.RESEARCHER.getRoleId();
    UserRole memberRole = new UserRole(dacRoleId, dacRoleName);
    user.addRole(memberRole);

    Set<Dataset> datasets = new HashSet<>();
    DarCollection collection = generateMockDarCollection(datasets);
    collection.setDarCollectionId(10);
    collection.setCreateUserId(user.getUserId());

    Election e = createMockElection();
    when(electionDAO.findElectionsByReferenceIds(any())).thenReturn(new ArrayList<>(){{add(e);}});
    when(darCollectionDAO.findDARCollectionByCollectionId(any())).thenReturn(collection);
    List<String> referenceIds = collection.getDars().values().stream().map(DataAccessRequest::getReferenceId).collect(Collectors.toList());

    Integer collectionId = collection.getDarCollectionId();

    initService();
    service.deleteByCollectionId(user, collectionId);}

  @Test
  public void testDeleteAsAdminWithElections() {
    User user = new User();
    user.setUserId(1);
    String dacRoleName = UserRoles.ADMIN.getRoleName();
    Integer dacRoleId = UserRoles.ADMIN.getRoleId();
    UserRole memberRole = new UserRole(dacRoleId, dacRoleName);
    user.addRole(memberRole);

    Set<Dataset> datasets = new HashSet<>();
    DarCollection collection = generateMockDarCollection(datasets);
    collection.setDarCollectionId(10);

    Election e = createMockElection();
    when(electionDAO.findElectionsByReferenceIds(any())).thenReturn(new ArrayList<>(){{add(e);}});
    when(darCollectionDAO.findDARCollectionByCollectionId(any())).thenReturn(collection);

    Integer collectionId = collection.getDarCollectionId();

    spy(electionDAO);
    spy(dataAccessRequestDAO);
    spy(darCollectionDAO);
    spy(voteDAO);

    initService();
    service.deleteByCollectionId(user, collectionId);

    verify(dataAccessRequestDAO, times(1)).deleteByReferenceIds(any());
    verify(dataAccessRequestDAO, times(1)).deleteDARDatasetRelationByReferenceIds(any());
    verify(matchDAO, times(1)).deleteMatchesByPurposeIds(any());
    verify(darCollectionDAO, times(1)).deleteByCollectionId(collectionId);
    verify(electionDAO, times(1)).deleteElectionsFromAccessRPs(any());
    verify(electionDAO, times(1)).deleteElectionsByIds(any());
    verify(voteDAO, times(1)).deleteVotesByReferenceIds(any());
  }


  @Test(expected = NotAuthorizedException.class)
  public void testDeleteAsUser() {
    User user = new User();
    user.setUserId(1);

    Set<Dataset> datasets = new HashSet<>();
    DarCollection collection = generateMockDarCollection(datasets);
    collection.setDarCollectionId(10);
    collection.setCreateUserId(2); // not same as user id

    Integer collectionId = collection.getDarCollectionId();

    when(darCollectionDAO.findDARCollectionByCollectionId(any())).thenReturn(collection);
    when(electionDAO.findElectionsByReferenceIds(any())).thenReturn(new ArrayList<>());

    initService();
    service.deleteByCollectionId(user, collectionId);
  }

  @Test(expected = NotFoundException.class)
  public void testDeleteButNoCollection() {
    User user = new User();
    user.setUserId(1);

    Set<Dataset> datasets = new HashSet<>();
    DarCollection collection = generateMockDarCollection(datasets);
    collection.setDarCollectionId(10);
    collection.setCreateUserId(1);

    Integer collectionId = collection.getDarCollectionId();

    when(darCollectionDAO.findDARCollectionByCollectionId(any())).thenReturn(null);
    when(electionDAO.findElectionsByReferenceIds(any())).thenReturn(new ArrayList<>());

    initService();
    service.deleteByCollectionId(user, collectionId);
  }

  @Test
  public void testProcessDarCollectionSummariesForDAC_SO_InProcess() {
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
    when(darCollectionSummaryDAO.getDarCollectionSummariesForSO(any())).thenReturn(List.of(summary));
    initService();

    List<DarCollectionSummary> summaries = service.getSummariesForRoleName(user, UserRoles.SIGNINGOFFICIAL.getRoleName());
    assertNotNull(summaries);
    assertEquals(1, summaries.size());
    DarCollectionSummary s = summaries.get(0);
    assertTrue(s.getStatus().equalsIgnoreCase(DarCollectionStatus.IN_PROCESS.getValue()));
  }

  @Test
  public void testProcessDarCollectionSummariesForDAC_SO_Complete() {
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
    when(darCollectionSummaryDAO.getDarCollectionSummariesForSO(any())).thenReturn(List.of(summary));
    initService();

    List<DarCollectionSummary> summaries = service.getSummariesForRoleName(user,
        UserRoles.SIGNINGOFFICIAL.getRoleName());
    assertNotNull(summaries);
    assertEquals(1, summaries.size());
    DarCollectionSummary s = summaries.get(0);
    assertTrue(s.getStatus().equalsIgnoreCase(DarCollectionStatus.COMPLETE.getValue()));
  }

  @Test
  public void testProcessDarCollectionSummariesForDAC_SO_Unreviewed() {
    User user = new User();
    user.setUserId(1);
    DarCollectionSummary summary = new DarCollectionSummary();
    Dataset datasetOne = new Dataset();
    datasetOne.setDataSetId(1);
    Dataset datasetTwo = new Dataset();
    datasetTwo.setDataSetId(2);
    summary.addDatasetId(datasetOne.getDataSetId());
    summary.addDatasetId(datasetTwo.getDataSetId());
    when(darCollectionSummaryDAO.getDarCollectionSummariesForSO(any())).thenReturn(List.of(summary));
    initService();

    List<DarCollectionSummary> summaries = service.getSummariesForRoleName(user,
        UserRoles.SIGNINGOFFICIAL.getRoleName());
    assertNotNull(summaries);
    assertEquals(1, summaries.size());
    DarCollectionSummary s = summaries.get(0);
    assertTrue(s.getStatus().equalsIgnoreCase(DarCollectionStatus.UNREVIEWED.getValue()));
  }

  @Test
  public void testProcessDarCollectionSummariesForResearcher() {

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
    DataAccessRequestData data = new DataAccessRequestData();
    data.setProjectTitle(RandomStringUtils.randomAlphabetic(10));
    data.setCreateDate(Calendar.getInstance().getTimeInMillis());
    draft.setDraft(true);
    draft.setData(data);
    List<DarCollectionSummary> mockSummaries = new ArrayList<>();
    mockSummaries.add(summaryOne);
    mockSummaries.add(summaryTwo);
    mockSummaries.add(summaryThree);
    when(dataAccessRequestDAO.findAllDraftsByUserId(any())).thenReturn(List.of(draft));
    when(darCollectionSummaryDAO.getDarCollectionSummariesForResearcher(any())).thenReturn(mockSummaries);

    initService();

    List<DarCollectionSummary> summaries = service.getSummariesForRoleName(user,
        UserRoles.RESEARCHER.getRoleName());
    assertNotNull(summaries);
    assertEquals(4, summaries.size());

    DarCollectionSummary testOne = summaries.get(0);
    Set<String> expectedOneActions = Set.of(
      DarCollectionActions.REVIEW.getValue()
    );
    assertTrue(testOne.getStatus().equalsIgnoreCase(DarCollectionStatus.IN_PROCESS.getValue()));
    assertEquals(testOne.getActions(), expectedOneActions);

    DarCollectionSummary testTwo = summaries.get(1);
    Set<String> expectedTwoActions = Set.of(
      DarCollectionActions.REVIEW.getValue(),
      DarCollectionActions.CANCEL.getValue()
    );
    assertTrue(testTwo.getStatus().equalsIgnoreCase(DarCollectionStatus.UNREVIEWED.getValue()));
    assertEquals(testTwo.getActions(), expectedTwoActions);

    DarCollectionSummary testThree = summaries.get(2);
    Set<String> expectedThreeActions = Set.of(
        DarCollectionActions.REVIEW.getValue(),
        DarCollectionActions.REVISE.getValue());
    assertTrue(testThree.getStatus().equalsIgnoreCase(DarCollectionStatus.CANCELED.getValue()));
    assertEquals(testThree.getActions(), expectedThreeActions);

    DarCollectionSummary testDraft = summaries.get(3);
    Set<String> expectedDraftActions = Set.of(
        DarCollectionActions.RESUME.getValue(),
        DarCollectionActions.DELETE.getValue());
    assertTrue(testDraft.getStatus().equalsIgnoreCase(DarCollectionStatus.DRAFT.getValue()));
    assertEquals(testDraft.getActions(), expectedDraftActions);
  }

  @Test
  public void testProcessDarCollectionSummariesForAdmin() {
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

    initService();

    List<DarCollectionSummary> summaries = service.getSummariesForRoleName(user, UserRoles.ADMIN.getRoleName());

    DarCollectionSummary testOne = summaries.get(0);
    Set<String> expectedOneActions = Set.of(
        DarCollectionActions.CANCEL.getValue());
    assertTrue(testOne.getStatus().equalsIgnoreCase(DarCollectionStatus.IN_PROCESS.getValue()));
    assertEquals(testOne.getActions(), expectedOneActions);

    DarCollectionSummary testTwo = summaries.get(1);
    Set<String> expectedTwoActions = Set.of(
        DarCollectionActions.CANCEL.getValue(),
        DarCollectionActions.OPEN.getValue());
    assertTrue(testTwo.getStatus().equalsIgnoreCase(DarCollectionStatus.IN_PROCESS.getValue()));
    assertEquals(testTwo.getActions(), expectedTwoActions);

    DarCollectionSummary testThree = summaries.get(2);
    Set<String> expectedThreeActions = Set.of(
        DarCollectionActions.OPEN.getValue());
    assertTrue(testThree.getStatus().equalsIgnoreCase(DarCollectionStatus.COMPLETE.getValue()));
    assertEquals(testThree.getActions(), expectedThreeActions);

    DarCollectionSummary testFour = summaries.get(3);
    Set<String> expectedFourActions = Set.of(
        DarCollectionActions.OPEN.getValue());
    assertTrue(testFour.getStatus().equalsIgnoreCase(DarCollectionStatus.UNREVIEWED.getValue()));
    assertEquals(testFour.getActions(), expectedFourActions);
  }

  @Test
  public void testProcessDarCollectionSummariesForDACMember() {
    User user = new User();
    user.setUserId(1);

    //summaryOne -> no open elections (no action)
    //summaryTwo -> at least one open election (vote button)

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
    electionThree.setElectionId(3);
    electionThree.setStatus(ElectionStatus.OPEN.getValue());
    summaryTwo.addElection(electionThree);

    DarCollectionSummary summaryThree = new DarCollectionSummary();
    summaryThree.addDatasetId(4);

    when(darCollectionSummaryDAO.getDarCollectionSummariesForDAC(any(), any()))
      .thenReturn(List.of(summary, summaryTwo, summaryThree));
    when(datasetDAO.findDatasetsByUserId(any())).thenReturn(Set.of());

    initService();

    List<DarCollectionSummary> summaries = service.getSummariesForRoleName(user, UserRoles.MEMBER.getRoleName());

    assertNotNull(summaries);
    assertEquals(3, summaries.size());

    DarCollectionSummary testOne = summaries.get(0);
    Set<String> expectedOneActions = Set.of();
    assertEquals(testOne.getActions(), expectedOneActions);
    assertEquals(DarCollectionStatus.COMPLETE.getValue(), testOne.getStatus());

    DarCollectionSummary testTwo = summaries.get(1);
    Set<String> expectedTwoActions = Set.of("Vote");
    assertEquals(testTwo.getActions(), expectedTwoActions);
    assertEquals(DarCollectionStatus.IN_PROCESS.getValue(), testTwo.getStatus());

    DarCollectionSummary testThree = summaries.get(2);
    Set<String> expectedThreeActions = Set.of();
    assertEquals(testThree.getActions(), expectedThreeActions);
    assertEquals(DarCollectionStatus.UNREVIEWED.getValue(), testThree.getStatus());
  }

  @Test
  public void testProcessDarCollectionSummariesForChair() {
    //summaryOne -> all elections present and open
    //summaryTwo -> mix of open elections : absent/canceled elections (in process)
    //summaryThree -> all canceled elections (Complete)
    //summaryFour -> no elections (unreviewed)
    //summaryFive -> mix of open : absent/closed elections (in process, but cancel action does not appear)
    //summarySix -> all closed elections (complete, only open available)

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
      .thenReturn(List.of(summaryOne, summaryTwo, summaryThree, summaryFour, summaryFive, summarySix));
    when(datasetDAO.findDatasetsByUserId(any())).thenReturn(Set.of());

    initService();

    List<DarCollectionSummary> summaries = service.getSummariesForRoleName(user, UserRoles.CHAIRPERSON.getRoleName());
    assertEquals(6, summaries.size());

    DarCollectionSummary testOne = summaries.get(0);
    Set<String> expectedOneActions = Set.of(
        DarCollectionActions.VOTE.getValue(),
        DarCollectionActions.CANCEL.getValue());
    assertTrue(testOne.getStatus().equalsIgnoreCase(DarCollectionStatus.IN_PROCESS.getValue()));
    assertEquals(testOne.getActions(), expectedOneActions);

    DarCollectionSummary testTwo = summaries.get(1);
    Set<String> expectedTwoActions = Set.of(
        DarCollectionActions.VOTE.getValue(),
        DarCollectionActions.CANCEL.getValue(),
        DarCollectionActions.OPEN.getValue());
    assertTrue(testTwo.getStatus().equalsIgnoreCase(DarCollectionStatus.IN_PROCESS.getValue()));
    assertEquals(testTwo.getActions(), expectedTwoActions);

    DarCollectionSummary testThree = summaries.get(2);
    Set<String> expectedThreeActions = Set.of(
        DarCollectionActions.OPEN.getValue());
    assertTrue(testThree.getStatus().equalsIgnoreCase(DarCollectionStatus.COMPLETE.getValue()));
    assertEquals(testThree.getActions(), expectedThreeActions);

    DarCollectionSummary testFour = summaries.get(3);
    Set<String> expectedFourActions = Set.of(
        DarCollectionActions.OPEN.getValue());
    assertTrue(testFour.getStatus().equalsIgnoreCase(DarCollectionStatus.UNREVIEWED.getValue()));
    assertEquals(testFour.getActions(), expectedFourActions);

    DarCollectionSummary testFive = summaries.get(4);
    Set<String> expectedFiveActions = Set.of(
        DarCollectionActions.OPEN.getValue(),
        DarCollectionActions.VOTE.getValue()
    );
    assertTrue(testFive.getStatus().equalsIgnoreCase(DarCollectionStatus.IN_PROCESS.getValue()));
    assertEquals(testFive.getActions(), expectedFiveActions);

    DarCollectionSummary testSix = summaries.get(5);
    Set<String> expectedSixActions = Set.of(
        DarCollectionActions.OPEN.getValue()
    );
    assertTrue(testSix.getStatus().equalsIgnoreCase(DarCollectionStatus.COMPLETE.getValue()));
    assertEquals(testSix.getActions(), expectedSixActions);

  }

  private void queryCollectionsAssertions(PaginationResponse<DarCollection> response, int expectedUnfilteredCount, int expectedFilteredCount) {
    assertEquals(expectedUnfilteredCount, (int)response.getUnfilteredCount());
    assertEquals(expectedFilteredCount, (int)response.getFilteredCount());
    assertEquals(1, (int)response.getFilteredPageCount());
    List<DarCollection> results = response.getResults();
    assertEquals(expectedFilteredCount, results.size());
  }

  private PaginationToken initPaginationToken(String sortField, String sortOrder, String filterTerm, int pageSize) {
    return new PaginationToken(1, pageSize, sortField, sortOrder, filterTerm, DarCollection.acceptableSortFields, DarCollection.defaultTokenSortField);
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
    service = new DarCollectionService(darCollectionDAO, darCollectionServiceDAO, datasetDAO, electionDAO, dataAccessRequestDAO, emailNotifierService, voteDAO, matchDAO, darCollectionSummaryDAO);
  }

  //NOTE: init method does not work well with role based queries due to fetches by role rather by user (unless researcher)
  private void initWithPaginationToken(PaginationToken token, int unfilteredCount, int filteredCount) {
    openMocks(this);
    List<DarCollection> unfilteredDars = createMockCollections(unfilteredCount);
    // Start the filtered ids at index 0 so tests can make more assertions.
    List<DarCollection> filteredDars = unfilteredDars.subList(0, filteredCount);
    token.setUnfilteredCount(unfilteredDars.size());
    token.setFilteredCount(filteredDars.size());
    List<DarCollection> collectionIdDars = new ArrayList<>();
    if (token.getStartIndex() <= token.getEndIndex()) {
        collectionIdDars.addAll(filteredDars.subList(token.getStartIndex(), token.getEndIndex()));
    }
    when(darCollectionDAO.findDARCollectionsCreatedByUserId(any())).thenReturn(unfilteredDars);
    when(darCollectionDAO.findAllDARCollectionsWithFiltersByUser(any(), any(), any(), any())).thenReturn(filteredDars);
    when(darCollectionDAO.findDARCollectionByCollectionIdsWithOrder(any(), any(), any())).thenReturn(collectionIdDars);
    service = new DarCollectionService(darCollectionDAO, darCollectionServiceDAO, datasetDAO, electionDAO, dataAccessRequestDAO, emailNotifierService, voteDAO, matchDAO, darCollectionSummaryDAO);
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

  private User createMockAdminUser() {
    User user = new User();
    UserRole admin = new UserRole(
            UserRoles.ADMIN.getRoleId(),
            UserRoles.ADMIN.getRoleName()
    );
    user.addRole(admin);
    return user;
  }
}
