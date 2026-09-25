package org.broadinstitute.consent.http.enumeration;

/** Where the DAC decision on a DAR-dataset pair stands. */
public enum DecisionState {
  APPROVED,
  DENIED,
  PENDING,
  CANCELED,
  NO_ELECTION
}
