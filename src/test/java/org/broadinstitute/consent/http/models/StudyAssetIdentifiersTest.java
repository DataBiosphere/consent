package org.broadinstitute.consent.http.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class StudyAssetIdentifiersTest {

  private static final String DUOS_IDENTIFIER = StudyAssetIdentifiers.DUOS_IDENTIFIER;

  static Stream<Arguments> decoratedLists() {
    return Stream.of(
        Arguments.of(StudyAssets.MODELS, "modelId", "DUOS-M-000045"),
        Arguments.of(StudyAssets.WORKSPACES, "workspaceId", "DUOS-W-000045"),
        Arguments.of(StudyAssets.BIOSPECIMENS, "biospecimenId", "DUOS-B-000045"));
  }

  @ParameterizedTest
  @MethodSource("decoratedLists")
  void testAnInRangeIntegerIdGetsItsIdentifier(String key, String idKey, String identifier) {
    List<Object> decorated = StudyAssetIdentifiers.decorate(key, List.of(Map.of(idKey, 45L)));

    assertEquals(List.of(Map.of(idKey, 45L, DUOS_IDENTIFIER, identifier)), decorated);
  }

  static Stream<Object> idsWithNoIdentifier() {
    return Stream.of("45", 0L, -3L, 45.5, 1_730_000_000_000L, 2_147_483_648L);
  }

  @ParameterizedTest
  @MethodSource("idsWithNoIdentifier")
  void testAnIdThatIsNotAnInRangeWholeNumberGetsNone(Object id) {
    List<Object> decorated =
        StudyAssetIdentifiers.decorate(StudyAssets.MODELS, List.of(Map.of("modelId", id)));

    assertFalse(((Map<?, ?>) decorated.getFirst()).containsKey(DUOS_IDENTIFIER));
  }

  @Test
  void testAMissingIdGetsNone() {
    List<Object> decorated =
        StudyAssetIdentifiers.decorate(StudyAssets.MODELS, List.of(Map.of("name", "A model")));

    assertEquals(List.of(Map.of("name", "A model")), decorated);
  }

  @Test
  void testAStoredIdentifierIsReplacedOnAValidIdAndDroppedWithout() {
    List<Object> decorated =
        StudyAssetIdentifiers.decorate(
            StudyAssets.WORKSPACES,
            List.of(
                Map.of("workspaceId", 12L, DUOS_IDENTIFIER, "DUOS-W-999999"),
                Map.of("workspaceId", "client-id", DUOS_IDENTIFIER, "DUOS-W-000001")));

    assertEquals(
        List.of(
            Map.of("workspaceId", 12L, DUOS_IDENTIFIER, "DUOS-W-000012"),
            Map.of("workspaceId", "client-id")),
        decorated);
  }

  @Test
  void testOtherListsAndNonObjectEntriesAreLeftAlone() {
    List<Object> publications = List.of(Map.of("publicationId", 7L, DUOS_IDENTIFIER, "kept"));

    assertSame(
        publications, StudyAssetIdentifiers.decorate(StudyAssets.PUBLICATIONS, publications));
    assertEquals(
        List.of("legacy"), StudyAssetIdentifiers.decorate(StudyAssets.MODELS, List.of("legacy")));
  }

  @Test
  void testTheEntriesItIsGivenAreNotChanged() {
    Map<String, Object> entry = new HashMap<>(Map.of("modelId", 45L, DUOS_IDENTIFIER, "stale"));

    StudyAssetIdentifiers.decorate(StudyAssets.MODELS, List.of(entry));

    assertEquals("stale", entry.get(DUOS_IDENTIFIER));
  }

  @Test
  void testAnAssetsObjectHasEachAssetListDecoratedAndTheRestKept() {
    Map<String, Object> assets =
        Map.of(
            StudyAssets.MODELS,
            List.of(Map.of("modelId", 3L)),
            StudyAssets.BIOSPECIMENS,
            List.of(Map.of("biospecimenId", 4L)),
            StudyAssets.PUBLICATIONS,
            List.of(Map.of("publicationId", "p")),
            "legacyNote",
            "kept");

    Map<String, Object> decorated = StudyAssetIdentifiers.decorate(assets);

    assertEquals(
        List.of(Map.of("modelId", 3L, DUOS_IDENTIFIER, "DUOS-M-000003")),
        decorated.get(StudyAssets.MODELS));
    assertEquals(
        List.of(Map.of("biospecimenId", 4L, DUOS_IDENTIFIER, "DUOS-B-000004")),
        decorated.get(StudyAssets.BIOSPECIMENS));
    assertEquals(assets.get(StudyAssets.PUBLICATIONS), decorated.get(StudyAssets.PUBLICATIONS));
    assertEquals("kept", decorated.get("legacyNote"));
  }

  @Test
  void testStripRemovesEveryStoredIdentifier() {
    Map<String, Object> assets =
        Map.of(
            StudyAssets.MODELS,
            List.of(Map.of("modelId", 3L, DUOS_IDENTIFIER, "DUOS-M-000003")),
            StudyAssets.WORKSPACES,
            "not a list");

    assertEquals(
        Map.of(
            StudyAssets.MODELS,
            List.of(Map.of("modelId", 3L)),
            StudyAssets.WORKSPACES,
            "not a list"),
        StudyAssetIdentifiers.strip(assets));
    assertEquals(
        List.of(Map.of("biospecimenId", 4L)),
        StudyAssetIdentifiers.strip(
            StudyAssets.BIOSPECIMENS, List.of(Map.of("biospecimenId", 4L, DUOS_IDENTIFIER, "x"))));
  }
}
