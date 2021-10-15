package org.broadinstitute.consent.http.enumeration;

public enum AssociationType {
  SAMPLE("sample"),
  SAMPLE_SET("sampleSet");

  private final String value;

  AssociationType(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}
