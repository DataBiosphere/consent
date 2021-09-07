package org.broadinstitute.consent.http.service;

import com.google.inject.Inject;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.broadinstitute.consent.http.db.DarCollectionDAO;
import org.broadinstitute.consent.http.models.DarCollection;
import org.broadinstitute.consent.http.models.PaginationToken;
import org.broadinstitute.consent.http.models.User;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class DarCollectionService {

  private final DarCollectionDAO darCollectionDAO;

  @Inject
  public DarCollectionService(DarCollectionDAO darCollectionDAO) {
    this.darCollectionDAO = darCollectionDAO;
  }

  public List<DarCollection> getAllCollections() {
    return darCollectionDAO.findAllDARCollections();
  }

  public ImmutablePair<List<DarCollection>, List<PaginationToken>> getCollectionsWithFilters(
      PaginationToken token, User user) {
    // TODO: Somehow filter on user access.
    // Researcher: only mine
    // Admin: all of them
    // Chair/member: only those in their DACs

    // TODO: Query for collections using token filters
    // 1. Fetch unfiltered count: Query #1
    // 2. Fetch filtered collection ids, no limit or offset: Query #2
    // 3. Slice that filtered list of ids based on token page # + count per page
    // 4. Get the collections for that list of ids: Query #3
    // 5. Update the token info with new counts if different

    // This is where we need to filter by access level/role/user
    List<DarCollection> unfilteredDars = darCollectionDAO.findAllDARCollections();

    String filterTerm = Objects.isNull(token.getFilterTerm()) ? "" : token.getFilterTerm();
    List<DarCollection> filteredDars = darCollectionDAO.findAllDARCollectionsWithFilters(filterTerm, token.getSortField(), token.getSortDirection());

    List<Integer> collectionIds = filteredDars.stream().map(DarCollection::getDarCollectionId).collect(Collectors.toList());
    // TODO: What is the slice
    int offset = token.getPageSize() * (token.getPage() - 1);
    // TODO: handle exceptions and inclusivity
    List<Integer> slice = collectionIds.subList(offset, offset + token.getPageSize());

    List<DarCollection> slicedCollections = darCollectionDAO.findDARCollectionByCollectionIds(slice, token.getSortField(), token.getSortDirection());

    // TODO: Update the return value to something that makes more sense.
    return ImmutablePair.of(slicedCollections, token.createListOfPaginationTokensFromSelf());
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

  public void deleteDarCollectionById(Integer collectionId) {
    darCollectionDAO.deleteByCollectionId(collectionId);
  }

  public DarCollection updateDarCollection(Integer collectionId, User user) {
    darCollectionDAO.updateDarCollection(collectionId, user.getDacUserId(), new Date());
    return null;
  }
}
