package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.jdbi.v3.core.statement.UnableToExecuteStatementException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class AssetCatalogTableTest extends DAOTestHelper {

  @Test
  void testADuplicateTypeAndNumberIsRejected() {
    execute("INSERT INTO asset_catalog (asset_type, asset_number) VALUES ('MODEL', 7)");

    assertThrows(
        UnableToExecuteStatementException.class,
        () -> execute("INSERT INTO asset_catalog (asset_type, asset_number) VALUES ('MODEL', 7)"));
  }

  @Test
  void testTwoReferencesOnOneEntryAreRejected() {
    Integer studyId = insertStudy();
    Integer modelId = insertRow("study_model", studyId);
    Integer workspaceId = insertRow("study_workspace", studyId);
    String sql =
        "INSERT INTO asset_catalog (asset_type, asset_number, model_id, workspace_id) VALUES ('MODEL', %d, %d, %d)"
            .formatted(modelId, modelId, workspaceId);

    assertThrows(UnableToExecuteStatementException.class, () -> execute(sql));
  }

  @ParameterizedTest
  @CsvSource({
    "dataset_id, MODEL",
    "model_id, WORKSPACE",
    "workspace_id, BIOSPECIMEN",
    "biospecimen_id, DATASET"
  })
  void testAReferenceOfAnotherTypeIsRejected(String column, String type) {
    Integer id = insertAsset(column);
    String sql =
        "INSERT INTO asset_catalog (asset_type, asset_number, %s) VALUES ('%s', %d, %d)"
            .formatted(column, type, id, id);

    assertThrows(UnableToExecuteStatementException.class, () -> execute(sql));
  }

  @ParameterizedTest
  @CsvSource({"model_id, MODEL", "workspace_id, WORKSPACE", "biospecimen_id, BIOSPECIMEN"})
  void testAReferenceThatIsNotTheNumberIsRejected(String column, String type) {
    Integer id = insertAsset(column);
    String sql =
        "INSERT INTO asset_catalog (asset_type, asset_number, %s) VALUES ('%s', %d, %d)"
            .formatted(column, type, id + 1, id);

    assertThrows(UnableToExecuteStatementException.class, () -> execute(sql));
  }

  @ParameterizedTest
  @ValueSource(strings = {"('PUBLICATION', 1)", "('MODEL', -1)"})
  void testAnUnknownTypeOrNegativeNumberIsRejected(String values) {
    String sql = "INSERT INTO asset_catalog (asset_type, asset_number) VALUES " + values;

    assertThrows(UnableToExecuteStatementException.class, () -> execute(sql));
  }

  @Test
  void testDeletingAnAssetKeepsItsEntryAndNullsTheReference() {
    Integer studyId = insertStudy();
    Integer modelId = insertRow("study_model", studyId);
    Integer datasetId = insertDataset();
    execute(
        "INSERT INTO asset_catalog (asset_type, asset_number, model_id) VALUES ('MODEL', %d, %d)"
            .formatted(modelId, modelId));
    execute(
        """
        INSERT INTO asset_catalog (asset_type, asset_number, dataset_id, source_dataset_id)
        SELECT 'DATASET', alias, dataset_id, dataset_id FROM dataset WHERE dataset_id = %d
        """
            .formatted(datasetId));

    datasetDAO.deleteDatasetById(datasetId);
    studyDAO.deleteStudyByStudyId(studyId);

    assertNull(referenceOf("MODEL", "model_id"));
    assertNull(referenceOf("DATASET", "dataset_id"));
    assertEquals(datasetId, referenceOf("DATASET", "source_dataset_id"));
  }

  private Integer insertAsset(String column) {
    return switch (column) {
      case "dataset_id" -> insertDataset();
      case "model_id" -> insertRow("study_model", insertStudy());
      case "workspace_id" -> insertRow("study_workspace", insertStudy());
      default -> insertRow("study_biospecimen", insertStudy());
    };
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

  private Integer insertRow(String table, Integer studyId) {
    String idColumn = table.replace("study_", "") + "_id";
    return jdbi.withHandle(
        h ->
            h.createQuery(
                    "INSERT INTO %s (study_id) VALUES (:studyId) RETURNING %s"
                        .formatted(table, idColumn))
                .bind("studyId", studyId)
                .mapTo(Integer.class)
                .one());
  }

  private Integer referenceOf(String type, String column) {
    return jdbi.withHandle(
        h ->
            h.createQuery("SELECT %s FROM asset_catalog WHERE asset_type = :type".formatted(column))
                .bind("type", type)
                .mapTo(Integer.class)
                .one());
  }

  private void execute(String sql) {
    jdbi.useHandle(h -> h.execute(sql));
  }
}
