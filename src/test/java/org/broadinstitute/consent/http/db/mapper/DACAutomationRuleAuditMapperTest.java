package org.broadinstitute.consent.http.db.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Timestamp;
import java.time.Instant;
import org.broadinstitute.consent.http.db.DAOTestHelper;
import org.broadinstitute.consent.http.rules.DACAutomationRuleAudit;
import org.junit.jupiter.api.Test;

/**
 * The rule audit action date for both a {@code timestamp} and a {@code timestamptz} column
 * (DT-4452). The rows are literal values, so no table is needed. Synthetic values only.
 */
class DACAutomationRuleAuditMapperTest extends DAOTestHelper {

  /**
   * A {@code timestamp} value is read in the JVM time zone, the same as the earlier {@code
   * Timestamp.valueOf} read of its text.
   */
  @Test
  void testTimestampColumn() {
    assertEquals(
        Timestamp.valueOf("2026-10-02 23:30:00").toInstant(),
        actionDate("CAST('2026-10-02 23:30:00' AS timestamp)"));
  }

  /** A {@code timestamptz} value keeps its moment, whatever its offset. */
  @Test
  void testTimestamptzColumn() {
    assertEquals(
        Instant.parse("2026-10-03T04:30:00Z"),
        actionDate("CAST('2026-10-02 23:30:00-05' AS timestamptz)"));
  }

  private Instant actionDate(String dateExpression) {
    String sql =
        """
        SELECT 'REMOVE' AS action, %s AS action_date,
            'AUTO_OPEN_DAR_FOR_ALL_MEMBERS' AS rule,
            'member@example.org' AS email, 'Example Member' AS display_name
        """
            .formatted(dateExpression);
    DACAutomationRuleAudit audit =
        jdbi.withHandle(
            handle -> handle.createQuery(sql).map(new DACAutomationRuleAuditMapper()).one());
    return audit.actionDate().toInstant();
  }
}
