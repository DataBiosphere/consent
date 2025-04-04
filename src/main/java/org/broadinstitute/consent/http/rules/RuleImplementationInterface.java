package org.broadinstitute.consent.http.rules;

import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.Dataset;

public interface RuleImplementationInterface {

  DACAutomationRuleType getRuleType();

  boolean compare(Dataset dataset, DataAccessRequest dataAccessRequest);

}
