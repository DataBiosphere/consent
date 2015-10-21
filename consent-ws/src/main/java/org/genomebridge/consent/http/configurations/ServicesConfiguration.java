package org.genomebridge.consent.http.configurations;

import javax.validation.constraints.NotNull;

public class ServicesConfiguration {

    @NotNull
    public String matchURL;

    @NotNull
    public String localURL;

    public String getLocalURL() {
        return localURL;
    }

    public void setLocalURL(String localURL) {
        this.localURL = localURL;
    }

    public String getMatchURL() {
        return matchURL;
    }

    public void setMatchURL(String matchURL) {
        this.matchURL = matchURL;
    }
}
