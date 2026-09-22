package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import liquibase.exception.LiquibaseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DatasetAliasSequenceMigrationTest extends MigrationTestHelper {

  private static final String CHANGELOG =
      "changesets/changelog-consent-2026-08-10-dataset-alias-sequence.xml";

  @Override
  protected String changelog() {
    return CHANGELOG;
  }

  @Override
  protected void createPreMigrationSchema() throws SQLException {
    try (Connection connection = connection();
        Statement statement = connection.createStatement()) {
      // Dev's legacy alias column is numeric, which exercises the setval bigint cast.
      statement.execute(
          "CREATE TABLE dataset (dataset_id bigserial PRIMARY KEY, alias numeric DEFAULT 0)");
      statement.execute("INSERT INTO dataset (alias) VALUES (42), (900000), (0)");
    }
  }

  @Test
  void migrationPreservesAliasesAndAllocatesAboveMaximumForOldAndNewWriters() throws Exception {
    update();

    assertEquals(42, queryLong("SELECT alias FROM dataset WHERE dataset_id = 1"));
    assertEquals(900000, queryLong("SELECT alias FROM dataset WHERE dataset_id = 2"));
    assertEquals(0, queryLong("SELECT alias FROM dataset WHERE dataset_id = 3"));

    // An old instance supplies its MAX(alias) + 1 result, but the compatibility trigger replaces
    // it.
    assertEquals(
        900001,
        queryLong(
            "INSERT INTO dataset (alias) "
                + "SELECT COALESCE(MAX(alias), 0) + 1 FROM dataset RETURNING alias"));
    execute("DELETE FROM dataset WHERE alias = 900001");
    // A new instance omits alias entirely.
    assertEquals(900002, queryLong("INSERT INTO dataset DEFAULT VALUES RETURNING alias"));

    assertThrows(
        SQLException.class, () -> execute("UPDATE dataset SET alias = NULL WHERE dataset_id = 1"));
    assertThrows(
        SQLException.class, () -> execute("UPDATE dataset SET alias = 42 WHERE dataset_id = 2"));
    assertThrows(
        SQLException.class, () -> execute("UPDATE dataset SET alias = -1 WHERE dataset_id = 1"));
    assertThrows(
        SQLException.class, () -> execute("UPDATE dataset SET alias = 1.5 WHERE dataset_id = 1"));
    assertThrows(
        SQLException.class,
        () -> execute("UPDATE dataset SET alias = 2147483648 WHERE dataset_id = 1"));
    execute("UPDATE dataset SET alias = 2147483647 WHERE dataset_id = 1");
    assertEquals(2147483647, queryLong("SELECT alias FROM dataset WHERE dataset_id = 1"));
  }

  @Test
  void migrationAllocatesLastIntegerAliasWhenExistingMaximumLeavesRoom() throws Exception {
    execute("UPDATE dataset SET alias = 2147483646 WHERE dataset_id = 2");

    update();

    assertEquals(2147483647, queryLong("INSERT INTO dataset DEFAULT VALUES RETURNING alias"));
    assertThrows(
        SQLException.class, () -> queryLong("INSERT INTO dataset DEFAULT VALUES RETURNING alias"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"NULL", "-1", "1.5", "42", "2147483647"})
  void migrationRejectsUnsafeExistingAliasesBeforeChangingSchema(String unsafeAlias)
      throws Exception {
    execute("INSERT INTO dataset (alias) VALUES (" + unsafeAlias + ")");

    assertThrows(LiquibaseException.class, this::update);

    assertNull(queryObject("SELECT to_regclass('dataset_alias_seq')"));
    assertFalse(
        queryBoolean(
            "SELECT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'dataset_alias_allocate')"));
    assertFalse(
        queryBoolean(
            "SELECT EXISTS (SELECT 1 FROM pg_constraint "
                + "WHERE conname = 'dataset_alias_valid_integer')"));
  }

  @Test
  void rollbackRestoresLegacyDefaultAndWriterBehavior() throws Exception {
    update();
    rollback();

    assertNull(queryObject("SELECT to_regclass('dataset_alias_seq')"));
    assertFalse(
        queryBoolean(
            "SELECT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'dataset_alias_allocate')"));
    assertFalse(
        queryBoolean(
            "SELECT EXISTS (SELECT 1 FROM pg_constraint "
                + "WHERE conname = 'dataset_alias_valid_integer')"));
    assertTrue(
        queryBoolean(
            "SELECT is_nullable = 'YES' FROM information_schema.columns "
                + "WHERE table_schema = 'public' AND table_name = 'dataset' AND column_name = 'alias'"));
    assertEquals(
        "0",
        queryObject(
            "SELECT pg_get_expr(adbin, adrelid) FROM pg_attrdef "
                + "WHERE adrelid = 'dataset'::regclass AND adnum = "
                + "(SELECT attnum FROM pg_attribute WHERE attrelid = 'dataset'::regclass AND attname = 'alias')"));

    assertEquals(0, queryLong("INSERT INTO dataset DEFAULT VALUES RETURNING alias"));
    assertEquals(7, queryLong("INSERT INTO dataset (alias) VALUES (7) RETURNING alias"));
  }
}
