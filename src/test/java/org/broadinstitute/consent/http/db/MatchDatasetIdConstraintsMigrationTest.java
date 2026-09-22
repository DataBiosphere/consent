package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import liquibase.exception.LiquibaseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * The precondition is the deployment gate for this release, so it is tested as a migration rather
 * than only through the constraints it guards: a gate that fails open would apply the constraints
 * to data that cannot satisfy them.
 */
class MatchDatasetIdConstraintsMigrationTest extends MigrationTestHelper {

  private static final String CHANGELOG =
      "changesets/changelog-consent-2026-09-09-match-dataset-id-constraints.xml";
  private static final String UNIQUE_CONSTRAINT = "match_entity_purpose_dataset_unique";

  @Override
  protected String changelog() {
    return CHANGELOG;
  }

  @Override
  protected void createPreMigrationSchema() throws SQLException {
    try (Connection connection = connection();
        Statement statement = connection.createStatement()) {
      statement.execute(
          """
          CREATE TABLE match_entity (
            match_id bigserial PRIMARY KEY,
            purpose text NOT NULL,
            dataset_id bigint,
            algorithm_version text)
          """);
      statement.execute(
          "INSERT INTO match_entity (purpose, dataset_id, algorithm_version) "
              + "VALUES ('DAR-1', 1, 'v5'), ('DAR-1', 2, 'v5'), ('DAR-2', 1, 'v5')");
    }
  }

  @Test
  void migrationAppliesBothConstraintsWhenEveryRowIsReady() throws Exception {
    update();

    assertFalse(datasetIdIsNullable());
    assertTrue(uniqueConstraintExists());
    assertThrows(
        SQLException.class,
        () ->
            execute(
                "INSERT INTO match_entity (purpose, dataset_id, algorithm_version) "
                    + "VALUES ('DAR-3', NULL, 'v5')"));
    assertThrows(
        SQLException.class,
        () ->
            execute(
                "INSERT INTO match_entity (purpose, dataset_id, algorithm_version) "
                    + "VALUES ('DAR-1', 1, 'v5')"));
  }

  /**
   * Each value is a row the run is supposed to have cleared; the uppercase stamps cover LOWER().
   */
  @ParameterizedTest
  @ValueSource(
      strings = {
        "('DAR-3', NULL, 'v5')",
        "('DAR-3', 3, 'v1')",
        "('DAR-3', 3, 'v2')",
        "('DAR-3', 3, 'V2')",
        "('DAR-3', 3, NULL)",
        "('DAR-3', 3, 'v5'), ('DAR-3', 3, 'v5')"
      })
  void migrationHaltsBeforeChangingSchemaWhenARowIsNotReady(String unreadyRows) throws Exception {
    execute(
        "INSERT INTO match_entity (purpose, dataset_id, algorithm_version) VALUES " + unreadyRows);

    assertThrows(LiquibaseException.class, this::update);

    assertTrue(datasetIdIsNullable());
    assertFalse(uniqueConstraintExists());
  }

  @Test
  void rollbackRemovesBothConstraints() throws Exception {
    update();
    rollback();

    assertTrue(datasetIdIsNullable());
    assertFalse(uniqueConstraintExists());
    execute(
        "INSERT INTO match_entity (purpose, dataset_id, algorithm_version) "
            + "VALUES ('DAR-3', NULL, 'v1'), ('DAR-1', 1, 'v5')");
  }

  private boolean datasetIdIsNullable() throws SQLException {
    return queryBoolean(
        "SELECT attnotnull = false FROM pg_attribute "
            + "WHERE attrelid = 'match_entity'::regclass AND attname = 'dataset_id'");
  }

  private boolean uniqueConstraintExists() throws SQLException {
    return queryBoolean(
        "SELECT EXISTS (SELECT 1 FROM pg_constraint "
            + "WHERE conname = ? AND conrelid = 'match_entity'::regclass)",
        UNIQUE_CONSTRAINT);
  }
}
