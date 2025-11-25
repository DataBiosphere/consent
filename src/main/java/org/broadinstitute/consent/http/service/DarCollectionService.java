package org.broadinstitute.consent.http.service;

import static org.broadinstitute.consent.http.enumeration.UserRoles.ADMIN;
import static org.broadinstitute.consent.http.resources.Resource.CHAIRPERSON;
import static org.broadinstitute.consent.http.resources.Resource.MEMBER;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Streams;
import com.google.gson.Gson;
import com.google.inject.Inject;
import freemarker.template.TemplateException;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.broadinstitute.consent.http.db.DacDAO;
import org.broadinstitute.consent.http.db.DarCollectionDAO;
import org.broadinstitute.consent.http.db.DarCollectionSummaryDAO;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
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
import org.broadinstitute.consent.http.rules.DACAutomationRule;
import org.broadinstitute.consent.http.rules.DACAutomationRuleType;
import org.broadinstitute.consent.http.service.dao.DarCollectionServiceDAO;
import org.broadinstitute.consent.http.util.ConsentLogger;
import org.jdbi.v3.core.Jdbi;

public class DarCollectionService implements ConsentLogger {

  private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
  private final DacDAO dacDAO;
  private final DarCollectionDAO darCollectionDAO;
  private final DarCollectionSummaryDAO darCollectionSummaryDAO;
  private final DataAccessRequestDAO dataAccessRequestDAO;
  private final DatasetDAO datasetDAO;
  private final ElectionDAO electionDAO;
  private final UserDAO userDAO;
  private final VoteDAO voteDAO;
  private final DarCollectionServiceDAO collectionServiceDAO;
  private final EmailService emailService;
  private final DACAutomationRuleService dacAutomationRuleService;

  @Inject
  public DarCollectionService(
      Jdbi jdbi,
      DarCollectionServiceDAO darCollectionServiceDAO,
      EmailService emailService,
      DACAutomationRuleService dacAutomationRuleService) {
    this.dacDAO = jdbi.onDemand(DacDAO.class);
    this.darCollectionDAO = jdbi.onDemand(DarCollectionDAO.class);
    this.darCollectionSummaryDAO = jdbi.onDemand(DarCollectionSummaryDAO.class);
    this.dataAccessRequestDAO = jdbi.onDemand(DataAccessRequestDAO.class);
    this.datasetDAO = jdbi.onDemand(DatasetDAO.class);
    this.electionDAO = jdbi.onDemand(ElectionDAO.class);
    this.userDAO = jdbi.onDemand(UserDAO.class);
    this.voteDAO = jdbi.onDemand(VoteDAO.class);
    this.collectionServiceDAO = darCollectionServiceDAO;
    this.emailService = emailService;
    this.dacAutomationRuleService = dacAutomationRuleService;
  }

  private void updateStatusCount(Map<String, Integer> statusCount, String status) {
    // If the status is null, track it as Undefined to ensure election is accounted for.
    statusCount.merge(Objects.requireNonNullElse(status, "Undefined"), 1, Integer::sum);
  }

  private void determineCollectionStatus(
      DarCollectionSummary summary,
      Map<String, Integer> statusCount,
      Integer datasetCount,
      Integer electionCount) {
    // If there are no elections, status is unreviewed
    // if there are some elections open, status is in process
    // if all elections are closed or canceled and electionCount == datasetCount, status is complete
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
    // if at least one election is open, show cancel
    // if at least one non-open/absent election, show open
    summaries.forEach(
        s -> {
          Map<String, Integer> statusCount = new HashMap<>();
          Map<Integer, Election> elections = s.getElections();
          if (elections.isEmpty()) {
            s.addAction(DarCollectionActions.OPEN);
            s.setStatus(DarCollectionStatus.SUBMITTED.getValue());
          } else {
            elections
                .values()
                .forEach(
                    e -> {
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
    // if an election exists, cancel does not appear
    // if there are no elections, review and cancel are present
    // if the collection is canceled, revise and review is present
    summaries.forEach(
        s -> {
          Map<String, Integer> statusCount = new HashMap<>();
          Map<Integer, Election> elections = s.getElections();
          int electionCount = elections.size();
          elections
              .values()
              .forEach(election -> updateStatusCount(statusCount, election.getStatus()));
          s.addAction(DarCollectionActions.REVIEW);
          // if the latest DAR in the collection has at least one approved dataset,
          // include the create progress report action
          Set<Integer> datasetIds =
              dataAccessRequestDAO.findDatasetApprovalsByDar(s.getLatestReferenceId());
          // Can only create a progress report if there are approved datasets, no closeout
          // supplement,
          // and no open elections.
          boolean hasOpenElections =
              statusCount.getOrDefault(ElectionStatus.OPEN.getValue(), 0) > 0;
          if (!hasOpenElections && !datasetIds.isEmpty() && s.getCloseoutSupplement() == null) {
            s.addAction(DarCollectionActions.CREATE_PROGRESS_REPORT);
          }

          // check dar statuses, if they're all canceled show revise (but only if there are no
          // elections)
          if (electionCount == 0) {
            Collection<String> darStatuses = s.getDarStatuses().values();
            boolean isCanceled =
                !darStatuses.isEmpty()
                    && darStatuses.stream()
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

  private void processDarCollectionSummariesForMember(
      List<DarCollectionSummary> summaries, Integer userId) {
    summaries.forEach(
        s -> {
          Collection<Election> elections = s.getElections().values();
          Integer electionCount = elections.size();
          // if there are no elections present, unreviewed
          // if there are elections present. in process
          if (electionCount == 0) {
            s.setStatus(DarCollectionStatus.SUBMITTED.getValue());
          } else {
            boolean isVotable =
                elections.stream()
                    .anyMatch(
                        election ->
                            election.getStatus().equalsIgnoreCase(ElectionStatus.OPEN.getValue()));

            if (isVotable) {
              s.setStatus(DarCollectionStatus.IN_PROCESS.getValue());
              List<Vote> votes =
                  s.getVotes().stream()
                      .filter(
                          v ->
                              v.getUserId().equals(userId)
                                  && v.getType().equals(VoteType.DAC.getValue()))
                      .toList();
              if (!votes.isEmpty()) {
                boolean hasVoted = votes.stream().map(Vote::getVote).allMatch(Objects::nonNull);
                DarCollectionActions targetAction =
                    hasVoted ? DarCollectionActions.UPDATE : DarCollectionActions.VOTE;
                s.addAction(targetAction);
              }
            } else {
              // non-votable states
              // all canceled (complete)
              // some datasets do not have elections (in process)
              // all voted on (complete)
              // no elections
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
    summaries.forEach(
        s -> {
          Map<String, Integer> statusCount = new HashMap<>();
          Map<Integer, Election> elections = s.getElections();
          if (elections.size() < s.getDatasetCount()) {
            s.addAction(DarCollectionActions.OPEN);
          }
          elections
              .values()
              .forEach(election -> updateStatusCount(statusCount, election.getStatus()));
          Integer closedCount = statusCount.get(ElectionStatus.CLOSED.getValue());
          Integer openCount = statusCount.get(ElectionStatus.OPEN.getValue());
          determineCollectionStatus(s, statusCount, s.getDatasetCount(), s.getElections().size());
          updateSummaryActionsForChair(s, closedCount, openCount);
        });
  }

  /**
   * Update the summary actions for a chairperson based on the summary and election counts.
   *
   * @param summary The DarCollectionSummary to update
   * @param closedCount The count of closed elections
   * @param openCount The count of open elections
   */
  private void updateSummaryActionsForChair(
      DarCollectionSummary summary, Integer closedCount, Integer openCount) {

    // By default, no actions can be taken on a closeout supplement
    if (summary.getCloseoutSupplement() != null) {
      summary.getActions().clear();
      // If the SO has approved the closeout supplement, allow review of the progress report.
      if (summary.getCloseoutSigningOfficialApprovalDate() != null) {
        summary.addAction(DarCollectionActions.REVIEW_PROGRESS_REPORT);
      }
      return;
    }

    // If there are no elections, only show open
    if (summary.getElections().isEmpty()) {
      summary.addAction(DarCollectionActions.OPEN);
    }

    // If there are closed or canceled elections, show open
    // If there are any open elections, show vote
    summary
        .getElections()
        .values()
        .forEach(
            election -> {
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
    summaries.forEach(
        s -> {
          Map<String, Integer> statusCount = new HashMap<>();
          s.getElections()
              .values()
              .forEach(election -> updateStatusCount(statusCount, election.getStatus()));
          determineCollectionStatus(s, statusCount, s.getDatasetCount(), s.getElections().size());
          updateSummaryActionsForSO(s);
        });
  }

  private void updateSummaryActionsForSO(DarCollectionSummary summary) {
    // If the SO has not yet approved the closeout supplement, allow review of the progress report.
    if (summary.getCloseoutSupplement() != null
        && summary.getCloseoutSigningOfficialApprovalDate() == null) {
      summary.addAction(DarCollectionActions.REVIEW_PROGRESS_REPORT);
    }
  }

  /**
   * Find all DarCollectionSummaries for a given role. Admins can see all summaries Chairs and
   * Members can see summaries for datasets they have access to Signing Officials can see summaries
   * for researchers in their institution Researchers can see only their own summaries
   *
   * @param user The user making the request
   * @param role The role the user is making the request as
   * @return List of DarCollectionSummary objects
   */
  public List<DarCollectionSummary> getSummariesForRole(User user, UserRoles role) {
    final List<DarCollectionSummary> summaries;
    Integer userId = user.getUserId();
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
        summaries =
            darCollectionSummaryDAO.getDarCollectionSummariesForDACRole(
                userId, UserRoles.CHAIRPERSON.getRoleId());
        processDarCollectionSummariesForChair(summaries);
        break;
      case MEMBER:
        summaries =
            darCollectionSummaryDAO.getDarCollectionSummariesForDACRole(
                userId, UserRoles.MEMBER.getRoleId());
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
    List<Integer> roleDacIds =
        user.getRoles().stream()
            .filter(ur -> Objects.nonNull(ur.getRoleId()))
            .filter(ur -> ur.getRoleId().equals(roleId))
            .map(UserRole::getDacId)
            .filter(Objects::nonNull)
            .toList();
    return datasetDAO.findDatasetIdsByDacIds(roleDacIds);
  }

  /**
   * Finds the DarCollectionSummary for a given darCollectionId, processed by the given role.
   *
   * @param user The user making the request
   * @param role The role the user is making the request as
   * @param collectionId The darCollectionId of the requested DarCollectionSummary
   * @return A DarCollectionSummary object
   */
  public DarCollectionSummary getSummaryForRoleByCollectionId(
      User user, UserRoles role, Integer collectionId) {
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
          summary =
              darCollectionSummaryDAO.getDarCollectionSummaryForDACByCollectionId(
                  userId, datasetIds, collectionId);
          processDarCollectionSummariesForChair(List.of(summary));
          break;
        case MEMBER:
          datasetIds = getDatasetIdsForUserAndRoleId(user, UserRoles.MEMBER.getRoleId());
          summary =
              darCollectionSummaryDAO.getDarCollectionSummaryForDACByCollectionId(
                  userId, datasetIds, collectionId);
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
    sourceCollection
        .getDars()
        .values()
        .forEach(
            (d) -> {
              Date now = new Date();
              DataAccessRequestData newData =
                  new Gson().fromJson(d.getData().toString(), DataAccessRequestData.class);
              newData.setStatus(null);
              newData.setReferenceId(d.getReferenceId());
              dataAccessRequestDAO.updateDataByReferenceId(
                  d.getReferenceId(), d.getUserId(), null, now, newData, null);
            });

    // get updated collection
    sourceCollection =
        this.darCollectionDAO.findDARCollectionByCollectionId(
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

  public DarCollection getByReferenceId(User user, String referenceId) {
    DarCollection collection = darCollectionDAO.findDARCollectionByReferenceId(referenceId);
    if (Objects.isNull(collection)) {
      throw new NotFoundException(
          "Collection with the reference id of " + referenceId + " was not found");
    }
    return filterCollectionVotesForUser(user, addDatasetsToCollection(collection));
  }

  public DarCollection getByCollectionId(User user, Integer collectionId) {
    DarCollection collection = darCollectionDAO.findDARCollectionByCollectionId(collectionId);
    if (Objects.isNull(collection)) {
      throw new NotFoundException(
          "Collection with the collection id of " + collectionId + " was not found");
    }
    return filterCollectionVotesForUser(user, addDatasetsToCollection(collection));
  }

  /**
   * Given a DarCollection, remove all votes from elections for roles that should not see them. This
   * method mutates the given collection.
   *
   * @param user The User requesting the collection
   * @param collection DarCollection to filter votes from
   * @return DarCollection
   */
  private DarCollection filterCollectionVotesForUser(User user, DarCollection collection) {
    // Individual votes are only visible to CHAIRPERSON, MEMBER, and ADMIN roles
    List<UserRoles> voteViewRoles = List.of(UserRoles.CHAIRPERSON, UserRoles.MEMBER, ADMIN);
    if (user.hasAnyUserRole(voteViewRoles)) {
      return collection;
    }
    // Remove votes from elections for all other users
    collection
        .getDars()
        .values()
        .forEach(
            dar -> dar.getElections().values().forEach(election -> election.setVotes(Map.of())));
    return collection;
  }

  public DarCollection getCollectionWithAllElectionsByCollectionId(
      User user, Integer collectionId) {
    DarCollection collection =
        darCollectionDAO.findCollectionWithAllElectionsByCollectionId(collectionId);
    if (Objects.isNull(collection)) {
      throw new NotFoundException(
          "Collection with the collection id of " + collectionId + " was not found");
    }
    return filterCollectionVotesForUser(user, addDatasetsToCollection(collection));
  }

  public DarCollection getCollectionWithElectionsByCollectionIdAndDatasetIds(
      User user, List<Integer> datasetIds, Integer collectionId) {
    DarCollection collection =
        darCollectionDAO.findCollectionWithElectionsByCollectionIdAndDatasetIds(
            datasetIds, collectionId);
    if (Objects.isNull(collection)) {
      throw new NotFoundException(
          "Collection with the collection id of " + collectionId + " was not found");
    }
    return filterCollectionVotesForUser(user, addDatasetsToCollection(collection));
  }

  /**
   * Given a DarCollection, add its relevant datasets.
   *
   * @param collection The list of DarCollections to iterate over.
   * @return collection with datasets added
   */
  @VisibleForTesting
  protected DarCollection addDatasetsToCollection(DarCollection collection) {
    // get datasetIds from each DAR from each collection
    List<String> referenceIds = List.copyOf(collection.getDars().keySet());
    List<Integer> datasetIds =
        referenceIds.isEmpty()
            ? List.of()
            : dataAccessRequestDAO.findAllDARDatasetRelations(referenceIds);
    if (!datasetIds.isEmpty()) {
      Map<Integer, Dataset> datasetMap =
          datasetDAO.findDatasetsByIdList(datasetIds).stream()
              .distinct()
              .collect(Collectors.toMap(Dataset::getDatasetId, Function.identity()));

      Set<Dataset> collectionDatasets =
          collection.getDars().values().stream()
              .map(DataAccessRequest::getDatasetIds)
              .flatMap(Collection::stream)
              .map(datasetMap::get)
              .filter(Objects::nonNull) // filtering out nulls which were getting captured by map
              .collect(Collectors.toSet());
      collection.setDatasets(collectionDatasets);
      return collection.deepCopy();
    }
    // There were no datasets to add, so we return the original list
    return collection;
  }

  /**
   * Cancel Elections or a dar for a DarCollection, given a user and a role. If the user is a chair,
   * or admin, cancel elections. If the user is a researcher, cancel the dar.
   *
   * @param user The User initiating the cancel
   * @param collection The DarCollection
   * @param role The role of the user, must be one of ADMIN, CHAIRPERSON, or RESEARCHER
   * @return The DarCollection that has been canceled
   */
  public DarCollection cancelDarCollectionByRole(
      User user, DarCollection collection, UserRoles role) {
    Collection<DataAccessRequest> dars = collection.getDars().values();
    if (dars.isEmpty()) {
      logWarn(
          "DAR Collection ID: [%s] does not have any associated DAR ids"
              .formatted(collection.getDarCollectionId()));
      return collection;
    }

    DarCollection cancelledCollection =
        switch (role) {
          case ADMIN -> cancelDarCollectionElectionsAsAdmin(collection, user);
          case CHAIRPERSON -> cancelDarCollectionElectionsAsChair(collection, user);
          default -> cancelDarCollectionAsResearcher(collection, user);
        };
    return getByCollectionId(user, cancelledCollection.getDarCollectionId());
  }

  /**
   * Cancel a DarCollection as a researcher.
   *
   * <p>If an election exists for a DAR within the collection, that DAR cannot be cancelled by the
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
    DarCollectionSummary summary =
        darCollectionSummaryDAO.getDarCollectionSummaryByCollectionId(
            collection.getDarCollectionId());
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
    List<String> activeDarIds =
        dars.stream()
            .filter(d -> !DataAccessRequest.isCanceled(d))
            .map(DataAccessRequest::getReferenceId)
            .toList();
    if (!activeDarIds.isEmpty()) {
      dataAccessRequestDAO.cancelByReferenceIds(activeDarIds);
    }

    return getByCollectionId(user, collection.getDarCollectionId());
  }

  /**
   * Cancel Elections for a DarCollection as an admin.
   *
   * <p>Admins can cancel all elections in a DarCollection
   *
   * @param collection The DarCollection
   * @return The DarCollection whose elections have been canceled
   */
  private DarCollection cancelDarCollectionElectionsAsAdmin(DarCollection collection, User user) {
    Collection<DataAccessRequest> dars = collection.getDars().values();
    List<String> referenceIds = dars.stream().map(DataAccessRequest::getReferenceId).toList();

    // Cancel all DAR elections
    cancelElectionsForReferenceIds(referenceIds);

    return getByCollectionId(user, collection.getDarCollectionId());
  }

  /**
   * Cancel Elections for a DarCollection as a chairperson.
   *
   * <p>Chairs can only cancel Elections that reference a dataset the chair is a DAC member for.
   *
   * @param collection The DarCollection
   * @return The DarCollection whose elections have been canceled
   */
  private DarCollection cancelDarCollectionElectionsAsChair(DarCollection collection, User user) {
    // Find dataset ids the chairperson has access to:
    Set<Integer> datasetIds = Set.copyOf(datasetDAO.findDatasetIdsByDACUserId(user.getUserId()));

    // Filter the list of DARs we can operate on by the datasets accessible to this chairperson
    List<String> referenceIds =
        collection.getDars().values().stream()
            .filter(d -> datasetIds.containsAll(d.getDatasetIds()))
            .map(DataAccessRequest::getReferenceId)
            .toList();

    if (referenceIds.isEmpty()) {
      logWarn(
          "DAR Collection ID: [%s] does not have any associated DARs that this chairperson can access"
              .formatted(collection.getDarCollectionId()));
      return collection;
    }

    // Cancel filtered DAR elections
    cancelElectionsForReferenceIds(referenceIds);

    return getByCollectionId(user, collection.getDarCollectionId());
  }

  /**
   * DarCollections with no elections, or with previously canceled elections, are valid for
   * initiating a new set of elections. Elections in open, closed, pending, or final states are not
   * valid.
   *
   * @param user The User initiating new elections for a collection
   * @param collection The DarCollection
   * @return The updated DarCollection
   */
  public DarCollection createElectionsForDarCollection(User user, DarCollection collection)
      throws Exception {
    try {
      DataAccessRequest dar = collection.getMostRecentDar();
      List<String> createdElectionReferenceIds =
          collectionServiceDAO.createElectionsForDarByUser(user, dar);
      if (createdElectionReferenceIds.isEmpty()) {
        var e =
            new IllegalStateException(
                "No elections were created for DAR Collection: %s %s"
                    .formatted(collection.getDarCode(), dar.getReferenceId()));
        logException(e);
        throw e;
      }
      try {
        List<User> voteUsers =
            voteDAO.findVoteUsersByElectionReferenceIdList(createdElectionReferenceIds);
        if (dar.getProgressReport()) {
          emailService.sendProgressReportNewCollectionElectionMessage(
              voteUsers, collection.getDarCode());
        } else {
          emailService.sendDarNewCollectionElectionMessage(voteUsers, collection.getDarCode());
        }

      } catch (Exception e) {
        logException(
            "Unable to send new case message to DAC members for DAR Collection: %s"
                .formatted(collection.getDarCode()),
            e);
      }
    } catch (Exception e) {
      logException(
          "Exception creating elections and votes for collection: %s"
              .formatted(collection.getDarCollectionId()),
          e);
      throw e;
    }
    return getByCollectionId(user, collection.getDarCollectionId());
  }

  // Private helper method to mark Elections as 'Canceled'
  private void cancelElectionsForReferenceIds(List<String> referenceIds) {
    List<Election> elections = electionDAO.findOpenElectionsByReferenceIds(referenceIds);
    elections.forEach(
        election -> {
          if (!election.getStatus().equals(ElectionStatus.CANCELED.getValue())) {
            electionDAO.updateElectionById(
                election.getElectionId(), ElectionStatus.CANCELED.getValue(), new Date());
          }
        });
  }

  public void sendNewDARCollectionMessage(Integer collectionId)
      throws IOException, TemplateException {
    // Retrieve the DAR collection by its ID
    DarCollection darCollection = darCollectionDAO.findDARCollectionByCollectionId(collectionId);
    if (darCollection == null) {
      logWarn("Could not find DAR collection for collection id: " + collectionId);
      return;
    }

    // Get the most recent DAR and its associated users
    DataAccessRequest latestDar = darCollection.getMostRecentDar();
    List<User> adminAndChairUsers = getDistinctAdminAndChairUsersForDAR(latestDar);

    // Get researcher details
    User researcher = userDAO.findUserById(darCollection.getCreateUserId());
    String researcherName = researcher == null ? "Unknown" : researcher.getDisplayName();

    // Get DACs and datasets for the DAR
    Collection<Dac> dacsForDar = dacDAO.findDacsForDatasetIds(latestDar.getDatasetIds());
    List<Integer> datasetIds = latestDar.getDatasetIds();
    List<Dataset> datasetsForDar =
        datasetIds.isEmpty() ? List.of() : datasetDAO.findDatasetsByIdList(datasetIds);

    // Prepare sets for DACs, users, and datasets based on automation rules
    Set<Integer> dacIds =
        datasetsForDar.stream().map(Dataset::getDacId).collect(Collectors.toSet());
    Set<Dac> autoOpenDacs = new HashSet<>();
    Set<Dac> manualOpenDacs = new HashSet<>();
    Set<User> autoOpenUsers = new HashSet<>();
    Set<User> manualOpenUsers = new HashSet<>();
    Set<Dataset> autoOpenDatasets = new HashSet<>();
    Set<Dataset> manualOpenDatasets = new HashSet<>();

    // Classify DACs and users based on automation rules
    for (Integer dacId : dacIds) {
      List<DACAutomationRule> automationRules = dacAutomationRuleService.findAllByDacId(dacId);
      boolean hasAutoOpenRule = false;
      Integer autoOpenUserId = null;

      for (DACAutomationRule rule : automationRules) {
        if (rule.ruleType() == DACAutomationRuleType.AUTO_OPEN_DAR_FOR_ALL_MEMBERS
            && rule.enabledByUserId() != null) {
          hasAutoOpenRule = true;
          autoOpenUserId = rule.enabledByUserId();
          break;
        }
      }

      Optional<Dac> dacOpt =
          dacsForDar.stream().filter(dac -> dac.getDacId().equals(dacId)).findFirst();
      Optional<Dataset> datasetOpt =
          datasetsForDar.stream().filter(dataset -> dataset.getDacId().equals(dacId)).findFirst();

      if (hasAutoOpenRule) {
        dacOpt.ifPresent(autoOpenDacs::add);
        datasetOpt.ifPresent(autoOpenDatasets::add);

        // Add eligible users for auto-open DACs
        for (User user : adminAndChairUsers) {
          if (user.verifyDACRole(CHAIRPERSON, dacId)
              || user.verifyDACRole(MEMBER, dacId)
              || user.hasUserRole(ADMIN)) {
            autoOpenUsers.add(user);
          }
        }

        // Create elections and votes automatically
        if (autoOpenUserId != null && datasetOpt.isPresent()) {
          int electionId =
              dacAutomationRuleService.createElectionForDAR(latestDar, datasetOpt.get());
          dacAutomationRuleService.createVoteForElection(
              electionId, autoOpenUserId, VoteType.CHAIRPERSON);

          for (User user : autoOpenUsers) {
            if (!user.getUserId().equals(autoOpenUserId)) {
              dacAutomationRuleService.createVoteForElection(
                  electionId, user.getUserId(), VoteType.DAC);
            }
          }
        }
      } else {
        dacOpt.ifPresent(manualOpenDacs::add);
        datasetOpt.ifPresent(manualOpenDatasets::add);

        // Add eligible users for manual DACs
        for (User user : adminAndChairUsers) {
          if (user.verifyDACRole(CHAIRPERSON, dacId) || user.hasUserRole(ADMIN)) {
            manualOpenUsers.add(user);
          }
        }
      }
    }

    // Notify users for auto-open DACs
    Map<String, List<String>> dacToDatasetsMap = new HashMap<>();
    for (User user : autoOpenUsers) {
      List<Dac> userDacs = getMatchingDacs(user, autoOpenDacs);
      for (Dac dac : userDacs) {
        List<String> datasets = getMatchingDatasets(dac, new ArrayList<>(autoOpenDatasets));
        dacToDatasetsMap.put(dac.getName(), datasets);
      }
      if (latestDar.getProgressReport()) {
        emailService.sendProgressReportNewCollectionElectionMessage(
            List.of(user), darCollection.getDarCode());
      } else {
        emailService.sendDarNewCollectionElectionMessage(List.of(user), darCollection.getDarCode());
      }
    }

    // Notify users for manual DACs
    dacToDatasetsMap.clear();
    for (User user : manualOpenUsers) {
      List<Dac> userDacs = getMatchingDacs(user, manualOpenDacs);
      for (Dac dac : userDacs) {
        List<String> datasets = getMatchingDatasets(dac, new ArrayList<>(manualOpenDatasets));
        dacToDatasetsMap.put(dac.getName(), datasets);
      }
      if (latestDar.getProgressReport()) {
        emailService.sendNewProgressReportRequestEmail(
            user,
            dacToDatasetsMap,
            researcherName,
            darCollection.getDarCode(),
            latestDar.getReferenceId());
      } else {
        emailService.sendNewDARRequestEmail(
            user, dacToDatasetsMap, researcherName, darCollection.getDarCode());
      }
    }

    // Notify signing officials of DAR submission
    notifySigningOfficialsOfDARSubmission(latestDar, researcher, darCollection.getDarCode());
  }

  @VisibleForTesting
  protected void notifySigningOfficialsOfDARSubmission(
      DataAccessRequest dar, User researcher, String darCode)
      throws TemplateException, IOException {
    if (researcher == null) {
      logWarn(
          "Unable to send new DAR/PR message to Signing Officials: Researcher does not exist: %s"
              .formatted(dar.getUserId()));
      return;
    }
    if (researcher.getInstitutionId() == null) {
      logWarn(
          "Unable to send new DAR/PR message to Signing Officials: Researcher does not have an institution id: %s"
              .formatted(dar.getUserId()));
      return;
    }
    List<User> signingOfficials = userDAO.getSOsByInstitution(researcher.getInstitutionId());
    List<Dataset> datasets = datasetDAO.findDatasetsByIdList(dar.getDatasetIds());
    for (User so : signingOfficials) {
      if (dar.getProgressReport()) {
        emailService.sendNewSoProgressReportSubmittedEmail(
            so, darCode, researcher, dar.getReferenceId(), datasets);
      } else {
        emailService.sendNewSoDARSubmittedEmail(
            so, darCode, researcher, dar.getReferenceId(), datasets);
      }
    }
  }

  private List<User> getDistinctAdminAndChairUsersForDAR(DataAccessRequest dar) {
    List<Integer> datasetIds = dar.getDatasetIds();
    return getDistinctAdminAndChairUsersForDatasetIds(datasetIds);
  }

  private List<User> getDistinctAdminAndChairUsersForDatasetIds(List<Integer> datasetIds) {
    List<User> admins = userDAO.findUsersByRoleId(ADMIN.getRoleId());
    Set<User> chairPersons =
        userDAO.findUsersForDatasetsByRole(
            datasetIds, Collections.singletonList(UserRoles.CHAIRPERSON.getRoleId()));
    // Ensure that admins/chairs are not double emailed
    return Streams.concat(admins.stream(), chairPersons.stream()).distinct().toList();
  }

  private List<Dac> getMatchingDacs(User user, Collection<Dac> dacsInDAR) {
    if (user.hasUserRole(ADMIN)) {
      return new ArrayList<>(dacsInDAR);
    }
    List<Integer> dacIDs =
        user.getRoles().stream().map(UserRole::getDacId).filter(Objects::nonNull).toList();
    return dacsInDAR.stream().filter(dac -> dacIDs.contains(dac.getDacId())).toList();
  }

  private List<String> getMatchingDatasets(Dac dac, List<Dataset> datasetsInDAR) {
    return datasetsInDAR.stream()
        .filter(dataset -> dataset.getDacId().equals(dac.getDacId()))
        .map(Dataset::getDatasetIdentifier)
        .toList();
  }
}
