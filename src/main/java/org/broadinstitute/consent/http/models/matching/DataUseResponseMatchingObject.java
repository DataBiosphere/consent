package org.broadinstitute.consent.http.models.matching;

import static org.broadinstitute.consent.http.models.matching.DataUseMatchResultType.Approve;

import java.util.List;

public class DataUseResponseMatchingObject {

  public DataUseMatchResultType result;

  public DataUseRequestMatchingObject matchPair;

  public List<String> failureReasons;

  public DataUseResponseMatchingObject(DataUseMatchResultType result,
      DataUseRequestMatchingObject matchPair, List<String> failureReasons) {
    this.result = result;
    this.matchPair = matchPair;
    this.failureReasons = failureReasons;
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

  public List<String> getFailureReasons() {
    return failureReasons;
  }

  public void setFailureReasons(List<String> failureReasons) {
    this.failureReasons = failureReasons;
  }
}
