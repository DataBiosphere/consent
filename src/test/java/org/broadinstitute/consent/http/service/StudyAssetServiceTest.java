package org.broadinstitute.consent.http.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.NotFoundException;
import java.util.List;
import java.util.Map;
import org.broadinstitute.consent.http.db.StudyDAO;
import org.broadinstitute.consent.http.enumeration.PropertyType;
import org.broadinstitute.consent.http.models.Study;
import org.broadinstitute.consent.http.models.StudyProperty;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StudyAssetServiceTest {

  @Mock private Jdbi jdbi;
  @Mock private StudyDAO studyDAO;
  @Mock private DatasetService datasetService;

  private final User user = new User();

  private StudyAssetService service;

  @BeforeEach
  void setUp() {
    when(jdbi.onDemand(StudyDAO.class)).thenReturn(studyDAO);
    service = new StudyAssetService(jdbi, datasetService);
  }

  /** The visibility gate passes the study through when the user may read it. */
  private void allowVisibility() {
    when(datasetService.verifyStudyVisibilityAccess(any(), any()))
        .thenAnswer(invocation -> invocation.getArgument(0));
  }

  /** Assets are read from the promoted first-class property. */
  @Test
  void returnsAssetsOfEachTypeFromPromotedProperties() {
    Study study = new Study();
    study.setStudyId(1);
    study.addProperty(
        new StudyProperty(
            "publications",
            GsonUtil.getInstance()
                .toJsonTree(List.of(Map.of("publicationId", "pub-1", "title", "A publication"))),
            PropertyType.Json));
    study.addProperty(
        new StudyProperty(
            "models",
            GsonUtil.getInstance().toJsonTree(List.of(Map.of("modelId", "model-1"))),
            PropertyType.Json));
    when(studyDAO.findStudyById(1)).thenReturn(study);
    allowVisibility();

    List<Object> publications = service.getAssetsByType(1, user, "publications");

    assertEquals(1, publications.size());
    assertEquals("A publication", ((Map<?, ?>) publications.getFirst()).get("title"));
    assertEquals(1, service.getAssetsByType(1, user, "models").size());
    // A type with no property reads as no assets of that type
    assertEquals(List.of(), service.getAssetsByType(1, user, "workspaces"));
  }

  /**
   * Until every client writes the promoted fields, a study still carrying the legacy assets object
   * must keep reading correctly.
   */
  @Test
  void returnsAssetsOfEachTypeFromLegacyAssetsProperty() {
    Study study = new Study();
    study.setStudyId(1);
    study.addProperty(
        new StudyProperty(
            "assets",
            GsonUtil.getInstance()
                .toJsonTree(
                    Map.of(
                        "publications",
                        List.of(Map.of("publicationId", "pub-1", "title", "A publication")),
                        "models",
                        List.of(Map.of("modelId", "model-1")))),
            PropertyType.Json));
    when(studyDAO.findStudyById(1)).thenReturn(study);
    allowVisibility();

    List<Object> publications = service.getAssetsByType(1, user, "publications");
    List<Object> models = service.getAssetsByType(1, user, "models");

    assertEquals(1, publications.size());
    assertEquals("A publication", ((Map<?, ?>) publications.getFirst()).get("title"));
    assertEquals(1, models.size());
    assertEquals("model-1", ((Map<?, ?>) models.getFirst()).get("modelId"));
    // A key absent from the property reads as no assets of that type
    assertEquals(List.of(), service.getAssetsByType(1, user, "workspaces"));
  }

  @Test
  void testStudyNotFound() {
    when(studyDAO.findStudyById(1)).thenReturn(null);

    assertThrows(NotFoundException.class, () -> service.getAssetsByType(1, user, "publications"));
  }

  /**
   * A study the user may not read must not leak its assets through the sub-resources, the same way
   * StudyResource hides the study itself.
   */
  @Test
  void testStudyNotVisibleToUser() {
    Study study = new Study();
    study.setStudyId(1);
    study.setPublicVisibility(false);
    when(studyDAO.findStudyById(1)).thenReturn(study);
    when(datasetService.verifyStudyVisibilityAccess(any(), any()))
        .thenThrow(new NotFoundException("Study not found"));

    assertThrows(NotFoundException.class, () -> service.getAssetsByType(1, user, "publications"));
  }

  @Test
  void testStudyWithoutAssetsProperty() {
    Study study = new Study();
    study.setStudyId(1);
    when(studyDAO.findStudyById(1)).thenReturn(study);
    allowVisibility();

    assertEquals(List.of(), service.getAssetsByType(1, user, "publications"));
  }

  @Test
  void testNonCollectionAssetValueReadsAsNoAssets() {
    Study study = new Study();
    study.setStudyId(1);
    study.addProperty(
        new StudyProperty(
            "assets",
            GsonUtil.getInstance().toJsonTree(Map.of("models", "not a list")),
            PropertyType.Json));
    when(studyDAO.findStudyById(1)).thenReturn(study);
    allowVisibility();

    assertEquals(List.of(), service.getAssetsByType(1, user, "models"));
  }

  /** The assets property is client-managed and unvalidated, so a bad value must not 500. */
  @Test
  void testMalformedAssetsPropertyReadsAsNoAssets() {
    Study study = new Study();
    study.setStudyId(1);
    study.addProperty(new StudyProperty("assets", "not json at all", PropertyType.String));
    when(studyDAO.findStudyById(1)).thenReturn(study);
    allowVisibility();

    assertEquals(List.of(), service.getAssetsByType(1, user, "publications"));
  }
}
