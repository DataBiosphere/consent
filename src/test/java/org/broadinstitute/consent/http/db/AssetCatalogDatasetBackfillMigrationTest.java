package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.broadinstitute.consent.http.enumeration.AssetType;
import org.broadinstitute.consent.http.models.AssetCatalogEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Runs the SQL of changelog-consent-2026-11-03-asset-catalog-dataset-backfill.xml over data.
 * Liquibase runs it at test startup against an empty schema, which proves only that it parses.
 */
class AssetCatalogDatasetBackfillMigrationTest extends DAOTestHelper {

  private static final String CHANGESET_ID =
      "changelog-consent-2026-11-03-asset-catalog-dataset-backfill";
  private static final Path CHANGESET =
      Path.of("src/main/resources/changesets/%s.xml".formatted(CHANGESET_ID));

  private AssetCatalogDAO catalogDAO;

  @BeforeEach
  void setUpDao() {
    catalogDAO = jdbi.onDemand(AssetCatalogDAO.class);
  }

  @Test
  void testEveryDatasetGetsOneEntryNumberedByItsAlias() throws Exception {
    Integer datasetId = insertDataset();
    Integer zeroAliasId = insertDataset();
    executeSql("UPDATE dataset SET alias = 0 WHERE dataset_id = %d".formatted(zeroAliasId));

    runMigration();

    AssetCatalogEntry entry =
        catalogDAO.findByTypeAndNumber(AssetType.DATASET, datasetAlias(datasetId));
    assertEquals(datasetId, entry.datasetId());
    assertEquals(datasetId, entry.sourceDatasetId());
    assertEquals(CHANGESET_ID, entry.backfillRun());
    assertEquals(zeroAliasId, catalogDAO.findByTypeAndNumber(AssetType.DATASET, 0).datasetId());
    assertEquals(2, countAssetCatalogEntries());
  }

  @Test
  void testADatasetThatHasAnEntryIsLeftAlone() throws Exception {
    Integer datasetId = insertDataset();
    catalogDAO.insertDatasetEntry(datasetId);

    runMigration();

    assertNull(
        catalogDAO.findByTypeAndNumber(AssetType.DATASET, datasetAlias(datasetId)).backfillRun());
    assertEquals(1, countAssetCatalogEntries());
  }

  @Test
  void testADatasetWhoseAliasAnotherEntryHoldsIsSkippedAndReported() throws Exception {
    Integer datasetId = insertDataset();
    executeSql(
        "INSERT INTO asset_catalog (asset_type, asset_number, asset_removed_date) VALUES ('DATASET', %d, now())"
            .formatted(datasetAlias(datasetId)));

    runMigration();

    assertEquals(1, countAssetCatalogEntries());
    List<Integer> reported =
        jdbi.withHandle(h -> h.createQuery(reportQuery()).mapTo(Integer.class).list());
    assertEquals(List.of(datasetId), reported);
  }

  @Test
  void testASecondRunChangesNothing() throws Exception {
    insertDataset();
    insertDataset();
    runMigration();
    int next = nextDatasetAlias();

    runMigration();

    assertEquals(2, countAssetCatalogEntries());
    assertEquals(next, nextDatasetAlias());
  }

  @Test
  void testTheSequenceMovesPastAnAliasAheadOfIt() throws Exception {
    Integer datasetId = insertDataset();
    int aheadAlias = nextDatasetAlias() + 10;
    executeSql(
        "UPDATE dataset SET alias = %d WHERE dataset_id = %d".formatted(aheadAlias, datasetId));

    runMigration();

    assertEquals(aheadAlias + 1, nextDatasetAlias());
  }

  @Test
  void testTheSequenceMovesPastARemovedEntryNumberAheadOfIt() throws Exception {
    int aheadNumber = nextDatasetAlias() + 10;
    executeSql(
        "INSERT INTO asset_catalog (asset_type, asset_number, asset_removed_date) VALUES ('DATASET', %d, now())"
            .formatted(aheadNumber));

    runMigration();

    assertEquals(aheadNumber + 1, nextDatasetAlias());
  }

  @Test
  void testTheSequenceStaysWhenItIsAhead() throws Exception {
    insertDataset();
    int next = nextDatasetAlias();

    runMigration();

    assertEquals(next, nextDatasetAlias());
  }

  /**
   * The changeset's statements, outside its rollback, in one transaction as Liquibase runs them.
   */
  private void runMigration() throws Exception {
    String forward = Files.readString(CHANGESET).split("<rollback>")[0];
    Matcher matcher = Pattern.compile("<sql>(.*?)</sql>", Pattern.DOTALL).matcher(forward);
    List<String> blocks = matcher.results().map(result -> unescape(result.group(1))).toList();
    assertEquals(4, blocks.size(), "changeset should ship four <sql> blocks");
    jdbi.useTransaction(handle -> blocks.forEach(block -> handle.execute(block.trim())));
  }

  /** The collision report query from the changeset's comment. */
  private String reportQuery() throws Exception {
    Matcher matcher =
        Pattern.compile("(SELECT d\\.dataset_id.*?);", Pattern.DOTALL)
            .matcher(Files.readString(CHANGESET));
    assertTrue(matcher.find(), "changeset should carry the collision report query");
    return matcher.group(1);
  }

  private static String unescape(String xml) {
    return xml.replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&");
  }

  private Integer insertDataset() {
    return datasetDAO.insertDataset(
        randomAlphabetic(20),
        Timestamp.from(Instant.now()),
        createUser().getUserId(),
        UUID.randomUUID().toString(),
        "{}",
        null);
  }
}
