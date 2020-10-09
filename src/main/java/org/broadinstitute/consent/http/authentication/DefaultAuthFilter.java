package org.broadinstitute.consent.http.authentication;

import io.dropwizard.auth.AuthFilter;
import java.security.Principal;
import javax.annotation.Priority;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Priority(1000)
public class DefaultAuthFilter<P extends Principal> extends AuthFilter<String, P> {

  private static final Logger logger = LoggerFactory.getLogger(DefaultAuthFilter.class);

  @Override
  public void filter(ContainerRequestContext requestContext) {
    String path = requestContext.getUriInfo().getPath();
    boolean match = path.matches("^(basic/|api/).*");
    if (!match) {
      logger.warn("Error processing path: " + path);
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
