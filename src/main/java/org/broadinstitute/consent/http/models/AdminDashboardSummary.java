package org.broadinstitute.consent.http.models;

import org.broadinstitute.consent.http.models.DashboardSummary.DarRequests;

public record AdminDashboardSummary(
    DarRequests darRequests,
    Dacs dacs,
    Users users,
    Institutions institutions,
    LibraryCards libraryCards,
    DaaAssociations daaAssociations,
    DashboardMetrics metrics) {
  public record Dacs(long total) {}

  public record Users(long total) {}

  public record Institutions(long total, long withoutSigningOfficial) {}

  public record LibraryCards(long total) {}

  public record DaaAssociations(long agreements, long researchersApproved) {}
}
