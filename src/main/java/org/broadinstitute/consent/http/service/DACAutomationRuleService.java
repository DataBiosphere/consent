package org.broadinstitute.consent.http.service;

import static java.util.Objects.isNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import freemarker.template.TemplateException;
import jakarta.ws.rs.InternalServerErrorException;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.broadinstitute.consent.http.db.DACAutomationRuleDAO;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.UserDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.enumeration.DataUseTranslationType;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.AutomationRuleToggleResponse;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.models.Vote;
import org.broadinstitute.consent.http.models.dto.DatasetMailDTO;
import org.broadinstitute.consent.http.rules.AuditPageResults;
import org.broadinstitute.consent.http.rules.DACAutomationRule;
import org.broadinstitute.consent.http.rules.DACAutomationRuleType;
import org.broadinstitute.consent.http.rules.RuleImplementationInterface;
import org.broadinstitute.consent.http.rules.Rules;
import org.broadinstitute.consent.http.service.dao.VoteServiceDAO;
import org.broadinstitute.consent.http.util.ConsentLogger;

public class DACAutomationRuleService implements ConsentLogger {

  private final DataAccessRequestDAO dataAccessRequestDAO;
  private final DatasetDAO datasetDAO;
  private final DACAutomationRuleDAO ruleDAO;
  private final ElectionDAO electionDAO;
  private final VoteDAO voteDAO;
  private final VoteService voteService;
  private final VoteServiceDAO voteServiceDAO;
  private final EmailService emailService;
  private final UseRestrictionConverter useRestrictionConverter;

  @Inject
  public DACAutomationRuleService(DataAccessRequestDAO dataAccessRequestDAO, DatasetDAO datasetDAO,
      DACAutomationRuleDAO ruleDAO, ElectionDAO electionDAO, VoteDAO voteDAO,
      VoteServiceDAO voteServiceDAO, EmailService emailService, VoteService voteService,
      UseRestrictionConverter useRestrictionConverter) {
    this.dataAccessRequestDAO = dataAccessRequestDAO;
    this.datasetDAO = datasetDAO;
    this.ruleDAO = ruleDAO;
    this.electionDAO = electionDAO;
    this.voteDAO = voteDAO;
    this.voteService = voteService;
    this.voteServiceDAO = voteServiceDAO;
    this.emailService = emailService;
    this.useRestrictionConverter = useRestrictionConverter;
  }

  @VisibleForTesting
  protected static RuleImplementationInterface getRuleImplementation(
      DACAutomationRule rule) {
    DACAutomationRuleType type = rule.ruleType();
    return Rules.implementationList.stream()
        .filter(r -> r.getRuleType().equals(type))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException(
            String.format("No rule implementation found for type: %s", type)));
  }

  public List<DACAutomationRule> findAll() {
    return ruleDAO.findAll();
  }

  public List<DACAutomationRule> findAllByDacId(Integer dacId) {
    return ruleDAO.findAllDACAutomationRulesByDACId(dacId);
  }

  public AutomationRuleToggleResponse toggleRule(Integer dacId, Integer ruleId, User user) {
    Optional<DACAutomationRule> matchingRule = ruleDAO.findAllDACAutomationRulesByDACId(dacId)
        .stream().filter(r -> Objects.equals(r.id(),
            ruleId) && !isNull(r.enabledByUserId())).findFirst();
    if (matchingRule.isPresent()) {
      ruleDAO.auditedDeleteDACRuleSetting(dacId, ruleId, user.getUserId());
      return new AutomationRuleToggleResponse(ruleId, false, -1, null, null);
    }
    Instant insertTime = Instant.now();
    ruleDAO.auditedInsertDACRuleSetting(dacId, ruleId, user.getUserId(), insertTime);
    return new AutomationRuleToggleResponse(ruleId, true, insertTime.toEpochMilli(),
        user.getDisplayName(), user.getEmail());
  }

  public Integer removeChairpersonFromDAC(Integer dacId, Integer userId, Integer auditUserId) {
    return ruleDAO.auditedDeleteDACRuleSettingByUser(dacId, userId, auditUserId);
  }

  public Integer auditedRemoveChairpersonFromDAC(Integer dacId, Integer userId,
      Integer auditUserId) {
    return ruleDAO.auditedDeleteDACRuleSettingByUser(dacId, userId, auditUserId);
  }

  public Integer removeChairpersonUser(Integer userId, Integer auditUserId) {
    return ruleDAO.auditedDeleteAllDACRuleSettingForUser(userId, auditUserId);
  }

  public AuditPageResults findAuditRecords(Integer dacId, Integer pageSize, Integer page) {
    int realPage = page - 1;
    int offset = realPage * pageSize;
    return new AuditPageResults(ruleDAO.findAutomationAuditsForDac(dacId, pageSize, offset),
        ruleDAO.findCountOfAutomationAuditsForDac(dacId), pageSize, page);
  }

  public void triggerDACRuleSettings(User researcher, List<Integer> datasetIds, String referenceId) {
    DataAccessRequest dar = dataAccessRequestDAO.findByReferenceId(referenceId);
    List<Vote> approvalVotes = new ArrayList<>();
    datasetIds.forEach(datasetId -> {
      Dataset dataset = datasetDAO.findDatasetById(datasetId);
      List<DACAutomationRule> rules = ruleDAO.findAllDACAutomationRulesByDACId(dataset.getDacId());
      rules.forEach(rule -> {
        boolean isActive = rule.enabledByUserId() != null;
        if (isActive) {
          Optional<Vote> optionalVote = applyRule(rule, dataset, dar);
          optionalVote.ifPresent(approvalVotes::add);
        }
      });
    });

    if (!approvalVotes.isEmpty()) {
      voteService.sendDatasetApprovalNotifications(approvalVotes, researcher);
    }
  }

  @VisibleForTesting
  protected Optional<Vote> applyRule(DACAutomationRule rule, Dataset dataset, DataAccessRequest dar) {
    RuleImplementationInterface ruleImplementation = getRuleImplementation(rule);
    boolean shouldApprove = ruleImplementation.compare(dataset, dar);
    if (shouldApprove) {
      Vote v = openElectionAndApprove(rule, ruleImplementation, dar, dataset);
      if (v != null) {
        return Optional.of(v);
      }
    } else {
      logInfo(String.format("Rule %s not triggered for DAC id: %s and dataset id: %s", rule.ruleType(), dataset.getDacId(),
          dataset.getDatasetId()));
    }
    return Optional.empty();
  }

  @VisibleForTesting
  protected Vote openElectionAndApprove(DACAutomationRule rule, RuleImplementationInterface ruleImplementation,
      DataAccessRequest dar, Dataset dataset) {
    int electionId = electionDAO.insertElection(ElectionType.DATA_ACCESS.getValue(),
        ElectionStatus.OPEN.getValue(), new Date(), dar.getReferenceId(), dataset.getDatasetId());
    int voteId = voteDAO.insertVote(rule.enabledByUserId(), electionId,
        VoteType.DACBOTAPPROVE.getValue());
    Vote vote = voteDAO.findVoteById(voteId);
    try {
      voteServiceDAO.updateVotesWithValue(List.of(vote), true, String.format("DACBot Approval using rule: %s", ruleImplementation.getRuleType()));
    } catch (SQLException e) {
      logException("Error updating vote", e);
      return null;
    }
    // TODO: Add better logging
    logInfo(String.format("Rule %s triggered for DAC id: %s and dataset id: %s", rule.ruleType(), dataset.getDacId(),
        dataset.getDatasetId()));
    return vote;
  }
}
