package org.broadinstitute.consent.http.rules;

import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.Dataset;

public class HealthMedicalBioMedicalWithDiseaseSpecificV1 implements RuleImplementationInterface {

  @Override
  public DACAutomationRuleType getRuleType() { return DACAutomationRuleType.HMB_DSV1;}

  @Override
  public boolean compare(Dataset dataset, DataAccessRequest dataAccessRequest) {
    return Boolean.TRUE.equals(dataset.getDataUse().getHmbResearch()) && requestIsHmbAndDiseases(dataAccessRequest.getData());
  }
}
