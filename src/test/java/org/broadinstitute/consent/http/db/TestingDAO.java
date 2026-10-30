package org.broadinstitute.consent.http.db;

import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.transaction.Transactional;

public interface TestingDAO extends Transactional<TestingDAO> {

  // One TRUNCATE for all tables: this runs before every DAO test, and one statement per table
  // made it the largest cost in the test suite.
  @SqlUpdate(
      """
      DO $$ BEGIN
          EXECUTE (SELECT 'TRUNCATE TABLE ' || string_agg(quote_ident(tablename), ', ') || ' CASCADE'
                FROM pg_tables WHERE schemaname='public'
                AND tablename NOT IN ('roles', 'dictionary', 'dac_automation_rules'));
      END $$;
      """)
  void truncateAllTables();
}
