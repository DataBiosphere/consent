package org.broadinstitute.consent.http.authentication;

import io.dropwizard.auth.Authorizer;
import jakarta.ws.rs.container.ContainerRequestContext;
import org.broadinstitute.consent.http.models.AuthUser;

public class UserAuthorizer implements Authorizer<AuthUser> {

  private final AuthorizationHelper authorizationHelper;

  UserAuthorizer(AuthorizationHelper authorizationHelper) {
    this.authorizationHelper = authorizationHelper;
  }

  @Override
  public boolean authorize(AuthUser user, String role, ContainerRequestContext context) {
    return authorizationHelper.authorize(user, role);
  }
}
