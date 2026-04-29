package org.broadinstitute.consent.http.enumeration;

import java.util.Arrays;
import java.util.Optional;

public enum DocumentEntity {
  DAC("dac"),
  DAR("dar"),
  DATASET("dataset"),
  STUDY("study");

  private final String value;

  DocumentEntity(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  public static Optional<DocumentEntity> fromValue(String value) {
    if (value == null) {
      return Optional.empty();
    }
    return Arrays.stream(values())
        .filter(entity -> entity.getValue().equalsIgnoreCase(value))
        .findFirst();
  }
}
