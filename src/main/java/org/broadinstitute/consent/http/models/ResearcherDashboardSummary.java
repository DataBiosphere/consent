package org.broadinstitute.consent.http.models;

import org.broadinstitute.consent.http.models.DashboardSummary.DarRequests;
import org.broadinstitute.consent.http.models.DashboardSummary.DataLibrary;

public record ResearcherDashboardSummary(
    DataLibrary dataLibrary,
    DarRequests darRequests,
    DatasetApprovals datasetApprovals,
    DataSubmissions dataSubmissions) {

  /**
   * One count per approved dataset per DAR. An approval expires {@link
   * DataAccessRequest#EXPIRATION_DURATION_MILLIS} after submission; {@code expiringSoon} is the
   * subset of {@code active} expiring within 30 days.
   */
  public record DatasetApprovals(long active, long expiringSoon, long expired) {}

  /** Sum of the nine My Data Submissions tab counts for records the researcher registered. */
  public record DataSubmissions(long total) {}
}
