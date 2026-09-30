package org.broadinstitute.consent.http.enumeration;

/**
 * Where a DAR stands with its signing official. {@link #SKIPPED} means pre-authorization let it
 * bypass SO review; {@link #NOT_DETERMINED} is a row from before submissions recorded which
 * applied.
 */
public enum SoApprovalStatus {
  APPROVED,
  PENDING,
  SKIPPED,
  NOT_DETERMINED
}
