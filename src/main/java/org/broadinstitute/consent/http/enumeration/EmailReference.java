package org.broadinstitute.consent.http.enumeration;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/** What an email type stores as its {@code email_entity.entity_reference_id}. */
public enum EmailReference {
  DAR_CODE,
  DAR_REFERENCE_ID,
  ELECTION_ID,
  DUOS_ID,
  DATASET_NAME,
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
          NEW_STUDY_REGISTRATION_CONFIRMATION,
          NEW_STUDY_DIGEST ->
          NONE;
    };
  }

  private static final List<String> KINDS_BY_TYPE_INT = kindsByTypeInt(EmailType.values());

  /** Each type's reference kind at the position Postgres reads as {@code kinds[type number]}. */
  public static List<String> kindsByTypeInt() {
    return KINDS_BY_TYPE_INT;
  }

  private static List<String> kindsByTypeInt(EmailType[] types) {
    int max = Stream.of(types).mapToInt(EmailType::getTypeInt).max().orElse(0);
    String[] kinds = new String[max];
    for (EmailType type : types) {
      kinds[type.getTypeInt() - 1] = of(type).name();
    }
    return Collections.unmodifiableList(Arrays.asList(kinds));
  }
}
