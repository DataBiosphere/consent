package org.broadinstitute.consent.http.authentication;

import com.google.inject.Inject;
import io.dropwizard.auth.Authenticator;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ServerErrorException;
import java.util.Optional;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.DuosUser;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.service.UserService;
import org.broadinstitute.consent.http.service.sam.SamService;

public class OAuthAuthenticator extends AuthenticatorHelper implements
    Authenticator<String, AuthUser> {

  @Inject
  public OAuthAuthenticator(SamService samService, UserService userService) {
    super(samService, userService);
  }

  @Override
  public Optional<AuthUser> authenticate(String bearer) {
    var headers = claimsCache.cache.getIfPresent(bearer);
    if (headers != null) {
      AuthUser authUser = buildAuthUserFromHeaders(headers);
      if (authUser.getEmail() != null) {
        authUser.setUserStatusInfo(getUserStatusInfo(authUser));
        try {
          User duosUser = userService.findUserByEmail(authUser.getEmail());
          return Optional.of(new DuosUser(authUser, duosUser));
        } catch (NotFoundException e) {
          logWarn("User not found, authentication incomplete: %s".formatted(authUser.getEmail()));
        }
      }
      return Optional.of(authUser);
    }
    logException(new ServerErrorException("Error reading request headers", 500));
    return Optional.empty();
  }
}
