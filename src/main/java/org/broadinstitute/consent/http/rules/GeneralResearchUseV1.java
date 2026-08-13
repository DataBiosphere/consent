package org.broadinstitute.consent.http.rules;

import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.Dataset;

public class GeneralResearchUseV1 implements RuleImplementationInterface {

  public boolean compare(Dataset dataset, DataAccessRequest dataAccessRequest) {
    return datasetQualifies(dataset) && requestIsOnlyHMB(dataAccessRequest.getData());
  }

  @Override
  public boolean datasetQualifies(Dataset dataset) {
    return datasetIsUnmodifiedGeneralUse(dataset);
  }

  public DACAutomationRuleType getRuleType() {
    return DACAutomationRuleType.GRU_V1;
  }
}
