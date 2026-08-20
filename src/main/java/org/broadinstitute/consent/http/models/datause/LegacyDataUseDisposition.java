package org.broadinstitute.consent.http.models.datause;

import org.broadinstitute.consent.http.models.DataUse;

/**
 * What an approved review decided to do with one noncanonical Data Use record.
 *
 * <p>Nothing derives a disposition from the record. Choosing a primary category for an Other text
 * is a domain judgement, so it is supplied to the process rather than inferred by it.
 */
public sealed interface LegacyDataUseDisposition {

  /**
   * @param approvalReference where the approval is recorded, so a changed record traces to it
   */
  record Normalize(DataUse approvedDataUse, String approvalReference)
      implements LegacyDataUseDisposition {}

  /** For a shape the validator accepts, such as Other-only, whose match predates the algorithm. */
  record RecomputeMatchesOnly() implements LegacyDataUseDisposition {}

  /** No automated action: no deterministic mapping, so the curator-assisted process handles it. */
  record Defer(String reason) implements LegacyDataUseDisposition {}
}
