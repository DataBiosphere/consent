package org.broadinstitute.consent.http.authentication;

import io.dropwizard.auth.AuthFilter;
import org.broadinstitute.consent.http.models.AuthUser;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import java.security.Principal;
import java.util.Optional;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.when;

public class BasicCustomAuthFilterTest {

    @Mock
    ContainerRequestContext requestContext;
    @Mock
    MultivaluedMap<String, String> headers;
    @Mock
    UriInfo uriInfo;
    @Mock
    BasicAuthenticator authenticator;

    private Optional<AuthUser> authUser;

    private AuthFilter<String, Principal> filter;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(requestContext.getHeaders()).thenReturn(headers);
        when(headers.getFirst("Authorization")).thenReturn("Basic dGVzdHVzZXI6dGVzdHBhc3N3b3Jk");
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        filter = Mockito.spy(new BasicCustomAuthFilter(authenticator));
    }

    @Test
    public void testFilterSuccessful() throws Exception {
        authUser = Optional.of(new AuthUser("Testing principal"));
        when(authenticator.authenticate(anyObject())).thenReturn(authUser);
        when(uriInfo.getPath()).thenReturn("basic/something");
        filter.filter(requestContext);
    }


    @Test(expected = WebApplicationException.class)
    public void testFilterExceptionBadCredentials() throws Exception {
        when(uriInfo.getPath()).thenReturn("basic/something");
        when(headers.getFirst("Authorization")).thenReturn(null);
        filter.filter(requestContext);
    }


}