package org.broadinstitute.consent.http.models;

import org.broadinstitute.consent.http.models.DashboardSummary.DarRequests;

public record AdminDashboardSummary(
    DarRequests darRequests,
    Dacs dacs,
    Users users,
    Institutions institutions,
    LibraryCards libraryCards,
    DaaAssociations daaAssociations,
    Metrics metrics) {
  public record Dacs(long total) {}

  public record Users(long total) {}

  public record Institutions(long total, long withoutSigningOfficial) {}

  public record LibraryCards(long total) {}

  public record DaaAssociations(long agreements, long researchersApproved) {}

  /** Headline figures for the metric tiles, over {@code from} to {@code to} inclusive. */
  public record Metrics(
      String from,
      String to,
      Decisions decisions,
      Turnaround turnaround,
      SoApprovals soApprovals,
      Volume volume,
      Expiration expiration) {}

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
