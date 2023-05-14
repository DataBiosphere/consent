package org.broadinstitute.consent.http.enumeration;

public enum DarStatus {
  // Elections and DARs share the same string value for canceled state.
  ARCHIVED("Archived"),
  CANCELED(ElectionStatus.CANCELED.getValue());

  private final String value;

  DarStatus(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}
