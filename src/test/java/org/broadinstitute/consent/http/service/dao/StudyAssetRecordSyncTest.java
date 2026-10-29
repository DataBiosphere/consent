package org.broadinstitute.consent.http.service.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.broadinstitute.consent.http.db.DAOTestHelper;
import org.broadinstitute.consent.http.db.StudyAssetRecordDAO;
import org.broadinstitute.consent.http.enumeration.PropertyType;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.models.Study;
import org.broadinstitute.consent.http.models.StudyModelRecord;
import org.broadinstitute.consent.http.models.StudyProperty;
import org.broadinstitute.consent.http.models.StudyWorkspaceRecord;
import org.broadinstitute.consent.http.service.dao.DatasetServiceDAO.DatasetInsert;
import org.broadinstitute.consent.http.service.dao.DatasetServiceDAO.StudyInsert;
import org.broadinstitute.consent.http.service.dao.DatasetServiceDAO.StudyUpdate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class StudyAssetRecordSyncTest extends DAOTestHelper {

  private DatasetServiceDAO serviceDAO;
  private StudyAssetRecordDAO assetDAO;

  @BeforeEach
  void setUp() {
    serviceDAO = new DatasetServiceDAO(jdbi);
    assetDAO = jdbi.onDemand(StudyAssetRecordDAO.class);
  }

  @Test
  void testCreateStoresEveryEntryAndWritesItsIdBack() throws Exception {
    Integer studyId =
        create(
            prop(
                "models",
                """
                [{"modelId": "", "name": "M", "cloud": ["GCP"], "maintainer": {"name": "Lab"},
                  "dataUse": {"generalUse": true}, "accessManagement": "open"}]"""),
            prop(
                "workspaces",
                """
                [{"workspaceId": "a1b2", "name": "W", "tools": ["Jupyter"]}]"""));

    StudyModelRecord model = assetDAO.findModelsByStudyId(studyId).getFirst();
    assertEquals("M", model.name());
    assertEquals(List.of("GCP"), model.cloud());
    assertEquals(
        JsonParser.parseString("{\"name\":\"Lab\"}"), JsonParser.parseString(model.maintainer()));
    assertEquals("open", model.accessManagement());
    assertNull(model.legacyKey());
    assertEquals(
        model.modelId(),
        stored(studyId, "models").get(0).getAsJsonObject().get("modelId").getAsInt());

    StudyWorkspaceRecord workspace = assetDAO.findWorkspacesByStudyId(studyId).getFirst();
    assertEquals("a1b2", workspace.legacyKey());
    assertEquals(List.of("Jupyter"), workspace.tools());
    assertEquals(workspace.workspaceId(), idOf(stored(studyId, "workspaces"), 0, "workspaceId"));
  }

  @Test
  void testModelsWithEmptyIdsGetTheirOwnRows() throws Exception {
    Integer studyId = create(prop("models", "[{\"modelId\": \"\"}, {\"modelId\": \"\"}]"));

    JsonArray models = stored(studyId, "models");
    assertEquals(2, assetDAO.findModelsByStudyId(studyId).size());
    assertNotEquals(idOf(models, 0, "modelId"), idOf(models, 1, "modelId"));
  }

  @Test
  void testUpdateKeepsIdsAcrossEditsAndReordering() throws Exception {
    Integer studyId = create(prop("models", "[{\"name\": \"A\"}, {\"name\": \"B\"}]"));
    JsonArray models = stored(studyId, "models");
    int a = idOf(models, 0, "modelId");
    int b = idOf(models, 1, "modelId");

    update(
        studyId,
        prop(
            "models",
            "[{\"modelId\": %d, \"name\": \"B2\"}, {\"modelId\": \"%d\"}]".formatted(b, a)));

    List<StudyModelRecord> rows = assetDAO.findModelsByStudyId(studyId);
    assertEquals(List.of(a, b), rows.stream().map(StudyModelRecord::modelId).toList());
    assertEquals("B2", rows.get(1).name());
    assertTrue(rows.stream().allMatch(row -> row.retiredDate() == null));
    assertEquals(List.of(b, a), ids(stored(studyId, "models"), "modelId"));
  }

  @Test
  void testRemovedEntriesAreRetired() throws Exception {
    Integer studyId =
        create(
            prop("models", "[{\"name\": \"A\"}, {\"name\": \"B\"}]"),
            prop("workspaces", "[{\"name\": \"W\"}]"));
    int b = idOf(stored(studyId, "models"), 1, "modelId");

    update(studyId, prop("models", "[{\"modelId\": %d}]".formatted(b)));

    assertEquals(List.of(b), assetDAO.findLiveModels(studyId).stream().map(k -> k.id()).toList());
    assertTrue(assetDAO.findLiveWorkspaces(studyId).isEmpty());

    update(studyId, prop("models", "[]"));

    assertTrue(assetDAO.findLiveModels(studyId).isEmpty());
    assertEquals(2, assetDAO.findModelsByStudyId(studyId).size());
  }

  @Test
  void testIdsThatClaimNoLiveRowGetNewRows() throws Exception {
    Integer other = create(prop("models", "[{\"name\": \"Other\"}]"));
    int otherId = idOf(stored(other, "models"), 0, "modelId");
    Integer studyId = create(prop("models", "[{\"name\": \"Gone\"}]"));
    int retired = idOf(stored(studyId, "models"), 0, "modelId");
    update(studyId, prop("models", "[]"));

    update(
        studyId,
        prop(
            "models",
            "[{\"modelId\": %d}, {\"modelId\": %d}, {\"modelId\": 1700000000000}, {\"modelId\": \"007\"}, {\"modelId\": 7.5}]"
                .formatted(otherId, retired)));

    List<Integer> ids = ids(stored(studyId, "models"), "modelId");
    assertEquals(5, assetDAO.findLiveModels(studyId).size());
    assertTrue(ids.stream().noneMatch(id -> id == otherId || id == retired));
    assertEquals("Other", assetDAO.findModelsByStudyId(other).getFirst().name());
  }

  @Test
  void testRepeatedIdsClaimTheRowOnce() throws Exception {
    Integer studyId = create(prop("workspaces", "[{\"workspaceId\": \"u-1\"}]"));
    int row = idOf(stored(studyId, "workspaces"), 0, "workspaceId");

    update(
        studyId,
        prop(
            "workspaces",
            "[{\"workspaceId\": %d}, {\"workspaceId\": %d}, {\"workspaceId\": \"u-1\"}, {\"workspaceId\": \"u-2\"}, {\"workspaceId\": \"u-2\"}]"
                .formatted(row, row)));

    JsonArray workspaces = stored(studyId, "workspaces");
    assertEquals(row, idOf(workspaces, 0, "workspaceId"));
    List<StudyWorkspaceRecord> rows = assetDAO.findWorkspacesByStudyId(studyId);
    assertEquals(5, rows.size());
    assertEquals("u-1", rows.getFirst().legacyKey());
    assertTrue(rows.stream().skip(1).allMatch(r -> r.legacyKey() == null));
  }

  @Test
  void testStaleFormMatchesByLegacyKey() throws Exception {
    Integer studyId = create(prop("workspaces", "[{\"workspaceId\": \"u-1\", \"name\": \"Old\"}]"));
    int row = idOf(stored(studyId, "workspaces"), 0, "workspaceId");

    update(studyId, prop("workspaces", "[{\"workspaceId\": \"u-1\", \"name\": \"New\"}]"));

    List<StudyWorkspaceRecord> rows = assetDAO.findWorkspacesByStudyId(studyId);
    assertEquals(1, rows.size());
    assertEquals("New", rows.getFirst().name());
    assertEquals(row, idOf(stored(studyId, "workspaces"), 0, "workspaceId"));
  }

  @Test
  void testAnIdInRowFormNeverMatchesALegacyKey() throws Exception {
    Integer studyId =
        create(prop("workspaces", "[{\"workspaceId\": \"2147483000.0\", \"name\": \"Old\"}]"));

    update(studyId, prop("workspaces", "[{\"workspaceId\": 2147483000.0, \"name\": \"New\"}]"));

    List<StudyWorkspaceRecord> rows = assetDAO.findWorkspacesByStudyId(studyId);
    assertEquals(2, rows.size());
    assertEquals("Old", rows.getFirst().name());
    assertNotNull(rows.getFirst().retiredDate());
  }

  @Test
  void testWriteBackChangesOnlyTheIdKey() throws Exception {
    String list =
        """
        [{"modelId": "", "extra": {"n": 12345678901234567890, "f": 1.50}, "age": "old"},
         "not an object", 42]""";
    Integer studyId = create(prop("models", list));

    JsonArray models = stored(studyId, "models");
    JsonObject model = models.get(0).getAsJsonObject();
    assertEquals("{\"n\":12345678901234567890,\"f\":1.50}", model.get("extra").toString());
    assertEquals("not an object", models.get(1).getAsString());
    assertEquals("42", models.get(2).toString());
    assertEquals(1, assetDAO.findModelsByStudyId(studyId).size());
  }

  @Test
  void testIdsThatAreNotTextOrNumbersGetNewRowsWithoutLegacyKeys() throws Exception {
    Integer studyId = create(prop("models", "[{\"modelId\": {\"a\": 1}}, {\"modelId\": true}]"));

    List<StudyModelRecord> rows = assetDAO.findModelsByStudyId(studyId);
    assertEquals(2, rows.size());
    assertTrue(rows.stream().allMatch(row -> row.legacyKey() == null));
    assertEquals(
        rows.stream().map(StudyModelRecord::modelId).toList(),
        ids(stored(studyId, "models"), "modelId"));
  }

  @Test
  void testAPropertyWithNoValueRetiresItsRows() throws Exception {
    Integer studyId = create(prop("models", "[{\"name\": \"A\"}]"));
    List<StudyProperty> props = List.of(new StudyProperty("models", null, PropertyType.Json));

    jdbi.useTransaction(
        handle -> new StudyAssetRecordSync().syncRegistration(handle, studyId, props));

    assertTrue(assetDAO.findLiveModels(studyId).isEmpty());
  }

  @Test
  void testAnUpdateThatMissesALiveRowFails() {
    int[] missed = {1, 0};

    StudyAssetRecordSync.requireAllLive(new int[] {1, 1}, 1, "models");
    assertThrows(
        IllegalStateException.class,
        () -> StudyAssetRecordSync.requireAllLive(missed, 1, "models"));
  }

  @Test
  void testFieldsOfTheWrongTypeAreStoredAsNull() throws Exception {
    Integer studyId =
        create(
            prop(
                "models",
                """
                [{"name": 5, "cloud": [1], "maintainer": "Lab", "description": {"a": 1},
                  "tags": "t", "trainedOnDatasets": [{}], "url": null}]"""));

    StudyModelRecord row = assetDAO.findModelsByStudyId(studyId).getFirst();
    assertNull(row.name());
    assertNull(row.cloud());
    assertNull(row.maintainer());
    assertNull(row.description());
    assertNull(row.tags());
    assertNull(row.trainedOnDatasets());
    assertNull(row.url());
    assertEquals(5, stored(studyId, "models").get(0).getAsJsonObject().get("name").getAsInt());
  }

  @Test
  void testNulCharactersAreStrippedFromColumnsOnly() throws Exception {
    Integer studyId =
        create(prop("models", "[{\"name\": \"a\\u0000b\", \"dataUse\": {\"x\": \"\\u0000c\"}}]"));

    StudyModelRecord row = assetDAO.findModelsByStudyId(studyId).getFirst();
    assertEquals("ab", row.name());
    assertEquals(JsonParser.parseString("{\"x\": \"c\"}"), JsonParser.parseString(row.dataUse()));
    assertEquals(
        "a\u0000b", stored(studyId, "models").get(0).getAsJsonObject().get("name").getAsString());

    Integer other = create(prop("workspaces", "[{\"workspaceId\": \"a\\u0000b\"}]"));
    assertNull(assetDAO.findWorkspacesByStudyId(other).getFirst().legacyKey());
  }

  @Test
  void testARetiredRowsLegacyKeyIsNotReused() throws Exception {
    Integer studyId = create(prop("workspaces", "[{\"workspaceId\": \"u-1\"}]"));
    update(studyId, prop("workspaces", "[]"));

    update(studyId, prop("workspaces", "[{\"workspaceId\": \"u-1\"}]"));

    List<StudyWorkspaceRecord> rows = assetDAO.findWorkspacesByStudyId(studyId);
    assertEquals(2, rows.size());
    assertNull(rows.get(1).legacyKey());
    assertNull(rows.get(1).retiredDate());
  }

  @Test
  void testAStoredValueThatIsNotAListLeavesTheRowsAlone() throws Exception {
    Integer studyId = create(prop("models", "[{\"name\": \"M\"}]"));

    update(studyId, prop("models", "{\"name\": \"M\"}"));

    assertEquals(1, assetDAO.findLiveModels(studyId).size());
  }

  @Test
  void testAFailedCreateRollsBackTheStudyItsRowsAndDatasets() {
    String name = UUID.randomUUID().toString();
    int modelRows = countModelRows();
    Integer dacId = dacDAO.createDac("dac", "dac", createUser().getUserId());
    DatasetInsert dataset =
        new DatasetInsert(
            "dataset",
            dacId,
            new DataUseBuilder().setGeneralUse(true).build(),
            createUser().getUserId(),
            List.of(),
            List.of());

    List<DatasetInsert> datasets = List.of(dataset, failingDataset());
    StudyProperty models = prop("models", "[{\"name\": \"M\"}]");

    assertThrows(NullPointerException.class, () -> create(name, datasets, models));

    assertNull(studyDAO.findStudyByName(name));
    assertEquals(modelRows, countModelRows());
    assertTrue(datasetDAO.findDatasetsAssociatedWithDac(dacId).isEmpty());
  }

  @Test
  void testAFailedUpdateRollsBackTheSync() throws Exception {
    Integer studyId = create(prop("models", "[{\"name\": \"M\"}]"));
    String before = stored(studyId, "models").toString();

    List<DatasetInsert> datasets = List.of(failingDataset());
    StudyProperty models = prop("models", "[]");

    assertThrows(NullPointerException.class, () -> update(studyId, datasets, models));

    assertEquals(1, assetDAO.findLiveModels(studyId).size());
    assertEquals(before, stored(studyId, "models").toString());
  }

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {
        "7|7",
        "true|",
        "7.0|7",
        "7e0|7",
        "7e-1|",
        "\"7\"|7",
        "\"2147483647\"|2147483647",
        "\"007\"|",
        "\"00000000001\"|",
        "7.5|",
        "2147483648|",
        "\"3000000000\"|",
        "\"5d2a1c34-8c1b-4a7e-9f0e-2b6c5d4e3f21\"|",
        "\"\"|",
        "null|",
        "0|",
        "-7|"
      })
  void testRowIdForm(String json, Integer expected) {
    assertEquals(expected, StudyAssetRecordSync.rowId(JsonParser.parseString(json)));
  }

  private Integer create(StudyProperty... props) throws Exception {
    return create(UUID.randomUUID().toString(), List.of(), props);
  }

  private Integer create(String name, List<DatasetInsert> datasets, StudyProperty... props)
      throws Exception {
    StudyInsert insert =
        new StudyInsert(
            name,
            "description",
            List.of("type"),
            "PI",
            "pi@example.org",
            null,
            true,
            createUser().getUserId(),
            List.of(props),
            List.of(),
            UUID.randomUUID());
    serviceDAO.insertDatasetRegistration(insert, datasets);
    return studyDAO.findStudyByName(name).getStudyId();
  }

  private void update(Integer studyId, StudyProperty... props) throws Exception {
    update(studyId, List.of(), props);
  }

  private void update(Integer studyId, List<DatasetInsert> datasets, StudyProperty... props)
      throws Exception {
    Study study = studyDAO.findStudyById(studyId);
    serviceDAO.updateStudy(
        new StudyUpdate(
            study.getName(),
            studyId,
            study.getDescription(),
            study.getDataTypes(),
            study.getPiName(),
            study.getPiEmail(),
            null,
            study.getPublicVisibility(),
            study.getCreateUserId(),
            List.of(props),
            List.of()),
        List.of(),
        datasets);
  }

  /** Fails in Java rather than SQL, since Postgres rolls back an aborted transaction anyway. */
  private DatasetInsert failingDataset() {
    return new DatasetInsert("dataset", null, null, createUser().getUserId(), List.of(), List.of());
  }

  /** A JSON study property, coerced as the registration mapper coerces it. */
  private static StudyProperty prop(String key, String json) {
    return new StudyProperty(key, PropertyType.Json.coerce(json), PropertyType.Json);
  }

  private JsonArray stored(Integer studyId, String key) {
    String value =
        jdbi.withHandle(
            handle ->
                handle
                    .createQuery(
                        "SELECT value FROM study_property WHERE study_id = :studyId AND key = :key")
                    .bind("studyId", studyId)
                    .bind("key", key)
                    .mapTo(String.class)
                    .one());
    return JsonParser.parseString(value).getAsJsonArray();
  }

  private int countModelRows() {
    return jdbi.withHandle(
        handle ->
            handle.createQuery("SELECT count(*) FROM study_model").mapTo(Integer.class).one());
  }

  private static int idOf(JsonArray entries, int index, String idKey) {
    return entries.get(index).getAsJsonObject().get(idKey).getAsInt();
  }

  private static List<Integer> ids(JsonArray entries, String idKey) {
    List<Integer> ids = new ArrayList<>();
    entries.forEach(e -> ids.add(e.getAsJsonObject().get(idKey).getAsInt()));
    return ids;
  }
}
