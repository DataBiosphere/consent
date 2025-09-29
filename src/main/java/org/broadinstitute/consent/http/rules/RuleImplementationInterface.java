package org.broadinstitute.consent.http.rules;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.Dataset;

public interface RuleImplementationInterface {

  DACAutomationRuleType getRuleType();

  boolean compare(Dataset dataset, DataAccessRequest dataAccessRequest);
  default boolean hasNoModifiers(DataUse data) {
    if (Boolean.TRUE.equals(data.getCollaboratorRequired())) {
      return false;
    }
    if (Boolean.TRUE.equals(data.getSexualDiseases())) {
      return false;
    }
    if (Boolean.TRUE.equals(data.getEthicsApprovalRequired())) {
      return false;
    }
    if (Boolean.TRUE.equals(data.getMethodsResearch())) {
      return false;
    }
    if (Boolean.TRUE.equals(data.getNonProfitUse())) {
      return false;
    }
    if (Boolean.TRUE.equals(data.getGeneticStudiesOnly())) {
      return false;
    }
    if (Boolean.TRUE.equals(data.getPopulationOriginsAncestry())) {
      return false;
    }
    if (Boolean.TRUE.equals(data.getPublicationResults())) {
      return false;
    }
    if (Boolean.TRUE.equals(data.getIllegalBehavior())) {
      return false;
    }
    if (Boolean.TRUE.equals(data.getAiLlmUse())) {
      return false;
    }
    if (Boolean.TRUE.equals(data.getControls())) {
      return false;
    }
    if (Boolean.TRUE.equals(data.getNotHealth())) {
      return false;
    }
    if (Boolean.TRUE.equals(data.getPopulation())) {
      return false;
    }
    if (Boolean.TRUE.equals(data.getPediatric())) {
      return false;
    }
    if (Boolean.TRUE.equals(data.getPsychologicalTraits())) {
      return false;
    }
    if (Boolean.TRUE.equals(data.getStigmatizeDiseases())) {
      return false;
    }
    if (Boolean.TRUE.equals(data.getVulnerablePopulations())) {
      return false;
    }
    if (!Boolean.TRUE.equals(data.getDiseaseRestrictions() == (null))
        && !Boolean.TRUE.equals(data.getDiseaseRestrictions().isEmpty())) {
      return false;
    }
    if (Boolean.TRUE.equals(!StringUtils.isBlank(data.getPublicationMoratorium()))) {
      return false;
    }
    if (Boolean.TRUE.equals(!StringUtils.isBlank(data.getOther()))) {
      return false;
    }
    if (Boolean.TRUE.equals(!StringUtils.isBlank(data.getGeographicalRestrictions()))) {
      return false;
    }
    if (Boolean.TRUE.equals(!StringUtils.isBlank(data.getSecondaryOther()))) {
      return false;
    }
    return !Boolean.TRUE.equals(!StringUtils.isBlank(data.getGender()));
  }

  default boolean secondaryConditionChecks(DataAccessRequestData data) {
    // Secondary condition checks, part 1
    if (Boolean.TRUE.equals(data.getAiLlmUse())) {
      return false;
    }
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
