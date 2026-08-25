package org.broadinstitute.consent.http.models.datause;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Stream;
import org.broadinstitute.consent.http.models.DataUseBuilder;
import org.broadinstitute.consent.http.models.datause.PersistedDataUseClassification.State;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class PersistedDataUseClassifierTest {

  @Test
  void classifyAbsentValueAsNull() {
    assertEquals(State.NULL, PersistedDataUseClassifier.classify((String) null).state());
  }

  @ParameterizedTest
  @ValueSource(strings = {"", " ", "\t"})
  void classifyBlankValueAsEmpty(String rawDataUse) {
    assertEquals(State.EMPTY, PersistedDataUseClassifier.classify(rawDataUse).state());
  }

  @ParameterizedTest
  @ValueSource(strings = {"{", "not json", "{\"generalUse\":", "[]"})
  void classifyMalformedValueAsUnparseable(String rawDataUse) {
    assertEquals(State.UNPARSEABLE, PersistedDataUseClassifier.classify(rawDataUse).state());
  }

  /** A JSON null literal parses cleanly but carries no data use, so it counts with the nulls. */
  @Test
  void classifyJsonNullLiteralAsNull() {
    assertEquals(State.NULL, PersistedDataUseClassifier.classify("null").state());
  }

  @Test
  void classifyValueWithNoPrimaryAsNone() {
    assertEquals(State.NONE, PersistedDataUseClassifier.classify("{}").state());
  }

  /** Matches DataUseMatcherV5: a secondary Other is not a primary category. */
  @Test
  void classifySecondaryOtherOnlyAsNone() {
    var classification =
        PersistedDataUseClassifier.classify(
            new DataUseBuilder().setSecondaryOther("secondary text").build());

    assertEquals(State.NONE, classification.state());
    assertEquals(List.of(), classification.categories());
  }

  @ParameterizedTest
  @MethodSource("singlePrimaryValues")
  void classifyEverySinglePrimary(String rawDataUse, DataUsePrimaryCategory expected) {
    var classification = PersistedDataUseClassifier.classify(rawDataUse);

    assertEquals(State.SINGLE, classification.state());
    assertEquals(List.of(expected), classification.categories());
  }

  static Stream<Arguments> singlePrimaryValues() {
    return Stream.of(
        Arguments.of("{\"generalUse\":true}", DataUsePrimaryCategory.GRU),
        Arguments.of("{\"hmbResearch\":true}", DataUsePrimaryCategory.HMB),
        Arguments.of("{\"populationOriginsAncestry\":true}", DataUsePrimaryCategory.POA),
        Arguments.of("{\"diseaseRestrictions\":[\"DOID:1\"]}", DataUsePrimaryCategory.DS),
        Arguments.of("{\"other\":\"bespoke restriction\"}", DataUsePrimaryCategory.OTHER));
  }

  @Test
  void classifyMultiplePrimaries() {
    var classification =
        PersistedDataUseClassifier.classify("{\"hmbResearch\":true,\"other\":\"not for profit\"}");

    assertEquals(State.MULTIPLE, classification.state());
    assertEquals(
        List.of(DataUsePrimaryCategory.HMB, DataUsePrimaryCategory.OTHER),
        classification.categories());
  }

  @Test
  void labelNamesCategoriesInCanonicalOrderRegardlessOfJsonOrder() {
    String hmbFirst = "{\"hmbResearch\":true,\"generalUse\":true}";
    String gruFirst = "{\"generalUse\":true,\"hmbResearch\":true}";

    assertEquals("MULTIPLE(GRU,HMB)", PersistedDataUseClassifier.classify(hmbFirst).label());
    assertEquals("MULTIPLE(GRU,HMB)", PersistedDataUseClassifier.classify(gruFirst).label());
  }

  @ParameterizedTest
  @MethodSource("labelCases")
  void labelIsStableAndRedacted(String rawDataUse, String expectedLabel) {
    assertEquals(expectedLabel, PersistedDataUseClassifier.classify(rawDataUse).label());
  }

  static Stream<Arguments> labelCases() {
    return Stream.of(
        Arguments.of(null, "NULL"),
        Arguments.of("", "EMPTY"),
        Arguments.of("{", "UNPARSEABLE"),
        Arguments.of("{}", "NONE"),
        Arguments.of("{\"generalUse\":true}", "SINGLE(GRU)"),
        Arguments.of("{\"other\":\"bespoke\"}", "SINGLE(OTHER)"));
  }

  /** The label reaches reports and logs, so it must never carry the Other free text. */
  @Test
  void labelOmitsOtherFreeText() {
    String secret = "participants from the Example Cohort may not be re-identified";

    String label =
        PersistedDataUseClassifier.classify("{\"other\":\"%s\"}".formatted(secret)).label();

    assertEquals("SINGLE(OTHER)", label);
    assertFalse(label.contains(secret));
  }

  @ParameterizedTest
  @MethodSource("canonicalShapeCases")
  void canonicalShapeDependsOnAccessManagement(
      String rawDataUse, boolean openAccess, boolean expectedCanonical) {
    var classification = PersistedDataUseClassifier.classify(rawDataUse);

    assertEquals(expectedCanonical, classification.isCanonicalFor(openAccess));
  }

  static Stream<Arguments> canonicalShapeCases() {
    return Stream.of(
        // Open access requires no primary
        Arguments.of("{}", true, true),
        Arguments.of("{\"generalUse\":true}", true, false),
        // Everything else requires exactly one
        Arguments.of("{\"generalUse\":true}", false, true),
        Arguments.of("{\"other\":\"bespoke\"}", false, true),
        Arguments.of("{}", false, false),
        Arguments.of("{\"generalUse\":true,\"hmbResearch\":true}", false, false),
        Arguments.of(null, false, false),
        Arguments.of("{", false, false));
  }

  /** An Other-only primary is a valid shape to persist even though V5 abstains when matching it. */
  @Test
  void otherOnlyPrimaryIsCanonicalButAbstains() {
    var classification = PersistedDataUseClassifier.classify("{\"other\":\"bespoke\"}");

    assertTrue(classification.isCanonicalFor(false));
    assertEquals(State.SINGLE, classification.state());
  }
}
