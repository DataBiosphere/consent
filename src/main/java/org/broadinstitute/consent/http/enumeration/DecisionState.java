package org.broadinstitute.consent.http.enumeration;

/**
 * Where a DAC decision stands, for a DAR-dataset pair or rolled up over a DAR. Pairs never report
 * {@link #MIXED}, and DARs never report {@link #NO_ELECTION}: a DAR still waiting on any dataset is
 * {@link #PENDING}.
 */
public enum DecisionState {
  APPROVED,
  DENIED,
  MIXED,
  PENDING,
  CANCELED,
  NO_ELECTION
}
