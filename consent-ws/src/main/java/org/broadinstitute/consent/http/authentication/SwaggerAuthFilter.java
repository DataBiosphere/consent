package org.broadinstitute.consent.http.authentication;

import io.dropwizard.auth.AuthFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import java.io.IOException;
import java.security.Principal;

public class SwaggerAuthFilter<P extends Principal> extends AuthFilter<String, P> {

    private static final Logger logger = LoggerFactory.getLogger(SwaggerAuthFilter.class);

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String path = requestContext.getUriInfo().getPath();
        boolean match = path.matches("/swagger.*");
        if (!match) {
            logger.debug("Unauthorized path request: " + path);
            throw new WebApplicationException(this.unauthorizedHandler.buildResponse(this.prefix, this.realm));
        }
    }

}
