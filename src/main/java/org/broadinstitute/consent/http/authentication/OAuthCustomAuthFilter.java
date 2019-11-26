package org.broadinstitute.consent.http.authentication;

import io.dropwizard.auth.AuthFilter;
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter;
import org.broadinstitute.consent.http.db.UserRoleDAO;
import org.broadinstitute.consent.http.models.AuthUser;

import javax.ws.rs.container.ContainerRequestContext;
import java.io.IOException;
import java.security.Principal;

public class OAuthCustomAuthFilter<P extends Principal> extends AuthFilter<String, P> {

    private AuthFilter filter;

    public OAuthCustomAuthFilter(OAuthAuthenticator authenticator, UserRoleDAO userRoleDAO) {
        filter = new OAuthCredentialAuthFilter.Builder<AuthUser>()
            .setAuthenticator(authenticator)
            .setAuthorizer(new UserAuthorizer(userRoleDAO))
            .setPrefix("Bearer")
            .setRealm("OAUTH-AUTH")
            .buildAuthFilter();
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String path = requestContext.getUriInfo().getPath();
        boolean match = path.matches("^((swagger|api)/).*");
        if (match) {
            filter.filter(requestContext);
        }
    }
}
