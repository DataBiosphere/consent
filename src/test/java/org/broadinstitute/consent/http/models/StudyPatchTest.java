package org.broadinstitute.consent.http.models;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.broadinstitute.consent.http.AbstractTestHelper;
import org.broadinstitute.consent.http.enumeration.PropertyType;
import org.broadinstitute.consent.http.models.dataset_registration_v1.DatasetRegistrationSchemaV1.StudyType;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StudyPatchTest extends AbstractTestHelper {

  @Test
  void testIsPatchableNoValues() {
    Study study = mockStudy();
    StudyPatch patch =
        new StudyPatch(null, null, null, null, null, null, null, null, null, null, null);
    assertFalse(patch.isPatchable(study));
  }

  @Test
  void testIsPatchableSameValues() {
    Study study = mockStudy();
    StudyPatch patch =
        new StudyPatch(
            study.getName(),
            StudyType.OBSERVATIONAL,
            study.getDescription(),
            study.getDataTypes(),
            "CANCER",
            "HUMAN",
            study.getPiName(),
            List.of("EMAIL1", "EMAIL2"),
            "01/01/2020",
            "01/01/2020",
            study.getPublicVisibility());
    assertFalse(patch.isPatchable(study));
  }

  @Test
  void testIsPatchableName() {
    Study study = mockStudy();
    StudyPatch patch =
        new StudyPatch("Name", null, null, null, null, null, null, null, null, null, null);
    assertTrue(patch.isPatchable(study));
  }

  @Test
  void testIsPatchableStudyType() {
    Study study = mockStudy();
    StudyPatch patch =
        new StudyPatch(
            null, StudyType.ANALYTICAL, null, null, null, null, null, null, null, null, null);
    assertTrue(patch.isPatchable(study));
  }

  @Test
  void testIsPatchableDescription() {
    Study study = mockStudy();
    StudyPatch patch =
        new StudyPatch(null, null, "Description", null, null, null, null, null, null, null, null);
    assertTrue(patch.isPatchable(study));
  }

  @Test
  void testIsPatchableDataTypes() {
    Study study = mockStudy();
    StudyPatch patch =
        new StudyPatch(
            null, null, null, List.of("type1", "type2"), null, null, null, null, null, null, null);
    assertTrue(patch.isPatchable(study));
  }

  @Test
  void testIsPatchablePhenotype() {
    Study study = mockStudy();
    StudyPatch patch =
        new StudyPatch(null, null, null, null, "New Phenotype", null, null, null, null, null, null);
    assertTrue(patch.isPatchable(study));
  }

  @Test
  void testIsPatchableSpecies() {
    Study study = mockStudy();
    StudyPatch patch =
        new StudyPatch(null, null, null, null, null, "New Species", null, null, null, null, null);
    assertTrue(patch.isPatchable(study));
  }

  @Test
  void testIsPatchablePIName() {
    Study study = mockStudy();
    StudyPatch patch =
        new StudyPatch(null, null, null, null, null, null, "New PI Name", null, null, null, null);
    assertTrue(patch.isPatchable(study));
  }

  @Test
  void testIsPatchableDataCustodians() {
    Study study = mockStudy();
    StudyPatch patch =
        new StudyPatch(
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            List.of("new_email1", "new_email2"),
            null,
            null,
            null);
    assertTrue(patch.isPatchable(study));
  }

  @Test
  void testIsPatchableTargetDeliveryDate() {
    Study study = mockStudy();
    StudyPatch patch =
        new StudyPatch(null, null, null, null, null, null, null, null, "New Date", null, null);
    assertTrue(patch.isPatchable(study));
  }

  @Test
  void testIsPatchableTargetReleaseDate() {
    Study study = mockStudy();
    StudyPatch patch =
        new StudyPatch(null, null, null, null, null, null, null, null, null, "New Date", null);
    assertTrue(patch.isPatchable(study));
  }

  @Test
  void testIsPatchablePublicVisibility() {
    Study study = mockStudy();
    StudyPatch patch =
        new StudyPatch(null, null, null, null, null, null, null, null, null, null, false);
    assertTrue(patch.isPatchable(study));
  }

  private Study mockStudy() {
    Study study = new Study();
    study.setName(randomAlphabetic(20));
    study.setDescription(randomAlphabetic(20));
    study.setDataTypes(List.of(randomAlphabetic(20), randomAlphabetic(20)));
    study.setPiName(randomAlphabetic(20));
    study.setPublicVisibility(true);
    study.addProperties(
        new StudyProperty("studyType", StudyType.OBSERVATIONAL.value(), PropertyType.String),
        new StudyProperty("phenotypeIndication", "CANCER", PropertyType.String),
        new StudyProperty("species", "HUMAN", PropertyType.String),
        new StudyProperty(
            "dataCustodianEmail",
            GsonUtil.getInstance().toJson(List.of("EMAIL1", "EMAIL2")),
            PropertyType.Json),
        new StudyProperty(
            "alternativeDataSharingPlanTargetDeliveryDate", "01/01/2020", PropertyType.String),
        new StudyProperty(
            "alternativeDataSharingPlanTargetPublicReleaseDate",
            "01/01/2020",
            PropertyType.String));
    return study;
  }
}
