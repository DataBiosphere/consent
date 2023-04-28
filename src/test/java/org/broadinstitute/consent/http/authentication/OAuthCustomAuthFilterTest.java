package org.broadinstitute.consent.http.authentication;

import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.when;

import io.dropwizard.auth.AuthFilter;
import java.util.Optional;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import org.broadinstitute.consent.http.db.UserRoleDAO;
import org.broadinstitute.consent.http.models.AuthUser;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class OAuthCustomAuthFilterTest {

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();
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

    @Before
    public void setUp(){
        MockitoAnnotations.initMocks(this);
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
    public void testFilterExceptionBadCredentials() throws Exception {
        principal = Optional.empty();
        when(uriInfo.getPath()).thenReturn("api/something");
        when(authenticator.authenticate("0cx2G9gKm4XZdK8BFxoWy7AE025tvq")).thenReturn(principal);
        expectedEx.expect(WebApplicationException.class);
        expectedEx.expectMessage("HTTP 401 Unauthorized");
        filter.filter(requestContext);
    }
}