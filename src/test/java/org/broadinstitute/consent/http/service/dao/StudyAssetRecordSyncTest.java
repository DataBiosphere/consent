package org.broadinstitute.consent.http.service.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;
import org.broadinstitute.consent.http.db.DAOTestHelper;
import org.broadinstitute.consent.http.db.StudyAssetRecordDAO;
import org.broadinstitute.consent.http.enumeration.PropertyType;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.models.Study;
import org.broadinstitute.consent.http.models.StudyBiospecimenRecord;
import org.broadinstitute.consent.http.models.StudyModelRecord;
import org.broadinstitute.consent.http.models.StudyProperty;
import org.broadinstitute.consent.http.models.StudyWorkspaceRecord;
import org.broadinstitute.consent.http.models.dto.registration.RegistrationRequestMapper;
import org.broadinstitute.consent.http.models.dto.registration.StudyRegistrationRequest;
import org.broadinstitute.consent.http.service.dao.DatasetServiceDAO.DatasetInsert;
import org.broadinstitute.consent.http.service.dao.DatasetServiceDAO.StudyInsert;
import org.broadinstitute.consent.http.service.dao.DatasetServiceDAO.StudyUpdate;
import org.jdbi.v3.core.HandleConsumer;
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
  void testAnIdTooLongForTheIndexKeepsNoLegacyKey() throws Exception {
    // Two UTF-16 units each, so the cap is counted in code points as SQL counts them.
    String longest = "\uD83D\uDE00".repeat(255);
    String tooLong = "\uD83D\uDE00".repeat(256);

    Integer studyId =
        create(
            prop(
                "models",
                "[{\"modelId\": \"%s\"}, {\"modelId\": \"%s\"}]".formatted(longest, tooLong)));

    List<StudyModelRecord> rows = assetDAO.findModelsByStudyId(studyId);
    assertEquals(longest, rows.get(0).legacyKey());
    assertNull(rows.get(1).legacyKey());
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
  void testIdsWithUnpairedSurrogatesKeepNoLegacyKey() throws Exception {
    Integer studyId =
        create(prop("models", "[{\"modelId\": \"a\\uD800\"}, {\"modelId\": \"a\\uDC00\"}]"));

    List<StudyModelRecord> rows = assetDAO.findModelsByStudyId(studyId);
    assertEquals(2, rows.size());
    assertTrue(rows.stream().allMatch(row -> row.legacyKey() == null));
  }

  @Test
  void testWriteBackKeepsNumbersAsTheRequestMapperStoredThem() throws Exception {
    StudyRegistrationRequest request =
        new ObjectMapper()
            .readValue(
                """
                {"models": [{"modelId": "", "extra": {"n": 12345678901234567890, "f": 1.50}}]}""",
                StudyRegistrationRequest.class);
    StudyProperty models =
        new RegistrationRequestMapper()
            .toStudyProperties(request).stream()
                .filter(p -> p.getKey().equals("models"))
                .findFirst()
                .orElseThrow();

    Integer studyId = create(models);

    // Jackson reads 1.50 as a double before the sync sees it; the sync changes nothing further.
    JsonObject model = stored(studyId, "models").get(0).getAsJsonObject();
    assertEquals("{\"n\":12345678901234567890,\"f\":1.5}", model.get("extra").toString());
    assertEquals(
        assetDAO.findModelsByStudyId(studyId).getFirst().modelId(),
        model.get("modelId").getAsInt());
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

  @Test
  void testBiospecimenFieldsAndIdAreStored() throws Exception {
    Integer studyId =
        create(
            prop(
                "biospecimens",
                """
                [{"biospecimenId": "S-1", "age": 40.5, "extractedDiagnoses": ["x"],
                  "postMortemInterval": {"value": 6, "unit": "HOURS"}}]"""));

    StudyBiospecimenRecord row = assetDAO.findBiospecimensByStudyId(studyId).getFirst();
    assertEquals("S-1", row.legacyKey());
    assertEquals("S-1", row.sampleId());
    assertEquals(new BigDecimal("40.5"), row.age());
    assertEquals(List.of("x"), row.extractedDiagnoses());
    assertEquals(new BigDecimal("6"), row.postMortemIntervalValue());
    assertEquals("HOURS", row.postMortemIntervalUnit());
    JsonObject entry = stored(studyId, "biospecimens").get(0).getAsJsonObject();
    assertEquals(row.biospecimenId(), entry.get("biospecimenId").getAsInt());
    assertEquals("S-1", entry.get("sampleId").getAsString());
  }

  @Test
  void testBiospecimenFieldsOfTheWrongTypeAreStoredAsNull() throws Exception {
    Integer studyId =
        create(
            prop(
                "biospecimens",
                """
                [{"biospecimenId": "S-1", "age": "forty", "postMortemInterval": 6},
                 {"biospecimenId": "S-2", "postMortemInterval": {"value": "six", "unit": "HOURS"}}]"""));

    List<StudyBiospecimenRecord> rows = assetDAO.findBiospecimensByStudyId(studyId);
    assertNull(rows.get(0).age());
    assertNull(rows.get(0).postMortemIntervalValue());
    assertNull(rows.get(1).postMortemIntervalValue());
    assertEquals("HOURS", rows.get(1).postMortemIntervalUnit());
  }

  @Test
  void testIdsAndSampleIdsThatAreNotTextOrNumbersAreNotKept() throws Exception {
    Integer studyId =
        create(
            prop(
                "biospecimens",
                """
                [{"biospecimenId": "a", "sampleId": {"x": 1}},
                 {"biospecimenId": {"x": 1}},
                 {"biospecimenId": true},
                 {"age": {"years": 40}}]"""));

    assertEquals(Arrays.asList("a", null, null, null), sampleIds(stored(studyId, "biospecimens")));
    List<StudyBiospecimenRecord> rows = assetDAO.findBiospecimensByStudyId(studyId);
    assertEquals("a", rows.get(0).sampleId());
    assertNull(rows.get(3).age());
  }

  @Test
  void testSampleIdsAreKeptCopiedOrStringified() throws Exception {
    Integer studyId =
        create(
            prop(
                "biospecimens",
                """
                [{"biospecimenId": "a", "sampleId": "kept"},
                 {"biospecimenId": "b", "sampleId": 12345678901234567890},
                 {"biospecimenId": "c", "sampleId": " "},
                 {"biospecimenId": 1700000000000},
                 {"biospecimenId": 12345678901234567890},
                 {"biospecimenId": 12.5},
                 {"biospecimenId": "S-1"},
                 {"biospecimenId": 0},
                 {"biospecimenId": -5},
                 {"biospecimenId": 1E+10}]"""));

    JsonArray entries = stored(studyId, "biospecimens");
    List<String> expected =
        Arrays.asList(
            "kept",
            "12345678901234567890",
            "c",
            "1700000000000",
            "12345678901234567890",
            null,
            "S-1",
            "0",
            "-5",
            "10000000000");
    assertEquals(expected, sampleIds(entries));
    assertTrue(entries.get(1).getAsJsonObject().get("sampleId").getAsJsonPrimitive().isString());
    assertEquals(
        expected,
        assetDAO.findBiospecimensByStudyId(studyId).stream()
            .map(StudyBiospecimenRecord::sampleId)
            .toList());
  }

  @Test
  void testARowIdMatchKeepsOrClearsTheSampleId() throws Exception {
    Integer studyId =
        create(
            prop("biospecimens", "[{\"biospecimenId\": \"S-1\"}, {\"biospecimenId\": \"S-2\"}]"));
    JsonArray entries = stored(studyId, "biospecimens");
    int first = idOf(entries, 0, "biospecimenId");
    int second = idOf(entries, 1, "biospecimenId");

    update(
        studyId,
        prop(
            "biospecimens",
            "[{\"biospecimenId\": %d, \"sampleId\": null}, {\"biospecimenId\": %d, \"sampleId\": \"\"}]"
                .formatted(first, second)));

    assertEquals(Arrays.asList("S-1", ""), sampleIds(stored(studyId, "biospecimens")));
    List<StudyBiospecimenRecord> rows = assetDAO.findBiospecimensByStudyId(studyId);
    assertEquals("S-1", rows.get(0).sampleId());
    assertNull(rows.get(1).sampleId());
  }

  @Test
  void testAStaleFormCopiesItsIdOverTheStoredSampleId() throws Exception {
    Integer studyId =
        create(prop("biospecimens", "[{\"biospecimenId\": \"S-1\", \"sampleId\": \"old\"}]"));

    update(studyId, prop("biospecimens", "[{\"biospecimenId\": \"S-1\", \"donorId\": \"D\"}]"));

    List<StudyBiospecimenRecord> rows = assetDAO.findBiospecimensByStudyId(studyId);
    assertEquals(1, rows.size());
    assertEquals("S-1", rows.getFirst().sampleId());
    assertEquals("D", rows.getFirst().donorId());
    assertEquals(List.of("S-1"), sampleIds(stored(studyId, "biospecimens")));
  }

  @Test
  void testRowIdMatchesKeepOrStringifyTheSampleId() throws Exception {
    Integer studyId =
        create(
            prop(
                "biospecimens",
                """
                [{"biospecimenId": "S-1"}, {"biospecimenId": "S-2"}, {"biospecimenId": "S-3"}]"""));
    JsonArray entries = stored(studyId, "biospecimens");
    List<Integer> rows = ids(entries, "biospecimenId");

    update(
        studyId,
        prop(
            "biospecimens",
            """
            [{"biospecimenId": %d}, {"biospecimenId": %d, "sampleId": 42},
             {"biospecimenId": %d, "sampleId": true}]"""
                .formatted(rows.get(0), rows.get(1), rows.get(2))));

    assertEquals(List.of("S-1", "42", "S-3"), sampleIds(stored(studyId, "biospecimens")));
    assertEquals(
        List.of("S-1", "42", "S-3"),
        assetDAO.findBiospecimensByStudyId(studyId).stream()
            .map(StudyBiospecimenRecord::sampleId)
            .toList());
  }

  @Test
  void testALegacyKeyMatchKeepsTheSampleItSends() throws Exception {
    Integer studyId = create(prop("biospecimens", "[{\"biospecimenId\": \"S-1\"}]"));

    update(studyId, prop("biospecimens", "[{\"biospecimenId\": \"S-1\", \"sampleId\": 2.5}]"));

    StudyBiospecimenRecord row = assetDAO.findBiospecimensByStudyId(studyId).getFirst();
    assertEquals("2.5", row.sampleId());
    assertEquals(List.of("2.5"), sampleIds(stored(studyId, "biospecimens")));
  }

  @Test
  void testASecondWriteOfSettledEntriesChangesNothing() throws Exception {
    Integer studyId =
        create(
            prop(
                "biospecimens",
                "[{\"biospecimenId\": \"S-1\"}, {\"biospecimenId\": 7, \"sampleId\": \"x\"}]"));
    String settled = stored(studyId, "biospecimens").toString();

    update(studyId, prop("biospecimens", settled));

    assertEquals(settled, stored(studyId, "biospecimens").toString());
    assertEquals(2, assetDAO.findBiospecimensByStudyId(studyId).size());
  }

  @Test
  void testNumbersGsonCannotReadAreStoredAsNull() throws Exception {
    Integer studyId =
        create(
            prop(
                "biospecimens",
                "[{\"biospecimenId\": 1e+10001, \"sampleId\": \"S\", \"age\": 1e-10001}]"));

    StudyBiospecimenRecord row = assetDAO.findBiospecimensByStudyId(studyId).getFirst();
    assertNull(row.age());
    assertEquals("S", row.sampleId());
  }

  @Test
  void testRequestNumbersReachTheSyncWithEveryDigit() throws Exception {
    StudyRegistrationRequest request =
        RegistrationRequestMapper.requestObjectMapper()
            .readValue(
                """
                {"biospecimens": [{"biospecimenId": 12345678901234567890.0},
                                  {"biospecimenId": "b", "sampleId": 1E+10}]}""",
                StudyRegistrationRequest.class);
    StudyProperty biospecimens =
        new RegistrationRequestMapper()
            .toStudyProperties(request).stream()
                .filter(p -> p.getKey().equals("biospecimens"))
                .findFirst()
                .orElseThrow();

    Integer studyId = create(biospecimens);

    assertEquals(
        List.of("12345678901234567890", "1E+10"), sampleIds(stored(studyId, "biospecimens")));
  }

  @Test
  void testARowFormBiospecimenIdThatClaimsNoRowIsRejected() throws Exception {
    String name = UUID.randomUUID().toString();

    List<DatasetInsert> none = List.of();
    StudyProperty rowForm = prop("biospecimens", "[{\"biospecimenId\": \"12345\"}]");
    StudyProperty oddSample =
        prop("biospecimens", "[{\"biospecimenId\": 12345, \"sampleId\": {}}]");
    assertThrows(BadRequestException.class, () -> create(name, none, rowForm));
    assertThrows(BadRequestException.class, () -> create(name, none, oddSample));
    assertNull(studyDAO.findStudyByName(name));

    Integer studyId = create(prop("biospecimens", "[{\"biospecimenId\": \"S-1\"}]"));
    int row = idOf(stored(studyId, "biospecimens"), 0, "biospecimenId");
    StudyProperty repeated =
        prop(
            "biospecimens",
            "[{\"biospecimenId\": %d}, {\"biospecimenId\": %d}]".formatted(row, row));
    assertThrows(BadRequestException.class, () -> update(studyId, repeated));
    assertEquals(List.of("S-1"), sampleIds(stored(studyId, "biospecimens")));
  }

  @Test
  void testRowFormBiospecimenIdsThatClaimNoRowNeedASampleId() throws Exception {
    Integer other = create(prop("biospecimens", "[{\"biospecimenId\": \"O-1\"}]"));
    int otherId = idOf(stored(other, "biospecimens"), 0, "biospecimenId");
    Integer studyId = create(prop("biospecimens", "[{\"biospecimenId\": \"S-1\"}]"));
    int retired = idOf(stored(studyId, "biospecimens"), 0, "biospecimenId");
    update(studyId, prop("biospecimens", "[]"));

    for (String id : List.of(String.valueOf(otherId), String.valueOf(retired), "12345678")) {
      StudyProperty unmatched = prop("biospecimens", "[{\"biospecimenId\": %s}]".formatted(id));
      assertThrows(BadRequestException.class, () -> update(studyId, unmatched));
    }
    update(
        studyId,
        prop(
            "biospecimens", "[{\"biospecimenId\": %d, \"sampleId\": \"S-9\"}]".formatted(retired)));

    List<StudyBiospecimenRecord> live =
        assetDAO.findBiospecimensByStudyId(studyId).stream()
            .filter(row -> row.retiredDate() == null)
            .toList();
    assertEquals(1, live.size());
    assertNotEquals(retired, live.getFirst().biospecimenId());
    assertEquals("S-9", live.getFirst().sampleId());
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

  @Test
  void testSyncStudyAssetsGivesAnUnsyncedListRows() throws Exception {
    Integer studyId = create();
    // Stored directly, as before the registration sync existed
    studyDAO.insertStudyProperty(
        studyId,
        "models",
        PropertyType.Json.toString(),
        "[{\"modelId\": \"m-1\", \"name\": \"A\"}]");

    serviceDAO.syncStudyAssets(studyId);

    StudyModelRecord row = assetDAO.findModelsByStudyId(studyId).getFirst();
    assertEquals("m-1", row.legacyKey());
    assertEquals(row.modelId(), idOf(stored(studyId, "models"), 0, "modelId"));
  }

  /** Task 6: a study stored before the registration sync, converted by the bulk resync. */
  @Test
  void testSyncStudyAssetsConvertsAStudyStoredBeforeTheSync() throws Exception {
    Integer studyId = create();
    String json = PropertyType.Json.toString();
    studyDAO.insertStudyProperty(
        studyId,
        "models",
        json,
        """
        [{"modelId": "uuid-a", "extra": {"k": 1}}, {"modelId": ""}, {"modelId": "dup"},
         {"modelId": "dup"}, {"modelId": 1700000000000}, 5]""");
    studyDAO.insertStudyProperty(
        studyId, "assets", json, "{\"Workspaces\": [{\"workspaceId\": \"w-1\"}]}");
    studyDAO.insertStudyProperty(
        studyId,
        "Biospecimens",
        json,
        "[{\"biospecimenId\": \"S-1\"}, {\"biospecimenId\": 1700000000000}]");

    serviceDAO.syncStudyAssets(studyId);

    JsonArray models = stored(studyId, "models");
    assertEquals(6, models.size());
    assertEquals(
        assetDAO.findModelsByStudyId(studyId).stream().map(StudyModelRecord::modelId).toList(),
        IntStream.range(0, 5).mapToObj(i -> idOf(models, i, "modelId")).toList());
    assertEquals(
        Arrays.asList("uuid-a", null, null, null, "1700000000000"),
        assetDAO.findModelsByStudyId(studyId).stream().map(StudyModelRecord::legacyKey).toList());
    assertEquals("{\"k\":1}", models.get(0).getAsJsonObject().get("extra").toString());
    assertEquals("5", models.get(5).toString());

    JsonArray workspaces = stored(studyId, "workspaces");
    StudyWorkspaceRecord workspace = assetDAO.findWorkspacesByStudyId(studyId).getFirst();
    assertEquals(workspace.workspaceId(), idOf(workspaces, 0, "workspaceId"));
    assertEquals("w-1", workspace.legacyKey());
    assertEquals(0, countProperties(studyId, "assets"));

    JsonArray biospecimens = stored(studyId, "Biospecimens");
    assertEquals(List.of("S-1", "1700000000000"), sampleIds(biospecimens));
    List<StudyBiospecimenRecord> specimens = assetDAO.findBiospecimensByStudyId(studyId);
    assertEquals(
        specimens.stream().map(StudyBiospecimenRecord::biospecimenId).toList(),
        IntStream.range(0, 2).mapToObj(i -> idOf(biospecimens, i, "biospecimenId")).toList());
    assertEquals(
        List.of("S-1", "1700000000000"),
        specimens.stream().map(StudyBiospecimenRecord::legacyKey).toList());

    List<String> convertedModels = storedValues(studyId, "models");
    List<String> convertedWorkspaces = storedValues(studyId, "workspaces");
    List<String> convertedBiospecimens = storedValues(studyId, "Biospecimens");
    List<List<Object>> biospecimenRows = biospecimenIdentities(studyId);
    serviceDAO.syncStudyAssets(studyId);
    assertEquals(convertedModels, storedValues(studyId, "models"));
    assertEquals(convertedWorkspaces, storedValues(studyId, "workspaces"));
    assertEquals(convertedBiospecimens, storedValues(studyId, "Biospecimens"));
    assertEquals(5, assetDAO.findModelsByStudyId(studyId).size());
    StudyWorkspaceRecord resynced = assetDAO.findWorkspacesByStudyId(studyId).getFirst();
    assertEquals(
        Arrays.asList(workspace.workspaceId(), workspace.legacyKey(), null),
        Arrays.asList(resynced.workspaceId(), resynced.legacyKey(), resynced.retiredDate()));
    assertEquals(1, assetDAO.findWorkspacesByStudyId(studyId).size());
    assertEquals(biospecimenRows, biospecimenIdentities(studyId));
  }

  @Test
  void testSyncStudyAssetsLeavesAStudyWithNoAssets() throws Exception {
    Integer studyId = create();

    serviceDAO.syncStudyAssets(studyId);

    assertTrue(assetDAO.findModelsByStudyId(studyId).isEmpty());
    assertTrue(assetDAO.findWorkspacesByStudyId(studyId).isEmpty());
    assertTrue(assetDAO.findBiospecimensByStudyId(studyId).isEmpty());
  }

  @Test
  void testSyncStudyAssetsRollsBackWhenALaterTypeFails() throws Exception {
    Integer studyId = create();
    String models = "[{\"modelId\": \"m-1\"}]";
    studyDAO.insertStudyProperty(studyId, "models", PropertyType.Json.toString(), models);
    // A row-id-form biospecimen id with no sampleId and no matching row is a 400
    studyDAO.insertStudyProperty(
        studyId, "biospecimens", PropertyType.Json.toString(), "[{\"biospecimenId\": 12345}]");

    assertThrows(BadRequestException.class, () -> serviceDAO.syncStudyAssets(studyId));

    assertTrue(assetDAO.findModelsByStudyId(studyId).isEmpty());
    assertEquals(models, storedValue(studyId, "models"));
  }

  @Test
  void testSyncStudyMovesALegacyOnlyListOnce() throws Exception {
    Integer studyId =
        create(
            prop(
                "assets",
                """
                {"Biospecimens": [{"biospecimenId": "S-1", "n": 12345678901234567890}],
                 "other": 1}"""));

    syncStudy(studyId);

    JsonObject entry = stored(studyId, "biospecimens").get(0).getAsJsonObject();
    StudyBiospecimenRecord row = assetDAO.findBiospecimensByStudyId(studyId).getFirst();
    assertEquals(row.biospecimenId(), entry.get("biospecimenId").getAsInt());
    assertEquals("S-1", entry.get("sampleId").getAsString());
    assertEquals("12345678901234567890", entry.get("n").toString());
    assertEquals("{\"other\":1}", storedValue(studyId, "assets"));

    String list = storedValue(studyId, "biospecimens");
    syncStudy(studyId);

    assertEquals(list, storedValue(studyId, "biospecimens"));
    assertEquals(1, assetDAO.findBiospecimensByStudyId(studyId).size());
  }

  @Test
  void testSyncStudyLeavesANonListPropertyAndItsLegacyCopy() throws Exception {
    String legacy = "{\"biospecimens\":[{\"biospecimenId\":\"S-1\"}]}";
    Integer studyId =
        create(prop("biospecimens", "{\"biospecimenId\": \"S-1\"}"), prop("assets", legacy));

    syncStudy(studyId);

    assertEquals("{\"biospecimenId\":\"S-1\"}", storedValue(studyId, "biospecimens"));
    assertEquals(legacy, storedValue(studyId, "assets"));
    assertTrue(assetDAO.findBiospecimensByStudyId(studyId).isEmpty());
  }

  @Test
  void testSyncStudyUsesTheListReadsUseAndWritesOnlyItsRow() throws Exception {
    Integer studyId = create();
    String json = PropertyType.Json.toString();
    studyDAO.insertStudyProperty(studyId, "Models", json, "\"not a list\"");
    studyDAO.insertStudyProperty(studyId, "models", json, "[{\"name\": \"A\"}]");
    studyDAO.insertStudyProperty(studyId, "models", json, "[{\"name\": \"B\"}]");

    syncStudy(studyId);

    List<StudyModelRecord> rows = assetDAO.findModelsByStudyId(studyId);
    assertEquals(List.of("A"), rows.stream().map(StudyModelRecord::name).toList());
    List<String> values = storedValues(studyId, "models");
    assertEquals("\"not a list\"", values.get(0));
    assertEquals(
        rows.getFirst().modelId(),
        idOf(JsonParser.parseString(values.get(1)).getAsJsonArray(), 0, "modelId"));
    assertEquals("[{\"name\": \"B\"}]", values.get(2));
  }

  @Test
  void testSyncStudyChangesOnlyTheAssetsRowItMovedFrom() throws Exception {
    Integer studyId = create();
    String json = PropertyType.Json.toString();
    studyDAO.insertStudyProperty(studyId, "assets", json, "{\"models\": [{\"name\": \"A\"}]}");
    studyDAO.insertStudyProperty(studyId, "assets", json, "{\"models\": [{\"name\": \"B\"}]}");

    syncStudy(studyId);

    assertEquals("A", assetDAO.findModelsByStudyId(studyId).getFirst().name());
    assertEquals(List.of("{\"models\": [{\"name\": \"B\"}]}"), storedValues(studyId, "assets"));
  }

  @Test
  void testSyncStudyNeedsATransaction() throws Exception {
    Integer studyId = create();
    StudyAssetRecordSync sync = new StudyAssetRecordSync();

    HandleConsumer<RuntimeException> outsideATransaction =
        handle -> sync.syncStudy(handle, studyId);

    assertThrows(IllegalStateException.class, () -> jdbi.useHandle(outsideATransaction));
  }

  @Test
  void testSyncStudyReadsTheFirstNonEmptyAssetsObject() throws Exception {
    Integer studyId = create();
    String json = PropertyType.Json.toString();
    studyDAO.insertStudyProperty(studyId, "assets", json, "{}");
    studyDAO.insertStudyProperty(studyId, "assets", json, "[1]");
    studyDAO.insertStudyProperty(studyId, "assets", json, "{\"models\": [{\"name\": \"A\"}]}");

    syncStudy(studyId);

    assertEquals("A", assetDAO.findModelsByStudyId(studyId).getFirst().name());
    assertEquals(List.of("{}", "[1]"), storedValues(studyId, "assets"));
  }

  @Test
  void testSyncStudyLeavesTheRowsOfAMalformedList() throws Exception {
    Integer studyId = create(prop("models", "[{\"name\": \"A\"}]"));
    studyDAO.updateStudyProperty(studyId, "models", PropertyType.Json.toString(), "[{");

    syncStudy(studyId);

    assertEquals(1, assetDAO.findLiveModels(studyId).size());
    assertEquals("[{", storedValue(studyId, "models"));
  }

  @Test
  void testSyncStudyCatchesUpWithEditsMadeOutsideTheSync() throws Exception {
    Integer studyId = create(prop("models", "[{\"name\": \"A\"}, {\"name\": \"B\"}]"));
    int a = idOf(stored(studyId, "models"), 0, "modelId");
    studyDAO.updateStudyProperty(
        studyId,
        "models",
        PropertyType.Json.toString(),
        "[{\"modelId\": %d, \"name\": \"A2\"}, {\"name\": \"C\"}]".formatted(a));

    syncStudy(studyId);

    List<StudyModelRecord> rows = assetDAO.findModelsByStudyId(studyId);
    assertEquals(List.of("A2", "B", "C"), rows.stream().map(StudyModelRecord::name).toList());
    assertNull(rows.get(0).retiredDate());
    assertNotNull(rows.get(1).retiredDate());
    assertEquals(List.of(a, rows.get(2).modelId()), ids(stored(studyId, "models"), "modelId"));
  }

  @Test
  void testConcurrentUpdatesLeaveOnlyTheLastWritesRows() throws Exception {
    Integer studyId = create(prop("models", "[{\"name\": \"A\"}]"));
    int a = idOf(stored(studyId, "models"), 0, "modelId");
    AtomicReference<Future<?>> edit = new AtomicReference<>();

    // Another edit holds the study row with an uncommitted row insert. The second edit only
    // updates, which takes no foreign key lock on the study, so only the study write orders it.
    try (ExecutorService pool = Executors.newSingleThreadExecutor()) {
      jdbi.useTransaction(
          handle -> {
            StudyAssetRecordDAO dao = handle.attach(StudyAssetRecordDAO.class);
            dao.lockStudy(studyId);
            dao.insertModels(List.of(model(studyId, "other edit")));
            edit.set(
                pool.submit(
                    () -> {
                      update(
                          studyId,
                          prop("models", "[{\"modelId\": %d, \"name\": \"A2\"}]".formatted(a)));
                      return null;
                    }));
            awaitABlockedLock();
          });
      edit.get().get(30, TimeUnit.SECONDS);
    }

    List<StudyModelRecord> live =
        assetDAO.findModelsByStudyId(studyId).stream()
            .filter(row -> row.retiredDate() == null)
            .toList();
    assertEquals(List.of("A2"), live.stream().map(StudyModelRecord::name).toList());
  }

  @Test
  void testSyncStudyWaitsForAnUpdateInProgress() throws Exception {
    Integer studyId = create(prop("models", "[{\"name\": \"A\"}]"));
    AtomicReference<Future<?>> recovery = new AtomicReference<>();

    try (ExecutorService pool = Executors.newSingleThreadExecutor()) {
      // The update joins this transaction, so it stays uncommitted until the recovery is queued.
      jdbi.useTransaction(
          handle -> {
            update(studyId, prop("models", "[{\"name\": \"B\"}]"));
            recovery.set(pool.submit(() -> syncStudy(studyId)));
            awaitABlockedLock();
          });
      recovery.get().get(30, TimeUnit.SECONDS);
    }

    assertEquals("B", stored(studyId, "models").get(0).getAsJsonObject().get("name").getAsString());
    List<StudyModelRecord> live =
        assetDAO.findModelsByStudyId(studyId).stream()
            .filter(row -> row.retiredDate() == null)
            .toList();
    assertEquals(List.of("B"), live.stream().map(StudyModelRecord::name).toList());
  }

  @Test
  void testSyncStudyKeepsAStoredKeyAndDropsAnEmptiedAssetsObject() throws Exception {
    Integer studyId =
        create(
            prop("Models", "[{\"name\": \"M\"}]"),
            prop("assets", "{\"workspaces\": [{\"name\": \"W\"}]}"));

    syncStudy(studyId);

    int model = assetDAO.findModelsByStudyId(studyId).getFirst().modelId();
    assertEquals(model, idOf(stored(studyId, "Models"), 0, "modelId"));
    assertEquals(1, assetDAO.findLiveWorkspaces(studyId).size());
    assertEquals(0, countProperties(studyId, "assets"));
    assertThrows(NotFoundException.class, () -> syncStudy(-1));
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

  private void update(Integer studyId, StudyProperty... props) {
    update(studyId, List.of(), props);
  }

  private void update(Integer studyId, List<DatasetInsert> datasets, StudyProperty... props) {
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

  /** A JSON study property, coerced as the registration mapper coerces its own output. */
  private static StudyProperty prop(String key, String json) {
    return new StudyProperty(key, PropertyType.Json.coerce(json), PropertyType.Json);
  }

  private void syncStudy(Integer studyId) {
    jdbi.useTransaction(handle -> new StudyAssetRecordSync().syncStudy(handle, studyId));
  }

  /** Waits until another connection is queued on a row lock. */
  private void awaitABlockedLock() throws InterruptedException {
    for (int i = 0; i < 500; i++) {
      int waiting =
          jdbi.withHandle(
              handle ->
                  handle
                      .createQuery("SELECT count(*) FROM pg_locks WHERE NOT granted")
                      .mapTo(Integer.class)
                      .one());
      if (waiting > 0) {
        return;
      }
      Thread.sleep(20);
    }
    throw new IllegalStateException("No connection blocked on a lock");
  }

  private static StudyModelRecord model(Integer studyId, String name) {
    return new StudyModelRecord(
        null, studyId, null, name, null, null, null, null, null, null, null, null, null, null, null,
        null, null);
  }

  private int countProperties(Integer studyId, String key) {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery(
                    "SELECT count(*) FROM study_property WHERE study_id = :studyId AND key = :key")
                .bind("studyId", studyId)
                .bind("key", key)
                .mapTo(Integer.class)
                .one());
  }

  /** What a resync must keep for each biospecimen row; a sync always stamps update_date. */
  private List<List<Object>> biospecimenIdentities(Integer studyId) {
    return assetDAO.findBiospecimensByStudyId(studyId).stream()
        .map(
            row ->
                Arrays.<Object>asList(
                    row.biospecimenId(), row.legacyKey(), row.sampleId(), row.retiredDate()))
        .toList();
  }

  /** Every row of a key in any case, in insertion order. */
  private List<String> storedValues(Integer studyId, String key) {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery(
                    """
                    SELECT value FROM study_property
                    WHERE study_id = :studyId AND lower(key) = lower(:key)
                    ORDER BY study_property_id
                    """)
                .bind("studyId", studyId)
                .bind("key", key)
                .mapTo(String.class)
                .list());
  }

  private String storedValue(Integer studyId, String key) {
    return jdbi.withHandle(
        handle ->
            handle
                .createQuery(
                    "SELECT value FROM study_property WHERE study_id = :studyId AND key = :key")
                .bind("studyId", studyId)
                .bind("key", key)
                .mapTo(String.class)
                .one());
  }

  private JsonArray stored(Integer studyId, String key) {
    return JsonParser.parseString(storedValue(studyId, key)).getAsJsonArray();
  }

  private int countModelRows() {
    return jdbi.withHandle(
        handle ->
            handle.createQuery("SELECT count(*) FROM study_model").mapTo(Integer.class).one());
  }

  private static int idOf(JsonArray entries, int index, String idKey) {
    return entries.get(index).getAsJsonObject().get(idKey).getAsInt();
  }

  private static List<String> sampleIds(JsonArray entries) {
    List<String> sampleIds = new ArrayList<>();
    entries.forEach(
        e -> {
          JsonElement sampleId = e.getAsJsonObject().get("sampleId");
          sampleIds.add(sampleId == null ? null : sampleId.getAsString());
        });
    return sampleIds;
  }

  private static List<Integer> ids(JsonArray entries, String idKey) {
    List<Integer> ids = new ArrayList<>();
    entries.forEach(e -> ids.add(e.getAsJsonObject().get(idKey).getAsInt()));
    return ids;
  }
}
