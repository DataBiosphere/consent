package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.broadinstitute.consent.http.enumeration.AssetType;
import org.broadinstitute.consent.http.models.AssetCatalogCheck;
import org.broadinstitute.consent.http.models.AssetCatalogCheck.Finding;
import org.broadinstitute.consent.http.models.AssetCatalogCheck.Findings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AssetCatalogCheckTest extends DAOTestHelper {

  private AssetCatalogDAO catalogDAO;
  private AssetCatalogCheckDAO checkDAO;

  @BeforeEach
  void setUpDaos() {
    catalogDAO = jdbi.onDemand(AssetCatalogDAO.class);
    checkDAO = jdbi.onDemand(AssetCatalogCheckDAO.class);
  }

  @Test
  void testAConsistentCatalogHasNoFindings() {
    Integer datasetId = insertDataset();
    catalogDAO.insertDatasetEntry(datasetId);
    Integer modelId = insertStudyAssetRow("study_model", insertStudy(), false);
    catalogDAO.insertAssetEntry(AssetType.MODEL, modelId);

    AssetCatalogCheck check = checkDAO.check();

    List.of(
            check.orphans(),
            check.missing(),
            check.collisions(),
            check.mismatches(),
            check.relinkable(),
            check.sequenceBehind())
        .forEach(findings -> assertEquals(new Findings(0, List.of()), findings));
  }

  @Test
  void testAnUnstampedEntryThatReferencesNothingIsAnOrphan() {
    int number = insertStudyAssetRow("study_workspace", insertStudy(), false) + 100;
    int orphan = insertBareEntry(AssetType.WORKSPACE, number, null, false);
    insertBareEntry(AssetType.WORKSPACE, number + 1, null, true);

    assertEquals(
        List.of(new Finding(AssetType.WORKSPACE, number, orphan, null)),
        checkDAO.check().orphans().items());
  }

  @Test
  void testARowWithNoEntryAndAFreeNumberIsMissing() {
    Integer datasetId = insertDataset();
    Integer biospecimenId = insertStudyAssetRow("study_biospecimen", insertStudy(), true);

    assertEquals(
        List.of(
            new Finding(AssetType.BIOSPECIMEN, biospecimenId, null, biospecimenId),
            new Finding(AssetType.DATASET, datasetAlias(datasetId), null, datasetId)),
        checkDAO.check().missing().items());
  }

  @Test
  void testADatasetWhoseAliasAnotherEntryHoldsIsACollisionOnly() {
    Integer datasetId = insertDataset();
    int alias = datasetAlias(datasetId);
    int entry = insertBareEntry(AssetType.DATASET, alias, null, true);

    AssetCatalogCheck check = checkDAO.check();

    assertEquals(
        List.of(new Finding(AssetType.DATASET, alias, entry, datasetId)),
        check.collisions().items());
    assertEquals(0, check.missing().count());
    assertEquals(0, check.relinkable().count());
  }

  @Test
  void testAnUnlinkedEntryWhoseRowIsBackIsRelinkableNotAnOrphan() {
    Integer modelId = insertStudyAssetRow("study_model", insertStudy(), false);
    int modelEntry = insertBareEntry(AssetType.MODEL, modelId, null, false);
    Integer datasetId = insertDataset();
    int alias = datasetAlias(datasetId);
    int datasetEntry = insertBareEntry(AssetType.DATASET, alias, datasetId, true);

    AssetCatalogCheck check = checkDAO.check();

    assertEquals(
        List.of(
            new Finding(AssetType.DATASET, alias, datasetEntry, datasetId),
            new Finding(AssetType.MODEL, modelId, modelEntry, modelId)),
        check.relinkable().items());
    assertEquals(0, check.orphans().count());
    assertEquals(0, check.missing().count());
    assertEquals(0, check.collisions().count());
  }

  @Test
  void testADatasetEntryWhoseAliasChangedIsAMismatch() {
    Integer datasetId = insertDataset();
    int alias = datasetAlias(datasetId);
    catalogDAO.insertDatasetEntry(datasetId);
    int entry = catalogDAO.findByTypeAndNumber(AssetType.DATASET, alias).catalogId();
    executeSql(
        "UPDATE dataset SET alias = %d WHERE dataset_id = %d".formatted(alias + 1000, datasetId));

    assertEquals(
        List.of(new Finding(AssetType.DATASET, alias, entry, datasetId)),
        checkDAO.check().mismatches().items());
  }

  @Test
  void testAnAliasOrEntryNumberTheSequenceWouldReissueIsReported() {
    Integer datasetId = insertDataset();
    int next = nextDatasetAlias();
    executeSql(
        "UPDATE dataset SET alias = %d WHERE dataset_id = %d".formatted(next + 5, datasetId));
    int entry = insertBareEntry(AssetType.DATASET, next + 10, null, true);

    assertEquals(
        List.of(
            new Finding(AssetType.DATASET, next + 5, null, datasetId),
            new Finding(AssetType.DATASET, next + 10, entry, null)),
        checkDAO.check().sequenceBehind().items());
  }

  @Test
  void testTheCheckChangesNothing() {
    insertDataset();
    Integer modelId = insertStudyAssetRow("study_model", insertStudy(), false);
    insertBareEntry(AssetType.MODEL, modelId, null, false);
    insertBareEntry(AssetType.WORKSPACE, modelId + 100, null, false);
    String before = catalogSnapshot();

    AssetCatalogCheck check = checkDAO.check();

    assertTrue(check.missing().count() > 0);
    assertEquals(before, catalogSnapshot());
  }

  @Test
  void testFindingsAreCappedButCountedInFull() {
    Integer studyId = insertStudy();
    Integer first = insertStudyAssetRow("study_model", studyId, false);
    insertStudyAssetRow("study_model", studyId, false);

    List<AssetCatalogCheckDAO.FindingRow> rows = checkDAO.findMissing(1);

    assertEquals(1, rows.size());
    assertEquals(first, rows.get(0).rowId());
    assertEquals(2, rows.get(0).total());
  }

  /** An entry with no reference, as a restore or an ad hoc insert leaves one. */
  private int insertBareEntry(
      AssetType type, int number, Integer sourceDatasetId, boolean stamped) {
    return jdbi.withHandle(
        h ->
            h.createQuery(
                    """
                    INSERT INTO asset_catalog
                        (asset_type, asset_number, source_dataset_id, asset_removed_date)
                    VALUES (:type, :number, :source, CASE WHEN :stamped THEN now() END)
                    RETURNING catalog_id
                    """)
                .bind("type", type)
                .bind("number", number)
                .bind("source", sourceDatasetId)
                .bind("stamped", stamped)
                .mapTo(Integer.class)
                .one());
  }

  private String catalogSnapshot() {
    return jdbi.withHandle(
        h ->
            h.createQuery(
                    """
                    SELECT string_agg(c::text, ';' ORDER BY catalog_id)
                    FROM asset_catalog c
                    """)
                .mapTo(String.class)
                .one());
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
