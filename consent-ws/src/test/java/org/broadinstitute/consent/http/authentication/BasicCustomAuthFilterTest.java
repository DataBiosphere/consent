package org.broadinstitute.consent.http.authentication;

import com.google.common.base.Optional;
import io.dropwizard.auth.AuthFilter;
import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.basic.BasicCredentials;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.when;

public class BasicCustomAuthFilterTest {

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @Mock
    ContainerRequestContext requestContext;
    @Mock
    MultivaluedMap<String, String> headers;
    @Mock
    UriInfo uriInfo;
    @Mock
    BasicAuthenticator authenticator;
    @Mock
    BasicCredentials credentials;
    @Mock
    Optional principal;

    AuthFilter filter;

    @Before
    public void setUp() throws AuthenticationException {
        MockitoAnnotations.initMocks(this);
        when(requestContext.getHeaders()).thenReturn(headers);
        when(headers.getFirst("Authorization")).thenReturn("Basic dGVzdHVzZXI6dGVzdHBhc3N3b3Jk");
        when(principal.isPresent()).thenReturn(true);
        when(principal.get()).thenReturn("Testing principal");
        when(authenticator.authenticate(anyObject())).thenReturn(principal);
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        filter = Mockito.spy(new BasicCustomAuthFilter(authenticator));
    }

    @Test
    public void testFilterSuccessful() throws Exception {
        when(uriInfo.getPath()).thenReturn("basic/something");
        filter.filter(requestContext);
    }


    @Test
    public void testFilterExceptionBadCredentials() throws Exception {
        when(uriInfo.getPath()).thenReturn("basic/something");
        when(headers.getFirst("Authorization")).thenReturn(null);
        expectedEx.expect(WebApplicationException.class);
        expectedEx.expectMessage("HTTP 401 Unauthorized");
        filter.filter(requestContext);
    }


}