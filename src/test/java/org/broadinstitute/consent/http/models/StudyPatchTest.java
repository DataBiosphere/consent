package org.broadinstitute.consent.http.models;

import static org.broadinstitute.consent.http.models.StudyPatch.ALTERNATIVE_DATA_SHARING_PLAN_TARGET_DELIVERY_DATE;
import static org.broadinstitute.consent.http.models.StudyPatch.ALTERNATIVE_DATA_SHARING_PLAN_TARGET_PUBLIC_RELEASE_DATE;
import static org.broadinstitute.consent.http.models.StudyPatch.DATA_CUSTODIAN_EMAIL;
import static org.broadinstitute.consent.http.models.StudyPatch.EXTERNAL_IDENTIFIER;
import static org.broadinstitute.consent.http.models.StudyPatch.EXTERNAL_IDENTIFIER_TYPE;
import static org.broadinstitute.consent.http.models.StudyPatch.PHENOTYPE_INDICATION;
import static org.broadinstitute.consent.http.models.StudyPatch.SPECIES_KEY;
import static org.broadinstitute.consent.http.models.StudyPatch.STUDY_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
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
            null, null, null, null, null, null, null, null, null, null, null, null, null, null,
            null, null, null, null);
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
            null,
            null,
            null,
            null,
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
            "Name", null, null, null, null, null, null, null, null, null, null, null, null, null,
            null, null, null, null);
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
            null, null, null, null, null, null, null, null, null, null, null, null, null, null,
            null, false, null, null);
    assertTrue(patch.isPatchable(study));
  }

  @Test
  void testIsPatchableExternalIdentifier() {
    Study study = mockStudy();
    StudyPatch patch =
        new StudyPatch(
            null, null, null, null, null, null, null, null, null, null, null, null, null, null,
            null, null, "SCP1671", null);
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
            null,
            null,
            null,
            null,
            "Single Cell Portal");
    assertTrue(patch.isPatchable(study));
  }

  @Test
  void testIsPatchableExternalIdentifierDifferentValue() {
    Study study = mockStudy();
    study.addProperty(new StudyProperty(EXTERNAL_IDENTIFIER, "OLD_ID", PropertyType.String));
    StudyPatch patch =
        new StudyPatch(
            null, null, null, null, null, null, null, null, null, null, null, null, null, null,
            null, null, "NEW_ID", null);
    assertTrue(patch.isPatchable(study));
  }

  @Test
  void testIsPatchableSameExternalIdentifier() {
    Study study = mockStudy();
    String existingValue = randomAlphabetic(10);
    study.addProperty(new StudyProperty(EXTERNAL_IDENTIFIER, existingValue, PropertyType.String));
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
            null,
            null,
            null,
            existingValue,
            null);
    assertFalse(patch.isPatchable(study));
  }

  @Test
  void testIsPatchableExternalIdentifierTypeDifferentValue() {
    Study study = mockStudy();
    study.addProperty(new StudyProperty(EXTERNAL_IDENTIFIER_TYPE, "Old Type", PropertyType.String));
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
            null,
            null,
            null,
            null,
            "New Type");
    assertTrue(patch.isPatchable(study));
  }

  @Test
  void testIsPatchableSameExternalIdentifierType() {
    Study study = mockStudy();
    String existingValue = randomAlphabetic(10);
    study.addProperty(
        new StudyProperty(EXTERNAL_IDENTIFIER_TYPE, existingValue, PropertyType.String));
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
            null,
            null,
            null,
            null,
            existingValue);
    assertFalse(patch.isPatchable(study));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", " ", "   "})
  void testIsPatchableExternalIdentifierBlankIsFalseWhenPropertyAbsent(String blank) {
    Study study = mockStudy();
    StudyPatch patch =
        new StudyPatch(
            null, null, null, null, null, null, null, null, null, null, null, null, null, null,
            null, null, blank, null);
    assertFalse(patch.isPatchable(study));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", " ", "   "})
  void testIsPatchableExternalIdentifierBlankIsTrueWhenPropertyExists(String blank) {
    Study study = mockStudy();
    study.addProperty(new StudyProperty(EXTERNAL_IDENTIFIER, "SCP1671", PropertyType.String));
    StudyPatch patch =
        new StudyPatch(
            null, null, null, null, null, null, null, null, null, null, null, null, null, null,
            null, null, blank, null);
    assertTrue(patch.isPatchable(study));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", " ", "   "})
  void testIsPatchableExternalIdentifierTypeBlankIsFalseWhenPropertyAbsent(String blank) {
    Study study = mockStudy();
    StudyPatch patch =
        new StudyPatch(
            null, null, null, null, null, null, null, null, null, null, null, null, null, null,
            null, null, null, blank);
    assertFalse(patch.isPatchable(study));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", " ", "   "})
  void testIsPatchableExternalIdentifierTypeBlankIsTrueWhenPropertyExists(String blank) {
    Study study = mockStudy();
    study.addProperty(
        new StudyProperty(EXTERNAL_IDENTIFIER_TYPE, "Single Cell Portal", PropertyType.String));
    StudyPatch patch =
        new StudyPatch(
            null, null, null, null, null, null, null, null, null, null, null, null, null, null,
            null, null, null, blank);
    assertTrue(patch.isPatchable(study));
  }

  @Test
  void testIsPatchablePiDetailsChanged() {
    Study study = mockStudy();

    assertTrue(patchWithPiDetails(1, null, null, null).isPatchable(study));
    assertTrue(patchWithPiDetails(null, "0000-0001-2345-6789", null, null).isPatchable(study));
    assertTrue(
        patchWithPiDetails(null, null, "https://linkedin.com/in/pi", null).isPatchable(study));
    assertTrue(patchWithPiDetails(null, null, null, "https://pi.example.com").isPatchable(study));
  }

  @Test
  void testIsPatchablePiDetailsSameValues() {
    Study study = mockStudy();
    Institution institution = new Institution();
    institution.setId(1);
    study.setPiInstitution(institution);
    study.setPiOrcid("0000-0001-2345-6789");
    study.setPiLinkedinUrl("https://linkedin.com/in/pi");
    study.setPiWebsiteUrl("https://pi.example.com");

    StudyPatch patch =
        patchWithPiDetails(
            1, "0000-0001-2345-6789", "https://linkedin.com/in/pi", "https://pi.example.com");
    assertFalse(patch.isPatchable(study));
  }

  @Test
  void testIsPatchablePiInstitutionChanged() {
    Study study = mockStudy();
    Institution institution = new Institution();
    institution.setId(1);
    study.setPiInstitution(institution);

    assertFalse(patchWithPiDetails(1, null, null, null).isPatchable(study));
    assertTrue(patchWithPiDetails(2, null, null, null).isPatchable(study));
  }

  /**
   * The PI columns are columns on the study row, not patchable properties, so they follow the JSON
   * convention: an absent field is a no-op and an explicit null clears. A blank string is
   * normalized to a clear rather than stored.
   */
  @Test
  void testIsPatchableClearsPiDetailsOnExplicitNull() {
    Study study = mockStudy();
    Institution institution = new Institution();
    institution.setId(1);
    study.setPiInstitution(institution);
    study.setPiOrcid("0000-0001-2345-6789");

    // An explicit null clears a stored value, so it is patchable
    assertTrue(StudyPatch.fromJson("{\"piOrcid\": null}").isPatchable(study));
    assertTrue(StudyPatch.fromJson("{\"piInstitutionId\": null}").isPatchable(study));
    // A blank string is normalized to the same clear
    assertTrue(StudyPatch.fromJson("{\"piOrcid\": \"\"}").isPatchable(study));

    // An absent field is a no-op, even when a value is stored
    assertFalse(StudyPatch.fromJson("{}").isPatchable(study));

    // Clearing a column that is already empty is not a change
    assertFalse(StudyPatch.fromJson("{\"piLinkedinUrl\": null}").isPatchable(study));
    assertFalse(StudyPatch.fromJson("{\"piWebsiteUrl\": \"\"}").isPatchable(study));

    Study noPiDetails = mockStudy();
    assertFalse(StudyPatch.fromJson("{\"piOrcid\": null}").isPatchable(noPiDetails));
    assertFalse(StudyPatch.fromJson("{\"piInstitutionId\": null}").isPatchable(noPiDetails));
  }

  @Test
  void testResolvePiDetailConventions() {
    // Absent = keep the stored value
    StudyPatch absent = StudyPatch.fromJson("{}");
    assertEquals("existing", absent.resolvePiOrcid("existing"));
    assertEquals("existing", absent.resolvePiLinkedinUrl("existing"));
    assertEquals("existing", absent.resolvePiWebsiteUrl("existing"));
    assertEquals(5, absent.resolvePiInstitutionId(5));

    // Explicit null = clear
    StudyPatch cleared =
        StudyPatch.fromJson(
            """
            {"piInstitutionId": null, "piOrcid": null,
             "piLinkedinUrl": null, "piWebsiteUrl": null}
            """);
    assertNull(cleared.resolvePiOrcid("existing"));
    assertNull(cleared.resolvePiLinkedinUrl("existing"));
    assertNull(cleared.resolvePiWebsiteUrl("existing"));
    assertNull(cleared.resolvePiInstitutionId(5));

    // Blank = clear; a value = set
    assertNull(StudyPatch.fromJson("{\"piOrcid\": \"\"}").resolvePiOrcid("existing"));
    assertNull(StudyPatch.fromJson("{\"piOrcid\": \"   \"}").resolvePiOrcid("existing"));
    assertEquals("new", StudyPatch.fromJson("{\"piOrcid\": \"new\"}").resolvePiOrcid("existing"));
    assertEquals(6, StudyPatch.fromJson("{\"piInstitutionId\": 6}").resolvePiInstitutionId(5));
  }

  /** A patch built directly, rather than from a body, has no explicitly nulled fields. */
  @Test
  void testDirectlyBuiltPatchHasNoExplicitNulls() {
    StudyPatch patch = patchWithPiDetails(null, null, null, null);
    assertEquals(Set.of(), patch.explicitNulls());
    assertEquals("existing", patch.resolvePiOrcid("existing"));
    assertEquals(5, patch.resolvePiInstitutionId(5));
  }

  private StudyPatch patchWithPiDetails(
      Integer piInstitutionId, String piOrcid, String piLinkedinUrl, String piWebsiteUrl) {
    return new StudyPatch(
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        piInstitutionId,
        piOrcid,
        piLinkedinUrl,
        piWebsiteUrl,
        null,
        null,
        null,
        null,
        null,
        null);
  }

  @Test
  void testFromJsonExternalIdentifierFields() {
    String json =
        "{\"externalIdentifier\": \"SCP1671\", \"externalIdentifierType\": \"Single Cell Portal\"}";
    StudyPatch patch = StudyPatch.fromJson(json);
    assertEquals("SCP1671", patch.externalIdentifier());
    assertEquals("Single Cell Portal", patch.externalIdentifierType());
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
