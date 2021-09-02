package org.broadinstitute.consent.http.authentication;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;
import org.broadinstitute.consent.http.models.AuthUser;
import org.broadinstitute.consent.http.models.sam.UserStatusInfo;
import org.broadinstitute.consent.http.service.sam.SamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Optional;


public class OAuthAuthenticator implements Authenticator<String, AuthUser> {

    private static final String TOKEN_INFO_URL = "https://www.googleapis.com/oauth2/v3/tokeninfo?access_token=";
    private static final String USER_INFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo?access_token=";
    private static final Logger logger = LoggerFactory.getLogger(OAuthAuthenticator.class);
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
            validateAudience(bearer);
            GoogleUser googleUser = getUserInfo(bearer);
            AuthUser user = new AuthUser(googleUser).setAuthToken(bearer);
            AuthUser userWithStatus = getUserWithStatusInfo(user);
            return Optional.of(userWithStatus);
        } catch (Exception e) {
            logger.error("Error authenticating credentials: " + e.getMessage());
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
            return authUser.deepCopy().setUserStatusInfo(userStatusInfo);
        } catch (NotFoundException e) {
            logger.warn("User not found: '" + authUser.getEmail());
        } catch (Throwable e) {
            logger.error("Exception retrieving Sam user info for '" + authUser.getEmail() + "': " + e.getMessage());
        }
        return authUser;
    }

    private void validateAudience(String bearer) throws AuthenticationException {
        HashMap<String, Object> tokenInfo = validateToken(bearer);
        try {
            String clientId = tokenInfo.containsKey("aud") ? tokenInfo.get("aud").toString() : tokenInfo.get("audience").toString();
            if (clientId == null) {
                unauthorized(bearer);
            }
        } catch (AuthenticationException e) {
            logger.error("Error validating audience: " + e.getMessage());
            unauthorized(bearer);
        }
    }

    private HashMap<String, Object> validateToken(String accessToken) throws AuthenticationException {
        HashMap<String, Object> tokenInfo = null;
        try {
            Response response = this.client.
                    target(TOKEN_INFO_URL + accessToken).
                    request(MediaType.APPLICATION_JSON_TYPE).
                    get(Response.class);
            String result = response.readEntity(String.class);
            tokenInfo = new ObjectMapper().readValue(result, new TypeReference<>() {});
        } catch (Exception e) {
            logger.error("Error validating access token: " + e.getMessage());
            unauthorized(accessToken);
        }
        return tokenInfo;
    }

    private GoogleUser getUserInfo(String bearer) throws AuthenticationException {
        GoogleUser u = null;
        try {
            Response response = this.client.
                    target(USER_INFO_URL + bearer).
                    request(MediaType.APPLICATION_JSON_TYPE).
                    get(Response.class);
            String result = response.readEntity(String.class);
            u = new GoogleUser(result);
        } catch (Exception e) {
            logger.error("Error getting user info from token: " + e.getMessage());
            unauthorized(bearer);
        }
        return u;
    }

    private void unauthorized(String accessToken) throws AuthenticationException {
        throw new AuthenticationException("Provided access token or user credential is either null or empty or does not have permissions to access this resource." + accessToken);
    }

}
