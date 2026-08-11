package org.broadinstitute.consent.http.models;

public record SigningOfficialDashboardSummary(
    ResearcherStatus researcherStatus,
    DarRequests darRequests,
    DarApprovals darApprovals,
    DataSubmitters dataSubmitters,
    InstitutionLibrary institutionLibrary,
    DaaAssociations daaAssociations) {
  public record ResearcherStatus(long active, long inactive) {}

  /**
   * Counts mirror the statuses shown on the Signing Official DAR Requests page (see {@code
   * DarCollectionStatus}): a collection the researcher withdrew is Canceled, and anything not yet
   * fully approved or canceled is In Process.
   */
  public record DarRequests(long total, long approved, long canceled, long inProcess) {}

  public record DarApprovals(long total, long awaitingSoAction) {}

  public record DataSubmitters(long approved) {}

  public record InstitutionLibrary(long datasets, long studies) {}

  public record DaaAssociations(long agreements, long researchersApproved) {}
}
