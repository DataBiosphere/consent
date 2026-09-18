package org.broadinstitute.consent.http.db;

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
 * The precondition carries the weight here. Dropping the run's rollback path in an environment
 * where the run never finished takes the recovery away exactly when it is wanted, and this release
 * also stops reading a row that has no dataset_id.
 */
class MatchMigrationSnapshotDropMigrationTest {

  private static final String CHANGELOG =
      "changesets/changelog-consent-2026-09-17-drop-match-migration-snapshots.xml";
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
      statement.execute("CREATE TABLE dataset (dataset_id bigserial PRIMARY KEY, alias bigint)");
      statement.execute("INSERT INTO dataset (alias) VALUES (42)");
      statement.execute(
          """
          CREATE TABLE match_entity (
            match_id bigserial PRIMARY KEY,
            consent varchar(255) NOT NULL,
            purpose text NOT NULL,
            dataset_id bigint NOT NULL REFERENCES dataset (dataset_id),
            CONSTRAINT match_entity_purpose_dataset_unique UNIQUE (purpose, dataset_id))
          """);
      statement.execute("CREATE TABLE match_migration_snapshot (match_id bigint PRIMARY KEY)");
      statement.execute(
          "CREATE TABLE match_migration_rationale_snapshot (rationale_id bigint PRIMARY KEY)");
    }
  }

  @Test
  void migrationDropsBothSnapshotTables() throws Exception {
    update();

    assertFalse(tableExists("match_migration_snapshot"));
    assertFalse(tableExists("match_migration_rationale_snapshot"));
  }

  @Test
  void migrationKeepsTheSnapshotsWhenTheDatasetIdConstraintsNeverApplied() throws Exception {
    execute("ALTER TABLE match_entity ALTER COLUMN dataset_id DROP NOT NULL");

    assertThrows(LiquibaseException.class, this::update);

    assertTrue(tableExists("match_migration_snapshot"));
    assertTrue(tableExists("match_migration_rationale_snapshot"));
  }

  @Test
  void migrationKeepsTheSnapshotsWhenTheReplacementConstraintNeverApplied() throws Exception {
    execute("ALTER TABLE match_entity DROP CONSTRAINT match_entity_purpose_dataset_unique");

    assertThrows(LiquibaseException.class, this::update);

    assertTrue(tableExists("match_migration_snapshot"));
  }

  @Test
  void rollbackRecreatesTheTablesEmpty() throws Exception {
    update();
    rollback();

    assertTrue(tableExists("match_migration_snapshot"));
    assertTrue(tableExists("match_migration_rationale_snapshot"));
  }

  private boolean tableExists(String table) throws SQLException {
    return (Boolean) queryObject("SELECT to_regclass('" + table + "') IS NOT NULL");
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

  private Object queryObject(String sql) throws SQLException {
    try (Connection connection = connection();
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery(sql)) {
      resultSet.next();
      return resultSet.getObject(1);
    }
  }
}
