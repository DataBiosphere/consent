package org.broadinstitute.consent.http.models;

import java.sql.Timestamp;

public record DarMetricsSummary(
    Timestamp updateDate,
    String projectTitle,
    String darCode,
    String nonTechRus,
    String referenceId,
    Boolean expired) {

  public DarMetricsSummary(DarCollectionSummary summary) {
    this(
        summary != null ? summary.getUpdateDate() : null,
        summary != null ? summary.getName() : null,
        summary != null ? summary.getDarCode() : null,
        summary != null ? summary.getNonTechRus() : null,
        summary != null ? summary.getLatestReferenceId() : null,
        summary == null || summary.isExpired());
  }
}
