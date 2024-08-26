package org.broadinstitute.consent.http.service;

import static java.util.stream.Collectors.toList;

import com.google.gson.Gson;
import com.google.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotAcceptableException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.broadinstitute.consent.http.db.DarCollectionDAO;
import org.broadinstitute.consent.http.db.DarCollectionSummaryDAO;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.MatchDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.enumeration.DarCollectionActions;
import org.broadinstitute.consent.http.enumeration.DarCollectionStatus;
import org.broadinstitute.consent.http.enumeration.DarStatus;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.DarCollection;
import org.broadinstitute.consent.http.models.DarCollectionSummary;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.Election;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.UserRole;
import org.broadinstitute.consent.http.models.Vote;
import org.broadinstitute.consent.http.service.dao.DarCollectionServiceDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DarCollectionService {

  private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  private final DarCollectionDAO darCollectionDAO;
  private final DarCollectionServiceDAO collectionServiceDAO;
  private final DarCollectionSummaryDAO darCollectionSummaryDAO;
  private final DataAccessRequestDAO dataAccessRequestDAO;
  private final DatasetDAO datasetDAO;
  private final ElectionDAO electionDAO;
  private final VoteDAO voteDAO;
  private final MatchDAO matchDAO;
  private final EmailService emailService;

  @Inject
  public DarCollectionService(DarCollectionDAO darCollectionDAO,
      DarCollectionServiceDAO collectionServiceDAO, DatasetDAO datasetDAO, ElectionDAO electionDAO,
      DataAccessRequestDAO dataAccessRequestDAO, EmailService emailService, VoteDAO voteDAO,
      MatchDAO matchDAO, DarCollectionSummaryDAO darCollectionSummaryDAO) {
    this.darCollectionDAO = darCollectionDAO;
    this.collectionServiceDAO = collectionServiceDAO;
    this.datasetDAO = datasetDAO;
    this.electionDAO = electionDAO;
    this.dataAccessRequestDAO = dataAccessRequestDAO;
    this.emailService = emailService;
    this.voteDAO = voteDAO;
    this.matchDAO = matchDAO;
    this.darCollectionSummaryDAO = darCollectionSummaryDAO;
  }

  private void updateStatusCount(Map<String, Integer> statusCount, String status) {
    if (Objects.isNull(status)) {
      //If the status is null, track it as Undefined to ensure election is accounted for
      status = "Undefined";
    }
    Integer count = statusCount.get(status);
    if (Objects.isNull(count)) {
      statusCount.put(status, 0);
      count = 0;
    }
    statusCount.put(status, count + 1);
  }

  private void determineCollectionStatus(DarCollectionSummary summary,
      Map<String, Integer> statusCount, Integer datasetCount, Integer electionCount) {
    //If there are no elections, status is unreviewed
    //if there are some elections open, status is in process
    //if all elections are closed or canceled and electionCount == datasetCount, status is complete
    if (electionCount.equals(0)) {
      summary.setStatus(DarCollectionStatus.UNREVIEWED.getValue());
    } else if (electionCount.equals(datasetCount)) {
      Integer openCount = statusCount.get(ElectionStatus.OPEN.getValue());
      if (Objects.isNull(openCount)) {
        summary.setStatus(DarCollectionStatus.COMPLETE.getValue());
      } else {
        summary.setStatus(DarCollectionStatus.IN_PROCESS.getValue());
      }
    } else {
      summary.setStatus(DarCollectionStatus.IN_PROCESS.getValue());
    }
  }

  private void processDarCollectionSummariesForAdmin(List<DarCollectionSummary> summaries) {
    //if at least one election is open, show cancel
    //if at least one non-open/absent election, show open
    summaries.forEach(s -> {
      Map<String, Integer> statusCount = new HashMap<>();
      Map<Integer, Election> elections = s.getElections();
      if (elections.size() == 0) {
        s.addAction(DarCollectionActions.OPEN.getValue());
        s.setStatus(DarCollectionStatus.UNREVIEWED.getValue());
      } else {
        elections.values().forEach(e -> {
          String status = e.getStatus();
          updateStatusCount(statusCount, status);
          if (status.equals(ElectionStatus.OPEN.getValue())) {
            s.addAction(DarCollectionActions.CANCEL.getValue());
          } else {
            s.addAction(DarCollectionActions.OPEN.getValue());
          }
        });
        determineCollectionStatus(s, statusCount, s.getDatasetCount(), s.getElections().size());
      }
    });
  }

  private DarCollectionSummary processDraftAsSummary(DataAccessRequest d) {
    try {
      DarCollectionSummary summary = new DarCollectionSummary();
      String darCode = "DRAFT_DAR_" + sdf.format(d.getCreateDate());
      summary.setDarCode(darCode);
      summary.setStatus(DarCollectionStatus.DRAFT.getValue());
      summary.setName(d.getData().getProjectTitle());
      summary.addAction(DarCollectionActions.RESUME.getValue());
      summary.addAction(DarCollectionActions.DELETE.getValue());
      summary.addReferenceId(d.referenceId);
      return summary;
    } catch (Exception e) {
      logger.warn("Error processing draft with id: " + d.getId());
    }
    return null;
  }

  private void processDarCollectionSummariesForResearcher(List<DarCollectionSummary> summaries) {
    //if an election exists, cancel does not appear
    //if there are no elections, review and cancel are present
    //if the collection is canceled, revise and review is present
    summaries.forEach(s -> {
      Map<String, Integer> statusCount = new HashMap<>();
      Map<Integer, Election> elections = s.getElections();
      int electionCount = elections.values().size();
      elections.values().forEach(election -> updateStatusCount(statusCount, election.getStatus()));
      s.addAction(DarCollectionActions.REVIEW.getValue());
      //check dar statuses, if they're all canceled show revise (but only if there are no elections)
      if (electionCount == 0) {
        Collection<String> darStatuses = s.getDarStatuses().values();
        Boolean isCanceled = darStatuses.size() > 0 && darStatuses.stream()
            .allMatch(st -> st.equalsIgnoreCase(DarStatus.CANCELED.getValue()));
        if (isCanceled) {
          s.addAction(DarCollectionActions.REVISE.getValue());
          s.setStatus(DarCollectionStatus.CANCELED.getValue());
        } else {
          s.addAction(DarCollectionActions.CANCEL.getValue());
          s.setStatus(DarCollectionStatus.UNREVIEWED.getValue());
        }
      } else {
        determineCollectionStatus(s, statusCount, s.getDatasetCount(), s.getElections().size());
      }
    });
  }

  private void processDarCollectionSummariesForMember(List<DarCollectionSummary> summaries,
      Integer userId) {
    summaries.forEach(s -> {
      Collection<Election> elections = s.getElections().values();
      Integer electionCount = elections.size();
      //if there are no elections present, unreviewed
      //if there are elections present. in process
      if (electionCount == 0) {
        s.setStatus(DarCollectionStatus.UNREVIEWED.getValue());
      } else {
        Boolean isVotable = elections
            .stream()
            .anyMatch(
                election -> election.getStatus().equalsIgnoreCase(ElectionStatus.OPEN.getValue()));

        if (isVotable) {
          s.setStatus(DarCollectionStatus.IN_PROCESS.getValue());
          List<Vote> votes = s.getVotes().stream()
              .filter(
                  v -> v.getUserId().equals(userId) && v.getType().equals(VoteType.DAC.getValue()))
              .collect(Collectors.toList());
          if (!votes.isEmpty()) {
            Boolean hasVoted = votes.stream().allMatch(v -> Objects.nonNull(v.getVote()));
            String targetActionString = hasVoted ? DarCollectionActions.UPDATE.getValue()
                : DarCollectionActions.VOTE.getValue();
            s.addAction(targetActionString);
          }
        } else {
          //non-votable states
          //all canceled (complete)
          //some datasets do not have elections (in process)
          //all voted on (complete)
          //no elections
          if (electionCount < s.getDatasetCount()) {
            s.setStatus(DarCollectionStatus.IN_PROCESS.getValue());
          } else {
            s.setStatus(DarCollectionStatus.COMPLETE.getValue());
          }
        }
      }
    });
  }


  private void processDarCollectionSummariesForChair(List<DarCollectionSummary> summaries) {
    summaries.forEach(s -> {
      //if there are no elections, only show open
      //if there is any closed or canceled elections, or if some datasets dont have an election, show open
      //if there are any open elections, show cancel and vote
      Map<String, Integer> statusCount = new HashMap<>();
      Map<Integer, Election> elections = s.getElections();
      if (elections.size() == 0) {
        s.setStatus(DarCollectionStatus.UNREVIEWED.getValue());
        s.addAction(DarCollectionActions.OPEN.getValue());
      } else {
        if (elections.size() < s.getDatasetCount()) {
          s.addAction(DarCollectionActions.OPEN.getValue());
        }
        elections.values().forEach(election -> {
          String statusString = election.getStatus();
          updateStatusCount(statusCount, statusString);
          ElectionStatus status = ElectionStatus.getStatusFromString(statusString);
          switch (status) {
            case CLOSED:
            case CANCELED:
              s.addAction(DarCollectionActions.OPEN.getValue());
              break;
            case OPEN:
              s.addAction(DarCollectionActions.VOTE.getValue());
            default:
              break;
          }
        });
        Integer closedCount = statusCount.get(ElectionStatus.CLOSED.getValue());
        Integer openCount = statusCount.get(ElectionStatus.OPEN.getValue());
        //add cancel if there are no closed elections and at least one open election
        if (Objects.isNull(closedCount) && Objects.nonNull(openCount)) {
          s.addAction(DarCollectionActions.CANCEL.getValue());
        }

        determineCollectionStatus(s, statusCount, s.getDatasetCount(), s.getElections().size());
      }
    });
  }

  private void processDarCollectionSummariesForSO(List<DarCollectionSummary> summaries) {
    summaries.forEach(s -> {
      Map<String, Integer> statusCount = new HashMap<>();
      s.getElections().values()
          .forEach(election -> updateStatusCount(statusCount, election.getStatus()));
      determineCollectionStatus(s, statusCount, s.getDatasetCount(), s.getElections().size());
    });
  }

  /**
   * Find all DarCollectionSummaries for a given role name. Admins can see all summaries Chairs and
   * Members can see summaries for datasets they have access to Signing Officials can see summaries
   * for researchers in their institution Researchers can see only their own summaries
   *
   * @param user     The user making the request
   * @param userRole The role the user is making the request as
   * @return List of DarCollectionSummary objects
   */
  public List<DarCollectionSummary> getSummariesForRoleName(User user, String userRole) {
    List<DarCollectionSummary> summaries = new ArrayList<>();
    UserRoles role = UserRoles.getUserRoleFromName(userRole);
    Integer userId = user.getUserId();
    List<Integer> datasetIds;
    switch (role) {
      case ADMIN:
        summaries = darCollectionSummaryDAO.getDarCollectionSummariesForAdmin();
        processDarCollectionSummariesForAdmin(summaries);
        break;
      case SIGNINGOFFICIAL:
        summaries = darCollectionSummaryDAO.getDarCollectionSummariesForSO(user.getInstitutionId());
        processDarCollectionSummariesForSO(summaries);
        break;
      case CHAIRPERSON:
        datasetIds = getDatasetIdsForUserAndRoleId(user, UserRoles.CHAIRPERSON.getRoleId());
        summaries = darCollectionSummaryDAO.getDarCollectionSummariesForDAC(userId, datasetIds);
        processDarCollectionSummariesForChair(summaries);
        break;
      case MEMBER:
        datasetIds = getDatasetIdsForUserAndRoleId(user, UserRoles.MEMBER.getRoleId());
        summaries = darCollectionSummaryDAO.getDarCollectionSummariesForDAC(userId, datasetIds);
        processDarCollectionSummariesForMember(summaries, userId);
        break;
      case RESEARCHER:
        summaries = darCollectionSummaryDAO.getDarCollectionSummariesForResearcher(userId);
        processDarCollectionSummariesForResearcher(summaries);
        List<DataAccessRequest> drafts = dataAccessRequestDAO.findAllDraftsByUserId(userId);
        summaries.addAll(drafts
            .stream()
            .map(this::processDraftAsSummary)
            .filter(Objects::nonNull)
            .toList()
        );
        break;
      default:
        break;
    }
    return summaries;
  }

  private List<Integer> getDatasetIdsForUserAndRoleId(User user, Integer roleId) {
    List<Integer> roleDacIds = user.getRoles().stream()
        .filter(ur -> Objects.nonNull(ur.getRoleId()))
        .filter(ur -> ur.getRoleId().equals(roleId))
        .map(UserRole::getDacId)
        .filter(Objects::nonNull)
        .toList();
    return Stream.of(roleDacIds)
        .filter(Predicate.not(List::isEmpty))
        .map(datasetDAO::findDatasetListByDacIds)
        .flatMap(List::stream)
        .map(Dataset::getDataSetId)
        .toList();
  }

  /**
   * Finds the DarCollectionSummary for a given darCollectionId, processed by the given role name.
   *
   * @param user         The user making the request
   * @param userRole     The role the user is making the request as
   * @param collectionId The darCollectionId of the requested DarCollectionSummary
   * @return A DarCollectionSummary object
   */
  public DarCollectionSummary getSummaryForRoleNameByCollectionId(User user, String userRole,
      Integer collectionId) {
    DarCollectionSummary summary = null;
    UserRoles role = UserRoles.getUserRoleFromName(userRole);
    Integer userId = user.getUserId();
    List<Integer> datasetIds;
    try {
      switch (role) {
        case ADMIN:
          summary = darCollectionSummaryDAO.getDarCollectionSummaryByCollectionId(collectionId);
          processDarCollectionSummariesForAdmin(List.of(summary));
          break;
        case SIGNINGOFFICIAL:
          summary = darCollectionSummaryDAO.getDarCollectionSummaryByCollectionId(collectionId);
          processDarCollectionSummariesForSO(List.of(summary));
          break;
        case CHAIRPERSON:
          datasetIds = getDatasetIdsForUserAndRoleId(user, UserRoles.CHAIRPERSON.getRoleId());
          summary = darCollectionSummaryDAO.getDarCollectionSummaryForDACByCollectionId(userId,
              datasetIds, collectionId);
          processDarCollectionSummariesForChair(List.of(summary));
          break;
        case MEMBER:
          datasetIds = getDatasetIdsForUserAndRoleId(user, UserRoles.MEMBER.getRoleId());
          summary = darCollectionSummaryDAO.getDarCollectionSummaryForDACByCollectionId(userId,
              datasetIds, collectionId);
          processDarCollectionSummariesForMember(List.of(summary), userId);
          break;
        case RESEARCHER:
          summary = darCollectionSummaryDAO.getDarCollectionSummaryByCollectionId(collectionId);
          processDarCollectionSummariesForResearcher(List.of(summary));
          break;
        default:
          break;
      }
      return summary;
    } catch (NullPointerException e) {
      throw new NotFoundException(
          "Collection summary with the collection id of " + collectionId + " was not found");
    }
  }

  public DarCollectionSummary updateCollectionToDraftStatus(DarCollection sourceCollection) {
    this.dataAccessRequestDAO.updateDraftByCollectionId(sourceCollection.getDarCollectionId(),
        true);
    sourceCollection.getDars().values().forEach((d) -> {
      Date now = new Date();
      DataAccessRequestData newData = new Gson().fromJson(d.getData().toString(),
          DataAccessRequestData.class);
      newData.setDarCode(null);
      newData.setStatus(null);
      newData.setReferenceId(d.getReferenceId());
      newData.setSortDate(now.getTime());
      dataAccessRequestDAO.updateDataByReferenceId(
          d.getReferenceId(),
          d.getUserId(),
          now,
          null,
          now,
          newData
      );
    });

    // get updated collection
    sourceCollection = this.darCollectionDAO.findDARCollectionByCollectionId(
        sourceCollection.getDarCollectionId());

    return this.processDraftAsSummary(new ArrayList<>(sourceCollection.getDars().values()).get(0));
  }

  public List<Integer> findDatasetIdsByUser(User user) {
    return datasetDAO.findDatasetsByAuthUserEmail(user.getEmail())
        .stream()
        .map(Dataset::getDataSetId)
        .collect(Collectors.toList());
  }

  public void deleteByCollectionId(User user, Integer collectionId)
      throws NotAcceptableException, NotAuthorizedException, NotFoundException {
    DarCollection coll = darCollectionDAO.findDARCollectionByCollectionId(collectionId);
    if (coll == null) {
      throw new NotFoundException("DAR Collection does not exist at that id.");
    }

    // ensure the user is capable of deleting the collection
    if (!user.hasUserRole(UserRoles.ADMIN) && !coll.getCreateUserId().equals(user.getUserId())) {
      throw new NotAuthorizedException("Not authorized to delete DAR Collection.");
    }

    // get the reference ids of the dars in the collection
    List<String> referenceIds =
        coll.getDars().values().stream().map(DataAccessRequest::getReferenceId).distinct()
            .collect(toList());

    // ensure there are no elections; if there are, will attempt to delete (must be admin)
    ensureNoElections(user, referenceIds);

    // no elections left & user has perms => safe to delete collection

    // delete DARs
    matchDAO.deleteRationalesByPurposeIds(referenceIds);
    matchDAO.deleteMatchesByPurposeIds(referenceIds);
    dataAccessRequestDAO.deleteDARDatasetRelationByReferenceIds(referenceIds);
    dataAccessRequestDAO.deleteByReferenceIds(referenceIds);

    // delete collection
    darCollectionDAO.deleteByCollectionId(collectionId);
  }

  // checks if there are any elections for any of the DARs in the referenceIds; if so,
  // will attempt to delete them (must be admin to delete)
  private void ensureNoElections(User user, List<String> referenceIds)
      throws NotAcceptableException {
    // get elections across all reference ids
    List<Election> allElections = electionDAO.findElectionsByReferenceIds(referenceIds);

    // if there are already no elections, we're done!
    if (allElections.isEmpty()) {
      return;
    }

    // if there are any elections, we need to delete them.
    // only admins can delete elections; make sure user is an admin
    if (!user.hasUserRole(UserRoles.ADMIN)) {
      throw new NotAcceptableException("Cannot delete DAR with elections.");
    }

    // delete all votes
    voteDAO.deleteVotesByReferenceIds(referenceIds);

    // delete all elections
    List<Integer> electionIds = allElections.stream().map(Election::getElectionId)
        .collect(toList());

    electionDAO.deleteElectionsByIds(electionIds);

  }

  public DarCollection getByReferenceId(String referenceId) {
    DarCollection collection = darCollectionDAO.findDARCollectionByReferenceId(referenceId);
    if (Objects.isNull(collection)) {
      throw new NotFoundException(
          "Collection with the reference id of " + referenceId + " was not found");
    }
    List<DarCollection> populatedCollections = addDatasetsToCollections(
        Collections.singletonList(collection), List.of());
    return populatedCollections.stream().findFirst().orElse(null);
  }

  public DarCollection getByCollectionId(Integer collectionId) {
    DarCollection collection = darCollectionDAO.findDARCollectionByCollectionId(collectionId);
    if (Objects.isNull(collection)) {
      throw new NotFoundException(
          "Collection with the collection id of " + collectionId + " was not found");
    }
    List<DarCollection> populatedCollections = addDatasetsToCollections(
        Collections.singletonList(collection), List.of());
    return populatedCollections.stream().findFirst().orElse(null);
  }

  /**
   * Iterate through a set of collections and add relevant datasets.
   *
   * @param collections      The list of DarCollections to iterate over.
   * @param filterDatasetIds An optional list of Dataset Ids used to filter down the datasets for
   *                         the collections
   * @return List<DarCollection>
   */
  public List<DarCollection> addDatasetsToCollections(List<DarCollection> collections,
      List<Integer> filterDatasetIds) {
    // get datasetIds from each DAR from each collection
    List<String> referenceIds = collections.stream()
        .map(DarCollection::getDars)
        .map(Map::keySet)
        .flatMap(Set::stream)
        .collect(Collectors.toList());
    List<Integer> datasetIds = referenceIds.isEmpty() ? List.of()
        : dataAccessRequestDAO.findAllDARDatasetRelations(referenceIds);
    if (!datasetIds.isEmpty()) {
      // if filterDatasetIds has values, get the intersection between that and datasetIds
      if (!filterDatasetIds.isEmpty()) {
        datasetIds.retainAll(filterDatasetIds);
      }
      Set<Dataset> datasets = datasetDAO.findDatasetWithDataUseByIdList(datasetIds);
      Map<Integer, Dataset> datasetMap = datasets.stream()
          .collect(Collectors.toMap(Dataset::getDataSetId, Function.identity()));

      return collections.stream().map(c -> {
        Set<Dataset> collectionDatasets = c.getDars().values().stream()
            .map(DataAccessRequest::getDatasetIds)
            .flatMap(Collection::stream)
            .map(datasetMap::get)
            .filter(Objects::nonNull) // filtering out nulls which were getting captured by map
            .collect(Collectors.toSet());
        DarCollection copy = c.deepCopy();
        copy.setDatasets(collectionDatasets);
        return copy;
      }).collect(Collectors.toList());
    }
    // There were no datasets to add, so we return the original list
    return collections;
  }

  /**
   * Cancel a DarCollection as a researcher.
   * <p>
   * If an election exists for a DAR within the collection, that DAR cannot be cancelled by the
   * researcher. Since it's now under DAC review, it's up to the DAC Chair (or admin) to ultimately
   * decline or cancel the elections for the collection.
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
    if (!elections.isEmpty()) {
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
   * <p>
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
   * <p>
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
        .filter(d -> datasetIds.containsAll(d.getDatasetIds()))
        .collect(Collectors.toList());

    List<String> referenceIds = dars.stream()
        .map(DataAccessRequest::getReferenceId)
        .collect(Collectors.toList());

    if (referenceIds.isEmpty()) {
      logger.warn(
          "DAR Collection does not have any associated DARs that this chairperson can access");
      return collection;
    }

    // Cancel filtered DAR elections
    cancelElectionsForReferenceIds(referenceIds);

    return darCollectionDAO.findDARCollectionByCollectionId(collection.getDarCollectionId());
  }

  /**
   * DarCollections with no elections, or with previously canceled elections, are valid for
   * initiating a new set of elections. Elections in open, closed, pending, or final states are not
   * valid.
   *
   * @param user       The User initiating new elections for a collection
   * @param collection The DarCollection
   * @return The updated DarCollection
   */
  public DarCollection createElectionsForDarCollection(User user, DarCollection collection) {
    try {
      List<String> createdElectionReferenceIds = collectionServiceDAO.createElectionsForDarCollection(
          user, collection);
      List<User> voteUsers = voteDAO.findVoteUsersByElectionReferenceIdList(
          createdElectionReferenceIds);
      try {
        emailService.sendDarNewCollectionElectionMessage(voteUsers, collection);
      } catch (Exception e) {
        logger.error("Unable to send new case message to DAC members for DAR Collection: "
            + collection.getDarCode());
      }
    } catch (Exception e) {
      logger.error("Exception creating elections and votes for collection: "
          + collection.getDarCollectionId());
    }
    return darCollectionDAO.findDARCollectionByCollectionId(collection.getDarCollectionId());
  }

  // Private helper method to mark Elections as 'Canceled'
  private void cancelElectionsForReferenceIds(List<String> referenceIds) {
    List<Election> elections = electionDAO.findOpenElectionsByReferenceIds(referenceIds);
    elections.forEach(election -> {
      if (!election.getStatus().equals(ElectionStatus.CANCELED.getValue())) {
        electionDAO.updateElectionById(election.getElectionId(), ElectionStatus.CANCELED.getValue(),
            new Date());
      }
    });
  }
}
