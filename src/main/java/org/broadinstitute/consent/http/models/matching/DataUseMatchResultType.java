package org.broadinstitute.consent.http.models.matching;

// Copied from MatchResultPair in consent-ontology
public enum DataUseMatchResultType {
  APPROVE,    // true
  DENY,       // false
  ABSTAIN;

  public static Boolean Approve(DataUseMatchResultType x) {
    return x == APPROVE;
  }

  public static Boolean Deny(DataUseMatchResultType x) {
    return x == DENY;
  }

  public static Boolean Abstain(DataUseMatchResultType x) {
    return x == ABSTAIN;
  }
}
