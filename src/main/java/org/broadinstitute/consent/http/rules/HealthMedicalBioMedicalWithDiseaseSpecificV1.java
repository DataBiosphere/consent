package org.broadinstitute.consent.http.rules;

import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.Dataset;

public class HealthMedicalBioMedicalWithDiseaseSpecificV1 implements RuleImplementationInterface {

  @Override
  public DACAutomationRuleType getRuleType() {
    return DACAutomationRuleType.HMB_DSV1;
  }

  @Override
  public boolean compare(Dataset dataset, DataAccessRequest dataAccessRequest) {
    return datasetQualifies(dataset) && requestHasDiseases(dataAccessRequest.getData());
  }

  @Override
  public boolean datasetQualifies(Dataset dataset) {
    return datasetIsUnmodifiedHmbResearch(dataset);
  }
}
