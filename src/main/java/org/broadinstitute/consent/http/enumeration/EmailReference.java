package org.broadinstitute.consent.http.enumeration;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

/** What an email type stores as its {@code email_entity.entity_reference_id}. */
public enum EmailReference {
  DAR_CODE,
  DAR_REFERENCE_ID,
  ELECTION_ID,
  DUOS_ID,
  DATASET_NAME,
  STUDY_UUID,
  NONE;

  @SuppressWarnings("deprecation")
  public static EmailReference of(EmailType type) {
    return switch (type) {
      case NEW_CASE,
          NEW_DAR,
          DATA_CUSTODIAN_APPROVAL,
          RESEARCHER_DAR_APPROVED,
          NEW_PROGRESS_REPORT_CASE,
          RESEARCHER_PROGRESS_REPORT_APPROVED,
          NEW_DAR_SO_NEEDS_TO_APPROVE ->
          DAR_CODE;
      case DAR_EXPIRED,
          DAR_EXPIRATION_REMINDER,
          NEW_PROGRESS_REPORT_REQUEST,
          RESEARCHER_CLOSEOUT_COMPLETED,
          SUBMITTED_CLOSEOUT,
          SO_DAR_SUBMITTED,
          SO_DAR_APPROVED,
          SO_PROGRESS_REPORT_SUBMITTED,
          SO_PROGRESS_REPORT_APPROVED,
          DAC_RADAR_APPROVED ->
          DAR_REFERENCE_ID;
      case REMINDER -> ELECTION_ID;
      case DATASET_DENIED, DATASET_APPROVED -> DUOS_ID;
      case NEW_DATASET -> DATASET_NAME;
      case NEW_STUDY_REGISTRATION_CONFIRMATION -> STUDY_UUID;
      case COLLECT,
          DISABLED_DATASET,
          CLOSED_DATASET_ELECTION,
          ADMIN_FLAGGED_DAR_APPROVED,
          DAR_CANCEL,
          DELEGATE_RESPONSIBILITIES,
          NEW_RESEARCHER,
          RESEARCHER_APPROVED,
          NEW_DAA_REQUEST,
          NEW_DAA_UPLOAD_RESEARCHER,
          NEW_DAA_UPLOAD_SO,
          NEW_LIBRARY_CARD_ISSUED,
          DAC_VOTE_REMINDER_DIGEST,
          NEW_STUDY_DIGEST ->
          NONE;
    };
  }

  /** Email types that announce an approval, so they concern only the datasets approved. */
  @SuppressWarnings("deprecation")
  public static boolean isApproval(EmailType type) {
    return switch (type) {
      case DATA_CUSTODIAN_APPROVAL,
          RESEARCHER_DAR_APPROVED,
          RESEARCHER_PROGRESS_REPORT_APPROVED,
          SO_DAR_APPROVED,
          SO_PROGRESS_REPORT_APPROVED,
          DAC_RADAR_APPROVED ->
          true;
      case COLLECT,
          NEW_CASE,
          REMINDER,
          NEW_DAR,
          DISABLED_DATASET,
          CLOSED_DATASET_ELECTION,
          ADMIN_FLAGGED_DAR_APPROVED,
          DAR_CANCEL,
          DELEGATE_RESPONSIBILITIES,
          NEW_RESEARCHER,
          RESEARCHER_APPROVED,
          NEW_DATASET,
          NEW_DAA_REQUEST,
          NEW_DAA_UPLOAD_RESEARCHER,
          NEW_DAA_UPLOAD_SO,
          DATASET_DENIED,
          DATASET_APPROVED,
          DAR_EXPIRED,
          DAR_EXPIRATION_REMINDER,
          NEW_PROGRESS_REPORT_REQUEST,
          NEW_PROGRESS_REPORT_CASE,
          RESEARCHER_CLOSEOUT_COMPLETED,
          SUBMITTED_CLOSEOUT,
          NEW_LIBRARY_CARD_ISSUED,
          SO_DAR_SUBMITTED,
          SO_PROGRESS_REPORT_SUBMITTED,
          NEW_DAR_SO_NEEDS_TO_APPROVE,
          DAC_VOTE_REMINDER_DIGEST,
          NEW_STUDY_REGISTRATION_CONFIRMATION,
          NEW_STUDY_DIGEST ->
          false;
    };
  }

  /**
   * DAR-code email types about a progress report, which concern the latest one submitted before the
   * send.
   */
  @SuppressWarnings("deprecation")
  public static boolean isAboutProgressReport(EmailType type) {
    return switch (type) {
      case NEW_PROGRESS_REPORT_CASE, RESEARCHER_PROGRESS_REPORT_APPROVED -> true;
      case COLLECT,
          NEW_CASE,
          REMINDER,
          NEW_DAR,
          DISABLED_DATASET,
          CLOSED_DATASET_ELECTION,
          DATA_CUSTODIAN_APPROVAL,
          RESEARCHER_DAR_APPROVED,
          ADMIN_FLAGGED_DAR_APPROVED,
          DAR_CANCEL,
          DELEGATE_RESPONSIBILITIES,
          NEW_RESEARCHER,
          RESEARCHER_APPROVED,
          NEW_DATASET,
          NEW_DAA_REQUEST,
          NEW_DAA_UPLOAD_RESEARCHER,
          NEW_DAA_UPLOAD_SO,
          DATASET_DENIED,
          DATASET_APPROVED,
          DAR_EXPIRED,
          DAR_EXPIRATION_REMINDER,
          NEW_PROGRESS_REPORT_REQUEST,
          RESEARCHER_CLOSEOUT_COMPLETED,
          SUBMITTED_CLOSEOUT,
          NEW_LIBRARY_CARD_ISSUED,
          SO_DAR_SUBMITTED,
          SO_DAR_APPROVED,
          SO_PROGRESS_REPORT_SUBMITTED,
          SO_PROGRESS_REPORT_APPROVED,
          DAC_RADAR_APPROVED,
          NEW_DAR_SO_NEEDS_TO_APPROVE,
          DAC_VOTE_REMINDER_DIGEST,
          NEW_STUDY_REGISTRATION_CONFIRMATION,
          NEW_STUDY_DIGEST ->
          false;
    };
  }

  private static final List<String> KINDS_BY_TYPE_INT = byTypeInt(type -> of(type).name());
  private static final List<String> ROLES_BY_TYPE_INT = byTypeInt(EmailReference::role);

  /** Each type's reference kind at the position Postgres reads as {@code kinds[type number]}. */
  public static List<String> kindsByTypeInt() {
    return KINDS_BY_TYPE_INT;
  }

  /**
   * Each type's role at its number: APPROVAL, PROGRESS_REPORT, PROGRESS_REPORT_APPROVAL or null.
   */
  public static List<String> rolesByTypeInt() {
    return ROLES_BY_TYPE_INT;
  }

  private static String role(EmailType type) {
    if (isApproval(type)) {
      return isAboutProgressReport(type) ? "PROGRESS_REPORT_APPROVAL" : "APPROVAL";
    }
    return isAboutProgressReport(type) ? "PROGRESS_REPORT" : null;
  }

  private static List<String> byTypeInt(Function<EmailType, String> value) {
    EmailType[] types = EmailType.values();
    int max = Stream.of(types).mapToInt(EmailType::getTypeInt).max().orElse(0);
    String[] values = new String[max];
    for (EmailType type : types) {
      values[type.getTypeInt() - 1] = value.apply(type);
    }
    return Collections.unmodifiableList(Arrays.asList(values));
  }
}
