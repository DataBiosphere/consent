package org.broadinstitute.consent.http.enumeration;

import java.util.List;

final class EmailTypeConstants {
  public static final List<String> SENDGRID_CATEGORIES = List.of("DUOS");

  private EmailTypeConstants() {
    // Prevent instantiation
  }
}

public enum EmailType {
  COLLECT(1),
  NEW_CASE(2, "new-case.ftl", EmailTypeConstants.SENDGRID_CATEGORIES),
  REMINDER(3, "reminder.ftl", EmailTypeConstants.SENDGRID_CATEGORIES),
  NEW_DAR(4, "new-request.ftl", EmailTypeConstants.SENDGRID_CATEGORIES),
  DISABLED_DATASET(5),
  CLOSED_DATASET_ELECTION(6),
  DATA_CUSTODIAN_APPROVAL(7, "data-custodian-approval.ftl", EmailTypeConstants.SENDGRID_CATEGORIES),
  RESEARCHER_DAR_APPROVED(8, "researcher-dar-approved.ftl", EmailTypeConstants.SENDGRID_CATEGORIES),
  ADMIN_FLAGGED_DAR_APPROVED(9),
  DAR_CANCEL(10),
  DELEGATE_RESPONSIBILITIES(11),
  NEW_RESEARCHER(12, "new-researcher-library-request.ftl", EmailTypeConstants.SENDGRID_CATEGORIES),
  RESEARCHER_APPROVED(13),
  NEW_DATASET(14, "dataset-submitted.ftl", EmailTypeConstants.SENDGRID_CATEGORIES),
  // Deprecated: maintained for historical purposes so legacy references to type 15 remain
  // representable.
  @Deprecated
  NEW_DAA_REQUEST(15, null, null),
  NEW_DAA_UPLOAD_RESEARCHER(
      16, "new-daa-upload-researcher.ftl", EmailTypeConstants.SENDGRID_CATEGORIES),
  NEW_DAA_UPLOAD_SO(
      17, "new-daa-upload-signing-official.ftl", EmailTypeConstants.SENDGRID_CATEGORIES),
  DATASET_DENIED(18, "dataset-denied.ftl", EmailTypeConstants.SENDGRID_CATEGORIES),
  DATASET_APPROVED(19, "dataset-approved.ftl", EmailTypeConstants.SENDGRID_CATEGORIES),
  DAR_EXPIRED(20, "dar-expired.ftl", EmailTypeConstants.SENDGRID_CATEGORIES),
  DAR_EXPIRATION_REMINDER(
      21, "dar-expiration-reminder.ftl", EmailTypeConstants.SENDGRID_CATEGORIES),
  NEW_PROGRESS_REPORT_REQUEST(
      22, "new-progress-report-request.ftl", EmailTypeConstants.SENDGRID_CATEGORIES),
  NEW_PROGRESS_REPORT_CASE(
      23, "new-progress-report-case.ftl", EmailTypeConstants.SENDGRID_CATEGORIES),
  RESEARCHER_PROGRESS_REPORT_APPROVED(
      24, "researcher-progress-report-approved.ftl", EmailTypeConstants.SENDGRID_CATEGORIES),
  RESEARCHER_CLOSEOUT_COMPLETED(
      25, "researcher-closeout-completed.ftl", EmailTypeConstants.SENDGRID_CATEGORIES),
  SUBMITTED_CLOSEOUT(26, "submitted-closeout.ftl", EmailTypeConstants.SENDGRID_CATEGORIES),
  NEW_LIBRARY_CARD_ISSUED(
      27, "new-library-card-issued.ftl", EmailTypeConstants.SENDGRID_CATEGORIES),
  SO_DAR_SUBMITTED(28, "so-dar-submitted.ftl", EmailTypeConstants.SENDGRID_CATEGORIES),
  SO_DAR_APPROVED(29, "so-dar-approved.ftl", EmailTypeConstants.SENDGRID_CATEGORIES),
  SO_PROGRESS_REPORT_SUBMITTED(
      30, "so-progress-report-submitted.ftl", EmailTypeConstants.SENDGRID_CATEGORIES),
  SO_PROGRESS_REPORT_APPROVED(
      31, "so-progress-report-approved.ftl", EmailTypeConstants.SENDGRID_CATEGORIES),
  DAC_RADAR_APPROVED(32, "dac-radar-approved.ftl", EmailTypeConstants.SENDGRID_CATEGORIES),
  NEW_DAR_SO_NEEDS_TO_APPROVE(
      33, "new-dar-so-needs-to-approve.ftl", EmailTypeConstants.SENDGRID_CATEGORIES),
  DAC_VOTE_REMINDER_DIGEST(34, "vote-digest.ftl", EmailTypeConstants.SENDGRID_CATEGORIES),
  NEW_STUDY_REGISTRATION_CONFIRMATION(
      35, "new-study-registration-confirmation.ftl", EmailTypeConstants.SENDGRID_CATEGORIES),
  NEW_STUDY_DIGEST(36, "new-study-digest.ftl", EmailTypeConstants.SENDGRID_CATEGORIES),
  ;

  private final Integer typeInt;
  public final String templateName;
  private final List<String> categories;

  EmailType(Integer typeInt) {
    this(typeInt, null, EmailTypeConstants.SENDGRID_CATEGORIES);
  }

  EmailType(Integer typeInt, String templateName, List<String> categories) {
    this.typeInt = typeInt;
    this.templateName = templateName;
    this.categories = categories;
  }

  public Integer getTypeInt() {
    return typeInt;
  }

  public List<String> getSendGridCategories() {
    return categories;
  }
}
