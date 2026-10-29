package org.broadinstitute.consent.http.service.showcase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ShowcaseJsonBodyTest {

  @Test
  void testFieldsInTheOrderSent() {
    Map<String, JsonElement> fields =
        read("{ \"mode\": \"identifiers\", \"identifiers\": [\"DUOS-000123\"], \"limit\": 12 }");

    assertEquals(List.of("mode", "identifiers", "limit"), List.copyOf(fields.keySet()));
    assertEquals(JsonParser.parseString("[\"DUOS-000123\"]"), fields.get("identifiers"));
  }

  @Test
  void testEmptyObject() {
    assertEquals(Map.of(), read("{}"));
  }

  /** The deepest valid request: a scope object holding an array of strings. */
  @Test
  void testMaximumDepth() {
    assertEquals(
        JsonParser.parseString("{ \"dac\": [\"Example DAC\"] }"),
        read("{ \"scope\": { \"dac\": [\"Example DAC\"] } }").get("scope"));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "{ \"scope\": { \"dac\": [[\"x\"]] } }",
        "{ \"scope\": { \"dac\": [{}] } }",
        "{ \"a\": [[[]]] }"
      })
  void testOneLevelTooDeep(String body) {
    assertRejected(body, ShowcaseJsonBody.TOO_DEEP);
  }

  /** Deep nesting is refused at the cap, not by running out of stack. */
  @Test
  void testVeryDeepNesting() {
    assertRejected(
        "{ \"mode\": " + "[".repeat(20_000) + "]".repeat(20_000) + " }", ShowcaseJsonBody.TOO_DEEP);
  }

  @ParameterizedTest
  @ValueSource(strings = {"mode", "section", "identifiers"})
  void testDuplicateField(String field) {
    assertRejected(
        "{ \"mode\": \"identifiers\", \"section\": \"x\", \"identifiers\": [], \"%s\": 1 }"
            .formatted(field),
        "Duplicate field \"" + field + "\"");
  }

  @Test
  void testDuplicateNestedField() {
    assertRejected(
        "{ \"scope\": { \"dac\": [\"A\"], \"dac\": [\"B\"] } }", "Duplicate field \"dac\"");
  }

  @Test
  void testLongDuplicateFieldIsCut() {
    String name = "f".repeat(60 * 1024);

    assertRejected(
        "{ \"%s\": 1, \"%s\": 2 }".formatted(name, name),
        "Duplicate field \"%s…\"".formatted("f".repeat(ShowcaseLookupException.MAX_ECHOED_LENGTH)));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "",
        "   ",
        "[]",
        "\"identifiers\"",
        "null",
        "1",
        "{",
        "{ mode: \"identifiers\" }",
        "{ 'mode': 'identifiers' }",
        "{ \"mode\": \"identifiers\", }",
        "{ \"mode\": \"identifiers\" } {}",
        "{ \"mode\": \"identifiers\" } x",
        "{ \"mode\": \"identifiers\" /* comment */ }",
        "{ \"mode\": \"identifiers\"; \"section\": \"open-access-datasets\" }",
        "{ \"mode\": NaN }",
        "{ \"mode\": \"a\tb\" }"
      })
  void testBodyMustBeOneStrictJsonObject(String body) {
    assertRejected(body, ShowcaseJsonBody.NOT_A_JSON_OBJECT);
  }

  @Test
  void testInvalidUtf8() {
    byte[] body = {'{', '"', 'm', '"', ':', '"', (byte) 0xC3, '"', '}'};

    ShowcaseLookupException exception =
        assertThrows(ShowcaseLookupException.class, () -> ShowcaseJsonBody.read(body));
    assertEquals(ShowcaseJsonBody.NOT_A_JSON_OBJECT, exception.getMessage());
  }

  /** Gson would skip it; the BFF's {@code JSON.parse} would not. */
  @ParameterizedTest
  @ValueSource(strings = {"\uFEFF{}", "\uFEFF{\"mode\": \"stats\"}"})
  void testLeadingByteOrderMark(String body) {
    assertRejected(body, ShowcaseJsonBody.NOT_A_JSON_OBJECT);
  }

  private static Map<String, JsonElement> read(String body) {
    return ShowcaseJsonBody.read(body.getBytes(StandardCharsets.UTF_8));
  }

  private static void assertRejected(String body, String message) {
    ShowcaseLookupException exception =
        assertThrows(ShowcaseLookupException.class, () -> read(body));
    assertEquals(message, exception.getMessage());
  }
}
