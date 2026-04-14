package org.broadinstitute.consent.http.enumeration;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum DocumentEntity {
  DATASET("dataset"),
  STUDY("study");

  private final String value;

  DocumentEntity(String value) {
    this.value = value;
  }

  public static Optional<DocumentEntity> fromValue(String value) {
    if (value == null || value.isBlank()) {
      return Optional.empty();
    }
    return Arrays.stream(values())
        .filter(entity -> entity.value.equals(value.toLowerCase(Locale.ROOT)))
        .findFirst();
  }
}
