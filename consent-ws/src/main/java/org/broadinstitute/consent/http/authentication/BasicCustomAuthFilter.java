package org.broadinstitute.consent.http.authentication;

import io.dropwizard.auth.AuthFilter;
import io.dropwizard.auth.basic.BasicCredentialAuthFilter;
import org.broadinstitute.consent.http.models.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.ws.rs.container.ContainerRequestContext;
import java.io.IOException;
import java.security.Principal;

public class BasicCustomAuthFilter<P extends Principal> extends AuthFilter<String, P>  {

    private static final Logger logger = LoggerFactory.getLogger(BasicCustomAuthFilter.class);
    private AuthFilter filter;

    public BasicCustomAuthFilter(BasicAuthenticator authenticator){
        filter = new BasicCredentialAuthFilter.Builder<User>()
                .setAuthenticator(authenticator)
                .setPrefix("Basic")
                .setRealm("BASIC-AUTH")
                .buildAuthFilter();
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String path = requestContext.getUriInfo().getPath();
        boolean match = path.matches("^(basic/).*");
        if(match){
            logger.info("basic authentication");
            filter.filter(requestContext);
        }
    }

}