package org.broadinstitute.consent.http.authentication;

import io.dropwizard.auth.Authorizer;
import jakarta.ws.rs.container.ContainerRequestContext;
import org.broadinstitute.consent.http.db.UserRoleDAO;
import org.broadinstitute.consent.http.models.DuosAuthUser;

public class DuosUserAuthorizer extends AbstractAuthorizer implements Authorizer<DuosAuthUser> {

  DuosUserAuthorizer(UserRoleDAO userRoleDAO) {
    super(userRoleDAO);
  }

  @Override
  public boolean authorize(DuosAuthUser user, String role, ContainerRequestContext context) {
    return super.authorize(user, role);
  }

}
