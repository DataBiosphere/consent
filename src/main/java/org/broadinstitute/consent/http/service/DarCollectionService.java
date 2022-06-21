package org.broadinstitute.consent.http.service;

import com.google.inject.Inject;
import org.broadinstitute.consent.http.db.DarCollectionDAO;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
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
import org.broadinstitute.consent.http.resources.Resource;
import org.broadinstitute.consent.http.service.dao.DarCollectionServiceDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
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

public class DarCollectionService {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  private final DarCollectionDAO darCollectionDAO;
  private final DarCollectionServiceDAO collectionServiceDAO;
  private final DataAccessRequestDAO dataAccessRequestDAO;
  private final DatasetDAO datasetDAO;
  private final ElectionDAO electionDAO;
  private final VoteDAO voteDAO;
  private final EmailNotifierService emailNotifierService;

  @Inject
  public DarCollectionService(DarCollectionDAO darCollectionDAO, DarCollectionServiceDAO collectionServiceDAO, DatasetDAO datasetDAO, ElectionDAO electionDAO, DataAccessRequestDAO dataAccessRequestDAO, EmailNotifierService emailNotifierService, VoteDAO voteDAO) {
    this.darCollectionDAO = darCollectionDAO;
    this.collectionServiceDAO = collectionServiceDAO;
    this.datasetDAO = datasetDAO;
    this.electionDAO = electionDAO;
    this.dataAccessRequestDAO = dataAccessRequestDAO;
    this.emailNotifierService = emailNotifierService;
    this.voteDAO = voteDAO;
  }

  public List<Integer> findDatasetIdsByUser(User user) {
    return datasetDAO.findDatasetsByAuthUserEmail(user.getEmail())
        .stream()
        .map(Dataset::getDataSetId)
        .collect(Collectors.toList());
  }

  public List<DarCollection> getAllCollections() {
    return addDatasetsToCollections(darCollectionDAO.findAllDARCollections(), null);
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
//          List<Dataset> userDatasets = datasetDAO.findDatasetsByAuthUserEmail(user.getEmail());
//          List<DarCollection> userCollections = getCollectionsByUserDacs(user);
//          userCollections.forEach(c -> {
//            c.getDatasets().retainAll(userDatasets);
//          });
//          collections.addAll(userCollections);
          collections.addAll(getCollectionsByUserDacs(user, true));
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
    return collections.stream().filter(collection -> !containsCanceledDars(collection)).collect(Collectors.toList());
  }

  private boolean containsCanceledDars(DarCollection collection) {
    return collection.getDars().values().stream().anyMatch(DataAccessRequest::isCanceled);
  }

  /**
   * Find all DAR Collections by the user's associated DACs
   *
   * @param user The User
   * @param filterByUserDacDatasets Specifies whether to filter on user-DAC-datasets
   * @return List<DarCollection>
   */
  public List<DarCollection> getCollectionsByUserDacs(User user, Boolean filterByUserDacDatasets) {
    List<Integer> dacIds = user.getRoles().stream()
        .map(UserRole::getDacId)
        .filter(Objects::nonNull)
        .distinct()
        .collect(Collectors.toList());
    List<Integer> collectionIds = dacIds.isEmpty() ?
        Collections.emptyList() :
        darCollectionDAO.findDARCollectionIdsByDacIds(dacIds);
    List<Dataset> userDatasets = filterByUserDacDatasets ?
        datasetDAO.findDatasetsByAuthUserEmail(user.getEmail()) :
        Collections.emptyList();
    if (!collectionIds.isEmpty()) {
      return addDatasetsToCollections(darCollectionDAO.findDARCollectionByCollectionIds(collectionIds), userDatasets);
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
      return addDatasetsToCollections(darCollectionDAO.findDARCollectionByCollectionIds(collectionIds), null);
    }
    return Collections.emptyList();
  }

  public List<DarCollection> getCollectionsForUser(User user) {
    List<DarCollection> collections = darCollectionDAO.findDARCollectionsCreatedByUserId(user.getDacUserId());
    return addDatasetsToCollections(collections, null);
  }

  /*
    Role based queries for Collection, this method should be utilized for search queries and pagination requests
  */
  public PaginationResponse<DarCollection> queryCollectionsByFiltersAndUserRoles(User user, PaginationToken token, String roleName) {
    List<Integer> dacIds = getDacIdsFromUser(user);
    Integer unfilteredCount = getCountForUnfilteredQueryByRole(user, roleName);
    if(unfilteredCount == 0) {
      createEmptyPaginationResponse(unfilteredCount);
    }
    token.setUnfilteredCount(unfilteredCount);

    List<DarCollection> filteredCollections = getFilteredCollectionsByUserRole(user, roleName, token, dacIds);
    if(filteredCollections.isEmpty()) {
      return createEmptyPaginationResponse(0);
    }
    token.setFilteredCount(filteredCollections.size());
    List<DarCollection> slicedCollections = new ArrayList<>();
    if(token.getStartIndex() <= token.getEndIndex()) {
      slicedCollections = filteredCollections.subList(token.getStartIndex(), token.getEndIndex());
    } else {
      logger.warn(String.format("Invalid pagination state: startIndex %s endIndex: %s", token.getStartIndex(), token.getEndIndex()));
    }
    List<PaginationToken> orderedTokens = token.createListOfPaginationTokensFromSelf();
    List<String> orderedTokenStrings = orderedTokens.stream().map(PaginationToken::toBase64).collect(Collectors.toList());
    return new PaginationResponse<DarCollection>()
      .setUnfilteredCount(token.getUnfilteredCount())
      .setFilteredCount(token.getFilteredCount())
      .setFilteredPageCount(orderedTokens.size())
      .setResults(slicedCollections)
      .setPaginationTokens(orderedTokenStrings);
  }

  //Helper function for queryCollectionsByFilterAndUserRoles
  //Function verifies user role permission and executes query via DAO methods
  private List<DarCollection> getFilteredCollectionsByUserRole(User user, String userRole, PaginationToken token, List<Integer> dacIds) {
    String sortOrder = Objects.isNull(token.getSortDirection()) ? DarCollection.defaultTokenSortOrder : token.getSortDirection();
    String sortField = Objects.isNull(token.getSortField()) ? DarCollection.defaultTokenSortField : token.getSortField();
    String filterTerm = Objects.isNull(token.getFilterTerm()) ? "" : token.getFilterTerm();
    List<DarCollection> collections;
    List<Integer> collectionIds;

    switch (userRole) {
      case Resource.ADMIN:
        collections = darCollectionDAO.getFilteredCollectionsForAdmin(sortField, sortOrder, filterTerm);
        break;
      case Resource.SIGNINGOFFICIAL:
        collections = darCollectionDAO.getFilteredCollectionsForSigningOfficial(sortField, sortOrder, user.getInstitutionId(), filterTerm);
        break;
      case Resource.CHAIRPERSON:
      case Resource.MEMBER:
        collectionIds = darCollectionDAO.findDARCollectionIdsByDacIds(dacIds);
        collections = darCollectionDAO.getFilteredCollectionsForDACByCollectionIds(sortField, sortOrder, collectionIds, filterTerm);
        break;
      default:
        collections = darCollectionDAO.getFilteredListForResearcher(sortField, sortOrder, user.getDacUserId(), filterTerm);
    }

    return addDatasetsToCollections(collections, null);
  }

  private List<Integer> getDacIdsFromUser (User user) {
    return user.getRoles().stream()
            .map(UserRole::getDacId)
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());
  }

  //Helper method for queryCollectionsByFiltersAndUserRoles
  //Verifies user role and determines unfiltered count total via DAO methods
  private Integer getCountForUnfilteredQueryByRole(User user, String userRole) {
    Integer size = 0;

    switch(userRole) {
      case Resource.ADMIN:
        size = darCollectionDAO.returnUnfilteredCollectionCount(); //Make new query to count specific query
        break;
      case Resource.SIGNINGOFFICIAL:
        size = darCollectionDAO.returnUnfilteredCountForInstitution(user.getInstitutionId()); //make new query to only get count
        break;
      case Resource.CHAIRPERSON:
      case Resource.MEMBER:
        List<Integer> dacIds = user.getRoles()
                .stream()
                .map(UserRole::getDacId)
                .distinct()
                .collect(Collectors.toList());
        size = (Integer) darCollectionDAO.findDARCollectionIdsByDacIds(dacIds).size();
        break;
      default:
        size = darCollectionDAO.returnUnfilteredResearcherCollectionCount(user.getDacUserId());
    }
    return size;
  }

  public DarCollection getByReferenceId(String referenceId) {
    DarCollection collection = darCollectionDAO.findDARCollectionByReferenceId(referenceId);
    if (Objects.isNull(collection)) {
      throw new NotFoundException("Collection with the reference id of " + referenceId + " was not found");
    }
    List<DarCollection> populatedCollections = addDatasetsToCollections(Collections.singletonList(collection), null);
    return populatedCollections.stream().findFirst().orElse(null);
  }

  public DarCollection getByCollectionId(Integer collectionId) {
    DarCollection collection = darCollectionDAO.findDARCollectionByCollectionId(collectionId);
    if (Objects.isNull(collection)) {
      throw new NotFoundException("Collection with the collection id of " + collectionId + " was not found");
    }
    List<DarCollection> populatedCollections = addDatasetsToCollections(Collections.singletonList(collection), null);
    return populatedCollections.stream().findFirst().orElse(null);
  }

  public List<DarCollection> addDatasetsToCollections(List<DarCollection> collections, List<Dataset> userDatasets) {
    // get datasetIds from each DAR from each collection
    List<Integer> datasetIds = collections.stream()
      .map(d-> d.getDars().values())
      .flatMap(Collection::stream)
      .map(d -> d.getData().getDatasetIds())
      .flatMap(Collection::stream)
      .collect(Collectors.toList());
    if(!datasetIds.isEmpty()) {
      // gets all datasets that should belong to the collection
      Set<Dataset> datasets = (Objects.nonNull(userDatasets)) ?
          userDatasets.stream()
              .filter(d -> datasetIds
              .contains(d.getDataSetId()))
              .collect(Collectors.toSet()) :
              datasetDAO.findDatasetWithDataUseByIdList(datasetIds);
      Map<Integer, Dataset> datasetMap = datasets.stream()
          .collect(Collectors.toMap(Dataset::getDataSetId, Function.identity()));

      return collections.stream().map(c -> {
        Set<Dataset> collectionDatasets = c.getDars().values().stream()
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

  /**
   * Cancel a DarCollection as a researcher.
   *
   * If an election exists for a DAR within the collection, that DAR cannot be cancelled by the
   * researcher. Since it's now under DAC review, it's up to the DAC Chair (or admin) to
   * ultimately decline or cancel the elections for the collection.
   *
   * @param collection The DarCollection
   * @return The canceled DarCollection
   */
  public DarCollection cancelDarCollectionAsResearcher(DarCollection collection) {
    Collection<DataAccessRequest> dars = collection.getDars().values();
    List<String> referenceIds = dars.stream()
      .map(DataAccessRequest::getReferenceId)
      .collect(Collectors.toList());

    if (referenceIds.isEmpty()) {
      logger.warn("DAR Collection does not have any associated DAR ids");
      return collection;
    }

    List<Election> elections = electionDAO.findLastElectionsByReferenceIds(referenceIds);
    if(!elections.isEmpty()) {
      throw new BadRequestException("Elections present on DARs; cannot cancel collection");
    }

    // Cancel active dars for the researcher
    List<String> activeDarIds = dars.stream()
      .filter(d -> !DataAccessRequest.isCanceled(d))
      .map(DataAccessRequest::getReferenceId)
      .collect(Collectors.toList());
    if (!activeDarIds.isEmpty()) {
      dataAccessRequestDAO.cancelByReferenceIds(activeDarIds);
    }

    return getByCollectionId(collection.getDarCollectionId());
  }

  /**
   * Cancel Elections for a DarCollection as an admin.
   *
   * Admins can cancel all elections in a DarCollection
   *
   * @param collection The DarCollection
   * @return The DarCollection whose elections have been canceled
   */
  public DarCollection cancelDarCollectionElectionsAsAdmin(DarCollection collection) {
    Collection<DataAccessRequest> dars = collection.getDars().values();
    List<String> referenceIds = dars.stream()
      .map(DataAccessRequest::getReferenceId)
      .collect(Collectors.toList());

    if (referenceIds.isEmpty()) {
      logger.warn("DAR Collection does not have any associated DAR ids");
      return collection;
    }

    // Cancel all DAR elections
    cancelElectionsForReferenceIds(referenceIds);

    return darCollectionDAO.findDARCollectionByCollectionId(collection.getDarCollectionId());
  }

  /**
   * Cancel Elections for a DarCollection as a chairperson.
   *
   * Chairs can only cancel Elections that reference a dataset the chair is a DAC member for.
   *
   * @param collection The DarCollection
   * @return The DarCollection whose elections have been canceled
   */
  public DarCollection cancelDarCollectionElectionsAsChair(DarCollection collection, User user) {
    // Find dataset ids the chairperson has access to:
    List<Integer> datasetIds = datasetDAO.findDatasetsByAuthUserEmail(user.getEmail())
      .stream()
      .map(Dataset::getDataSetId)
      .collect(Collectors.toList());

    // Filter the list of DARs we can operate on by the datasets accessible to this chairperson
    List<DataAccessRequest> dars = collection.getDars().values().stream()
      .filter(d -> datasetIds.containsAll(d.getData().getDatasetIds()))
      .collect(Collectors.toList());

    List<String> referenceIds = dars.stream()
      .map(DataAccessRequest::getReferenceId)
      .collect(Collectors.toList());

    if (referenceIds.isEmpty()) {
      logger.warn("DAR Collection does not have any associated DARs that this chairperson can access");
      return collection;
    }

    // Cancel filtered DAR elections
    cancelElectionsForReferenceIds(referenceIds);

    return darCollectionDAO.findDARCollectionByCollectionId(collection.getDarCollectionId());
  }

  /**
   * DarCollections with no elections, or with previously canceled elections, are valid
   * for initiating a new set of elections. Elections in open, closed, pending, or final
   * states are not valid.
   *
   * @param user The User initiating new elections for a collection
   * @param collection The DarCollection
   * @return The updated DarCollection
   */
  public DarCollection createElectionsForDarCollection(User user, DarCollection collection) {
    try {
      List<String> createdElectionReferenceIds = collectionServiceDAO.createElectionsForDarCollection(user, collection);
      List<User> voteUsers = voteDAO.findVoteUsersByElectionReferenceIdList(createdElectionReferenceIds);
      try {
        emailNotifierService.sendDarNewCollectionElectionMessage(voteUsers, collection);
      } catch (Exception e) {
        logger.error("Unable to send new case message to DAC members for DAR Collection: " + collection.getDarCode());
      }
    } catch (Exception e) {
      logger.error("Exception creating elections and votes for collection: " + collection.getDarCollectionId());
    }
    return darCollectionDAO.findDARCollectionByCollectionId(collection.getDarCollectionId());
  }

  // Private helper method to mark Elections as 'Canceled'
  private void cancelElectionsForReferenceIds(List<String> referenceIds) {
    List<Election> elections = electionDAO.findOpenElectionsByReferenceIds(referenceIds);
    elections.forEach(election -> {
      if (!election.getStatus().equals(ElectionStatus.CANCELED.getValue())) {
        electionDAO.updateElectionById(election.getElectionId(), ElectionStatus.CANCELED.getValue(), new Date());
      }
    });
  }
}
