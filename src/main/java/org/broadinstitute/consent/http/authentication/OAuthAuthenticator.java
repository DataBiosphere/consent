package org.broadinstitute.consent.http.authentication;

import com.google.inject.Inject;
import io.dropwizard.auth.Authenticator;
import java.util.Objects;
import java.util.Optional;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.sam.UserStatus;
import org.broadinstitute.consent.http.models.sam.UserStatusInfo;
import org.broadinstitute.consent.http.service.sam.SamService;
import org.broadinstitute.consent.http.util.ConsentLogger;


public class OAuthAuthenticator implements Authenticator<String, AuthUser>, ConsentLogger {

    private static final String USER_INFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo?access_token=";
    private final Client client;
    private final SamService samService;

    @Inject
    public OAuthAuthenticator(Client client, SamService samService) {
        this.client = client;
        this.samService = samService;
    }

    @Override
    public Optional<AuthUser> authenticate(String bearer) {
        try {
            GenericUser genericUser = getUserProfileInfo(bearer);
            AuthUser user = Objects.nonNull(genericUser) ?
                new AuthUser(genericUser).setAuthToken(bearer) :
                new AuthUser().setAuthToken(bearer);
            AuthUser userWithStatus = getUserWithStatusInfo(user);
            return Optional.of(userWithStatus);
        } catch (Exception e) {
            logException("Error authenticating credentials", e);
            return Optional.empty();
        }
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
            // safety check in case the call to generic user (i.e. Google) failed.
            if (Objects.isNull(authUser.getEmail())) {
                authUser.setEmail(userStatusInfo.getUserEmail());
            }
            if (Objects.isNull(authUser.getName())) {
                authUser.setName(userStatusInfo.getUserEmail());
            }
            return authUser.deepCopy().setUserStatusInfo(userStatusInfo);
        } catch (NotFoundException e) {
            // Try to post the user to Sam if they have not registered previously
            try {
                UserStatus userStatus = samService.postRegistrationInfo(authUser);
                if (Objects.nonNull(userStatus) && Objects.nonNull(userStatus.getUserInfo())) {
                    authUser.setEmail(userStatus.getUserInfo().getUserEmail());
                }
            } catch (Exception exc) {
                // Same
                logException("User not able to be registered: '" + authUser.getEmail(), exc);
            }
        } catch (Throwable e) {
            logException("Exception retrieving Sam user info for '" + authUser.getEmail() + "'", new Exception(e.getMessage()));
        }
        return authUser;
    }

    /**
     * This method is currently google-centric. When we fully support B2C authentication,
     * we should ensure that we can look up user info from a MS service.
     * @param bearer Bearer Token
     * @return GenericUser
     */
    private GenericUser getUserProfileInfo(String bearer) {
        GenericUser u = null;
        try {
            Response response = this.client.
                    target(USER_INFO_URL + bearer).
                    request(MediaType.APPLICATION_JSON_TYPE).
                    get(Response.class);
            String result = response.readEntity(String.class);
            u = new GenericUser(result);
        } catch (Exception e) {
            logWarn("Error getting Google user info from token: " + e.getMessage());
        }
        return u;
    }

}
