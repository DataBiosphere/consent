package org.broadinstitute.consent.http.enumeration;

public enum ElectionFields {

  ID("election_id"),
  TYPE("election_type"),
  STATUS("status"),
  CREATE_DATE("create_date"),
  REFERENCE_ID("reference_id"),
  LAST_UPDATE("last_update"),
  FINAL_ACCESS_VOTE("final_access_vote"),
  FINAL_VOTE("final_vote"),
  FINAL_RATIONALE("final_rationale"),
  FINAL_VOTE_DATE("final_vote_date"),
  DATA_USE_LETTER("data_use_letter"),
  DATASET_ID("dataset_id"),
  VERSION("version"),
  ARCHIVED("archived"),
  DUL_NAME("dul_name");

  private final String value;

  ElectionFields(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

}
