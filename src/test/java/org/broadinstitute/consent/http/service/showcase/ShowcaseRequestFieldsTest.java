package org.broadinstitute.consent.http.service.showcase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class ShowcaseRequestFieldsTest {

  private static final String IDS = "identifiers";

  @Test
  void testIdentifiers() {
    assertEquals(
        List.of("DUOS-000123", "DUOS-000999"),
        identifiers(quoted("DUOS-000123") + ", " + quoted("DUOS-000999")));
  }

  @Test
  void testDuplicatesAreRemovedInFirstOccurrenceOrder() {
    assertEquals(
        List.of("DUOS-000456", "DUOS-000123"),
        identifiers("\"DUOS-000456\", \"DUOS-000123\", \"DUOS-000456\""));
  }

  @Test
  void testEmptyIdentifiers() {
    assertEquals(List.of(), identifiers(""));
  }

  @Test
  void testSevenDigitAliasIsCanonical() {
    assertEquals(List.of("DUOS-1234567"), identifiers(quoted("DUOS-1234567")));
  }

  @Test
  void testMaximumIdentifiers() {
    assertEquals(200, identifiers(distinct(200)).size());
  }

  @Test
  void testOneOverMaximumIdentifiers() {
    assertRejected(
        () -> identifiers(distinct(201)), "Field \"identifiers\" has more than 200 entries");
  }

  /** The cap counts submitted entries, so repeats that dedupe to one still count. */
  @Test
  void testMaximumRepeatedIdentifiers() {
    assertEquals(List.of("DUOS-000001"), identifiers(repeated(200)));
  }

  @Test
  void testOneOverMaximumRepeatedIdentifiers() {
    assertRejected(
        () -> identifiers(repeated(201)), "Field \"identifiers\" has more than 200 entries");
  }

  @Test
  void testMaximumIdentifierLength() {
    // 32 characters: "DUOS-" and a 27-digit alias, which is canonical but too large to parse.
    String identifier = "DUOS-1" + "0".repeat(26);
    assertEquals(32, identifier.length());

    assertRejected(
        () -> identifiers(quoted(identifier)),
        "Identifier \"" + identifier + "\" is not a DUOS identifier");
  }

  @Test
  void testOneOverMaximumIdentifierLength() {
    assertRejected(
        () -> identifiers(quoted("DUOS-000001") + ", " + quoted("DUOS-1" + "0".repeat(27))),
        "Field \"identifiers\" entry 1 is longer than 32 characters");
  }

  @ParameterizedTest
  @CsvSource({"DUOS-123, DUOS-000123", "DUOS-0000123, DUOS-000123", "DUOS-+00123, DUOS-000123"})
  void testNonCanonicalIdentifier(String identifier, String canonical) {
    assertRejected(
        () -> identifiers(quoted(identifier)),
        "Identifier \"%s\" is not canonical; use \"%s\"".formatted(identifier, canonical));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "DUOS-", "duos-000123", "DS-000123", "DUOS-00012a", "DUOS--00123"})
  void testNotAnIdentifier(String identifier) {
    assertRejected(
        () -> identifiers(quoted(identifier)),
        "Identifier \"" + identifier + "\" is not a DUOS identifier");
  }

  @ParameterizedTest
  @ValueSource(strings = {"\"DUOS-000123\", 123", "null", "[\"DUOS-000123\"]", "{}"})
  void testIdentifiersMustBeStrings(String entries) {
    assertRejected(() -> identifiers(entries), "Field \"identifiers\" must be an array of strings");
  }

  @ParameterizedTest
  @ValueSource(strings = {"\"DUOS-000123\"", "null", "{}", "1"})
  void testIdentifiersMustBeAnArray(String value) {
    assertRejected(
        () -> fields("{ \"identifiers\": %s }".formatted(value)).requiredIdentifiers(IDS, 200),
        "Field \"identifiers\" must be an array of strings");
  }

  @Test
  void testMissingField() {
    assertRejected(
        () -> fields("{}").requiredIdentifiers(IDS, 200), "Missing field \"identifiers\"");
  }

  private static List<String> identifiers(String entries) {
    return fields("{ \"identifiers\": [%s] }".formatted(entries)).requiredIdentifiers(IDS, 200);
  }

  private static ShowcaseRequestFields fields(String body) {
    return new ShowcaseRequestFields(
        ShowcaseJsonBody.read(body.getBytes(StandardCharsets.UTF_8)), "test");
  }

  private static void assertRejected(Executable read, String message) {
    assertEquals(message, assertThrows(ShowcaseLookupException.class, read).getMessage());
  }

  private static String distinct(int count) {
    return String.join(
        ", ",
        IntStream.rangeClosed(1, count).mapToObj(i -> quoted("DUOS-%06d".formatted(i))).toList());
  }

  private static String repeated(int count) {
    return String.join(", ", Collections.nCopies(count, quoted("DUOS-000001")));
  }

  private static String quoted(String value) {
    return "\"" + value + "\"";
  }
}
