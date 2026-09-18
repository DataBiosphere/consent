package org.broadinstitute.consent.http.models;

import java.sql.Timestamp;

public record DarMetricsSummary(
    Timestamp updateDate,
    Timestamp submissionDate,
    String projectTitle,
    String darCode,
    String nonTechRus,
    String referenceId,
    String piName,
    String institutionName,
    Boolean expired) {

  /**
   * The same summary with no requester identity on it.
   *
   * <p>Named rather than rebuilt positionally at the call site: a nine-argument constructor call
   * still compiles after two same-typed components are reordered, and would quietly move values
   * into the wrong slots. Here the compiler names every field.
   */
  public DarMetricsSummary withoutRequesterIdentity() {
    return new DarMetricsSummary(
        updateDate,
        submissionDate,
        projectTitle,
        darCode,
        nonTechRus,
        referenceId,
        null,
        null,
        expired);
  }

  public DarMetricsSummary(
      Timestamp updateDate,
      String projectTitle,
      String darCode,
      String nonTechRus,
      String referenceId,
      Boolean expired) {
    this(updateDate, null, projectTitle, darCode, nonTechRus, referenceId, null, null, expired);
  }
}
