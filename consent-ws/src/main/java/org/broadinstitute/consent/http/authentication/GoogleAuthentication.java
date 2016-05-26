package org.broadinstitute.consent.http.authentication;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.broadinstitute.consent.http.configurations.GoogleOAuth2Config;

import java.io.InputStream;
import java.util.HashMap;

public class GoogleAuthentication implements GoogleAuthenticationAPI {

    private GoogleOAuth2Config config;
    private final String tokenInfoUrl = "https://www.googleapis.com/oauth2/v3/tokeninfo?access_token=";

    public GoogleAuthentication(GoogleOAuth2Config config) {
        this.config = config;
    }

    @Override
    public void validateAccessToken(String authHeader) throws Exception {
        String accessToken;
        accessToken = authHeader.substring(7).trim();
        HashMap<String,Object> tokenInfo = validateToken(accessToken);
        try{
            String clientId = tokenInfo.containsKey("aud") ? tokenInfo.get("aud").toString() : tokenInfo.get("audience").toString();
            if (clientId == null || !clientId.equals(config.getClientId())){
                unauthorized(accessToken);
            }
        }catch (Exception e){
            unauthorized(accessToken);
        }
    }

    private HashMap<String,Object> validateToken(String accessToken) throws Exception {
        HttpClient httpclient = HttpClients.createDefault();
        HashMap<String,Object> tokenInfo = null;
        HttpPost httppost = new HttpPost(tokenInfoUrl + accessToken);
        HttpResponse response = httpclient.execute(httppost);
        HttpEntity entity = response.getEntity();
        if (entity != null) {
            InputStream tokenInfoStream = entity.getContent();
            try {
                String result = IOUtils.toString(tokenInfoStream);
                tokenInfo =  new ObjectMapper().readValue(result, HashMap.class);

            } finally {
                tokenInfoStream.close();
            }
        }
        return tokenInfo;
    }

    private void unauthorized(String accessToken) throws Exception {
        throw new Exception("Provided access token or user credential is either null or empty or does not have permissions to access this resource." + accessToken);
    }

}
