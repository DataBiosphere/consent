package org.broadinstitute.consent.http.enumeration;

/** Who may read a DAR trend report for one study or dataset. */
public enum DarReportAudience {
  /**
   * Whoever can read the study's or dataset's DAR history. For reports that cover granted access
   * alone, which that history already lists.
   */
  HISTORY_READERS,

  /**
   * Admins, and the chairs and members of the DAC in charge of the datasets, who must also be able
   * to read the DAR history. For reports that count requests that were denied or are still pending:
   * narrowed to one day they give away a single request's outcome, which the history never shows.
   */
  DAC_IN_CHARGE
}
