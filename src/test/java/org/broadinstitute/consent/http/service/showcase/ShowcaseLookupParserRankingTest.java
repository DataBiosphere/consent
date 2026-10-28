package org.broadinstitute.consent.http.service.showcase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** The {@code ranking} mode. */
class ShowcaseLookupParserRankingTest {

  private static final String NEWEST = "\"mode\": \"ranking\", \"ranking\": \"newest\"";
  private static final String LATEST = NEWEST + ", \"section\": \"latest-data-releases\"";

  @Test
  void testFullRankingRequest() {
    ShowcaseLookup lookup =
        parse(
            """
            { "mode": "ranking", "section": "latest-data-releases", "ranking": "newest",
              "scope": { "dac": ["Example DAC", "Example DAC"] }, "limit": 12,
              "pin": ["DUOS-000789"], "exclude": ["DUOS-000111", "DUOS-000111"] }
            """);

    assertEquals(
        new ShowcaseRankingLookup(
            ShowcaseSection.LATEST_DATA_RELEASES,
            ShowcaseRanking.NEWEST,
            new ShowcaseScope(List.of("Example DAC")),
            12,
            List.of("DUOS-000789"),
            List.of("DUOS-000111")),
        lookup);
  }

  @Test
  void testRankingDefaults() {
    assertEquals(
        new ShowcaseRankingLookup(
            ShowcaseSection.OPEN_ACCESS_DATASETS,
            ShowcaseRanking.NEWEST,
            null,
            ShowcaseLookupLimits.DEFAULT_RANKING_LIMIT,
            List.of(),
            List.of()),
        parse("{ %s, \"section\": \"open-access-datasets\" }".formatted(NEWEST)));
  }

  @Test
  void testScopeWithoutKeysHasNoDacFilter() {
    assertEquals(new ShowcaseScope(null), ranking("\"scope\": {}").scope());
  }

  @Test
  void testEmptyDacScopeIsKept() {
    assertEquals(new ShowcaseScope(List.of()), ranking("\"scope\": { \"dac\": [] }").scope());
  }

  @ParameterizedTest
  @ValueSource(strings = {"1", "50", "50.0", "5e1"})
  void testLimitWithinRange(String limit) {
    assertEquals((int) Double.parseDouble(limit), ranking("\"limit\": " + limit).limit());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {"0", "51", "-1", "1.5", "\"12\"", "null", "true", "[12]", "1e400", "99999999999"})
  void testLimitOutOfRange(String limit) {
    assertRejected(
        "{ %s, \"limit\": %s }".formatted(LATEST, limit),
        "Field \"limit\" must be an integer from 1 to 50");
  }

  @Test
  void testMaximumPins() {
    assertEquals(50, ranking("\"pin\": [%s]".formatted(distinct(50))).pins().size());
  }

  @Test
  void testOneOverMaximumPins() {
    assertRejected(
        "{ %s, \"pin\": [%s] }".formatted(LATEST, distinct(51)),
        "Field \"pin\" has more than 50 entries");
  }

  @Test
  void testMaximumExclusions() {
    assertEquals(
        List.of("DUOS-000001"), ranking("\"exclude\": [%s]".formatted(repeated(200))).exclusions());
  }

  @Test
  void testOneOverMaximumExclusions() {
    assertRejected(
        "{ %s, \"exclude\": [%s] }".formatted(LATEST, distinct(201)),
        "Field \"exclude\" has more than 200 entries");
  }

  @Test
  void testPinIdentifiersMustBeCanonical() {
    assertRejected(
        "{ %s, \"pin\": [\"DUOS-789\"] }".formatted(LATEST),
        "Identifier \"DUOS-789\" is not canonical; use \"DUOS-000789\"");
  }

  @Test
  void testMaximumScopeValues() {
    String names =
        String.join(", ", IntStream.range(0, 20).mapToObj(i -> "\"DAC " + i + "\"").toList());
    assertEquals(
        20, ranking("\"scope\": { \"dac\": [%s] }".formatted(names)).scope().dacNames().size());
  }

  @Test
  void testOneOverMaximumScopeValues() {
    String names = String.join(", ", Collections.nCopies(21, "\"Example DAC\""));
    assertRejected(
        "{ %s, \"scope\": { \"dac\": [%s] } }".formatted(LATEST, names),
        "Field \"scope.dac\" has more than 20 entries");
  }

  @ParameterizedTest
  @ValueSource(strings = {"\"dac\"", "[]", "null", "1"})
  void testScopeMustBeAnObject(String scope) {
    assertRejected(
        "{ %s, \"scope\": %s }".formatted(LATEST, scope), "Field \"scope\" must be an object");
  }

  @ParameterizedTest
  @ValueSource(strings = {"study", "institution", "DAC", "$where"})
  void testUnknownScopeKey(String key) {
    assertRejected(
        "{ %s, \"scope\": { \"%s\": [\"x\"] } }".formatted(LATEST, key),
        "Unrecognized scope key \"" + key + "\"");
  }

  /** A nested query object, or a value that is not a string, is not a DAC name. */
  @ParameterizedTest
  @ValueSource(strings = {"\"Example DAC\"", "{ \"$ne\": \"\" }", "[1]", "[null]", "[true]"})
  void testDacScopeMustBeStrings(String value) {
    assertRejected(
        "{ %s, \"scope\": { \"dac\": %s } }".formatted(LATEST, value),
        "Field \"scope.dac\" must be an array of strings");
  }

  @Test
  void testDuplicateScopeKey() {
    assertRejected(
        "{ %s, \"scope\": { \"dac\": [\"A\"], \"dac\": [\"B\"] } }".formatted(LATEST),
        "Duplicate field \"dac\"");
  }

  @ParameterizedTest
  @ValueSource(strings = {"most-requested-datasets", "hero", ""})
  void testUnknownRankingSection(String section) {
    assertRejected(
        "{ %s, \"section\": \"%s\" }".formatted(NEWEST, section),
        "Unknown section \"" + section + "\" for mode \"ranking\"");
  }

  @ParameterizedTest
  @ValueSource(strings = {"most-requested", "largest-cohort", "Newest", ""})
  void testUnknownRanking(String ranking) {
    assertRejected(
        "{ \"mode\": \"ranking\", \"section\": \"latest-data-releases\", \"ranking\": \"%s\" }"
            .formatted(ranking),
        "Unknown ranking \"" + ranking + "\" for section \"latest-data-releases\"");
  }

  @Test
  void testMissingRanking() {
    assertRejected(
        "{ \"mode\": \"ranking\", \"section\": \"latest-data-releases\" }",
        "Missing field \"ranking\"");
  }

  @ParameterizedTest
  @ValueSource(strings = {"identifiers", "items", "pageSize", "cursor"})
  void testFieldNotAllowedInRankingMode(String field) {
    assertRejected(
        "{ %s, \"%s\": 1 }".formatted(LATEST, field),
        "Unrecognized field \"%s\" for mode \"ranking\"".formatted(field));
  }

  @Test
  void testNestingCap() {
    assertRejected(
        "{ %s, \"scope\": { \"dac\": [[\"x\"]] } }".formatted(LATEST), ShowcaseJsonBody.TOO_DEEP);
  }

  private static ShowcaseRankingLookup ranking(String fields) {
    return (ShowcaseRankingLookup) parse("{ %s, %s }".formatted(LATEST, fields));
  }

  private static ShowcaseLookup parse(String body) {
    return ShowcaseLookupParser.parse(body.getBytes(StandardCharsets.UTF_8));
  }

  private static void assertRejected(String body, String message) {
    ShowcaseLookupException exception =
        assertThrows(ShowcaseLookupException.class, () -> parse(body));
    assertEquals(message, exception.getMessage());
  }

  private static String distinct(int count) {
    return String.join(
        ", ", IntStream.rangeClosed(1, count).mapToObj(i -> "\"DUOS-%06d\"".formatted(i)).toList());
  }

  private static String repeated(int count) {
    return String.join(", ", Collections.nCopies(count, "\"DUOS-000001\""));
  }
}
