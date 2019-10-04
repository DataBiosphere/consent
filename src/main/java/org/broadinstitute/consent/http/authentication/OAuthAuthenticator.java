package org.broadinstitute.consent.http.authentication;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.auth.AuthenticationException;
import org.broadinstitute.consent.http.models.AuthUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Optional;


public class OAuthAuthenticator extends AbstractOAuthAuthenticator {

    private static final String TOKEN_INFO_URL = "https://www.googleapis.com/oauth2/v3/tokeninfo?access_token=";
    private static final String USER_INFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo?access_token=";
    private static final Logger logger = LoggerFactory.getLogger(OAuthAuthenticator.class);
    private Client client;

    public static void initInstance() {
        AuthenticatorAPIHolder.setInstance(new OAuthAuthenticator());
    }

    private OAuthAuthenticator() {
    }

    public void setClient(Client client) {
        this.client = client;
    }

    @Override
    public Optional<AuthUser> authenticate(String bearer) {
        try {
            String email = validateAccessToken(bearer);
            GoogleUser googleUser = getUserInfo(bearer);
            AuthUser user = new AuthUser(email, googleUser);
            return Optional.of(user);
        } catch (Exception e) {
            logger.error("Error authenticating credentials: " + e.getMessage());
            return Optional.empty();
        }
    }

    private String validateAccessToken(String bearer) throws AuthenticationException {
        HashMap<String, Object> tokenInfo = validateToken(bearer);
        try {
            String clientId = tokenInfo.containsKey("aud") ? tokenInfo.get("aud").toString() : tokenInfo.get("audience").toString();
            if (clientId == null) {
                unauthorized(bearer);
            }
        } catch (AuthenticationException e) {
            unauthorized(bearer);
        }
        return tokenInfo.get("email").toString();
    }

    private HashMap<String, Object> validateToken(String accessToken) throws AuthenticationException {
        HashMap<String, Object> tokenInfo = null;
        try {
            Response response = this
                    .client
                    .target(TOKEN_INFO_URL + accessToken)
                    .request(MediaType.APPLICATION_JSON_TYPE)
                    .get(Response.class);
            String result = response.readEntity(String.class);
            tokenInfo = new ObjectMapper().readValue(result, new TypeReference<HashMap<String, Object>>() {});
        } catch (Exception e) {
            unauthorized(accessToken);
        }
        return tokenInfo;
    }

    private GoogleUser getUserInfo(String bearer) throws AuthenticationException {
        GoogleUser u = null;
        try {
            Response response = this
                    .client
                    .target(USER_INFO_URL + bearer)
                    .request(MediaType.APPLICATION_JSON_TYPE)
                    .get(Response.class);
            String result = response.readEntity(String.class);
            u = new GoogleUser(result);
        } catch (Exception e) {
            unauthorized(bearer);
        }
        return u;
    }

    private void unauthorized(String accessToken) throws AuthenticationException {
        throw new AuthenticationException("Provided access token or user credential is either null or empty or does not have permissions to access this resource." + accessToken);
    }

}
