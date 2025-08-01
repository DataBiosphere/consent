package org.broadinstitute.consent.http.authentication;

import io.dropwizard.auth.Authorizer;
import jakarta.ws.rs.container.ContainerRequestContext;
import org.broadinstitute.consent.http.models.DuosUser;

public class DuosUserAuthorizer implements Authorizer<DuosUser> {

  private final AuthorizationHelper authorizationHelper;

  DuosUserAuthorizer(AuthorizationHelper authorizationHelper) {
    this.authorizationHelper = authorizationHelper;
  }

  @Override
  public boolean authorize(DuosUser user, String role, ContainerRequestContext context) {
    return authorizationHelper.authorize(user, role);
  }
}
