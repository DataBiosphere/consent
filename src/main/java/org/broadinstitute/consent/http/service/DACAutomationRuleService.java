package org.broadinstitute.consent.http.service;

import static java.util.Objects.isNull;

import com.google.inject.Inject;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.broadinstitute.consent.http.db.DACAutomationRuleDAO;
import org.broadinstitute.consent.http.db.DataAccessRequestDAO;
import org.broadinstitute.consent.http.db.DatasetDAO;
import org.broadinstitute.consent.http.db.ElectionDAO;
import org.broadinstitute.consent.http.db.VoteDAO;
import org.broadinstitute.consent.http.enumeration.ElectionStatus;
import org.broadinstitute.consent.http.enumeration.ElectionType;
import org.broadinstitute.consent.http.enumeration.VoteType;
import org.broadinstitute.consent.http.models.AutomationRuleToggleResponse;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.Dataset;
import org.broadinstitute.consent.http.rules.AuditPageResults;
import org.broadinstitute.consent.http.rules.DACAutomationRule;
import org.broadinstitute.consent.http.rules.DACAutomationRuleType;
import org.broadinstitute.consent.http.rules.RuleImplementationInterface;
import org.broadinstitute.consent.http.rules.Rules;

public class DACAutomationRuleService {

  private final DataAccessRequestDAO dataAccessRequestDAO;
  private final DatasetDAO datasetDAO;
  private final DACAutomationRuleDAO ruleDAO;
  private final ElectionDAO electionDAO;
  private final VoteDAO voteDAO;

  @Inject
  public DACAutomationRuleService(DataAccessRequestDAO dataAccessRequestDAO, DatasetDAO datasetDAO, DACAutomationRuleDAO ruleDAO, ElectionDAO electionDAO, VoteDAO voteDAO) {
    this.dataAccessRequestDAO = dataAccessRequestDAO;
    this.datasetDAO = datasetDAO;
    this.ruleDAO = ruleDAO;
    this.electionDAO = electionDAO;
    this.voteDAO = voteDAO;
  }

  public List<DACAutomationRule> findAll() {
    return ruleDAO.findAll();
  }

  public List<DACAutomationRule> findAllByDacId(Integer dacId) {
    return ruleDAO.findAllDACAutomationRulesByDACId(dacId);
  }

  public AutomationRuleToggleResponse toggleRule(Integer dacId, Integer ruleId, Integer userId) {
    Optional<DACAutomationRule> matchingRule = ruleDAO.findAllDACAutomationRulesByDACId(dacId)
        .stream().filter(r -> Objects.equals(r.id(),
            ruleId) && !isNull(r.enabledByUserId())).findFirst();
    if (matchingRule.isPresent()) {
      ruleDAO.auditedDeleteDACRuleSetting(dacId, ruleId, userId);
      return new AutomationRuleToggleResponse(ruleId, false);
    }
    ruleDAO.auditedInsertDACRuleSetting(dacId, ruleId, userId);
    return new AutomationRuleToggleResponse(ruleId, true);
  }

  public Integer removeChairpersonFromDAC(Integer dacId, Integer userId, Integer auditUserId) {
    return ruleDAO.auditedDeleteDACRuleSettingByUser(dacId, userId, auditUserId);
  }

  public Integer auditedRemoveChairpersonFromDAC(Integer dacId, Integer userId, Integer auditUserId) {
    return ruleDAO.auditedDeleteDACRuleSettingByUser(dacId, userId, auditUserId);
  }

  public Integer removeChairpersonUser(Integer userId, Integer auditUserId) {
    return ruleDAO.auditedDeleteAllDACRuleSettingForUser(userId, auditUserId);
  }

  public AuditPageResults findAuditRecords(Integer dacId, Integer pageSize, Integer page) {
    int offset = page * pageSize;
    return new AuditPageResults(ruleDAO.findAutomationAuditsForDac(dacId, pageSize, offset),
        ruleDAO.findCountOfAutomationAuditsForDac(dacId),pageSize, page);
  }

  public void triggerDACRuleSettings(List<Integer> datasetIds, String referenceId) {
    DataAccessRequest dar = dataAccessRequestDAO.findByReferenceId(referenceId);
    datasetIds.forEach(datasetId -> {
      Dataset dataset = datasetDAO.findDatasetById(datasetId);
      List<DACAutomationRule> rules = ruleDAO.findAllDACAutomationRulesByDACId(dataset.getDacId());
      rules.forEach(rule -> {
        boolean isActive = rule.enabledByUserId() != null;
        if (isActive) {
          DACAutomationRuleType type = rule.ruleType();
          List<RuleImplementationInterface> ruleImplementations = Rules.implementationList.stream().filter(r -> r.getRuleType().equals(type)).toList();
          ruleImplementations.forEach(ruleImplementation -> {
            boolean shouldApprove = ruleImplementation.compare(dataset, dar);
            if (shouldApprove) {
              // TODO _ Check on new election type ...
              int electionId = electionDAO.insertElection(ElectionType.DATA_ACCESS.getValue(), ElectionStatus.OPEN.getValue(), new Date(),  dar.getReferenceId(), datasetId);
              int voteId = voteDAO.insertVote(rule.enabledByUserId(), electionId, VoteType.FINAL.getValue());
              // TODO: bug in that this changes the create date
              voteDAO.updateVote(true, "DAC Bot", new Date(), voteId, false, electionId, new Date(), false);
              // TODO: Add emails
            }
          });
        }
      });
    });
  }
}
