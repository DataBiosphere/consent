package org.broadinstitute.consent.http.service.showcase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * The {@code stats} mode. It reads {@code scope} through the same code as the ranking mode, which
 * {@link ShowcaseLookupParserRankingTest} covers in full.
 */
class ShowcaseLookupParserStatsTest {

  @Test
  void testWholeCatalog() {
    assertEquals(new ShowcaseStatsLookup(null), parse("{ \"mode\": \"stats\" }"));
  }

  /** The duos-ui {@code lookupExamples} partner hero request. */
  @Test
  void testScope() {
    assertEquals(
        new ShowcaseStatsLookup(new ShowcaseScope(List.of("Example DAC"))),
        parse(
            "{ \"mode\": \"stats\", \"scope\": { \"dac\": [\"Example DAC\", \"Example DAC\"] } }"));
  }

  @Test
  void testEmptyDacScopeIsKept() {
    assertEquals(
        new ShowcaseStatsLookup(new ShowcaseScope(List.of())),
        parse("{ \"mode\": \"stats\", \"scope\": { \"dac\": [] } }"));
  }

  @Test
  void testScopeMustBeAnObject() {
    assertRejected("{ \"mode\": \"stats\", \"scope\": [] }", "Field \"scope\" must be an object");
  }

  @Test
  void testUnknownScopeKey() {
    assertRejected(
        "{ \"mode\": \"stats\", \"scope\": { \"study\": [\"x\"] } }",
        "Unrecognized scope key \"study\"");
  }

  /** {@code limit} gives the message in the duos-ui {@code lookupExamples} fixture. */
  @ParameterizedTest
  @ValueSource(
      strings = {
        "limit",
        "section",
        "identifiers",
        "ranking",
        "pin",
        "exclude",
        "pageSize",
        "cursor"
      })
  void testFieldNotAllowedInStatsMode(String field) {
    assertRejected(
        "{ \"mode\": \"stats\", \"%s\": 12 }".formatted(field),
        "Unrecognized field \"%s\" for mode \"stats\"".formatted(field));
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
