package org.broadinstitute.consent.http.filters;

import jakarta.annotation.Priority;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;

@Provider
@Priority(Integer.MIN_VALUE)
public class RequestHeaderCacheFilter implements ContainerRequestFilter {

  private final ClaimsCache claimsCache = ClaimsCache.getInstance();

  @Override
  public void filter(ContainerRequestContext containerRequestContext) throws IOException {
    var headers = containerRequestContext.getHeaders();
    var token = headers.getFirst(HttpHeaders.AUTHORIZATION);
    if (token != null) {
      var bearer = token.replace("Bearer ", "");
      claimsCache.loadCache(bearer, headers);
    }
  }
}
