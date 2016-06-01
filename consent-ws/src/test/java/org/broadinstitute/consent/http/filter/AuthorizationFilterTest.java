package org.broadinstitute.consent.http.filter;

import junit.framework.TestCase;
import org.apache.http.HttpHeaders;
import org.broadinstitute.consent.http.authentication.BasicAuthenticationAPI;
import org.broadinstitute.consent.http.authentication.GoogleAuthenticationAPI;
import org.broadinstitute.consent.http.enumeration.AuthenticationType;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.HttpMethod;

import static org.mockito.Mockito.*;

public class AuthorizationFilterTest extends TestCase {

    private AuthorizationFilter filter;
    @Mock
    GoogleAuthenticationAPI mockGoogleAuthApi;
    @Mock
    BasicAuthenticationAPI mockBasicAuthAPI;
    @Mock
    HttpServletRequest request;
    @Mock
    HttpServletResponse response;
    @Mock
    FilterChain chain;

    @Before
    public void setUp(){
        MockitoAnnotations.initMocks(this);
        filter = new AuthorizationFilter(mockGoogleAuthApi, mockBasicAuthAPI);
        when(request.getMethod()).thenReturn(HttpMethod.GET);
    }

    @Test
    public void testDoFilterGoogle() throws Exception {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(AuthenticationType.BEARER.getValue());
        filter.doFilter(request, response, chain);
        verify(mockGoogleAuthApi, times(1)).validateAccessToken(AuthenticationType.BEARER.getValue());
        verify(chain, times(1)).doFilter(request, response);
        verify(mockBasicAuthAPI, never()).validateUser(AuthenticationType.BEARER.getValue());
    }

    public void testDoFilterBasic() throws Exception {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(AuthenticationType.BASIC.getValue());
        filter.doFilter(request, response, chain);
        verify(mockBasicAuthAPI, times(1)).validateUser(AuthenticationType.BASIC.getValue());
        verify(chain, times(1)).doFilter(request, response);
        verify(mockGoogleAuthApi, never()).validateAccessToken(AuthenticationType.BASIC.getValue());
    }

    @Test
    public void testDoFilterException() throws Exception {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Invalid header");
        filter.doFilter(request, response, chain);
        verify(response, times(1)).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    public void testDoFilterOptions() throws Exception {
        when(request.getMethod()).thenReturn(HttpMethod.OPTIONS);
        filter.doFilter(request, response, chain);
        verify(chain, times(1)).doFilter(request, response);
    }
}