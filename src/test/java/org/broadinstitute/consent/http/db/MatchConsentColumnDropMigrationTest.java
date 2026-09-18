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
 * The final schema: dataset identity is the foreign key and nothing else. The precondition is the
 * interesting half - dropping purpose_consent in an environment where the replacement never applied
 * would leave a purpose able to hold two matches for one dataset.
 */
class MatchConsentColumnDropMigrationTest {

  private static final String CHANGELOG =
      "changesets/changelog-consent-2026-09-19-drop-match-consent.xml";
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
      // numeric, as it still is in some environments, and one value carrying a scale: both
      // satisfy the alias = trunc(alias) check the sequence migration applied.
      statement.execute("CREATE TABLE dataset (dataset_id bigserial PRIMARY KEY, alias numeric)");
      statement.execute("INSERT INTO dataset (alias) VALUES (42), (1234567.0)");
      statement.execute(
          """
          CREATE TABLE match_entity (
            match_id bigserial PRIMARY KEY,
            consent varchar(255),
            purpose text NOT NULL,
            dataset_id bigint NOT NULL REFERENCES dataset (dataset_id),
            CONSTRAINT purpose_consent UNIQUE (purpose, consent),
            CONSTRAINT match_entity_purpose_dataset_unique UNIQUE (purpose, dataset_id))
          """);
      statement.execute(
          "INSERT INTO match_entity (consent, purpose, dataset_id) "
              + "VALUES ('DUOS-000042', 'DAR-1', 1), (NULL, 'DAR-1', 2)");
    }
  }

  @Test
  void migrationLeavesDatasetIdAsTheOnlyIdentity() throws Exception {
    update();

    assertFalse(columnExists());
    assertFalse(constraintExists("purpose_consent"));
    assertTrue(constraintExists("match_entity_purpose_dataset_unique"));
    execute("INSERT INTO match_entity (purpose, dataset_id) VALUES ('DAR-2', 1)");
    assertThrows(
        SQLException.class,
        () -> execute("INSERT INTO match_entity (purpose, dataset_id) VALUES ('DAR-2', 1)"));
  }

  @Test
  void migrationHaltsWhenTheReplacementConstraintNeverApplied() throws Exception {
    execute("ALTER TABLE match_entity DROP CONSTRAINT match_entity_purpose_dataset_unique");

    assertThrows(LiquibaseException.class, this::update);

    assertTrue(columnExists());
    assertTrue(constraintExists("purpose_consent"));
  }

  @Test
  void migrationHaltsWhileAnInstanceCouldStillBeWritingTheColumn() throws Exception {
    // A non-null column means the release that stops writing it has not applied here, so an
    // instance still supplying a value would fail every insert once the column is gone.
    execute("UPDATE match_entity SET consent = 'DUOS-1234567' WHERE consent IS NULL");
    execute("ALTER TABLE match_entity ALTER COLUMN consent SET NOT NULL");

    assertThrows(LiquibaseException.class, this::update);

    assertTrue(columnExists());
  }

  @Test
  void rollbackRederivesTheColumnFromTheDataset() throws Exception {
    update();
    rollback();

    assertTrue(columnExists());
    assertTrue(constraintExists("purpose_consent"));
    assertEquals(
        "DUOS-000042", queryObject("SELECT consent FROM match_entity WHERE dataset_id = 1"));
    // Restored by derivation, so a row that held no value gains the one its dataset implies.
    assertEquals(
        "DUOS-1234567", queryObject("SELECT consent FROM match_entity WHERE dataset_id = 2"));
  }

  private boolean columnExists() throws SQLException {
    return (Boolean)
        queryObject(
            "SELECT EXISTS (SELECT 1 FROM information_schema.columns "
                + "WHERE table_schema = 'public' AND table_name = 'match_entity' "
                + "AND column_name = 'consent')");
  }

  private boolean constraintExists(String name) throws SQLException {
    return (Boolean)
        queryObject("SELECT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = '" + name + "')");
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
