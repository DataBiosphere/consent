package org.broadinstitute.consent.http.models.draft;

public enum StudyType {

  OBSERVATIONAL("Observational"),
  INTERVENTIONAL("Interventional"),
  DESCRIPTIVE("Descriptive"),
  ANALYTICAL("Analytical"),
  PROSPECTIVE("Prospective"),
  RETROSPECTIVE("Retrospective"),
  CASE_REPORT("Case report"),
  CASE_SERIES("Case series"),
  CROSS_SECTIONAL("Cross-sectional"),
  COHORT_STUDY("Cohort study");
  private final String value;

  StudyType(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return this.value;
  }

  public String value() {
    return this.value;
  }

}