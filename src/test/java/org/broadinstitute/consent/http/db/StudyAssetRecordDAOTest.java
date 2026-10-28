package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.broadinstitute.consent.http.models.StudyAssetKey;
import org.broadinstitute.consent.http.models.StudyBiospecimenRecord;
import org.broadinstitute.consent.http.models.StudyModelRecord;
import org.broadinstitute.consent.http.models.StudyWorkspaceRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StudyAssetRecordDAOTest extends DAOTestHelper {

  private static final String MAINTAINER = "{\"name\":\"Lab\",\"email\":\"lab@example.org\"}";
  private static final String DATA_USE = "{\"generalUse\":true}";

  private StudyAssetRecordDAO assetDAO;

  @BeforeEach
  void setUpDao() {
    assetDAO = jdbi.onDemand(StudyAssetRecordDAO.class);
  }

  @Test
  void testModelsRoundTrip() {
    Integer studyId = insertStudy();

    List<Integer> ids =
        assetDAO.insertModels(
            List.of(model(null, studyId, "a", "First"), model(null, studyId, null, "Second")));

    assertEquals(2, ids.size());
    List<StudyModelRecord> rows = assetDAO.findModelsByStudyId(studyId);
    assertEquals(ids, rows.stream().map(StudyModelRecord::modelId).toList());
    StudyModelRecord first = rows.getFirst();
    assertEquals(studyId, first.studyId());
    assertEquals("a", first.legacyKey());
    assertEquals("First", first.name());
    assertEquals(List.of("GCP", "AWS"), first.cloud());
    assertEquals(List.of("DUOS-000001"), first.trainedOnDatasets());
    assertEquals(List.of("tag"), first.tags());
    assertEquals(JsonParser.parseString(MAINTAINER), JsonParser.parseString(first.maintainer()));
    assertEquals("open", first.accessManagement());
    assertEquals(JsonParser.parseString(DATA_USE), JsonParser.parseString(first.dataUse()));
    assertNotNull(first.createDate());
    assertNotNull(first.updateDate());
    assertNull(first.retiredDate());
    assertNull(rows.get(1).legacyKey());
  }

  @Test
  void testWorkspacesRoundTrip() {
    Integer studyId = insertStudy();

    List<Integer> ids = assetDAO.insertWorkspaces(List.of(workspace(null, studyId, "w1", "Space")));

    StudyWorkspaceRecord row = assetDAO.findWorkspacesByStudyId(studyId).getFirst();
    assertEquals(ids.getFirst(), row.workspaceId());
    assertEquals("w1", row.legacyKey());
    assertEquals("Space", row.name());
    assertEquals(List.of("GCP"), row.cloud());
    assertEquals(List.of("Jupyter", "RStudio"), row.tools());
    assertEquals("controlled", row.access());
    assertEquals(JsonParser.parseString(DATA_USE), JsonParser.parseString(row.dataUse()));
    assertNull(row.retiredDate());
  }

  @Test
  void testBiospecimensRoundTrip() {
    Integer studyId = insertStudy();

    List<Integer> ids =
        assetDAO.insertBiospecimens(List.of(biospecimen(null, studyId, "b1", "SAMPLE-1")));

    StudyBiospecimenRecord row = assetDAO.findBiospecimensByStudyId(studyId).getFirst();
    assertEquals(ids.getFirst(), row.biospecimenId());
    assertEquals("b1", row.legacyKey());
    assertEquals("SAMPLE-1", row.sampleId());
    assertEquals(0, new BigDecimal("12.5").compareTo(row.postMortemIntervalValue()));
    assertEquals("hours", row.postMortemIntervalUnit());
    assertEquals(0, new BigDecimal("64").compareTo(row.age()));
    assertEquals(List.of("Alzheimer's disease", "Hypertension"), row.extractedDiagnoses());
    assertNull(row.optionalDataUse());
  }

  @Test
  void testNullArraysAndJsonRoundTrip() {
    Integer studyId = insertStudy();
    StudyModelRecord empty =
        new StudyModelRecord(
            null, studyId, null, null, null, null, null, null, null, null, null, null, null, null,
            null, null, null);

    assetDAO.insertModels(List.of(empty));

    StudyModelRecord row = assetDAO.findModelsByStudyId(studyId).getFirst();
    assertNull(row.cloud());
    assertNull(row.maintainer());
    assertNull(row.dataUse());
  }

  @Test
  void testUpdateChangesLiveRowOfTheStudyOnly() {
    Integer studyId = insertStudy();
    Integer otherStudyId = insertStudy();
    Integer modelId =
        assetDAO.insertModels(List.of(model(null, studyId, "a", "Before"))).getFirst();
    Integer workspaceId =
        assetDAO.insertWorkspaces(List.of(workspace(null, studyId, "w", "Before"))).getFirst();
    Integer biospecimenId =
        assetDAO.insertBiospecimens(List.of(biospecimen(null, studyId, "b", "Before"))).getFirst();

    assertArrayEquals(
        new int[] {1, 0},
        assetDAO.updateModels(
            List.of(
                model(modelId, studyId, null, "After"), model(modelId, otherStudyId, null, "X"))));
    assertArrayEquals(
        new int[] {1, 0},
        assetDAO.updateWorkspaces(
            List.of(
                workspace(workspaceId, studyId, null, "After"),
                workspace(workspaceId, otherStudyId, null, "X"))));
    assertArrayEquals(
        new int[] {1, 0},
        assetDAO.updateBiospecimens(
            List.of(
                biospecimen(biospecimenId, studyId, null, "After"),
                biospecimen(biospecimenId, otherStudyId, null, "X"))));

    StudyModelRecord model = assetDAO.findModelsByStudyId(studyId).getFirst();
    assertEquals("After", model.name());
    assertEquals("a", model.legacyKey());
    assertEquals("After", assetDAO.findWorkspacesByStudyId(studyId).getFirst().name());
    StudyBiospecimenRecord biospecimen = assetDAO.findBiospecimensByStudyId(studyId).getFirst();
    assertEquals("After", biospecimen.sampleId());
    assertEquals("b", biospecimen.legacyKey());
  }

  @Test
  void testUpdateOfRetiredRowChangesNothing() {
    Integer studyId = insertStudy();
    Integer modelId = assetDAO.insertModels(List.of(model(null, studyId, null, "Kept"))).getFirst();
    Integer workspaceId =
        assetDAO.insertWorkspaces(List.of(workspace(null, studyId, null, "Kept"))).getFirst();
    Integer biospecimenId =
        assetDAO.insertBiospecimens(List.of(biospecimen(null, studyId, null, "Kept"))).getFirst();
    assetDAO.retireModelsExcept(studyId, List.of());
    assetDAO.retireWorkspacesExcept(studyId, List.of());
    assetDAO.retireBiospecimensExcept(studyId, List.of());

    assertArrayEquals(
        new int[] {0}, assetDAO.updateModels(List.of(model(modelId, studyId, null, "Changed"))));
    assertArrayEquals(
        new int[] {0},
        assetDAO.updateWorkspaces(List.of(workspace(workspaceId, studyId, null, "Changed"))));
    assertArrayEquals(
        new int[] {0},
        assetDAO.updateBiospecimens(List.of(biospecimen(biospecimenId, studyId, null, "Changed"))));
    assertEquals("Kept", assetDAO.findModelsByStudyId(studyId).getFirst().name());
    assertEquals("Kept", assetDAO.findWorkspacesByStudyId(studyId).getFirst().name());
    assertEquals("Kept", assetDAO.findBiospecimensByStudyId(studyId).getFirst().sampleId());
  }

  @Test
  void testRetireExceptKeepsListedRowsAndLeavesOtherStudies() {
    Integer studyId = insertStudy();
    Integer otherStudyId = insertStudy();
    List<Integer> modelIds =
        assetDAO.insertModels(
            List.of(model(null, studyId, "a", "A"), model(null, studyId, "b", "B")));
    List<Integer> workspaceIds =
        assetDAO.insertWorkspaces(
            List.of(workspace(null, studyId, null, "A"), workspace(null, studyId, null, "B")));
    List<Integer> biospecimenIds =
        assetDAO.insertBiospecimens(
            List.of(biospecimen(null, studyId, null, "A"), biospecimen(null, studyId, null, "B")));
    assetDAO.insertModels(List.of(model(null, otherStudyId, null, "Other")));

    assertEquals(1, assetDAO.retireModelsExcept(studyId, List.of(modelIds.getFirst())));
    assertEquals(1, assetDAO.retireWorkspacesExcept(studyId, List.of(workspaceIds.get(1))));
    assertEquals(2, assetDAO.retireBiospecimensExcept(studyId, List.of()));

    assertEquals(
        List.of(new StudyAssetKey(modelIds.getFirst(), "a")), assetDAO.findLiveModels(studyId));
    assertEquals(
        List.of(new StudyAssetKey(workspaceIds.get(1), null)),
        assetDAO.findLiveWorkspaces(studyId));
    assertTrue(assetDAO.findLiveBiospecimens(studyId).isEmpty());
    assertEquals(1, assetDAO.findLiveModels(otherStudyId).size());
    StudyBiospecimenRecord retired = assetDAO.findBiospecimensByStudyId(studyId).getFirst();
    assertNotNull(retired.retiredDate());
    assertEquals(retired.retiredDate(), retired.updateDate());
    assertEquals(
        biospecimenIds,
        assetDAO.findBiospecimensByStudyId(studyId).stream()
            .map(StudyBiospecimenRecord::biospecimenId)
            .toList());
  }

  @Test
  void testRetireSkipsRowsAlreadyRetired() {
    Integer studyId = insertStudy();
    assetDAO.insertModels(List.of(model(null, studyId, null, "A")));
    assetDAO.retireModelsExcept(studyId, List.of());

    assertEquals(0, assetDAO.retireModelsExcept(studyId, List.of()));
  }

  @Test
  void testLegacyKeyIsUniquePerStudyIncludingRetiredRows() {
    Integer studyId = insertStudy();
    Integer otherStudyId = insertStudy();
    assetDAO.insertModels(List.of(model(null, studyId, "dup", "A")));
    assetDAO.insertWorkspaces(List.of(workspace(null, studyId, "dup", "A")));
    assetDAO.insertBiospecimens(List.of(biospecimen(null, studyId, "dup", "A")));
    assetDAO.retireModelsExcept(studyId, List.of());
    assetDAO.retireWorkspacesExcept(studyId, List.of());
    assetDAO.retireBiospecimensExcept(studyId, List.of());

    Exception model =
        assertThrows(
            Exception.class,
            () -> assetDAO.insertModels(List.of(model(null, studyId, "dup", "B"))));
    assertTrue(model.getMessage().contains("uq_study_model_legacy_key"));
    Exception workspace =
        assertThrows(
            Exception.class,
            () -> assetDAO.insertWorkspaces(List.of(workspace(null, studyId, "dup", "B"))));
    assertTrue(workspace.getMessage().contains("uq_study_workspace_legacy_key"));
    Exception biospecimen =
        assertThrows(
            Exception.class,
            () -> assetDAO.insertBiospecimens(List.of(biospecimen(null, studyId, "dup", "B"))));
    assertTrue(biospecimen.getMessage().contains("uq_study_biospecimen_legacy_key"));

    assetDAO.insertModels(List.of(model(null, otherStudyId, "dup", "C")));
    assetDAO.insertWorkspaces(List.of(workspace(null, otherStudyId, "dup", "C")));
    assetDAO.insertBiospecimens(List.of(biospecimen(null, otherStudyId, "dup", "C")));
    assertEquals(List.of("dup"), assetDAO.findUsedModelLegacyKeys(studyId));
    assertEquals(List.of("dup"), assetDAO.findUsedWorkspaceLegacyKeys(studyId));
    assertEquals(List.of("dup"), assetDAO.findUsedBiospecimenLegacyKeys(studyId));
  }

  @Test
  void testRowsWithoutLegacyKeyAreNotUniqueAndNotListed() {
    Integer studyId = insertStudy();

    assetDAO.insertModels(
        List.of(model(null, studyId, null, "A"), model(null, studyId, null, "B")));

    assertEquals(2, assetDAO.findLiveModels(studyId).size());
    assertTrue(assetDAO.findUsedModelLegacyKeys(studyId).isEmpty());
  }

  @Test
  void testDeletingTheStudyDeletesItsRows() {
    Integer studyId = insertStudy();
    assetDAO.insertModels(List.of(model(null, studyId, null, "A")));
    assetDAO.insertWorkspaces(List.of(workspace(null, studyId, null, "A")));
    assetDAO.insertBiospecimens(List.of(biospecimen(null, studyId, null, "A")));

    studyDAO.deleteStudyByStudyId(studyId);

    assertTrue(assetDAO.findModelsByStudyId(studyId).isEmpty());
    assertTrue(assetDAO.findWorkspacesByStudyId(studyId).isEmpty());
    assertTrue(assetDAO.findBiospecimensByStudyId(studyId).isEmpty());
  }

  private static StudyModelRecord model(
      Integer modelId, Integer studyId, String legacyKey, String name) {
    return new StudyModelRecord(
        modelId,
        studyId,
        legacyKey,
        name,
        "A model",
        "https://example.org/model",
        List.of("GCP", "AWS"),
        "ONNX",
        "MIT",
        List.of("DUOS-000001"),
        MAINTAINER,
        List.of("tag"),
        "open",
        DATA_USE,
        null,
        null,
        null);
  }

  private static StudyWorkspaceRecord workspace(
      Integer workspaceId, Integer studyId, String legacyKey, String name) {
    return new StudyWorkspaceRecord(
        workspaceId,
        studyId,
        legacyKey,
        name,
        "Terra",
        "https://example.org/workspace",
        List.of("GCP"),
        "A workspace",
        List.of("Jupyter", "RStudio"),
        "controlled",
        List.of("tag"),
        "controlled",
        DATA_USE,
        null,
        null,
        null);
  }

  private static StudyBiospecimenRecord biospecimen(
      Integer biospecimenId, Integer studyId, String legacyKey, String sampleId) {
    return new StudyBiospecimenRecord(
        biospecimenId,
        studyId,
        legacyKey,
        sampleId,
        "DONOR-1",
        "Brain",
        "Frozen",
        "-80C",
        "2020-01-01",
        new BigDecimal("12.5"),
        "hours",
        "Female",
        new BigDecimal("64"),
        "Unknown",
        "United States",
        List.of("Alzheimer's disease", "Hypertension"),
        "None",
        "Biobank",
        "Hospital",
        null,
        null,
        null,
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
