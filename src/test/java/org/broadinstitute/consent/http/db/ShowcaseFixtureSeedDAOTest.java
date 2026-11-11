package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.util.List;
import org.broadinstitute.consent.http.models.AssetCatalogCheck;
import org.broadinstitute.consent.http.models.ShowcaseDatasetSummary;
import org.broadinstitute.consent.http.models.User;
import org.junit.jupiter.api.Test;

/**
 * The local showcase fixture (scripts/sql/showcase-fixture.sql, DT-4358) seeds what it promises:
 * the public lookup returns its three eligible datasets and leaves out the two ineligible ones, and
 * a second run restores the fixture's values.
 */
class ShowcaseFixtureSeedDAOTest extends DAOTestHelper {

  private static final Path FIXTURE = Path.of("scripts", "sql", "showcase-fixture.sql");
  private static final List<Integer> FIXTURE_ALIASES =
      List.of(990001, 990002, 990003, 990004, 990005);

  /** Runs the fixture in one session, with the target the runner script sets, or none. */
  private void seed(boolean local) throws IOException {
    String sql = Files.readString(FIXTURE);
    jdbi.useHandle(
        handle -> {
          handle.execute(
              local
                  ? "SET duos.showcase_fixture_target = 'localdb'"
                  : "RESET duos.showcase_fixture_target");
          try {
            handle.createScript(sql).execute();
          } finally {
            // A failed run leaves its transaction open and aborted; end it before the connection
            // goes back to the pool. After a COMMIT this only warns.
            handle.execute("ROLLBACK");
            handle.execute("RESET duos.showcase_fixture_target");
          }
        });
  }

  private void seed() throws IOException {
    seed(true);
  }

  private List<String> eligible() {
    return datasetDAO.findShowcaseSummaries(FIXTURE_ALIASES).stream()
        .map(summary -> summary.identifier() + " " + summary.accessManagement())
        .toList();
  }

  @Test
  void testLookupReturnsTheEligibleFixtureDatasetsOnly() throws IOException {
    seed();

    assertEquals(
        List.of("DUOS-990001 controlled", "DUOS-990002 open", "DUOS-990003 external"), eligible());
    ShowcaseDatasetSummary controlled =
        datasetDAO.findShowcaseSummaries(List.of(990001)).getFirst();
    assertEquals("Showcase Fixture Controlled Cohort", controlled.title());
    assertEquals("Showcase Fixture DAC", controlled.dacName());
    assertEquals(Integer.valueOf(1200), controlled.participantCount());
  }

  @Test
  void testRefusesToRunWithoutTheLocalTarget() {
    assertThrows(Exception.class, () -> seed(false));
    assertEquals(List.of(), eligible());
    int fixtureUsers =
        jdbi.withHandle(
            handle ->
                handle
                    .createQuery("SELECT count(*) FROM users WHERE user_id = 990001")
                    .mapTo(Integer.class)
                    .one());
    assertEquals(0, fixtureUsers);
  }

  @Test
  void testSeedsWhenADictionaryKeyIsDuplicated() throws IOException {
    // The dictionary has no unique key on `key`, so a restored database can hold duplicates.
    executeSql("INSERT INTO dictionary (key, required) VALUES ('URL', false), ('URL', false)");

    seed();

    assertEquals(
        List.of("DUOS-990001 controlled", "DUOS-990002 open", "DUOS-990003 external"), eligible());
  }

  @Test
  void testSecondRunReplacesPropertiesTheAppRecreated() throws IOException {
    seed();
    // The app edits a property by deleting its row and inserting one from the sequence.
    executeSql("DELETE FROM dataset_property WHERE property_id = 990002");
    executeSql(
        """
        INSERT INTO dataset_property (dataset_id, property_key, schema_property, property_type,
                                      property_value, create_date)
        SELECT 990001, key_id, 'numberOfParticipants', 'number', '7', now()
        FROM dictionary WHERE key = '# of participants' ORDER BY key_id LIMIT 1
        """);

    seed();

    assertEquals(
        Integer.valueOf(1200),
        datasetDAO.findShowcaseSummaries(List.of(990001)).getFirst().participantCount());
  }

  @Test
  void testCatalogCheckFindsNothingAfterTheSeed() throws IOException {
    seed();

    AssetCatalogCheck check = jdbi.onDemand(AssetCatalogCheckDAO.class).check();

    assertEquals(0, check.missing().count());
    assertEquals(0, check.collisions().count());
    assertEquals(0, check.mismatches().count());
    assertEquals(0, check.relinkable().count());
    assertEquals(0, check.orphans().count());
    assertEquals(0, check.sequenceBehind().count());
  }

  @Test
  void testStopsWhenAnotherCatalogEntryHoldsAFixtureAlias() {
    // An entry left by another dataset (or inserted by hand) with a fixture alias as its number.
    executeSql(
        """
        INSERT INTO asset_catalog (asset_type, asset_number, source_dataset_id)
        VALUES ('DATASET', 990001, 12345)
        """);

    Exception error = assertThrows(Exception.class, this::seed);

    assertTrue(error.getMessage().contains("another asset catalog entry holds DUOS-990001"));
    assertEquals(List.of(), eligible());
  }

  @Test
  void testDatasetsRegisteredAfterTheSeedGetAliasesPastTheFixture() throws IOException {
    seed();
    // Deleting a fixture dataset in the app calls this; it must leave the sequence where it is.
    jdbi.useTransaction(
        handle -> handle.attach(AssetCatalogDAO.class).advanceAliasSequencePast(List.of(990001)));

    User user = createUser();
    Integer datasetId =
        datasetDAO.insertDataset(
            "Registered after the fixture",
            new Timestamp(System.currentTimeMillis()),
            user.getUserId(),
            null,
            "{\"generalUse\":true}",
            null);

    long alias =
        jdbi.withHandle(
            handle ->
                handle
                    .createQuery("SELECT alias FROM dataset WHERE dataset_id = :id")
                    .bind("id", datasetId)
                    .mapTo(Long.class)
                    .one());
    assertEquals(990006L, alias);
  }

  @Test
  void testNewestRankingReturnsTheEligibleFixtureDatasets() throws IOException {
    seed();

    assertEquals(
        List.of(990001, 990002, 990003),
        datasetDAO.findShowcaseNewestAliases(false, null, null, 10).stream().sorted().toList());
  }

  @Test
  void testSecondRunRestoresTheFixture() throws IOException {
    seed();
    executeSql(
        "UPDATE dataset SET name = 'Edited by a tester', dac_approval = false WHERE alias = 990001");
    executeSql("UPDATE study SET public_visibility = false WHERE study_id = 990001");

    seed();

    assertEquals(
        List.of("DUOS-990001 controlled", "DUOS-990002 open", "DUOS-990003 external"), eligible());
    assertEquals(
        "Showcase Fixture Controlled Cohort",
        datasetDAO.findShowcaseSummaries(List.of(990001)).getFirst().title());
    int fixtureDatasets =
        jdbi.withHandle(
            handle ->
                handle
                    .createQuery(
                        "SELECT count(*) FROM dataset WHERE alias BETWEEN 990001 AND 990099")
                    .mapTo(Integer.class)
                    .one());
    assertEquals(5, fixtureDatasets);
  }
}
