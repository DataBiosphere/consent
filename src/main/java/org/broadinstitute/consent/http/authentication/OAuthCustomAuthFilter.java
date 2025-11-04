package org.broadinstitute.consent.http.authentication;

import io.dropwizard.auth.AuthFilter;
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import java.io.IOException;
import java.security.Principal;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.DuosUser;

@Priority(Priorities.AUTHENTICATION)
public class OAuthCustomAuthFilter<P extends Principal> extends AuthFilter<String, P> {

  private final AuthFilter filter;

  /**
   * Constructor for OAuthCustomAuthFilter intended to be used with AuthUsers.
   *
   * @param authenticator OAuthAuthenticator
   * @param authorizationHelper AuthorizationHelper
   */
  public OAuthCustomAuthFilter(
      OAuthAuthenticator authenticator, AuthorizationHelper authorizationHelper) {
    filter =
        new OAuthCredentialAuthFilter.Builder<AuthUser>()
            .setAuthenticator(authenticator)
            .setAuthorizer(new UserAuthorizer(authorizationHelper))
            .setPrefix("Bearer")
            .setRealm("OAUTH-AUTH")
            .buildAuthFilter();
  }

  /**
   * Constructor for OAuthCustomAuthFilter intended to be used with DuosUsers.
   *
   * @param authenticator DuosUserAuthenticator
   * @param authorizationHelper AuthorizationHelper
   */
  public OAuthCustomAuthFilter(
      DuosUserAuthenticator authenticator, AuthorizationHelper authorizationHelper) {
    filter =
        new OAuthCredentialAuthFilter.Builder<DuosUser>()
            .setAuthenticator(authenticator)
            .setAuthorizer(new DuosUserAuthorizer(authorizationHelper))
            .setPrefix("Bearer")
            .setRealm("OAUTH-AUTH")
            .buildAuthFilter();
  }

  @Override
  public void filter(ContainerRequestContext requestContext) throws IOException {
    String path = requestContext.getUriInfo().getPath();
    boolean match = path.matches("^((swagger|api)/).*");
    if (match) {
      filter.filter(requestContext);
    }
  }
}
