package org.broadinstitute.consent.http.authentication;


import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.oauth2.Oauth2;
import com.google.api.services.oauth2.model.Tokeninfo;
import org.broadinstitute.consent.http.configurations.GoogleConfiguration;

public class GoogleAuthentication implements GoogleAuthenticationAPI {

    private GoogleConfiguration config;

    public GoogleAuthentication(GoogleConfiguration config) {
        this.config = config;
    }



    @Override
    public void validateAccessToken(String authHeader) throws Exception {
        String accessToken;
        accessToken = authHeader.substring(7).trim();
        Credential credential = authorize(accessToken);
        HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        Oauth2 oauth2 = new Oauth2.Builder(
                httpTransport,
                JacksonFactory.getDefaultInstance(), credential)
                .build();
        Tokeninfo tokenInfo = oauth2.tokeninfo().setAccessToken(accessToken).execute();
        if (tokenInfo == null || !tokenInfo.getAudience().equals(config.getClientId())) {
            unauthorized(accessToken);
        }
    }

    private Credential authorize(String accessToken) {
        GoogleCredential credential =
                new GoogleCredential.Builder()
                        .setTransport(new NetHttpTransport())
                        .setJsonFactory(JacksonFactory.getDefaultInstance())
                        .setClientSecrets(config.getClientId(), config.getClientSecret()).build();
        credential.setAccessToken(accessToken);
        return credential;
    }

    private void unauthorized(String accessToken) throws Exception {
        throw new Exception("Provided access token or user credential is either null or empty or does not have permissions to access this resource." + accessToken);
    }

}
