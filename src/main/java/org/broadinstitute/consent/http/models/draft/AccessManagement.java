package org.broadinstitute.consent.http.models.draft;

public enum AccessManagement {
  OPEN("open"),
  CONTROLLED("controlled"),
  EXTERNAL("external");
  private final String value;

  AccessManagement(String value) {
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
