package org.broadinstitute.consent.http.matching;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.matching.DataUseMatchResultType;

public class DataUseMatchCasesV4 {

  private DataUseMatchCasesV4() {
    /* This utility class should not be instantiated */
  }

  // rationale for failures
  private static final String HMB_F1 =
      "The GRU Research Purpose does not match the HMB data use limitations.";
  private static final String HMB_F2 =
      "The HMB Research Purpose does not match the Disease-Specific data use limitations.";
  private static final String HMB_F3 =
      "The HMB Research Purpose does not match the POA data use limitations.";
  private static final String DS_F2 =
      "The Disease-Specific: %s Research Purpose is not a valid subclass of the Disease-Specific data use limitations.";
  private static final String MDS_F1 =
      "The Methods development Research Purpose does not match the Disease-Specific data use limitations.";
  private static final String POA_F1 =
      "The Populations, Origins, Ancestry Research Purpose does not match the HMB or Disease-Specific data use limitation.";
  private static final String NCU_F1 =
      "The For-Profit Research Purpose does not match the Non-Profit Use (NPU) only data use limitation.";

  // rationale for approvals - based on dataset terms
  private static final String DS_APPROVE_GRU =
      "The proposed disease-specific research is within the bounds of the general research use permissions of the dataset(s)";
  private static final String DS_APPROVE_HMB =
      "The proposed disease-specific research is within the bounds of the health, medical, biomedical use permissions of the dataset(s)";
  private static final String HMB_APPROVE_HMB =
      "The proposed health, medical, biomedical research is within the bounds of the health, medical, biomedical use permissions of the dataset(s)";
  private static final String HMB_APPROVE_GRU =
      "The proposed health, medical, biomedical research is within the bounds of the general research use permissions of the dataset(s)";
  private static final String POA_APPROVE_GRU =
      "The proposed population, origins, and/or ancestry research is within the bounds of the general research use permissions of the dataset(s)";
  private static final String POA_APPROVE_POA =
      "The proposed population, origins, and/or ancestry research is within the bounds of the population, origins, and/or ancestry use permissions of the dataset(s)";
  private static final String MDS_APPROVE =
      "Methods research is permitted on controlled-access data so long as it is not expressly prohibited";

  // rationale for abstain cases
  private static final String ABSTAIN = "The Research Purpose does not result in DUOS Decision.";

  /**
   * RP: Disease Focused Research Future use is limited to research involving the following disease
   * area(s) [DS] Datasets: Any dataset with GRU=true Any dataset with HMB=true Any dataset tagged
   * to this disease exactly Any dataset tagged to a DOID ontology Parent of disease X Denied
   * Datasets: Any dataset NOT the DS- or a subclass
   *
   * @param purpose The data use object representing the Research Purpose
   * @param dataset The data use object representing the Dataset
   * @param purposeDiseaseIdMap is a map of each purpose term id to a list of that term's parent
   *     term ids
   */
  static MatchResult matchDiseases(
      DataUse purpose, DataUse dataset, Map<String, List<String>> purposeDiseaseIdMap) {

    boolean purposeDSX = getNullableOrFalse(!purpose.getDiseaseRestrictions().isEmpty());
    boolean datasetDSX = getNullableOrFalse(!dataset.getDiseaseRestrictions().isEmpty());
    boolean datasetGRU = getNullableOrFalse(dataset.getGeneralUse());
    boolean datasetHMB = getNullableOrFalse(dataset.getHmbResearch());

    // short-circuit if no disease focused research
    if (!purposeDSX && !datasetDSX) {
      return MatchResult.from(DataUseMatchResultType.APPROVE, Collections.emptyList());
    }
    // Approve dataset GRU condition
    if (purposeDSX && datasetGRU) {
      return MatchResult.from(
          DataUseMatchResultType.APPROVE, Collections.singletonList(DS_APPROVE_GRU));
    }
    // Approve dataset HMB condition
    if (purposeDSX && datasetHMB) {
      return MatchResult.from(
          DataUseMatchResultType.APPROVE, Collections.singletonList(DS_APPROVE_HMB));
    }

    // We want all-purpose disease IDs to be a subclass of any dataset disease ID
    List<String> failures = new ArrayList<>();
    for (Map.Entry<String, List<String>> entry : purposeDiseaseIdMap.entrySet()) {
      boolean match =
          entry.getValue().stream().anyMatch(dataset.getDiseaseRestrictions()::contains);
      if (!match) {
        failures.add(String.format(DS_F2, entry.getKey()));
      }
    }

    return MatchResult.from(
        failures.isEmpty() ? DataUseMatchResultType.APPROVE : DataUseMatchResultType.DENY,
        failures);
  }

  /**
   * RP: HMB Approved Datasets: Any dataset tagged with GRU Any dataset tagged with HMB Denied
   * Datasets: Any dataset tagged with DS- Any dataset tagged with POA
   *
   * @param purpose The data use object representing the Research Purpose
   * @param dataset The data use object representing the Dataset
   */
  static MatchResult matchHMB(DataUse purpose, DataUse dataset) {
    // short-circuit hmb if not set
    if (Objects.isNull(purpose.getHmbResearch()) && (Objects.isNull(dataset.getHmbResearch()))) {
      return MatchResult.from(DataUseMatchResultType.APPROVE, Collections.emptyList());
    }

    List<String> failures = new ArrayList<>();
    boolean purposeGRU = getNullableOrFalse(purpose.getGeneralUse());
    boolean purposeHMB = getNullableOrFalse(purpose.getHmbResearch());
    boolean datasetGRU = getNullableOrFalse(dataset.getGeneralUse());
    boolean datasetHMB = getNullableOrFalse(dataset.getHmbResearch());
    boolean datasetDSX = getNullableOrFalse(!dataset.getDiseaseRestrictions().isEmpty());
    boolean datasetPOA = getNullableOrFalse(dataset.getPopulationOriginsAncestry());

    // Deny purpose GRU condition
    if (datasetHMB && purposeGRU) {
      failures.add(HMB_F1);
    }

    // Deny dataset DS- condition
    if (purposeHMB && datasetDSX) {
      failures.add(HMB_F2);
    }

    // Deny dataset POA condition
    if (purposeHMB && datasetPOA) {
      failures.add(HMB_F3);
    }

    // Approve dataset GRU condition
    if (datasetGRU) {
      return MatchResult.from(
          DataUseMatchResultType.APPROVE, Collections.singletonList(HMB_APPROVE_GRU));
    }

    // Approve dataset HMB condition
    if (purposeHMB && datasetHMB) {
      return MatchResult.from(
          DataUseMatchResultType.APPROVE, Collections.singletonList(HMB_APPROVE_HMB));
    } else {

      return MatchResult.from(
          failures.isEmpty() ? DataUseMatchResultType.APPROVE : DataUseMatchResultType.DENY,
          failures);
    }
  }

  /**
   * RP: Study population origins or ancestry Future use is limited to research involving a specific
   * population [POA] Approved Datasets: Any dataset tagged with GRU Any dataset tagged with POA
   * Denied Datasets: Any dataset tagged with DS- Any dataset tagged with HMB
   */
  static MatchResult matchPOA(DataUse purpose, DataUse dataset) {
    // short-circuit if no POA clause
    if (Objects.isNull(purpose.getPopulationOriginsAncestry())) {
      return MatchResult.from(DataUseMatchResultType.APPROVE, Collections.emptyList());
    }

    boolean purposePOA = getNullableOrFalse(purpose.getPopulationOriginsAncestry());
    boolean datasetPOA = getNullableOrFalse(dataset.getPopulationOriginsAncestry());
    boolean datasetGRU = getNullableOrFalse(dataset.getGeneralUse());

    // Approve dataset GRU condition
    if (datasetGRU) {
      return MatchResult.from(
          DataUseMatchResultType.APPROVE, Collections.singletonList(POA_APPROVE_GRU));
    }

    // Approve dataset POA condition
    if (purposePOA && datasetPOA) {
      return MatchResult.from(
          DataUseMatchResultType.APPROVE, Collections.singletonList(POA_APPROVE_POA));
    }

    List<String> failures = new ArrayList<>();
    if (!(purpose.getPopulationOriginsAncestry() && getNullableOrFalse(dataset.getGeneralUse()))) {
      failures.add(POA_F1);
    }

    return MatchResult.from(DataUseMatchResultType.DENY, failures);
  }

  /**
   * RP: Methods development/Validation study Future use for methods research
   * (analytic/software/technology development) outside the bounds of the other specified
   * restrictions Approved Datasets: Any dataset where GRU = true Any dataset where HMB = true Any
   * dataset where POA = true Any dataset where DS-X match
   */
  static MatchResult matchMDS(
      DataUse purpose, DataUse dataset, DataUseMatchResultType diseaseMatch) {
    // short-circuit if no disease focused research
    if (purpose.getDiseaseRestrictions().isEmpty() && dataset.getDiseaseRestrictions().isEmpty()) {
      return MatchResult.from(DataUseMatchResultType.APPROVE, Collections.emptyList());
    }

    // short-circuit if no MDS clause or if MDS is false
    if ((Objects.isNull(purpose.getMethodsResearch())) || (!purpose.getMethodsResearch())) {
      return MatchResult.from(DataUseMatchResultType.APPROVE, Collections.emptyList());
    }

    boolean purposeMDS = getNullableOrFalse(purpose.getMethodsResearch());
    boolean datasetGRU = getNullableOrFalse(dataset.getGeneralUse());
    boolean datasetHMB = getNullableOrFalse(dataset.getHmbResearch());
    boolean datasetPOA = getNullableOrFalse(dataset.getPopulationOriginsAncestry());

    // Approve dataset GRU condition
    if (datasetGRU) {
      return MatchResult.from(
          DataUseMatchResultType.APPROVE, Collections.singletonList(MDS_APPROVE));
    }

    // Approve dataset HMB condition
    if (purposeMDS && datasetHMB) {
      return MatchResult.from(
          DataUseMatchResultType.APPROVE, Collections.singletonList(MDS_APPROVE));
    }

    // Approve dataset POA condition
    if (purposeMDS && datasetPOA) {
      return MatchResult.from(
          DataUseMatchResultType.APPROVE, Collections.singletonList(MDS_APPROVE));
    }

    // short-circuit if there is a disease match
    if (diseaseMatch == DataUseMatchResultType.APPROVE) {
      return MatchResult.from(
          DataUseMatchResultType.APPROVE, Collections.singletonList(MDS_APPROVE));
    } else {
      return MatchResult.from(DataUseMatchResultType.DENY, Collections.singletonList(MDS_F1));
    }
  }

  /**
   * RP: Non-profit Use [NPU]=true Approved Datasets: Any dataset where [NPU]= true OR false When a
   * dataset [NPU] == false, that means there are effectively NO NPU restrictions on it. That means
   * that any purpose [NPU] == true|false should be approved. However, if the RP is [NPU]=false and
   * the dataset is NPU=true, then it should be denied.
   */
  static MatchResult matchNonProfitUse(DataUse purpose, DataUse dataset) {
    // short-circuit if no non-profit clauses
    if (dataset.getNonProfitUse() == null && purpose.getNonProfitUse() == null) {
      return MatchResult.from(DataUseMatchResultType.APPROVE, Collections.emptyList());
    }

    boolean purposeNpu = getNullableOrFalse(purpose.getNonProfitUse());
    boolean datasetNPU = getNullableOrFalse(dataset.getNonProfitUse());
    List<String> failures = new ArrayList<>();

    // Approve if the dataset NPU=false as that reflects the case where the dataset has no NPU
    // restrictions.
    if (!datasetNPU) {
      return MatchResult.from(DataUseMatchResultType.APPROVE, Collections.emptyList());
    }

    // At this point, we have a dataset NPU=true, so we need to check the purpose NPU
    if (!purposeNpu) {
      failures.add(NCU_F1);
      return MatchResult.from(DataUseMatchResultType.DENY, failures);
    }

    return MatchResult.from(DataUseMatchResultType.APPROVE, Collections.emptyList());
  }

  /**
   * DUOS Algorithm: Abstain From Decision Due to the variety of sensitive research areas, ethical
   * reasons, and areas where categorization is not possible, the DUOS system will not render a
   * decision in any of the cases not addressed in the methods above.
   *
   * <p>Abstained RP's: Any RP that is not GRU, DS-X, POA, MDS, Commercial
   */
  static MatchResult abstainDecision(
      DataUse purpose,
      DataUse dataset,
      Map<String, List<String>> purposeDiseaseIdMap,
      DataUseMatchResultType diseaseMatch) {

    // Immediate Abstain Cases:
    if (getNullableOrFalse(purpose.getControls())
        || getNullableOrFalse(purpose.getPopulation())
        || Objects.nonNull(purpose.getGender())
        || getNullableOrFalse(purpose.getPediatric())
        || getNullableOrFalse(purpose.getVulnerablePopulations())
        || getNullableOrFalse(purpose.getIllegalBehavior())
        || getNullableOrFalse(purpose.getSexualDiseases())
        || getNullableOrFalse(purpose.getPsychologicalTraits())
        || getNullableOrFalse(purpose.getNotHealth())
        || getNullableOrFalse(purpose.getStigmatizeDiseases())) {
      return MatchResult.from(DataUseMatchResultType.ABSTAIN, Collections.singletonList(ABSTAIN));
    }

    // Valid RPs
    boolean purposeDSX = getNullableOrFalse(!purpose.getDiseaseRestrictions().isEmpty());
    boolean purposeHMB = getNullableOrFalse(purpose.getHmbResearch());
    boolean purposePOA = getNullableOrFalse(purpose.getPopulationOriginsAncestry());
    boolean purposeMDS = getNullableOrFalse(purpose.getMethodsResearch());
    boolean purposeGRU = getNullableOrFalse(purpose.getGeneralUse());
    boolean purposeNPUExists = purpose.getNonProfitUse() != null;
    boolean datasetNPUExists = dataset.getNonProfitUse() != null;

    // If RP is valid then call that method
    if (purposeDSX) {
      return matchDiseases(purpose, dataset, purposeDiseaseIdMap);
    }
    if (purposeHMB || purposeGRU) {
      return matchHMB(purpose, dataset);
    }
    if (purposePOA) {
      return matchPOA(purpose, dataset);
    }
    if (purposeMDS) {
      return matchMDS(purpose, dataset, diseaseMatch);
    }
    if (purposeNPUExists || datasetNPUExists) {
      return matchNonProfitUse(purpose, dataset);
    }
    return MatchResult.from(DataUseMatchResultType.ABSTAIN, Collections.singletonList(ABSTAIN));
  }

  // Helper Methods

  /**
   * @param bool nullable boolean value
   * @return boolean The value, or false otherwise
   */
  private static boolean getNullableOrFalse(Boolean bool) {
    return Optional.ofNullable(bool).orElse(false);
  }
}
