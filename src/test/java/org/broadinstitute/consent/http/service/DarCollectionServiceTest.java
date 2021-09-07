package org.broadinstitute.consent.http.service;

import org.apache.commons.lang3.RandomStringUtils;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class DarCollectionServiceTest {

  private DarCollectionService service;

  @Mock private DarCollectionDAO darCollectionDAO;

  @Mock private User user;

  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  private void init() {
    service = new DarCollectionService(darCollectionDAO);
  }

  @Test
  public void testGetCollectionsWithFilters() {
    PaginationToken token = new PaginationToken(1, 10, null, null, null);
    List<DarCollection> unfilteredDars = createMockDars(100);
    List<DarCollection> filteredDars = unfilteredDars.subList(0, 75);
    List<DarCollection> collectionIdDars = filteredDars.subList(0, token.getPageSize());

    when(darCollectionDAO.findDARCollectionsCreatedByUserId(any())).thenReturn(unfilteredDars);
    when(darCollectionDAO.findAllDARCollectionsWithFiltersByUser(any(), any(), any(), any())).thenReturn(filteredDars);
    when(darCollectionDAO.findDARCollectionByCollectionIds(any(), any(), any())).thenReturn(collectionIdDars);
    init();

    PaginationResponse<DarCollection> response = service.getCollectionsWithFilters(token, user);
    response.getPaginationTokens().forEach(System.out::println);
    response.getResults().forEach(System.out::println);
  }

  private List<DarCollection> createMockDars(int count) {
    return IntStream
            .rangeClosed(1, count)
            .mapToObj(i -> {
              DarCollection collection = new DarCollection();
              collection.setDarCollectionId(i);
              collection.setDarCode(RandomStringUtils.randomAlphanumeric(5));
              return collection;
            })
            .collect(Collectors.toList());
  }

}
