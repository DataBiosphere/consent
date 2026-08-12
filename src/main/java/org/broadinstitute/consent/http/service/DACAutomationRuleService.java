package org.broadinstitute.consent.http.service;

import static java.util.Objects.isNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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

  /** Guards both reindex flags; see {@link #reindexDatasetsForRuleChange}. */
  private final Object reindexLock = new Object();

  private boolean reindexRunning = false;
  private boolean reindexPending = false;

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
      reindexDatasetsForRuleChange();
      return new AutomationRuleToggleResponse(ruleId, false, -1, null, null);
    }
    Instant insertTime = Instant.now();
    ruleDAO.auditedInsertDACRuleSetting(dacId, ruleId, user.getUserId(), insertTime);
    reindexDatasetsForRuleChange();
    return new AutomationRuleToggleResponse(
        ruleId, true, insertTime.toEpochMilli(), user.getDisplayName(), user.getEmail());
  }

  /**
   * Indexed datasets carry state derived from their DAC's automation rules — the Signing Official
   * approval model and instant-approval eligibility — so a toggle leaves those documents stale.
   *
   * <p>Reindexes the whole corpus rather than just the toggling DAC's datasets, matching {@code
   * POST /api/dataset/index}. Rule toggles are rare, and this keeps the reindex off the
   * unbounded-IN-list path a DAC with many datasets would otherwise take. It runs off the request
   * thread so toggle latency does not track corpus size.
   *
   * <p>Reindexes are coalesced: a toggle arriving while one is in flight does not start a second
   * pass over the whole corpus, it marks another pass as pending so exactly one more runs once the
   * current one finishes. Coalescing rather than skipping matters — the toggle that arrives last
   * carries the newest state, so dropping it would leave the index behind until something else
   * triggered a reindex.
   */
  private void reindexDatasetsForRuleChange() {
    synchronized (reindexLock) {
      reindexPending = true;
      if (reindexRunning) {
        return;
      }
      reindexRunning = true;
    }
    try {
      executorService.submit(this::drainPendingReindexes);
    } catch (RuntimeException e) {
      // Rejected submission (an executor shutting down) would otherwise leave reindexRunning set
      // with nothing draining it, so every later toggle would coalesce into a pass that never runs.
      // reindexPending stays true, so the next toggle that does schedule picks this one up.
      synchronized (reindexLock) {
        reindexRunning = false;
      }
      logException("Unable to schedule dataset reindex after DAC rule toggle", e);
    }
  }

  /**
   * Runs reindex passes until none is pending. Both flags are read and written under {@code
   * reindexLock}, so a toggle cannot observe {@code reindexRunning} as true moments before this
   * clears it and have its request dropped.
   */
  private void drainPendingReindexes() {
    while (true) {
      synchronized (reindexLock) {
        if (!reindexPending) {
          reindexRunning = false;
          return;
        }
        reindexPending = false;
      }
      reindexAllDatasets();
    }
  }

  /**
   * Failures are logged rather than raised: the toggle that triggered this is already committed and
   * audited, so nothing here may fail it, and the next reindex corrects the documents either way.
   */
  private void reindexAllDatasets() {
    try {
      List<Integer> datasetIds = datasetDAO.findAllDatasetIds();
      if (datasetIds.isEmpty()) {
        return;
      }
      try (Response response = elasticSearchService.indexDatasets(datasetIds)) {
        if (response.getStatus() >= 400) {
          logWarn(
              "Error reindexing datasets after DAC rule toggle: status %d"
                  .formatted(response.getStatus()));
        }
      }
    } catch (Exception e) {
      logException("Unable to reindex datasets after DAC rule toggle", e);
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
