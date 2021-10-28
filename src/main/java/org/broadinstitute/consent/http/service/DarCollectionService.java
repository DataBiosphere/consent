package org.broadinstitute.consent.http.service;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import org.broadinstitute.consent.http.db.DarCollectionDAO;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.enumeration.ElectionType;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DarCollectionService {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  private final DarCollectionDAO darCollectionDAO;
  private final DataAccessRequestDAO dataAccessRequestDAO;
  private final DatasetDAO datasetDAO;
  private final ElectionDAO electionDAO;

  @Inject
  public DarCollectionService(DarCollectionDAO darCollectionDAO, DatasetDAO datasetDAO, ElectionDAO electionDAO, DataAccessRequestDAO dataAccessRequestDAO) {
    this.darCollectionDAO = darCollectionDAO;
    this.datasetDAO = datasetDAO;
    this.electionDAO = electionDAO;
    this.dataAccessRequestDAO = dataAccessRequestDAO;
  }

  public List<DarCollection> getAllCollections() {
    return addDatasetsToCollections(darCollectionDAO.findAllDARCollections());
  }

  public List<DarCollection> getCollectionsForUserByRoleName(User user, String roleName) {
    List<DarCollection> collections = new ArrayList<>();
    UserRoles selectedRole = UserRoles.getUserRoleFromName(roleName);
    if (Objects.nonNull(selectedRole) && user.hasUserRole(selectedRole)) {
      switch (selectedRole) {
        case ADMIN:
          collections.addAll(getAllCollections());
          break;
        case CHAIRPERSON:
        case MEMBER:
          collections.addAll(getCollectionsByUserDacs(user));
          break;
        case SIGNINGOFFICIAL:
          collections.addAll(getCollectionsByUserInstitution(user));
          break;
        default:
          collections.addAll(getCollectionsForUser(user));
      }
    } else {
      collections.addAll(getCollectionsForUser(user));
    }
    return collections;
  }

  /**
   * Find all DAR Collections by the user's associated DACs
   *
   * @param user The User
   * @return List<DarCollection>
   */
  public List<DarCollection> getCollectionsByUserDacs(User user) {
    List<Integer> dacIds = user.getRoles().stream()
        .map(UserRole::getDacId)
        .filter(Objects::nonNull)
        .distinct()
        .collect(Collectors.toList());
    List<Integer> collectionIds = dacIds.isEmpty() ?
        Collections.emptyList() :
        darCollectionDAO.findDARCollectionIdsByDacIds(dacIds);
    if (!collectionIds.isEmpty()) {
      return addDatasetsToCollections(darCollectionDAO.findDARCollectionByCollectionIds(collectionIds));
    }
    return Collections.emptyList();
  }

  /**
   * Find all DAR Collections by the user's associated Institution
   *
   * @param user The User
   * @return List<DarCollection>
   * @throws IllegalArgumentException If the user does not have a valid institution
   */
  public List<DarCollection> getCollectionsByUserInstitution(User user) throws IllegalArgumentException {
    if (Objects.isNull(user.getInstitutionId())) {
      logger.warn("User does not have a valid institution: " + user.getEmail());
      throw new IllegalArgumentException("User does not have a valid institution");
    }
    List<Integer> collectionIds = darCollectionDAO.findDARCollectionIdsByInstitutionId(user.getInstitutionId());
    if (!collectionIds.isEmpty()) {
      return addDatasetsToCollections(darCollectionDAO.findDARCollectionByCollectionIds(collectionIds));
    }
    return Collections.emptyList();
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
    if (unfilteredDars.isEmpty()) {
      return createEmptyPaginationResponse(0);
    }
    token.setUnfilteredCount(unfilteredDars.size());

    String filterTerm = Objects.isNull(token.getFilterTerm()) ? "" : token.getFilterTerm();
    List<DarCollection> filteredDars = darCollectionDAO.findAllDARCollectionsWithFiltersByUser(filterTerm, user.getDacUserId(), token.getSortField(), token.getSortDirection());
    token.setFilteredCount(filteredDars.size());
    if(filteredDars.isEmpty()) {
      return createEmptyPaginationResponse(token.getUnfilteredCount());
    }

    List<Integer> collectionIds = filteredDars.stream().map(DarCollection::getDarCollectionId).collect(Collectors.toList());
    List<Integer> slice = new ArrayList<>();
    if (token.getStartIndex() <= token.getEndIndex()) {
      slice.addAll(collectionIds.subList(token.getStartIndex(), token.getEndIndex()));
    } else {
      logger.warn(String.format("Invalid pagination state: startIndex: %s endIndex: %s", token.getStartIndex(), token.getEndIndex()));
    }
    List<DarCollection> slicedCollections = darCollectionDAO.findDARCollectionByCollectionIdsWithOrder(slice, token.getSortField(), token.getSortDirection());
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
    if (Objects.isNull(collection)) {
      throw new NotFoundException("Collection with the reference id of " + referenceId + " was not found");
    }
    List<DarCollection> populatedCollections = addDatasetsToCollections(Collections.singletonList(collection));
    return populatedCollections.stream().findFirst().orElse(null);
  }

  public DarCollection getByCollectionId(Integer collectionId) {
    DarCollection collection = darCollectionDAO.findDARCollectionByCollectionId(collectionId);
    if (Objects.isNull(collection)) {
      throw new NotFoundException("Collection with the collection id of " + collectionId + " was not found");
    }
    List<DarCollection> populatedCollections = addDatasetsToCollections(Collections.singletonList(collection));
    return populatedCollections.stream().findFirst().orElse(null);
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

  private PaginationResponse<DarCollection> createEmptyPaginationResponse(Integer unfilteredCount) {
    return new PaginationResponse<DarCollection>()
          .setUnfilteredCount(unfilteredCount)
          .setFilteredCount(0)
          .setFilteredPageCount(1) //should this be 0 or 1?
          .setResults(Collections.emptyList())
          .setPaginationTokens(Collections.emptyList());
  }
  // If an election exists for a DAR within the collection, that DAR cannot be cancelled by the researcher
  // Since it's now under DAC review, it's up to the DAC Chair (or admin) to ultimately decline or cancel via elections
  public DarCollection cancelDarCollection(DarCollection collection) {
    List<DataAccessRequest> dars = collection.getDars();
    List<String> referenceIds = dars.stream()
      .map(DataAccessRequest::getReferenceId)
      .collect(Collectors.toList());

    if(referenceIds.isEmpty()) {
      logger.warn("DAR Collection does not have any associated DAR ids");
      return collection;
    }

    List<Integer> electionIds = electionDAO.getElectionIdsByReferenceIds(referenceIds);
    if(!electionIds.isEmpty()) {
      throw new BadRequestException("Elections present on DARs; cannot cancel collection");
    }
    List<String> nonCanceledIds = dars.stream()
      .filter(DataAccessRequest::isNotCanceled)
      .map(DataAccessRequest::getReferenceId)
      .collect(Collectors.toList());

    //if no dars are valid, simply return the collection (since researcher cancelled DARs should be skipped over)
    if(!nonCanceledIds.isEmpty()) {
      dataAccessRequestDAO.cancelByReferenceIds(nonCanceledIds);
    }
    return darCollectionDAO.findDARCollectionByCollectionId(collection.getDarCollectionId());
  }
}

