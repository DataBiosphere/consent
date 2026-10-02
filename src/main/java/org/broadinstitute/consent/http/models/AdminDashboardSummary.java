package org.broadinstitute.consent.http.models;

import org.broadinstitute.consent.http.models.DashboardSummary.DarRequests;

public record AdminDashboardSummary(
    DarRequests darRequests, Dacs dacs, Users users, LibraryCards libraryCards) {
  public record Dacs(long total) {}

  public record Users(long total) {}

  public record LibraryCards(long total) {}
}
