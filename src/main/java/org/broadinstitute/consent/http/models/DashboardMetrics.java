package org.broadinstitute.consent.http.models;

/**
 * Headline figures for a dashboard's metric tiles, over {@code from} to {@code to} inclusive.
 * {@code soApprovals} is null where the dashboard has no SO approvals tile.
 */
public record DashboardMetrics(
    String from,
    String to,
    Decisions decisions,
    Turnaround turnaround,
    SoApprovals soApprovals,
    Volume volume,
    Expiration expiration) {

  public record Decisions(
      long submitted, long pending, long approved, long denied, long mixed, long canceled) {}

  /**
   * Statistics cover the decisions with a usable vote date; {@code unmeasured} is the rest. Null
   * when none were measured.
   */
  public record Turnaround(long decided, long unmeasured, Double medianDays, Integer modeDays) {}

  public record SoApprovals(long approved, long pending, long skipped) {}

  public record Volume(long dars, long researchers, long institutions) {}

  public record Expiration(long expired, long closedOut, long renewals) {}
}
