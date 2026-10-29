package org.broadinstitute.consent.http.service.showcase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * The mode, section and field checks. {@link ShowcaseJsonBodyTest} and {@link
 * ShowcaseRequestFieldsTest} cover the body and identifier rules in full.
 */
class ShowcaseLookupParserTest {

  private static final String SECTION = "\"section\": \"open-access-datasets\"";

  @Test
  void testIdentifiersRequest() {
    ShowcaseIdentifiersLookup lookup =
        parse(
            """
            { "mode": "identifiers", "section": "open-access-datasets",
              "identifiers": ["DUOS-000123", "DUOS-000999", "DUOS-000123"] }
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

  /** The message that the duos-ui {@code lookupExamples} fixture expects. */
  @Test
  void testNonCanonicalIdentifier() {
    assertRejected(
        "{ \"mode\": \"identifiers\", %s, \"identifiers\": [\"DUOS-123\"] }".formatted(SECTION),
        "Identifier \"DUOS-123\" is not canonical; use \"DUOS-000123\"");
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

  /** A real section that the identifiers mode cannot serve says so. */
  @Test
  void testSectionThatTakesNoIdentifiers() {
    assertRejected(
        "{ \"mode\": \"identifiers\", \"section\": \"latest-data-releases\", \"identifiers\": [] }",
        "Section \"latest-data-releases\" does not take mode \"identifiers\"");
  }

  /** Section names match exactly. */
  @ParameterizedTest
  @ValueSource(strings = {"hero", "Open-Access-Datasets", ""})
  void testUnknownSection(String section) {
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

  /** The other modes arrive in later pull requests; until then they are unknown. */
  @ParameterizedTest
  @ValueSource(strings = {"ranking", "all", "stats", "IDENTIFIERS", ""})
  void testUnknownMode(String mode) {
    assertRejected("{ \"mode\": %s }".formatted(quoted(mode)), "Unknown mode \"" + mode + "\"");
  }

  /** Long caller values come back cut, so a 400 never repeats a large request. */
  @Test
  void testLongEchoedValuesAreCut() {
    String value = "v".repeat(60 * 1024);
    String cut = "v".repeat(ShowcaseLookupException.MAX_ECHOED_LENGTH) + "…";

    assertRejected("{ \"mode\": %s }".formatted(quoted(value)), "Unknown mode \"" + cut + "\"");
    assertRejected(
        "{ \"mode\": \"identifiers\", \"section\": %s, \"identifiers\": [] }"
            .formatted(quoted(value)),
        "Unknown section \"" + cut + "\" for mode \"identifiers\"");
    assertRejected(
        "{ \"mode\": \"identifiers\", %s, \"identifiers\": [], %s: 1 }"
            .formatted(SECTION, quoted(value)),
        "Unrecognized field \"" + cut + "\" for mode \"identifiers\"");
  }

  @ParameterizedTest
  @ValueSource(strings = {"1", "null", "[\"identifiers\"]", "{}", "true"})
  void testModeMustBeAString(String mode) {
    assertRejected(
        "{ \"mode\": %s, %s, \"identifiers\": [] }".formatted(mode, SECTION),
        "Field \"mode\" must be a string");
  }

  @Test
  void testBodyMustBeOneJsonObject() {
    assertRejected("[]", ShowcaseJsonBody.NOT_A_JSON_OBJECT);
  }

  private static ShowcaseIdentifiersLookup parse(String body) {
    return (ShowcaseIdentifiersLookup)
        ShowcaseLookupParser.parse(body.getBytes(StandardCharsets.UTF_8));
  }

  private static void assertRejected(String body, String message) {
    ShowcaseLookupException exception =
        assertThrows(ShowcaseLookupException.class, () -> parse(body));
    assertEquals(message, exception.getMessage());
  }

  private static String quoted(String value) {
    return "\"" + value + "\"";
  }
}
