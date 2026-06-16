package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UserSessionTest extends DAOTestHelper {

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

  /** Mirrors encode(sha256(sid::bytea), 'hex') used inside the trigger functions. */
  private static String sha256Hex(String value) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
      StringBuilder hex = new StringBuilder(hash.length * 2);
      for (byte b : hash) {
        hex.append(String.format("%02x", b));
      }
      return hex.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  private void insertSession(String sid, String sessJson) {
    jdbi.useHandle(
        h ->
            h.execute(
                "INSERT INTO user_sessions (sid, sess, expire) VALUES (?, ?::json, NOW() + INTERVAL '8 hours')",
                sid,
                sessJson));
  }

  private void updateSessionSess(String sid, String sessJson) {
    jdbi.useHandle(
        h -> h.execute("UPDATE user_sessions SET sess = ?::json WHERE sid = ?", sessJson, sid));
  }

  private void deleteSession(String sid) {
    jdbi.useHandle(h -> h.execute("DELETE FROM user_sessions WHERE sid = ?", sid));
  }

  private Map<String, Object> querySession(String sid) {
    return jdbi.withHandle(
        h ->
            Objects.requireNonNull(
                h.createQuery("SELECT * FROM user_sessions WHERE sid = :sid")
                    .bind("sid", sid)
                    .mapToMap()
                    .findOne()
                    .orElse(null)));
  }

  private Map<String, Object> queryAuditBySid(String sid) {
    return jdbi.withHandle(
        h ->
            Objects.requireNonNull(
                h.createQuery("SELECT * FROM user_session_audit WHERE sid_hash = :hash")
                    .bind("hash", sha256Hex(sid))
                    .mapToMap()
                    .findOne()
                    .orElse(null)));
  }

  /** Simulates the Phase 2 logout handler stamping end_reason before session.destroy(). */
  private void stampAuditEndReason(String sid) {
    jdbi.useHandle(
        h ->
            h.execute(
                "UPDATE user_session_audit SET end_reason = ? WHERE sid_hash = ? AND ended_at IS NULL",
                "logout",
                sha256Hex(sid)));
  }

  // ---------------------------------------------------------------------------
  // sync_session_idp — BEFORE INSERT OR UPDATE
  // ---------------------------------------------------------------------------

  @Test
  void syncIdp_populatesIdpColumnOnInsert() {
    String sid = UUID.randomUUID().toString();
    insertSession(sid, "{\"idp\":\"google\"}");

    Map<String, Object> row = querySession(sid);
    assertNotNull(row);
    assertEquals("google", row.get("idp"));
  }

  @Test
  void syncIdp_updatesIdpColumnWhenSessJsonChanges() {
    String sid = UUID.randomUUID().toString();
    insertSession(sid, "{\"idp\":\"google\"}");

    updateSessionSess(sid, "{\"idp\":\"azure\"}");

    assertEquals("azure", querySession(sid).get("idp"));
  }

  @Test
  void syncIdp_nullWhenIdpAbsentFromSessJson() {
    String sid = UUID.randomUUID().toString();
    insertSession(sid, "{}");

    assertNull(querySession(sid).get("idp"));
  }

  // ---------------------------------------------------------------------------
  // audit_session_start — AFTER INSERT
  // ---------------------------------------------------------------------------

  @Test
  void auditStart_createsAuditRowOnInsert() {
    String sid = UUID.randomUUID().toString();
    insertSession(sid, "{\"idp\":\"google\",\"userId\":\"researcher@example.com\"}");

    Map<String, Object> audit = queryAuditBySid(sid);
    assertNotNull(audit);
    assertEquals(sha256Hex(sid), audit.get("sid_hash"));
    assertEquals("researcher@example.com", audit.get("user_email"));
    assertEquals("google", audit.get("idp"));
    assertNotNull(audit.get("created_at"));
    assertNull(audit.get("ended_at"));
    assertNull(audit.get("end_reason"));
  }

  @Test
  void auditStart_nullEmailWhenUserIdAbsentFromSessJson() {
    // Simulates PKCE initiation: the session row is created before the OAuth
    // callback populates userId, so the audit row opens with a null user_email.
    String sid = UUID.randomUUID().toString();
    insertSession(sid, "{\"idp\":\"azure\"}");

    Map<String, Object> audit = queryAuditBySid(sid);
    assertNotNull(audit);
    assertNull(audit.get("user_email"));
    assertEquals("azure", audit.get("idp"));
  }

  // ---------------------------------------------------------------------------
  // audit_session_update — AFTER UPDATE
  // ---------------------------------------------------------------------------

  @Test
  void auditUpdate_backfillsEmailAndIdpAfterOAuthCallback() {
    // Phase 1: PKCE initiation — session inserted without userId or idp.
    String sid = UUID.randomUUID().toString();
    insertSession(sid, "{}");

    Map<String, Object> auditBefore = queryAuditBySid(sid);
    assertNull(auditBefore.get("user_email"));
    assertNull(auditBefore.get("idp"));

    // Phase 2: OAuth callback — sess updated with userId and idp.
    updateSessionSess(sid, "{\"userId\":\"researcher@example.com\",\"idp\":\"google\"}");

    Map<String, Object> auditAfter = queryAuditBySid(sid);
    assertEquals("researcher@example.com", auditAfter.get("user_email"));
    assertEquals("google", auditAfter.get("idp"));
  }

  @Test
  void auditUpdate_doesNotOverwriteEmailOrIdpAlreadySet() {
    // COALESCE means once user_email and idp are recorded they cannot be
    // changed by a subsequent session update (e.g. a rolling-expiry touch).
    String sid = UUID.randomUUID().toString();
    insertSession(sid, "{\"userId\":\"first@example.com\",\"idp\":\"google\"}");

    updateSessionSess(sid, "{\"userId\":\"second@example.com\",\"idp\":\"azure\"}");

    Map<String, Object> audit = queryAuditBySid(sid);
    assertEquals("first@example.com", audit.get("user_email"));
    assertEquals("google", audit.get("idp"));
  }

  // ---------------------------------------------------------------------------
  // audit_session_end — AFTER DELETE
  // ---------------------------------------------------------------------------

  @Test
  void auditEnd_stampsEndedAtWithExpiredReasonOnDelete() {
    String sid = UUID.randomUUID().toString();
    insertSession(
        sid,
        """
        {"idp":"google","userId":"researcher@example.com"}
        """);

    deleteSession(sid);

    Map<String, Object> audit = queryAuditBySid(sid);
    assertNotNull(audit.get("ended_at"));
    assertEquals("expired", audit.get("end_reason"));
  }

  @Test
  void auditEnd_preservesLogoutReasonSetByLogoutHandlerBeforeDelete() {
    // The Phase 2 logout handler stamps end_reason = 'logout' on the audit row
    // before calling session.destroy().  The AFTER DELETE trigger's COALESCE
    // must not overwrite it with 'expired'.
    String sid = UUID.randomUUID().toString();
    insertSession(
        sid,
        """
        {"idp":"google","userId":"researcher@example.com"}
        """);
    stampAuditEndReason(sid);

    deleteSession(sid);

    Map<String, Object> audit = queryAuditBySid(sid);
    assertNotNull(audit.get("ended_at"));
    assertEquals("logout", audit.get("end_reason"));
  }
}
