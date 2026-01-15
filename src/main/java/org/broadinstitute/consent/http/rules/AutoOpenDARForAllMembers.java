package org.broadinstitute.consent.http.rules;

import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.Dataset;

public class AutoOpenDARForAllMembers implements RuleImplementationInterface {

  @Override
  public DACAutomationRuleType getRuleType() {
    return DACAutomationRuleType.AUTO_OPEN_DAR_FOR_ALL_MEMBERS;
  }

  @Override
  public boolean compare(Dataset dataset, DataAccessRequest dataAccessRequest) {
    return false;
  }
}
