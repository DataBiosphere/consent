package org.broadinstitute.consent.http.matching;

import static org.broadinstitute.consent.http.matching.DataUseMatchCasesV4.abstainDecision;
import static org.broadinstitute.consent.http.matching.DataUseMatchCasesV4.matchDiseases;
import static org.broadinstitute.consent.http.matching.DataUseMatchCasesV4.matchHMB;
import static org.broadinstitute.consent.http.matching.DataUseMatchCasesV4.matchMDS;
import static org.broadinstitute.consent.http.matching.DataUseMatchCasesV4.matchNonProfitUse;
import static org.broadinstitute.consent.http.matching.DataUseMatchCasesV4.matchPOA;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.consent.http.models.DataUse;
import org.broadinstitute.consent.http.models.matching.DataUseMatchResultType;
import org.broadinstitute.consent.http.service.OntologyService;

public class DataUseMatcherV4 {

  private final DataUseUtil dataUseUtil;

  public DataUseMatcherV4() {
    dataUseUtil = new DataUseUtil();
  }

  @Inject
  public void setOntologyService(OntologyService ontologyService) {
    dataUseUtil.setOntologyService(ontologyService);
  }

  // Matching Algorithm
  public MatchResult matchPurposeAndDatasetV4(DataUse purpose, DataUse dataset) {
    Map<String, List<String>> purposeDiseaseIdMap;
    try {
      purposeDiseaseIdMap =
          dataUseUtil.generatePurposeDiseaseIdMap(purpose.getDiseaseRestrictions());
    } catch (Exception e) {
      List<String> diseases =
          (purpose == null || purpose.getDiseaseRestrictions() == null)
              ? List.of()
              : purpose.getDiseaseRestrictions();
      String purposeRestrictions = StringUtils.join(diseases, ", ");
      List<String> errors =
          Arrays.asList(
              e.getMessage(), "Error found in one of the purpose terms: " + purposeRestrictions);
      return MatchResult.from(DataUseMatchResultType.DENY, errors);
    }

    MatchResult diseaseMatch = matchDiseases(purpose, dataset, purposeDiseaseIdMap);
    final List<MatchResult> matchReasons = new ArrayList<>();
    matchReasons.add(diseaseMatch);
    matchReasons.add(matchHMB(purpose, dataset));
    matchReasons.add(matchPOA(purpose, dataset));
    matchReasons.add(matchMDS(purpose, dataset, diseaseMatch.getMatchResultType()));
    matchReasons.add(matchNonProfitUse(purpose, dataset));
    matchReasons.add(
        abstainDecision(purpose, dataset, purposeDiseaseIdMap, diseaseMatch.getMatchResultType()));
    final boolean allMatch =
        matchReasons.stream()
            .map(MatchResult::getMatchResultType)
            .allMatch(rt -> rt.equals(DataUseMatchResultType.APPROVE));
    final boolean anyAbstain =
        matchReasons.stream()
            .map(MatchResult::getMatchResultType)
            .anyMatch(rt -> rt.equals(DataUseMatchResultType.ABSTAIN));
    final List<String> reasons =
        matchReasons.stream()
            .map(MatchResult::getMessage)
            .flatMap(Collection::stream)
            .filter(StringUtils::isNotBlank)
            .toList();
    // if all items match, decision is APPROVED
    // if not, determine whether DENY or ABSTAIN
    DataUseMatchResultType type;
    if (allMatch) {
      type = DataUseMatchResultType.APPROVE;
    } else if (anyAbstain) {
      type = DataUseMatchResultType.ABSTAIN;
    } else {
      type = DataUseMatchResultType.DENY;
    }
    return MatchResult.from(type, reasons);
  }
}
