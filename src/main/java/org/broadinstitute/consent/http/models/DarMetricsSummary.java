package org.broadinstitute.consent.http.models;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class DarMetricsSummary {

  final Timestamp updateDate;
  final String projectTitle;
  final String darCode;
  final String nonTechRus;
  final String referenceId;
  final Boolean expired;

  public DarMetricsSummary(DataAccessRequest dar) {
    Instant instant = Instant.now().minus(1, ChronoUnit.YEARS);
    Timestamp lastYear = Timestamp.from(instant);
    if (dar != null && dar.getData() != null) {
      this.updateDate = dar.getUpdateDate();
      this.projectTitle = dar.getData().getProjectTitle();
      this.darCode = dar.getDarCode();
      this.nonTechRus = dar.getData().getNonTechRus();
      this.referenceId = dar.getReferenceId();
      this.expired = dar.getSubmissionDate().before(lastYear);
    } else {
      this.updateDate = null;
      this.projectTitle = null;
      this.darCode = null;
      this.nonTechRus = null;
      this.referenceId = null;
      this.expired = null;
    }
  }
}
