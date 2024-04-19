package org.broadinstitute.consent.http.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import jakarta.ws.rs.core.MultivaluedHashMap;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.broadinstitute.consent.http.WithMockServer;
import org.broadinstitute.consent.http.configurations.OidcConfiguration;
import org.broadinstitute.consent.http.configurations.ServicesConfiguration;
import org.broadinstitute.consent.http.models.OidcAuthorityConfiguration;
import org.broadinstitute.consent.http.util.HttpClientUtil;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockserver.client.MockServerClient;
import org.testcontainers.containers.MockServerContainer;

@ExtendWith(MockitoExtension.class)
class OidcAuthorityDAOTest implements WithMockServer {

  private OidcAuthorityDAO dao;

  private MockServerClient mockServerClient;

  private static final MockServerContainer container = new MockServerContainer(IMAGE);

  @BeforeAll
  public static void setUp() {
    container.start();
  }

  @AfterAll
  public static void tearDown() {
    container.stop();
  }

  @BeforeEach
  public void init() {
    mockServerClient = new MockServerClient(container.getHost(), container.getServerPort());
    mockServerClient.reset();
    OidcConfiguration config = new OidcConfiguration();
    config.setAuthorityEndpoint(getMockContainerBaseUrl());
    dao = new OidcAuthorityDAO(new HttpClientUtil(new ServicesConfiguration()), config);
  }

  @NotNull
  private static String getMockContainerBaseUrl() {
    return "http://" + container.getHost() + ":" + container.getServerPort();
  }

  @Test
  public void testGetOidcAuthorityConfiguration() {
    final String expectedIssuer = "https://example.com";
    final String expectedAuthorizationEndpoint = expectedIssuer + "/oauth2/authorize";
    final String expectedTokenEndpoint = expectedIssuer + "/oauth2/token";
    // the only things that matter in this body are the issuer, authorization_endpoint, and token_endpoint
    // the rest of the fields are just to simulate a real response
    final String bodyFormat = """
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

    mockServerClient
        .when(
            request()
                .withMethod("GET")
                .withPath("/.well-known/openid-configuration"))
        .respond(
            response()
                .withStatusCode(200)
                .withHeader("Content-Type", "application/json")
                .withBody(bodyFormat.formatted(expectedIssuer, expectedAuthorizationEndpoint, expectedTokenEndpoint, expectedIssuer)));
    var actual = dao.getOidcAuthorityConfiguration();
    assertEquals(expectedTokenEndpoint, actual.token_endpoint());
    assertEquals(expectedAuthorizationEndpoint, actual.authorization_endpoint());
    assertEquals(expectedIssuer, actual.issuer());
  }

  @Test
  public void testOauthTokenPost() {
    // the content of this response doesn't matter, it's just to simulate a real response
    var expectedResponse = """
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
    dao.setOidcAuthorityConfiguration(new OidcAuthorityConfiguration(null, null, getMockContainerBaseUrl() + tokenPath));
    mockServerClient
        .when(
            request()
                .withMethod("POST")
                .withPath(tokenPath)
                .withQueryStringParameters(queryParameters.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
                .withBody(URLEncodedUtils.format(formParameters.entrySet().stream().flatMap(entry -> entry.getValue().stream().map(value -> new BasicNameValuePair(entry.getKey(), value))).toList(), StandardCharsets.UTF_8))
        )
        .respond(
            response()
                .withStatusCode(200)
                .withHeader("Content-Type", "application/json")
                .withBody(
                    expectedResponse));
    var actual = dao.oauthTokenPost(formParameters, queryParameters);
    assertEquals(expectedResponse, actual);
  }
}
