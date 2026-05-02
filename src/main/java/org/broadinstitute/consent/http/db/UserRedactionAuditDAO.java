package org.broadinstitute.consent.http.db;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

public interface UserRedactionAuditDAO {

  /**
   * Redact a user's PII and write an audit record in a single atomic SQL statement.
   *
   * <p>A writeable CTE first updates the {@code users} row, replacing the email with {@code
   * 'redacted_' || encode(user_id::text::bytea, 'base64')} (the same pattern used in the 2026-03-05
   * FK migration), setting display_name to {@code 'redacted'}, and nulling institution_id. The
   * outer INSERT then writes the audit row, selecting from the CTE so that if the UPDATE matches no
   * rows the INSERT is also a no-op.
   *
   * @param userId the user_id of the account being redacted
   * @param adminUserId the user_id of the admin performing the redaction
   * @param originalEmail the email address captured before redaction
   * @param originalDisplayName the display_name captured before redaction (nullable)
   * @param originalInstitutionId the institution_id captured before redaction (nullable)
   */
  @SqlUpdate(
      """
      WITH update_user AS (
        UPDATE users
        SET
          email          = 'redacted_' || encode(:userId::text::bytea, 'base64'),
          display_name   = 'redacted',
          institution_id = NULL
        WHERE user_id = :userId
        RETURNING user_id
      )
      INSERT INTO user_redaction_audit
        (user_id, admin_user_id, original_email, original_display_name, original_institution_id, action_date)
      SELECT :userId, :adminUserId, :originalEmail, :originalDisplayName, :originalInstitutionId, NOW()
      FROM update_user
      """)
  void redactUser(
      @Bind("userId") Integer userId,
      @Bind("adminUserId") Integer adminUserId,
      @Bind("originalEmail") String originalEmail,
      @Bind("originalDisplayName") String originalDisplayName,
      @Bind("originalInstitutionId") Integer originalInstitutionId);
}
