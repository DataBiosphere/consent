package org.broadinstitute.consent.http.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.broadinstitute.consent.http.models.AssetDataUse.Prepared;
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

    Prepared prepared = AssetDataUse.prepare(key, entries);

    assertTrue(prepared.violations().isEmpty());
    assertEquals(entries, prepared.stored());
    assertNotSame(entries, prepared.stored());
  }

  @Test
  void testInvalidAccessManagementIsReportedWithItsLocation() {
    List<Object> entries =
        List.of(Map.of("name", "fine"), Map.of("name", "bad", "accessManagement", "Open"));

    Prepared prepared = AssetDataUse.prepare(StudyAssets.MODELS, entries);

    assertEquals(
        List.of("models[1].accessManagement: must be one of open, controlled, external"),
        prepared.violations());
  }

  @Test
  void testNonStringAccessManagementIsReported() {
    Prepared prepared =
        AssetDataUse.prepare(StudyAssets.WORKSPACES, List.of(Map.of("accessManagement", 1)));

    assertEquals(
        List.of("workspaces[0].accessManagement: must be one of open, controlled, external"),
        prepared.violations());
  }

  @Test
  void testDataUseFailingThePrimaryRuleIsReported() {
    List<Object> entries =
        List.of(
            Map.of("accessManagement", "open", "dataUse", Map.of("generalUse", true)),
            Map.of("dataUse", Map.of("generalUse", true, "hmbResearch", true)),
            Map.of("accessManagement", "external", "dataUse", Map.of()));

    Prepared prepared = AssetDataUse.prepare(StudyAssets.MODELS, entries);

    assertEquals(
        List.of(
            "models[0].dataUse: " + DataUsePrimaryValidator.VALIDATION_MESSAGE,
            "models[1].dataUse: " + DataUsePrimaryValidator.VALIDATION_MESSAGE,
            "models[2].dataUse: " + DataUsePrimaryValidator.VALIDATION_MESSAGE),
        prepared.violations());
  }

  @Test
  void testMalformedDataUseIsReported() {
    List<Object> entries =
        List.of(
            Map.of("dataUse", "HMB"),
            Map.of("dataUse", Map.of("diseaseRestrictions", "not a list")));

    Prepared prepared = AssetDataUse.prepare(StudyAssets.WORKSPACES, entries);

    assertEquals(
        List.of(
            "workspaces[0].dataUse: must be a data use object",
            "workspaces[1].dataUse: must be a data use object"),
        prepared.violations());
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

    Prepared prepared = AssetDataUse.prepare(StudyAssets.MODELS, entries);

    assertEquals(
        List.of(
            "models[0].dataUse: unknown or mistyped fields hmb",
            "models[1].dataUse: unknown or mistyped fields generalUse, other, diseaseRestrictions"),
        prepared.violations());
  }

  @Test
  void testNullDataUseFieldsAreTreatedAsAbsent() {
    Map<String, Object> dataUse = new LinkedHashMap<>();
    dataUse.put("hmbResearch", true);
    dataUse.put("generalUse", null);

    Prepared prepared =
        AssetDataUse.prepare(StudyAssets.MODELS, List.of(Map.of("dataUse", dataUse)));

    assertTrue(prepared.violations().isEmpty());
  }

  @Test
  void testInvalidAccessManagementSkipsThePrimaryRule() {
    Prepared prepared =
        AssetDataUse.prepare(
            StudyAssets.MODELS,
            List.of(Map.of("accessManagement", "public", "dataUse", Map.of("generalUse", true))));

    assertEquals(1, prepared.violations().size());
    assertTrue(prepared.violations().getFirst().startsWith("models[0].accessManagement"));
  }

  @Test
  void testSummaryIsDroppedAndOtherKeysKeepTheirOrder() {
    Map<String, Object> entry = new LinkedHashMap<>();
    entry.put("zeta", 1);
    entry.put("dataUseSummary", Map.of("primary", List.of()));
    entry.put("accessManagement", "open");
    entry.put("alpha", "kept");

    Prepared prepared = AssetDataUse.prepare(StudyAssets.MODELS, List.of(entry));

    Map<?, ?> stored = (Map<?, ?>) prepared.stored().getFirst();
    assertEquals(List.of("zeta", "accessManagement", "alpha"), List.copyOf(stored.keySet()));
    assertTrue(entry.containsKey("dataUseSummary"), "the input entry is not changed");
  }

  @Test
  void testOtherAssetTypesAndNonMapEntriesPassThrough() {
    List<Object> publications =
        List.of(Map.of("accessManagement", "nope", "dataUseSummary", "kept"));
    Prepared other = AssetDataUse.prepare(StudyAssets.PUBLICATIONS, publications);
    assertSame(publications, other.stored());
    assertTrue(other.violations().isEmpty());

    List<Object> nonMaps = List.of("a string", 42);
    Prepared models = AssetDataUse.prepare(StudyAssets.MODELS, nonMaps);
    assertEquals(nonMaps, models.stored());
    assertTrue(models.violations().isEmpty());
  }

  @Test
  void testNullListPassesThrough() {
    Prepared prepared = AssetDataUse.prepare(StudyAssets.MODELS, null);
    assertNull(prepared.stored());
    assertTrue(prepared.violations().isEmpty());
  }
}
