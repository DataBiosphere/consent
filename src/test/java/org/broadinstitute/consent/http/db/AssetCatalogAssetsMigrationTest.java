package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
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
 * Runs the SQL of changelog-consent-2026-11-04-asset-catalog-assets.xml over data. Liquibase runs
 * it at test startup against an empty schema, which proves only that it parses.
 */
class AssetCatalogAssetsMigrationTest extends DAOTestHelper {

  private static final String CHANGESET_ID = "changelog-consent-2026-11-04-asset-catalog-assets";
  private static final Path CHANGESET =
      Path.of("src/main/resources/changesets/%s.xml".formatted(CHANGESET_ID));

  private AssetCatalogDAO catalogDAO;

  @BeforeEach
  void setUpDao() {
    catalogDAO = jdbi.onDemand(AssetCatalogDAO.class);
  }

  @Test
  void testLiveAndRetiredRowsEachGetOneEntryNumberedByTheirRowId() throws Exception {
    Integer studyId = insertStudy();
    Integer liveModel = insertStudyAssetRow("study_model", studyId, false);
    Integer retiredModel = insertStudyAssetRow("study_model", studyId, true);
    Integer workspace = insertStudyAssetRow("study_workspace", studyId, true);
    Integer biospecimen = insertStudyAssetRow("study_biospecimen", studyId, false);

    runMigration();

    AssetCatalogEntry live = catalogDAO.findByTypeAndNumber(AssetType.MODEL, liveModel);
    assertEquals(liveModel, live.modelId());
    assertEquals(CHANGESET_ID, live.backfillRun());
    assertEquals(
        retiredModel, catalogDAO.findByTypeAndNumber(AssetType.MODEL, retiredModel).modelId());
    assertEquals(
        workspace, catalogDAO.findByTypeAndNumber(AssetType.WORKSPACE, workspace).workspaceId());
    assertEquals(
        biospecimen,
        catalogDAO.findByTypeAndNumber(AssetType.BIOSPECIMEN, biospecimen).biospecimenId());
    assertEquals(4, countAssetCatalogEntries());
  }

  @Test
  void testARowThatHasAnEntryIsLeftAlone() throws Exception {
    Integer modelId = insertStudyAssetRow("study_model", insertStudy(), false);
    catalogDAO.insertAssetEntry(AssetType.MODEL, modelId);

    runMigration();

    assertNull(catalogDAO.findByTypeAndNumber(AssetType.MODEL, modelId).backfillRun());
    assertEquals(1, countAssetCatalogEntries());
  }

  @Test
  void testARowWhoseNumberAnUnlinkedEntryHoldsIsSkippedAndReported() throws Exception {
    Integer studyId = insertStudy();
    Integer modelId = insertStudyAssetRow("study_model", studyId, false);
    Integer workspaceId = insertStudyAssetRow("study_workspace", studyId, false);
    executeSql(
        "INSERT INTO asset_catalog (asset_type, asset_number) VALUES ('WORKSPACE', %d)"
            .formatted(workspaceId));

    runMigration();

    assertNull(catalogDAO.findByTypeAndNumber(AssetType.WORKSPACE, workspaceId).workspaceId());
    assertEquals(modelId, catalogDAO.findByTypeAndNumber(AssetType.MODEL, modelId).modelId());
    List<Integer> reported =
        jdbi.withHandle(
            h -> h.createQuery(reportQuery()).map((rs, ctx) -> rs.getInt("row_id")).list());
    assertEquals(List.of(workspaceId), reported);
  }

  @Test
  void testASecondRunChangesNothing() throws Exception {
    Integer studyId = insertStudy();
    insertStudyAssetRow("study_model", studyId, false);
    insertStudyAssetRow("study_biospecimen", studyId, true);
    runMigration();

    runMigration();

    assertEquals(2, countAssetCatalogEntries());
  }

  @Test
  void testRollbackRemovesOnlyThisBackfillsEntriesForLiveRows() throws Exception {
    Integer studyId = insertStudy();
    Integer appModel = insertStudyAssetRow("study_model", studyId, false);
    catalogDAO.insertAssetEntry(AssetType.MODEL, appModel);
    Integer backfilledModel = insertStudyAssetRow("study_model", studyId, false);
    Integer deletedModel = insertStudyAssetRow("study_model", studyId, false);
    Integer unlinkedModel = insertStudyAssetRow("study_model", studyId, false);
    Integer workspace = insertStudyAssetRow("study_workspace", studyId, false);
    Integer biospecimen = insertStudyAssetRow("study_biospecimen", studyId, true);
    runMigration();
    executeSql(
        """
        UPDATE asset_catalog SET asset_removed_date = now() WHERE model_id = %d;
        DELETE FROM study_model WHERE model_id IN (%d, %d)
        """
            .formatted(deletedModel, deletedModel, unlinkedModel));

    jdbi.useHandle(h -> h.execute(rollbackSql()));

    assertEquals(appModel, catalogDAO.findByTypeAndNumber(AssetType.MODEL, appModel).modelId());
    assertNull(catalogDAO.findByTypeAndNumber(AssetType.MODEL, backfilledModel));
    assertNull(catalogDAO.findByTypeAndNumber(AssetType.WORKSPACE, workspace));
    assertNull(catalogDAO.findByTypeAndNumber(AssetType.BIOSPECIMEN, biospecimen));
    assertEquals(
        CHANGESET_ID, catalogDAO.findByTypeAndNumber(AssetType.MODEL, deletedModel).backfillRun());
    AssetCatalogEntry unlinked = catalogDAO.findByTypeAndNumber(AssetType.MODEL, unlinkedModel);
    assertNull(unlinked.modelId());
    assertNull(unlinked.assetRemovedDate());
    assertEquals(3, countAssetCatalogEntries());
  }

  /**
   * The changeset's statements, outside its rollback, in one transaction as Liquibase runs them.
   */
  private void runMigration() throws Exception {
    String forward = Files.readString(CHANGESET).split("<rollback>")[0];
    List<String> blocks = sqlBlocks(forward);
    assertEquals(5, blocks.size(), "changeset should ship five <sql> blocks");
    jdbi.useTransaction(handle -> blocks.forEach(handle::execute));
  }

  private String rollbackSql() throws Exception {
    List<String> blocks = sqlBlocks(Files.readString(CHANGESET).split("<rollback>")[1]);
    assertEquals(1, blocks.size(), "rollback should ship one <sql> block");
    return blocks.get(0);
  }

  /** The relinkable-row report query from the changeset's comment. */
  private String reportQuery() throws Exception {
    Matcher matcher =
        Pattern.compile("(SELECT r\\.asset_type.*?);", Pattern.DOTALL)
            .matcher(Files.readString(CHANGESET));
    assertTrue(matcher.find(), "changeset should carry the relinkable-row report query");
    return matcher.group(1);
  }

  private Integer insertStudy() {
    return studyDAO.insertStudy(
        randomAlphabetic(20),
        randomAlphabetic(20),
        randomAlphabetic(20),
        null,
        List.of(randomAlphabetic(10)),
        true,
        createUser().getUserId(),
        Instant.now(),
        UUID.randomUUID());
  }
}
