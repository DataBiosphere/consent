package org.broadinstitute.consent.http.service;

import com.google.inject.Inject;
import org.broadinstitute.consent.http.db.DarCollectionDAO;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
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
import org.broadinstitute.consent.http.models.Vote;
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
import java.util.stream.Stream;

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

  // public PaginationResponse<DarCollection> queryCollectionsByFiltersAndUserRoles(User user, PaginationToken token, String roleName) {
    
  // }

  // private int getCountForUnfilteredQueryByRole(User user, String userRole) {
  //   if(userRole.equalsIgnoreCase(UserRoles.ADMIN.getRoleName()) {
  //     return darCollectionDAO.findAllDARCollections().size(); //Make new query to count specific query
  //   } else if (userRole.equalsIgnoreCase(UserRoles.SIGNINGOFFICIAL.getRoleName())) {
  //     return darCollectionDAO.findDARCollectionIdsByInstitutionId(user.getInstitutionId()).size(); //make new query to only get count
  //   } else if (
  //       userRole.equalsIgnoreCase(UserRoles.CHAIRPERSON.getRoleName()) ||
  //       userRole.equalsIgnoreCase(UserRoles.MEMBER.getRoleName())
  //   ) {
  //     return darCollectionDAO.findDarCollectionByDacId(/*logic for dac id needed*/);
  //   }
  // }

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
      .map(d-> d.getDars().values())
      .flatMap(Collection::stream)
      .map(d -> d.getData().getDatasetIds())
      .flatMap(Collection::stream)
      .collect(Collectors.toList());

    if(!datasetIds.isEmpty()) {
      Set<DataSet> datasets = datasetDAO.findDatasetWithDataUseByIdList(datasetIds);
      Map<Integer, DataSet> datasetMap = datasets.stream()
          .collect(Collectors.toMap(DataSet::getDataSetId, Function.identity()));

      return collections.stream().map(c -> {
        Set<DataSet> collectionDatasets = c.getDars().values().stream()
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

    return darCollectionDAO.findDARCollectionByCollectionId(collection.getDarCollectionId());
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
    List<Integer> datasetIds = datasetDAO.findDataSetsByAuthUserEmail(user.getEmail())
      .stream()
      .map(DataSet::getDataSetId)
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
    final List<String> invalidStatuses = Stream.of(
            ElectionStatus.CLOSED, ElectionStatus.OPEN, ElectionStatus.FINAL, ElectionStatus.PENDING_APPROVAL
    ).map(ElectionStatus::getValue).collect(Collectors.toList());
    List<String> referenceIds = collection.getDars().values().stream().map(DataAccessRequest::getReferenceId).collect(Collectors.toList());
    if (!referenceIds.isEmpty()) {
      List<Election> nonCanceledElections = electionDAO.findLastElectionsByReferenceIds(referenceIds)
        .stream()
        .filter(e -> invalidStatuses.contains(e.getStatus()))
        .collect(Collectors.toList());
      if (!nonCanceledElections.isEmpty()) {
        logger.error("Non-canceled elections exist for collection: " + collection.getDarCollectionId());
        throw new IllegalArgumentException("Non-canceled elections exist for this collection.");
      }
    }
    try {
      collectionServiceDAO.createElectionsForDarCollection(collection);
      collection.getDars().values().forEach(dar -> {
        Election accessElection = electionDAO.findLastElectionByReferenceIdAndType(dar.getReferenceId(), ElectionType.DATA_ACCESS.getValue());
        if (Objects.nonNull(accessElection)) {
          List<Vote> votes = voteDAO.findVotesByElectionId(accessElection.getElectionId());
          try {
            emailNotifierService.sendNewCaseMessageToList(votes, accessElection);
          } catch (Exception e) {
            logger.error("Unable to send new case message to DAC members for DAR: " + dar.getReferenceId());
          }
        } else {
          logger.error("Did not find a created access election for DAR: " + dar.getReferenceId());
        }
      });
    } catch (Exception e) {
      logger.error("Exception creating elections and votes for collection: " + collection.getDarCollectionId());
    }
    return darCollectionDAO.findDARCollectionByCollectionId(collection.getDarCollectionId());
  }

  // Private helper method to mark Elections as 'Canceled'
  private void cancelElectionsForReferenceIds(List<String> referenceIds) {
    List<Election> elections = electionDAO.findLastElectionsByReferenceIds(referenceIds);
    elections.forEach(election -> {
      if (!election.getStatus().equals(ElectionStatus.CANCELED.getValue())) {
        electionDAO.updateElectionById(election.getElectionId(), ElectionStatus.CANCELED.getValue(), new Date());
      }
    });
  }
}
