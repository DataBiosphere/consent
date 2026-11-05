package org.broadinstitute.consent.http.authentication;

import com.google.inject.Inject;
import io.dropwizard.auth.Authenticator;
import java.util.Optional;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.DuosUser;
import org.broadinstitute.consent.http.util.ConsentLogger;

public class OAuthAuthenticator implements Authenticator<String, AuthUser>, ConsentLogger {

  static final String NO_CACHED_CLAIMS =
      "No cached claims for the bearer token, request is anonymous";

  private final AuthorizationHelper authorizationHelper;

  @Inject
  public OAuthAuthenticator(AuthorizationHelper authorizationHelper) {
    this.authorizationHelper = authorizationHelper;
  }

  @Override
  public Optional<AuthUser> authenticate(String bearer) {
    var headers = authorizationHelper.getCache().getIfPresent(bearer);
    if (headers != null) {
      AuthUser user = authorizationHelper.buildAuthUserFromHeaders(headers);
      user.setUserStatusInfo(authorizationHelper.getUserStatusInfo(new DuosUser(user, null)));
      if (user.getUserStatusInfo() == null) {
        logWarn("User with status is null, authentication incomplete");
      }
      return Optional.of(user);
    }
    // No claims are cached for this bearer: the request did not come through the sidecar, or the
    // sidecar sent no email claim. Only a console warning, not a Sentry alert, so a direct caller
    // on the app port cannot raise one alert for each request (DT-4419).
    logWarn(NO_CACHED_CLAIMS);
    return Optional.empty();
  }
}
