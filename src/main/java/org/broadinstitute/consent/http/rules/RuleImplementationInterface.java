package org.broadinstitute.consent.http.rules;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.Dataset;

public interface RuleImplementationInterface {

  DACAutomationRuleType getRuleType();

  boolean compare(Dataset dataset, DataAccessRequest dataAccessRequest);

  default boolean secondaryConditionChecks(DataAccessRequestData data) {
    // Secondary condition checks, part 1
    if (Boolean.TRUE.equals(data.getControls())) {
      return false;
    }
    if (Boolean.TRUE.equals(data.getPopulation())) {
      return false;
    }
    if (Boolean.TRUE.equals(data.getForProfit())) {
      return false;
    }

    // Secondary condition checks, part 2
    if (!StringUtils.isBlank(data.getGender())) {
      return false;
    }
    if (Boolean.TRUE.equals(data.getPediatric())) {
      return false;
    }
    if (Boolean.TRUE.equals(data.getVulnerablePopulation())) {
      return false;
    }

    // Secondary condition checks, part 3
    if (Boolean.TRUE.equals(data.getIllegalBehavior())) {
      return false;
    }
    if (Boolean.TRUE.equals(data.getSexualDiseases())) {
      return false;
    }
    if (Boolean.TRUE.equals(data.getPsychiatricTraits())) {
      return false;
    }
    if (Boolean.TRUE.equals(data.getNotHealth())) {
      return false;
    }
    if (Boolean.TRUE.equals(data.getStigmatizedDiseases())) {
      return false;
    }
    if (Boolean.TRUE.equals(data.getAddiction())) {
      return false;
    }
    return true;
  }

  default boolean requestIsOnlyHMB(DataAccessRequestData data) {
    // Primary condition checks
    if (Boolean.TRUE.equals(data.getDiseases())) {
      return false;
    }
    if (!StringUtils.isBlank(data.getOtherText())) {
      return false;
    }
    if (Boolean.TRUE.equals(data.getOther())) {
      return false;
    }

    if (!secondaryConditionChecks(data)) {
      return false;
    }

    return Boolean.TRUE.equals(data.getHmb());
  }

  default boolean requestHasDiseases(DataAccessRequestData data) {
    if (!Boolean.TRUE.equals(data.getDiseases())) {
      return false;
    }

    if (!StringUtils.isBlank(data.getOtherText())) {
      return false;
    }
    if (Boolean.TRUE.equals(data.getOther())) {
      return false;
    }

    if (!secondaryConditionChecks(data)) {
      return false;
    }

    return Boolean.TRUE.equals(!data.getOntologies().isEmpty());
  }
}
