package org.broadinstitute.consent.http.enumeration;

public enum EmailType {
  COLLECT(1),
  NEW_CASE(2, "new-case.ftl"),
  REMINDER(3, "reminder.ftl"),
  NEW_DAR(4, "new-request.ftl"),
  DISABLED_DATASET(5),
  CLOSED_DATASET_ELECTION(6),
  DATA_CUSTODIAN_APPROVAL(7, "data-custodian-approval.ftl"),
  RESEARCHER_DAR_APPROVED(8, "researcher-dar-approved.ftl"),
  ADMIN_FLAGGED_DAR_APPROVED(9),
  DAR_CANCEL(10),
  DELEGATE_RESPONSIBILITIES(11),
  NEW_RESEARCHER(12, "new-researcher-library-request.ftl"),
  RESEARCHER_APPROVED(13),
  NEW_DATASET(14, "dataset-submitted.ftl"),
  // Deprecated: maintained for historical purposes so legacy references to type 15 remain
  // representable.
  @Deprecated
  NEW_DAA_REQUEST(15, null),
  NEW_DAA_UPLOAD_RESEARCHER(16, "new-daa-upload-researcher.ftl"),
  NEW_DAA_UPLOAD_SO(17, "new-daa-upload-signing-official.ftl"),
  DATASET_DENIED(18, "dataset-denied.ftl"),
  DATASET_APPROVED(19, "dataset-approved.ftl"),
  DAR_EXPIRED(20, "dar-expired.ftl"),
  DAR_EXPIRATION_REMINDER(21, "dar-expiration-reminder.ftl"),
  NEW_PROGRESS_REPORT_REQUEST(22, "new-progress-report-request.ftl"),
  NEW_PROGRESS_REPORT_CASE(23, "new-progress-report-case.ftl"),
  RESEARCHER_PROGRESS_REPORT_APPROVED(24, "researcher-progress-report-approved.ftl"),
  RESEARCHER_CLOSEOUT_COMPLETED(25, "researcher-closeout-completed.ftl"),
  SUBMITTED_CLOSEOUT(26, "submitted-closeout.ftl"),
  NEW_LIBRARY_CARD_ISSUED(27, "new-library-card-issued.ftl"),
  SO_DAR_SUBMITTED(28, "so-dar-submitted.ftl"),
  SO_DAR_APPROVED(29, "so-dar-approved.ftl"),
  SO_PROGRESS_REPORT_SUBMITTED(30, "so-progress-report-submitted.ftl"),
  SO_PROGRESS_REPORT_APPROVED(31, "so-progress-report-approved.ftl"),
  DAC_RADAR_APPROVED(32, "dac-radar-approved.ftl"),
  NEW_DAR_SO_NEEDS_TO_APPROVE(33, "new-dar-so-needs-to-approve.ftl"),
  DAC_VOTE_REMINDER_DIGEST(34, "vote-digest.ftl"),
  NEW_STUDY_REGISTRATION_CONFIRMATION(35, "new-study-registration-confirmation.ftl"),
  NEW_STUDY_DIGEST(36, "new-study-digest.ftl"),
  ;

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
