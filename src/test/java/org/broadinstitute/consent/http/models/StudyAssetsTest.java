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

  /**
   * The round trip the migration's preservation depends on. A legacy value under a promoted name
   * that is not a list cannot be promoted, so the migration keeps it in the assets object. It has
   * to survive a read and the next write too: dropping it from assemble would remove it from
   * registration and search responses, and stripping it on write would then delete the only copy.
   */
  @Test
  void testAWrongShapedLegacyValueSurvivesAReadAndTheNextWrite() {
    Set<StudyProperty> properties =
        Set.of(jsonProperty(StudyAssets.ASSETS, "{\"models\": \"custom-value\", \"other\": true}"));

    Map<String, Object> assembled = studyAssets.assemble(properties);
    assertEquals(Map.of("models", "custom-value", "other", true), assembled);

    // What a client would send back is what it was given, so the next write sees the same object.
    assertNull(StudyAssets.promotedValue(null, assembled, StudyAssets.MODELS));
    assertEquals(
        Map.of("models", "custom-value", "other", true), StudyAssets.stripPromoted(assembled));
  }

  /** A list under a promoted name is still stripped: it is stored as the promoted property. */
  @Test
  void testStripPromotedStillRemovesAPromotableList() {
    Map<String, Object> assets =
        Map.of(StudyAssets.MODELS, List.of(Map.of("name", "a")), "other", true);

    assertEquals(Map.of("other", true), StudyAssets.stripPromoted(assets));
  }

  /**
   * A promoted property is the newer intent, so it wins the key in the assembled object. The legacy
   * value is not stripped on write, so it is masked rather than destroyed.
   */
  @Test
  void testAPromotedPropertyWinsOverAWrongShapedLegacyValue() {
    Set<StudyProperty> properties =
        Set.of(
            jsonProperty(StudyAssets.MODELS, "[{\"name\": \"promoted\"}]"),
            jsonProperty(StudyAssets.ASSETS, "{\"models\": \"custom-value\"}"));

    assertEquals(
        List.of(Map.of("name", "promoted")), studyAssets.assemble(properties).get("models"));
  }

  /**
   * The legacy object is client-supplied, so its casing is not guaranteed. A differently cased
   * promoted name has to be treated the same by every path: a lookup that missed "Models" while the
   * removal matched it would drop the list from the assets object without promoting it.
   */
  @Test
  void testADifferentlyCasedPromotedKeyRoundTrips() {
    List<Object> models = List.of(Map.of("name", "a"));
    Map<String, Object> assets = Map.of("Models", models, "other", true);

    // Promoted from the differently cased key, not ignored
    assertEquals(models, StudyAssets.promotedValue(null, assets, StudyAssets.MODELS));
    // And therefore safe to strip, because it is now stored as the promoted property
    assertEquals(Map.of("other", true), StudyAssets.stripPromoted(assets));
  }

  /** The same casing tolerance on the read path. */
  @Test
  void testFindAssetListReadsADifferentlyCasedLegacyKey() {
    Set<StudyProperty> properties =
        Set.of(jsonProperty(StudyAssets.ASSETS, "{\"Publications\": [{\"title\": \"legacy\"}]}"));

    assertEquals(1, studyAssets.findAssetList(properties, StudyAssets.PUBLICATIONS).size());
  }

  /** A wrong-shaped value under a differently cased name is kept by both paths, not just one. */
  @Test
  void testADifferentlyCasedWrongShapedValueIsNotStripped() {
    Map<String, Object> assets = Map.of("Models", "custom-value", "other", true);

    assertNull(StudyAssets.promotedValue(null, assets, StudyAssets.MODELS));
    assertEquals(assets, StudyAssets.stripPromoted(assets));
  }

  private StudyProperty jsonProperty(String key, String value) {
    StudyProperty property = new StudyProperty();
    property.setKey(key);
    property.setType(PropertyType.Json);
    property.setValue(PropertyType.Json.coerce(value));
    return property;
  }
}
