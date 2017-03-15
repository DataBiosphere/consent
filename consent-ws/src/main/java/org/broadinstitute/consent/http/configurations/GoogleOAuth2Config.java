package org.broadinstitute.consent.http.configurations;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"WeakerAccess", "unused"})
public class GoogleOAuth2Config {

    @NotNull
    public String clientId;

    @NotNull
    public List<String> authorizedAccounts;

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public List<String> getAuthorizedAccounts() {
        return authorizedAccounts;
    }

    public void setAuthorizedAccounts(List<String> authorizedAccounts) {
        this.authorizedAccounts = authorizedAccounts;
    }

    public List<String> getAuthorizedServiceAccounts() {
        List<String> serviceAccounts = new ArrayList<>(getAuthorizedAccounts());
        serviceAccounts.add(getClientId());
        return serviceAccounts;
    }

}
