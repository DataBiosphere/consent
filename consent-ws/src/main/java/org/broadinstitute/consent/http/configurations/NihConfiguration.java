package org.broadinstitute.consent.http.configurations;

import javax.validation.constraints.NotNull;

public class NihConfiguration {

    @NotNull
    private String signingSecret;


    public byte[] getSigningSecret() {
        return signingSecret.getBytes();
    }

}
