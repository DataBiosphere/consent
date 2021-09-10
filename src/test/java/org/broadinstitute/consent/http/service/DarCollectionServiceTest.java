package org.broadinstitute.consent.http.service;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.db.DarCollectionDAO;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.models.DarCollection;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.PaginationResponse;
import org.broadinstitute.consent.http.models.PaginationToken;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.dto.DatasetDTO;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

public class DarCollectionServiceTest {

  private DarCollectionService service;

  @Mock private DarCollectionDAO darCollectionDAO;
  @Mock private DatasetDAO datasetDAO;

  @Mock private User user;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testGetCollectionsWithFiltersByPage() {
    IntStream.rangeClosed(1, 8)
        .forEach(
            page -> {
              int filteredCount = 75;
              int unfilteredCount = 100;
              PaginationToken token = new PaginationToken(page, 10, null, null, null);
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
  public void testGetCollectionsWithFiltersByPageLessThanPageSize() {
      int filteredCount = 3;
      int unfilteredCount = 5;
      PaginationToken token = new PaginationToken(1, 10, null, null, null);
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
      PaginationToken token = new PaginationToken(2, 10, null, null, null);
      initWithPaginationToken(token, unfilteredCount, filteredCount);

      // Start index will be > end index in this case since we're trying to get results 11-20 when
      // there are only 5 items in the results array, so there should be 0 results returned
      PaginationResponse<DarCollection> response = service.getCollectionsWithFilters(token, user);
      assertTrue(response.getResults().isEmpty());
  }

  @Test
  public void testAddDatasetsToCollection() {
    List<DarCollection> collections = new ArrayList<DarCollection>();
    Set<DatasetDTO> datasets = new HashSet<DatasetDTO>();
    collections.add(generateMockDarCollection(datasets));
    List<Integer> datasetIds = datasets.stream()
      .map(d -> d.getDataSetId())
      .sorted()
      .collect(Collectors.toList());

    when(datasetDAO.findDatasetDTOByIdList(anyList())).thenReturn(datasets);
    initService();

    service.addDatasetsToCollections(collections);
    assertEquals(1, collections.size());

    DarCollection collection = collections.get(0);
    Set<DatasetDTO> datasetsFromCollection = collection.getDatasets();
    assertEquals(2, datasetsFromCollection.size());

    List<Integer> collectionDatasetIds = datasetsFromCollection.stream()
      .map(d -> d.getDataSetId())
      .sorted()
      .collect(Collectors.toList());
    assertEquals(datasetIds, collectionDatasetIds);
  }

  private DarCollection generateMockDarCollection(Set<DatasetDTO> datasets) {
    DarCollection collection = new DarCollection();
    List<DataAccessRequest> dars = new ArrayList<DataAccessRequest>();
    dars.add(generateMockDarWithDatasetId(datasets));
    dars.add(generateMockDarWithDatasetId(datasets));
    return collection;
  }

  private DataAccessRequest generateMockDarWithDatasetId(Set<DatasetDTO> datasets) {
    DataAccessRequest dar = new DataAccessRequest();
    DataAccessRequestData data = new DataAccessRequestData();
    
    Integer datasetId = RandomUtils.nextInt(1, 100);
    datasets.add(generateMockDatasetDTO(datasetId));
    data.setDatasetIds(Collections.singletonList(datasetId));
    dar.setData(data);
    return dar;
  }

  private DatasetDTO generateMockDatasetDTO(Integer datasetId) {
    DatasetDTO dataset = new DatasetDTO();
    dataset.setDataSetId(datasetId);
    return dataset;
  }

  private void initService() {
    service = new DarCollectionService(darCollectionDAO, datasetDAO);
  }

  private void initWithPaginationToken(PaginationToken token, int unfilteredCount, int filteredCount) {
    MockitoAnnotations.openMocks(this);
    List<DarCollection> unfilteredDars = createMockDars(unfilteredCount);
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
    when(darCollectionDAO.findDARCollectionByCollectionIds(any(), any(), any())).thenReturn(collectionIdDars);
    service = new DarCollectionService(darCollectionDAO, datasetDAO);
  }

  private List<DarCollection> createMockDars(int count) {
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
