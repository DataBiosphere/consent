package org.broadinstitute.consent.http.authentication;

import io.dropwizard.auth.AuthFilter;
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter;
import org.broadinstitute.consent.http.db.DACUserRoleDAO;
import org.broadinstitute.consent.http.models.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import java.io.IOException;
import java.security.Principal;

public class SwaggerAuthFilter<P extends Principal> extends AuthFilter<String, P> {

    private static final Logger logger = LoggerFactory.getLogger(SwaggerAuthFilter.class);

    private AuthFilter filter;

    public SwaggerAuthFilter(OAuthAuthenticator authenticator, DACUserRoleDAO dacUserRoleDAO) {
        filter = new OAuthCredentialAuthFilter.Builder<User>()
            .setAuthenticator(authenticator)
            .setAuthorizer(new UserAuthorizer(dacUserRoleDAO))
            .setPrefix("Bearer")
            .setRealm("OAUTH-AUTH")
            .buildAuthFilter();
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String path = requestContext.getUriInfo().getPath();
        boolean match = path.matches("^(swagger/).*");
        if (match) {
            logger.info("swagger oauth authentication");
            filter.filter(requestContext);
        }
    }

}
