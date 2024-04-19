package org.broadinstitute.consent.http.enumeration;

public enum EmailType {

  COLLECT(1),
  NEW_CASE(2),
  REMINDER(3),
  NEW_DAR(4),
  DISABLED_DATASET(5),
  CLOSED_DATASET_ELECTION(6),
  DATA_CUSTODIAN_APPROVAL(7),
  RESEARCHER_DAR_APPROVED(8),
  ADMIN_FLAGGED_DAR_APPROVED(9),
  DAR_CANCEL(10),
  DELEGATE_RESPONSIBILITIES(11),
  NEW_RESEARCHER(12),
  RESEARCHER_APPROVED(13),
  NEW_DATASET(14),
  NEW_DAA_REQUEST(15);

  private final Integer typeInt;

  EmailType(Integer typeInt) {
    this.typeInt = typeInt;
  }

  public Integer getTypeInt() {
    return typeInt;
  }
}
