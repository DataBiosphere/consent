package org.broadinstitute.consent.http.authentication;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.when;

import io.dropwizard.auth.AuthenticationException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriInfo;
import java.security.Principal;
import java.util.Optional;
import org.broadinstitute.consent.http.db.UserRoleDAO;
import org.broadinstitute.consent.http.models.AuthUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OAuthCustomAuthFilterTest {

  @Mock
  private ContainerRequestContext requestContext;
  @Mock
  private MultivaluedMap<String, String> headers;
  @Mock
  private UriInfo uriInfo;
  @Mock
  private OAuthAuthenticator authenticator;
  @Mock
  private UserRoleDAO userRoleDAO;

  @Mock
  private OAuthCustomAuthFilter<Principal> filter;

  @Mock
  private AuthUser user;

  private final String token = "0cx2G9gKm4XZdK8BFxoWy7AE025tvq";

  @BeforeEach
  void setUp() throws AuthenticationException {
    when(requestContext.getHeaders()).thenReturn(headers);
    when(requestContext.getUriInfo()).thenReturn(uriInfo);
    when(headers.getFirst("Authorization")).thenReturn("Bearer %s".formatted(token));
    when(authenticator.authenticate(notNull())).thenReturn(Optional.of(user));
    filter = Mockito.spy(new OAuthCustomAuthFilter<>(authenticator, userRoleDAO));
    user = new AuthUser().setName("Test User").setEmail("test@gmail.com");
  }

  @Test
  void testFilterSuccessful() throws AuthenticationException {
    when(uriInfo.getPath()).thenReturn("api/something");
    when(authenticator.authenticate(token)).thenReturn(Optional.of(user));
    assertDoesNotThrow(() -> filter.filter(requestContext));
  }


  @Test
  void testFilterExceptionBadCredentials() throws AuthenticationException {
    when(uriInfo.getPath()).thenReturn("api/something");
    when(authenticator.authenticate(token)).thenReturn(Optional.empty());
    assertThrows(WebApplicationException.class, () -> filter.filter(requestContext));
  }

  @Test
  void testFilterAuthWebApplicationException() throws AuthenticationException {
    when(uriInfo.getPath()).thenReturn("api/something");
    when(authenticator.authenticate(token)).thenThrow(new WebApplicationException("errorMessage"));
    WebApplicationException ex = assertThrows(WebApplicationException.class, () -> filter.filter(requestContext));
    assertEquals("errorMessage", ex.getMessage());
  }

  @Test
  void testFilterAuthAuthenticationException() throws AuthenticationException {
    when(uriInfo.getPath()).thenReturn("api/something");
    when(authenticator.authenticate(token)).thenThrow(new AuthenticationException("errorMessage"));
    WebApplicationException ex = assertThrows(WebApplicationException.class, () -> filter.filter(requestContext));
    assertEquals("HTTP 500 Internal Server Error", ex.getMessage());
  }
}