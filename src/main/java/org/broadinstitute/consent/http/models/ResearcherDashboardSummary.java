package org.broadinstitute.consent.http.models;

public record ResearcherDashboardSummary(
    DataLibrary dataLibrary,
    DarRequests darRequests,
    DatasetApprovals datasetApprovals,
    DataSubmissions dataSubmissions) {

  /** What the researcher can see in the Data Library, by asset tab. */
  public record DataLibrary(long studies, long datasets, long models, long workspaces) {}

  /**
   * Mirrors the statuses on the researcher's DAR Requests page (see {@code DarCollectionStatus}).
   * No denied count: the system records no denial, so an unapproved request stays In Process.
   * Drafts are excluded.
   */
  public record DarRequests(long total, long approved, long canceled, long inProcess) {}

  /**
   * One count per approved dataset per DAR. An approval expires {@link
   * DataAccessRequest#EXPIRATION_DURATION_MILLIS} after submission; {@code expiringSoon} is the
   * subset of {@code active} expiring within 30 days.
   */
  public record DatasetApprovals(long active, long expiringSoon, long expired) {}

  /** Sum of the nine My Data Submissions tab counts for records the researcher registered. */
  public record DataSubmissions(long total) {}
}
