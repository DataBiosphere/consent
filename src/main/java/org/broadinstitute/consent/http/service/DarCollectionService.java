package org.broadinstitute.consent.http.service;

import static org.broadinstitute.consent.http.enumeration.ElectionType.DATA_ACCESS;
import static org.broadinstitute.consent.http.enumeration.ElectionType.RP;
import static org.broadinstitute.consent.http.enumeration.UserRoles.ADMIN;
import static org.broadinstitute.consent.http.resources.Resource.CHAIRPERSON;
import static org.broadinstitute.consent.http.resources.Resource.MEMBER;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;
import com.google.inject.Inject;
import freemarker.template.TemplateException;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
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
import org.broadinstitute.consent.http.db.DaaDAO;
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
import org.broadinstitute.consent.http.exceptions.ConsentConflictException;
import org.broadinstitute.consent.http.mail.message.NewCaseMessage;
import org.broadinstitute.consent.http.mail.message.NewDARRequestMessage;
import org.broadinstitute.consent.http.mail.message.NewDARSigningOfficialRequestMessage;
import org.broadinstitute.consent.http.mail.message.NewProgressReportCaseMessage;
import org.broadinstitute.consent.http.mail.message.NewProgressReportRequestMessage;
import org.broadinstitute.consent.http.mail.message.SoDARSubmitted;
import org.broadinstitute.consent.http.mail.message.SoPRSubmitted;
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
import org.glassfish.jersey.server.ContainerRequest;
import org.jdbi.v3.core.Jdbi;

public class DarCollectionService implements ConsentLogger {

  private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
  private final DacDAO dacDAO;
  private final DaaDAO daaDAO;
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
    this.daaDAO = jdbi.onDemand(DaaDAO.class);
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
          int electionCount = elections.size();
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
          if ((!s.requiresSOApproval() || s.getSOApprover() != null)
              && elections.size() < s.getDatasetCount()) {
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
    if ((!summary.requiresSOApproval() || summary.getSOApprover() != null)
        && summary.getElections().isEmpty()) {
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

  private void processDarCollectionSummariesForSO(User user, List<DarCollectionSummary> summaries) {
    summaries.forEach(
        s -> {
          Map<String, Integer> statusCount = new HashMap<>();
          s.getElections()
              .values()
              .forEach(election -> updateStatusCount(statusCount, election.getStatus()));
          determineCollectionStatus(s, statusCount, s.getDatasetCount(), s.getElections().size());
          updateSummaryActionsForSO(user, s);
        });
  }

  private void updateSummaryActionsForSO(User user, DarCollectionSummary summary) {
    // If the SO has not yet approved the closeout supplement, allow review of the progress report.
    if (summary.getCloseoutSupplement() != null
        && summary.getCloseoutSigningOfficialApprovalDate() == null
        && user.getUserId().equals(summary.getCloseoutSupplement().signingOfficialId())) {
      summary.addAction(DarCollectionActions.REVIEW_PROGRESS_REPORT);
    }
    if (summary.requiresSOApproval()
        && summary.getSOApprover() == null
        && user.getEmail().equalsIgnoreCase(summary.getSigningOfficialEmail())) {
      summary.addAction(DarCollectionActions.APPROVE);
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
        processDarCollectionSummariesForSO(user, summaries);
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
          processDarCollectionSummariesForSO(user, List.of(summary));
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
              daaDAO.deleteDarDatasetDaaSnapshotsByReferenceId(d.getReferenceId());
              dataAccessRequestDAO.updateDataByReferenceId(
                  d.getReferenceId(), d.getUserId(), null, now, newData, null);
            });

    // get updated collection
    sourceCollection =
        this.darCollectionDAO.findDARCollectionByCollectionId(
            sourceCollection.getDarCollectionId());

    return this.processDraftAsSummary(
        new ArrayList<>(sourceCollection.getDars().values()).getFirst());
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
            dar ->
                dar.getElections()
                    .values()
                    .forEach(election -> election.setVotes(deIdentifyVotes(election.getVotes()))));
    return collection;
  }

  /**
   * Given a map of votes, return a new map with the same keys but with de-identified votes (userId
   * and vote value removed).
   *
   * @param votes The original map of votes to de-identify
   * @return A new map of votes with all info removed except for the type of vote (DAC or RP) and
   *     Vote Result (True/False/null)
   */
  private Map<Integer, Vote> deIdentifyVotes(Map<Integer, Vote> votes) {
    return votes.entrySet().stream()
        .collect(
            Collectors.toMap(
                Map.Entry::getKey,
                entry -> {
                  Vote v = entry.getValue();
                  Vote deidentified = new Vote();
                  deidentified.setVote(v.getVote());
                  deidentified.setType(v.getType());
                  return deidentified;
                }));
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
   * @param user The User initiating new elections for a collection
   * @param collection The DarCollection
   * @param request The request context
   * @return The updated DarCollection
   * @throws ConsentConflictException Can be thrown when the collection no longer requires approval
   *     or the underlying data access request does not support this operation.
   * @throws BadRequestException Can be thrown when a collection required approval but is already
   *     approved
   * @throws ForbiddenException The user is not the specified signing official required to approve
   *     the collection
   */
  public DarCollection approveDarCollection(
      User user, DarCollection collection, ContainerRequest request)
      throws ConsentConflictException, BadRequestException, ForbiddenException {
    DataAccessRequest dar = collection.getMostRecentDar();
    if (dar.getRequiresSOApproval()) {
      approveDataAccessRequestBySigningOfficial(user, dar, request);
      return darCollectionDAO.findDARCollectionByCollectionId(dar.getCollectionId());
    }
    throw new ConsentConflictException("This collection does not require approval.");
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
      throws BadRequestException, ForbiddenException, ConsentConflictException, SQLException {
    DataAccessRequest dar = collection.getMostRecentDar();
    if ((!dar.getRequiresSOApproval() || dar.getApprovingSigningOfficialUserId() != null)) {
      try {
        List<String> createdElectionReferenceIds =
            collectionServiceDAO.createElectionsForDarByUser(user, dar);
        if (createdElectionReferenceIds.isEmpty()) {
          var e =
              new IllegalStateException(
                  "No elections were created for DAR Collection: %s %s"
                      .formatted(collection.getDarCode(), dar.getReferenceId()));
          logWarn(e.getMessage());
          throw e;
        }
        try {
          List<User> voteUsers =
              voteDAO.findVoteUsersByElectionReferenceIdList(createdElectionReferenceIds);
          if (dar.getProgressReport()) {
            sendProgressReportNewCollectionElectionMessage(voteUsers, collection.getDarCode());
          } else {
            sendDarNewCollectionElectionMessage(voteUsers, collection.getDarCode());
          }

        } catch (Exception e) {
          logException(
              "Unable to send new case message to DAC members for DAR Collection: %s"
                  .formatted(collection.getDarCode()),
              e);
        }
        return getByCollectionId(user, collection.getDarCollectionId());
      } catch (Exception e) {
        logException(
            "Exception creating elections and votes for collection: %s"
                .formatted(collection.getDarCollectionId()),
            e);
        throw e;
      }
    } else {
      throw new ForbiddenException(
          "The user is either not authorized to make this request or the request is awaiting an additional approval processes.");
    }
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

  /** Creates elections for a new DAR collection. */
  public void createElectionsForNewDarCollection(Integer collectionId) {
    DarCollectionContext context = getDarCollectionContext(collectionId);
    if (context == null) {
      return;
    }

    // Create elections and votes for auto-open DACs
    createElectionsAndVotesForAutoOpenDacs(context.classification(), context.latestDar());
  }

  /** Sends notification messages for a new DAR collection. */
  public void sendNewDARCollectionMessage(Integer collectionId)
      throws IOException, TemplateException {
    DarCollectionContext context = getDarCollectionContext(collectionId);
    if (context == null) {
      return;
    }
    if (!context.latestDar().getRequiresSOApproval()
        || context.latestDar().getApprovingSigningOfficialUserId() != null) {
      // Notify users for auto-open DACs
      notifyUsersForDacs(
          context.classification().autoOpenUsers,
          context.classification().autoOpenDacs,
          context.classification().autoOpenDatasets,
          context.latestDar(),
          context.darCollection(),
          context.researcherName(),
          true);

      // Notify users for manual DACs
      notifyUsersForDacs(
          context.classification().manualOpenUsers,
          context.classification().manualOpenDacs,
          context.classification().manualOpenDatasets,
          context.latestDar(),
          context.darCollection(),
          context.researcherName(),
          false);
    } else {
      notifySpecificSigningOfficialOfApprovalNeeded(context.latestDar(), context.researcher());
    }

    // Notify signing officials of DAR submission
    notifySigningOfficialsOfDARSubmission(
        context.latestDar(), context.researcher(), context.darCollection().getDarCode());
  }

  /** Helper method to retrieve the DAR collection context for processing. */
  private DarCollectionContext getDarCollectionContext(Integer collectionId) {
    // Retrieve the DAR collection by its ID
    DarCollection darCollection = darCollectionDAO.findDARCollectionByCollectionId(collectionId);
    if (darCollection == null) {
      logWarn("Could not find DAR collection for collection id: " + collectionId);
      return null;
    }

    // Get the most recent DAR and its associated users
    DataAccessRequest latestDar = darCollection.getMostRecentDar();
    List<User> dacUsers = getDacUsersForDAR(latestDar);

    // Get researcher details
    User researcher = userDAO.findUserById(darCollection.getCreateUserId());
    String researcherName = researcher == null ? "Unknown" : researcher.getDisplayName();

    // Get DACs and datasets for the DAR
    List<Integer> datasetIds = latestDar.getDatasetIds();
    List<Dataset> datasetsForDar = datasetDAO.findDatasetsByIdList(datasetIds);
    Collection<Dac> dacsForDar = dacDAO.findDacsForDatasetIds(datasetIds);

    // Classify DACs and users by automation rules
    DacUserClassification classification =
        classifyDacsAndUsers(dacsForDar, datasetsForDar, dacUsers);

    return new DarCollectionContext(
        darCollection, latestDar, researcher, researcherName, classification);
  }

  /** Classifies DACs and users based on automation rules. */
  @VisibleForTesting
  protected DacUserClassification classifyDacsAndUsers(
      Collection<Dac> dacsForDar, List<Dataset> datasetsForDar, List<User> dacUsers) {

    Set<Integer> dacIds =
        datasetsForDar.stream().map(Dataset::getDacId).collect(Collectors.toSet());

    DacUserClassification result = new DacUserClassification();

    for (Integer dacId : dacIds) {
      Optional<DACAutomationRule> autoOpenRule =
          dacAutomationRuleService.findAllByDacId(dacId).stream()
              .filter(
                  r ->
                      r.ruleType() == DACAutomationRuleType.AUTO_OPEN_DAR_FOR_ALL_MEMBERS
                          && r.enabledByUserId() != null)
              .findFirst();

      Dac dac =
          dacsForDar.stream().filter(d -> d.getDacId().equals(dacId)).findFirst().orElse(null);
      List<Dataset> datasetsForDac =
          datasetsForDar.stream().filter(ds -> ds.getDacId().equals(dacId)).toList();

      if (autoOpenRule.isPresent()) {
        if (dac != null) {
          result.autoOpenDacs.add(dac);
        }
        result.autoOpenDatasets.addAll(datasetsForDac);
        result.autoOpenUserIds.put(dacId, autoOpenRule.get().enabledByUserId());
        addUsers(result.autoOpenUsers, dacId, dacUsers, true);
      } else {
        if (dac != null) {
          result.manualOpenDacs.add(dac);
        }
        result.manualOpenDatasets.addAll(datasetsForDac);
        addUsers(result.manualOpenUsers, dacId, dacUsers, false);
      }
    }

    return result;
  }

  /** Adds users to the target set based on their DAC roles and the autoOpen flag. */
  @VisibleForTesting
  protected void addUsers(Set<User> targetSet, Integer dacId, List<User> users, boolean autoOpen) {

    for (User user : users) {
      boolean isChair = user.verifyDACRole(CHAIRPERSON, dacId);
      boolean isMember = user.verifyDACRole(MEMBER, dacId);
      if (autoOpen) {
        if (isChair || isMember) {
          targetSet.add(user);
        }
      } else { // manual
        if (isChair) {
          targetSet.add(user);
        }
      }
    }
  }

  /** Returns users from the set that hold a CHAIRPERSON or MEMBER role for the given DAC. */
  @VisibleForTesting
  protected Set<User> filterUsersForDac(Set<User> allUsers, Integer dacId) {
    return allUsers.stream()
        .filter(u -> u.verifyDACRole(CHAIRPERSON, dacId) || u.verifyDACRole(MEMBER, dacId))
        .collect(Collectors.toSet());
  }

  /** Creates elections and votes for DACs with auto-open rules. */
  @VisibleForTesting
  protected void createElectionsAndVotesForAutoOpenDacs(
      DacUserClassification classification, DataAccessRequest latestDar) {
    if (latestDar.requiresSOApproval && latestDar.getApprovingSigningOfficialUserId() == null) {
      return;
    }
    for (Dataset dataset : classification.autoOpenDatasets) {
      if (hasOpenElection(latestDar, dataset)) {
        continue;
      }

      archiveOldElections(latestDar, dataset);

      // Wrap in transaction to ensure election and votes are created atomically
      electionDAO.inTransaction(
          _ -> {
            int dataAccessElectionId =
                dacAutomationRuleService.createOpenElectionForDAR(latestDar, dataset, DATA_ACCESS);
            int rpElectionId =
                dacAutomationRuleService.createOpenElectionForDAR(latestDar, dataset, RP);

            Integer dacId = dataset.getDacId();
            Set<User> dacUsers = filterUsersForDac(classification.autoOpenUsers, dacId);
            createVotesForAllUsers(dacUsers, dataAccessElectionId, rpElectionId, latestDar, dacId);

            return null;
          });

      logAutoOpenTrigger(classification, dataset, latestDar);
    }
  }

  /** Checks if there is an open election for the given DAR and dataset. */
  private boolean hasOpenElection(DataAccessRequest dar, Dataset dataset) {
    Election existing =
        electionDAO.findLastElectionByReferenceIdDatasetIdAndType(
            dar.getReferenceId(), dataset.getDatasetId(), DATA_ACCESS.getValue());

    return existing != null
        && ElectionStatus.OPEN.getValue().equalsIgnoreCase(existing.getStatus());
  }

  /** Archives old elections for the given DAR and dataset. */
  private void archiveOldElections(DataAccessRequest dar, Dataset dataset) {
    List<Integer> oldElectionIds =
        electionDAO
            .findElectionsByReferenceIdAndDatasetId(dar.getReferenceId(), dataset.getDatasetId())
            .stream()
            .map(Election::getElectionId)
            .toList();

    if (!oldElectionIds.isEmpty()) {
      electionDAO.archiveElectionByIds(oldElectionIds, new Date());
    }
  }

  /** Creates standard votes for all users for the given elections. */
  @VisibleForTesting
  protected void createVotesForAllUsers(
      Set<User> users,
      int dataAccessElectionId,
      int rpElectionId,
      DataAccessRequest dar,
      Integer dacId) {

    for (User user : users) {
      createStandardVotes(dataAccessElectionId, rpElectionId, user);

      if (user.verifyDACRole(CHAIRPERSON, dacId)) {
        createChairpersonVotes(dataAccessElectionId, rpElectionId, user, dar);
      }
    }
  }

  /** Creates standard votes for the given elections. */
  private void createStandardVotes(int dataAccessElectionId, int rpElectionId, User user) {
    dacAutomationRuleService.createVoteForElection(
        dataAccessElectionId, user.getUserId(), VoteType.DAC);

    dacAutomationRuleService.createVoteForElection(rpElectionId, user.getUserId(), VoteType.DAC);
  }

  /** Creates chairperson votes for the given elections. */
  private void createChairpersonVotes(
      int dataAccessElectionId, int rpElectionId, User user, DataAccessRequest dar) {

    dacAutomationRuleService.createVoteForElection(
        dataAccessElectionId, user.getUserId(), VoteType.CHAIRPERSON);
    dacAutomationRuleService.createVoteForElection(
        rpElectionId, user.getUserId(), VoteType.CHAIRPERSON);

    dacAutomationRuleService.createVoteForElection(
        dataAccessElectionId, user.getUserId(), VoteType.FINAL);

    if (!dar.requiresManualReview()) {
      dacAutomationRuleService.createVoteForElection(
          dataAccessElectionId, user.getUserId(), VoteType.AGREEMENT);
    }
  }

  /** Logs the auto-open trigger event. */
  private void logAutoOpenTrigger(
      DacUserClassification classification, Dataset dataset, DataAccessRequest dar) {

    Integer autoOpenUserId = classification.autoOpenUserIds.get(dataset.getDacId());
    if (autoOpenUserId == null) return;

    User enablingUser = userDAO.findUserById(autoOpenUserId);
    if (enablingUser == null) return;

    logInfo(
        "Auto-open rule triggered by userId=%s for DAC=%s, datasetId=%s, DAR referenceId=%s"
            .formatted(
                enablingUser.getUserId(),
                dataset.getDacId(),
                dataset.getDatasetId(),
                dar.getReferenceId()));
  }

  /**
   * Notifies users for the given DACs and datasets.
   *
   * <p>When {@code isAutoOpen} is {@code true}, each user receives a single auto-open email; {@code
   * dacs} and {@code datasets} are not consulted. When {@code false}, per-user DAC/dataset
   * membership is resolved from {@code dacs} and {@code datasets} to build the email body.
   */
  @VisibleForTesting
  protected void notifyUsersForDacs(
      Set<User> users,
      Set<Dac> dacs,
      Set<Dataset> datasets,
      DataAccessRequest latestDar,
      DarCollection darCollection,
      String researcherName,
      boolean isAutoOpen)
      throws IOException, TemplateException {
    for (User user : users) {
      if (isAutoOpen) {
        // Send election notification for auto-open DACs
        if (latestDar.getProgressReport()) {
          sendProgressReportNewCollectionElectionMessage(List.of(user), darCollection.getDarCode());
        } else {
          sendDarNewCollectionElectionMessage(List.of(user), darCollection.getDarCode());
        }
      } else {
        // Send manual notification for manual DACs
        Map<String, List<String>> dacToDatasetsMap = new HashMap<>();
        List<Dac> userDacs = getMatchingDacs(user, dacs);
        for (Dac dac : userDacs) {
          List<String> datasetIdentifiers = getMatchingDatasets(dac, new ArrayList<>(datasets));
          dacToDatasetsMap.put(dac.getName(), datasetIdentifiers);
        }
        if (latestDar.getProgressReport()) {
          sendNewProgressReportRequestEmail(
              user,
              dacToDatasetsMap,
              researcherName,
              darCollection.getDarCode(),
              latestDar.getReferenceId());
        } else {
          sendNewDARRequestEmail(
              user, dacToDatasetsMap, researcherName, darCollection.getDarCode());
        }
      }
    }
  }

  private void notifySpecificSigningOfficialOfApprovalNeeded(
      DataAccessRequest dataAccessRequest, User researcher) throws TemplateException, IOException {
    String soEmail = dataAccessRequest.getData().getSigningOfficialEmail();
    User soUser = userDAO.findUserByEmail(soEmail);
    sendNewDARSigningOfficialRequestEmail(
        soUser, researcher.getDisplayName(), dataAccessRequest.getDarCode());
  }

  @VisibleForTesting
  protected void sendDarNewCollectionElectionMessage(List<User> users, String darCode)
      throws IOException, TemplateException {
    String electionType = "Data Access Request";
    for (User user : users) {
      emailService.sendMessage(new NewCaseMessage(user, darCode, electionType), user.getUserId());
    }
  }

  @VisibleForTesting
  protected void sendProgressReportNewCollectionElectionMessage(List<User> users, String darCode)
      throws IOException, TemplateException {
    for (User user : users) {
      emailService.sendMessage(new NewProgressReportCaseMessage(user, darCode), user.getUserId());
    }
  }

  @VisibleForTesting
  protected void sendNewDARRequestEmail(
      User user, Map<String, List<String>> dacDatasetMap, String researcherName, String darCode)
      throws TemplateException, IOException {
    emailService.sendMessage(
        new NewDARRequestMessage(user, darCode, dacDatasetMap, researcherName), user.getUserId());
  }

  @VisibleForTesting
  protected void sendNewDARSigningOfficialRequestEmail(
      User signingOfficial, String researcherName, String darCode)
      throws TemplateException, IOException {
    emailService.sendMessage(
        new NewDARSigningOfficialRequestMessage(signingOfficial, darCode, researcherName),
        signingOfficial.getUserId());
  }

  @VisibleForTesting
  protected void sendNewProgressReportRequestEmail(
      User user,
      Map<String, List<String>> dacDatasetMap,
      String researcherName,
      String darCode,
      String referenceId)
      throws TemplateException, IOException {
    emailService.sendMessage(
        new NewProgressReportRequestMessage(
            user, darCode, referenceId, dacDatasetMap, researcherName),
        user.getUserId());
  }

  @VisibleForTesting
  protected void sendNewSoProgressReportSubmittedEmail(
      User user, String darCode, User researcher, String referenceId, List<Dataset> datasets)
      throws TemplateException, IOException {
    emailService.sendMessage(
        new SoPRSubmitted(user, darCode, researcher, referenceId, datasets), user.getUserId());
  }

  @VisibleForTesting
  protected void sendNewSoDARSubmittedEmail(
      User user, String darCode, User researcher, String referenceId, List<Dataset> datasets)
      throws TemplateException, IOException {
    emailService.sendMessage(
        new SoDARSubmitted(user, darCode, researcher, referenceId, datasets), user.getUserId());
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
        sendNewSoProgressReportSubmittedEmail(
            so, darCode, researcher, dar.getReferenceId(), datasets);
      } else {
        sendNewSoDARSubmittedEmail(so, darCode, researcher, dar.getReferenceId(), datasets);
      }
    }
  }

  private void approveDataAccessRequestBySigningOfficial(
      User signingOfficial, DataAccessRequest dar, ContainerRequest request) {
    validateSigningOfficialApproval(signingOfficial, dar);
    dataAccessRequestDAO.updateDarApprovalSO(signingOfficial.getUserId(), dar.getReferenceId());
    User researcher = userDAO.findUserById(dar.getUserId());
    List<Integer> datasetIds = dar.getDatasetIds();
    if (!dar.getIsCloseoutProgressReport() && !dar.getHasDMI()) {
      dacAutomationRuleService.triggerDACRuleSettings(
          researcher, datasetIds, dar.getReferenceId(), request);
    }
  }

  private void validateSigningOfficialApproval(User signingOfficial, DataAccessRequest dar) {
    if (dar.getData() == null) {
      throw new ConsentConflictException(
          ("This data access request does not support this operation"));
    }
    String darSigningOfficialEmail = dar.data.getSigningOfficialEmail();
    if (dar.getApprovingSigningOfficialUserId() != null) {
      throw new BadRequestException("This data access request has already been approved");
    }
    if (signingOfficial.getEmail() == null
        || !signingOfficial.getEmail().equalsIgnoreCase(darSigningOfficialEmail)) {
      throw new ForbiddenException("You are not the Signing Official for this request.");
    }
  }

  private List<User> getDacUsersForDAR(DataAccessRequest dar) {
    return getDacUsersForDatasetIds(dar.getDatasetIds());
  }

  private List<User> getDacUsersForDatasetIds(List<Integer> datasetIds) {
    return new ArrayList<>(
        userDAO.findUsersForDatasetsByRole(
            datasetIds, List.of(UserRoles.CHAIRPERSON.getRoleId(), UserRoles.MEMBER.getRoleId())));
  }

  private List<Dac> getMatchingDacs(User user, Collection<Dac> dacsInDAR) {
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

  private record DarCollectionContext(
      DarCollection darCollection,
      DataAccessRequest latestDar,
      User researcher,
      String researcherName,
      DacUserClassification classification) {}

  /** Helper class to hold classification results for DACs, users, and datasets. */
  @VisibleForTesting
  static class DacUserClassification {
    Set<Dac> autoOpenDacs = new HashSet<>();
    Set<Dac> manualOpenDacs = new HashSet<>();
    Set<User> autoOpenUsers = new HashSet<>();
    Set<User> manualOpenUsers = new HashSet<>();
    Set<Dataset> autoOpenDatasets = new HashSet<>();
    Set<Dataset> manualOpenDatasets = new HashSet<>();
    Map<Integer, Integer> autoOpenUserIds = new HashMap<>();
  }
}
