package org.broadinstitute.consent.http.typedsearch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Checks the request of each typed search fixture against the contract, and that the status of the
 * fixture fits the order of the checks (contract section 9): a scope that the roles cannot use gets
 * a 403 before any 400; a 400 request breaks a rule (or names a serverCheck); a 200 breaks none.
 */
class TypedSearchRequestTest {

  static Stream<String> fixtures() throws Exception {
    return TypedSearchFixtures.fixtures();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("fixtures")
  void requestFitsTheStatus(String name) throws Exception {
    JsonNode fixture = TypedSearchFixtures.read(name);
    JsonNode response = fixture.path("response");
    int status = response.path("status").asInt();
    boolean serverCheck = response.has("serverCheck");
    List<String> shape = new ArrayList<>();
    List<String> access = new ArrayList<>();
    TypedSearchRequestCheck.checkRequest(
        fixture.get("request"), TypedSearchFixtures.roles(fixture), shape, access);

    if (!access.isEmpty()) {
      assertEquals(403, status, name + ": a scope the roles cannot use gets 403 before any 400");
    } else if (status == 400) {
      assertTrue(
          !shape.isEmpty() || serverCheck,
          name + ": a 400 request must break a rule of the check, or name a serverCheck");
    } else {
      assertEquals(List.of(), shape, name + ": request");
      if (status == 403) {
        assertTrue(
            serverCheck, name + ": a 403 needs a scope the roles cannot use, or a serverCheck");
      }
    }
  }
}
