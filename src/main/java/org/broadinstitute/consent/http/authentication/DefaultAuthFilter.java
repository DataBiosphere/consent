package org.broadinstitute.consent.http.authentication;

import io.dropwizard.auth.AuthFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Priority;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import java.io.IOException;
import java.security.Principal;

@Priority(1000)
public class DefaultAuthFilter<P extends Principal> extends AuthFilter<String, P> {

    private static final Logger logger = LoggerFactory.getLogger(DefaultAuthFilter.class);

    public DefaultAuthFilter(){

    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String path = requestContext.getUriInfo().getPath();
        boolean match = path.matches("^(basic/|api/).*");
        try{
            if(!match){
                throw new WebApplicationException(401);
            }
        }catch(Exception e){
            logger.error("Error authenticating credentials.");
            throw new WebApplicationException(this.unauthorizedHandler.buildResponse(this.prefix, this.realm));
        }
    }

    public static class Builder<P extends Principal> extends AuthFilterBuilder<String, P, DefaultAuthFilter<P>> {
        public Builder() {
        }
        protected DefaultAuthFilter<P> newInstance() {
            return new DefaultAuthFilter();
        }
    }

}