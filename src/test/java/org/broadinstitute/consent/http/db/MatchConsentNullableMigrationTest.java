package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import liquibase.exception.LiquibaseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * The rolling deployment is the point of this release: an instance on the previous one still
 * supplies a value while a new one does not, so both shapes have to be insertable at once.
 */
class MatchConsentNullableMigrationTest extends MigrationTestHelper {

  private static final String CHANGELOG =
      "changesets/changelog-consent-2026-09-18-match-consent-nullable.xml";

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
  void migrationRejectsTheMixedWriterCollisionOnTheReplacementConstraint() throws Exception {
    update();

    // The two writers overlapping on one (purpose, dataset_id): the old release supplies a value
    // and the new one does not, so purpose_consent sees no duplicate and only
    // match_entity_purpose_dataset_unique stands between them. Both orderings are rejected.
    execute("INSERT INTO match_entity (purpose, dataset_id) VALUES ('DAR-4', 1)");
    assertThrows(
        SQLException.class,
        () ->
            execute(
                "INSERT INTO match_entity (consent, purpose, dataset_id) "
                    + "VALUES ('DUOS-000042', 'DAR-4', 1)"));

    execute(
        "INSERT INTO match_entity (consent, purpose, dataset_id) "
            + "VALUES ('DUOS-000042', 'DAR-5', 1)");
    assertThrows(
        SQLException.class,
        () -> execute("INSERT INTO match_entity (purpose, dataset_id) VALUES ('DAR-5', 1)"));
  }

  private static final String CREATE_SNAPSHOT =
      "CREATE TABLE match_migration_snapshot (match_id bigint PRIMARY KEY)";
  private static final String CREATE_RATIONALE_SNAPSHOT =
      "CREATE TABLE match_migration_rationale_snapshot (match_id bigint PRIMARY KEY)";
  private static final String DROP_REPLACEMENT_CONSTRAINT =
      "ALTER TABLE match_entity DROP CONSTRAINT match_entity_purpose_dataset_unique";

  /**
   * Each state below is the predecessor release not having applied. The last two pair an unmet half
   * with another so that neither can be paid for by the other, since the gate checks each half on
   * its own.
   */
  private enum UnmetGate {
    SNAPSHOT_SURVIVES(CREATE_SNAPSHOT),
    RATIONALE_SNAPSHOT_SURVIVES(CREATE_RATIONALE_SNAPSHOT),
    // Without the replacement constraint the rows this release writes with a null consent fall
    // under no uniqueness rule at all, which is why purpose_consent is allowed to go toothless.
    REPLACEMENT_CONSTRAINT_MISSING(DROP_REPLACEMENT_CONSTRAINT),
    SNAPSHOT_OFFSETS_MISSING_CONSTRAINT(CREATE_SNAPSHOT, DROP_REPLACEMENT_CONSTRAINT),
    RATIONALE_SNAPSHOT_OFFSETS_MISSING_CONSTRAINT(
        CREATE_RATIONALE_SNAPSHOT, DROP_REPLACEMENT_CONSTRAINT);

    private final List<String> preconditions;

    UnmetGate(String... preconditions) {
      this.preconditions = List.of(preconditions);
    }
  }

  @ParameterizedTest(name = "{0}")
  @EnumSource(UnmetGate.class)
  void migrationHaltsWhileAGateIsUnmet(UnmetGate gate) throws Exception {
    for (String sql : gate.preconditions) {
      execute(sql);
    }

    assertThrows(LiquibaseException.class, this::update, gate::name);

    assertFalse(consentIsNullable(), gate::name);
  }

  @Test
  void rollbackRederivesTheValuesThisReleaseStoppedWriting() throws Exception {
    update();
    execute("INSERT INTO match_entity (purpose, dataset_id) VALUES ('DAR-2', 2)");

    rollback();

    assertFalse(consentIsNullable());
    assertEquals(
        "DUOS-1234567", queryObject("SELECT consent FROM match_entity WHERE purpose = 'DAR-2'"));
    // The row that predates the release re-derives to the value it already carried.
    assertEquals(
        "DUOS-000042", queryObject("SELECT consent FROM match_entity WHERE purpose = 'DAR-1'"));
  }

  @Test
  void rollbackSurvivesALegacyValueThatDisagreesWithItsDataset() throws Exception {
    // A hand-fixed row whose stored text does not match its own dataset. Re-deriving only the
    // nulls would point it and the new row at the same (purpose, consent) and abort the rollback.
    execute("UPDATE match_entity SET consent = 'DUOS-1234567' WHERE purpose = 'DAR-1'");
    update();
    execute("INSERT INTO match_entity (purpose, dataset_id) VALUES ('DAR-1', 2)");

    rollback();

    assertFalse(consentIsNullable());
    assertEquals(
        "DUOS-000042",
        queryObject("SELECT consent FROM match_entity WHERE purpose = 'DAR-1' AND dataset_id = 1"));
    assertEquals(
        "DUOS-1234567",
        queryObject("SELECT consent FROM match_entity WHERE purpose = 'DAR-1' AND dataset_id = 2"));
  }

  private boolean consentIsNullable() throws SQLException {
    return queryBoolean(
        "SELECT NOT attnotnull FROM pg_attribute "
            + "WHERE attrelid = 'match_entity'::regclass AND attname = 'consent'");
  }
}
