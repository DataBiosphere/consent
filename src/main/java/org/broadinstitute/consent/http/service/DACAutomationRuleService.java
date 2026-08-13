package org.broadinstitute.consent.http.service;

import static java.util.Objects.isNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import org.broadinstitute.consent.http.db.DACAutomationRuleDAO;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.exceptions.ConsentConflictException;
import org.broadinstitute.consent.http.exceptions.UnprocessableEntityException;
import org.broadinstitute.consent.http.models.AutomationRuleToggleResponse;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.Vote;
import org.broadinstitute.consent.http.models.datause.DataUsePrimaryClassifier;
import org.broadinstitute.consent.http.rules.AuditPageResults;
import org.broadinstitute.consent.http.rules.DACAutomationRule;
import org.broadinstitute.consent.http.rules.DACAutomationRuleType;
import org.broadinstitute.consent.http.rules.RuleImplementationInterface;
import org.broadinstitute.consent.http.rules.Rules;
import org.broadinstitute.consent.http.service.dao.VoteServiceDAO;
import org.broadinstitute.consent.http.util.ComplianceLogger;
import org.broadinstitute.consent.http.util.ConsentLogger;
import org.broadinstitute.consent.http.util.CountryValidator;
import org.glassfish.jersey.server.ContainerRequest;
import org.jdbi.v3.core.Jdbi;

public class DACAutomationRuleService implements ConsentLogger {

  private final DataAccessRequestDAO dataAccessRequestDAO;
  private final DatasetDAO datasetDAO;
  private final DACAutomationRuleDAO ruleDAO;
  private final ElectionDAO electionDAO;
  private final UserDAO userDAO;
  private final VoteDAO voteDAO;
  private final VoteService voteService;
  private final VoteServiceDAO voteServiceDAO;
  private final ElasticSearchService elasticSearchService;
  private final ExecutorService executorService;

  /** Guards the queue and the running flag; see {@link #reindexDatasetsForRuleChange}. */
  private final Object reindexLock = new Object();

  /** DACs awaiting a reindex, in toggle order. A Set so a DAC queued twice is reindexed once. */
  private final Set<Integer> pendingDacIds = new LinkedHashSet<>();

  private boolean reindexRunning = false;

  @Inject
  public DACAutomationRuleService(
      Jdbi jdbi,
      VoteServiceDAO voteServiceDAO,
      VoteService voteService,
      ElasticSearchService elasticSearchService,
      ExecutorService executorService) {
    this.elasticSearchService = elasticSearchService;
    this.executorService = executorService;
    this.dataAccessRequestDAO = jdbi.onDemand(DataAccessRequestDAO.class);
    this.datasetDAO = jdbi.onDemand(DatasetDAO.class);
    this.ruleDAO = jdbi.onDemand(DACAutomationRuleDAO.class);
    this.electionDAO = jdbi.onDemand(ElectionDAO.class);
    this.userDAO = jdbi.onDemand(UserDAO.class);
    this.voteDAO = jdbi.onDemand(VoteDAO.class);
    this.voteService = voteService;
    this.voteServiceDAO = voteServiceDAO;
  }

  @VisibleForTesting
  protected static RuleImplementationInterface getRuleImplementation(DACAutomationRule rule) {
    DACAutomationRuleType type = rule.ruleType();
    return Rules.implementationList.stream()
        .filter(r -> r.getRuleType().equals(type))
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    String.format("No rule implementation found for type: %s", type)));
  }

  public List<DACAutomationRule> findAll() {
    return ruleDAO.findAll();
  }

  public List<DACAutomationRule> findAllByDacId(Integer dacId) {
    return ruleDAO.findAllDACAutomationRulesByDACId(dacId);
  }

  public AutomationRuleToggleResponse toggleRule(Integer dacId, Integer ruleId, User user)
      throws ConsentConflictException, UnprocessableEntityException {
    List<DACAutomationRule> dacRules = ruleDAO.findAllDACAutomationRulesByDACId(dacId);
    DACAutomationRule ruleBeingToggled =
        dacRules.stream()
            .filter(r -> Objects.equals(r.id(), ruleId))
            .findFirst()
            .orElseThrow(() -> new UnprocessableEntityException("Rule ID not found."));
    if (!isNull(ruleBeingToggled.enabledByUserId())) {
      ruleDAO.auditedDeleteDACRuleSetting(dacId, ruleId, user.getUserId());
      reindexDatasetsForRuleChange(dacId);
      return new AutomationRuleToggleResponse(ruleId, false, -1, null, null);
    }
    Instant insertTime = Instant.now();
    ruleDAO.auditedInsertDACRuleSetting(dacId, ruleId, user.getUserId(), insertTime);
    reindexDatasetsForRuleChange(dacId);
    return new AutomationRuleToggleResponse(
        ruleId, true, insertTime.toEpochMilli(), user.getDisplayName(), user.getEmail());
  }

  /**
   * Indexed datasets carry state derived from their DAC's automation rules, so a toggle leaves that
   * DAC's documents stale. Only they are reindexed — most of the corpus is external entries with no
   * DAC, which no rule change can affect. It runs off the request thread so toggle latency does not
   * track DAC size.
   *
   * <p>Queued FIFO, so DACs are reindexed in the order they were toggled. A DAC already queued is
   * not queued twice: the pending pass has not started and will read the newest state when it does.
   */
  private void reindexDatasetsForRuleChange(Integer dacId) {
    synchronized (reindexLock) {
      pendingDacIds.add(dacId);
      if (reindexRunning) {
        return;
      }
      reindexRunning = true;
    }
    try {
      executorService.submit(this::drainPendingReindexes);
    } catch (RuntimeException e) {
      // Released so a rejected submission does not leave every later toggle queueing behind a drain
      // that never runs. The queue keeps its entries, so the next toggle picks this one up.
      synchronized (reindexLock) {
        reindexRunning = false;
      }
      logException("Unable to schedule dataset reindex after DAC rule toggle", e);
    }
  }

  /**
   * Reindexes queued DACs until the queue is empty. The queue and {@code reindexRunning} are read
   * and written under {@code reindexLock}, so a toggle cannot have its DAC dropped by clearing it.
   */
  private void drainPendingReindexes() {
    boolean released = false;
    try {
      while (true) {
        Integer dacId;
        synchronized (reindexLock) {
          Iterator<Integer> queued = pendingDacIds.iterator();
          if (!queued.hasNext()) {
            reindexRunning = false;
            released = true;
            return;
          }
          dacId = queued.next();
          queued.remove();
        }
        reindexDatasetsForDac(dacId);
      }
    } finally {
      // Reached only when an Error escapes reindexDatasetsForDac, which catches every Exception.
      // The guard must still be released; the flag is local because another drain may own it by
      // now.
      if (!released) {
        synchronized (reindexLock) {
          reindexRunning = false;
        }
        logWarn("Dataset reindex after DAC rule toggle terminated unexpectedly");
      }
    }
  }

  /**
   * Failures are logged rather than raised: the toggle that triggered this is already committed and
   * audited, so nothing here may fail it, and the next reindex corrects the documents either way.
   */
  private void reindexDatasetsForDac(Integer dacId) {
    try {
      // Covers datasets carrying the DAC as a property as well as those assigned to it directly
      List<Integer> datasetIds =
          datasetDAO.findDatasetsAssociatedWithDac(dacId).stream()
              .map(Dataset::getDatasetId)
              .distinct()
              .toList();
      if (datasetIds.isEmpty()) {
        return;
      }
      try (Response response = elasticSearchService.indexDatasets(datasetIds)) {
        if (response.getStatus() >= 400) {
          logWarn(
              "Error reindexing datasets for DAC %d after rule toggle: status %d"
                  .formatted(dacId, response.getStatus()));
        }
      }
    } catch (Exception e) {
      logException("Unable to reindex datasets for DAC %d after rule toggle".formatted(dacId), e);
    }
  }

  public Integer removeChairpersonFromDAC(Integer dacId, Integer userId, Integer auditUserId) {
    return ruleDAO.auditedDeleteDACRuleSettingByUser(dacId, userId, auditUserId);
  }

  public Integer auditedRemoveChairpersonFromDAC(
      Integer dacId, Integer userId, Integer auditUserId) {
    return ruleDAO.auditedDeleteDACRuleSettingByUser(dacId, userId, auditUserId);
  }

  public Integer removeChairpersonUser(Integer userId, Integer auditUserId) {
    return ruleDAO.auditedDeleteAllDACRuleSettingForUser(userId, auditUserId);
  }

  public AuditPageResults findAuditRecords(Integer dacId, Integer pageSize, Integer page) {
    int realPage = page - 1;
    int offset = realPage * pageSize;
    return new AuditPageResults(
        ruleDAO.findAutomationAuditsForDac(dacId, pageSize, offset),
        ruleDAO.findCountOfAutomationAuditsForDac(dacId),
        pageSize,
        page);
  }

  public void triggerDACRuleSettings(
      User researcher, List<Integer> datasetIds, String referenceId, ContainerRequest request) {
    try {
      DataAccessRequest dar = dataAccessRequestDAO.findByReferenceId(referenceId);
      List<Vote> approvalVotes = new ArrayList<>();
      datasetIds.forEach(
          datasetId -> {
            Dataset dataset = datasetDAO.findDatasetById(datasetId);
            List<DACAutomationRule> rules =
                ruleDAO.findAllDACAutomationRulesByDACId(dataset.getDacId());
            rules.forEach(
                rule -> {
                  boolean isActive = rule.enabledByUserId() != null;
                  if (isActive) {
                    Optional<Vote> optionalVote = applyRule(rule, dataset, dar, request);
                    optionalVote.ifPresent(approvalVotes::add);
                  }
                });
          });

      if (!approvalVotes.isEmpty()) {
        voteService.sendDatasetApprovalNotifications(approvalVotes, researcher);
      }
    } catch (Exception e) {
      logWarn("Error triggering DAC Rule Settings", e);
    }
  }

  @VisibleForTesting
  protected Optional<Vote> applyRule(
      DACAutomationRule rule, Dataset dataset, DataAccessRequest dar, ContainerRequest request) {
    if (!DataUsePrimaryClassifier.hasCanonicalSinglePrimary(dataset.getDataUse())) {
      logInfo(
          String.format(
              "Rule %s not triggered for DAC id: %s and dataset id: %s because the dataset does not have a canonical single primary Data Use",
              rule.ruleType(), dataset.getDacId(), dataset.getDatasetId()));
      return Optional.empty();
    }
    RuleImplementationInterface ruleImplementation = getRuleImplementation(rule);
    boolean darContainsBannedCountry = CountryValidator.containsBannedCountry(dar);
    boolean shouldApprove = ruleImplementation.compare(dataset, dar);
    if (shouldApprove && !darContainsBannedCountry) {
      Vote v = openElectionAndApprove(rule, ruleImplementation, dar, dataset, request);
      if (v != null) {
        return Optional.of(v);
      }
    } else {
      logInfo(
          String.format(
              "Rule %s not triggered for DAC id: %s and dataset id: %s with contains banned country: %b",
              rule.ruleType(),
              dataset.getDacId(),
              dataset.getDatasetId(),
              darContainsBannedCountry));
    }
    return Optional.empty();
  }

  @VisibleForTesting
  protected Vote openElectionAndApprove(
      DACAutomationRule rule,
      RuleImplementationInterface ruleImplementation,
      DataAccessRequest dar,
      Dataset dataset,
      ContainerRequest request) {

    // Wrap in transaction to ensure election and vote are created together
    Vote vote =
        electionDAO.inTransaction(
            _ -> {
              int electionId = createOpenElectionForDAR(dar, dataset);
              int voteId =
                  createVoteForElection(electionId, rule.enabledByUserId(), VoteType.RADAR_APPROVE);
              return voteDAO.findVoteById(voteId);
            });

    try {
      List<Vote> updatedVotes =
          voteServiceDAO.updateVotesWithValue(
              List.of(vote),
              true,
              String.format(
                  "Rule Automated DAR (RADAR) Approval using rule: %s",
                  ruleImplementation.getRuleType()));
      assert (updatedVotes.size() == 1);
      vote = updatedVotes.getFirst();
    } catch (Exception e) {
      logException("Error updating vote", e);
      return null;
    }
    User user = userDAO.findUserById(rule.enabledByUserId());
    ComplianceLogger.logRadarApproval(user, List.of(dataset), request, 200);
    logInfo(
        String.format(
            "Rule %s triggered for DAC id: %s and dataset id: %s",
            rule.ruleType(), dataset.getDacId(), dataset.getDatasetId()));
    return vote;
  }

  protected int createOpenElectionForDAR(DataAccessRequest dar, Dataset dataset) {
    return electionDAO.insertElection(
        ElectionType.DATA_ACCESS.getValue(),
        ElectionStatus.OPEN.getValue(),
        new Date(),
        dar.getReferenceId(),
        dataset.getDatasetId());
  }

  protected int createVoteForElection(int electionId, int userId, VoteType voteType) {
    return voteDAO.insertVote(userId, electionId, voteType.getValue());
  }
}
