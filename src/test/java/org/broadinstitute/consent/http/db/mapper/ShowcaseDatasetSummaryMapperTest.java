package org.broadinstitute.consent.http.db.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.broadinstitute.consent.http.db.DAOTestHelper;
import org.broadinstitute.consent.http.models.ShowcaseDatasetSummary;
import org.junit.jupiter.api.Test;

/**
 * The showcase available date for both a {@code timestamp} and a {@code timestamptz} column
 * (DT-4452). The rows are literal values, so no table is needed. Synthetic values only.
 */
class ShowcaseDatasetSummaryMapperTest extends DAOTestHelper {

  /** A {@code timestamp} value has no zone, so its own date is the available date. */
  @Test
  void testTimestampColumnGivesItsOwnDate() {
    assertEquals(
        "2026-10-02", availableDate("CAST('2026-10-02 23:30:00' AS timestamp)").availableDate());
  }

  /** A {@code timestamptz} value is one moment, so the available date is its UTC date. */
  @Test
  void testTimestamptzColumnGivesItsUtcDate() {
    assertEquals(
        "2026-10-03",
        availableDate("CAST('2026-10-02 23:30:00-05' AS timestamptz)").availableDate());
  }

  @Test
  void testNoDate() {
    assertNull(availableDate("CAST(NULL AS timestamptz)").availableDate());
  }

  private ShowcaseDatasetSummary availableDate(String dateExpression) {
    String sql =
        """
        SELECT 1 AS alias, 'Example Dataset' AS name, NULL AS dac_name,
            '{"generalUse": true}' AS data_use, 'open' AS access_management,
            %s AS available_date,
            NULL AS dataset_properties, NULL AS study_properties
        """
            .formatted(dateExpression);
    return jdbi.withHandle(
        handle -> handle.createQuery(sql).map(new ShowcaseDatasetSummaryMapper()).one());
  }
}
