package org.broadinstitute.consent.http.enumeration;

public enum AuditActions {
  ADD("add"),
  CREATE("create"),
  DELETE("delete"),
  REMOVE("remove"),
  REPLACE("replace"),
  UPDATE("update"),
  INDEXED("indexed"),
  DEINDEXED("deindexed");

  private final String value;

  AuditActions(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}
