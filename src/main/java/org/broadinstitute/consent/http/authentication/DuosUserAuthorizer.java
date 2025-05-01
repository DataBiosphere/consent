package org.broadinstitute.consent.http.authentication;

import io.dropwizard.auth.Authorizer;
import jakarta.ws.rs.container.ContainerRequestContext;
import org.broadinstitute.consent.http.db.UserRoleDAO;
import org.broadinstitute.consent.http.models.DuosUser;

public class DuosUserAuthorizer extends AuthorizerHelper implements Authorizer<DuosUser> {

  DuosUserAuthorizer(UserRoleDAO userRoleDAO) {
    super(userRoleDAO);
  }

  @Override
  public boolean authorize(DuosUser user, String role, ContainerRequestContext context) {
    return super.authorize(user, role);
  }
}
