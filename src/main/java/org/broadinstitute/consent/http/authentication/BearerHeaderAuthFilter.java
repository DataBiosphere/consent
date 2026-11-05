package org.broadinstitute.consent.http.authentication;

import io.dropwizard.auth.AuthFilter;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.SecurityContext;
import java.security.Principal;

/**
 * Reads a bearer token only from the {@code Authorization: Bearer <token>} header.
 *
 * <p>Dropwizard's {@code OAuthCredentialAuthFilter} also reads the token from the {@code
 * access_token} query parameter when there is no header, and it has no setting that turns this off.
 * Consent's authenticators do not validate the token: they look up the claims that the sidecar
 * cached for it. So each way to send a token to them must be guarded. This filter has one way only,
 * and it never reads the query (DT-4428). A token in a URL can also leak through logs, browser
 * history, and {@code Referer} headers (RFC 6750 section 2.3).
 *
 * <p>The rest of the behavior is the same as {@code OAuthCredentialAuthFilter}: the prefix match is
 * not case sensitive, and a request with no credentials, or that the authenticator rejects, gets
 * the 401 from the unauthorized handler.
 *
 * @param <P> the type of the principal
 */
@Priority(Priorities.AUTHENTICATION)
public class BearerHeaderAuthFilter<P extends Principal> extends AuthFilter<String, P> {

  private BearerHeaderAuthFilter() {}

  @Override
  public void filter(ContainerRequestContext requestContext) {
    String credentials =
        getCredentials(requestContext.getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
    if (!authenticate(requestContext, credentials, SecurityContext.BASIC_AUTH)) {
      throw unauthorizedHandler.buildException(prefix, realm);
    }
  }

  /**
   * Gets the token from an {@code Authorization} header value such as {@code Bearer abc123}.
   *
   * @return the token, or null if the header is missing or has a different scheme
   */
  private String getCredentials(String header) {
    if (header == null) {
      return null;
    }
    int space = header.indexOf(' ');
    if (space <= 0) {
      return null;
    }
    String method = header.substring(0, space);
    if (!prefix.equalsIgnoreCase(method)) {
      return null;
    }
    return header.substring(space + 1);
  }

  /**
   * Builder for {@link BearerHeaderAuthFilter}.
   *
   * @param <P> the type of the principal
   */
  public static class Builder<P extends Principal>
      extends AuthFilterBuilder<String, P, BearerHeaderAuthFilter<P>> {

    @Override
    protected BearerHeaderAuthFilter<P> newInstance() {
      return new BearerHeaderAuthFilter<>();
    }
  }
}
