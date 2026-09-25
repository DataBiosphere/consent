package org.broadinstitute.consent.http.enumeration;

/** Who made a DAC decision. {@link #MIXED} applies only to a DAR decided both ways. */
public enum DecidedVia {
  MANUAL,
  RADAR,
  MIXED
}
