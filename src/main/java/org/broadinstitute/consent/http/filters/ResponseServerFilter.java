package org.broadinstitute.consent.http.filters;

import java.io.IOException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;

@Provider
public class ResponseServerFilter implements ContainerResponseFilter {

  @Override
  public void filter(
      ContainerRequestContext requestContext,
      ContainerResponseContext responseContext) throws IOException {
    if (!requestContext.getUriInfo().getPath().contains("swagger-ui")) {
      responseContext.getHeaders().add("X-Content-Type-Options", "nosniff");
    }
  }
}
