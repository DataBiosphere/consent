package org.broadinstitute.consent.http.models.draft;

public enum AlternativeDataSharingPlanDataSubmitted {

  WITHIN_3_MONTHS_OF_THE_LAST_DATA_GENERATED_OR_LAST_CLINICAL_VISIT(
      "Within 3 months of the last data generated or last clinical visit"),
  BY_BATCHES_OVER_STUDY_TIMELINE_E_G_BASED_ON_CLINICAL_TRIAL_ENROLLMENT_BENCHMARKS(
      "By batches over Study Timeline (e.g. based on clinical trial enrollment benchmarks)");
  private final String value;

  AlternativeDataSharingPlanDataSubmitted(String value) {
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
