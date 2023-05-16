package org.broadinstitute.consent.http.filters;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;

@Provider
public class ResponseServerFilter implements ContainerResponseFilter {

  @Override
  public void filter(
      ContainerRequestContext requestContext,
      ContainerResponseContext responseContext) throws IOException {
    // When the no-sniff header is on the swagger-ui files, it breaks the overall UI
    if (!requestContext.getUriInfo().getPath().contains("swagger-ui")) {
      responseContext.getHeaders().add("X-Content-Type-Options", "nosniff");
    }
  }
}
