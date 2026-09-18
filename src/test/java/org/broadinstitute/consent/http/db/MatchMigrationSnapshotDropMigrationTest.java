package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import liquibase.exception.LiquibaseException;
import org.junit.jupiter.api.Test;

/**
 * The precondition carries the weight here. Dropping the run's rollback path in an environment
 * where the run never finished takes the recovery away exactly when it is wanted, and this release
 * also stops reading a row that has no dataset_id.
 */
class MatchMigrationSnapshotDropMigrationTest extends MigrationTestHelper {

  private static final String SNAPSHOT_CHANGELOG =
      "changesets/changelog-consent-2026-09-09-match-migration-snapshot.xml";
  private static final String CHANGELOG =
      "changesets/changelog-consent-2026-09-17-drop-match-migration-snapshots.xml";
  private static final String SNAPSHOT_TABLE = "match_migration_snapshot";
  private static final String RATIONALE_SNAPSHOT_TABLE = "match_migration_rationale_snapshot";

  @Override
  protected String changelog() {
    return CHANGELOG;
  }

  /**
   * The snapshot tables come from the changeset that created them rather than a hand-built
   * stand-in, so the rollback is compared against the shape it is supposed to restore.
   */
  @Override
  protected void createPreMigrationSchema() throws Exception {
    try (Connection connection = connection();
        Statement statement = connection.createStatement()) {
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
    }
    update(SNAPSHOT_CHANGELOG);
  }

  @Test
  void migrationDropsBothSnapshotTables() throws Exception {
    update();

    assertFalse(tableExists(SNAPSHOT_TABLE));
    assertFalse(tableExists(RATIONALE_SNAPSHOT_TABLE));
  }

  @Test
  void migrationKeepsTheSnapshotsWhenTheDatasetIdConstraintsNeverApplied() throws Exception {
    execute("ALTER TABLE match_entity ALTER COLUMN dataset_id DROP NOT NULL");

    assertThrows(LiquibaseException.class, this::update);

    assertTrue(tableExists(SNAPSHOT_TABLE));
    assertTrue(tableExists(RATIONALE_SNAPSHOT_TABLE));
  }

  @Test
  void migrationKeepsTheSnapshotsWhenTheReplacementConstraintNeverApplied() throws Exception {
    execute("ALTER TABLE match_entity DROP CONSTRAINT match_entity_purpose_dataset_unique");

    assertThrows(LiquibaseException.class, this::update);

    assertTrue(tableExists(SNAPSHOT_TABLE));
    assertTrue(tableExists(RATIONALE_SNAPSHOT_TABLE));
  }

  @Test
  void rollbackRecreatesBothTablesEmptyAndInTheirOriginalShape() throws Exception {
    execute("INSERT INTO match_migration_snapshot (match_id, purpose) VALUES (1, 'DAR-1')");
    execute(
        "INSERT INTO match_migration_rationale_snapshot (rationale_id, match_id, rationale) "
            + "VALUES (1, 1, 'because')");
    String snapshotColumns = columnSignature(SNAPSHOT_TABLE);
    String snapshotIndexes = indexSignature(SNAPSHOT_TABLE);
    String rationaleColumns = columnSignature(RATIONALE_SNAPSHOT_TABLE);
    String rationaleIndexes = indexSignature(RATIONALE_SNAPSHOT_TABLE);

    update();
    rollback();

    // The rollback block is hand-copied DDL, so it is compared against the changeset that owns the
    // shape rather than merely checked for existence. The captured rows are gone for good.
    assertEquals(snapshotColumns, columnSignature(SNAPSHOT_TABLE));
    assertEquals(snapshotIndexes, indexSignature(SNAPSHOT_TABLE));
    assertEquals(rationaleColumns, columnSignature(RATIONALE_SNAPSHOT_TABLE));
    assertEquals(rationaleIndexes, indexSignature(RATIONALE_SNAPSHOT_TABLE));
    assertEquals(0, queryLong("SELECT COUNT(*) FROM " + SNAPSHOT_TABLE));
    assertEquals(0, queryLong("SELECT COUNT(*) FROM " + RATIONALE_SNAPSHOT_TABLE));
  }

  private boolean tableExists(String table) throws SQLException {
    return queryBoolean("SELECT to_regclass(?) IS NOT NULL", table);
  }

  private String columnSignature(String table) throws SQLException {
    return (String)
        queryObject(
            "SELECT string_agg(attname || ' ' || format_type(atttypid, atttypmod) "
                + "|| CASE WHEN attnotnull THEN ' NOT NULL' ELSE '' END, ', ' ORDER BY attnum) "
                + "FROM pg_attribute WHERE attrelid = ?::regclass "
                + "AND attnum > 0 AND NOT attisdropped",
            table);
  }

  private String indexSignature(String table) throws SQLException {
    return (String)
        queryObject(
            "SELECT string_agg(pg_get_indexdef(indexrelid), '; ' "
                + "ORDER BY indexrelid::regclass::text) "
                + "FROM pg_index WHERE indrelid = ?::regclass",
            table);
  }
}
