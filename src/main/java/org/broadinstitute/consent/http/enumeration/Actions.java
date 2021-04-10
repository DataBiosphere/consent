package org.broadinstitute.consent.http.enumeration;

public enum Actions {
  ADD("add"),
  CREATE("create"),
  DELETE("delete"),
  REMOVE("remove"),
  REPLACE("replace");

  private final String value;

  Actions(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}
