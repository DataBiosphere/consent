package org.broadinstitute.consent.http.enumeration;

import java.util.EnumSet;
import java.util.Optional;

/**
 * The only election type supported for current use is `DATA_ACCESS`.
 * `TRANSLATE_DUL`, `RP`, and `DATA_SET` are deprecated and should not be used for new elections.
 * We are maintaining the deprecated enums to support legacy elections.
 */
public enum ElectionType {
  DATA_ACCESS("DataAccess"),
  @Deprecated TRANSLATE_DUL("TranslateDUL"),
  @Deprecated RP("RP"),
  @Deprecated DATA_SET("DataSet");

  private final String value;

  ElectionType(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  public static String getValue(String value) {
    for (ElectionType e : ElectionType.values()) {
      if (e.getValue().equalsIgnoreCase(value)) {
        return e.getValue();
      }
    }
    return null;
  }

  public static ElectionType getFromValue(String value) {
    Optional<ElectionType> type =
        EnumSet.allOf(ElectionType.class).stream()
            .filter(t -> t.getValue().equalsIgnoreCase(value))
            .findFirst();
    return type.orElse(null);
  }
}
