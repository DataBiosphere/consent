package org.broadinstitute.consent.http.authentication;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.broadinstitute.consent.http.configurations.GoogleOAuth2Config;
import org.broadinstitute.consent.http.models.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;


public class OAuthAuthenticator implements Authenticator<String, User> {

    private GoogleOAuth2Config config;
    private final String tokenInfoUrl = "https://www.googleapis.com/oauth2/v3/tokeninfo?access_token=";
    private static final Logger logger = LoggerFactory.getLogger(OAuthAuthenticator.class);

    public OAuthAuthenticator(GoogleOAuth2Config config) {
        this.config = config;
    }

    @Override
    public Optional<User> authenticate(String bearer){
        try{
            String email = validateAccessToken(bearer);
            User user = new User(email);
            return Optional.of(user);
        }catch (Exception e){
            logger.error("Error authenticating credentials.");
            return Optional.absent();
        }

    }

    public String validateAccessToken(String bearer) throws AuthenticationException {
        HashMap<String, Object> tokenInfo = validateToken(bearer);
        try {
            String clientId = tokenInfo.containsKey("aud") ? tokenInfo.get("aud").toString() : tokenInfo.get("audience").toString();
            if (clientId == null || !clientId.equals(config.getClientId())) {
                unauthorized(bearer);
            }
        } catch (AuthenticationException e) {
            unauthorized(bearer);
        }
        return tokenInfo.get("email").toString();
    }

    private HashMap<String, Object> validateToken(String accessToken) throws AuthenticationException {
        HttpClient httpclient = HttpClients.createDefault();
        HashMap<String, Object> tokenInfo = null;
        HttpPost httppost = new HttpPost(tokenInfoUrl + accessToken);
        try {
            HttpResponse response = httpclient.execute(httppost);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                InputStream tokenInfoStream = entity.getContent();
                String result = IOUtils.toString(tokenInfoStream);
                tokenInfo = new ObjectMapper().readValue(result, HashMap.class);
                tokenInfoStream.close();
            }
        } catch (IOException e) {
            unauthorized(accessToken);
        }
        return tokenInfo;
    }


    private void unauthorized(String accessToken) throws AuthenticationException {
        throw new AuthenticationException("Provided access token or user credential is either null or empty or does not have permissions to access this resource." + accessToken);
    }


}