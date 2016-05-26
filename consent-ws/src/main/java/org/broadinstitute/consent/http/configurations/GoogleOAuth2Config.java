package org.broadinstitute.consent.http.configurations;

import javax.validation.constraints.NotNull;

public class GoogleOAuth2Config {

    @NotNull
    public String clientId;

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

}
