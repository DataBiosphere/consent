package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.testcontainers.containers.PostgreSQLContainer;

class DatasetAliasSequenceMigrationTest {

  private static final String CHANGELOG =
      "changesets/changelog-consent-2026-08-10-dataset-alias-sequence.xml";
  private static PostgreSQLContainer<?> postgres;

  @BeforeAll
  static void startPostgres() {
    postgres = new PostgreSQLContainer<>(DAOTestHelper.POSTGRES_IMAGE);
    postgres.start();
  }

  @AfterAll
  static void stopPostgres() {
    postgres.stop();
  }

  @BeforeEach
  void createPreMigrationSchema() throws SQLException {
    try (Connection connection = connection();
        Statement statement = connection.createStatement()) {
      statement.execute("DROP SCHEMA public CASCADE");
      statement.execute("CREATE SCHEMA public");
      // Dev's legacy alias column is numeric, which exercises the setval bigint cast.
      statement.execute(
          "CREATE TABLE dataset (dataset_id bigserial PRIMARY KEY, alias numeric DEFAULT 0)");
      statement.execute("INSERT INTO dataset (alias) VALUES (42), (900000)");
    }
  }

  @Test
  void migrationPreservesAliasesAndAllocatesAboveMaximumForOldAndNewWriters() throws Exception {
    update();

    assertEquals(42, queryLong("SELECT alias FROM dataset WHERE dataset_id = 1"));
    assertEquals(900000, queryLong("SELECT alias FROM dataset WHERE dataset_id = 2"));

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
  }

  @ParameterizedTest
  @ValueSource(strings = {"NULL", "0", "42"})
  void migrationRejectsUnsafeExistingAliasesBeforeChangingSchema(String unsafeAlias)
      throws Exception {
    execute("INSERT INTO dataset (alias) VALUES (" + unsafeAlias + ")");

    assertThrows(LiquibaseException.class, this::update);

    assertNull(queryObject("SELECT to_regclass('dataset_alias_seq')"));
    assertFalse(
        queryBoolean(
            "SELECT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'dataset_alias_allocate')"));
  }

  @Test
  void rollbackRestoresLegacyDefaultAndWriterBehavior() throws Exception {
    update();
    rollback();

    assertNull(queryObject("SELECT to_regclass('dataset_alias_seq')"));
    assertFalse(
        queryBoolean(
            "SELECT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'dataset_alias_allocate')"));
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

  private static Connection connection() throws SQLException {
    return DriverManager.getConnection(
        postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
  }

  private void update() throws Exception {
    try (Connection connection = connection()) {
      Database database =
          DatabaseFactory.getInstance()
              .findCorrectDatabaseImplementation(new JdbcConnection(connection));
      try (Liquibase liquibase =
          new Liquibase(CHANGELOG, new ClassLoaderResourceAccessor(), database)) {
        liquibase.update(new Contexts(), new LabelExpression());
      }
    }
  }

  private void rollback() throws Exception {
    try (Connection connection = connection()) {
      Database database =
          DatabaseFactory.getInstance()
              .findCorrectDatabaseImplementation(new JdbcConnection(connection));
      try (Liquibase liquibase =
          new Liquibase(CHANGELOG, new ClassLoaderResourceAccessor(), database)) {
        liquibase.rollback(1, new Contexts(), new LabelExpression());
      }
    }
  }

  private void execute(String sql) throws SQLException {
    try (Connection connection = connection();
        Statement statement = connection.createStatement()) {
      statement.execute(sql);
    }
  }

  private long queryLong(String sql) throws SQLException {
    return ((Number) queryObject(sql)).longValue();
  }

  private boolean queryBoolean(String sql) throws SQLException {
    return (Boolean) queryObject(sql);
  }

  private Object queryObject(String sql) throws SQLException {
    try (Connection connection = connection();
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery(sql)) {
      resultSet.next();
      return resultSet.getObject(1);
    }
  }
}
