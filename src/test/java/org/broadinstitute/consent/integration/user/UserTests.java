package org.broadinstitute.consent.integration.user;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.api.client.http.HttpStatusCodes;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import java.util.UUID;
import java.util.stream.Stream;
import org.broadinstitute.consent.integration.ContainerTests;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class UserTests extends ContainerTests {

  /**
   * Stub the two external services that {@code GET /api/user/me} calls through:
   *
   * <ul>
   *   <li><b>Sam</b> {@code GET /api/users/v2/self/combinedState}: returns a valid {@code
   *       CombinedState} JSON so {@link
   *       org.broadinstitute.consent.http.authentication.DuosUserAuthenticator} can build a {@code
   *       DuosUser} with a non-null {@code UserStatusInfo}.
   *   <li><b>ECM</b> {@code GET /api/oauth/v1/ras} (also matches the double-slash form produced by
   *       the config concatenation): returns 404 so {@link
   *       org.broadinstitute.consent.http.service.NihService#syncAccount} treats the user as having
   *       no NIH account and returns the user record cleanly.
   * </ul>
   *
   * Both stubs are idempotent and do not need to be reset between tests in this class.
   */
  @BeforeAll
  static void stubExternalServices() {
    // Sam – combinedState: return an enabled user who has accepted the current ToS.
    String combinedStateBody =
        """
        {
          "samUser": {
            "email": "ci-user@example.com",
            "enabled": true,
            "googleSubjectId": "ci-user-google-subject",
            "id": "ci-user-google-subject",
            "azureB2CId": null,
            "createdAt": "2024-01-01T00:00:00.000Z",
            "updatedAt": "2024-01-01T00:00:00.000Z"
          },
          "termsOfServiceDetails": {
            "acceptedOn": "2024-01-01T00:00:00.000Z",
            "isCurrentVersion": true,
            "latestAcceptedVersion": "v1",
            "permitsSystemUsage": true
          }
        }
        """;
    WIRE_MOCK.stubFor(
        get(urlPathEqualTo("/api/users/v2/self/combinedState"))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withStatus(HttpStatusCodes.STATUS_CODE_OK)
                    .withBody(combinedStateBody)));

    // ECM – RAS provider: 404 → NihService treats this as "no NIH account" and returns the user.
    // The config concatenates ecmUrl ("…9999/") + "/api/oauth/v1/ras", which can produce a
    // double slash, so the pattern matches both "/api/oauth/v1/ras" and "//api/oauth/v1/ras".
    WIRE_MOCK.stubFor(
        get(urlPathMatching("/+api/oauth/v1/ras"))
            .willReturn(aResponse().withStatus(HttpStatusCodes.STATUS_CODE_NOT_FOUND)));
  }

  static Stream<CiUser> ciUsers() {
    return CI_USERS.stream();
  }

  /**
   * Authenticates as each CI user seeded in {@link ContainerTests} and verifies that {@code GET
   * /api/user/me} returns that user's profile.
   *
   * <p>Auth is performed by including the OAUTH2_CLAIM_* headers that the app's {@link
   * org.broadinstitute.consent.http.filters.RequestHeaderCacheFilter} reads on every inbound
   * request and stores in {@link org.broadinstitute.consent.http.filters.ClaimsCache}, keyed by the
   * Bearer token value. The auth filter then resolves the token → claims → user.
   *
   * <p>The Sam {@code combinedState} stub email does not need to match the CI user email;
   * authentication resolves the DUOS user via {@code OAUTH2_CLAIM_email}, not the Sam response.
   */
  @ParameterizedTest
  @MethodSource("ciUsers")
  void testGetMeForAllCiUsers(CiUser user) {
    String bearer = UUID.randomUUID().toString();
    Response response =
        getClient()
            .target(String.format("http://localhost:%d/api/user/me", APPLICATION.getLocalPort()))
            .request()
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearer)
            .header("OAUTH2_CLAIM_email", user.email())
            .header("OAUTH2_CLAIM_name", user.displayName())
            .header("OAUTH2_CLAIM_access_token", bearer)
            .header("OAUTH2_CLAIM_aud", "test-aud")
            .get();

    assertEquals(200, response.getStatus());
    String body = response.readEntity(String.class);
    assertTrue(
        body.contains(user.email()),
        "Response body should contain %s; got: %s".formatted(user.email(), body));
  }
}
