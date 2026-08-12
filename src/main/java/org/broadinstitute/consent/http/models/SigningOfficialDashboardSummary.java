package org.broadinstitute.consent.http.models;

import org.broadinstitute.consent.http.models.DashboardSummary.DarRequests;
import org.broadinstitute.consent.http.models.DashboardSummary.InstitutionLibrary;

public record SigningOfficialDashboardSummary(
    ResearcherStatus researcherStatus,
    DarRequests darRequests,
    DarApprovals darApprovals,
    DataSubmitters dataSubmitters,
    InstitutionLibrary institutionLibrary,
    DaaAssociations daaAssociations) {
  public record ResearcherStatus(long active, long inactive) {}

  public record DarApprovals(long total, long awaitingSoAction) {}

  public record DataSubmitters(long approved) {}

  public record DaaAssociations(long agreements, long researchersApproved) {}
}
