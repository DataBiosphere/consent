package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import liquibase.exception.LiquibaseException;
import org.junit.jupiter.api.Test;

/**
 * Runs against the schema the compatibility release actually left behind, by applying that
 * changelog first: the trigger this one removes is the thing under test, so a hand-built
 * approximation of it would not prove the removal.
 */
class DatasetAliasDefaultMigrationTest extends MigrationTestHelper {

  private static final String COMPATIBILITY_CHANGELOG =
      "changesets/changelog-consent-2026-08-10-dataset-alias-sequence.xml";
  private static final String CHANGELOG =
      "changesets/changelog-consent-2026-09-17-dataset-alias-default.xml";

  @Override
  protected String changelog() {
    return CHANGELOG;
  }

  @Override
  protected void createPreMigrationSchema() throws Exception {
    try (Connection connection = connection();
        Statement statement = connection.createStatement()) {
      statement.execute(
          "CREATE TABLE dataset (dataset_id bigserial PRIMARY KEY, alias numeric DEFAULT 0)");
      statement.execute("INSERT INTO dataset (alias) VALUES (42), (900000), (0)");
    }
    update(COMPATIBILITY_CHANGELOG);
  }

  @Test
  void migrationKeepsAllocatingFromTheSequenceOnceTheTriggerIsGone() throws Exception {
    update();

    assertFalse(triggerExists());
    assertFalse(functionExists());
    assertEquals(900001, queryLong("INSERT INTO dataset DEFAULT VALUES RETURNING alias"));
    assertEquals(900002, queryLong("INSERT INTO dataset DEFAULT VALUES RETURNING alias"));
    // Aliases stay public identifiers: uniqueness and the integer bound still hold.
    assertThrows(SQLException.class, () -> execute("INSERT INTO dataset (alias) VALUES (900001)"));
    assertThrows(
        SQLException.class, () -> execute("INSERT INTO dataset (alias) VALUES (2147483648)"));
  }

  @Test
  void migrationStopsOverridingAnExplicitlySuppliedAlias() throws Exception {
    update();

    // The trigger replaced supplied values; the default only fills an omitted one. Nothing in the
    // application supplies an alias, so this documents the behaviour change rather than relying
    // on it.
    assertEquals(7, queryLong("INSERT INTO dataset (alias) VALUES (7) RETURNING alias"));
    assertEquals(900001, queryLong("INSERT INTO dataset DEFAULT VALUES RETURNING alias"));
  }

  @Test
  void anExplicitAliasTheSequenceHasNotReachedCollidesWithItsLaterAllocation() throws Exception {
    update();

    // A column default cannot reserve the value against the sequence the way the trigger did, so
    // uniqueness is what catches it - on the allocation, not on the insert that took the value.
    assertEquals(900001, queryLong("INSERT INTO dataset (alias) VALUES (900001) RETURNING alias"));
    assertThrows(SQLException.class, () -> execute("INSERT INTO dataset DEFAULT VALUES"));

    execute(
        "SELECT setval('dataset_alias_seq', ((SELECT MAX(alias) FROM dataset) + 1)::bigint, false)");
    assertEquals(900002, queryLong("INSERT INTO dataset DEFAULT VALUES RETURNING alias"));
  }

  @Test
  void anExhaustedSequenceFailsTheSameWayItDidUnderTheTrigger() throws Exception {
    // The last integer alias is allocated, so the next nextval() is out of range. That state
    // already broke inserts through the trigger; this pins that swapping in the default neither
    // introduces nor repairs it, which is why the precondition does not gate on it.
    execute("SELECT setval('dataset_alias_seq', 2147483647, false)");
    assertEquals(2147483647, queryLong("INSERT INTO dataset DEFAULT VALUES RETURNING alias"));
    assertThrows(SQLException.class, () -> execute("INSERT INTO dataset DEFAULT VALUES"));

    // The precondition passes here: no alias can reach the out-of-range value the sequence would
    // hand out next, which is the same reason it cannot detect the state.
    update();

    assertThrows(SQLException.class, () -> execute("INSERT INTO dataset DEFAULT VALUES"));
  }

  @Test
  void migrationHaltsBeforeChangingSchemaWhenTheSequenceTrailsAnExistingAlias() throws Exception {
    Object legacyDefault = columnDefault();
    execute("ALTER TABLE dataset DISABLE TRIGGER dataset_alias_allocate");
    execute("INSERT INTO dataset (alias) VALUES (5000000)");
    execute("ALTER TABLE dataset ENABLE TRIGGER dataset_alias_allocate");

    assertThrows(LiquibaseException.class, this::update);

    assertEquals(legacyDefault, columnDefault());
    assertTrue(triggerExists());
    assertTrue(functionExists());
  }

  @Test
  void rollbackRestoresTheCompatibilityTrigger() throws Exception {
    Object legacyDefault = columnDefault();
    assertNotNull(legacyDefault);
    update();
    rollback();

    assertEquals(legacyDefault, columnDefault());
    assertTrue(triggerExists());
    assertTrue(functionExists());
    // The trigger is back in charge, so a supplied alias is overridden again.
    assertEquals(900001, queryLong("INSERT INTO dataset (alias) VALUES (7) RETURNING alias"));
  }

  private Object columnDefault() throws SQLException {
    return queryObject(
        "SELECT pg_get_expr(adbin, adrelid) FROM pg_attrdef "
            + "WHERE adrelid = 'dataset'::regclass AND adnum = (SELECT attnum FROM pg_attribute "
            + "WHERE attrelid = 'dataset'::regclass AND attname = 'alias')");
  }

  private boolean triggerExists() throws SQLException {
    return queryBoolean(
        "SELECT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'dataset_alias_allocate')");
  }

  private boolean functionExists() throws SQLException {
    return queryBoolean(
        "SELECT EXISTS (SELECT 1 FROM pg_proc WHERE proname = 'allocate_dataset_alias')");
  }
}
