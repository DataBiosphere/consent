package org.broadinstitute.consent.http.authentication;

import io.dropwizard.auth.AuthFilter;
import io.dropwizard.auth.DefaultUnauthorizedHandler;
import io.dropwizard.auth.UnauthorizedHandler;
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter;
import org.broadinstitute.consent.http.configurations.GoogleOAuth2Config;
import org.broadinstitute.consent.http.db.DACUserRoleDAO;
import org.broadinstitute.consent.http.models.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.AuthenticationException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import java.io.IOException;
import java.security.Principal;

public class OAuthCustomAuthFilter<P extends Principal> extends AuthFilter<String, P>  {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultAuthFilter.class);
    private AuthFilter filter;
    private UnauthorizedHandler unauthorizedHandler = new DefaultUnauthorizedHandler();

    public OAuthCustomAuthFilter(GoogleOAuth2Config config, DACUserRoleDAO dacUserRoleDAO){
        filter = new OAuthCredentialAuthFilter.Builder<User>()
                .setAuthenticator(new OAuthAuthenticator(config))
                .setAuthorizer(new UserAuthorizer(dacUserRoleDAO))
                .setPrefix("Bearer")
                .setRealm("OAUTH-AUTH")
                .buildAuthFilter();
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String header = requestContext.getHeaders().getFirst("Authorization");
        String path = requestContext.getUriInfo().getPath();
        boolean match = path.matches("^(api/).*");
        try{
            if(header != null && match){
                filter.filter(requestContext);
            } else {
                throw new AuthenticationException("Authentication header is not empty, you tried to access an authenticated endpoint without credentials.");
            }
        }catch(Exception e){
            LOGGER.error("Error authenticating OAuth credentials: " + header + " for path: " + path);
            throw new WebApplicationException(this.unauthorizedHandler.buildResponse(this.prefix, this.realm));
        }
    }
}
