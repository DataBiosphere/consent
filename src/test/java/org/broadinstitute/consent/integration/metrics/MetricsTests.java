package org.broadinstitute.consent.integration.metrics;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.api.client.http.HttpStatusCodes;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import java.util.UUID;
import org.broadinstitute.consent.integration.ContainerTests;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class MetricsTests extends ContainerTests {

  private static final String RANGE = "?from=2026-01-01&to=2026-03-31&bucket=month";

  @BeforeAll
  static void stubSam() {
    WIRE_MOCK.stubFor(
        get(urlPathEqualTo("/api/users/v2/self/combinedState"))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withStatus(HttpStatusCodes.STATUS_CODE_OK)
                    .withBody(
                        """
                        {
                          "samUser": {"email": "ci-user@example.com", "enabled": true},
                          "termsOfServiceDetails": {"isCurrentVersion": true, "permitsSystemUsage": true}
                        }
                        """)));
  }

  @ParameterizedTest
  @ValueSource(strings = {"/api/metrics/dar-dataset-decisions"})
  void adminGetsTheReport(String path) {
    try (Response response = request(path + RANGE, "ci-admin@example.com")) {
      assertEquals(HttpStatusCodes.STATUS_CODE_OK, response.getStatus());
      String body = response.readEntity(String.class);
      assertTrue(body.contains("\"bucket\":\"MONTH\""), body);
      assertTrue(body.contains("\"total\":0"), body);
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"/api/metrics/dar-dataset-decisions"})
  void nonAdminIsForbidden(String path) {
    try (Response response = request(path + RANGE, "ci-researcher@example.com")) {
      assertEquals(HttpStatusCodes.STATUS_CODE_FORBIDDEN, response.getStatus());
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"/api/metrics/dar-dataset-decisions"})
  void missingRangeIsABadRequest(String path) {
    try (Response response = request(path, "ci-admin@example.com")) {
      assertEquals(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, response.getStatus());
    }
  }

  private static Response request(String path, String email) {
    String bearer = UUID.randomUUID().toString();
    return getClient()
        .target(serviceUrl(path))
        .request()
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearer)
        .header("OAUTH2_CLAIM_email", email)
        .header("OAUTH2_CLAIM_name", email)
        .header("OAUTH2_CLAIM_access_token", bearer)
        .header("OAUTH2_CLAIM_aud", "test-aud")
        .get();
  }
}
