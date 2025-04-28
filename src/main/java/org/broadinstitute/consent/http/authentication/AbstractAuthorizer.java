package org.broadinstitute.consent.http.authentication;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.consent.http.db.UserRoleDAO;
import org.broadinstitute.consent.http.models.AuthUser;

public class AbstractAuthorizer {

  private final UserRoleDAO userRoleDAO;

  public AbstractAuthorizer(UserRoleDAO userRoleDAO) {
    this.userRoleDAO = userRoleDAO;
  }

  public boolean authorize(AuthUser user, String role) {
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
