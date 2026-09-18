package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
 * The rolling deployment is the point of this release: an instance on the previous one still
 * supplies a value while a new one does not, so both shapes have to be insertable at once.
 */
class MatchConsentNullableMigrationTest {

  private static final String CHANGELOG =
      "changesets/changelog-consent-2026-09-18-match-consent-nullable.xml";
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
            consent varchar(255) NOT NULL,
            purpose text NOT NULL,
            dataset_id bigint NOT NULL REFERENCES dataset (dataset_id),
            CONSTRAINT purpose_consent UNIQUE (purpose, consent),
            CONSTRAINT match_entity_purpose_dataset_unique UNIQUE (purpose, dataset_id))
          """);
      statement.execute(
          "INSERT INTO match_entity (consent, purpose, dataset_id) VALUES ('DUOS-000042', 'DAR-1', 1)");
    }
  }

  @Test
  void migrationLetsBothReleasesWriteWhileTheyOverlap() throws Exception {
    update();

    execute(
        "INSERT INTO match_entity (consent, purpose, dataset_id) VALUES ('DUOS-000042', 'DAR-2', 1)");
    execute("INSERT INTO match_entity (purpose, dataset_id) VALUES ('DAR-3', 1)");
    // purpose_consent ignores nulls, so it cannot reject the second dataset a purpose gets from
    // the new release. The replacement constraint is what still rejects a real duplicate.
    execute("INSERT INTO match_entity (purpose, dataset_id) VALUES ('DAR-3', 2)");
    assertThrows(
        SQLException.class,
        () -> execute("INSERT INTO match_entity (purpose, dataset_id) VALUES ('DAR-3', 1)"));
  }

  @Test
  void migrationHaltsWhileTheReleaseThatStoppedReadingIsNotApplied() throws Exception {
    execute("CREATE TABLE match_migration_snapshot (match_id bigint PRIMARY KEY)");

    assertThrows(LiquibaseException.class, this::update);

    assertFalse(consentIsNullable());
  }

  @Test
  void rollbackRederivesTheValuesThisReleaseStoppedWriting() throws Exception {
    update();
    execute("INSERT INTO match_entity (purpose, dataset_id) VALUES ('DAR-2', 2)");

    rollback();

    assertFalse(consentIsNullable());
    assertEquals(
        "DUOS-1234567", queryObject("SELECT consent FROM match_entity WHERE purpose = 'DAR-2'"));
  }

  private boolean consentIsNullable() throws SQLException {
    return (Boolean)
        queryObject(
            "SELECT is_nullable = 'YES' FROM information_schema.columns "
                + "WHERE table_schema = 'public' AND table_name = 'match_entity' "
                + "AND column_name = 'consent'");
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
