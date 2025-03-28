package org.broadinstitute.consent.http.service;

import com.google.inject.Inject;
import java.util.List;
import org.broadinstitute.consent.http.db.DACAutomationRuleDAO;
import org.broadinstitute.consent.http.rules.DACAutomationRule;

public class DACAutomationRuleService {

  private DACAutomationRuleDAO ruleDAO;

  @Inject
  public DACAutomationRuleService(DACAutomationRuleDAO ruleDAO) {
    this.ruleDAO = ruleDAO;
  }

  public List<DACAutomationRule> findAll() {
    return ruleDAO.findAll();
  }

  public List<DACAutomationRule> findAllAvailable() {
    return ruleDAO.findAllAvailable();
  }

}
