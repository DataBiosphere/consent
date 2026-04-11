package org.broadinstitute.consent.http.db;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import jakarta.ws.rs.core.MultivaluedHashMap;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.broadinstitute.consent.http.WireMockTestHelper;
import org.broadinstitute.consent.http.configurations.OidcConfiguration;
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
import org.broadinstitute.consent.http.models.OidcAuthorityConfiguration;
import org.broadinstitute.consent.http.util.HttpClientUtil;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OidcAuthorityDAOTest extends WireMockTestHelper {

  private OidcAuthorityDAO dao;

  @NotNull
  private static String getMockContainerBaseUrl() {
    return mockServerBaseUrl();
  }

  @BeforeEach
  void init() {
    OidcConfiguration config = new OidcConfiguration();
    config.setAuthorityEndpoint(getMockContainerBaseUrl());
    dao = new OidcAuthorityDAO(new HttpClientUtil(new ServicesConfiguration()), config);
  }

  @Test
  void testGetOidcAuthorityConfiguration() {
    final String expectedIssuer = "https://example.com";
    final String expectedAuthorizationEndpoint = expectedIssuer + "/oauth2/authorize";
    final String expectedTokenEndpoint = expectedIssuer + "/oauth2/token";
    // the only things that matter in this body are the issuer, authorization_endpoint, and
    // token_endpoint
    // the rest of the fields are just to simulate a real response
    final String bodyFormat =
        """
        {
          "issuer":"%s",
          "authorization_endpoint":"%s",
          "token_endpoint":"%s",
          "userinfo_endpoint":"%s/oauth2/userinfo",
          "revocation_endpoint":"https://example.com/oauth2/revoke",
          "jwks_uri":"https://example.com/oauth2/keys",
          "response_types_supported":["code","token","id_token","code token","code id_token","token id_token","code token id_token","none"],
          "subject_types_supported":["public"],
          "id_token_signing_alg_values_supported":["RS256"],
          "scopes_supported":["openid","profile","email","address","phone"],
          "token_endpoint_auth_methods_supported":["client_secret_basic","client_secret_post"],
          "claims_supported":["sub","iss","email","email_verified","phone_number","phone_number_verified","address","name","client_id"],
          "code_challenge_methods_supported":["plain","S256"]
        }
        """;

    wireMockServer.stubFor(
        get(urlPathEqualTo("/.well-known/openid-configuration"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        bodyFormat.formatted(
                            expectedIssuer,
                            expectedAuthorizationEndpoint,
                            expectedTokenEndpoint,
                            expectedIssuer))));
    var actual = dao.getOidcAuthorityConfiguration();
    assertEquals(expectedTokenEndpoint, actual.token_endpoint());
    assertEquals(expectedAuthorizationEndpoint, actual.authorization_endpoint());
    assertEquals(expectedIssuer, actual.issuer());
  }

  @Test
  void testOauthTokenPost() {
    // the content of this response doesn't matter, it's just to simulate a real response
    var expectedResponse =
        """
        {
          "access_token":"1234567890",
          "token_type":"Bearer",
          "expires_in":3600,
          "refresh_token":"0987654321",
          "id_token":"1234567890"
        }
        """;
    var formParameters = new MultivaluedHashMap<>(Map.of("formParam", "formValue"));
    var queryParameters = new MultivaluedHashMap<>(Map.of("queryParam", "queryValue"));
    var tokenPath = "/oauth2/token";
    dao.setOidcAuthorityConfiguration(
        new OidcAuthorityConfiguration(null, null, getMockContainerBaseUrl() + tokenPath));

    String expectedFormBody =
        URLEncodedUtils.format(
            formParameters.entrySet().stream()
                .flatMap(
                    entry ->
                        entry.getValue().stream()
                            .map(value -> new BasicNameValuePair(entry.getKey(), value)))
                .toList(),
            StandardCharsets.UTF_8);

    MappingBuilder mapping =
        post(urlPathEqualTo(tokenPath)).withRequestBody(equalTo(expectedFormBody));
    for (var entry : queryParameters.entrySet()) {
      for (String value : entry.getValue()) {
        mapping = mapping.withQueryParam(entry.getKey(), equalTo(value));
      }
    }
    wireMockServer.stubFor(
        mapping.willReturn(
            aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(expectedResponse)));
    var actual = dao.oauthTokenPost(formParameters, queryParameters);
    assertEquals(expectedResponse, actual);
  }
}
