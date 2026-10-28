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

/** The {@code ranking} mode. The {@code scope} field follows in the next pull request. */
class ShowcaseLookupParserRankingTest {

  private static final String NEWEST = "\"mode\": \"ranking\", \"ranking\": \"newest\"";
  private static final String LATEST = NEWEST + ", \"section\": \"latest-data-releases\"";

  @Test
  void testFullRankingRequest() {
    ShowcaseLookup lookup =
        parse(
            """
            { "mode": "ranking", "section": "latest-data-releases", "ranking": "newest",
              "limit": 12,
              "pin": ["DUOS-000789"], "exclude": ["DUOS-000111", "DUOS-000111"] }
            """);

    assertEquals(
        new ShowcaseRankingLookup(
            ShowcaseSection.LATEST_DATA_RELEASES,
            ShowcaseRanking.NEWEST,
            null,
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

  @ParameterizedTest
  @ValueSource(strings = {"1", "50", "50.0", "5e1"})
  void testLimitWithinRange(String limit) {
    assertEquals((int) Double.parseDouble(limit), ranking("\"limit\": " + limit).limit());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "0",
        "51",
        "-1",
        "1.5",
        "\"12\"",
        "null",
        "true",
        "[12]",
        "1e400",
        "99999999999",
        "1e2147483648",
        "1e-2147483649"
      })
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
  @ValueSource(strings = {"identifiers", "scope", "items", "pageSize", "cursor"})
  void testFieldNotAllowedInRankingMode(String field) {
    assertRejected(
        "{ %s, \"%s\": 1 }".formatted(LATEST, field),
        "Unrecognized field \"%s\" for mode \"ranking\"".formatted(field));
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
