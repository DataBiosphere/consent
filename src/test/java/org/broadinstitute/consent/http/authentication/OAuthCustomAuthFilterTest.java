package org.broadinstitute.consent.http.authentication;

import io.dropwizard.auth.AuthFilter;
import org.broadinstitute.consent.http.db.UserRoleDAO;
import org.broadinstitute.consent.http.models.AuthUser;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import java.util.Optional;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

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

    @Before
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
        try {
            filter.filter(requestContext);
            fail("Filter should have failed");
        } catch (Exception e) {
            assertTrue(e instanceof WebApplicationException);
        }
    }
}