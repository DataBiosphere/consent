package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/* WARNING: This test class makes use of underlying Java Cryptographic functions.  If these cryptographic operations were to reach production code and customer requirements dictate, this code would need to be running in a FIPS compliant JVM.
 */
class UserSessionTest extends DAOTestHelper {

  // ---------------------------------------------------------------------------
  // sync_session_idp — BEFORE INSERT OR UPDATE
  // ---------------------------------------------------------------------------

  @Test
  void syncIdp_populatesIdpColumnOnInsert() {
    String sid = UUID.randomUUID().toString();
    insertSession(sid, "{\"idp\":\"google\"}");

    Map<String, Object> row = querySession(sid).orElseThrow();
    assertEquals("google", row.get("idp"));
  }

  @Test
  void syncIdp_updatesIdpColumnWhenSessJsonChanges() {
    String sid = UUID.randomUUID().toString();
    insertSession(sid, "{\"idp\":\"google\"}");

    updateSessionSess(sid, "{\"idp\":\"azure\"}");

    assertEquals("azure", querySession(sid).orElseThrow().get("idp"));
  }

  @Test
  void syncIdp_nullWhenIdpAbsentFromSessJson() {
    String sid = UUID.randomUUID().toString();
    insertSession(sid, "{}");

    assertNull(querySession(sid).orElseThrow().get("idp"));
  }

  // ---------------------------------------------------------------------------
  // audit_session_start — AFTER INSERT
  // ---------------------------------------------------------------------------

  @Test
  void auditStart_createsAuditRowOnInsert() {
    String sid = UUID.randomUUID().toString();
    insertSession(sid, "{\"idp\":\"google\",\"userId\":\"researcher@example.com\"}");

    Map<String, Object> audit = queryAuditBySid(sid).orElseThrow();
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

    Map<String, Object> audit = queryAuditBySid(sid).orElseThrow();
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

    Map<String, Object> auditBefore = queryAuditBySid(sid).orElseThrow();
    assertNull(auditBefore.get("user_email"));
    assertNull(auditBefore.get("idp"));

    // Phase 2: OAuth callback — sess updated with userId and idp.
    updateSessionSess(sid, "{\"userId\":\"researcher@example.com\",\"idp\":\"google\"}");

    Map<String, Object> auditAfter = queryAuditBySid(sid).orElseThrow();
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

    Map<String, Object> audit = queryAuditBySid(sid).orElseThrow();
    assertEquals("first@example.com", audit.get("user_email"));
    assertEquals("google", audit.get("idp"));
  }

  @Test
  void auditUpdate_rotationClosesTheAuditRowAndOpensANewOne() {
    // @fastify/session 11.1.6 keeps the row on regenerate() and records a new
    // generation. A row written before 11.1.6 has none, so its generation is the sid.
    String sid = UUID.randomUUID().toString();
    insertSession(sid, "{\"idp\":\"google\"}");

    updateSessionSess(
        sid,
        "{\"__fastifySessionMetadata\":{\"generation\":\"gen-2\"},\"userId\":\"researcher@example.com\",\"idp\":\"google\"}");

    List<Map<String, Object>> audits = queryAuditsBySid(sid);
    assertEquals(2, audits.size());
    assertNotNull(audits.get(0).get("ended_at"));
    assertEquals("rotated", audits.get(0).get("end_reason"));
    assertNull(audits.get(0).get("user_email"));
    assertNull(audits.get(1).get("ended_at"));
    assertEquals("researcher@example.com", audits.get(1).get("user_email"));
    assertEquals("google", audits.get(1).get("idp"));
  }

  @Test
  void auditUpdate_signInRotatesToAnEmptySessionThenBackfillsTheUser() {
    // The BFF's sign-in saves the regenerated, empty session first, then writes
    // the user's fields with the same generation (duos-ui session/rotation.ts).
    String sid = UUID.randomUUID().toString();
    insertSession(sid, "{}");
    String metadata = "\"__fastifySessionMetadata\":{\"generation\":\"gen-2\"}";

    updateSessionSess(sid, "{" + metadata + "}");
    updateSessionSess(
        sid, "{" + metadata + ",\"userId\":\"researcher@example.com\",\"idp\":\"google\"}");

    List<Map<String, Object>> audits = queryAuditsBySid(sid);
    assertEquals(2, audits.size());
    assertEquals("rotated", audits.get(0).get("end_reason"));
    assertEquals("researcher@example.com", audits.get(1).get("user_email"));
    assertEquals("google", audits.get(1).get("idp"));
    assertNull(audits.get(1).get("ended_at"));
  }

  @Test
  void auditUpdate_secondSignInOnTheSameRowRecordsTheNewUser() {
    String sid = UUID.randomUUID().toString();
    insertSession(
        sid,
        "{\"__fastifySessionMetadata\":{\"generation\":\"gen-1\"},\"userId\":\"first@example.com\",\"idp\":\"google\"}");

    updateSessionSess(
        sid,
        "{\"__fastifySessionMetadata\":{\"generation\":\"gen-2\"},\"userId\":\"second@example.com\",\"idp\":\"azure\"}");

    List<Map<String, Object>> audits = queryAuditsBySid(sid);
    assertEquals(2, audits.size());
    assertEquals("first@example.com", audits.get(0).get("user_email"));
    assertEquals("rotated", audits.get(0).get("end_reason"));
    assertEquals("second@example.com", audits.get(1).get("user_email"));
    assertEquals("azure", audits.get(1).get("idp"));
    assertNull(audits.get(1).get("ended_at"));
  }

  @Test
  void auditUpdate_sameGenerationOnlyBackfills() {
    // A token refresh saves the session without regenerating it.
    String sid = UUID.randomUUID().toString();
    String metadata = "\"__fastifySessionMetadata\":{\"generation\":\"gen-1\"}";
    insertSession(sid, "{" + metadata + "}");

    updateSessionSess(
        sid, "{" + metadata + ",\"userId\":\"researcher@example.com\",\"idp\":\"google\"}");

    List<Map<String, Object>> audits = queryAuditsBySid(sid);
    assertEquals(1, audits.size());
    assertEquals("researcher@example.com", audits.get(0).get("user_email"));
    assertNull(audits.get(0).get("ended_at"));
  }

  @Test
  void auditEnd_afterRotationClosesOnlyTheOpenRow() {
    String sid = UUID.randomUUID().toString();
    insertSession(sid, "{}");
    updateSessionSess(
        sid,
        "{\"__fastifySessionMetadata\":{\"generation\":\"gen-2\"},\"userId\":\"researcher@example.com\"}");
    stampAuditEndReason(sid);

    deleteSession(sid);

    List<Map<String, Object>> audits = queryAuditsBySid(sid);
    assertEquals(2, audits.size());
    assertEquals("rotated", audits.get(0).get("end_reason"));
    assertEquals("logout", audits.get(1).get("end_reason"));
    assertNotNull(audits.get(1).get("ended_at"));
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

    Map<String, Object> audit = queryAuditBySid(sid).orElseThrow();
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

    Map<String, Object> audit = queryAuditBySid(sid).orElseThrow();
    assertNotNull(audit.get("ended_at"));
    assertEquals("logout", audit.get("end_reason"));
  }

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

  private Optional<Map<String, Object>> querySession(String sid) {
    return jdbi.withHandle(
        h ->
            h.createQuery("SELECT * FROM user_sessions WHERE sid = :sid")
                .bind("sid", sid)
                .mapToMap()
                .findOne());
  }

  private Optional<Map<String, Object>> queryAuditBySid(String sid) {
    return jdbi.withHandle(
        h ->
            h.createQuery("SELECT * FROM user_session_audit WHERE sid_hash = :hash")
                .bind("hash", sha256Hex(sid))
                .mapToMap()
                .findOne());
  }

  private List<Map<String, Object>> queryAuditsBySid(String sid) {
    return jdbi.withHandle(
        h ->
            h.createQuery("SELECT * FROM user_session_audit WHERE sid_hash = :hash ORDER BY id")
                .bind("hash", sha256Hex(sid))
                .mapToMap()
                .list());
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
}
