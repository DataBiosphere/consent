package org.broadinstitute.consent.http.service;

import com.google.inject.Inject;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.broadinstitute.consent.http.db.DarCollectionDAO;
import org.broadinstitute.consent.http.models.DarCollection;
import org.broadinstitute.consent.http.models.PaginationToken;
import org.broadinstitute.consent.http.models.User;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class DarCollectionService {

  private final DarCollectionDAO darCollectionDAO;

  @Inject
  public DarCollectionService(DarCollectionDAO darCollectionDAO) {
    this.darCollectionDAO = darCollectionDAO;
  }

  public List<DarCollection> getAllCollections() {
    return Collections.emptyList();
  }

  public ImmutablePair<List<DarCollection>, List<PaginationToken>> getCollectionsWithFilters(
      PaginationToken token) {
    // TODO: Query for collections using token filters
    // 1. Fetch unfiltered count: Query #1
    // 2. Fetch filtered collection ids, no limit or offset: Query #2
    // 3. Slice that filtered list of ids based on token page # + count per page
    // 4. Get the collections for that list of ids: Query #3
    // 5. Update the token info with new counts if different
    return ImmutablePair.of(Collections.emptyList(), makePaginationTokensFromToken(token));
  }

  /**
   * Generate an ordered sequence of tokens from the current token.
   *
   * @param token Current PaginationToken
   * @return Ordered list of PaginationTokens
   */
  private List<PaginationToken> makePaginationTokensFromToken(PaginationToken token) {
    int currentPage = token.getPage();
    int lastPage = token.getFilteredPageCount();
    return IntStream.rangeClosed(1, lastPage)
        .mapToObj(
            i -> {
              if (i == currentPage) {
                return token;
              }
              return new PaginationToken(
                  i,
                  token.getPageSize(),
                  token.getSortField(),
                  token.getSortDirection(),
                  token.getFilterTerms(),
                  token.getFilteredCount(),
                  token.getFilteredPageCount(),
                  token.getUnfilteredCount());
            })
        .collect(Collectors.toList());
  }

  public DarCollection getByReferenceId(String referenceId) {
    return darCollectionDAO.findDARCollectionByReferenceId(referenceId);
  }

  public DarCollection getByCollectionId(Integer collectionId) {
    return darCollectionDAO.findDARCollectionByCollectionId(collectionId);
  }

  public List<DarCollection> getCollectionsByUser(User user) {
    return darCollectionDAO.findDARCollectionsCreatedByUserId(user.getDacUserId());
  }

  public DarCollection createDarCollection(String darCode, User user) {
    Integer collectionId =
        darCollectionDAO.insertDarCollection(darCode, user.getDacUserId(), new Date());
    return darCollectionDAO.findDARCollectionByCollectionId(collectionId);
  }

  public void deleteDarCollectionById(Integer darCollectionId) {
    darCollectionDAO.deleteByCollectionId(darCollectionId);
  }

  public DarCollection updateDarCollection(Integer darCollectionId, User user) {
    return null;
  }
}
