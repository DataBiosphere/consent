package org.broadinstitute.consent.http.authentication;

import com.google.common.base.Optional;
import io.dropwizard.auth.AuthFilter;
import org.broadinstitute.consent.http.db.DACUserRoleDAO;
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
    DACUserRoleDAO dacUserRoleDAO;
    @Mock
    Optional principal;

    AuthFilter filter;

    @Before
    public void setUp(){
        MockitoAnnotations.initMocks(this);
        when(requestContext.getHeaders()).thenReturn(headers);
        when(headers.getFirst("Authorization")).thenReturn("Bearer 0cx2G9gKm4XZdK8BFxoWy7AE025tvq");
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(principal.isPresent()).thenReturn(true);
        when(principal.get()).thenReturn("Testing principal");
        when(authenticator.authenticate(anyObject())).thenReturn(principal);
        filter = Mockito.spy(new OAuthCustomAuthFilter(authenticator, dacUserRoleDAO));

    }

    @Test
    public void testFilterSuccessful() throws Exception {
        when(uriInfo.getPath()).thenReturn("api/something");
        when(authenticator.authenticate("0cx2G9gKm4XZdK8BFxoWy7AE025tvq")).thenReturn(principal);
        filter.filter(requestContext);
    }


    @Test
    public void testFilterExceptionBadCredentials() throws Exception {
        when(uriInfo.getPath()).thenReturn("api/something");
        when(authenticator.authenticate("0cx2G9gKm4XZdK8BFxoWy7AE025tvq")).thenReturn(principal.absent());
        expectedEx.expect(WebApplicationException.class);
        expectedEx.expectMessage("HTTP 401 Unauthorized");
        filter.filter(requestContext);
    }
}