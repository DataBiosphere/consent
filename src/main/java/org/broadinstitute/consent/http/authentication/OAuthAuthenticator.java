package org.broadinstitute.consent.http.authentication;

import com.google.inject.Inject;
import io.dropwizard.auth.Authenticator;
import jakarta.ws.rs.ServerErrorException;
import java.util.Optional;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.util.ConsentLogger;

public class OAuthAuthenticator implements Authenticator<String, AuthUser>, ConsentLogger {

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
      user.setUserStatusInfo(authorizationHelper.getUserStatusInfo(user));
      if (user.getUserStatusInfo() == null) {
        logWarn("User with status is null, authentication incomplete");
      }
      return Optional.of(user);
    }
    logException(new ServerErrorException("Error reading request headers", 500));
    return Optional.empty();
  }
}
