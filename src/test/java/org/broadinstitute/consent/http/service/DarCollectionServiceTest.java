package org.broadinstitute.consent.http.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.ws.rs.BadRequestException;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.db.DarCollectionDAO;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.models.DarCollection;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.PaginationResponse;
import org.broadinstitute.consent.http.models.PaginationToken;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class DarCollectionServiceTest {

  private DarCollectionService service;

  @Mock private DarCollectionDAO darCollectionDAO;
  @Mock private DatasetDAO datasetDAO;
  @Mock private ElectionDAO electionDAO;
  @Mock private DataAccessRequestDAO dataAccessRequestDAO;

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
    user.addRole(new UserRole(UserRoles.CHAIRPERSON.getRoleId(), UserRoles.CHAIRPERSON.getRoleName()));
    user.addRole(new UserRole(UserRoles.MEMBER.getRoleId(), UserRoles.MEMBER.getRoleName()));
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
    user.setDacUserId(1);
    user.addRole(new UserRole(UserRoles.RESEARCHER.getRoleId(), UserRoles.RESEARCHER.getRoleName()));
    DarCollection collection = new DarCollection();
    when(darCollectionDAO.findDARCollectionsCreatedByUserId(user.getDacUserId())).thenReturn(List.of(collection));
    initService();

    List<DarCollection> collections = service.getCollectionsForUserByRoleName(user, UserRoles.RESEARCHER.getRoleName());
    assertEquals(1, collections.size());
  }

  // TODO:
  // Tests for getCollectionsByUserDacs
  // Tests for getCollectionsByUserInstitution

  @Test
  public void testGetCollectionsWithFiltersByPage() {
    IntStream.rangeClosed(1, 8)
        .forEach(
            page -> {
              int filteredCount = 75;
              int unfilteredCount = 100;
              PaginationToken token = new PaginationToken(page, 10, "darCode", "DESC", null, DarCollection.acceptableSortFields, DarCollection.defaultTokenSortField);
              initWithPaginationToken(token, unfilteredCount, filteredCount);
              PaginationResponse<DarCollection> response = service.getCollectionsWithFilters(token, user);
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
    PaginationToken token = new PaginationToken(1, 10, "darCode", "DESC", null, DarCollection.acceptableSortFields, DarCollection.defaultTokenSortField);
    when(darCollectionDAO.findDARCollectionsCreatedByUserId(anyInt())).thenReturn(Collections.emptyList());
    initService();

    PaginationResponse<DarCollection> response = service.getCollectionsWithFilters(token, user);

    assertEquals(1, response.getFilteredPageCount().intValue());
    assertEquals(0, response.getUnfilteredCount().intValue());
    assertEquals(0, response.getFilteredCount().intValue());
  }

  @Test
  public void testGetCollectionsWithFilters_EmptyFiltered() {
    PaginationToken token = new PaginationToken(1, 10, "darCode", "DESC", null, DarCollection.acceptableSortFields, DarCollection.defaultTokenSortField);
    List<DarCollection> collections = createMockCollections(3);
    when(darCollectionDAO.findDARCollectionsCreatedByUserId(anyInt())).thenReturn(collections);
    when(darCollectionDAO.findAllDARCollectionsWithFiltersByUser(anyString(), anyInt(), anyString(), anyString())).thenReturn(Collections.emptyList());
    initService();

    PaginationResponse<DarCollection> response = service.getCollectionsWithFilters(token, user);

    assertEquals(1, response.getFilteredPageCount().intValue());
    assertEquals(collections.size(), response.getUnfilteredCount().intValue());
    assertEquals(0, response.getFilteredCount().intValue());
  }

  @Test
  public void testGetCollectionsWithFiltersByPageLessThanPageSize() {
      int filteredCount = 3;
      int unfilteredCount = 5;
      PaginationToken token = new PaginationToken(1, 10, "darCode", "DESC", null, DarCollection.acceptableSortFields, DarCollection.defaultTokenSortField);
      initWithPaginationToken(token, unfilteredCount, filteredCount);
      PaginationResponse<DarCollection> response = service.getCollectionsWithFilters(token, user);

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
      PaginationResponse<DarCollection> response = service.getCollectionsWithFilters(token, user);
      assertTrue(response.getResults().isEmpty());
  }

  @Test
  public void testAddDatasetsToCollection() {
    List<DarCollection> collections = new ArrayList<>();
    Set<DataSet> datasets = new HashSet<>();
    collections.add(generateMockDarCollection(datasets));
    List<Integer> datasetIds = datasets.stream()
      .map(DataSet::getDataSetId)
      .sorted()
      .collect(Collectors.toList());

    when(datasetDAO.findDatasetWithDataUseByIdList(anyList())).thenReturn(datasets);
    initService();

    collections = service.addDatasetsToCollections(collections);
    assertEquals(1, collections.size());

    DarCollection collection = collections.get(0);
    Set<DataSet> datasetsFromCollection = collection.getDatasets();
    assertEquals(datasetIds.size(), datasetsFromCollection.size());

    List<Integer> collectionDatasetIds = datasetsFromCollection.stream()
      .map(DataSet::getDataSetId)
      .sorted()
      .collect(Collectors.toList());
    assertEquals(datasetIds, collectionDatasetIds);
  }

  @Test
  public void testCancelDarCollection_noElections() {
    Set<DataSet> datasets = new HashSet<>();
    DarCollection collection = generateMockDarCollection(datasets);
    collection.getDars().forEach(d -> {
      d.getData().setStatus("Canceled");
    });
    List<Election> elections = new ArrayList<>();
    User user = new User();
    user.setDacUserId(RandomUtils.nextInt(1,100));

    when(electionDAO.findLastElectionsByReferenceIdsAndType(anyList(), anyString())).thenReturn(elections);
    doNothing().when(dataAccessRequestDAO).cancelByReferenceIds(anyList());
    when(darCollectionDAO.findDARCollectionByCollectionId(any())).thenReturn(collection);
    initService();

    DarCollection canceledCollection = service.cancelDarCollection(collection, user);
    for (DataAccessRequest collectionDar : canceledCollection.getDars()) {
      assertEquals("canceled", collectionDar.getData().getStatus().toLowerCase());
    }
  }

  @Test(expected = BadRequestException.class)
  public void testCancelDarCollection_electionPresent() {
    Set<DataSet> datasets = new HashSet<>();
    DarCollection collection = generateMockDarCollection(datasets);
    List<Election> elections = Collections.singletonList(new Election());
    User user = new User();
    user.setDacUserId(RandomUtils.nextInt(1, 100));

    when(electionDAO.findLastElectionsByReferenceIdsAndType(anyList(), anyString())).thenReturn(elections);
    doNothing().when(dataAccessRequestDAO).cancelByReferenceIds(anyList());
    when(darCollectionDAO.findDARCollectionByCollectionId(any())).thenReturn(collection);
    initService();

    service.cancelDarCollection(collection, user);
  }

  private DarCollection generateMockDarCollection(Set<DataSet> datasets) {
    DarCollection collection = new DarCollection();
    List<DataAccessRequest> dars = new ArrayList<>();
    dars.add(generateMockDarWithDatasetId(datasets));
    dars.add(generateMockDarWithDatasetId(datasets));
    collection.setDars(dars);
    return collection;
  }

  private DataAccessRequest generateMockDarWithDatasetId(Set<DataSet> datasets) {
    DataAccessRequest dar = new DataAccessRequest();
    DataAccessRequestData data = new DataAccessRequestData();

    Integer datasetId = RandomUtils.nextInt(1, 100);
    datasets.add(generateMockDatasetWithDataUse(datasetId));
    data.setDatasetIds(Collections.singletonList(datasetId));
    dar.setData(data);
    return dar;
  }

  private DataSet generateMockDatasetWithDataUse(Integer datasetId) {
    DataSet dataset = new DataSet();
    dataset.setDataSetId(datasetId);
    return dataset;
  }

  private void initService() {
    service = new DarCollectionService(darCollectionDAO, datasetDAO, electionDAO, dataAccessRequestDAO);
  }

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
    service = new DarCollectionService(darCollectionDAO, datasetDAO, electionDAO, dataAccessRequestDAO);
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
}
