package org.broadinstitute.consent.http.service;

import static java.util.Objects.isNull;

import com.google.inject.Inject;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.broadinstitute.consent.http.db.DACAutomationRuleDAO;
import org.broadinstitute.consent.http.models.AutomationRuleToggleResponse;
import org.broadinstitute.consent.http.rules.DACAutomationRule;

public class DACAutomationRuleService {

  private final DACAutomationRuleDAO ruleDAO;

  @Inject
  public DACAutomationRuleService(DACAutomationRuleDAO ruleDAO) {
    this.ruleDAO = ruleDAO;
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
      ruleDAO.deleteDACRuleSetting(dacId, ruleId);
      return new AutomationRuleToggleResponse(ruleId, false);
    }
    ruleDAO.insertDACRuleSetting(dacId, ruleId, userId);
    return new AutomationRuleToggleResponse(ruleId, true);
  }

  public Integer removeChairpersonFromDAC(Integer dacId, Integer userId) {
    return ruleDAO.deleteDACRuleSettingByUser(dacId, userId);
  }

  public Integer removeChairpersonUser(Integer userId) {
    return ruleDAO.deleteAllDACRuleSettingForUser(userId);
  }

}
