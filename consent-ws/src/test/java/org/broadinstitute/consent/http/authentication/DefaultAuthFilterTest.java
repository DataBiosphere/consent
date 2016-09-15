package org.broadinstitute.consent.http.authentication;

import io.dropwizard.auth.AuthFilter;
import io.dropwizard.auth.DefaultUnauthorizedHandler;
import io.dropwizard.auth.UnauthorizedHandler;
import org.broadinstitute.consent.http.models.User;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.Optional;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.when;

public class DefaultAuthFilterTest {

    public static final int CREATED = Response.Status.CREATED.getStatusCode();
    private Optional principal = Optional.of("Testing DefaulthAuthFilter");

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();
    @Mock
    ContainerRequestContext requestContext;
    @Mock
    MultivaluedMap<String, String> headers;
    @Mock
    UriInfo uriInfo;
    @Spy
    DefaultAuthenticator authenticator = new DefaultAuthenticator();
    @Spy
    UnauthorizedHandler unauthorizedHandler = new DefaultUnauthorizedHandler();

    AuthFilter filter = new DefaultAuthFilter.Builder<User>()
            .setAuthenticator(authenticator)
            .setRealm(" ")
            .setUnauthorizedHandler(unauthorizedHandler)
            .buildAuthFilter();

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(requestContext.getHeaders()).thenReturn(headers);
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
    }

    @Test
    public void testUnauthorizedUrl() throws Exception {
        when(uriInfo.getPath()).thenReturn("/something");
        when(headers.getFirst("Authorization")).thenReturn(null);
        expectedEx.expect(WebApplicationException.class);
        expectedEx.expectMessage("HTTP 401 Unauthorized");
        filter.filter(requestContext);
    }

    @Test
    public void testBasicUrl() throws Exception {
        principal = Optional.of("Testing principal");
        when(authenticator.authenticate(anyObject())).thenReturn(principal);
        when(uriInfo.getPath()).thenReturn("basic/something");
        filter.filter(requestContext);
    }

    @Test
    public void testApiUrl() throws Exception{
        principal = Optional.of("Testing principal");
        when(authenticator.authenticate(anyObject())).thenReturn(principal);
        when(uriInfo.getPath()).thenReturn("api/something");
        filter.filter(requestContext);
    }
}

