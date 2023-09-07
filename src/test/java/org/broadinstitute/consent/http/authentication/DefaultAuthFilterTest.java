package org.broadinstitute.consent.http.authentication;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import io.dropwizard.auth.AuthFilter;
import io.dropwizard.auth.DefaultUnauthorizedHandler;
import io.dropwizard.auth.UnauthorizedHandler;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.UriInfo;
import org.broadinstitute.consent.http.models.AuthUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultAuthFilterTest {

  @Mock
  private ContainerRequestContext requestContext;
  @Mock
  private UriInfo uriInfo;
  @Spy
  private final DefaultAuthenticator authenticator = new DefaultAuthenticator();
  @Spy
  private final UnauthorizedHandler unauthorizedHandler = new DefaultUnauthorizedHandler();

  private final AuthFilter<String, AuthUser> filter = new DefaultAuthFilter.Builder<AuthUser>()
      .setAuthenticator(authenticator)
      .setRealm(" ")
      .setUnauthorizedHandler(unauthorizedHandler)
      .buildAuthFilter();

  @Test
  void testUnauthorizedUrl() {
    when(requestContext.getUriInfo()).thenReturn(uriInfo);
    when(uriInfo.getPath()).thenReturn("/something");
    assertThrows(WebApplicationException.class, () -> {
      filter.filter(requestContext);
    });
  }

  @Test
  void testApiUrl() {
    when(requestContext.getUriInfo()).thenReturn(uriInfo);
    when(uriInfo.getPath()).thenReturn("api/something");
    assertDoesNotThrow(() -> {
      filter.filter(requestContext);
    });
  }
}

