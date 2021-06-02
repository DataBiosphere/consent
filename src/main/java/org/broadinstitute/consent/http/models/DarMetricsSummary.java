package org.broadinstitute.consent.http.models;

import java.sql.Timestamp;

public class DarMetricsSummary {
    final Timestamp updateDate;
    final String projectTitle;
    final String darCode;
    final String nonTechRus;
    final String investigator;

    public DarMetricsSummary(Timestamp updateDate, String projectTitle, String darCode, String nonTechRus, String investigator) {
      this.updateDate = updateDate;
      this.projectTitle = projectTitle;
      this.darCode = darCode;
      this.nonTechRus = nonTechRus;
      this.investigator = investigator;
    }

    public DarMetricsSummary() {
      this.updateDate = null;
      this.projectTitle = null;
      this.darCode = null;
      this.nonTechRus = null;
      this.investigator = null;
    }
  
}
