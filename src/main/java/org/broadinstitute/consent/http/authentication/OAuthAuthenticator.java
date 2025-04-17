package org.broadinstitute.consent.http.authentication;

import com.google.gson.Gson;
import com.google.inject.Inject;
import io.dropwizard.auth.Authenticator;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ServerErrorException;
import jakarta.ws.rs.WebApplicationException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.broadinstitute.consent.http.filters.ClaimsCache;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.sam.UserStatus;
import org.broadinstitute.consent.http.models.sam.UserStatusInfo;
import org.broadinstitute.consent.http.service.UserService;
import org.broadinstitute.consent.http.service.sam.SamService;
import org.broadinstitute.consent.http.util.ConsentLogger;
import org.broadinstitute.consent.http.util.gson.GsonUtil;


public class OAuthAuthenticator implements Authenticator<String, AuthUser>, ConsentLogger {

  private final ClaimsCache claimsCache;
  private final SamService samService;
  private final UserService userService;

  @Inject
  public OAuthAuthenticator(SamService samService, UserService userService) {
    this.claimsCache = ClaimsCache.getInstance();
    this.samService = samService;
    this.userService = userService;
  }

  @Override
  public Optional<AuthUser> authenticate(String bearer) {
    var headers = claimsCache.cache.getIfPresent(bearer);
    if (headers != null) {
      AuthUser user = buildAuthUserFromHeaders(headers);
      AuthUser userWithStatus = getUserWithStatusInfo(user);
      if (userWithStatus == null) {
        logWarn("User with status is null, authentication incomplete");
        return Optional.of(user);
      }
      return Optional.of(userWithStatus);
    }
    logException(new ServerErrorException("Error reading request headers", 500));
    return Optional.empty();
  }

  private AuthUser buildAuthUserFromHeaders(Map<String, String> headers) {
    String aud = headers.get(ClaimsCache.OAUTH2_CLAIM_aud);
    String token = headers.get(ClaimsCache.OAUTH2_CLAIM_access_token);
    String email = headers.get(ClaimsCache.OAUTH2_CLAIM_email);
    String name = headers.get(ClaimsCache.OAUTH2_CLAIM_name);
    // Name is not a guaranteed header
    if (name == null || name.equals("unknown")) {
      name = email;
    }
    if (email == null) {
      logWarn(String.format("Reading oauth2 claim headers: email is null, auth user is incomplete. Aud: %s Name: %s", aud, name));
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
   * @return A cloned AuthUser with Sam registration status and a Consent User
   */
  private AuthUser getUserWithStatusInfo(AuthUser authUser) {
    if (authUser == null || authUser.getEmail() == null) {
      logWarn("AuthUser/email is null, cannot get user status info");
      return null;
    }
    try {
      authUser.setUser(userService.findUserByEmail(authUser.getEmail()));
    } catch (Exception e) {
      logException("Error finding Consent user: " + authUser.getEmail(), e);
    }
    try {
      UserStatusInfo userStatusInfo = samService.getRegistrationInfo(authUser);
      if (Objects.nonNull(userStatusInfo)) {
        // safety check in case the call to generic user (i.e. Google) failed.
        if (authUser.getEmail() == null) {
          authUser.setEmail(userStatusInfo.getUserEmail());
        }
        if (authUser.getName() == null) {
          authUser.setName(userStatusInfo.getUserEmail());
        }
      } else {
        logWarn("Error getting user status info back from Sam for user: " + authUser.getEmail());
      }
      return authUser.deepCopy().setUserStatusInfo(userStatusInfo);
    } catch (NotFoundException e) {
      try {
        // Try to post the user to Sam if they have not registered previously
        UserStatus userStatus = samService.postRegistrationInfo(authUser);
        if ((userStatus != null) && (userStatus.getUserInfo() != null)) {
          authUser.setEmail(userStatus.getUserInfo().getUserEmail());
        } else {
          Gson gson = GsonUtil.gsonBuilderWithAdapters().create();
          logWarn("Error posting to Sam, AuthUser not able to be registered: " + gson.toJson(authUser));
        }
      } catch (Exception ex) {
        // if post response is not successful, propagate the error to the user
        throw new WebApplicationException(ex.getMessage());
      }
    } catch (Exception e) {
      // if there is some other error getting the user, log it and return the user without status info
      logWarn(String.format("Exception retrieving Sam user info for '%s'", authUser.getEmail()), e);
    }
    return authUser;
  }

}
