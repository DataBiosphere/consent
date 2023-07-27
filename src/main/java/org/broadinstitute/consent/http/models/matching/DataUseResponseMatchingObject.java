package org.broadinstitute.consent.http.models.matching;

import static org.broadinstitute.consent.http.models.matching.DataUseMatchResultType.Approve;

import java.util.List;

public class DataUseResponseMatchingObject {

  public DataUseMatchResultType result;

  public DataUseRequestMatchingObject matchPair;

  public List<String> rationale;

  public DataUseResponseMatchingObject(DataUseMatchResultType result,
      DataUseRequestMatchingObject matchPair, List<String> rationale) {
    this.result = result;
    this.matchPair = matchPair;
    this.rationale = rationale;
  }

  public DataUseMatchResultType getResult() {
    return result;
  }

  public boolean isResult() {
    return Approve(result);
  }

  public void setResult(DataUseMatchResultType result) {
    this.result = result;
  }

  public DataUseRequestMatchingObject getMatchPair() {
    return matchPair;
  }

  public void setMatchPair(DataUseRequestMatchingObject matchPair) {
    this.matchPair = matchPair;
  }

  public List<String> getRationale() {
    return rationale;
  }

  public void setRationale(List<String> rationale) {
    this.rationale = rationale;
  }
}
