package org.broadinstitute.consent.http.models;

import static org.broadinstitute.consent.http.models.StudyPatch.ALTERNATIVE_DATA_SHARING_PLAN_TARGET_DELIVERY_DATE;
import static org.broadinstitute.consent.http.models.StudyPatch.ALTERNATIVE_DATA_SHARING_PLAN_TARGET_PUBLIC_RELEASE_DATE;
import static org.broadinstitute.consent.http.models.StudyPatch.DATA_CUSTODIAN_EMAIL;
import static org.broadinstitute.consent.http.models.StudyPatch.PHENOTYPE_INDICATION;
import static org.broadinstitute.consent.http.models.StudyPatch.SPECIES_KEY;
import static org.broadinstitute.consent.http.models.StudyPatch.STUDY_TYPE;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.broadinstitute.consent.http.AbstractTestHelper;
import org.broadinstitute.consent.http.enumeration.PropertyType;
import org.broadinstitute.consent.http.models.dataset_registration_v1.DatasetRegistrationSchemaV1.StudyType;
import org.broadinstitute.consent.http.util.gson.GsonUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StudyPatchTest extends AbstractTestHelper {

  @ParameterizedTest
  @ValueSource(
      strings = {
        "{ invalid json }",
        "{ \"unknownField\": \"some value\" }",
        "{ \"name\": 12345 }",
        "{ \"studyType\": \"not a study type\" }",
        "{ \"publicVisibility\": \"not a boolean\" }",
        "{ \"publicVisibility\": \"true\" }",
        "{ \"publicVisibility\": \"false\" }"
      })
  void testFromJson(String json) {
    assertThrows(Exception.class, () -> StudyPatch.fromJson(json));
  }

  @Test
  void testIsPatchableNoValues() {
    Study study = mockStudy();
    StudyPatch patch =
        new StudyPatch(
            null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    assertFalse(patch.isPatchable(study));
  }

  @Test
  void testIsPatchableSameValues() {
    Study mockStudy = mockStudy();
    StudyPatch patch =
        new StudyPatch(
            mockStudy.getName(),
            StudyType.OBSERVATIONAL,
            mockStudy.getDescription(),
            mockStudy.getDataTypes(),
            "CANCER",
            "HUMAN",
            mockStudy.getPiName(),
            mockStudy.getPiEmail(),
            List.of("EMAIL1", "EMAIL2"),
            "01/01/2020",
            "01/01/2020",
            mockStudy.getPublicVisibility(),
            null,
            null);
    assertFalse(patch.isPatchable(mockStudy));
  }

  @Test
  void testIsPatchableName() {
    Study study = mockStudy();
    StudyPatch patch =
        new StudyPatch(
            "Name", null, null, null, null, null, null, null, null, null, null, null, null, null);
    assertTrue(patch.isPatchable(study));
  }

  @Test
  void testIsPatchableStudyType() {
    Study study = mockStudy();
    StudyPatch patch =
        new StudyPatch(
            null,
            StudyType.ANALYTICAL,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null);
    assertTrue(patch.isPatchable(study));
  }

  @Test
  void testIsPatchableDescription() {
    Study study = mockStudy();
    StudyPatch patch =
        new StudyPatch(
            null,
            null,
            "Description",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null);
    assertTrue(patch.isPatchable(study));
  }

  @Test
  void testIsPatchableDataTypes() {
    Study study = mockStudy();
    StudyPatch patch =
        new StudyPatch(
            null,
            null,
            null,
            List.of("type1", "type2"),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null);
    assertTrue(patch.isPatchable(study));
  }

  @Test
  void testIsPatchablePhenotype() {
    Study study = mockStudy();
    StudyPatch patch =
        new StudyPatch(
            null,
            null,
            null,
            null,
            "New Phenotype",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null);
    assertTrue(patch.isPatchable(study));
  }

  @Test
  void testIsPatchableSpecies() {
    Study study = mockStudy();
    StudyPatch patch =
        new StudyPatch(
            null,
            null,
            null,
            null,
            null,
            "New Species",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null);
    assertTrue(patch.isPatchable(study));
  }

  @Test
  void testIsPatchablePIName() {
    Study study = mockStudy();
    StudyPatch patch =
        new StudyPatch(
            null,
            null,
            null,
            null,
            null,
            null,
            "New PI Name",
            null,
            null,
            null,
            null,
            null,
            null,
            null);
    assertTrue(patch.isPatchable(study));
  }

  @Test
  void testIsPatchablePIEmail() {
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
            "New PI Email",
            null,
            null,
            null,
            null,
            null,
            null);
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
            null,
            List.of("new_email1", "new_email2"),
            null,
            null,
            null,
            null,
            null);
    assertTrue(patch.isPatchable(study));
  }

  @Test
  void testIsPatchableTargetDeliveryDate() {
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
            null,
            null,
            "New Date",
            null,
            null,
            null,
            null);
    assertTrue(patch.isPatchable(study));
  }

  @Test
  void testIsPatchableTargetReleaseDate() {
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
            null,
            null,
            null,
            "New Date",
            null,
            null,
            null);
    assertTrue(patch.isPatchable(study));
  }

  @Test
  void testIsPatchablePublicVisibility() {
    Study study = mockStudy();
    StudyPatch patch =
        new StudyPatch(
            null, null, null, null, null, null, null, null, null, null, null, false, null, null);
    assertTrue(patch.isPatchable(study));
  }

  @Test
  void testIsPatchableExternalIdentifier() {
    Study study = mockStudy();
    StudyPatch patch =
        new StudyPatch(
            null, null, null, null, null, null, null, null, null, null, null, null, "SCP1671",
            null);
    assertTrue(patch.isPatchable(study));
  }

  @Test
  void testIsPatchableExternalIdentifierType() {
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
            null,
            null,
            null,
            null,
            null,
            null,
            "Single Cell Portal");
    assertTrue(patch.isPatchable(study));
  }

  private Study mockStudy() {
    Study study = new Study();
    study.setName(randomAlphabetic(20));
    study.setDescription(randomAlphabetic(20));
    study.setDataTypes(List.of(randomAlphabetic(20), randomAlphabetic(20)));
    study.setPiName(randomAlphabetic(20));
    study.setPiEmail(randomAlphabetic(20));
    study.setPublicVisibility(true);
    study.addProperties(
        new StudyProperty(STUDY_TYPE, StudyType.OBSERVATIONAL.value(), PropertyType.String),
        new StudyProperty(PHENOTYPE_INDICATION, "CANCER", PropertyType.String),
        new StudyProperty(SPECIES_KEY, "HUMAN", PropertyType.String),
        new StudyProperty(
            DATA_CUSTODIAN_EMAIL,
            GsonUtil.getInstance().toJson(List.of("EMAIL1", "EMAIL2")),
            PropertyType.Json),
        new StudyProperty(
            ALTERNATIVE_DATA_SHARING_PLAN_TARGET_DELIVERY_DATE, "01/01/2020", PropertyType.String),
        new StudyProperty(
            ALTERNATIVE_DATA_SHARING_PLAN_TARGET_PUBLIC_RELEASE_DATE,
            "01/01/2020",
            PropertyType.String));
    return study;
  }
}
