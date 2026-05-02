package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.broadinstitute.consent.http.models.User;
import org.junit.jupiter.api.Test;

class UserRedactionAuditDAOTest extends DAOTestHelper {

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

  /** Computes the expected redacted email using the same formula as the CTE. */
  private static String redactedEmail(Integer userId) {
    return "redacted_"
        + Base64.getEncoder()
            .encodeToString(String.valueOf(userId).getBytes(StandardCharsets.UTF_8));
  }

  private List<Map<String, Object>> queryAuditRows(Integer userId) {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery(
                    "SELECT * FROM user_redaction_audit WHERE user_id = :userId ORDER BY id")
                .bind("userId", userId)
                .mapToMap()
                .list());
  }

  // ---------------------------------------------------------------------------
  // Tests
  // ---------------------------------------------------------------------------

  @Test
  void testRedactUser_updatesEmailDisplayNameAndNullsInstitutionId() {
    User target = createUserWithInstitution();
    User admin = createUser();
    String originalEmail = target.getEmail();
    assertNotNull(target.getInstitutionId());

    userRedactionAuditDAO.redactUser(
        target.getUserId(),
        admin.getUserId(),
        originalEmail,
        target.getDisplayName(),
        target.getInstitutionId());

    User redacted = userDAO.findUserById(target.getUserId());
    assertEquals(redactedEmail(target.getUserId()), redacted.getEmail());
    assertEquals("redacted", redacted.getDisplayName());
    assertNull(redacted.getInstitutionId());
  }

  @Test
  void testRedactUser_insertsAuditRow() {
    User target = createUserWithInstitution();
    User admin = createUser();
    String originalEmail = target.getEmail();
    String originalDisplayName = target.getDisplayName();
    Integer originalInstitutionId = target.getInstitutionId();

    userRedactionAuditDAO.redactUser(
        target.getUserId(),
        admin.getUserId(),
        originalEmail,
        originalDisplayName,
        originalInstitutionId);

    List<Map<String, Object>> rows = queryAuditRows(target.getUserId());
    assertEquals(1, rows.size());
    Map<String, Object> row = rows.getFirst();
    assertEquals(target.getUserId().longValue(), row.get("user_id"));
    assertEquals(admin.getUserId().longValue(), row.get("admin_user_id"));
    assertEquals(originalEmail, row.get("original_email"));
    assertEquals(originalDisplayName, row.get("original_display_name"));
    assertEquals(originalInstitutionId.longValue(), row.get("original_institution_id"));
    assertNotNull(row.get("action_date"));
  }

  @Test
  void testRedactUser_nullDisplayName_insertsAuditRow() {
    User target = createUser();
    User admin = createUser();
    String originalEmail = target.getEmail();

    userRedactionAuditDAO.redactUser(
        target.getUserId(), admin.getUserId(), originalEmail, null, null);

    List<Map<String, Object>> rows = queryAuditRows(target.getUserId());
    assertEquals(1, rows.size());
    Map<String, Object> row = rows.getFirst();
    assertEquals(originalEmail, row.get("original_email"));
    // nullable columns must be present in the row but contain null
    assertTrue(row.containsKey("original_display_name"));
    assertTrue(row.containsKey("original_institution_id"));
  }

  @Test
  void testRedactUser_nullInstitutionId_usersRowRemainsNull() {
    // createUser() produces a user with no institution; institution_id starts null.
    User target = createUser();
    User admin = createUser();
    assertNull(target.getInstitutionId());

    userRedactionAuditDAO.redactUser(
        target.getUserId(), admin.getUserId(), target.getEmail(), target.getDisplayName(), null);

    User redacted = userDAO.findUserById(target.getUserId());
    assertNull(redacted.getInstitutionId());
  }

  @Test
  void testRedactUser_nullInstitutionId_auditRowRecordsNull() {
    User target = createUser();
    User admin = createUser();
    assertNull(target.getInstitutionId());

    userRedactionAuditDAO.redactUser(
        target.getUserId(), admin.getUserId(), target.getEmail(), target.getDisplayName(), null);

    List<Map<String, Object>> rows = queryAuditRows(target.getUserId());
    assertEquals(1, rows.size());
    // original_institution_id column must exist and be null
    assertTrue(rows.getFirst().containsKey("original_institution_id"));
    assertNull(rows.getFirst().get("original_institution_id"));
  }

  @Test
  void testRedactUser_idempotent_multipleRedactionsCreateMultipleAuditRows() {
    User target = createUser();
    User admin = createUser();
    String originalEmail = target.getEmail();

    userRedactionAuditDAO.redactUser(
        target.getUserId(), admin.getUserId(), originalEmail, target.getDisplayName(), null);

    // A second call (e.g. replayed or re-triggered by an operator) should produce
    // another audit row but leave the users row unchanged.
    String alreadyRedactedEmail = redactedEmail(target.getUserId());
    userRedactionAuditDAO.redactUser(
        target.getUserId(), admin.getUserId(), alreadyRedactedEmail, "redacted", null);

    List<Map<String, Object>> rows = queryAuditRows(target.getUserId());
    assertEquals(2, rows.size());
  }

  @Test
  void testRedactUser_noMatchingUser_insertsNoAuditRow() {
    User admin = createUser();
    int nonExistentUserId = Integer.MAX_VALUE;

    // The CTE UPDATE matches no row, so the outer INSERT via SELECT FROM update_user is a no-op.
    userRedactionAuditDAO.redactUser(
        nonExistentUserId, admin.getUserId(), "ghost@example.com", "Ghost", null);

    List<Map<String, Object>> rows = queryAuditRows(nonExistentUserId);
    assertTrue(rows.isEmpty());
  }
}
