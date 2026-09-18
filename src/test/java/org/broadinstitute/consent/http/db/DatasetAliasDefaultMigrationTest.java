package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Runs against the schema the compatibility release actually left behind, by applying that
 * changelog first: the trigger this one removes is the thing under test, so a hand-built
 * approximation of it would not prove the removal.
 */
class DatasetAliasDefaultMigrationTest {

  private static final String COMPATIBILITY_CHANGELOG =
      "changesets/changelog-consent-2026-08-10-dataset-alias-sequence.xml";
  private static final String CHANGELOG =
      "changesets/changelog-consent-2026-09-17-dataset-alias-default.xml";
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
  void createPreMigrationSchema() throws Exception {
    try (Connection connection = connection();
        Statement statement = connection.createStatement()) {
      statement.execute("DROP SCHEMA public CASCADE");
      statement.execute("CREATE SCHEMA public");
      statement.execute(
          "CREATE TABLE dataset (dataset_id bigserial PRIMARY KEY, alias numeric DEFAULT 0)");
      statement.execute("INSERT INTO dataset (alias) VALUES (42), (900000), (0)");
    }
    update(COMPATIBILITY_CHANGELOG);
  }

  @Test
  void migrationKeepsAllocatingFromTheSequenceOnceTheTriggerIsGone() throws Exception {
    update(CHANGELOG);

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
    update(CHANGELOG);

    // The trigger replaced supplied values; the default only fills an omitted one. Nothing in the
    // application supplies an alias, so this documents the behaviour change rather than relying
    // on it.
    assertEquals(7, queryLong("INSERT INTO dataset (alias) VALUES (7) RETURNING alias"));
    assertEquals(900001, queryLong("INSERT INTO dataset DEFAULT VALUES RETURNING alias"));
  }

  @Test
  void anExplicitAliasTheSequenceHasNotReachedCollidesWithItsLaterAllocation() throws Exception {
    update(CHANGELOG);

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
    update(CHANGELOG);

    assertThrows(SQLException.class, () -> execute("INSERT INTO dataset DEFAULT VALUES"));
  }

  @Test
  void migrationHaltsBeforeChangingSchemaWhenTheSequenceTrailsAnExistingAlias() throws Exception {
    execute("ALTER TABLE dataset DISABLE TRIGGER dataset_alias_allocate");
    execute("INSERT INTO dataset (alias) VALUES (5000000)");
    execute("ALTER TABLE dataset ENABLE TRIGGER dataset_alias_allocate");

    assertThrows(LiquibaseException.class, () -> update(CHANGELOG));

    assertTrue(triggerExists());
    assertTrue(functionExists());
  }

  @Test
  void rollbackRestoresTheCompatibilityTrigger() throws Exception {
    String legacyDefault = columnDefault();
    update(CHANGELOG);
    rollback();

    assertEquals(legacyDefault, columnDefault());
    assertTrue(triggerExists());
    assertTrue(functionExists());
    // The trigger is back in charge, so a supplied alias is overridden again.
    assertEquals(900001, queryLong("INSERT INTO dataset (alias) VALUES (7) RETURNING alias"));
  }

  private Object queryObjectOrNull(String sql) throws SQLException {
    try (Connection connection = connection();
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery(sql)) {
      return resultSet.next() ? resultSet.getObject(1) : null;
    }
  }

  private String columnDefault() throws SQLException {
    Object value =
        queryObjectOrNull(
            "SELECT pg_get_expr(adbin, adrelid) FROM pg_attrdef "
                + "WHERE adrelid = 'dataset'::regclass AND adnum = (SELECT attnum FROM pg_attribute "
                + "WHERE attrelid = 'dataset'::regclass AND attname = 'alias')");
    return String.valueOf(value);
  }

  private boolean triggerExists() throws SQLException {
    return queryBoolean(
        "SELECT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'dataset_alias_allocate')");
  }

  private boolean functionExists() throws SQLException {
    return queryBoolean(
        "SELECT EXISTS (SELECT 1 FROM pg_proc WHERE proname = 'allocate_dataset_alias')");
  }

  private static Connection connection() throws SQLException {
    return DriverManager.getConnection(
        postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
  }

  private void update(String changelog) throws Exception {
    try (Connection connection = connection()) {
      Database database =
          DatabaseFactory.getInstance()
              .findCorrectDatabaseImplementation(new JdbcConnection(connection));
      try (Liquibase liquibase =
          new Liquibase(changelog, new ClassLoaderResourceAccessor(), database)) {
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
