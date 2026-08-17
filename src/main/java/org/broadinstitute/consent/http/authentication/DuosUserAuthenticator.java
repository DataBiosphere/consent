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
        authUser.setUserStatusInfo(
            authorizationHelper.getUserStatusInfo(new DuosUser(authUser, null)));
        try {
          User duosUser = authorizationHelper.userService.findUserByEmail(authUser.getEmail());
          return Optional.of(new DuosUser(authUser, duosUser));
        } catch (NotFoundException e) {
          // The token itself is good; only the DUOS account is missing. Returning an empty
          // Optional here would surface as a 401, indistinguishable from a rejected token, so
          // let the 404 propagate and keep "not registered yet" a distinct answer.
          logWarn("User not found, authentication incomplete: %s".formatted(authUser.getEmail()));
          throw e;
        }
      }
      return Optional.empty();
    }
    logException(new ServerErrorException("Error reading request headers", 500));
    return Optional.empty();
  }
}
