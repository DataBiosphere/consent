package org.genomebridge.consent.http.service;

import javax.validation.constraints.NotNull;

public class ServicesConfiguration {

    @NotNull
    public String matchURL;

    public String getMatchURL() {
        return matchURL;
    }

    public void setMatchURL(String matchURL) {
        this.matchURL = matchURL;
    }
}
