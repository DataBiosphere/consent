package org.broadinstitute.consent.http.enumeration;

/**
 * Where a DAR's institution came from: recorded at submission, or read from the submitter's current
 * profile for DARs submitted before submissions recorded one.
 */
public enum InstitutionSource {
  RECORDED,
  CURRENT
}
