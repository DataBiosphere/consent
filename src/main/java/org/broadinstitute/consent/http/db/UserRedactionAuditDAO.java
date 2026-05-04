package org.broadinstitute.consent.http.db;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

public interface UserRedactionAuditDAO {

  /**
   * Redact a user's PII and write an audit record in a single atomic SQL statement.
   *
   * <p>The statement uses two writeable CTEs:
   *
   * <ol>
   *   <li>{@code original} — SELECT … FOR UPDATE locks and captures the current row values so the
   *       database is the sole source of truth for what is being redacted.
   *   <li>{@code update_user} — UPDATE … FROM original performs the redaction; because it joins
   *       through {@code original}, it is automatically a no-op when the user does not exist.
   * </ol>
   *
   * The outer INSERT selects from both CTEs, so all three operations (lock, update, audit) are
   * atomic and no PII is passed through the application layer.
   *
   * @param userId the user_id of the account being redacted
   * @param adminUserId the user_id of the admin performing the redaction
   */
  @SqlUpdate(
      """
      WITH original AS (
        SELECT email, display_name, institution_id
        FROM   users
        WHERE  user_id = :userId
        FOR UPDATE
      ),
      update_user AS (
        UPDATE users
        SET
          email            = 'redacted_' || encode(:userId::text::bytea, 'base64'),
          display_name     = 'redacted',
          institution_id   = NULL,
          email_preference = false
        FROM   original
        WHERE  users.user_id = :userId
        RETURNING users.user_id
      )
      INSERT INTO user_redaction_audit
        (user_id, admin_user_id, original_email, original_display_name, original_institution_id, action_date)
      SELECT :userId, :adminUserId, o.email, o.display_name, o.institution_id, NOW()
      FROM   update_user
      JOIN   original o ON true
      """)
  void redactUser(@Bind("userId") Integer userId, @Bind("adminUserId") Integer adminUserId);
}
