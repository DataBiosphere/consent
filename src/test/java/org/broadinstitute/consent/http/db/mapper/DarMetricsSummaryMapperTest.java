package org.broadinstitute.consent.http.db.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.broadinstitute.consent.http.db.DAOTestHelper;
import org.broadinstitute.consent.http.models.DarMetricsSummary;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * The lapsed flag compares the end of access with the {@code as_of} column, which the queries read
 * from the database clock (DT-4553). Each {@code as_of} here is far from the Java clock, so a
 * mapper that reads the Java clock gives the wrong flag. The rows are literal values, so no table
 * is needed. Synthetic values only.
 */
class DarMetricsSummaryMapperTest extends DAOTestHelper {

  private static final String NO_CLOSEOUT = "";

  @ParameterizedTest(name = "{4}")
  @CsvSource(
      delimiter = '|',
      textBlock =
          """
          2000-01-01 00:00:00+00 | | 2000-06-01 00:00:00+00 | false | a term that had not run out as of a past date
          2098-01-01 00:00:00+00 | | 2100-01-01 00:00:00+00 | true  | a term that had run out as of a future date
          2000-01-01 00:00:00+00 | 2000-03-01 00:00:00+00 | 2000-02-01 00:00:00+00 | false | a closeout later than the as-of date
          2098-01-01 00:00:00+00 | 2098-03-01 00:00:00+00 | 2098-03-02 00:00:00+00 | true  | a closeout earlier than the as-of date
          2000-01-01 00:00:00+00 | 2000-03-01 00:00:00.000002+00 | 2000-03-01 00:00:00.000001+00 | false | a closeout one microsecond after the as-of date
          2098-01-01 00:00:00+00 | 2098-03-01 00:00:00.000001+00 | 2098-03-01 00:00:00.000002+00 | true  | a closeout one microsecond before the as-of date
          2098-01-01 00:00:00+00 | 2098-03-01 00:00:00+00 | 2098-03-01 00:00:00+00 | true  | a closeout at the as-of date
          2098-01-01 00:00:00+00 | | 2099-01-01 00:00:00+00 | true  | a term end at the as-of date
          """)
  void testLapsedComesFromTheAsOfColumn(
      String expirationBasis, String closeout, String asOf, boolean expected, String description) {
    DarMetricsSummary summary =
        map(expirationBasis, closeout == null ? NO_CLOSEOUT : closeout, asOf);

    assertEquals(expected, summary.expired(), description);
  }

  private DarMetricsSummary map(String expirationBasis, String closeout, String asOf) {
    String sql =
        """
        SELECT CAST(:expirationBasis AS timestamptz) AS update_date,
            CAST(:expirationBasis AS timestamptz) AS submission_date,
            'Example Project' AS project_title, 'DAR-1' AS dar_code,
            'Example summary' AS non_tech_rus, 'Example statement' AS rus,
            'example-reference-id' AS reference_id, 'Example Institution' AS institution_name,
            CAST(NULL AS timestamptz) AS approval_date,
            CAST(NULL AS timestamptz[]) AS progress_report_approval_dates,
            CAST(:expirationBasis AS timestamptz) AS expiration_basis_date,
            CAST(NULLIF(:closeout, '') AS timestamptz) AS closeout_date,
            CAST(:asOf AS timestamptz) AS as_of
        """;
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery(sql)
                .bind("expirationBasis", expirationBasis)
                .bind("closeout", closeout)
                .bind("asOf", asOf)
                .map(new DarMetricsSummaryMapper())
                .one());
  }
}
