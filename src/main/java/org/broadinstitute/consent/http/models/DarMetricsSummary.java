package org.broadinstitute.consent.http.models;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

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
        computeExpired(dar));
  }

  private static Boolean computeExpired(DataAccessRequest dar) {
    // If the DAR or its submission date is null, we consider it expired for metrics purposes
    if (dar == null || dar.getSubmissionDate() == null) {
      return true;
    }
    LocalDateTime oneYearAgo = LocalDateTime.now().minusYears(1);
    ZonedDateTime zonedDateTime = oneYearAgo.atZone(ZoneId.systemDefault());
    Timestamp lastYear = Timestamp.from(zonedDateTime.toInstant());
    return dar.getSubmissionDate().before(lastYear);
  }
}
