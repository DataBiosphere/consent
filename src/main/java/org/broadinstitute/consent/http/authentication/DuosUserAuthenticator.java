package org.broadinstitute.consent.http.authentication;

import io.dropwizard.auth.Authenticator;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ServerErrorException;
import java.util.Optional;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.DuosAuthUser;
import org.broadinstitute.consent.http.models.User;
import org.broadinstitute.consent.http.service.UserService;
import org.broadinstitute.consent.http.service.sam.SamService;

public class DuosUserAuthenticator extends AbstractAuthenticator implements Authenticator<String, DuosAuthUser> {
  public DuosUserAuthenticator(SamService samService,
      UserService userService) {
    super(samService, userService);
  }


  @Override
  public Optional<DuosAuthUser> authenticate(String bearer) {
    var headers = claimsCache.cache.getIfPresent(bearer);
    if (headers != null) {
      AuthUser authUser = buildAuthUserFromHeaders(headers);
      if (authUser.getEmail() != null) {
        authUser.setUserStatusInfo(getUserStatusInfo(authUser));
        try {
          User duosUser = userService.findUserByEmail(authUser.getEmail());
          return Optional.of(new DuosAuthUser(authUser, duosUser));
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
