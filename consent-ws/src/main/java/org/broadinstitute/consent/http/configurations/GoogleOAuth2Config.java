package org.broadinstitute.consent.http.configurations;

import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.List;

public class GoogleOAuth2Config {

    @NotNull
    public String clientId;

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public List<String> getAuthorizedServiceAccounts() {
        return Collections.singletonList(clientId);
    }

}
