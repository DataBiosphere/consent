package org.broadinstitute.consent.http.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.broadinstitute.consent.http.enumeration.PropertyType;
import org.junit.jupiter.api.Test;

class StudyAssetsTest {

  private final StudyAssets studyAssets = new StudyAssets();

  @Test
  void testFindAssetListReadsThePromotedProperty() {
    Set<StudyProperty> properties =
        Set.of(jsonProperty(StudyAssets.PUBLICATIONS, "[{\"title\": \"promoted\"}]"));

    assertEquals(1, studyAssets.findAssetList(properties, StudyAssets.PUBLICATIONS).size());
  }

  /** A study whose registration has not been rewritten since the promotion still reads. */
  @Test
  void testFindAssetListFallsBackToTheLegacyObject() {
    Set<StudyProperty> properties =
        Set.of(jsonProperty(StudyAssets.ASSETS, "{\"publications\": [{\"title\": \"legacy\"}]}"));

    assertEquals(1, studyAssets.findAssetList(properties, StudyAssets.PUBLICATIONS).size());
  }

  /**
   * An explicitly empty promoted property means the submitter removed the last asset of that type.
   * Falling back to the legacy object here would resurrect it.
   */
  @Test
  void testAnEmptyPromotedPropertyIsNotOverriddenByTheLegacyObject() {
    Set<StudyProperty> properties =
        Set.of(
            jsonProperty(StudyAssets.PUBLICATIONS, "[]"),
            jsonProperty(StudyAssets.ASSETS, "{\"publications\": [{\"title\": \"removed\"}]}"));

    assertTrue(studyAssets.findAssetList(properties, StudyAssets.PUBLICATIONS).isEmpty());
  }

  /** A malformed promoted value is treated as absent, so the legacy object is still consulted. */
  @Test
  void testAMalformedPromotedPropertyFallsBackToTheLegacyObject() {
    StudyProperty malformed = new StudyProperty();
    malformed.setKey(StudyAssets.PUBLICATIONS);
    malformed.setType(PropertyType.String);
    malformed.setValue("not json");
    Set<StudyProperty> properties =
        Set.of(
            malformed,
            jsonProperty(StudyAssets.ASSETS, "{\"publications\": [{\"title\": \"legacy\"}]}"));

    assertEquals(1, studyAssets.findAssetList(properties, StudyAssets.PUBLICATIONS).size());
  }

  /** An emptied list is stored as an empty list, not dropped back to the legacy copy. */
  @Test
  void testPromotedValueKeepsAnExplicitlyEmptyTopLevelList() {
    List<Object> legacyCopy = List.of(Map.of("title", "removed"));

    assertEquals(
        List.of(),
        StudyAssets.promotedValue(
            List.of(), Map.of(StudyAssets.PUBLICATIONS, legacyCopy), StudyAssets.PUBLICATIONS));
  }

  /** An omitted field is still filled in from the deprecated object. */
  @Test
  void testPromotedValueFallsBackWhenTheTopLevelFieldIsAbsent() {
    List<Object> legacyCopy = List.of(Map.of("title", "legacy"));

    assertEquals(
        legacyCopy,
        StudyAssets.promotedValue(
            null, Map.of(StudyAssets.PUBLICATIONS, legacyCopy), StudyAssets.PUBLICATIONS));
    assertNull(StudyAssets.promotedValue(null, Map.of(), StudyAssets.PUBLICATIONS));
  }

  /** An emptied list drops out of the assembled object, so a later read has nothing to restore. */
  @Test
  void testAssembleOmitsAnEmptyPromotedList() {
    Set<StudyProperty> properties = Set.of(jsonProperty(StudyAssets.PUBLICATIONS, "[]"));

    assertTrue(studyAssets.assemble(properties).isEmpty());
  }

  /** An explicit empty promoted property also removes a stale copy from the legacy object. */
  @Test
  void testAssembleDoesNotResurrectLegacyAssets() {
    Set<StudyProperty> properties =
        Set.of(
            jsonProperty(StudyAssets.PUBLICATIONS, "[]"),
            jsonProperty(
                StudyAssets.ASSETS,
                "{\"publications\": [{\"title\": \"removed\"}], \"other\": true}"));

    assertEquals(Map.of("other", true), studyAssets.assemble(properties));
  }

  private StudyProperty jsonProperty(String key, String value) {
    StudyProperty property = new StudyProperty();
    property.setKey(key);
    property.setType(PropertyType.Json);
    property.setValue(PropertyType.Json.coerce(value));
    return property;
  }
}
