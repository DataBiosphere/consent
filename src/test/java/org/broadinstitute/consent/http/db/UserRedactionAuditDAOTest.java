package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.broadinstitute.consent.http.models.DataAccessRequestData;
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

  private String insertSubmittedDar(User user) {
    Date now = new Date();
    Integer collectionId =
        darCollectionDAO.insertDarCollection("DAR-" + UUID.randomUUID(), user.getUserId(), now);
    String referenceId = UUID.randomUUID().toString();
    dataAccessRequestDAO.insertDataAccessRequest(
        collectionId,
        referenceId,
        user.getUserId(),
        now,
        now,
        now,
        new DataAccessRequestData(),
        null);
    dataAccessRequestDAO.updateSubmissionInstitution(referenceId, user.getInstitutionId());
    return referenceId;
  }

  private Map<String, Object> queryDarInstitution(String referenceId) {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery(
                    "SELECT institution_id, institution_name, institution_snapshot_date FROM data_access_request WHERE reference_id = :referenceId")
                .bind("referenceId", referenceId)
                .mapToMap()
                .one());
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
    assertNotNull(target.getInstitutionId());

    userRedactionAuditDAO.redactUser(target.getUserId(), admin.getUserId());

    User redacted = userDAO.findUserById(target.getUserId());
    assertEquals(redactedEmail(target.getUserId()), redacted.getEmail());
    assertEquals("redacted", redacted.getDisplayName());
    assertNull(redacted.getInstitutionId());
  }

  @Test
  void testRedactUser_disablesEmailPreference() {
    User target = createUserWithInstitution();
    User admin = createUser();
    userDAO.updateEmailPreference(target.getUserId(), true);

    userRedactionAuditDAO.redactUser(target.getUserId(), admin.getUserId());

    User redacted = userDAO.findUserById(target.getUserId());
    assertEquals(Boolean.FALSE, redacted.getEmailPreference());
  }

  @Test
  void testRedactUser_insertsAuditRow() {
    User target = createUserWithInstitution();
    User admin = createUser();
    String originalEmail = target.getEmail();
    String originalDisplayName = target.getDisplayName();
    Integer originalInstitutionId = target.getInstitutionId();

    userRedactionAuditDAO.redactUser(target.getUserId(), admin.getUserId());

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

    userRedactionAuditDAO.redactUser(target.getUserId(), admin.getUserId());

    List<Map<String, Object>> rows = queryAuditRows(target.getUserId());
    assertEquals(1, rows.size());
    Map<String, Object> row = rows.getFirst();
    assertEquals(originalEmail, row.get("original_email"));
    assertTrue(row.containsKey("original_display_name"));
    assertTrue(row.containsKey("original_institution_id"));
  }

  @Test
  void testRedactUser_nullInstitutionId_usersRowRemainsNull() {
    User target = createUser();
    User admin = createUser();
    assertNull(target.getInstitutionId());

    userRedactionAuditDAO.redactUser(target.getUserId(), admin.getUserId());

    User redacted = userDAO.findUserById(target.getUserId());
    assertNull(redacted.getInstitutionId());
  }

  @Test
  void testRedactUser_nullInstitutionId_auditRowRecordsNull() {
    User target = createUser();
    User admin = createUser();
    assertNull(target.getInstitutionId());

    userRedactionAuditDAO.redactUser(target.getUserId(), admin.getUserId());

    List<Map<String, Object>> rows = queryAuditRows(target.getUserId());
    assertEquals(1, rows.size());
    assertTrue(rows.getFirst().containsKey("original_institution_id"));
    assertNull(rows.getFirst().get("original_institution_id"));
  }

  @Test
  void testRedactUser_idempotent_multipleRedactionsCreateMultipleAuditRows() {
    User target = createUser();
    User admin = createUser();

    userRedactionAuditDAO.redactUser(target.getUserId(), admin.getUserId());
    userRedactionAuditDAO.redactUser(target.getUserId(), admin.getUserId());

    List<Map<String, Object>> rows = queryAuditRows(target.getUserId());
    assertEquals(2, rows.size());
  }

  @Test
  void testRedactUser_noMatchingUser_insertsNoAuditRow() {
    User admin = createUser();
    int nonExistentUserId = Integer.MAX_VALUE;

    // The original CTE returns no rows so the UPDATE and INSERT are both no-ops.
    userRedactionAuditDAO.redactUser(nonExistentUserId, admin.getUserId());

    List<Map<String, Object>> rows = queryAuditRows(nonExistentUserId);
    assertTrue(rows.isEmpty());
  }

  @Test
  void testRedactUser_clearsInstitutionRecordedOnOwnDarsOnly() {
    User target = createUserWithInstitution();
    User other = createUserWithInstitution();
    User admin = createUser();
    String targetDar = insertSubmittedDar(target);
    String otherDar = insertSubmittedDar(other);

    userRedactionAuditDAO.redactUser(target.getUserId(), admin.getUserId());

    Map<String, Object> redacted = queryDarInstitution(targetDar);
    assertNull(redacted.get("institution_id"));
    assertNull(redacted.get("institution_name"));
    assertNotNull(redacted.get("institution_snapshot_date"));
    assertEquals(
        other.getInstitutionId().longValue(), queryDarInstitution(otherDar).get("institution_id"));
  }
}
