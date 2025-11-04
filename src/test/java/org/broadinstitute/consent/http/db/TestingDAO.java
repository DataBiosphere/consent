package org.broadinstitute.consent.http.db;

import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.transaction.Transactional;

public interface TestingDAO extends Transactional<TestingDAO> {

  @SqlUpdate(
      """
      DO $$ DECLARE
          table_name text;
      BEGIN
          FOR table_name IN (SELECT tablename FROM pg_tables WHERE schemaname='public'
                AND tablename NOT IN ('roles', 'dictionary', 'dac_automation_rules')) LOOP
              EXECUTE 'TRUNCATE TABLE ' || table_name || ' CASCADE;';
          END LOOP;
      END $$;
      """)
  void truncateAllTables();
}
