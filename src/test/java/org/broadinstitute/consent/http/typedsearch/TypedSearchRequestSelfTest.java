package org.broadinstitute.consent.http.typedsearch;

import static java.util.stream.Collectors.joining;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Checks that {@link TypedSearchRequestCheck} rejects each rule of the contract that it claims to.
 */
class TypedSearchRequestSelfTest {

  @Test
  void checkAcceptsAnOffsetPastTheLargestInt() throws Exception {
    List<String> shape = new ArrayList<>();
    TypedSearchRequestCheck.checkRequest(
        TypedSearchFixtures.MAPPER.readTree(
            "{\"view\":\"datasets\",\"scope\":{\"library\":null},"
                + "\"page\":{\"offset\":3000000000,\"limit\":10}}"),
        Set.of("RESEARCHER"),
        shape,
        new ArrayList<>());
    assertEquals(List.of(), shape);
  }

  /**
   * Requests that each break one rule of the contract, so the check must reject them: those in
   * {@code self-test/requests.json}, and requests at each limit of contract section 6 plus one.
   */
  static Stream<JsonNode> requestsThatBreakTheContract() throws Exception {
    List<JsonNode> requests = new ArrayList<>();
    TypedSearchFixtures.read("self-test/requests.json").forEach(requests::add);
    String library = "{\"view\":\"datasets\",\"scope\":{\"library\":null},";
    String ids = IntStream.rangeClosed(1, 10_001).mapToObj(String::valueOf).collect(joining(","));
    String x = "\"" + "x".repeat(1_001) + "\"";
    for (String json :
        List.of(
            "{\"view\":\"datasets\",\"scope\":{\"datasetIds\":[" + ids + "]}}",
            library
                + "\"filters\":["
                + times("{\"key\":\"participantCount\",\"gte\":1}", 1_025)
                + "]}",
            library + "\"filters\":[{\"key\":\"dac\",\"values\":[" + times("\"x\"", 1_025) + "]}]}",
            library + "\"search\":" + x + "}",
            library + "\"filters\":[{\"key\":\"dac\",\"values\":[" + x + "]}]}")) {
      requests.add(TypedSearchFixtures.MAPPER.readTree(json));
    }
    return requests.stream();
  }

  private static String times(String json, int count) {
    return String.join(",", Collections.nCopies(count, json));
  }

  @ParameterizedTest
  @MethodSource("requestsThatBreakTheContract")
  void checkRejectsARequestThatBreaksTheContract(JsonNode request) throws Exception {
    List<String> shape = new ArrayList<>();
    TypedSearchRequestCheck.checkRequest(request, Set.of("ADMIN"), shape, new ArrayList<>());
    String text = request.toString();
    assertFalse(shape.isEmpty(), text.substring(0, Math.min(200, text.length())));
  }
}
