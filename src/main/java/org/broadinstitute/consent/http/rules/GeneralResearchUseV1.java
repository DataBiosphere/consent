package org.broadinstitute.consent.http.rules;

import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.Dataset;

public class GeneralResearchUseV1 implements RuleImplementationInterface {

  public boolean compare(Dataset dataset, DataAccessRequest dataAccessRequest) {
    return Boolean.TRUE.equals(dataset.getDataUse().getGeneralUse())
        && hasNoModifiers(dataset.getDataUse())
        && requestIsOnlyHMB(dataAccessRequest.getData());
  }

  public DACAutomationRuleType getRuleType() {
    return DACAutomationRuleType.GRU_V1;
  }

}
