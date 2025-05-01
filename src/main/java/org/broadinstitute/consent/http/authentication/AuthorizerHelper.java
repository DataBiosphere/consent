package org.broadinstitute.consent.http.authentication;

import java.util.List;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.consent.http.db.UserRoleDAO;
import org.broadinstitute.consent.http.models.AuthUser;

/**
 * This class is used to in authorizer classes that checks if a user has a specific role. It is used
 * by the UserAuthorizer and DuosUserAuthorizer classes.
 */
public abstract class AuthorizerHelper {

  private final UserRoleDAO userRoleDAO;

  protected AuthorizerHelper(UserRoleDAO userRoleDAO) {
    this.userRoleDAO = userRoleDAO;
  }

  protected boolean authorize(AuthUser user, String role) {
    boolean authorize = false;
    if (StringUtils.isNotEmpty(role)) {
      List<String> roles = userRoleDAO.findRoleNamesByUserEmail(user.getEmail());
      List<String> existentRole = roles.stream().filter(r -> r.equalsIgnoreCase(role)).toList();
      if (CollectionUtils.isNotEmpty(existentRole)) {
        authorize = true;
      }
    }
    return authorize;
  }
}
