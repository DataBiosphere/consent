package org.broadinstitute.consent.http.models;

/** Response fragments shared by the role-specific console dashboard summaries. */
public final class DashboardSummary {

  private DashboardSummary() {}

  /** Counts for the four tabs in the main Data Library. */
  public record DataLibrary(long studies, long datasets, long models, long workspaces) {}

  /** DAR statuses shared by the Researcher and Signing Official consoles. */
  public record DarRequests(long total, long approved, long canceled, long inProcess) {}

  /** Counts for the Signing Official's institution-scoped Data Library. */
  public record InstitutionLibrary(long datasets, long studies) {}
}
