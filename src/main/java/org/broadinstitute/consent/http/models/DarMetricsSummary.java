package org.broadinstitute.consent.http.models;

import java.sql.Timestamp;

public record DarMetricsSummary(
    Timestamp updateDate,
    String projectTitle,
    String darCode,
    String nonTechRus,
    String referenceId,
    Boolean expired) {

  public DarMetricsSummary(DataAccessRequest dar) {
    this(
        dar != null ? dar.getUpdateDate() : null,
        dar != null && dar.getData() != null ? dar.getData().getProjectTitle() : null,
        dar != null ? dar.getDarCode() : null,
        dar != null && dar.getData() != null ? dar.getData().getNonTechRus() : null,
        dar != null ? dar.getReferenceId() : null,
        dar == null || dar.getSubmissionDate() == null || dar.getExpired());
  }
}
