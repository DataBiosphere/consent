package org.broadinstitute.consent.http.typedsearch;

import static org.broadinstitute.consent.http.typedsearch.TypedSearchFixtures.fields;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.broadinstitute.consent.http.enumeration.UserRoles;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Checks the format of each typed search fixture ({@code README.md} in the fixture folder): the
 * fields, the source document, the user, the outcome tags, and the response envelope. The request
 * and the response body are checked against the contract by their own tests.
 */
class TypedSearchFixtureFormatTest {

  private static final Set<Integer> ERROR_STATUSES = Set.of(400, 403, 404, 413, 429, 500, 504);
  private static final Set<String> QUESTIONS = Set.of("Q6", "Q12", "Q13", "Q15");

  static Stream<String> fixtures() throws Exception {
    return TypedSearchFixtures.fixtures();
  }

  @Test
  void registryHasOnlyTheContractViews() {
    assertFalse(TypedSearchFixtures.registry().isEmpty());
    for (String view : TypedSearchFixtures.registry().keySet()) {
      assertTrue(Set.of("datasets", "studies", "assets", "counts").contains(view), view);
    }
  }

  @Test
  void fixturesExist() throws Exception {
    assertFalse(fixtures().toList().isEmpty());
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("fixtures")
  void fixtureHasTheFormat(String name) throws Exception {
    JsonNode fixture = TypedSearchFixtures.read(name);
    List<String> errors = new ArrayList<>();
    fields(
        fixture,
        Set.of("example", "source", "user", "outcome", "request", "response"),
        Set.of("example", "source", "user", "request", "response"),
        "fixture",
        errors);
    if (!fixture.path("source").asText().startsWith("docs/plans/typed-search-")) {
      errors.add("source must name a typed search document");
    }

    fields(fixture.path("user"), Set.of("roles", "relation"), Set.of("roles"), "user", errors);
    Set<String> roles = TypedSearchFixtures.roles(fixture);
    if (roles.isEmpty()) {
      errors.add("user.roles must have at least one role");
    }
    for (String role : roles) {
      try {
        UserRoles.valueOf(role);
      } catch (IllegalArgumentException e) {
        errors.add("unknown role: " + role);
      }
    }

    JsonNode outcome = fixture.path("outcome");
    boolean tagged = outcome.isObject() && !outcome.isEmpty();
    if (tagged != name.startsWith("open-questions") || fixture.has("outcome") != tagged) {
      errors.add("outcome is a non-empty object in open-questions/, and only there");
    }
    outcome
        .properties()
        .forEach(
            e -> {
              if (!QUESTIONS.contains(e.getKey())
                  || !Set.of("A", "B", "AB").contains(e.getValue().asText())) {
                errors.add("outcome must map Q6, Q12, Q13, or Q15 to A, B, or AB");
              }
            });

    JsonNode response = fixture.path("response");
    int status = response.path("status").asInt();
    if (status == 200) {
      fields(response, Set.of("status", "body"), Set.of("status", "body"), "response", errors);
    } else if (ERROR_STATUSES.contains(status)) {
      fields(
          response, Set.of("status", "body", "serverCheck"), Set.of("status"), "response", errors);
      JsonNode body = response.path("body");
      boolean errorBody =
          body.size() == 2 && body.path("message").isTextual() && body.path("code").isInt();
      if (status == 429
          ? response.has("body")
          : !errorBody || body.path("code").asInt() != status) {
        errors.add("an error body is { message, code: the status }; a 429 has none (section 9)");
      }
      if (response.has("serverCheck") && !response.get("serverCheck").isTextual()) {
        errors.add("serverCheck must be text: the reason for the error");
      }
      if (status == 404 && !response.has("serverCheck")) {
        errors.add("a 404 needs a serverCheck: the test cannot tell that a target is missing");
      }
    } else {
      errors.add("response.status must be 200 or a status of the contract: " + status);
    }
    assertEquals(List.of(), errors, name);
  }
}
