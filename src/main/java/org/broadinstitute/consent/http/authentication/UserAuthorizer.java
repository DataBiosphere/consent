package org.broadinstitute.consent.http.authentication;

import io.dropwizard.auth.Authorizer;
import jakarta.ws.rs.container.ContainerRequestContext;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.consent.http.db.UserRoleDAO;
import org.broadinstitute.consent.http.models.AuthUser;

public class UserAuthorizer implements Authorizer<AuthUser> {

  private UserRoleDAO userRoleDAO;

  UserAuthorizer(UserRoleDAO userRoleDAO) {
    this.userRoleDAO = userRoleDAO;
  }

  @Override
  public boolean authorize(AuthUser user, String role, ContainerRequestContext context) {
    boolean authorize = false;
    if (StringUtils.isNotEmpty(role)) {
      List<String> roles = userRoleDAO.findRoleNamesByUserEmail(user.getEmail());
      List<String> existentRole = roles.stream()
          .filter(r -> r.equalsIgnoreCase(role))
          .collect(Collectors.toCollection(ArrayList::new));
      if (CollectionUtils.isNotEmpty(existentRole)) {
        authorize = true;
      }
    }
    return authorize;
  }

}
