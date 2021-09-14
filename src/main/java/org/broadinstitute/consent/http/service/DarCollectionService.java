package org.broadinstitute.consent.http.service;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.broadinstitute.consent.http.db.DarCollectionDAO;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.models.DarCollection;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.DataSet;
import org.broadinstitute.consent.http.models.PaginationResponse;
import org.broadinstitute.consent.http.models.PaginationToken;
import org.broadinstitute.consent.http.models.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DarCollectionService {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  private final DarCollectionDAO darCollectionDAO;
  private final DatasetDAO datasetDAO;

  @Inject
  public DarCollectionService(DarCollectionDAO darCollectionDAO, DatasetDAO datasetDAO) {
    this.darCollectionDAO = darCollectionDAO;
    this.datasetDAO = datasetDAO;
  }

  public List<DarCollection> getAllCollections() {
    return darCollectionDAO.findAllDARCollections();
  }

  public List<DarCollection> getCollectionsForUser(User user) {
    List<DarCollection> collections = darCollectionDAO.findDARCollectionsCreatedByUserId(user.getDacUserId());
    return addDatasetsToCollections(collections);
  }

  /**
   * Find all filtered DAR Collections for a user
   *  1. Fetch unfiltered count: Query #1
   *  2. Fetch filtered collection ids, no limit or offset: Query #2
   *  3. Update the token info with new counts if different
   *  4. Slice that filtered list of ids based on token page # + count per page
   *  5. Get the collections for that list of ids: Query #3
   *
   * @param token A PaginationToken
   * @param user A User
   * @return A PaginationResponse object
   */
  public PaginationResponse<DarCollection> getCollectionsWithFilters(PaginationToken token, User user) {

    List<DarCollection> unfilteredDars = darCollectionDAO.findDARCollectionsCreatedByUserId(user.getDacUserId());
    token.setUnfilteredCount(unfilteredDars.size());

    String filterTerm = Objects.isNull(token.getFilterTerm()) ? "" : token.getFilterTerm();
    List<DarCollection> filteredDars = darCollectionDAO.findAllDARCollectionsWithFiltersByUser(filterTerm, user.getDacUserId(), token.getSortField(), token.getSortDirection());
    token.setFilteredCount(filteredDars.size());

    List<Integer> collectionIds = filteredDars.stream().map(DarCollection::getDarCollectionId).collect(Collectors.toList());
    List<Integer> slice = new ArrayList<>();
    if (token.getStartIndex() <= token.getEndIndex()) {
      slice.addAll(collectionIds.subList(token.getStartIndex(), token.getEndIndex()));
    } else {
      logger.warn(String.format("Invalid pagination state: startIndex: %s endIndex: %s", token.getStartIndex(), token.getEndIndex()));
    }
    List<DarCollection> slicedCollections = darCollectionDAO.findDARCollectionByCollectionIds(slice, token.getSortField(), token.getSortDirection());
    List<PaginationToken> orderedTokens = token.createListOfPaginationTokensFromSelf();
    List<String> orderedTokenStrings = orderedTokens.stream().map(PaginationToken::toBase64).collect(Collectors.toList());
    return new PaginationResponse<DarCollection>()
            .setUnfilteredCount(token.getUnfilteredCount())
            .setFilteredCount(token.getFilteredCount())
            .setFilteredPageCount(orderedTokens.size())
            .setResults(slicedCollections)
            .setPaginationTokens(orderedTokenStrings);
  }

  public DarCollection getByReferenceId(String referenceId) {
    DarCollection collection = darCollectionDAO.findDARCollectionByReferenceId(referenceId);
    List<DarCollection> updatedCollections = addDatasetsToCollections(Collections.singletonList(collection));
    return updatedCollections.stream().findFirst().orElse(null);
  }

  public DarCollection getByCollectionId(Integer collectionId) {
    DarCollection collection = darCollectionDAO.findDARCollectionByCollectionId(collectionId);
    List<DarCollection> updatedCollections = addDatasetsToCollections(Collections.singletonList(collection));
    return updatedCollections.stream().findFirst().orElse(null);
  }

  public List<DarCollection> getCollectionsByUser(User user) {
    List<DarCollection> collections = darCollectionDAO.findDARCollectionsCreatedByUserId(user.getDacUserId());
    return addDatasetsToCollections(collections);
  }

  public DarCollection createDarCollection(String darCode, User user) {
    Integer collectionId = darCollectionDAO.insertDarCollection(darCode, user.getDacUserId(), new Date());
    return getByCollectionId(collectionId);
  }

  public void deleteDarCollectionById(Integer collectionId) {
    darCollectionDAO.deleteByCollectionId(collectionId);
  }

  public DarCollection updateDarCollection(Integer collectionId, User user) {
    darCollectionDAO.updateDarCollection(collectionId, user.getDacUserId(), new Date());
    return getByCollectionId(collectionId);
  }

  public List<DarCollection> addDatasetsToCollections(List<DarCollection> collections) {

    List<Integer> datasetIds = collections.stream()
      .map(DarCollection::getDars)
      .flatMap(Collection::stream)
      .map(d -> d.getData().getDatasetIds())
      .flatMap(Collection::stream)
      .collect(Collectors.toList());

    if(!datasetIds.isEmpty()) {
      Set<DataSet> datasets = datasetDAO.findDatasetWithDataUseByIdList(datasetIds);
      Map<Integer, DataSet> datasetMap = datasets.stream()
          .collect(Collectors.toMap(DataSet::getDataSetId, Function.identity()));

      return collections.stream().map(c -> {
        Set<DataSet> collectionDatasets = c.getDars().stream()
          .map(DataAccessRequest::getData)
          .map(DataAccessRequestData::getDatasetIds)
          .flatMap(Collection::stream)
          .map(datasetMap::get)
          .collect(Collectors.toSet());
        DarCollection copy = c.deepCopy();
        copy.setDatasets(collectionDatasets);
        return copy;
      }).collect(Collectors.toList());
    }
    // There were no datasets to add, so we return the original list
    return collections;
  }

}
