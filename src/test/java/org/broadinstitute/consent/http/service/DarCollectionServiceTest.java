package org.broadinstitute.consent.http.service;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.broadinstitute.consent.http.db.DarCollectionDAO;
import org.broadinstitute.consent.http.models.DarCollection;
import org.broadinstitute.consent.http.models.PaginationResponse;
import org.broadinstitute.consent.http.models.PaginationToken;
import org.broadinstitute.consent.http.models.User;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

// TODO: The token tests can be parameterized for a better range of conditions.
public class DarCollectionServiceTest {

  private DarCollectionService service;

  @Mock private DarCollectionDAO darCollectionDAO;

  @Mock private User user;

  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  private void initWithPaginationToken(PaginationToken token, int filterCount) {
    List<DarCollection> unfilteredDars = createMockDars(filterCount + RandomUtils.nextInt(10, 100));
    // Start the filtered ids at index 0 so tests can make more assertions.
    List<DarCollection> filteredDars = unfilteredDars.subList(0, filterCount - 1);
    token.setUnfilteredCount(unfilteredDars.size());
    token.setFilteredCount(filteredDars.size());
    List<DarCollection> collectionIdDars = filteredDars.subList(token.getStartIndex(), token.getEndIndex());
    when(darCollectionDAO.findDARCollectionsCreatedByUserId(any())).thenReturn(unfilteredDars);
    when(darCollectionDAO.findAllDARCollectionsWithFiltersByUser(any(), any(), any(), any())).thenReturn(filteredDars);
    when(darCollectionDAO.findDARCollectionByCollectionIds(any(), any(), any())).thenReturn(collectionIdDars);
    service = new DarCollectionService(darCollectionDAO);
  }

  @Test
  public void testGetCollectionsWithFilters_Page1() {
    PaginationToken token = new PaginationToken(1, 10, null, null, null);
    initWithPaginationToken(token, 25);

    PaginationResponse<DarCollection> response = service.getCollectionsWithFilters(token, user);
    assertEquals((int) token.getPageSize(), response.getResults().size());
    assertEquals(Integer.valueOf(1), response.getResults().get(0).getDarCollectionId());
  }

  @Test
  public void testGetCollectionsWithFilters_Page2() {
    PaginationToken token = new PaginationToken(2, 10, null, null, null);
    initWithPaginationToken(token, 50);

    PaginationResponse<DarCollection> response = service.getCollectionsWithFilters(token, user);
    assertEquals((int) token.getPageSize(), response.getResults().size());
    assertEquals(Integer.valueOf(11), response.getResults().get(0).getDarCollectionId());
  }

  @Test
  public void testGetCollectionsWithFilters_Page10() {
    PaginationToken token = new PaginationToken(8, 10, null, null, null);
    initWithPaginationToken(token, 75);

    PaginationResponse<DarCollection> response = service.getCollectionsWithFilters(token, user);
    /*
      page 1: ids 01-10
      page 2: ids 11-20
      page 3: ids 21-30
      ...
      page 8: ids 71-75
     */
    int expectedLastPageSize = token.getFilteredCount() % token.getPageSize() - 1;
    assertEquals(expectedLastPageSize, response.getResults().size());
    assertEquals(Integer.valueOf(71), response.getResults().get(0).getDarCollectionId());
  }

  private List<DarCollection> createMockDars(int count) {
    return IntStream
            .rangeClosed(1, count)
            .mapToObj(i -> {
              DarCollection collection = new DarCollection();
              collection.setDarCollectionId(i);
              collection.setDarCode(RandomStringUtils.randomAlphanumeric(5));
              collection.setCreateUserId(1);
              return collection;
            })
            .collect(Collectors.toList());
  }

}
