package org.broadinstitute.consent.http.authentication;

import io.dropwizard.auth.Authenticator;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ServerErrorException;
import java.util.Optional;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.DuosUser;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.util.ConsentLogger;

public class DuosUserAuthenticator implements Authenticator<String, DuosUser>, ConsentLogger {

  private final AuthorizationHelper authorizationHelper;

  public DuosUserAuthenticator(AuthorizationHelper authorizationHelper) {
    this.authorizationHelper = authorizationHelper;
  }

  @Override
  public Optional<DuosUser> authenticate(String bearer) {
    var headers = authorizationHelper.getCache().getIfPresent(bearer);
    if (headers != null) {
      AuthUser authUser = authorizationHelper.buildAuthUserFromHeaders(headers);
      if (authUser.getEmail() != null) {
        authUser.setUserStatusInfo(authorizationHelper.getUserStatusInfo(authUser));
        try {
          User duosUser = authorizationHelper.userService.findUserByEmail(authUser.getEmail());
          return Optional.of(new DuosUser(authUser, duosUser));
        } catch (NotFoundException e) {
          logWarn("User not found, authentication incomplete: %s".formatted(authUser.getEmail()));
        }
      }
      return Optional.empty();
    }
    logException(new ServerErrorException("Error reading request headers", 500));
    return Optional.empty();
  }
}
