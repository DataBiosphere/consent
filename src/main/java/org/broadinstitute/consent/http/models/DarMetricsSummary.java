package org.broadinstitute.consent.http.models;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public record DarMetricsSummary(
    Timestamp updateDate,
    String projectTitle,
    String darCode,
    String nonTechRus,
    String referenceId,
    Boolean expired) {

  public DarMetricsSummary(DataAccessRequest dar) {
    this(
        dar != null && dar.getData() != null ? dar.getUpdateDate() : null,
        dar != null && dar.getData() != null ? dar.getData().getProjectTitle() : null,
        dar != null && dar.getData() != null ? dar.getDarCode() : null,
        dar != null && dar.getData() != null ? dar.getData().getNonTechRus() : null,
        dar != null && dar.getData() != null ? dar.getReferenceId() : null,
        computeExpired(dar));
  }

  private static Boolean computeExpired(DataAccessRequest dar) {
    // If the DAR or its submission date is null, we consider it expired for metrics purposes
    if (dar == null || dar.getSubmissionDate() == null) {
      return true;
    }
    Instant instant = Instant.now().minus(1, ChronoUnit.YEARS);
    Timestamp lastYear = Timestamp.from(instant);
    return dar.getSubmissionDate().before(lastYear);
  }
}
