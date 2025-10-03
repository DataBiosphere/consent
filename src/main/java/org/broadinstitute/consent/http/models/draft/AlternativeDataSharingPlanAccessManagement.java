package org.broadinstitute.consent.http.models.draft;

public enum AlternativeDataSharingPlanAccessManagement {
  CONTROLLED_ACCESS("Controlled Access"),
  OPEN_ACCESS("Open Access"),
  EXTERNAL_ACCESS("External Access");
  private final String value;

  AlternativeDataSharingPlanAccessManagement(String value) {
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
