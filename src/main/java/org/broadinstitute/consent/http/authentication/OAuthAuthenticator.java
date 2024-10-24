package org.broadinstitute.consent.http.authentication;

import com.google.gson.Gson;
import com.google.inject.Inject;
import io.dropwizard.auth.AuthenticationException;
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
import org.broadinstitute.consent.http.service.sam.SamService;
import org.broadinstitute.consent.http.util.ConsentLogger;


public class OAuthAuthenticator implements Authenticator<String, AuthUser>, ConsentLogger {

  private final SamService samService;
  private final ClaimsCache claimsCache;

  @Inject
  public OAuthAuthenticator(SamService samService) {
    this.samService = samService;
    this.claimsCache = ClaimsCache.getInstance();
  }

  @Override
  public Optional<AuthUser> authenticate(String bearer) throws AuthenticationException {
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
    if (name == null) {
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
   * @return A cloned AuthUser with Sam registration status
   */
  private AuthUser getUserWithStatusInfo(AuthUser authUser) throws AuthenticationException {
    if (authUser == null || authUser.getEmail() == null) {
      logWarn("AuthUser/email is null, cannot get user status info");
      return null;
    }
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
        // if post response is not successful this will be thrown, propagate the error to the user
      } catch (WebApplicationException ex) {
        throw ex;
        // if building the request or parsing the response fails, this will be thrown
        // authenticationExceptions are caught and rethrown as a 500 error in AuthFilter
      } catch (Exception ex2) {
        throw new AuthenticationException("AuthUser not able to be registered: '" + gson.toJson(authUser), ex2);
      }
    // if there is some other error getting the user, log it and return the user without status info
    } catch (Throwable e) {
      logWarn(String.format("Exception retrieving Sam user info for '%s'", authUser.getEmail()), e);
    }
    return authUser;
  }

}
