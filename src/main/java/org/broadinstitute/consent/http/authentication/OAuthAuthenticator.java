package org.broadinstitute.consent.http.authentication;

import com.google.gson.Gson;
import com.google.inject.Inject;
import io.dropwizard.auth.Authenticator;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ServerErrorException;
import jakarta.ws.rs.core.MultivaluedMap;
import java.util.Objects;
import java.util.Optional;
import org.broadinstitute.consent.http.filters.RequestHeaderCache;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.sam.UserStatus;
import org.broadinstitute.consent.http.models.sam.UserStatusInfo;
import org.broadinstitute.consent.http.service.sam.SamService;
import org.broadinstitute.consent.http.util.ConsentLogger;


public class OAuthAuthenticator implements Authenticator<String, AuthUser>, ConsentLogger {

  private final SamService samService;
  private final RequestHeaderCache requestHeaderCache;

  @Inject
  public OAuthAuthenticator(SamService samService) {
    this.samService = samService;
    this.requestHeaderCache = RequestHeaderCache.getInstance();
  }

  @Override
  public Optional<AuthUser> authenticate(String bearer) {
    try {
      var headers = requestHeaderCache.cache.getIfPresent(bearer);
      if (headers != null) {
        AuthUser user = buildAuthUserFromHeaders(headers);
        AuthUser userWithStatus = getUserWithStatusInfo(user);
        return Optional.of(userWithStatus);
      }
      logException(new ServerErrorException("Error reading request headers", 500));
      return Optional.empty();
    } catch (Exception e) {
      logException("Error authenticating credentials", e);
      return Optional.empty();
    }
  }

  private AuthUser buildAuthUserFromHeaders(MultivaluedMap<String, String> headers) {
    String aud = headers.getFirst(RequestHeaderCache.OAUTH2_CLAIM_aud);
    String token = headers.getFirst(RequestHeaderCache.OAUTH2_CLAIM_access_token);
    String email = headers.getFirst(RequestHeaderCache.OAUTH2_CLAIM_email);
    String name = headers.getFirst(RequestHeaderCache.OAUTH2_CLAIM_name);
    // Name is not a guaranteed header
    if (name == null) {
      name = email;
    }
    return new AuthUser()
        .setAud(aud)
        .setAuthToken(token)
        .setEmail(email)
        .setName(name);
  }

  /**
   * Attempt to get the registration status of the current user and set the value on AuthUser
   *
   * @param authUser The AuthUser
   * @return A cloned AuthUser with Sam registration status
   */
  private AuthUser getUserWithStatusInfo(AuthUser authUser) {
    try {
      UserStatusInfo userStatusInfo = samService.getRegistrationInfo(authUser);
      if (Objects.nonNull(userStatusInfo)) {
        // safety check in case the call to generic user (i.e. Google) failed.
        if (Objects.isNull(authUser.getEmail())) {
          authUser.setEmail(userStatusInfo.getUserEmail());
        }
        if (Objects.isNull(authUser.getName())) {
          authUser.setName(userStatusInfo.getUserEmail());
        }
      } else {
        logWarn("Error getting user status info back from Sam for user: " + authUser.getEmail());
      }
      return authUser.deepCopy().setUserStatusInfo(userStatusInfo);
    } catch (NotFoundException e) {
      Gson gson = new Gson();
      // Try to post the user to Sam if they have not registered previously
      try {
        UserStatus userStatus = samService.postRegistrationInfo(authUser);
        if (Objects.nonNull(userStatus) && Objects.nonNull(userStatus.getUserInfo())) {
          authUser.setEmail(userStatus.getUserInfo().getUserEmail());
        } else {
          logWarn("Error posting to Sam, AuthUser not able to be registered: " + gson.toJson(authUser));
        }
      } catch (Exception exc) {
        logException("AuthUser not able to be registered: '" + gson.toJson(authUser), exc);
      }
    } catch (Throwable e) {
      logException("Exception retrieving Sam user info for '" + authUser.getEmail() + "'",
          new Exception(e.getMessage()));
    }
    return authUser;
  }

}
