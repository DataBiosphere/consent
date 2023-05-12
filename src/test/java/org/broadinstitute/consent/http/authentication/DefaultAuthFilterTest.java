package org.broadinstitute.consent.http.authentication;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import io.dropwizard.auth.AuthFilter;
import io.dropwizard.auth.DefaultUnauthorizedHandler;
import io.dropwizard.auth.UnauthorizedHandler;
import java.util.Optional;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import org.broadinstitute.consent.http.models.AuthUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Spy;

public class DefaultAuthFilterTest {

    @Mock
    private ContainerRequestContext requestContext;
    @Mock
    private MultivaluedMap<String, String> headers;
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

    @BeforeEach
    public void setUp() {
        openMocks(this);
        when(requestContext.getHeaders()).thenReturn(headers);
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
    }

    @Test
    public void testUnauthorizedUrl() {
        when(uriInfo.getPath()).thenReturn("/something");
        when(headers.getFirst("Authorization")).thenReturn(null);
        assertThrows(WebApplicationException.class, () -> {
            filter.filter(requestContext);
        });
    }

    @Test
    public void testApiUrl() throws Exception {
        Optional<AuthUser> principal = Optional.of(new AuthUser("test@email.com"));
        when(authenticator.authenticate(notNull())).thenReturn(principal);
        when(uriInfo.getPath()).thenReturn("api/something");
        filter.filter(requestContext);
    }
}

