package org.broadinstitute.consent.http.service.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.broadinstitute.consent.http.db.AssetCatalogCheckDAO;
import org.broadinstitute.consent.http.db.AssetCatalogDAO;
import org.broadinstitute.consent.http.db.DAOTestHelper;
import org.broadinstitute.consent.http.enumeration.AssetType;
import org.broadinstitute.consent.http.exceptions.ConsentConflictException;
import org.broadinstitute.consent.http.models.AssetCatalogCheck;
import org.broadinstitute.consent.http.models.AssetCatalogEntry;
import org.broadinstitute.consent.http.models.AssetCatalogRepair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AssetCatalogServiceDAOTest extends DAOTestHelper {

  private AssetCatalogDAO catalogDAO;
  private AssetCatalogCheckDAO checkDAO;
  private AssetCatalogServiceDAO serviceDAO;

  @BeforeEach
  void setUpDaos() {
    catalogDAO = jdbi.onDemand(AssetCatalogDAO.class);
    checkDAO = jdbi.onDemand(AssetCatalogCheckDAO.class);
    serviceDAO = new AssetCatalogServiceDAO(jdbi);
  }

  @Test
  void testRepairStampsAnOrphanWhichIsThenNotReported() {
    int number = insertStudyAssetRow("study_model", insertStudy(), false) + 100;
    catalogDAO.insertAssetEntry(AssetType.MODEL, number - 100);
    executeSql(
        "INSERT INTO asset_catalog (asset_type, asset_number) VALUES ('MODEL', %d)"
            .formatted(number));

    AssetCatalogRepair repair = serviceDAO.repair();

    assertEquals(1, repair.stamped());
    assertNotNull(catalogDAO.findByTypeAndNumber(AssetType.MODEL, number).assetRemovedDate());
    assertEquals(0, checkDAO.check().orphans().count());
  }

  @Test
  void testRepairRelinksModelEntriesARestoreUnlinkedInsteadOfStampingThem() {
    Integer studyId = insertStudy();
    Integer unlinked = insertStudyAssetRow("study_model", studyId, false);
    Integer stampedBack = insertStudyAssetRow("study_model", studyId, true);
    catalogDAO.insertAssetEntry(AssetType.MODEL, unlinked);
    catalogDAO.insertAssetEntry(AssetType.MODEL, stampedBack);
    executeSql(
        "UPDATE asset_catalog SET model_id = NULL WHERE model_id IN (%d, %d)"
            .formatted(unlinked, stampedBack));
    executeSql(
        "UPDATE asset_catalog SET asset_removed_date = now() WHERE asset_number = %d"
            .formatted(stampedBack));

    AssetCatalogRepair repair = serviceDAO.repair();

    assertEquals(new AssetCatalogRepair(0, 2, 0, 0, null), repair);
    for (Integer modelId : List.of(unlinked, stampedBack)) {
      AssetCatalogEntry entry = catalogDAO.findByTypeAndNumber(AssetType.MODEL, modelId);
      assertEquals(modelId, entry.modelId());
      assertNull(entry.assetRemovedDate());
    }
  }

  @Test
  void testRepairRelinksADatasetEntryWhoseDatasetARestoreBroughtBack() {
    Integer datasetId = insertDataset();
    int alias = datasetAlias(datasetId);
    catalogDAO.insertDatasetEntry(datasetId);
    executeSql(
        "UPDATE asset_catalog SET dataset_id = NULL WHERE dataset_id = %d".formatted(datasetId));

    AssetCatalogRepair repair = serviceDAO.repair();

    assertEquals(1, repair.relinked());
    assertEquals(datasetId, catalogDAO.findByTypeAndNumber(AssetType.DATASET, alias).datasetId());
  }

  @Test
  void testRepairLeavesAStampedDatasetEntryWhoseAliasANewDatasetReused() {
    Integer removedId = insertDataset();
    int alias = datasetAlias(removedId);
    catalogDAO.insertDatasetEntry(removedId);
    catalogDAO.markRemovedByDataset(removedId);
    datasetDAO.deleteDatasetById(removedId);
    Integer reuserId = insertDataset();
    executeSql("UPDATE dataset SET alias = %d WHERE dataset_id = %d".formatted(alias, reuserId));
    AssetCatalogEntry before = catalogDAO.findByTypeAndNumber(AssetType.DATASET, alias);

    AssetCatalogRepair repair = serviceDAO.repair();

    assertEquals(before, catalogDAO.findByTypeAndNumber(AssetType.DATASET, alias));
    assertEquals(1, repair.skipped());
    assertEquals(1, checkDAO.check().collisions().count());
  }

  @Test
  void testRepairInsertsOnlyMissingEntriesSkipsCollisionsAndASecondRepairChangesNothing() {
    Integer datasetId = insertDataset();
    Integer workspaceId = insertStudyAssetRow("study_workspace", insertStudy(), true);
    Integer collidedId = insertDataset();
    executeSql(
        "INSERT INTO asset_catalog (asset_type, asset_number, asset_removed_date) VALUES ('DATASET', %d, now())"
            .formatted(datasetAlias(collidedId)));
    Integer linkedId = insertDataset();
    catalogDAO.insertDatasetEntry(linkedId);

    AssetCatalogRepair first = serviceDAO.repair();
    AssetCatalogRepair second = serviceDAO.repair();

    assertEquals(new AssetCatalogRepair(2, 0, 0, 1, null), first);
    assertEquals(
        datasetId,
        catalogDAO.findByTypeAndNumber(AssetType.DATASET, datasetAlias(datasetId)).datasetId());
    assertEquals(
        workspaceId,
        catalogDAO.findByTypeAndNumber(AssetType.WORKSPACE, workspaceId).workspaceId());
    assertEquals(new AssetCatalogRepair(0, 0, 0, 1, null), second);
    assertEquals(4, countAssetCatalogEntries());
  }

  @Test
  void testRepairAdvancesASequenceAnAdHocInsertLeftBehind() {
    int ahead = nextDatasetAlias() + 10;
    executeSql(
        "INSERT INTO asset_catalog (asset_type, asset_number, asset_removed_date) VALUES ('DATASET', %d, now())"
            .formatted(ahead));

    AssetCatalogRepair repair = serviceDAO.repair();

    assertEquals(Long.valueOf(ahead), repair.aliasSequenceAdvancedTo());
    assertEquals(ahead + 1, nextDatasetAlias());
    AssetCatalogCheck check = checkDAO.check();
    assertEquals(0, check.sequenceBehind().count());
  }

  @Test
  void testASecondRepairWhileOneRunsIsAConflict() {
    jdbi.useHandle(
        holder -> {
          holder.begin();
          assertTrue(
              holder
                  .createQuery("SELECT pg_try_advisory_xact_lock(hashtext('asset_catalog_repair'))")
                  .mapTo(Boolean.class)
                  .one());

          // Another thread, so the repair gets its own session rather than reusing this handle.
          CompletionException thrown =
              assertThrows(
                  CompletionException.class,
                  () -> CompletableFuture.supplyAsync(serviceDAO::repair).join());
          assertInstanceOf(ConsentConflictException.class, thrown.getCause());

          holder.rollback();
        });
    assertEquals(new AssetCatalogRepair(0, 0, 0, 0, null), serviceDAO.repair());
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
