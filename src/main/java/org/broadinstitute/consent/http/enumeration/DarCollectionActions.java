package org.broadinstitute.consent.http.enumeration;

public enum DarCollectionActions {
  CANCEL("Cancel"),
  OPEN("Open"),
  VOTE("Vote"),
  UPDATE("Update"),
  REVIEW("Review"),
  REVISE("Revise"),
  RESUME("Resume"),
  DELETE("Delete"),
  CREATE_PROGRESS_REPORT("Create_Progress_Report");

  private final String value;

  DarCollectionActions(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}
