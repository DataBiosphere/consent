package org.broadinstitute.consent.http.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.UriInfo;
import java.net.URI;
import java.net.URISyntaxException;
import org.broadinstitute.consent.http.models.OAuth2Configuration;
import org.broadinstitute.consent.http.service.OidcService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OAuth2ResourceTest {
  @Mock private OidcService mockOidcService;
  @Mock private UriInfo mockUriInfo;

  @Test
  void testGetOAuth2Redirect() throws URISyntaxException {
    var queryParameters = new MultivaluedHashMap<String, String>();
    when(mockUriInfo.getQueryParameters()).thenReturn(queryParameters);
    when(mockOidcService.getAuthorizationURI(queryParameters)).thenReturn(new URI("http://example.com"));
    OAuth2Resource resource = new OAuth2Resource(mockOidcService);
    var response = resource.getAuthorizationEndpoint(mockUriInfo);
    assertEquals(302, response.getStatus());
    assertEquals("http://example.com", response.getLocation().toString());
  }

  @Test
  void testGetToken() {
    var queryParameters = new MultivaluedHashMap<String, String>();
    var bodyParams = new MultivaluedHashMap<String, String>();
    when(mockUriInfo.getQueryParameters()).thenReturn(queryParameters);
    when(mockOidcService.tokenExchange(bodyParams, queryParameters)).thenReturn("token");
    OAuth2Resource resource = new OAuth2Resource(mockOidcService);
    var response = resource.getToken(mockUriInfo, bodyParams);
    assertEquals(200, response.getStatus());
    assertEquals("token", response.getEntity());
    assertEquals("application/json", response.getMediaType().toString());
  }

  @Test
  void testGetOAuth2Configuration() {
    var expectedConfig = new OAuth2Configuration("http://example.com", "test-client-id");
    when(mockOidcService.getOAuth2Configuration()).thenReturn(expectedConfig);
    OAuth2Resource resource = new OAuth2Resource(mockOidcService);
    var response = resource.getOAuth2Configuration();
    assertEquals(200, response.getStatus());
    assertEquals(expectedConfig, response.getEntity());
  }
}
