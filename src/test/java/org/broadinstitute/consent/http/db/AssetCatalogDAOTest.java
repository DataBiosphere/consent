package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.broadinstitute.consent.http.enumeration.AssetType;
import org.broadinstitute.consent.http.models.AssetCatalogEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AssetCatalogDAOTest extends DAOTestHelper {

  private AssetCatalogDAO catalogDAO;

  @BeforeEach
  void setUpDao() {
    catalogDAO = jdbi.onDemand(AssetCatalogDAO.class);
  }

  @Test
  void testInsertDatasetEntryNumbersItByAlias() {
    Integer datasetId = insertDataset();
    int alias = aliasOf(datasetId);

    assertEquals(1, catalogDAO.insertDatasetEntry(datasetId));

    AssetCatalogEntry entry = catalogDAO.findByTypeAndNumber(AssetType.DATASET, alias);
    assertEquals(datasetId, entry.datasetId());
    assertEquals(datasetId, entry.sourceDatasetId());
    assertNotNull(entry.createDate());
    assertNull(entry.assetRemovedDate());
    assertNull(entry.backfillRun());
  }

  @Test
  void testInsertDatasetEntrySkipsAnAliasAnotherEntryHolds() {
    Integer datasetId = insertDataset();
    insertBareEntry(AssetType.DATASET, aliasOf(datasetId));

    assertEquals(0, catalogDAO.insertDatasetEntry(datasetId));
  }

  @Test
  void testInsertAssetEntryNumbersEachTypeByRowId() {
    Integer studyId = insertStudy();
    Integer modelId = insertRow("study_model", studyId, false);
    Integer workspaceId = insertRow("study_workspace", studyId, false);
    Integer biospecimenId = insertRow("study_biospecimen", studyId, false);

    catalogDAO.insertAssetEntry(AssetType.MODEL, modelId);
    catalogDAO.insertAssetEntry(AssetType.WORKSPACE, workspaceId);
    catalogDAO.insertAssetEntry(AssetType.BIOSPECIMEN, biospecimenId);

    assertEquals(modelId, catalogDAO.findByTypeAndNumber(AssetType.MODEL, modelId).modelId());
    assertEquals(
        workspaceId,
        catalogDAO.findByTypeAndNumber(AssetType.WORKSPACE, workspaceId).workspaceId());
    AssetCatalogEntry biospecimen =
        catalogDAO.findByTypeAndNumber(AssetType.BIOSPECIMEN, biospecimenId);
    assertEquals(biospecimenId, biospecimen.biospecimenId());
    assertNull(biospecimen.modelId());
    assertNull(biospecimen.sourceDatasetId());
  }

  @Test
  void testInsertAssetEntryRejectsADataset() {
    Integer datasetId = insertDataset();

    assertThrows(
        IllegalArgumentException.class,
        () -> catalogDAO.insertAssetEntry(AssetType.DATASET, datasetId));
  }

  @Test
  void testFindByTypeAndNumberSeparatesTypes() {
    Integer studyId = insertStudy();
    Integer modelId = insertRow("study_model", studyId, false);
    catalogDAO.insertAssetEntry(AssetType.MODEL, modelId);

    assertNull(catalogDAO.findByTypeAndNumber(AssetType.WORKSPACE, modelId));
  }

  @Test
  void testEnsureDatasetEntryInsertsAMissingEntry() {
    Integer datasetId = insertDataset();

    assertNull(catalogDAO.ensureDatasetEntry(datasetId));

    assertEquals(
        datasetId,
        catalogDAO.findByTypeAndNumber(AssetType.DATASET, aliasOf(datasetId)).datasetId());
  }

  @Test
  void testEnsureDatasetEntryLeavesAnExistingEntryAlone() {
    Integer datasetId = insertDataset();
    catalogDAO.insertDatasetEntry(datasetId);

    assertNull(catalogDAO.ensureDatasetEntry(datasetId));

    assertEquals(1, countEntries());
  }

  @Test
  void testEnsureDatasetEntryRelinksTheEntryARestoreUnlinkedAndClearsItsStamp() {
    Integer datasetId = insertDataset();
    catalogDAO.insertDatasetEntry(datasetId);
    catalogDAO.markRemovedByDataset(datasetId);
    unlink("dataset_id", datasetId);

    assertNull(catalogDAO.ensureDatasetEntry(datasetId));

    assertEquals(1, countEntries());
    AssetCatalogEntry entry = catalogDAO.findByTypeAndNumber(AssetType.DATASET, aliasOf(datasetId));
    assertEquals(datasetId, entry.datasetId());
    assertNull(entry.assetRemovedDate());
  }

  @Test
  void testEnsureDatasetEntryLeavesAnEntryWhoseAliasChanged() {
    Integer datasetId = insertDataset();
    catalogDAO.insertDatasetEntry(datasetId);
    int oldAlias = aliasOf(datasetId);
    execute(
        "UPDATE dataset SET alias = %d WHERE dataset_id = %d"
            .formatted(oldAlias + 100000, datasetId));

    assertNull(catalogDAO.ensureDatasetEntry(datasetId));

    assertEquals(1, countEntries());
    assertEquals(
        datasetId, catalogDAO.findByTypeAndNumber(AssetType.DATASET, oldAlias).datasetId());
  }

  @Test
  void testEnsureDatasetEntryReturnsNullForAMissingDataset() {
    assertNull(catalogDAO.ensureDatasetEntry(Integer.MAX_VALUE));

    assertEquals(0, countEntries());
  }

  @Test
  void testEnsureDatasetEntryReportsAnAliasAnUnownedEntryHolds() {
    Integer datasetId = insertDataset();
    int alias = aliasOf(datasetId);
    insertBareEntry(AssetType.DATASET, alias);

    AssetCatalogEntry collision = catalogDAO.ensureDatasetEntry(datasetId);

    assertNotNull(collision);
    assertEquals(alias, collision.assetNumber());
    assertNull(collision.datasetId());
    assertEquals(1, countEntries());
  }

  @Test
  void testEnsureDatasetEntryDoesNotRelinkAnotherDatasetsEntry() {
    Integer datasetId = insertDataset();
    Integer otherId = insertDataset();
    int alias = aliasOf(datasetId);
    jdbi.useHandle(
        h ->
            h.createUpdate(
                    """
                    INSERT INTO asset_catalog (asset_type, asset_number, source_dataset_id)
                    VALUES ('DATASET', :alias, :otherId)
                    """)
                .bind("alias", alias)
                .bind("otherId", otherId)
                .execute());

    AssetCatalogEntry collision = catalogDAO.ensureDatasetEntry(datasetId);

    assertNotNull(collision);
    assertNull(collision.datasetId());
    assertEquals(otherId, collision.sourceDatasetId());
  }

  @Test
  void testEnsureStudyEntriesCoversLiveAndRetiredRows() {
    Integer studyId = insertStudy();
    Integer liveModel = insertRow("study_model", studyId, false);
    Integer retiredModel = insertRow("study_model", studyId, true);
    Integer workspace = insertRow("study_workspace", studyId, true);
    Integer biospecimen = insertRow("study_biospecimen", studyId, false);
    Integer otherStudyModel = insertRow("study_model", insertStudy(), false);
    catalogDAO.insertAssetEntry(AssetType.MODEL, liveModel);

    catalogDAO.ensureStudyEntries(studyId);
    catalogDAO.ensureStudyEntries(studyId);

    assertEquals(4, countEntries());
    assertEquals(
        retiredModel, catalogDAO.findByTypeAndNumber(AssetType.MODEL, retiredModel).modelId());
    assertEquals(
        workspace, catalogDAO.findByTypeAndNumber(AssetType.WORKSPACE, workspace).workspaceId());
    assertEquals(
        biospecimen,
        catalogDAO.findByTypeAndNumber(AssetType.BIOSPECIMEN, biospecimen).biospecimenId());
    assertNull(catalogDAO.findByTypeAndNumber(AssetType.MODEL, otherStudyModel));
  }

  @Test
  void testRestoredRowsAreRelinkedWithTheirStampsClearedAndThenStamped() {
    Integer studyId = insertStudy();
    Integer model = insertRow("study_model", studyId, false);
    Integer workspace = insertRow("study_workspace", studyId, true);
    Integer biospecimen = insertRow("study_biospecimen", studyId, false);
    catalogDAO.ensureStudyEntries(studyId);
    catalogDAO.markRemovedByStudy(studyId);
    unlink("model_id", model);
    unlink("workspace_id", workspace);
    unlink("biospecimen_id", biospecimen);

    catalogDAO.ensureStudyEntries(studyId);

    AssetCatalogEntry modelEntry = catalogDAO.findByTypeAndNumber(AssetType.MODEL, model);
    AssetCatalogEntry workspaceEntry =
        catalogDAO.findByTypeAndNumber(AssetType.WORKSPACE, workspace);
    AssetCatalogEntry biospecimenEntry =
        catalogDAO.findByTypeAndNumber(AssetType.BIOSPECIMEN, biospecimen);
    assertEquals(model, modelEntry.modelId());
    assertEquals(workspace, workspaceEntry.workspaceId());
    assertEquals(biospecimen, biospecimenEntry.biospecimenId());
    assertNull(modelEntry.assetRemovedDate());
    assertNull(workspaceEntry.assetRemovedDate());
    assertNull(biospecimenEntry.assetRemovedDate());
    assertEquals(3, countEntries());
    assertEquals(3, catalogDAO.markRemovedByStudy(studyId));
  }

  @Test
  void testMarkRemovedByStudyLeavesAnUnlinkedEntry() {
    Integer studyId = insertStudy();
    Integer model = insertRow("study_model", studyId, false);
    catalogDAO.insertAssetEntry(AssetType.MODEL, model);
    unlink("model_id", model);

    assertEquals(0, catalogDAO.markRemovedByStudy(studyId));
  }

  @Test
  void testMarkRemovedByDatasetStampsOnceAndTheDeleteNullsTheReference() {
    Integer datasetId = insertDataset();
    int alias = aliasOf(datasetId);
    catalogDAO.insertDatasetEntry(datasetId);

    assertEquals(1, catalogDAO.markRemovedByDataset(datasetId));
    Timestamp stamped = catalogDAO.findByTypeAndNumber(AssetType.DATASET, alias).assetRemovedDate();
    assertEquals(0, catalogDAO.markRemovedByDataset(datasetId));
    datasetDAO.deleteDatasetById(datasetId);

    AssetCatalogEntry entry = catalogDAO.findByTypeAndNumber(AssetType.DATASET, alias);
    assertNotNull(stamped);
    assertEquals(stamped, entry.assetRemovedDate());
    assertNull(entry.datasetId());
    assertEquals(datasetId, entry.sourceDatasetId());
  }

  @Test
  void testMarkRemovedByStudyStampsOnlyThatStudysRows() {
    Integer studyId = insertStudy();
    Integer model = insertRow("study_model", studyId, true);
    Integer workspace = insertRow("study_workspace", studyId, false);
    Integer biospecimen = insertRow("study_biospecimen", studyId, false);
    Integer otherModel = insertRow("study_model", insertStudy(), false);
    catalogDAO.insertAssetEntry(AssetType.MODEL, model);
    catalogDAO.insertAssetEntry(AssetType.WORKSPACE, workspace);
    catalogDAO.insertAssetEntry(AssetType.BIOSPECIMEN, biospecimen);
    catalogDAO.insertAssetEntry(AssetType.MODEL, otherModel);

    assertEquals(3, catalogDAO.markRemovedByStudy(studyId));
    assertEquals(0, catalogDAO.markRemovedByStudy(studyId));

    assertNotNull(catalogDAO.findByTypeAndNumber(AssetType.MODEL, model).assetRemovedDate());
    assertNull(catalogDAO.findByTypeAndNumber(AssetType.MODEL, otherModel).assetRemovedDate());
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

  private int aliasOf(Integer datasetId) {
    return datasetDAO.findDatasetById(datasetId).getAlias();
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

  private Integer insertRow(String table, Integer studyId, boolean retired) {
    String idColumn = table.replace("study_", "") + "_id";
    return jdbi.withHandle(
        h ->
            h.createQuery(
                    "INSERT INTO %s (study_id, retired_date) VALUES (:studyId, %s) RETURNING %s"
                        .formatted(table, retired ? "now()" : "NULL", idColumn))
                .bind("studyId", studyId)
                .mapTo(Integer.class)
                .one());
  }

  private void insertBareEntry(AssetType type, int number) {
    jdbi.useHandle(
        h ->
            h.createUpdate(
                    "INSERT INTO asset_catalog (asset_type, asset_number) VALUES (:type, :number)")
                .bind("type", type)
                .bind("number", number)
                .execute());
  }

  /** What a restore of the asset tables without the catalog leaves: the reference, nulled. */
  private void unlink(String column, Integer id) {
    execute("UPDATE asset_catalog SET %s = NULL WHERE %s = %d".formatted(column, column, id));
  }

  private int countEntries() {
    return jdbi.withHandle(
        h -> h.createQuery("SELECT count(*) FROM asset_catalog").mapTo(Integer.class).one());
  }

  private void execute(String sql) {
    jdbi.useHandle(h -> h.execute(sql));
  }
}
