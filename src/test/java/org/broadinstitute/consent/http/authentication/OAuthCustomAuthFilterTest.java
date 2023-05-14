package org.broadinstitute.consent.http.authentication;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import io.dropwizard.auth.AuthFilter;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriInfo;
import java.util.Optional;
import org.broadinstitute.consent.http.db.UserRoleDAO;
import org.broadinstitute.consent.http.models.AuthUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

public class OAuthCustomAuthFilterTest {

  @Mock
  ContainerRequestContext requestContext;
  @Mock
  MultivaluedMap<String, String> headers;
  @Mock
  UriInfo uriInfo;
  @Mock
  OAuthAuthenticator authenticator;
  @Mock
  UserRoleDAO userRoleDAO;

  Optional principal;

  AuthFilter filter;

  AuthUser user;

  GenericUser genericUser;

  @BeforeEach
  public void setUp() {
    openMocks(this);
    when(requestContext.getHeaders()).thenReturn(headers);
    when(requestContext.getUriInfo()).thenReturn(uriInfo);
    when(headers.getFirst("Authorization")).thenReturn("Bearer 0cx2G9gKm4XZdK8BFxoWy7AE025tvq");
    when(authenticator.authenticate(notNull())).thenReturn(principal);
    filter = Mockito.spy(new OAuthCustomAuthFilter(authenticator, userRoleDAO));
    genericUser = new GenericUser();
    genericUser.setName("Test User");
    genericUser.setEmail("test@gmail.com");
    user = new AuthUser(genericUser);
  }

  @Test
  public void testFilterSuccessful() throws Exception {
    principal = Optional.of(user);
    when(uriInfo.getPath()).thenReturn("api/something");
    when(authenticator.authenticate("0cx2G9gKm4XZdK8BFxoWy7AE025tvq")).thenReturn(principal);
    filter.filter(requestContext);
  }


  @Test
  public void testFilterExceptionBadCredentials() {
    principal = Optional.empty();
    when(uriInfo.getPath()).thenReturn("api/something");
    when(authenticator.authenticate("0cx2G9gKm4XZdK8BFxoWy7AE025tvq")).thenReturn(principal);
    assertThrows(WebApplicationException.class, () -> {
      filter.filter(requestContext);
    });
  }
}