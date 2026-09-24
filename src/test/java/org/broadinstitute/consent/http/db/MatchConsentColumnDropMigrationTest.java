package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import liquibase.exception.LiquibaseException;
import org.junit.jupiter.api.Test;

/**
 * The final schema: dataset identity is the foreign key and nothing else. The precondition is the
 * interesting half - dropping purpose_consent in an environment where the replacement never applied
 * would leave a purpose able to hold two matches for one dataset.
 */
class MatchConsentColumnDropMigrationTest extends MigrationTestHelper {

  private static final String CHANGELOG =
      "changesets/changelog-consent-2026-09-19-drop-match-consent.xml";

  @Override
  protected String changelog() {
    return CHANGELOG;
  }

  @Override
  protected void createPreMigrationSchema() throws SQLException {
    try (Connection connection = connection();
        Statement statement = connection.createStatement()) {
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
  void migrationDropsTheColumnWhereTheLegacyRuleIsOnlyAnIndex() throws Exception {
    // The shape a MySQL-migrated environment carries: pgloader turned the unique key into a bare,
    // renamed unique index, so there is no purpose_consent constraint to drop by name.
    execute("ALTER TABLE match_entity DROP CONSTRAINT purpose_consent");
    execute("CREATE UNIQUE INDEX idx_20033_purpose_consent ON match_entity (purpose, consent)");
    execute("CREATE INDEX idx_20033_fkmatchconsent ON match_entity (consent)");

    update();

    assertFalse(columnExists());
    assertFalse(indexExists("idx_20033_purpose_consent"));
    assertFalse(indexExists("idx_20033_fkmatchconsent"));
    assertTrue(constraintExists("match_entity_purpose_dataset_unique"));
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

  @Test
  void migrationHaltsWhenASurvivingValueDisagreesWithItsDataset() throws Exception {
    // dropColumn is irreversible and the rollback rebuilds by derivation, so this row's value
    // would be discarded and replaced with DUOS-000042 rather than restored.
    execute("UPDATE match_entity SET consent = 'DUOS-1234567' WHERE dataset_id = 1");

    assertThrows(LiquibaseException.class, this::update);

    assertTrue(columnExists());
    assertTrue(constraintExists("purpose_consent"));
  }

  private boolean columnExists() throws SQLException {
    return queryBoolean(
        "SELECT EXISTS (SELECT 1 FROM pg_attribute "
            + "WHERE attrelid = 'match_entity'::regclass AND attname = 'consent' "
            + "AND attnum > 0 AND NOT attisdropped)");
  }

  private boolean indexExists(String name) throws SQLException {
    try (Connection connection = connection();
        PreparedStatement statement =
            connection.prepareStatement("SELECT to_regclass(?) IS NOT NULL")) {
      statement.setString(1, name);
      try (ResultSet resultSet = statement.executeQuery()) {
        resultSet.next();
        return resultSet.getBoolean(1);
      }
    }
  }

  private boolean constraintExists(String name) throws SQLException {
    try (Connection connection = connection();
        PreparedStatement statement =
            connection.prepareStatement(
                "SELECT EXISTS (SELECT 1 FROM pg_constraint "
                    + "WHERE conname = ? AND conrelid = 'match_entity'::regclass)")) {
      statement.setString(1, name);
      try (ResultSet resultSet = statement.executeQuery()) {
        resultSet.next();
        return resultSet.getBoolean(1);
      }
    }
  }
}
