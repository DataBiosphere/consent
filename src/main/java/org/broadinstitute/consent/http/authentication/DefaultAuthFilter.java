package org.broadinstitute.consent.http.authentication;

import io.dropwizard.auth.AuthFilter;
import jakarta.annotation.Priority;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ContainerRequestContext;
import java.security.Principal;
import org.broadinstitute.consent.http.util.ConsentLogger;

@Priority(1000)
public class DefaultAuthFilter<P extends Principal> extends AuthFilter<String, P> implements
    ConsentLogger {

  @Override
  public void filter(ContainerRequestContext requestContext) {
    String path = requestContext.getUriInfo().getPath();
    boolean match = path.matches("^(api/).*");
    if (!match) {
      logWarn("Error processing path: " + path);
      throw new WebApplicationException(401);
    }
  }

  public static class Builder<P extends Principal>
      extends AuthFilterBuilder<String, P, DefaultAuthFilter<P>> {

    protected DefaultAuthFilter<P> newInstance() {
      return new DefaultAuthFilter();
    }
  }
}
