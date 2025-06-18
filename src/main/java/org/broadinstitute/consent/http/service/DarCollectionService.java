package org.broadinstitute.consent.http.service;

import static java.util.stream.Collectors.toList;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Streams;
import com.google.gson.Gson;
import com.google.inject.Inject;
import freemarker.template.TemplateException;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotAcceptableException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;
import java.io.IOException;
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
import org.broadinstitute.consent.http.db.DacDAO;
import org.broadinstitute.consent.http.db.DarCollectionDAO;
import org.broadinstitute.consent.http.db.DarCollectionSummaryDAO;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.MatchDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.enumeration.DarCollectionActions;
import org.broadinstitute.consent.http.enumeration.DarCollectionStatus;
import org.broadinstitute.consent.http.enumeration.DarStatus;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.Dac;
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
import org.broadinstitute.consent.http.util.ConsentLogger;

public class DarCollectionService implements ConsentLogger {

  private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
  private final DarCollectionDAO darCollectionDAO;
  private final DarCollectionServiceDAO collectionServiceDAO;
  private final DacDAO dacDAO;
  private final DarCollectionSummaryDAO darCollectionSummaryDAO;
  private final DataAccessRequestDAO dataAccessRequestDAO;
  private final DatasetDAO datasetDAO;
  private final ElectionDAO electionDAO;
  private final EmailService emailService;
  private final MatchDAO matchDAO;
  private final UserDAO userDAO;
  private final VoteDAO voteDAO;

  @Inject
  public DarCollectionService(DarCollectionDAO darCollectionDAO,
      DarCollectionServiceDAO collectionServiceDAO, DatasetDAO datasetDAO, ElectionDAO electionDAO,
      DataAccessRequestDAO dataAccessRequestDAO, EmailService emailService, VoteDAO voteDAO,
      MatchDAO matchDAO, DarCollectionSummaryDAO darCollectionSummaryDAO, UserDAO userDAO,
      DacDAO dacDAO) {
    this.darCollectionDAO = darCollectionDAO;
    this.collectionServiceDAO = collectionServiceDAO;
    this.datasetDAO = datasetDAO;
    this.electionDAO = electionDAO;
    this.dataAccessRequestDAO = dataAccessRequestDAO;
    this.emailService = emailService;
    this.voteDAO = voteDAO;
    this.matchDAO = matchDAO;
    this.darCollectionSummaryDAO = darCollectionSummaryDAO;
    this.userDAO = userDAO;
    this.dacDAO = dacDAO;
  }

  private void updateStatusCount(Map<String, Integer> statusCount, String status) {
    // If the status is null, track it as Undefined to ensure election is accounted for.
    statusCount.merge(Objects.requireNonNullElse(status, "Undefined"), 1, Integer::sum);
  }

  private void determineCollectionStatus(DarCollectionSummary summary,
      Map<String, Integer> statusCount, Integer datasetCount, Integer electionCount) {
    //If there are no elections, status is unreviewed
    //if there are some elections open, status is in process
    //if all elections are closed or canceled and electionCount == datasetCount, status is complete
    if (electionCount.equals(0)) {
      summary.setStatus(DarCollectionStatus.SUBMITTED.getValue());
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
      if (elections.isEmpty()) {
        s.addAction(DarCollectionActions.OPEN);
        s.setStatus(DarCollectionStatus.SUBMITTED.getValue());
      } else {
        elections.values().forEach(e -> {
          String status = e.getStatus();
          updateStatusCount(statusCount, status);
          if (status.equals(ElectionStatus.OPEN.getValue())) {
            s.addAction(DarCollectionActions.CANCEL);
          } else {
            s.addAction(DarCollectionActions.OPEN);
          }
        });
        determineCollectionStatus(s, statusCount, s.getDatasetCount(), s.getElections().size());
      }
      if (s.getCloseoutSupplement() != null) {
        s.getActions().clear();
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
      summary.addAction(DarCollectionActions.RESUME);
      summary.addAction(DarCollectionActions.DELETE);
      summary.addReferenceId(d.referenceId);
      return summary;
    } catch (Exception e) {
      logWarn("Error processing draft with id: %s".formatted(d.getId()), e);
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
      int electionCount = elections.size();
      elections.values().forEach(election -> updateStatusCount(statusCount, election.getStatus()));
      s.addAction(DarCollectionActions.REVIEW);
      //if the latest DAR in the collection has at least one approved dataset,
      //include the create progress report action
      Set<Integer> datasetIds = dataAccessRequestDAO.findDatasetApprovalsByDar(s.getLatestReferenceId());
      // Can only create a progress report if there are approved datasets and no closeout supplement
      if (!datasetIds.isEmpty() && s.getCloseoutSupplement() == null) {
          s.addAction(DarCollectionActions.CREATE_PROGRESS_REPORT);
        }

      //check dar statuses, if they're all canceled show revise (but only if there are no elections)
      if (electionCount == 0) {
        Collection<String> darStatuses = s.getDarStatuses().values();
        boolean isCanceled = !darStatuses.isEmpty() && darStatuses.stream()
            .allMatch(st -> st.equalsIgnoreCase(DarStatus.CANCELED.getValue()));
        if (isCanceled) {
          s.addAction(DarCollectionActions.REVISE);
          s.setStatus(DarCollectionStatus.CANCELED.getValue());
        } else {
          if (!s.getProgressReport()) {
            s.addAction(DarCollectionActions.CANCEL);
          }
          s.setStatus(DarCollectionStatus.SUBMITTED.getValue());
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
        s.setStatus(DarCollectionStatus.SUBMITTED.getValue());
      } else {
        boolean isVotable = elections
            .stream()
            .anyMatch(
                election -> election.getStatus().equalsIgnoreCase(ElectionStatus.OPEN.getValue()));

        if (isVotable) {
          s.setStatus(DarCollectionStatus.IN_PROCESS.getValue());
          List<Vote> votes = s.getVotes().stream()
              .filter(
                  v -> v.getUserId().equals(userId) && v.getType().equals(VoteType.DAC.getValue()))
              .toList();
          if (!votes.isEmpty()) {
            boolean hasVoted = votes.stream().map(Vote::getVote).allMatch(Objects::nonNull);
            DarCollectionActions targetAction = hasVoted ? DarCollectionActions.UPDATE
                : DarCollectionActions.VOTE;
            s.addAction(targetAction);
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

  /**
   * Process the DarCollectionSummaries for a chairperson. Note that this decorates the raw
   * summaries with status and actions based on the elections present in each summary.
   *
   * @param summaries The list of DarCollectionSummaries to process
   */
  private void processDarCollectionSummariesForChair(List<DarCollectionSummary> summaries) {
    summaries.forEach(s -> {
      Map<String, Integer> statusCount = new HashMap<>();
      Map<Integer, Election> elections = s.getElections();
      if (elections.size() < s.getDatasetCount()) {
        s.addAction(DarCollectionActions.OPEN);
      }
      elections.values().forEach(election -> updateStatusCount(statusCount, election.getStatus()));
      Integer closedCount = statusCount.get(ElectionStatus.CLOSED.getValue());
      Integer openCount = statusCount.get(ElectionStatus.OPEN.getValue());
      determineCollectionStatus(s, statusCount, s.getDatasetCount(), s.getElections().size());
      updateSummaryActionsForChair(s, closedCount, openCount);
    });
  }

  /**
   * Update the summary actions for a chairperson based on the summary and election counts.
   *
   * @param summary  The DarCollectionSummary to update
   * @param closedCount The count of closed elections
   * @param openCount The count of open elections
   */
  private void updateSummaryActionsForChair(
      DarCollectionSummary summary,
      Integer closedCount,
      Integer openCount) {

    // No actions can be taken on a closeout supplement
    if (summary.getCloseoutSupplement() != null) {
      summary.getActions().clear();
      return;
    }

    // If there are no elections, only show open
    if (summary.getElections().isEmpty()) {
      summary.addAction(DarCollectionActions.OPEN);
    }

    // If there are closed or canceled elections, show open
    // If there are any open elections, show vote
    summary.getElections().values().forEach(election -> {
      ElectionStatus status = ElectionStatus.getStatusFromString(election.getStatus());
      switch (Objects.requireNonNull(status)) {
        case CLOSED, CANCELED:
          summary.addAction(DarCollectionActions.OPEN);
          break;
        case OPEN:
          summary.addAction(DarCollectionActions.VOTE);
          break;
        default:
          break;
      }
    });

    // Add cancel if there are no closed elections and at least one open election
    if (Objects.isNull(closedCount) && Objects.nonNull(openCount)) {
      summary.addAction(DarCollectionActions.CANCEL);
    }
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
   * Find all DarCollectionSummaries for a given role. Admins can see all summaries Chairs and
   * Members can see summaries for datasets they have access to Signing Officials can see summaries
   * for researchers in their institution Researchers can see only their own summaries
   *
   * @param user     The user making the request
   * @param role The role the user is making the request as
   * @return List of DarCollectionSummary objects
   */
  public List<DarCollectionSummary> getSummariesForRole(User user, UserRoles role) {
    final List<DarCollectionSummary> summaries;
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
        var darSummaries = darCollectionSummaryDAO.getDarCollectionSummariesForResearcher(userId);
        processDarCollectionSummariesForResearcher(darSummaries);
        List<DataAccessRequest> drafts = dataAccessRequestDAO.findAllDraftsByUserId(userId);
        summaries =
            Stream.concat(
                    darSummaries.stream(),
                    drafts.stream().map(this::processDraftAsSummary).filter(Objects::nonNull))
                .toList();
        break;
      default:
        summaries = List.of();
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
        .map(Dataset::getDatasetId)
        .toList();
  }

  /**
   * Finds the DarCollectionSummary for a given darCollectionId, processed by the given role.
   *
   * @param user         The user making the request
   * @param role         The role the user is making the request as
   * @param collectionId The darCollectionId of the requested DarCollectionSummary
   * @return A DarCollectionSummary object
   */
  public DarCollectionSummary getSummaryForRoleByCollectionId(User user, UserRoles role,
      Integer collectionId) {
    DarCollectionSummary summary = null;
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
          newData,
          null
      );
    });

    // get updated collection
    sourceCollection = this.darCollectionDAO.findDARCollectionByCollectionId(
        sourceCollection.getDarCollectionId());

    return this.processDraftAsSummary(new ArrayList<>(sourceCollection.getDars().values()).get(0));
  }

  /**
   * Find all dataset ids by the DAC User. Will return ids for Chairpersons or Members
   *
   * @param user The DAC User
   * @return List of Dataset IDs
   */
  public List<Integer> findDatasetIdsByDACUser(User user) {
    return datasetDAO.findDatasetIdsByDACUserId(user.getUserId());
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
    return addDatasetsToCollection(collection);
  }

  public DarCollection getByCollectionId(Integer collectionId) {
    DarCollection collection = darCollectionDAO.findDARCollectionByCollectionId(collectionId);
    if (Objects.isNull(collection)) {
      throw new NotFoundException(
          "Collection with the collection id of " + collectionId + " was not found");
    }
    return addDatasetsToCollection(collection);
  }

  /**
   * Given a DarCollection, add its relevant datasets.
   *
   * @param collection      The list of DarCollections to iterate over.
   * @return collection with datasets added
   */
  @VisibleForTesting
  protected DarCollection addDatasetsToCollection(DarCollection collection) {
    // get datasetIds from each DAR from each collection
    List<String> referenceIds = List.copyOf(collection.getDars().keySet());
    List<Integer> datasetIds = referenceIds.isEmpty() ? List.of()
        : dataAccessRequestDAO.findAllDARDatasetRelations(referenceIds);
    if (!datasetIds.isEmpty()) {
      Map<Integer, Dataset> datasetMap = datasetDAO.findDatasetsByIdList(datasetIds)
          .stream()
          .distinct()
          .collect(Collectors.toMap(Dataset::getDatasetId, Function.identity()));

        Set<Dataset> collectionDatasets = collection.getDars().values().stream()
            .map(DataAccessRequest::getDatasetIds)
            .flatMap(Collection::stream)
            .map(datasetMap::get)
            .filter(Objects::nonNull) // filtering out nulls which were getting captured by map
            .collect(Collectors.toSet());
        DarCollection copy = collection.deepCopy();
        copy.setDatasets(collectionDatasets);
        return copy;
    }
    // There were no datasets to add, so we return the original list
    return collection;
  }

  /**
   * Cancel Elections or a dar for a DarCollection, given a user and a role. If the user is a chair,
   * or admin, cancel elections. If the user is a researcher, cancel the dar.
   *
   * @param user       The User initiating the cancel
   * @param collection The DarCollection
   * @param role       The role of the user, must be one of ADMIN, CHAIRPERSON, or RESEARCHER
   * @return The DarCollection that has been canceled
   */
  public DarCollection cancelDarCollectionByRole(User user, DarCollection collection, UserRoles role) {
    Collection<DataAccessRequest> dars = collection.getDars().values();
    if (dars.isEmpty()) {
      logWarn("DAR Collection ID: [%s] does not have any associated DAR ids".formatted(
          collection.getDarCollectionId()));
      return collection;
    }

    return switch (role) {
      case ADMIN -> cancelDarCollectionElectionsAsAdmin(collection);
      case CHAIRPERSON ->
          cancelDarCollectionElectionsAsChair(collection, user);
      default -> cancelDarCollectionAsResearcher(collection, user);
    };
  }

  /**
   * Cancel a DarCollection as a researcher.
   * <p>
   * If an election exists for a DAR within the collection, that DAR cannot be cancelled by the
   * researcher. Since it's now under DAC review, it's up to the DAC Chair (or admin) to ultimately
   * decline or cancel the elections for the collection.
   *
   * @param collection The DarCollection
   * @param user the researcher requesting the cancel
   * @return The canceled DarCollection
   */
  private DarCollection cancelDarCollectionAsResearcher(DarCollection collection, User user) {
    if (!user.getUserId().equals(collection.getCreateUserId())) {
      throw new NotFoundException();
    }
    DarCollectionSummary summary = darCollectionSummaryDAO
        .getDarCollectionSummaryByCollectionId(collection.getDarCollectionId());
    if (summary.getProgressReport()) {
      throw new BadRequestException("Cannot cancel a progress report");
    }

    Collection<DataAccessRequest> dars = collection.getDars().values();
    List<String> referenceIds = dars.stream().map(DataAccessRequest::getReferenceId).toList();

    List<Election> elections = electionDAO.findLastElectionsByReferenceIds(referenceIds);
    if (!elections.isEmpty()) {
      throw new BadRequestException("Elections present on DARs; cannot cancel collection");
    }

    // Cancel active dars for the researcher
    List<String> activeDarIds = dars.stream()
        .filter(d -> !DataAccessRequest.isCanceled(d))
        .map(DataAccessRequest::getReferenceId)
        .toList();
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
  private DarCollection cancelDarCollectionElectionsAsAdmin(DarCollection collection) {
    Collection<DataAccessRequest> dars = collection.getDars().values();
    List<String> referenceIds = dars.stream().map(DataAccessRequest::getReferenceId).toList();

    // Cancel all DAR elections
    cancelElectionsForReferenceIds(referenceIds);

    return getByCollectionId(collection.getDarCollectionId());
  }

  /**
   * Cancel Elections for a DarCollection as a chairperson.
   * <p>
   * Chairs can only cancel Elections that reference a dataset the chair is a DAC member for.
   *
   * @param collection The DarCollection
   * @return The DarCollection whose elections have been canceled
   */
  private DarCollection cancelDarCollectionElectionsAsChair(DarCollection collection, User user) {
    // Find dataset ids the chairperson has access to:
    Set<Integer> datasetIds = Set.copyOf(datasetDAO.findDatasetIdsByDACUserId(user.getUserId()));

    // Filter the list of DARs we can operate on by the datasets accessible to this chairperson
    List<String> referenceIds = collection.getDars().values().stream()
        .filter(d -> datasetIds.containsAll(d.getDatasetIds()))
        .map(DataAccessRequest::getReferenceId)
        .toList();

    if (referenceIds.isEmpty()) {
      logWarn(
          "DAR Collection ID: [%s] does not have any associated DARs that this chairperson can access".formatted(
              collection.getDarCollectionId()));
      return collection;
    }

    // Cancel filtered DAR elections
    cancelElectionsForReferenceIds(referenceIds);

    return getByCollectionId(collection.getDarCollectionId());
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
  public DarCollection createElectionsForDarCollection(User user, DarCollection collection)
      throws Exception {
    try {
      DataAccessRequest dar = collection.getMostRecentDar();
      List<String> createdElectionReferenceIds = collectionServiceDAO.createElectionsForDarByUser(
          user, dar);
      if (createdElectionReferenceIds.isEmpty()) {
        var e = new IllegalStateException(
            "No elections were created for DAR Collection: %s %s".formatted(
                collection.getDarCode(), dar.getReferenceId()));
        logException(e);
        throw e;
      }
      try {
        List<User> voteUsers = voteDAO.findVoteUsersByElectionReferenceIdList(
            createdElectionReferenceIds);
        if (dar.getProgressReport()) {
          emailService.sendProgressReportNewCollectionElectionMessage(voteUsers, collection.getDarCode());
        } else {
          emailService.sendDarNewCollectionElectionMessage(voteUsers, collection.getDarCode());
        }

      } catch (Exception e) {
        logException(
            "Unable to send new case message to DAC members for DAR Collection: %s".formatted(
                collection.getDarCode()), e);
      }
    } catch (Exception e) {
      logException("Exception creating elections and votes for collection: %s".formatted(
          collection.getDarCollectionId()), e);
      throw e;
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


  public void sendNewDARCollectionMessage(Integer collectionId)
      throws IOException, TemplateException {
    DarCollection collection = darCollectionDAO.findDARCollectionByCollectionId(collectionId);
    if (collection == null) {
      logWarn(
          "Sending new DAR Collection message: Could not find collection for specified collection id: "
              + collectionId);
      return;
    }
    // Do this, but only for a single DAR
    DataAccessRequest dar = collection.getMostRecentDar();
    List<User> distinctUsers = getDistinctAdminAndChairUsersForDAR(dar);
    User researcher = userDAO.findUserById(collection.getCreateUserId());
    if (researcher == null) {
      logWarn(
          "Sending new DAR Collection message: Could not find researcher for specified user id: "
              + collection.getCreateUserId());
    }
    String researcherName = researcher == null ? "Unknown" : researcher.getDisplayName();
    // Only do this for the DAR... dacDAO.findDacsForDatasetIds(dar.getDatasetIds())
    Collection<Dac> dacsInDAR = dacDAO.findDacsForDatasetIds(dar.getDatasetIds());
    // Use only the datasets from the dar
    List<Integer> datasetIds = dar.getDatasetIds();
    List<Dataset> datasetsInDAR =
        datasetIds.isEmpty() ? List.of() : datasetDAO.findDatasetsByIdList(datasetIds);

    Map<String, List<String>> sendList = new HashMap<>();
    for (User user : distinctUsers) {
      List<Dac> matchingDacsForUser = getMatchingDacs(user, dacsInDAR);
      for (Dac dac : matchingDacsForUser) {
        List<String> matchingDatasetsForDac = getMatchingDatasets(dac, datasetsInDAR);
        if (matchingDatasetsForDac != null) {
          sendList.put(dac.getName(), matchingDatasetsForDac);
        }
      }
      // If the dar is not a progress report, use the DAR template else use the PR template.
      if (dar.getProgressReport()) {
        // Use the reference ID to link the fact that this progress report will have been noted.
        // the DAR Code at this point will be ambiguous.
        emailService.sendNewProgressReportRequestEmail(user, sendList, researcherName, collection.getDarCode(), dar.getReferenceId());
      } else {
        emailService.sendNewDARRequestEmail(user, sendList, researcherName, collection.getDarCode());
      }
    }
    notifySigningOfficials(collection, dar, researcher);
  }

  @VisibleForTesting
  protected void notifySigningOfficials(DarCollection collection, DataAccessRequest dar,
      User researcher) throws TemplateException, IOException {
    if (researcher == null) {
      logWarn(
          "Unable to send new DAR/PR message to Signing Officials: Researcher does not exist: %s".formatted(
              collection.getCreateUserId()));
      return;
    }
    if (researcher.getInstitutionId() == null) {
      logWarn(
          "Unable to send new DAR/PR message to Signing Officials: Researcher does not have an institution id: %s".formatted(
              collection.getCreateUserId()));
      return;
    }
    List<User> signingOfficials = userDAO.getSOsByInstitution(researcher.getInstitutionId());
    List<Dataset> datasets = datasetDAO.findDatasetsByIdList(dar.getDatasetIds());
    for (User so : signingOfficials) {
      if (Boolean.TRUE.equals(so.getEmailPreference())) {
        if (dar.getProgressReport()) {
          emailService.sendNewSoProgressReportSubmittedEmail(so, collection.getDarCode(),
              researcher, dar.getReferenceId(), datasets);
        } else {
          emailService.sendNewSoDARSubmittedEmail(so, collection.getDarCode(), researcher,
              dar.getReferenceId(), datasets);
        }
      } else {
        logWarn(
            "Signing Official '%s' has notifications disabled.".formatted(so.getDisplayName()));
      }
    }
  }

  private List<User> getDistinctAdminAndChairUsersForDAR(DataAccessRequest dar) {
    List<Integer> datasetIds = dar.getDatasetIds();
    return getDistinctAdminAndChairUsersForDatasetIds(datasetIds);
  }

  private List<User> getDistinctAdminAndChairUsersForDatasetIds(List<Integer> datasetIds) {
    List<User> admins = userDAO.describeUsersByRoleAndEmailPreference(UserRoles.ADMIN.getRoleName(),
        true);
    Set<User> chairPersons = userDAO.findUsersForDatasetsByRole(datasetIds,
        Collections.singletonList(UserRoles.CHAIRPERSON.getRoleName()));
    // Ensure that admins/chairs are not double emailed
    // and filter users that don't want to receive email
    return Streams.concat(admins.stream(), chairPersons.stream())
        .filter(u -> Boolean.TRUE.equals(u.getEmailPreference()))
        .distinct()
        .toList();
  }

  private List<Dac> getMatchingDacs(User user, Collection<Dac> dacsInDAR) {
    List<Integer> dacIDs = user.getRoles().stream()
        .map(UserRole::getDacId)
        .filter(Objects::nonNull)
        .toList();
    return dacsInDAR.stream()
        .filter(dac -> dacIDs.contains(dac.getDacId()))
        .toList();
  }

  private List<String> getMatchingDatasets(Dac dac, List<Dataset> datasetsInDAR) {
    return datasetsInDAR.stream()
        .filter(dataset -> dataset.getDacId().equals(dac.getDacId()))
        .map(Dataset::getDatasetIdentifier)
        .toList();
  }

}
