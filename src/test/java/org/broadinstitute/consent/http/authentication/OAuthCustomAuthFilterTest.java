package org.broadinstitute.consent.http.authentication;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriInfo;
import java.security.Principal;
import java.util.Optional;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.DuosUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OAuthCustomAuthFilterTest {

  @Mock private ContainerRequestContext requestContext;
  @Mock private UriInfo uriInfo;
  @Mock private OAuthAuthenticator authenticator;
  @Mock private DuosUserAuthenticator duosUserAuthenticator;
  @Mock private AuthorizationHelper authorizationHelper;

  private OAuthCustomAuthFilter<Principal> filter;

  private final AuthUser user = new AuthUser().setName("Test User").setEmail("test@example.com");

  private final String token = "0cx2G9gKm4XZdK8BFxoWy7AE025tvq";

  @BeforeEach
  void setUp() {
    filter = new OAuthCustomAuthFilter<>(authenticator, authorizationHelper);
  }

  /** A request on the given path with these headers. */
  private void stubRequest(String path, MultivaluedMap<String, String> headers) {
    when(requestContext.getUriInfo()).thenReturn(uriInfo);
    when(uriInfo.getPath()).thenReturn(path);
    when(requestContext.getHeaders()).thenReturn(headers);
  }

  private static MultivaluedMap<String, String> authorization(String value) {
    MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
    headers.add(HttpHeaders.AUTHORIZATION, value);
    return headers;
  }

  @Test
  void testFilterSuccessful() {
    stubRequest("api/something", authorization("Bearer " + token));
    when(authenticator.authenticate(token)).thenReturn(Optional.of(user));

    assertDoesNotThrow(() -> filter.filter(requestContext));

    ArgumentCaptor<SecurityContext> context = ArgumentCaptor.forClass(SecurityContext.class);
    verify(requestContext).setSecurityContext(context.capture());
    assertEquals(user, context.getValue().getUserPrincipal());
  }

  /** The scheme name in the Authorization header is not case sensitive (RFC 7235). */
  @Test
  void testFilterAcceptsLowerCaseBearerPrefix() {
    stubRequest("api/something", authorization("bearer " + token));
    when(authenticator.authenticate(token)).thenReturn(Optional.of(user));

    assertDoesNotThrow(() -> filter.filter(requestContext));
  }

  @Test
  void testFilterExceptionBadCredentials() {
    stubRequest("api/something", authorization("Bearer " + token));
    when(authenticator.authenticate(token)).thenReturn(Optional.empty());

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> filter.filter(requestContext));
    assertEquals(401, ex.getResponse().getStatus());
  }

  @Test
  void testFilterAuthWebApplicationException() {
    stubRequest("api/something", authorization("Bearer " + token));
    when(authenticator.authenticate(token)).thenThrow(new WebApplicationException("errorMessage"));

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> filter.filter(requestContext));
    assertEquals("errorMessage", ex.getMessage());
  }

  /**
   * Consent accepts a bearer only from the Authorization header (DT-4428). A request that sends the
   * token only in the {@code access_token} query parameter has no Authorization header, so it gets
   * 401. The filter never reads the query, so a token there cannot reach the authenticator.
   */
  @ParameterizedTest
  @ValueSource(strings = {"api/user/me", "swagger/index.html"})
  void testFilterRejectsAccessTokenQueryParameter(String path) {
    stubRequest(path, new MultivaluedHashMap<>());

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> filter.filter(requestContext));

    assertEquals(401, ex.getResponse().getStatus());
    verify(uriInfo, never()).getQueryParameters();
    verify(uriInfo, never()).getQueryParameters(anyBoolean());
    verify(authenticator, never()).authenticate(any());
  }

  /** The DuosUser filter also reads the bearer only from the Authorization header. */
  @Test
  void testDuosUserFilterRejectsAccessTokenQueryParameter() {
    OAuthCustomAuthFilter<Principal> duosFilter =
        new OAuthCustomAuthFilter<>(duosUserAuthenticator, authorizationHelper);
    stubRequest("api/user/me", new MultivaluedHashMap<>());

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> duosFilter.filter(requestContext));

    assertEquals(401, ex.getResponse().getStatus());
    verify(uriInfo, never()).getQueryParameters();
    verify(duosUserAuthenticator, never()).authenticate(any());
  }

  @Test
  void testDuosUserFilterSuccessful() {
    OAuthCustomAuthFilter<Principal> duosFilter =
        new OAuthCustomAuthFilter<>(duosUserAuthenticator, authorizationHelper);
    stubRequest("api/something", authorization("Bearer " + token));
    DuosUser duosUser = new DuosUser(user, null);
    when(duosUserAuthenticator.authenticate(token)).thenReturn(Optional.of(duosUser));

    assertDoesNotThrow(() -> duosFilter.filter(requestContext));
  }

  /** A header with a scheme other than Bearer, or with no token, gives no credentials. */
  @ParameterizedTest
  @ValueSource(strings = {"Basic dXNlcjpwYXNz", "Bearer", token, "Bearertoken"})
  void testFilterRejectsMalformedAuthorizationHeader(String value) {
    stubRequest("api/something", authorization(value));

    WebApplicationException ex =
        assertThrows(WebApplicationException.class, () -> filter.filter(requestContext));

    assertEquals(401, ex.getResponse().getStatus());
    verify(authenticator, never()).authenticate(any());
  }

  /** Routes other than /api and /swagger skip authentication. */
  @Test
  void testFilterSkipsOtherRoutes() {
    when(requestContext.getUriInfo()).thenReturn(uriInfo);
    when(uriInfo.getPath()).thenReturn("status");

    assertDoesNotThrow(() -> filter.filter(requestContext));

    verify(requestContext, never()).getHeaders();
    verify(requestContext, never()).setSecurityContext(any());
  }
}
