package org.broadinstitute.consent.http.service.showcase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** The {@code all} mode and its cursor. */
class ShowcaseLookupParserAllTest {

  @Test
  void testAllDefaults() {
    assertEquals(
        new ShowcaseAllLookup(ShowcaseLookupLimits.DEFAULT_PAGE_SIZE, null),
        parse("{ \"mode\": \"all\" }"));
  }

  @Test
  void testAllWithCursor() {
    String cursor = ShowcaseCursor.encode(123);

    assertEquals(
        new ShowcaseAllLookup(200, 123),
        parse("{ \"mode\": \"all\", \"pageSize\": 200, \"cursor\": \"%s\" }".formatted(cursor)));
  }

  @ParameterizedTest
  @ValueSource(strings = {"0", "201", "2.5", "\"100\""})
  void testPageSizeOutOfRange(String pageSize) {
    assertRejected(
        "{ \"mode\": \"all\", \"pageSize\": %s }".formatted(pageSize),
        "Field \"pageSize\" must be an integer from 1 to 200");
  }

  @Test
  void testMaximumCursorLengthIsRead() {
    // A 512-character cursor is read, and then refused because it is not one Consent made.
    assertRejected(
        "{ \"mode\": \"all\", \"cursor\": \"%s\" }".formatted("A".repeat(512)),
        "Field \"cursor\" is not a valid cursor");
  }

  @Test
  void testOneOverMaximumCursorLength() {
    assertRejected(
        "{ \"mode\": \"all\", \"cursor\": \"%s\" }".formatted("A".repeat(513)),
        "Field \"cursor\" is longer than 512 characters");
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "not base64!", "djE6YWxsOi0x", "djE6cmFua2luZzox", "djI6YWxsOjE"})
  void testMalformedOrMismatchedCursor(String cursor) {
    // djE6YWxsOi0x is v1:all:-1, djE6cmFua2luZzox is v1:ranking:1, djI6YWxsOjE is v2:all:1.
    assertRejected(
        "{ \"mode\": \"all\", \"cursor\": \"%s\" }".formatted(cursor),
        "Field \"cursor\" is not a valid cursor");
  }

  @Test
  void testCursorMustBeAString() {
    assertRejected("{ \"mode\": \"all\", \"cursor\": 1 }", "Field \"cursor\" must be a string");
  }

  @ParameterizedTest
  @ValueSource(strings = {"section", "ranking", "scope", "limit", "identifiers"})
  void testFieldNotAllowedInAllMode(String field) {
    assertRejected(
        "{ \"mode\": \"all\", \"%s\": 1 }".formatted(field),
        "Unrecognized field \"%s\" for mode \"all\"".formatted(field));
  }

  @Test
  void testCursorRoundTrip() {
    assertEquals(0, ShowcaseCursor.decode(ShowcaseCursor.encode(0)));
    assertEquals(
        Integer.MAX_VALUE, ShowcaseCursor.decode(ShowcaseCursor.encode(Integer.MAX_VALUE)));
    assertNull(((ShowcaseAllLookup) parse("{ \"mode\": \"all\", \"pageSize\": 1 }")).afterAlias());
  }

  private static ShowcaseLookup parse(String body) {
    return ShowcaseLookupParser.parse(body.getBytes(StandardCharsets.UTF_8));
  }

  private static void assertRejected(String body, String message) {
    ShowcaseLookupException exception =
        assertThrows(ShowcaseLookupException.class, () -> parse(body));
    assertEquals(message, exception.getMessage());
  }
}
