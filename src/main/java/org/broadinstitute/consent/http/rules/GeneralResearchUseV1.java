package org.broadinstitute.consent.http.rules;

import org.broadinstitute.consent.http.models.DataAccessRequest;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
import org.broadinstitute.consent.http.models.Dataset;

public class GeneralResearchUseV1 {

  public boolean compare(Dataset dataset, DataAccessRequest dataAccessRequest) {
    return Boolean.TRUE.equals(dataset.getDataUse().getGeneralUse()) && isOnlyHMB(dataAccessRequest.getData());
  }

  private boolean isOnlyHMB(DataAccessRequestData data) {
    // Primary condition checks
    if (Boolean.TRUE.equals(data.getDiseases())) return false;
    if (data.getOtherText() != null) return false;
    if (Boolean.TRUE.equals(data.getOther())) return false;

    // Secondary condition checks, part 1
    if (Boolean.TRUE.equals(data.getControls())) return false;
    if (Boolean.TRUE.equals(data.getPopulation())) return false;
    if (Boolean.TRUE.equals(data.getForProfit())) return false;

    // Secondary condition checks, part 2
    if (data.getGender() != null) return false;
    if (Boolean.TRUE.equals(data.getPediatric())) return false;
    if (Boolean.TRUE.equals(data.getVulnerablePopulation())) return false;

    // Secondary condition checks, part 3
    if (Boolean.TRUE.equals(data.getIllegalBehavior())) return false;
    if (Boolean.TRUE.equals(data.getSexualDiseases())) return false;
    if (Boolean.TRUE.equals(data.getPsychiatricTraits())) return false;
    if (Boolean.TRUE.equals(data.getNotHealth())) return false;
    if (Boolean.TRUE.equals(data.getStigmatizedDiseases())) return false;
    if (Boolean.TRUE.equals(data.getAddiction())) return false;

    return Boolean.TRUE.equals(data.getHmb());
  }

}
