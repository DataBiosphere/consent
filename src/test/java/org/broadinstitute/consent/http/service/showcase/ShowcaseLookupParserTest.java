package org.broadinstitute.consent.http.service.showcase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class ShowcaseLookupParserTest {

  private static final String SECTION = "\"section\": \"open-access-datasets\"";

  @Test
  void testIdentifiersRequest() {
    ShowcaseIdentifiersLookup lookup =
        parse(
            """
            { "mode": "identifiers", "section": "open-access-datasets",
              "identifiers": ["DUOS-000123", "DUOS-000999"] }
            """);

    assertEquals(ShowcaseSection.OPEN_ACCESS_DATASETS, lookup.section());
    assertEquals(List.of("DUOS-000123", "DUOS-000999"), lookup.identifiers());
  }

  @Test
  void testFieldOrderDoesNotMatter() {
    ShowcaseIdentifiersLookup lookup =
        parse(
            """
            { "identifiers": ["DUOS-000123"], "section": "open-access-datasets",
              "mode": "identifiers" }
            """);

    assertEquals(List.of("DUOS-000123"), lookup.identifiers());
  }

  @Test
  void testDuplicatesAreRemovedInFirstOccurrenceOrder() {
    ShowcaseIdentifiersLookup lookup =
        parse(identifiersRequest("\"DUOS-000456\", \"DUOS-000123\", \"DUOS-000456\""));

    assertEquals(List.of("DUOS-000456", "DUOS-000123"), lookup.identifiers());
  }

  @Test
  void testEmptyIdentifiers() {
    assertEquals(List.of(), parse(identifiersRequest("")).identifiers());
  }

  @Test
  void testSevenDigitAliasIsCanonical() {
    assertEquals(
        List.of("DUOS-1234567"), parse(identifiersRequest("\"DUOS-1234567\"")).identifiers());
  }

  @Test
  void testMaximumIdentifiers() {
    assertEquals(200, parse(identifiersRequest(distinctIdentifiers(200))).identifiers().size());
  }

  @Test
  void testOneOverMaximumIdentifiers() {
    assertRejected(
        identifiersRequest(distinctIdentifiers(201)),
        "Field \"identifiers\" has more than 200 entries");
  }

  /** The cap counts submitted entries, so repeats that dedupe to one still count. */
  @Test
  void testMaximumRepeatedIdentifiers() {
    assertEquals(
        List.of("DUOS-000001"), parse(identifiersRequest(repeatedIdentifier(200))).identifiers());
  }

  @Test
  void testOneOverMaximumRepeatedIdentifiers() {
    assertRejected(
        identifiersRequest(repeatedIdentifier(201)),
        "Field \"identifiers\" has more than 200 entries");
  }

  @Test
  void testMaximumIdentifierLength() {
    // 32 characters: "DUOS-" and a 27-digit alias, which is canonical but too large to parse.
    String identifier = "DUOS-1" + "0".repeat(26);
    assertEquals(32, identifier.length());

    assertRejected(
        identifiersRequest(quoted(identifier)),
        "Identifier \"" + identifier + "\" is not a DUOS identifier");
  }

  @Test
  void testOneOverMaximumIdentifierLength() {
    String identifier = "DUOS-1" + "0".repeat(27);

    assertRejected(
        identifiersRequest(quoted("DUOS-000001") + ", " + quoted(identifier)),
        "Identifier at index 1 is longer than 32 characters");
  }

  @ParameterizedTest
  @CsvSource({"DUOS-123, DUOS-000123", "DUOS-0000123, DUOS-000123", "DUOS-+00123, DUOS-000123"})
  void testNonCanonicalIdentifier(String identifier, String canonical) {
    assertRejected(
        identifiersRequest(quoted(identifier)),
        "Identifier \"%s\" is not canonical; use \"%s\"".formatted(identifier, canonical));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "DUOS-", "duos-000123", "DS-000123", "DUOS-00012a", "DUOS--00123"})
  void testNotAnIdentifier(String identifier) {
    assertRejected(
        identifiersRequest(quoted(identifier)),
        "Identifier \"" + identifier + "\" is not a DUOS identifier");
  }

  @ParameterizedTest
  @ValueSource(strings = {"\"DUOS-000123\", 123", "null", "[\"DUOS-000123\"]", "{}"})
  void testIdentifiersMustBeStrings(String entries) {
    assertRejected(
        identifiersRequest(entries), "Field \"identifiers\" must be an array of strings");
  }

  @ParameterizedTest
  @ValueSource(strings = {"\"DUOS-000123\"", "null", "{}", "1"})
  void testIdentifiersMustBeAnArray(String value) {
    assertRejected(
        "{ \"mode\": \"identifiers\", %s, \"identifiers\": %s }".formatted(SECTION, value),
        "Field \"identifiers\" must be an array of strings");
  }

  @Test
  void testMissingIdentifiers() {
    assertRejected(
        "{ \"mode\": \"identifiers\", %s }".formatted(SECTION), "Missing field \"identifiers\"");
  }

  @Test
  void testMissingSection() {
    assertRejected(
        "{ \"mode\": \"identifiers\", \"identifiers\": [] }", "Missing field \"section\"");
  }

  @ParameterizedTest
  @ValueSource(strings = {"latest-data-releases", "hero", "Open-Access-Datasets", ""})
  void testSectionThatTakesNoIdentifiers(String section) {
    assertRejected(
        "{ \"mode\": \"identifiers\", \"section\": %s, \"identifiers\": [] }"
            .formatted(quoted(section)),
        "Unknown section \"" + section + "\" for mode \"identifiers\"");
  }

  @Test
  void testSectionMustBeAString() {
    assertRejected(
        "{ \"mode\": \"identifiers\", \"section\": 1, \"identifiers\": [] }",
        "Field \"section\" must be a string");
  }

  @ParameterizedTest
  @ValueSource(strings = {"limit", "pin", "exclude", "scope", "ranking", "pageSize", "cursor", "x"})
  void testFieldNotAllowedInIdentifiersMode(String field) {
    assertRejected(
        "{ \"mode\": \"identifiers\", %s, \"identifiers\": [], %s: 1 }"
            .formatted(SECTION, quoted(field)),
        "Unrecognized field \"%s\" for mode \"identifiers\"".formatted(field));
  }

  @Test
  void testMissingMode() {
    assertRejected("{ %s, \"identifiers\": [] }".formatted(SECTION), "Missing field \"mode\"");
  }

  /** Ranking, all and stats arrive in later pull requests; until then they are unknown. */
  @ParameterizedTest
  @ValueSource(strings = {"ranking", "all", "stats", "IDENTIFIERS", ""})
  void testUnknownMode(String mode) {
    assertRejected("{ \"mode\": %s }".formatted(quoted(mode)), "Unknown mode \"" + mode + "\"");
  }

  @ParameterizedTest
  @ValueSource(strings = {"1", "null", "[\"identifiers\"]", "{}", "true"})
  void testModeMustBeAString(String mode) {
    assertRejected(
        "{ \"mode\": %s, %s, \"identifiers\": [] }".formatted(mode, SECTION),
        "Field \"mode\" must be a string");
  }

  @ParameterizedTest
  @ValueSource(strings = {"mode", "section", "identifiers"})
  void testDuplicateField(String field) {
    String request =
        "{ \"mode\": \"identifiers\", %s, \"identifiers\": [], %s: \"identifiers\" }"
            .formatted(SECTION, quoted(field));

    assertRejected(request, "Duplicate field \"" + field + "\"");
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "",
        "   ",
        "[]",
        "\"identifiers\"",
        "null",
        "{",
        "{ mode: \"identifiers\" }",
        "{ 'mode': 'identifiers' }",
        "{ \"mode\": \"identifiers\", }",
        "{ \"mode\": \"identifiers\" } {}",
        "{ \"mode\": \"identifiers\" } x",
        "{ \"mode\": \"identifiers\" /* comment */ }",
        "{ \"mode\": \"identifiers\"; \"section\": \"open-access-datasets\" }"
      })
  void testBodyMustBeOneStrictJsonObject(String body) {
    assertRejected(body, ShowcaseLookupParser.NOT_A_JSON_OBJECT);
  }

  @Test
  void testInvalidUtf8() {
    byte[] body = {'{', '"', 'm', '"', ':', '"', (byte) 0xC3, '"', '}'};

    ShowcaseLookupException exception =
        assertThrows(ShowcaseLookupException.class, () -> ShowcaseLookupParser.parse(body));
    assertEquals(ShowcaseLookupParser.NOT_A_JSON_OBJECT, exception.getMessage());
  }

  private static ShowcaseIdentifiersLookup parse(String body) {
    return ShowcaseLookupParser.parse(body.getBytes(StandardCharsets.UTF_8));
  }

  private static void assertRejected(String body, String message) {
    ShowcaseLookupException exception =
        assertThrows(ShowcaseLookupException.class, () -> parse(body));
    assertEquals(message, exception.getMessage());
  }

  private static String identifiersRequest(String entries) {
    return "{ \"mode\": \"identifiers\", %s, \"identifiers\": [%s] }".formatted(SECTION, entries);
  }

  private static String distinctIdentifiers(int count) {
    return String.join(
        ", ",
        IntStream.rangeClosed(1, count).mapToObj(i -> quoted("DUOS-%06d".formatted(i))).toList());
  }

  private static String repeatedIdentifier(int count) {
    return String.join(", ", Collections.nCopies(count, quoted("DUOS-000001")));
  }

  private static String quoted(String value) {
    return "\"" + value + "\"";
  }
}
