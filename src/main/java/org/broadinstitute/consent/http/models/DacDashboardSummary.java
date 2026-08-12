package org.broadinstitute.consent.http.models;

import org.broadinstitute.consent.http.models.DashboardSummary.DataLibrary;

public record DacDashboardSummary(
    DarRequests darRequests, Dacs dacs, DacDatasets dacDatasets, DataLibrary dataLibrary) {

  /**
   * Mirrors the DAC DAR Requests page. A completed collection is counted as approved; everything
   * else is pending. There is deliberately no denied count because DUOS has no denied collection
   * status.
   */
  public record DarRequests(long total, long approved, long pending, long awaitingMyVote) {}

  /** The DACs available on the chair-only Manage DACs page. Zero for a DAC member. */
  public record Dacs(long total) {}

  /** The datasets associated with any of the caller's DAC roles. Zero for a DAC member. */
  public record DacDatasets(long total) {}
}
