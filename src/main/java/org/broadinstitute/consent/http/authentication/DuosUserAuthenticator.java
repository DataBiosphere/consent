package org.broadinstitute.consent.http.authentication;

import io.dropwizard.auth.Authenticator;
import jakarta.ws.rs.NotFoundException;
import java.util.Optional;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.DuosUser;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.util.ConsentLogger;

public class DuosUserAuthenticator implements Authenticator<String, DuosUser>, ConsentLogger {

  static final String NO_CACHED_CLAIMS =
      "No cached claims for the bearer token, request is anonymous";

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
          logWarn("User not found, authentication incomplete: %s".formatted(authUser.getEmail()));
        }
      }
      return Optional.empty();
    }
    // No claims are cached for this bearer: the request did not come through the sidecar, or the
    // sidecar sent no email claim. Only a console warning, not a Sentry alert, so a direct caller
    // on the app port cannot raise one alert for each request (DT-4419).
    logWarn(NO_CACHED_CLAIMS);
    return Optional.empty();
  }
}
