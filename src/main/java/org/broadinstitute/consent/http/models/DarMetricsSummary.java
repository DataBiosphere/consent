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
