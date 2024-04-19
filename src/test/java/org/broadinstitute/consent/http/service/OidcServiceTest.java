package org.broadinstitute.consent.http.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.core.MultivaluedHashMap;
import java.net.URI;
import java.net.URISyntaxException;
import org.broadinstitute.consent.http.configurations.OidcConfiguration;
import org.broadinstitute.consent.http.db.OidcAuthorityDAO;
import org.broadinstitute.consent.http.models.OidcAuthorityConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OidcServiceTest {

  @Mock private OidcAuthorityDAO mockOidcAuthorityDAO;
  OidcAuthorityConfiguration testConfig = new OidcAuthorityConfiguration(
      "http://example.com",
      "http://example.com/authorization",
      "http://example.com/token");

  @Test
  public void testGetAuthorizationURI() throws URISyntaxException {
    var configuration = new OidcConfiguration();
    var service = new OidcService(mockOidcAuthorityDAO, configuration);
    var parameters = new MultivaluedHashMap<String, String>();
    parameters.add("foo", "bar1");
    parameters.add("foo", "bar2");

    when(mockOidcAuthorityDAO.getOidcAuthorityConfiguration()).thenReturn(testConfig);
    var actual = service.getAuthorizationURI(parameters);
    assertEquals(new URI(testConfig.authorization_endpoint() + "?foo=bar1&foo=bar2"), actual);
  }

  @Test
  public void testGetAuthorizationURIWithExtraAuthParams() throws URISyntaxException {
    var configuration = new OidcConfiguration();
    configuration.setExtraAuthParams("foo1=bar1&foo2=bar2");
    var service = new OidcService(mockOidcAuthorityDAO, configuration);
    var parameters = new MultivaluedHashMap<String, String>();
    when(mockOidcAuthorityDAO.getOidcAuthorityConfiguration()).thenReturn(testConfig);
    var actual = service.getAuthorizationURI(parameters);
    assertEquals(new URI(testConfig.authorization_endpoint() + "?foo1=bar1&foo2=bar2"), actual);
  }

  @Test
  public void testGetAuthorizationURIAddsClientIdToScope() throws URISyntaxException {
    var configuration = new OidcConfiguration();
    configuration.setAddClientIdToScope(true);
    var testClientId = "test-client-id";
    configuration.setClientId(testClientId);
    var service = new OidcService(mockOidcAuthorityDAO, configuration);
    var parameters = new MultivaluedHashMap<String, String>();
    when(mockOidcAuthorityDAO.getOidcAuthorityConfiguration()).thenReturn(testConfig);
    var actual = service.getAuthorizationURI(parameters);
    assertEquals(new URI(testConfig.authorization_endpoint() + "?" + OidcService.SCOPE_PARAM + "=" + testClientId), actual);
  }

  @Test
  public void testGetAuthorizationURIAddsClientIdToExistingScope() throws URISyntaxException {
    var configuration = new OidcConfiguration();
    configuration.setAddClientIdToScope(true);
    var testClientId = "test-client-id";
    configuration.setClientId(testClientId);
    var service = new OidcService(mockOidcAuthorityDAO, configuration);
    var parameters = new MultivaluedHashMap<String, String>();
    parameters.add(OidcService.SCOPE_PARAM, "foo");
    when(mockOidcAuthorityDAO.getOidcAuthorityConfiguration()).thenReturn(testConfig);
    var actual = service.getAuthorizationURI(parameters);
    assertEquals(new URI(testConfig.authorization_endpoint() + "?" + OidcService.SCOPE_PARAM + "=foo+" + testClientId), actual);
  }

  @Test
  public void testTokenExchange() {
    var configuration = new OidcConfiguration();
    var service = new OidcService(mockOidcAuthorityDAO, configuration);
    var formParameters = new MultivaluedHashMap<String, String>();
    formParameters.add("foo1", "bar1");
    var queryParameters = new MultivaluedHashMap<String, String>();
    queryParameters.add("foo2", "bar2");
    when(mockOidcAuthorityDAO.oauthTokenPost(formParameters, queryParameters)).thenReturn("test");
    var actual = service.tokenExchange(formParameters, queryParameters);
    assertEquals("test", actual);
  }

  @Test
  public void testTokenExchangeWithClientSecret() {
    var configuration = new OidcConfiguration();
    configuration.setClientSecret("test-secret");
    var service = new OidcService(mockOidcAuthorityDAO, configuration);
    var formParameters = new MultivaluedHashMap<String, String>();
    var queryParameters = new MultivaluedHashMap<String, String>();
    var expectedFormParameters = new MultivaluedHashMap<>(formParameters);
    expectedFormParameters.add(OidcService.CLIENT_SECRET_PARAM, configuration.getClientSecret());
    when(mockOidcAuthorityDAO.oauthTokenPost(expectedFormParameters, queryParameters)).thenReturn("test");
    var actual = service.tokenExchange(formParameters, queryParameters);
    assertEquals("test", actual);
  }

  @Test
  public void testTokenExchangeWithExistingClientSecret() {
    var configuration = new OidcConfiguration();
    configuration.setClientSecret("test-secret");
    var service = new OidcService(mockOidcAuthorityDAO, configuration);
    var formParameters = new MultivaluedHashMap<String, String>();
    formParameters.add(OidcService.CLIENT_SECRET_PARAM, "existing-secret");
    var queryParameters = new MultivaluedHashMap<String, String>();
    when(mockOidcAuthorityDAO.oauthTokenPost(formParameters, queryParameters)).thenReturn("test");
    var actual = service.tokenExchange(formParameters, queryParameters);
    assertEquals("test", actual);
  }

  @Test
  public void testGetOAuth2Configuration() {
    var configuration = new OidcConfiguration();
    configuration.setClientId("test-client-id");
    var service = new OidcService(mockOidcAuthorityDAO, configuration);
    when(mockOidcAuthorityDAO.getOidcAuthorityConfiguration()).thenReturn(testConfig);
    var actual = service.getOAuth2Configuration();
    assertEquals(testConfig.authorization_endpoint(), actual.authorityEndpoint());
    assertEquals(configuration.getClientId(), actual.clientId());
  }
}
