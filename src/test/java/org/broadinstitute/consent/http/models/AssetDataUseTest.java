package org.broadinstitute.consent.http.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.broadinstitute.consent.http.models.datause.DataUsePrimaryValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AssetDataUseTest {

  @ParameterizedTest
  @ValueSource(strings = {StudyAssets.MODELS, StudyAssets.WORKSPACES})
  void testValidEntriesHaveNoViolations(String key) {
    List<Object> entries =
        List.of(
            Map.of("name", "no new fields"),
            Map.of("accessManagement", "open", "dataUse", Map.of()),
            Map.of("accessManagement", "controlled", "dataUse", Map.of("hmbResearch", true)),
            Map.of("dataUse", Map.of("generalUse", true, "nonProfitUse", true)));

    assertTrue(AssetDataUse.violations(key, entries, List.of()).isEmpty());
    List<Object> stored = AssetDataUse.withoutSummaries(key, entries);
    assertEquals(entries, stored);
    assertNotSame(entries, stored);
  }

  @Test
  void testInvalidAccessManagementIsReportedWithItsLocation() {
    List<Object> entries =
        List.of(Map.of("name", "fine"), Map.of("name", "bad", "accessManagement", "Open"));

    List<String> violations = AssetDataUse.violations(StudyAssets.MODELS, entries, List.of());

    assertEquals(
        List.of("models[1].accessManagement: must be one of open, controlled, external"),
        violations);
  }

  @Test
  void testNonStringAccessManagementIsReported() {
    List<String> violations =
        AssetDataUse.violations(
            StudyAssets.WORKSPACES, List.of(Map.of("accessManagement", 1)), List.of());

    assertEquals(
        List.of("workspaces[0].accessManagement: must be one of open, controlled, external"),
        violations);
  }

  @Test
  void testDataUseFailingThePrimaryRuleIsReported() {
    List<Object> entries =
        List.of(
            Map.of("accessManagement", "open", "dataUse", Map.of("generalUse", true)),
            Map.of("dataUse", Map.of("generalUse", true, "hmbResearch", true)),
            Map.of("accessManagement", "external", "dataUse", Map.of()));

    List<String> violations = AssetDataUse.violations(StudyAssets.MODELS, entries, List.of());

    assertEquals(
        List.of(
            "models[0].dataUse: " + DataUsePrimaryValidator.VALIDATION_MESSAGE,
            "models[1].dataUse: " + DataUsePrimaryValidator.VALIDATION_MESSAGE,
            "models[2].dataUse: " + DataUsePrimaryValidator.VALIDATION_MESSAGE),
        violations);
  }

  @Test
  void testFieldThatGsonRejectsIsNamed() {
    Map<String, Object> dataUse = new LinkedHashMap<>();
    dataUse.put("generalUse", 1);
    dataUse.put("hmbResearch", true);
    dataUse.put("diseaseRestrictions", "x");

    List<String> violations =
        AssetDataUse.violations(StudyAssets.MODELS, List.of(Map.of("dataUse", dataUse)), List.of());

    assertEquals(
        List.of("models[0].dataUse: unknown or mistyped fields generalUse, diseaseRestrictions"),
        violations);
  }

  @Test
  void testEntryMatchingAStoredEntryIsNotChecked() {
    Map<String, Object> stored =
        Map.of("name", "old", "accessManagement", "Open", "dataUse", Map.of("hmb", true));
    Map<String, Object> renamed =
        Map.of("name", "new", "accessManagement", "Open", "dataUse", Map.of("hmb", true));
    Map<String, Object> changed =
        Map.of("name", "new", "accessManagement", "open", "dataUse", Map.of("hmb", true));

    List<String> violations =
        AssetDataUse.violations(StudyAssets.MODELS, List.of(renamed, changed), List.of(stored));

    assertEquals(List.of("models[1].dataUse: unknown or mistyped fields hmb"), violations);
  }

  @Test
  void testStoredPairCopiedIntoAnotherEntryIsChecked() {
    Map<String, Object> stored = Map.of("name", "old", "accessManagement", "Open");
    Map<String, Object> copy = Map.of("name", "copy", "accessManagement", "Open");

    List<String> violations =
        AssetDataUse.violations(StudyAssets.MODELS, List.of(stored, copy), List.of(stored));

    assertEquals(
        List.of("models[1].accessManagement: must be one of open, controlled, external"),
        violations);
  }

  @Test
  void testMalformedDataUseIsReported() {
    List<Object> entries =
        List.of(
            Map.of("dataUse", "HMB"),
            Map.of("dataUse", Map.of("diseaseRestrictions", "not a list")));

    List<String> violations = AssetDataUse.violations(StudyAssets.WORKSPACES, entries, List.of());

    assertEquals(
        List.of(
            "workspaces[0].dataUse: must be a data use object",
            "workspaces[1].dataUse: unknown or mistyped fields diseaseRestrictions"),
        violations);
  }

  /** Gson would coerce or skip these, so the check would pass on values that are stored as sent. */
  @Test
  void testDataUseFieldsThatDoNotReadBackExactlyAreReported() {
    Map<String, Object> mistyped = new LinkedHashMap<>();
    mistyped.put("generalUse", "yes");
    mistyped.put("hmbResearch", true);
    mistyped.put("other", true);
    mistyped.put("diseaseRestrictions", List.of(1));
    List<Object> entries =
        List.of(
            Map.of("accessManagement", "open", "dataUse", Map.of("hmb", true)),
            Map.of("dataUse", mistyped));

    List<String> violations = AssetDataUse.violations(StudyAssets.MODELS, entries, List.of());

    assertEquals(
        List.of(
            "models[0].dataUse: unknown or mistyped fields hmb",
            "models[1].dataUse: unknown or mistyped fields generalUse, other, diseaseRestrictions"),
        violations);
  }

  @Test
  void testNullDataUseFieldsAreTreatedAsAbsent() {
    Map<String, Object> dataUse = new LinkedHashMap<>();
    dataUse.put("hmbResearch", true);
    dataUse.put("generalUse", null);

    List<String> violations =
        AssetDataUse.violations(StudyAssets.MODELS, List.of(Map.of("dataUse", dataUse)), List.of());

    assertTrue(violations.isEmpty());
  }

  @Test
  void testInvalidAccessManagementSkipsThePrimaryRule() {
    List<String> violations =
        AssetDataUse.violations(
            StudyAssets.MODELS,
            List.of(Map.of("accessManagement", "public", "dataUse", Map.of("generalUse", true))),
            List.of());

    assertEquals(1, violations.size());
    assertTrue(violations.getFirst().startsWith("models[0].accessManagement"));
  }

  @Test
  void testSummaryIsDroppedAndOtherKeysKeepTheirOrder() {
    Map<String, Object> entry = new LinkedHashMap<>();
    entry.put("zeta", 1);
    entry.put("dataUseSummary", Map.of("primary", List.of()));
    entry.put("accessManagement", "open");
    entry.put("alpha", "kept");

    Map<?, ?> stored =
        (Map<?, ?>) AssetDataUse.withoutSummaries(StudyAssets.MODELS, List.of(entry)).getFirst();
    assertEquals(List.of("zeta", "accessManagement", "alpha"), List.copyOf(stored.keySet()));
    assertTrue(entry.containsKey("dataUseSummary"), "the input entry is not changed");
  }

  @Test
  void testOtherAssetTypesAndNonMapEntriesPassThrough() {
    List<Object> publications =
        List.of(Map.of("accessManagement", "nope", "dataUseSummary", "kept"));
    assertSame(publications, AssetDataUse.withoutSummaries(StudyAssets.PUBLICATIONS, publications));
    assertTrue(
        AssetDataUse.violations(StudyAssets.PUBLICATIONS, publications, List.of()).isEmpty());

    List<Object> nonMaps = List.of("a string", 42);
    assertEquals(nonMaps, AssetDataUse.withoutSummaries(StudyAssets.MODELS, nonMaps));
    assertTrue(AssetDataUse.violations(StudyAssets.MODELS, nonMaps, List.of()).isEmpty());
  }

  @Test
  void testNullListPassesThrough() {
    assertNull(AssetDataUse.withoutSummaries(StudyAssets.MODELS, null));
    assertTrue(AssetDataUse.violations(StudyAssets.MODELS, null, List.of()).isEmpty());
  }
}
