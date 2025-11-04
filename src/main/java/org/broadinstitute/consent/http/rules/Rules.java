package org.broadinstitute.consent.http.rules;

import java.util.List;

public class Rules {

  public static List<RuleImplementationInterface> implementationList =
      List.of(
          new GeneralResearchUseV1(), new GeneralResearchUseWithDiseaseSpecificV1(),
          new HealthMedicalBioMedicalV1(), new HealthMedicalBioMedicalWithDiseaseSpecificV1());
}
