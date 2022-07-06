package org.broadinstitute.consent.http.service;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.db.*;
import org.broadinstitute.consent.http.enumeration.DarStatus;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.DarCollection;
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
import java.util.ArrayList;
import java.util.Collections;
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
    data.setDatasetIds(List.of(dataset.getDataSetId()));
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
    data.setDatasetIds(List.of(dataset.getDataSetId()));
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
    data.setDatasetIds(Collections.singletonList(datasetId));
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
    service = new DarCollectionService(darCollectionDAO, darCollectionServiceDAO, datasetDAO, electionDAO, dataAccessRequestDAO, emailNotifierService, voteDAO, matchDAO);
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
    service = new DarCollectionService(darCollectionDAO, darCollectionServiceDAO, datasetDAO, electionDAO, dataAccessRequestDAO, emailNotifierService, voteDAO, matchDAO);
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
