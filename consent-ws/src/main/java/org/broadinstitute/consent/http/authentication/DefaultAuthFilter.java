package org.broadinstitute.consent.http.authentication;

import com.google.common.base.Optional;
import io.dropwizard.auth.AuthFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Priority;
import javax.naming.AuthenticationException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.SecurityContext;
import java.io.IOException;
import java.security.Principal;
import java.util.regex.Pattern;

@Priority(1000)
public class DefaultAuthFilter<P extends Principal> extends AuthFilter<String, P> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultAuthFilter.class);
    private final Pattern p = Pattern.compile("(basic/|api/)");

    public DefaultAuthFilter(){

    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String header = requestContext.getHeaders().getFirst("Authorization");
        String path = requestContext.getUriInfo().getPath();
        boolean match = path.matches("^(basic/|api/).*");
        try{
            if(header != null || match){
                throw new AuthenticationException("Authentication header is not empty, you tried to access an authenticated endpoint without credentials.");
            } else {
                final Optional user = this.authenticator.authenticate("Anonymous User");
                requestContext.setSecurityContext(new SecurityContext() {
                    public Principal getUserPrincipal() {
                        return (Principal)user.get();
                    }

                    public boolean isUserInRole(String role) {
                        return true;
                    }

                    public boolean isSecure() {
                        return requestContext.getSecurityContext().isSecure();
                    }

                    public String getAuthenticationScheme() {
                        return "DEFAULT";
                    }
                });
                return;
            }
        }catch(Exception e){
            LOGGER.warn("Error authenticating credentials. You don't have to send authentication header to non-authenticated endpoints. ");
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
