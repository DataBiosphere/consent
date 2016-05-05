package org.broadinstitute.consent.http.configurations;

import javax.validation.constraints.NotNull;

public class GoogleOAuth2Config {

    @NotNull
    public String clientId;

    @NotNull
    public String clientSecret;


    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

}
