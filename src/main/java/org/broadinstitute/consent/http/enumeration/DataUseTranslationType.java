package org.broadinstitute.consent.http.enumeration;

import java.util.List;

/**
 * Ontology's translation endpoint for a `DataUse` object has separate
 * translations depending on the whether the use restrictions apply to
 * a dataset (i.e. consent) or a purpose (i.e. DAR or research purpose).
 */
public enum DataUseTranslationType {
  DATASET("dataset"),
  PURPOSE("purpose");
  private final String value;

  DataUseTranslationType(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  public static String getValue(String value) {
    for (DataUseTranslationType e : DataUseTranslationType.values()) {
      if (e.getValue().equalsIgnoreCase(value)) {
        return e.getValue();
      }
    }
    return null;
  }

  public static boolean contains(List<String> valueList) {
    for (String value : valueList) {
      if (!contains(value)) {
        return false;
      }
    }
    return true;
  }

  public static boolean contains(String value) {
    for (DataUseTranslationType c : DataUseTranslationType.values()) {
      if (c.name().equalsIgnoreCase(value)) {
        return true;
      }
    }
    return false;
  }
}
