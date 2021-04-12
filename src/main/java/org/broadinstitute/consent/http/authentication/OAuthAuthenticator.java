package org.broadinstitute.consent.http.authentication;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;
import org.broadinstitute.consent.http.models.AuthUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Optional;


public class OAuthAuthenticator implements Authenticator<String, AuthUser> {

    private static final String TOKEN_INFO_URL = "https://www.googleapis.com/oauth2/v3/tokeninfo?access_token=";
    private static final String USER_INFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo?access_token=";
    private static final Logger logger = LoggerFactory.getLogger(OAuthAuthenticator.class);
    private Client client;

    @Inject
    public OAuthAuthenticator(Client client) {
        this.client = client;
    }

    @Override
    public Optional<AuthUser> authenticate(String bearer) {
        try {
            validateAudience(bearer);
            GoogleUser googleUser = getUserInfo(bearer);
            AuthUser user = new AuthUser(googleUser);
            return Optional.of(user);
        } catch (Exception e) {
            logger.error("Error authenticating credentials: " + e.getMessage());
            return Optional.empty();
        }
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
            tokenInfo = new ObjectMapper().readValue(result, new TypeReference<HashMap<String, Object>>() {});
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
