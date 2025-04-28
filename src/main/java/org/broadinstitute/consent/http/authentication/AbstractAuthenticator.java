package org.broadinstitute.consent.http.authentication;

import com.google.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import java.util.Map;
import org.broadinstitute.consent.http.filters.ClaimsCache;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.sam.UserStatus;
import org.broadinstitute.consent.http.models.sam.UserStatusInfo;
import org.broadinstitute.consent.http.service.UserService;
import org.broadinstitute.consent.http.service.sam.SamService;
import org.broadinstitute.consent.http.util.ConsentLogger;

public abstract class AbstractAuthenticator implements ConsentLogger {

  protected final ClaimsCache claimsCache;
  protected final SamService samService;
  protected final UserService userService;

  @Inject
  protected AbstractAuthenticator(SamService samService,
      UserService userService) {
    this.claimsCache = ClaimsCache.getInstance();
    this.samService = samService;
    this.userService = userService;
  }

  protected AuthUser buildAuthUserFromHeaders(Map<String, String> headers) {
    String aud = headers.get(ClaimsCache.OAUTH2_CLAIM_aud);
    String token = headers.get(ClaimsCache.OAUTH2_CLAIM_access_token);
    String email = headers.get(ClaimsCache.OAUTH2_CLAIM_email);
    String name = headers.get(ClaimsCache.OAUTH2_CLAIM_name);
    // Name is not a guaranteed header
    if (name == null || name.equals("unknown")) {
      name = email;
    }
    if (email == null) {
      logWarn(String.format(
          "Reading oauth2 claim headers: email is null, auth user is incomplete. Aud: %s Name: %s",
          aud, name));
    }
    return new AuthUser(token, email, name, aud);
  }

  /**
   * Attempt to get the registration status of the current user. If the user is not registered,
   * attempt to register them and return the registration status.
   *
   * @param authUser The AuthUser
   * @return A Sam UserStatusInfo entity
   */
  protected UserStatusInfo getUserStatusInfo(AuthUser authUser) {
    try {
      return samService.getRegistrationInfo(authUser);
    } catch (NotFoundException e) {
      try {
        // Try to post the user to Sam if they have not registered previously
        UserStatus userStatus = samService.postRegistrationInfo(authUser);
        // If we succeed, return a basic version of UserStatusInfo. Future API calls will
        // return the full UserStatusInfo object.
        return new UserStatusInfo()
            .setUserEmail(authUser.getEmail())
            .setUserSubjectId(userStatus.getUserInfo().getUserSubjectId());
      } catch (Exception ex) {
        // if post response is not successful, propagate the error to the user
        throw new WebApplicationException(ex.getMessage());
      }
    } catch (Exception e) {
      logWarn(String.format("Exception retrieving Sam user info for '%s'", authUser.getEmail()), e);
    }
    return null;
  }

}
