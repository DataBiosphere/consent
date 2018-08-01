package org.broadinstitute.consent.http.configurations;

import javax.validation.constraints.NotNull;

public class NihConfiguration {

    @NotNull
    private String signingSecret;


    public String getSigningSecret() {
        return signingSecret;
    }

}
