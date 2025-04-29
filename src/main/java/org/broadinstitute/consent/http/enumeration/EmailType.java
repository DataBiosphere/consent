package org.broadinstitute.consent.http.enumeration;

public enum EmailType {

  COLLECT(1),
  NEW_CASE(2, "new-case.html"),
  REMINDER(3, "reminder.html"),
  NEW_DAR(4, "new-request.html"),
  DISABLED_DATASET(5),
  CLOSED_DATASET_ELECTION(6),
  DATA_CUSTODIAN_APPROVAL(7, "data-custodian-approval.html"),
  RESEARCHER_DAR_APPROVED(8, "researcher-dar-approved.html"),
  ADMIN_FLAGGED_DAR_APPROVED(9),
  DAR_CANCEL(10),
  DELEGATE_RESPONSIBILITIES(11),
  NEW_RESEARCHER(12, "new-researcher-library-request.html"),
  RESEARCHER_APPROVED(13),
  NEW_DATASET(14, "dataset-submitted.html"),
  NEW_DAA_REQUEST(15, "new-daa-request.html"),
  NEW_DAA_UPLOAD_RESEARCHER(16, "new-daa-upload-researcher.html"),
  NEW_DAA_UPLOAD_SO(17, "new-daa-upload-signing-official.html"),
  DATASET_DENIED(18, "dataset-denied.html"),
  DATASET_APPROVED(19, "dataset-approved.html"),
  DAC_EXPIRED(20, "dac-expired.html");

  private final Integer typeInt;
  public final String templateName;

  EmailType(Integer typeInt) {
    this(typeInt, null);
  }
  EmailType(Integer typeInt, String templateName) {
    this.typeInt = typeInt;
    this.templateName = templateName;
  }

  public Integer getTypeInt() {
    return typeInt;
  }
}
